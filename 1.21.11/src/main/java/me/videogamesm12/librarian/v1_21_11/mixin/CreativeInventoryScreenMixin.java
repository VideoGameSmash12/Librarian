/*
 * Copyright (C) 2025 Video
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.videogamesm12.librarian.v1_21_11.mixin;

import com.google.common.eventbus.Subscribe;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.videogamesm12.librarian.Librarian;
import me.videogamesm12.librarian.api.*;
import me.videogamesm12.librarian.api.event.*;
import me.videogamesm12.librarian.util.ComponentProcessor;
import me.videogamesm12.librarian.v1_21_11.addon.FabricAPIAddon;
import net.fabricmc.fabric.impl.client.itemgroup.FabricCreativeGuiComponents;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.HotbarStorage;
import net.minecraft.client.option.HotbarStorageEntry;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.IntStream;

@Mixin(CreativeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin extends Screen
{
	@Shadow protected abstract void setSelectedTab(ItemGroup group);

	@Unique
	private static Librarian librarian;

	@Unique
	private static ItemGroup lastGroup = null;
	@Unique
	private static boolean readyToSave = true;

	@Shadow
	private TextFieldWidget searchBox;
	@Shadow
	private float scrollPosition;

	@Shadow
	private @Nullable List<Slot> slots;

	@Shadow
	protected abstract boolean isCreativeInventorySlot(@Nullable Slot slot);

	@Shadow
	@Final
	private boolean operatorTabEnabled;
	@Unique
	private IMechanicFactory mechanic;

	@Unique
	private String lastSuccessfulChange = null;

	@Unique
	private TextFieldWidget renameHotbarField;
	@Unique
	private ButtonWidget nextButton;
	@Unique
	private ButtonWidget backupButton;
	@Unique
	private ButtonWidget previousButton;

	protected CreativeInventoryScreenMixin(Text title)
	{
		super(title);
	}

	// Adds the buttons when the screen is initialized
	@Inject(method = "init", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/screen/PlayerScreenHandler;addListener(Lnet/minecraft/screen/ScreenHandlerListener;)V"))
	public void injectInit(CallbackInfo ci)
	{
		if (librarian == null) librarian = Librarian.getInstance();
		librarian.getEventBus().register(this);
		mechanic = librarian.getMechanic();

		// Offset
		int x = ((HandledScreenAccessor) this).getX() + 167;
		int y = ((HandledScreenAccessor) this).getY() + 4;

		// Adds "rename hotbar" text field
		renameHotbarField = new TextFieldWidget(textRenderer, ((HandledScreenAccessor) this).getX() + 8,
				((HandledScreenAccessor) this).getY() + 6, 144, 12, Text.empty())
		{
			@Override
			public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta)
			{
				// Show the "editor" text when focused
				if (isFocused())
				{
					super.renderWidget(context, mouseX, mouseY, delta);
				}
				// Otherwise, show the regular text and emulate vanilla behavior
				else
				{
					context.drawText(textRenderer, getMessage(), this.getX(), this.getY(), -12566464, false);
				}
			}

			@Override
			public boolean isVisible()
			{
				return tabIsHotbar(getSelectedTab());
			}

			@Override
			public boolean isActive()
			{
				return tabIsHotbar(getSelectedTab()) && isFocused();
			}

			@Override
			public void onClick(Click click, boolean bl)
			{
				setFocused(true);
			}
		};

		renameHotbarField.setFocused(false);
		renameHotbarField.setText(renameHotbarField.getMessage().getString());
		renameHotbarField.setDrawsBackground(false);
		renameHotbarField.setMaxLength(65535);
		renameHotbarField.setTooltip(Tooltip.of(mechanic.createText(Component.translatable("librarian.tooltip.click_to_rename"))));

		// Update the label if we are set to use the HOTBAR ItemGroup type
		// This primarily aims to emulate vanilla behavior and avoid lagspikes when opening the creative menu whilst
		// 	the current page isn't loaded
		if (tabIsHotbar(getSelectedTab()))
		{
			renameHotbarField.setMessage(mechanic.createText(librarian.getCurrentPage().librarian$getMetadata()
					.map(HotbarPageMetadata::getName).orElse(Component.translatable("librarian.saved_hotbars.tab",
							Component.text(librarian.getCurrentPageNumber().toString())))));
		}

		// Even though we override the isVisible and isActive methods, internally ClickableWidget still uses the
		// 	variable themselves to determine other characteristics, so we still need to set them
		renameHotbarField.active = tabIsHotbar(getSelectedTab());
		renameHotbarField.visible = tabIsHotbar(getSelectedTab());

		// Initialize buttons
		nextButton = mechanic.createButton(x + 12, y,12, 12, Component.text("→"),
				Component.text("Next page"), () -> librarian.nextPage());
		backupButton = mechanic.createButton(x, y,12, 12, Component.text("\uD83D\uDCBE")
						.font(Key.key("librarian", "default")), Component.text("Make a backup of this page"),
				() -> librarian.queue(() -> librarian.getCurrentPage().librarian$backup()));
		previousButton = mechanic.createButton(x - 12, y,12, 12, Component.text("←"),
				Component.text("Previous page"), () -> librarian.previousPage());

		// Marks visibility and usability of the buttons
		nextButton.visible = tabIsHotbar(getSelectedTab());
		backupButton.visible = tabIsHotbar(getSelectedTab());
		backupButton.active = librarian.getCurrentPage().exists();
		previousButton.visible = tabIsHotbar(getSelectedTab());

		// Adds the "rename hotbar" text field
		addDrawableChild(renameHotbarField);

		// Adds the buttons to the screen
		addDrawableChild(nextButton);
		addDrawableChild(backupButton);
		addDrawableChild(previousButton);
	}

	@Inject(method = "removed", at = @At(value = "RETURN"))
	public void unregisterOnRemoval(CallbackInfo ci)
	{
		// Unregisters us as an event listener when the menu is closed
		librarian.getEventBus().unregister(this);
	}

	@Inject(method = "setSelectedTab", at = @At("HEAD"))
	public void hookTabSelected(ItemGroup group, CallbackInfo ci)
	{
		// Keep track of the last group prior for later use
		lastGroup = getSelectedTab();

		boolean shouldShowElements = tabIsHotbar(group);

		// Determine visibility and other stuff
		if (renameHotbarField != null)
		{
			if (shouldShowElements && librarian.getCurrentPage().librarian$getLoadStatus() == LoadStatus.LOADED)
			{
				// Updates the "message" which we use to display the formatted text in non-edit mode
				renameHotbarField.setMessage(mechanic.createText(librarian.getCurrentPage().librarian$getMetadata()
						.map(HotbarPageMetadata::getName).orElse(Component.translatable("librarian.saved_hotbars.tab",
								Component.text(librarian.getCurrentPageNumber().toString())))));
			}

			// Updates the text in the field to be the contents of the label so that it stays consistent in the
			// 	edit vs. non-edit modes
			renameHotbarField.setText(renameHotbarField.getMessage().getString());
			renameHotbarField.setFocused(false);

			// See above for why we still set these
			renameHotbarField.visible = shouldShowElements;
			renameHotbarField.active = shouldShowElements;

			// Resets the last successful change
			lastSuccessfulChange = null;
		}

		if (nextButton != null) nextButton.visible = shouldShowElements;
		if (backupButton != null)
		{
			backupButton.visible = shouldShowElements;
			backupButton.active = librarian.getCurrentPage().exists();
		}
		if (previousButton != null) previousButton.visible = shouldShowElements;

		// Avoid overlaps - https://github.com/FabricMC/fabric/pull/2742
		((ScreenAccessor) this).getDrawables().stream().filter(entry ->
				entry instanceof FabricCreativeGuiComponents.ItemGroupButtonWidget).forEach(button ->
				((ButtonWidget) button).visible = !shouldShowElements);
	}

	@Inject(method = "setSelectedTab", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;getCreativeHotbarStorage()Lnet/minecraft/client/option/HotbarStorage;", shift = At.Shift.AFTER), cancellable = true)
	private void insertEmptyLoadingScreen(ItemGroup group, CallbackInfo ci)
	{
		if (tabIsHotbar(group))
		{
			final IWrappedHotbarStorage page = librarian.getCurrentPage();
			switch (page.librarian$getLoadStatus())
			{
				case NOT_LOADED:
				{
					if (librarian.getConfig().optimizations().backgroundLoading())
					{
						page.librarian$loadAsync();
					}
					else
					{
						return;
					}
				}
				case LOADING:
				{
					((CreativeInventoryScreen.CreativeScreenHandler) ((HandledScreenAccessor) this).getHandler()).itemList.clear();

					if (renameHotbarField != null)
					{
						renameHotbarField.setMessage(Text.translatable("librarian.messages.loading",
								page.librarian$getLocation().getName()));
						renameHotbarField.setFocused(false);
						renameHotbarField.active = false;
					}

					searchBox.setVisible(false);
					searchBox.setFocusUnlocked(false);
					searchBox.setFocused(false);
					searchBox.setText("");

					if (lastGroup != null && lastGroup.getType() == ItemGroup.Type.INVENTORY)
					{
						((HandledScreenAccessor) this).getHandler().slots.clear();
						((HandledScreenAccessor) this).getHandler().slots.addAll(Objects.requireNonNull(this.slots));
						this.slots = null;
					}

					scrollPosition = 0.0f;
					((CreativeInventoryScreen.CreativeScreenHandler) ((HandledScreenAccessor) this).getHandler()).scrollItems(0.0f);
					ci.cancel();
					break;
				}
				default:
				{
					// Do nothing
				}
			}
		}
	}

	@ModifyConstant(method = "setSelectedTab", constant = @Constant(intValue = 9, ordinal = 0))
	private int setHotbarRowCount(int constant)
	{
		return librarian.getCurrentPage().librarian$getRowCount();
	}

	@Inject(method = "drawForeground", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemGroup;shouldRenderName()Z", shift = At.Shift.AFTER), cancellable = true)
	public void cancelForegroundTextRendering(DrawContext context, int mouseX, int mouseY, CallbackInfo ci)
	{
		if (tabIsHotbar(getSelectedTab()))
		{
			ci.cancel();
		}
	}

	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	public void workaroundTypingInRenameField(KeyInput input, CallbackInfoReturnable<Boolean> cir)
	{
		if (tabIsHotbar(getSelectedTab()))
		{
			// Special keys
			renameHotbarField.keyPressed(input);

			// Handle key presses if the field is focused
			if (renameHotbarField.isFocused())
			{
				final IWrappedHotbarStorage page = librarian.getCurrentPage();

				// Abort changes if the user presses ESC
				if (input.getKeycode() == GLFW.GLFW_KEY_ESCAPE)
				{
					renameHotbarField.setText(lastSuccessfulChange != null ? lastSuccessfulChange :
							renameHotbarField.getMessage().getString());
					renameHotbarField.setFocused(false);
				}
				// Apply the changes if the user presses ENTER
				else if (input.getKeycode() == GLFW.GLFW_KEY_ENTER)
				{
					final Component newName = ComponentProcessor.findBestPick(renameHotbarField.getText())
							.processComponent(renameHotbarField.getText());

					page.librarian$getMetadata().ifPresentOrElse(metadata ->
							metadata.setName(newName), () ->
							page.librarian$setMetadata(HotbarPageMetadata.builder().name(newName).build()));
					((HotbarStorage) page).save();

					renameHotbarField.setFocused(false);
					renameHotbarField.setMessage(mechanic.createText(newName));

					lastSuccessfulChange = renameHotbarField.getText();

					// Hacky fix, but oh well
					backupButton.active = librarian.getCurrentPage().exists();
				}

				cir.setReturnValue(true);
			}
			else
			{
				super.keyPressed(input);
			}
		}
	}

	@Inject(method = "keyPressed", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;keyPressed(Lnet/minecraft/client/input/KeyInput;)Z",
			shift = At.Shift.BEFORE), cancellable = true)
	public void handleActionKeys(KeyInput input, CallbackInfoReturnable<Boolean> cir)
	{
		// Librarian-specific keybinds
		if (tabIsHotbar(getSelectedTab()))
		{
			// Fabric API keybinds
			final FabricAPIAddon fabric = librarian.getAddon(FabricAPIAddon.class);
			if (fabric.getNextKey().matchesKey(input))
			{
				librarian.nextPage();
				cir.setReturnValue(true);
				return;
			}
			else if (fabric.getPreviousKey().matchesKey(input))
			{
				librarian.previousPage();
				cir.setReturnValue(true);
				return;
			}
			else if (fabric.getBackupKey().matchesKey(input))
			{
				librarian.queue(() -> librarian.getCurrentPage().librarian$backup());
				cir.setReturnValue(true);
				return;
			}
			else if (fabric.getDeleteKey().matchesKey(input))
			{
				final Slot focusedSlot = ((HandledScreenAccessor) this).getFocusedSlot();
				if (focusedSlot == null
						|| !isCreativeInventorySlot(focusedSlot)
						|| focusedSlot.getStack().isEmpty()
						|| librarian.getCurrentPage().librarian$getLoadStatus() != LoadStatus.LOADED)
				{
					return;
				}

				final CreativeInventoryScreen.CreativeScreenHandler handler = ((HandledScreenAccessor) this).getHandler();

				final int row = (((CreativeScreenHandlerMixin) handler).invokeGetRow(scrollPosition) + (focusedSlot.getIndex() / 9));
				final int column = focusedSlot.getIndex() % 9;

				final IWrappedHotbarStorage wrappedStorage = librarian.getCurrentPage();
				final HotbarStorage storage = (HotbarStorage) wrappedStorage;
				final IWrappedHotbarStorageEntry<ItemStack> entry = wrappedStorage.librarian$get(row);


				float scroll = scrollPosition;

				checkBeforeOperation(storage,
						entry,
						Collections.singletonList(ItemStack.EMPTY),
						() -> {
							entry.librarian$setItem(column, ItemStack.EMPTY);
							storage.save();

							// If we weren't prompted, update the current screen
							if (client.currentScreen instanceof CreativeInventoryScreen)
							{
								setSelectedTab(Registries.ITEM_GROUP.get(ItemGroups.HOTBAR));
								handler.scrollItems(scroll);
								scrollPosition = scroll;
							}
						},
						(value) -> {
							final CreativeInventoryScreen screen = new CreativeInventoryScreen(Objects.requireNonNull(client.player),
									Objects.requireNonNull(client.getNetworkHandler()).getEnabledFeatures(),
									operatorTabEnabled);

							client.setScreen(screen);

							((CreativeInventoryScreen.CreativeScreenHandler) ((HandledScreenAccessor) screen).getHandler()).scrollItems(scroll);
							// https://www.youtube.com/watch?v=oiZ3VFCIUy0
							((CreativeInventoryScreenAccessor) screen).setScrollPosition(scroll);
						},
						column,
						"nonmatching");


				cir.setReturnValue(true);
				return;
			}

			// Built-in key binds
			switch (input.modifiers())
			{
				// CTRL
				case GLFW.GLFW_MOD_CONTROL ->
				{
					// R
					if (input.getKeycode() == GLFW.GLFW_KEY_R)
					{
						librarian.reloadCurrentPage();
						cir.setReturnValue(true);
					}
				}
				// ALT
				case GLFW.GLFW_MOD_ALT ->
				{
					// LEFT ARROW
					if (input.getKeycode() == GLFW.GLFW_KEY_LEFT)
					{
						librarian.previousPage();
						cir.setReturnValue(true);
					}
					// RIGHT ARROW
					else if (input.getKeycode() == GLFW.GLFW_KEY_RIGHT)
					{
						librarian.nextPage();
						cir.setReturnValue(true);
					}
				}
				// SHIFT
				case GLFW.GLFW_MOD_SHIFT ->
				{
					// LEFT ARROW
					if (input.getKeycode() == GLFW.GLFW_KEY_LEFT)
					{
						librarian.advanceBy(-5);
						cir.setReturnValue(true);
					}
					// RIGHT ARROW
					else if (input.getKeycode() == GLFW.GLFW_KEY_RIGHT)
					{
						librarian.advanceBy(5);
						cir.setReturnValue(true);
					}
				}
				// ALT + SHIFT
				case GLFW.GLFW_MOD_ALT + GLFW.GLFW_MOD_SHIFT ->
				{
					// LEFT ARROW
					if (input.getKeycode() == GLFW.GLFW_KEY_LEFT)
					{
						librarian.advanceBy(-10);
						cir.setReturnValue(true);
					}
					// RIGHT ARROW
					else if (input.getKeycode() == GLFW.GLFW_KEY_RIGHT)
					{
						librarian.advanceBy(10);
						cir.setReturnValue(true);
					}
				}
			}
		}
	}

	@Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
	private void injectCharTyped(CharInput input, CallbackInfoReturnable<Boolean> cir)
	{
		if (tabIsHotbar(getSelectedTab()))
		{
			if (renameHotbarField.charTyped(input))
			{
				cir.setReturnValue(true);
			}
			else
			{
				cir.setReturnValue(false);
			}
		}
	}

	@WrapMethod(method = "onHotbarKeyPress")
	private static void wrapHotbarSaving(MinecraftClient client, int index, boolean restore, boolean save,
										 Operation<Void> original)
	{
		if (librarian == null) librarian = Librarian.getInstance();

		final HotbarStorage storage = client.getCreativeHotbarStorage();
		final IWrappedHotbarStorage wrappedStorage = (IWrappedHotbarStorage) storage;

		if (librarian.getConfig().optimizations().backgroundLoading())
		{
			switch (wrappedStorage.librarian$getLoadStatus())
			{
				case NOT_LOADED:
				{
					wrappedStorage.librarian$loadAsync();
				}
				case LOADING:
				{
					Objects.requireNonNull(client.player).sendMessage(Text.translatable("librarian.messages.loading",
							wrappedStorage.librarian$getLocation().getName()), true);
					return;
				}
				default:
				{
					break;
				}
			}
		}

		if (save)
		{
			checkBeforeOperation(storage,
					wrappedStorage.librarian$get(index),
					IntStream.range(0, PlayerInventory.getHotbarSize()).mapToObj(column ->
							Objects.requireNonNull(client.player).getInventory().getStack(column)).toList(),
					() -> original.call(client, index, restore, save),
					(value) -> MinecraftClient.getInstance().setScreen(null),
					-1337);
		}
		else
		{
			original.call(client, index, restore, save);
		}
	}

	@ModifyArg(method = "onHotbarKeyPress", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/entity/player/PlayerInventory;setStack(ILnet/minecraft/item/ItemStack;)V"))
	private static ItemStack copyCachedItemsOnRestore(ItemStack stack)
	{
		if (librarian.getConfig().optimizations().preprocessHotbarRows())
		{
			return stack.copyWithCount(stack.getCount());
		}

		return stack;
	}

	@Inject(method = "onMouseClick", at = @At("HEAD"), cancellable = true)
	public void directlyModifyHotbar(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci)
	{
		if (!tabIsHotbar(getSelectedTab())
				|| slot == null
				|| Librarian.getInstance().getCurrentPage().librarian$getLoadStatus() != LoadStatus.LOADED)
		{
			return;
		}

		final CreativeInventoryScreen.CreativeScreenHandler handler = ((HandledScreenAccessor) this).getHandler();

		// Get everything related to the current hotbar page
		final HotbarStorage page = (HotbarStorage) Librarian.getInstance().getCurrentPage();
		final IWrappedHotbarStorage wrapped = (IWrappedHotbarStorage) page;

		// Determine row and column
		int row = (((CreativeScreenHandlerMixin) handler).invokeGetRow(scrollPosition) + (slot.getIndex() / 9));
		int column = slot.getIndex() % 9;

		// Get the item stack in their cursor
		final ItemStack cursor = handler.getCursorStack().copy();

		// If the item in their cursor isn't air and the action they are trying to do is related to adding/swapping them...
		if (cursor.getItem() != Items.AIR
				&& isCreativeInventorySlot(slot)
				&& (actionType == SlotActionType.PICKUP || actionType == SlotActionType.SWAP))
		{
			Librarian.getLogger().warn("Detected click relevant to us!");

			final IWrappedHotbarStorageEntry<ItemStack> wrappedStorageEntry = wrapped.librarian$get(row);
			final ItemStack original = wrappedStorageEntry.librarian$getItem(column).copy();
			float scroll = scrollPosition;

			final int fuckOffIntellij = column;

			checkBeforeOperation(page,
					wrappedStorageEntry,
					Collections.singletonList(cursor.copy()),
					() -> {
						wrappedStorageEntry.librarian$setItem(fuckOffIntellij, cursor.copy());
						page.save();

						// If we weren't prompted, update the current screen
						if (client.currentScreen instanceof CreativeInventoryScreen screen)
						{
							setSelectedTab(Registries.ITEM_GROUP.get(ItemGroups.HOTBAR));
							handler.scrollItems(scroll);
							scrollPosition = scroll;

							if (!original.isEmpty())
							{
								((HandledScreenAccessor) screen).getHandler().setCursorStack(original);
							}
						}
					},
					(value) -> {
						final CreativeInventoryScreen screen = new CreativeInventoryScreen(Objects.requireNonNull(client.player),
								Objects.requireNonNull(client.getNetworkHandler()).getEnabledFeatures(),
								operatorTabEnabled);

						client.setScreen(screen);

						((CreativeInventoryScreen.CreativeScreenHandler) ((HandledScreenAccessor) screen).getHandler()).scrollItems(scroll);
						// What the hell do you mean "it'll throw a class cast exception"? No it won't?
						((CreativeInventoryScreenAccessor) screen).setScrollPosition(scroll);
						((HandledScreenAccessor) screen).getHandler().setCursorStack(value ? original : cursor);
					},
					column);

			ci.cancel();
		}
		// If the item they're clicking isn't air, is in their inventory, and the user shift-clicked...
		else if (slot.getStack().getItem() != Items.AIR
				&& !isCreativeInventorySlot(slot)
				&& actionType == SlotActionType.QUICK_MOVE)
		{
			// Basic idea for functionality: upon shift-clicking the item from your inventory, we look for a spot for
			// 	empty space and if we find one, we use that. Otherwise, we simply do nothing.

			// Prevent items from being lost accidentally
			ci.cancel();

			// Avoid saving until we are flagged as ready to do so
			if (!readyToSave)
			{
				return;
			}

			// Scan for empty slots
			scan:
			for (row = 0; row < wrapped.librarian$getRowCount(); row++)
			{
				final IWrappedHotbarStorageEntry<ItemStack> entry = wrapped.librarian$get(row);

				for (column = 0; column < PlayerInventory.getHotbarSize(); column++)
				{
					// Found an empty slot!
					if (entry.librarian$getItem(column).getItem() == Items.AIR)
					{
						// Store a hash of the menu so we can determine if we need to update it later
						final int screenCode = Objects.requireNonNull(client.currentScreen).hashCode();

						float scroll = scrollPosition;
						final int fuckOffIntellij = column;

						// We're not concerned about accidentally overwriting a row since that isn't possible, but we
						// 	are concerned about other checks like accidental downgrades.
						checkBeforeOperation(page,
								entry,
								Collections.singletonList(entry.librarian$getItem(column)),
								() -> {
									// Prevent accidentally
									readyToSave = false;
									entry.librarian$setItem(fuckOffIntellij, slot.getStack().copy());
									page.save();

									readyToSave = true;

									// Remove the item from their inventory
									Objects.requireNonNull(client.player).getInventory().setStack(slot.getIndex(), ItemStack.EMPTY);
									Objects.requireNonNull(client.interactionManager).clickCreativeStack(ItemStack.EMPTY, slotId);

									if (client.currentScreen != null && client.currentScreen.hashCode() == screenCode)
									{
										setSelectedTab(Registries.ITEM_GROUP.get(ItemGroups.HOTBAR));
										handler.scrollItems(scroll);
										scrollPosition = scroll;
									}
								},
								(value) -> {
									final CreativeInventoryScreen screen = new CreativeInventoryScreen(Objects.requireNonNull(client.player),
											Objects.requireNonNull(client.getNetworkHandler()).getEnabledFeatures(),
											operatorTabEnabled);

									client.setScreen(screen);

									((CreativeInventoryScreen.CreativeScreenHandler) ((HandledScreenAccessor) screen).getHandler()).scrollItems(scroll);
									// It is literally impossible for it to throw that exception, shut up Intellij
									((CreativeInventoryScreenAccessor) screen).setScrollPosition(scroll);
								},
								column);

						break scan;
					}
				}
			}
		}
	}

	@Subscribe
	@Unique
	public void onNavigation(NavigationEvent event)
	{
		// Refresh!
		if (tabIsHotbar(getSelectedTab()))
		{
			setSelectedTab(Registries.ITEM_GROUP.get(ItemGroups.HOTBAR));
		}
	}

	@Subscribe
	@Unique
	public void onReload(ReloadPageEvent event)
	{
		if (tabIsHotbar(getSelectedTab()))
		{
			setSelectedTab(Registries.ITEM_GROUP.get(ItemGroups.HOTBAR));
		}
	}

	@Subscribe
	@Unique
	public void onCacheClear(CacheClearEvent event)
	{
		if (tabIsHotbar(getSelectedTab()))
		{
			setSelectedTab(Registries.ITEM_GROUP.get(ItemGroups.HOTBAR));
		}
	}

	@Subscribe
	@Unique
	public void onPageLoad(AsyncPageLoadEvent event)
	{
		if (tabIsHotbar(getSelectedTab()) && event.getPage().librarian$getPageNumber().equals(librarian.getCurrentPageNumber()))
		{
			MinecraftClient.getInstance().execute(() -> setSelectedTab(Registries.ITEM_GROUP.get(ItemGroups.HOTBAR)));
		}
	}

	@Unique
	private static void checkBeforeOperation(HotbarStorage storage, IWrappedHotbarStorageEntry<ItemStack> row,
											 List<ItemStack> items, Runnable ifYes, Consumer<Boolean> cleanup,
											 int columnIfSingular, String... ignored)
	{
		final MinecraftClient client = MinecraftClient.getInstance();
		final IWrappedHotbarStorage wrapped = (IWrappedHotbarStorage) storage;
		final boolean backgroundSaving = librarian.getConfig().optimizations().backgroundSaving();
		final List<String> ignoredIssues = Arrays.stream(ignored).toList();

		Runnable scanner = () ->
		{
			final List<String> issues = new ArrayList<>();

			downgradeCheck:
			{
				if (ignoredIssues.contains("downgrade"))
				{
					break downgradeCheck;
				}

				if (wrapped.librarian$dataVersion() > SharedConstants.getGameVersion().dataVersion().id())
				{
					issues.add("downgrade");
					break downgradeCheck;
				}
			}

			overwriteCheck:
			{
				if (ignoredIssues.contains("nonmatching"))
				{
					break overwriteCheck;
				}

				final List<ItemStack> storageEntry = ((HotbarStorageEntry) row).deserialize(Objects.requireNonNull(client.world)
						.getRegistryManager());

				if (storageEntry.isEmpty())
				{
					break overwriteCheck;
				}

				if (items.size() == 1)
				{
					final ItemStack inventoryStack = items.getFirst();
					final ItemStack hotbarEntry = storageEntry.get(columnIfSingular);

					if (!hotbarEntry.isEmpty() && !ItemStack.areItemsAndComponentsEqual(inventoryStack, hotbarEntry))
					{
						issues.add("nonmatching");
						break overwriteCheck;
					}
				}
				else
				{
					for (int i = 0; i < PlayerInventory.getHotbarSize(); i++)
					{
						ItemStack inventoryStack = items.get(i);
						ItemStack hotbarEntry = storageEntry.get(i);

						if (!hotbarEntry.isEmpty() && !ItemStack.areItemsAndComponentsEqual(inventoryStack, hotbarEntry))
						{
							issues.add("nonmatching");
							break overwriteCheck;
						}
					}
				}
			}

			if (!issues.isEmpty())
			{
				final MutableText title;
				final MutableText description;

				// Only one issue found, use more specific message for that
				if (issues.size() == 1)
				{
					title = Text.translatable("librarian.messages.issues." + issues.getFirst() + ".title");
					description = Text.translatable("librarian.messages.issues." + issues.getFirst() + ".description");
				}
				// Otherwise, use more brief versions instead
				else
				{
					title = Text.translatable("librarian.messages.possible_loss_scenario_detected.title");
					description = Text.translatable("librarian.messages.possible_loss_scenario_detected.description");
					issues.forEach(issue -> description.append(Text.translatable("librarian.messages.issues." + issue + ".summary")).append("\n\n"));
					description.append(Text.translatable("librarian.messages.possible_loss_scenario_detected.footer"));
				}

				// This is unbelievably bad for an optimization hack, but if I don't run setScreen() on the game
				// 	thread, the entire game crashes. If I don't account for background saving in the nested block,
				// 	then the game runs the "save" code on the game thread when it shouldn't, causing lagspikes
				client.execute(() -> client.setScreen(new ConfirmScreen((value) ->
				{
					if (value)
					{
						if (backgroundSaving)
						{
							Librarian.getInstance().queue(ifYes);
						}
						else
						{
							ifYes.run();
						}
					}

					cleanup.accept(value);
				}, title, description)));
			}
			else
			{
				if (backgroundSaving)
				{
					Librarian.getInstance().queue(ifYes);
				}
				else
				{
					ifYes.run();
				}
			}
		};

		if (backgroundSaving)
		{
			Librarian.getInstance().queue(scanner);
		}
		else
		{
			scanner.run();
		}
	}

	@Unique
	private boolean tabIsHotbar(ItemGroup group)
	{
		return group.getType() == ItemGroup.Type.HOTBAR;
	}

	@Accessor
	public abstract ItemGroup getSelectedTab();
}