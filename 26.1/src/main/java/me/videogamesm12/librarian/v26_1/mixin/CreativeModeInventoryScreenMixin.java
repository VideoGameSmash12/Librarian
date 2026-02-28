/*
 * Copyright (C) 2026 Video
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

package me.videogamesm12.librarian.v26_1.mixin;

import com.google.common.eventbus.Subscribe;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.videogamesm12.librarian.Librarian;
import me.videogamesm12.librarian.api.HotbarPageMetadata;
import me.videogamesm12.librarian.api.IMechanicFactory;
import me.videogamesm12.librarian.api.IWrappedHotbarStorage;
import me.videogamesm12.librarian.api.event.CacheClearEvent;
import me.videogamesm12.librarian.api.event.NavigationEvent;
import me.videogamesm12.librarian.api.event.ReloadPageEvent;
import me.videogamesm12.librarian.util.ComponentProcessor;
import me.videogamesm12.librarian.v26_1.addon.FabricAPIAddon;
import net.fabricmc.fabric.impl.client.creativetab.FabricCreativeGuiComponents;
import net.kyori.adventure.key.Key;
import net.minecraft.client.HotbarManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Objects;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin extends Screen
{
	@Shadow protected abstract void selectTab(CreativeModeTab group);

	@Shadow
	private static CreativeModeTab selectedTab;
	@Unique
	private IMechanicFactory mechanic;

	@Unique
	private String lastSuccessfulChange = null;

	@Unique
	private EditBox renameHotbarField;
	@Unique
	private Button nextButton;
	@Unique
	private Button backupButton;
	@Unique
	private Button previousButton;

	protected CreativeModeInventoryScreenMixin(net.minecraft.network.chat.Component title)
	{
		super(title);
	}

	// Adds the buttons when the screen is initialized
	@Inject(method = "init", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/inventory/InventoryMenu;addSlotListener(Lnet/minecraft/world/inventory/ContainerListener;)V"))
	public void injectInit(CallbackInfo ci)
	{
		Librarian.getInstance().getEventBus().register(this);
		mechanic = Librarian.getInstance().getMechanic();

		// Offset
		int x = ((AbstractContainerScreenAccessor) this).getX() + 167;
		int y = ((AbstractContainerScreenAccessor) this).getY() + 4;

		// Adds "rename hotbar" text field
		renameHotbarField = new EditBox(font, ((AbstractContainerScreenAccessor) this).getX() + 8,
				((AbstractContainerScreenAccessor) this).getY() + 6, 144, 12, Component.empty())
		{
			@Override
			public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float delta)
			{
				// Show the "editor" text when focused
				if (isFocused())
				{
					super.renderWidget(graphics, mouseX, mouseY, delta);
				}
				// Otherwise, show the regular text and emulate vanilla behavior
				else
				{
					graphics.drawString(font, getMessage(), this.getX(), this.getY(), -12566464, false);
				}
			}

			@Override
			public boolean isVisible()
			{
				return tabIsHotbar(selectedTab);
			}

			@Override
			public boolean isActive()
			{
				return tabIsHotbar(selectedTab) && isFocused();
			}

			@Override
			public boolean mouseClicked(MouseButtonEvent event, boolean bl)
			{
				Librarian.getLogger().fatal("Debug - mouse clicked event");

				if (!isFocused())
				{
					Librarian.getLogger().fatal("Not focused");
					setFocused(true);
					return true;
				}

				Librarian.getLogger().fatal("Focused");
				return super.mouseClicked(event, bl);
			}
		};

		renameHotbarField.setFocused(false);
		renameHotbarField.setValue(renameHotbarField.getMessage().getString());
		renameHotbarField.setBordered(false);
		renameHotbarField.setMaxLength(65535);
		renameHotbarField.setTooltip(Tooltip.create(Component.translatable("librarian.tooltip.click_to_rename")));

		// Update the label if we are set to use the HOTBAR ItemGroup type
		// This primarily aims to emulate vanilla behavior and avoid lagspikes when opening the creative menu whilst
		// 	the current page isn't loaded
		if (tabIsHotbar(selectedTab))
		{
			renameHotbarField.setMessage(mechanic.createText(Librarian.getInstance().getCurrentPage().librarian$getMetadata()
					.map(HotbarPageMetadata::getName).orElse(net.kyori.adventure.text.Component.translatable("librarian.saved_hotbars.tab",
							net.kyori.adventure.text.Component.text(Librarian.getInstance().getCurrentPageNumber().toString())))));
		}

		// Even though we override the isVisible and isActive methods, internally ClickableWidget still uses the
		// 	variable themselves to determine other characteristics, so we still need to set them
		renameHotbarField.active = tabIsHotbar(selectedTab);
		renameHotbarField.visible = tabIsHotbar(selectedTab);

		// Initialize buttons
		nextButton = mechanic.createButton(x + 12, y,12, 12, net.kyori.adventure.text.Component.text("→"),
				net.kyori.adventure.text.Component.text("Next page"), () -> Librarian.getInstance().nextPage());
		backupButton = mechanic.createButton(x, y,12, 12, net.kyori.adventure.text.Component.text("\uD83D\uDCBE")
						.font(Key.key("librarian", "default")), net.kyori.adventure.text.Component.text("Make a backup of this page"),
				() -> Librarian.getInstance().getCurrentPage().librarian$backup());
		previousButton = mechanic.createButton(x - 12, y,12, 12, net.kyori.adventure.text.Component.text("←"),
				net.kyori.adventure.text.Component.text("Previous page"), () -> Librarian.getInstance().previousPage());

		// Marks visibility and usability of the buttons
		nextButton.visible = tabIsHotbar(selectedTab);
		backupButton.visible = tabIsHotbar(selectedTab);
		backupButton.active = Librarian.getInstance().getCurrentPage().exists();
		previousButton.visible = tabIsHotbar(selectedTab);

		// Adds the "rename hotbar" text field
		addRenderableWidget(renameHotbarField);

		// Adds the buttons to the screen
		addRenderableWidget(nextButton);
		addRenderableWidget(backupButton);
		addRenderableWidget(previousButton);
	}

	@Inject(method = "removed", at = @At(value = "RETURN"))
	public void unregisterOnRemoval(CallbackInfo ci)
	{
		// Unregisters us as an event listener when the menu is closed
		Librarian.getInstance().getEventBus().unregister(this);
	}

	@Inject(method = "selectTab", at = @At("HEAD"))
	public void hookTabSelected(CreativeModeTab group, CallbackInfo ci)
	{
		boolean shouldShowElements = tabIsHotbar(group);

		// Determine visibility and other stuff
		if (renameHotbarField != null)
		{
			if (shouldShowElements)
			{
				// Updates the "message" which we use to display the formatted text in non-edit mode
				renameHotbarField.setMessage(mechanic.createText(Librarian.getInstance().getCurrentPage().librarian$getMetadata()
						.map(HotbarPageMetadata::getName).orElse(net.kyori.adventure.text.Component.translatable("librarian.saved_hotbars.tab",
								net.kyori.adventure.text.Component.text(Librarian.getInstance().getCurrentPageNumber().toString())))));
			}

			// Updates the text in the field to be the contents of the label so that it stays consistent in the
			// 	edit vs. non-edit modes
			renameHotbarField.setValue(renameHotbarField.getMessage().getString());
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
			backupButton.active = Librarian.getInstance().getCurrentPage().exists();
		}
		if (previousButton != null) previousButton.visible = shouldShowElements;

		// Avoid overlaps - https://github.com/FabricMC/fabric/pull/2742
		((ScreenAccessor) this).getRenderables().stream().filter(entry ->
				entry instanceof FabricCreativeGuiComponents.CreativeModeTabButton).forEach(button ->
				((Button) button).visible = !shouldShowElements);
	}

	@Inject(method = "renderLabels", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/CreativeModeTab;showTitle()Z", shift = At.Shift.AFTER), cancellable = true)
	public void cancelForegroundTextRendering(GuiGraphics context, int mouseX, int mouseY, CallbackInfo ci)
	{
		if (tabIsHotbar(selectedTab))
		{
			ci.cancel();
		}
	}

	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	public void workaroundTypingInRenameField(KeyEvent input, CallbackInfoReturnable<Boolean> cir)
	{
		if (tabIsHotbar(selectedTab))
		{
			// Special keys
			renameHotbarField.keyPressed(input);

			// Handle key presses if the field is focused
			if (renameHotbarField.isFocused())
			{
				final IWrappedHotbarStorage page = Librarian.getInstance().getCurrentPage();

				// Abort changes if the user presses ESC
				if (input.key() == GLFW.GLFW_KEY_ESCAPE)
				{
					renameHotbarField.setValue(lastSuccessfulChange != null ? lastSuccessfulChange :
							renameHotbarField.getMessage().getString());
					renameHotbarField.setFocused(false);
				}
				// Apply the changes if the user presses ENTER
				else if (input.key() == GLFW.GLFW_KEY_ENTER)
				{
					final net.kyori.adventure.text.Component newName = ComponentProcessor.findBestPick(renameHotbarField.getValue())
							.processComponent(renameHotbarField.getValue());

					page.librarian$getMetadata().ifPresentOrElse(metadata ->
							metadata.setName(newName), () ->
							page.librarian$setMetadata(HotbarPageMetadata.builder().name(newName).build()));
					((HotbarManager) page).save();

					renameHotbarField.setFocused(false);
					renameHotbarField.setMessage(mechanic.createText(newName));

					lastSuccessfulChange = renameHotbarField.getValue();

					// Hacky fix, but oh well
					backupButton.active = Librarian.getInstance().getCurrentPage().exists();
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
			target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;keyPressed(Lnet/minecraft/client/input/KeyEvent;)Z",
			shift = At.Shift.BEFORE), cancellable = true)
	public void handleNavigationKeys(KeyEvent input, CallbackInfoReturnable<Boolean> cir)
	{
		// Librarian-specific keybinds
		if (tabIsHotbar(selectedTab))
		{
			// Fabric API keybinds
			final FabricAPIAddon fabric = Librarian.getInstance().getAddon(FabricAPIAddon.class);
			if (fabric.getNextKey().matches(input))
			{
				Librarian.getInstance().nextPage();
				cir.setReturnValue(true);
				return;
			}
			else if (fabric.getPreviousKey().matches(input))
			{
				Librarian.getInstance().previousPage();
				cir.setReturnValue(true);
				return;
			}
			else if (fabric.getBackupKey().matches(input))
			{
				Librarian.getInstance().getCurrentPage().librarian$backup();
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
					if (input.key() == GLFW.GLFW_KEY_R)
					{
						Librarian.getInstance().reloadCurrentPage();
						cir.setReturnValue(true);
					}
				}
				// ALT
				case GLFW.GLFW_MOD_ALT ->
				{
					// LEFT ARROW
					if (input.key() == GLFW.GLFW_KEY_LEFT)
					{
						Librarian.getInstance().previousPage();
						cir.setReturnValue(true);
					}
					// RIGHT ARROW
					else if (input.key() == GLFW.GLFW_KEY_RIGHT)
					{
						Librarian.getInstance().nextPage();
						cir.setReturnValue(true);
					}
				}
				// SHIFT
				case GLFW.GLFW_MOD_SHIFT ->
				{
					// LEFT ARROW
					if (input.key() == GLFW.GLFW_KEY_LEFT)
					{
						Librarian.getInstance().advanceBy(-5);
						cir.setReturnValue(true);
					}
					// RIGHT ARROW
					else if (input.key() == GLFW.GLFW_KEY_RIGHT)
					{
						Librarian.getInstance().advanceBy(5);
						cir.setReturnValue(true);
					}
				}
				// ALT + SHIFT
				case GLFW.GLFW_MOD_ALT + GLFW.GLFW_MOD_SHIFT ->
				{
					// LEFT ARROW
					if (input.key() == GLFW.GLFW_KEY_LEFT)
					{
						Librarian.getInstance().advanceBy(-10);
						cir.setReturnValue(true);
					}
					// RIGHT ARROW
					else if (input.key() == GLFW.GLFW_KEY_RIGHT)
					{
						Librarian.getInstance().advanceBy(10);
						cir.setReturnValue(true);
					}
				}
			}
		}
	}

	@Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
	private void injectCharTyped(CharacterEvent input, CallbackInfoReturnable<Boolean> cir)
	{
		if (tabIsHotbar(selectedTab))
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

	@WrapMethod(method = "handleHotbarLoadOrSave")
	private static void checkForAccidentalOverwrites(Minecraft client, int index, boolean restore, boolean save,
													 Operation<Void> original)
	{
		if (save)
		{
			final HotbarManager storage = client.getHotbarManager();
			final List<ItemStack> storageEntry = storage.get(index).load(Objects.requireNonNull(client.level)
					.registryAccess());

			if (storageEntry.isEmpty())
			{
				original.call(client, index, restore, save);
				return;
			}

			boolean confirm = false;

			for (int i = 0; i < Inventory.getSelectionSize(); i++)
			{
				ItemStack inventoryStack = Objects.requireNonNull(client.player).getInventory().getItem(i);
				ItemStack hotbarEntry = storageEntry.get(i);

				if (!hotbarEntry.isEmpty() && !ItemStack.isSameItemSameComponents(inventoryStack, hotbarEntry))
				{
					confirm = true;
					break;
				}
			}

			if (confirm)
			{
				Minecraft.getInstance().setScreen(new ConfirmScreen((value) ->
				{
					if (value) original.call(client, index, restore, save);
					Minecraft.getInstance().setScreen(null);
				}, net.minecraft.network.chat.Component.translatable("librarian.messages.possible_overwrite_detected.title"), net.minecraft.network.chat.Component.translatable("librarian.messages.possible_overwrite_detected.description")));
			}
			else
			{
				original.call(client, index, restore, save);
			}
		}
		else
		{
			original.call(client, index, restore, save);
		}
	}

	@Subscribe
	@Unique
	public void onNavigation(NavigationEvent event)
	{
		// Refresh!
		if (tabIsHotbar(selectedTab))
		{
			selectTab(BuiltInRegistries.CREATIVE_MODE_TAB.getValue(CreativeModeTabs.HOTBAR));
		}
	}

	@Subscribe
	@Unique
	public void onReload(ReloadPageEvent event)
	{
		if (tabIsHotbar(selectedTab))
		{
			selectTab(BuiltInRegistries.CREATIVE_MODE_TAB.getValue(CreativeModeTabs.HOTBAR));
		}
	}

	@Subscribe
	@Unique
	public void onCacheClear(CacheClearEvent event)
	{
		if (tabIsHotbar(selectedTab))
		{
			selectTab(BuiltInRegistries.CREATIVE_MODE_TAB.getValue(CreativeModeTabs.HOTBAR));
		}
	}

	@Unique
	private boolean tabIsHotbar(CreativeModeTab group)
	{
		return group.getType() == CreativeModeTab.Type.HOTBAR;
	}
}
