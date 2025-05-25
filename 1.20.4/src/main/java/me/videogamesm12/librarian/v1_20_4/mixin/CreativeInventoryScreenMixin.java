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

package me.videogamesm12.librarian.v1_20_4.mixin;

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
import me.videogamesm12.librarian.v1_20_4.addon.FabricAPIAddon;
import net.fabricmc.fabric.impl.client.itemgroup.FabricCreativeGuiComponents;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.option.HotbarStorage;
import net.minecraft.client.option.HotbarStorageEntry;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(CreativeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin extends Screen
{
	@Shadow protected abstract void setSelectedTab(ItemGroup group);

	@Shadow private static ItemGroup selectedTab;
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
		Librarian.getInstance().getEventBus().register(this);
		mechanic = Librarian.getInstance().getMechanic();

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
					context.drawText(textRenderer, getMessage(), this.getX(), this.getY(), 0x404040, false);
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
			public void onRelease(double mouseX, double mouseY)
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
		if (tabIsHotbar(selectedTab))
		{
			renameHotbarField.setMessage(mechanic.createText(Librarian.getInstance().getCurrentPage().librarian$getMetadata()
					.map(HotbarPageMetadata::getName).orElse(Component.translatable("librarian.saved_hotbars.tab",
							Component.text(Librarian.getInstance().getCurrentPageNumber().toString())))));
		}

		// Even though we override the isVisible and isActive methods, internally ClickableWidget still uses the
		// 	variable themselves to determine other characteristics, so we still need to set them
		renameHotbarField.active = tabIsHotbar(selectedTab);
		renameHotbarField.visible = tabIsHotbar(selectedTab);

		// Initialize buttons
		nextButton = mechanic.createButton(x + 12, y,12, 12, Component.text("→"),
				Component.text("Next page"), () -> Librarian.getInstance().nextPage());
		backupButton = mechanic.createButton(x, y,12, 12, Component.text("\uD83D\uDCBE")
				.font(Key.key("librarian", "default")), Component.text("Make a backup of this page"),
				() -> Librarian.getInstance().getCurrentPage().librarian$backup());
		previousButton = mechanic.createButton(x - 12, y,12, 12, Component.text("←"),
				Component.text("Previous page"), () -> Librarian.getInstance().previousPage());

		// Marks visibility and usability of the buttons
		nextButton.visible = tabIsHotbar(selectedTab);
		backupButton.visible = tabIsHotbar(selectedTab);
		backupButton.active = Librarian.getInstance().getCurrentPage().exists();
		previousButton.visible = tabIsHotbar(selectedTab);

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
		Librarian.getInstance().getEventBus().unregister(this);
	}

	@Inject(method = "setSelectedTab", at = @At("HEAD"))
	public void hookTabSelected(ItemGroup group, CallbackInfo ci)
	{
		boolean shouldShowElements = tabIsHotbar(group);

		// Determine visibility and other stuff
		if (renameHotbarField != null)
		{
			if (shouldShowElements)
			{
				// Updates the "message" which we use to display the formatted text in non-edit mode
				renameHotbarField.setMessage(mechanic.createText(Librarian.getInstance().getCurrentPage().librarian$getMetadata()
						.map(HotbarPageMetadata::getName).orElse(Component.translatable("librarian.saved_hotbars.tab",
								Component.text(Librarian.getInstance().getCurrentPageNumber().toString())))));
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

		// Determine visibility and other stuff
		if (nextButton != null) nextButton.visible = shouldShowElements;
		if (backupButton != null)
		{
			backupButton.visible = shouldShowElements;
			backupButton.active = Librarian.getInstance().getCurrentPage().exists();
		}
		if (previousButton != null) previousButton.visible = shouldShowElements;

		// Avoid overlaps - https://github.com/FabricMC/fabric/pull/2742
		((ScreenAccessor) this).getDrawables().stream().filter(entry ->
				entry instanceof FabricCreativeGuiComponents.ItemGroupButtonWidget).forEach(button ->
				((ButtonWidget) button).visible = !shouldShowElements);
	}

	@Inject(method = "drawForeground", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemGroup;shouldRenderName()Z", shift = At.Shift.AFTER), cancellable = true)
	public void cancelForegroundTextRendering(DrawContext context, int mouseX, int mouseY, CallbackInfo ci)
	{
		if (tabIsHotbar(selectedTab))
		{
			ci.cancel();
		}
	}

	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	public void workaroundTypingInRenameField(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir)
	{
		if (tabIsHotbar(selectedTab))
		{
			// Special keys
			renameHotbarField.keyPressed(keyCode, scanCode, modifiers);

			// Handle key presses if the field is focused
			if (renameHotbarField.isFocused())
			{
				final IWrappedHotbarStorage page = Librarian.getInstance().getCurrentPage();

				// Abort changes if the user presses ESC
				if (keyCode == GLFW.GLFW_KEY_ESCAPE)
				{
					renameHotbarField.setText(lastSuccessfulChange != null ? lastSuccessfulChange :
							renameHotbarField.getMessage().getString());
					renameHotbarField.setFocused(false);
				}
				// Apply the changes if the user presses ENTER
				else if (keyCode == GLFW.GLFW_KEY_ENTER)
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
					backupButton.active = Librarian.getInstance().getCurrentPage().exists();
				}

				cir.setReturnValue(true);
			}
			else
			{
				super.keyPressed(keyCode, scanCode, modifiers);
			}
		}
	}

	@Inject(method = "keyPressed", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screen/ingame/AbstractInventoryScreen;keyPressed(III)Z",
			shift = At.Shift.BEFORE), cancellable = true)
	public void handleNavigationKeys(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir)
	{
		// Librarian-specific keybinds
		if (tabIsHotbar(selectedTab))
		{
			// Fabric API keybinds
			final FabricAPIAddon fabric = Librarian.getInstance().getAddon(FabricAPIAddon.class);
			if (fabric.getNextKey().matchesKey(keyCode, scanCode))
			{
				Librarian.getInstance().nextPage();
				cir.setReturnValue(true);
				return;
			}
			else if (fabric.getPreviousKey().matchesKey(keyCode, scanCode))
			{
				Librarian.getInstance().previousPage();
				cir.setReturnValue(true);
				return;
			}
			else if (fabric.getBackupKey().matchesKey(keyCode, scanCode))
			{
				Librarian.getInstance().getCurrentPage().librarian$backup();
				cir.setReturnValue(true);
				return;
			}

			// Built-in key binds
			switch (modifiers)
			{
				// CTRL
				case GLFW.GLFW_MOD_CONTROL ->
				{
					// R
					if (keyCode == GLFW.GLFW_KEY_R)
					{
						Librarian.getInstance().reloadCurrentPage();
						cir.setReturnValue(true);
					}
				}
				// ALT
				case GLFW.GLFW_MOD_ALT ->
				{
					// LEFT ARROW
					if (keyCode == GLFW.GLFW_KEY_LEFT)
					{
						Librarian.getInstance().previousPage();
						cir.setReturnValue(true);
					}
					// RIGHT ARROW
					else if (keyCode == GLFW.GLFW_KEY_RIGHT)
					{
						Librarian.getInstance().nextPage();
						cir.setReturnValue(true);
					}
				}
				// SHIFT
				case GLFW.GLFW_MOD_SHIFT ->
				{
					// LEFT ARROW
					if (keyCode == GLFW.GLFW_KEY_LEFT)
					{
						Librarian.getInstance().advanceBy(-5);
						cir.setReturnValue(true);
					}
					// RIGHT ARROW
					else if (keyCode == GLFW.GLFW_KEY_RIGHT)
					{
						Librarian.getInstance().advanceBy(5);
						cir.setReturnValue(true);
					}
				}
				// ALT + SHIFT
				case GLFW.GLFW_MOD_ALT + GLFW.GLFW_MOD_SHIFT ->
				{
					// LEFT ARROW
					if (keyCode == GLFW.GLFW_KEY_LEFT)
					{
						Librarian.getInstance().advanceBy(-10);
						cir.setReturnValue(true);
					}
					// RIGHT ARROW
					else if (keyCode == GLFW.GLFW_KEY_RIGHT)
					{
						Librarian.getInstance().advanceBy(10);
						cir.setReturnValue(true);
					}
				}
			}
		}
	}

	@Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
	private void injectCharTyped(char chr, int modifiers, CallbackInfoReturnable<Boolean> cir)
	{
		if (tabIsHotbar(selectedTab))
		{
			if (renameHotbarField.charTyped(chr, modifiers))
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
	private static void checkForAccidentalOverwrites(MinecraftClient client, int index, boolean restore, boolean save, Operation<Void> original)
	{
		if (save)
		{
			final HotbarStorage storage = client.getCreativeHotbarStorage();
			final HotbarStorageEntry storageEntry = storage.getSavedHotbar(index);

			if (storageEntry.isEmpty())
			{
				original.call(client, index, restore, save);
				return;
			}

			boolean confirm = false;

			for (int i = 0; i < PlayerInventory.getHotbarSize(); i++)
			{
				ItemStack inventoryStack = Objects.requireNonNull(client.player).getInventory().getStack(i);
				ItemStack hotbarEntry = storageEntry.get(i);

				if (!hotbarEntry.isEmpty() && !ItemStack.areEqual(inventoryStack, hotbarEntry))
				{
					confirm = true;
					break;
				}
			}

			if (confirm)
			{
				MinecraftClient.getInstance().setScreen(new ConfirmScreen((value) ->
				{
					if (value) original.call(client, index, restore, save);
					MinecraftClient.getInstance().setScreen(null);
				}, Text.translatable("librarian.messages.possible_overwrite_detected.title"), Text.translatable("librarian.messages.possible_overwrite_detected.description")));
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
			setSelectedTab(Registries.ITEM_GROUP.get(ItemGroups.HOTBAR));
		}
	}

	@Subscribe
	@Unique
	public void onReload(ReloadPageEvent event)
	{
		if (tabIsHotbar(selectedTab))
		{
			setSelectedTab(Registries.ITEM_GROUP.get(ItemGroups.HOTBAR));
		}
	}

	@Subscribe
	@Unique
	public void onCacheClear(CacheClearEvent event)
	{
		if (tabIsHotbar(selectedTab))
		{
			setSelectedTab(Registries.ITEM_GROUP.get(ItemGroups.HOTBAR));
		}
	}

	@Unique
	private boolean tabIsHotbar(ItemGroup group)
	{
		return group.getType() == ItemGroup.Type.HOTBAR;
	}
}
