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

package me.videogamesm12.librarian.v1_12_2.ornithe.mixin;

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
import me.videogamesm12.librarian.v1_12_2.ornithe.ILButtonWidget;
import me.videogamesm12.librarian.v1_12_2.ornithe.addon.OSLAddon;
import me.videogamesm12.librarian.v1_12_2.ornithe.widget.FormattedTextFieldWidget;
import net.kyori.adventure.text.Component;
import net.minecraft.client.Hotbar;
import net.minecraft.client.HotbarManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.menu.CreativeInventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.CreativeModeTab;
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(CreativeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin extends Screen
{
	@Shadow private static int selectedTab;

	@Shadow protected abstract void setSelectedTab(CreativeModeTab tab);

	@Unique
	private IMechanicFactory mechanic;

	@Unique
	private String lastSuccessfulChange = null;

	@Unique
	private FormattedTextFieldWidget renameHotbarField;
	@Unique
	private ButtonWidget nextButton;
	@Unique
	private ButtonWidget backupButton;
	@Unique
	private ButtonWidget previousButton;

	@Inject(method = "init", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/inventory/menu/InventoryMenu;addListener(Lnet/minecraft/inventory/menu/InventoryMenuListener;)V"))
	public void injectInit(CallbackInfo ci)
	{
		Librarian.getInstance().getEventBus().register(this);
		mechanic = Librarian.getInstance().getMechanic();

		// Offset
		int x = ((InventoryMenuScreenAccessor) this).getX() + 167;
		int y = ((InventoryMenuScreenAccessor) this).getY() + 4;

		renameHotbarField = new FormattedTextFieldWidget(2021, Minecraft.getInstance().textRenderer,
				((InventoryMenuScreenAccessor) this).getX() + 8,
				((InventoryMenuScreenAccessor) this).getY() + 6, 144, 12, "", new LiteralText(""))
		{
			@Override
			public boolean isVisible()
			{
				return tabIsHotbar(selectedTab);
			}
		};

		renameHotbarField.setFocused(false);
		renameHotbarField.setFocusUnlocked(true);
		renameHotbarField.setHasBorder(false);
		renameHotbarField.setMaxLength(65535);

		// Update the label if we are set to use the HOTBAR ItemGroup type
		// This primarily aims to emulate vanilla behavior and avoid lagspikes when opening the creative menu whilst
		// 	the current page isn't loaded
		if (tabIsHotbar(selectedTab))
		{
			renameHotbarField.setActualMessage(mechanic.createText(Librarian.getInstance().getCurrentPage().getMetadata()
					.map(HotbarPageMetadata::getName).orElse(Component.translatable("librarian.saved_toolbars.tab",
							Component.text(Librarian.getInstance().getCurrentPageNumber().toString())))));
		}
		renameHotbarField.setText(renameHotbarField.getActualMessage().getString());

		// Even though we override the isVisible method, internally ClickableWidget still uses the variable themselves
		// 	to determine other characteristics, so we still need to set them
		renameHotbarField.setVisible(tabIsHotbar(selectedTab));

		// Initialize our buttons
		nextButton = mechanic.createButton(x + 12, y, 12, 12, Component.text(">"),
				Component.text("Next page"), () -> Librarian.getInstance().nextPage());
		backupButton = mechanic.createButton(x, y, 12, 12, Component.text("✍"),
				Component.text("Make a backup of this page"), () -> Librarian.getInstance().getCurrentPage().backup());
		previousButton = mechanic.createButton(x - 12, y, 12, 12, Component.text("<"),
				Component.text("Previous page"), () -> Librarian.getInstance().previousPage());

		// Marks visibility and usability of the buttons
		nextButton.visible = tabIsHotbar(selectedTab);
		backupButton.visible = tabIsHotbar(selectedTab);
		backupButton.active = Librarian.getInstance().getCurrentPage().exists();
		previousButton.visible = tabIsHotbar(selectedTab);

		// Add the buttons
		addButton(nextButton);
		addButton(backupButton);
		addButton(previousButton);
	}

	@Inject(method = "removed", at = @At(value = "RETURN"))
	public void unregisterOnRemoval(CallbackInfo ci)
	{
		// Unregisters us as an event listener when the menu is closed
		Librarian.getInstance().getEventBus().unregister(this);
	}

	// Upon setting the current tab,
	@Inject(method = "setSelectedTab", at = @At("HEAD"))
	public void hookTabSelected(CreativeModeTab group, CallbackInfo ci)
	{
		// Determine visibility and other stuff
		boolean shouldShowElements = tabIsHotbar(group);

		if (renameHotbarField != null)
		{
			if (shouldShowElements)
			{
				// Updates the "message" which we use to display the formatted text in non-edit mode
				renameHotbarField.setActualMessage(mechanic.createText(Librarian.getInstance().getCurrentPage().getMetadata()
						.map(HotbarPageMetadata::getName).orElse(Component.translatable("librarian.saved_toolbars.tab",
								Component.text(Librarian.getInstance().getCurrentPageNumber().toString())))));
			}

			// Updates the text in the field to be the contents of the label so that it stays consistent in the
			// 	edit vs. non-edit modes
			renameHotbarField.setText(renameHotbarField.getActualMessage().getString());
			//renameHotbarField.setFocused(shouldShowElements);
			//renameHotbarField.setFocusUnlocked(!shouldShowElements);

			// See above for why we still set these
			renameHotbarField.setVisible(shouldShowElements);

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
	}

	@Inject(method = "drawForeground", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/CreativeModeTab;hasTooltips()Z", shift = At.Shift.AFTER), cancellable = true)
	public void cancelForegroundTextRendering(int mouseX, int mouseY, CallbackInfo ci)
	{
		if (tabIsHotbar(selectedTab))
		{
			ci.cancel();
		}
	}

	@Inject(method = "drawBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;render()V", shift = At.Shift.AFTER))
	public void renderOurText(float tickDelta, int mouseX, int mouseY, CallbackInfo ci)
	{
		if (tabIsHotbar(selectedTab))
		{
			renameHotbarField.render();
		}
	}

	@Inject(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/inventory/menu/PlayerInventoryScreen;mouseClicked(III)V", shift = At.Shift.BEFORE))
	public void makeTextFieldClickable(int mouseX, int mouseY, int mouseButton, CallbackInfo ci)
	{
		if (tabIsHotbar(selectedTab))
		{
			renameHotbarField.mouseClicked(mouseX, mouseY, mouseButton);
		}
	}

	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	public void workaroundTypingInRenameField(char chr, int key, CallbackInfo ci)
	{
		if (tabIsHotbar(selectedTab))
		{
			// Special keys
			renameHotbarField.keyPressed(chr, key);

			// Handle key presses if the field is focused
			if (renameHotbarField.isFocused())
			{
				final IWrappedHotbarStorage page = Librarian.getInstance().getCurrentPage();

				// Abort changes if the user presses ESC
				if (key == Keyboard.KEY_ESCAPE)
				{
					renameHotbarField.setText(lastSuccessfulChange != null ? lastSuccessfulChange :
							renameHotbarField.getActualMessage().getString());
					renameHotbarField.setFocused(false);
					renameHotbarField.setFocusUnlocked(true);
				}
				// Apply the changes if the user presses ENTER
				else if (key == Keyboard.KEY_RETURN)
				{
					final Component newName = ComponentProcessor.findBestPick(renameHotbarField.getText())
							.processComponent(renameHotbarField.getText());

					if (page.getMetadata().isPresent())
					{
						page.getMetadata().get().setName(newName);
					}
					else
					{
						page.setMetadata(HotbarPageMetadata.builder().name(newName).build());
					}
					((HotbarManager) page).save();

					renameHotbarField.setFocused(false);
					renameHotbarField.setFocusUnlocked(true);
					renameHotbarField.setActualMessage(mechanic.createText(newName));

					lastSuccessfulChange = renameHotbarField.getText();

					// Hacky fix, but oh well
					backupButton.active = Librarian.getInstance().getCurrentPage().exists();
				}

				ci.cancel();
			}
			else
			{
				super.keyPressed(chr, key);
			}
		}
	}

	/**
	 * Redirect setSelectedTab's HotbarManager instance to our own instances.
	 * Legacy Minecraft does not have a fancy method like getHotbarStorage that we can hook into, so we have to do this.
	 * @param instance	Minecraft
	 * @return			HotbarManager
	 */
	@Redirect(method = "setSelectedTab", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;hotbarManager:Lnet/minecraft/client/HotbarManager;"))
	private HotbarManager redirectHotbarManagerSST(Minecraft instance)
	{
		return (HotbarManager) Librarian.getInstance().getCurrentPage();
	}

	/**
	 * Redirect saveOrLoadToolbar's HotbarManager instance to our own instances.
	 * Legacy Minecraft does not have a fancy method like getHotbarStorage that we can hook into, so we have to do this.
	 * @param instance	Minecraft
	 * @return			HotbarManager
	 */
	@Redirect(method = "saveOrLoadToolbar", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;hotbarManager:Lnet/minecraft/client/HotbarManager;"))
	private static HotbarManager redirectHotbarManagerSOLT(Minecraft instance)
	{
		return (HotbarManager) Librarian.getInstance().getCurrentPage();
	}

	/**
	 * Legacy Minecraft handles button clicks differently, so we have to implement it ourselves.
	 * @param button	ButtonWidget
	 * @param ci		CallbackInfo
	 */
	@Inject(method = "buttonClicked", at = @At("HEAD"))
	public void injectButtonClicked(ButtonWidget button, CallbackInfo ci)
	{
		((ILButtonWidget) button).librarian$onClick();
	}

	@Inject(method = "keyPressed", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screen/inventory/menu/PlayerInventoryScreen;keyPressed(CI)V",
			shift = At.Shift.BEFORE), cancellable = true)
	public void handleNavigationKeys(char chr, int key, CallbackInfo ci)
	{
		// OSL keybinds
		final OSLAddon osl = Librarian.getInstance().getAddon(OSLAddon.class);
		if (osl.getNextKey().getKeyCode() == key)
		{
			Librarian.getInstance().nextPage();
			ci.cancel();
		}
		else if (osl.getBackupKey().getKeyCode() == key)
		{
			Librarian.getInstance().getCurrentPage().backup();
			ci.cancel();
		}
		else if (osl.getPreviousKey().getKeyCode() == key)
		{
			Librarian.getInstance().previousPage();
			ci.cancel();
		}
	}


	@WrapMethod(method = "saveOrLoadToolbar")
	private static void checkForAccidentalOverwrites(Minecraft client, int index, boolean restore, boolean save, Operation<Void> original)
	{
		if (save)
		{
			final HotbarManager storage = (HotbarManager) Librarian.getInstance().getCurrentPage();
			final Hotbar storageEntry = storage.get(index);

			if (storageEntry.isEmpty())
			{
				original.call(client, index, restore, save);
				return;
			}

			boolean confirm = false;

			for (int i = 0; i < PlayerInventory.getHotbarSize(); i++)
			{
				ItemStack inventoryStack = Objects.requireNonNull(client.player).inventory.getStack(i);
				ItemStack hotbarEntry = storageEntry.get(i);

				if (!hotbarEntry.isEmpty() && !inventoryStack.matchesItem(hotbarEntry))
				{
					confirm = true;
					break;
				}
			}

			if (confirm)
			{
				Minecraft.getInstance().openScreen(new ConfirmScreen((bl, i) ->
				{
					if (bl) original.call(client, index, restore, save);
				}, I18n.translate("librarian.messages.possible_overwrite_detected.title"), I18n.translate("librarian.messages.possible_overwrite_detected.description"), 1337));
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
			setSelectedTab(CreativeModeTab.HOTBAR);
		}
	}

	@Subscribe
	@Unique
	public void onReload(ReloadPageEvent event)
	{
		if (tabIsHotbar(selectedTab))
		{
			setSelectedTab(CreativeModeTab.HOTBAR);
		}
	}

	@Subscribe
	@Unique
	public void onCacheClear(CacheClearEvent event)
	{
		if (tabIsHotbar(selectedTab))
		{
			setSelectedTab(CreativeModeTab.HOTBAR);
		}
	}

	@Unique
	private boolean tabIsHotbar(int group)
	{
		return group == CreativeModeTab.HOTBAR.getId();
	}

	@Unique
	private boolean tabIsHotbar(CreativeModeTab group)
	{
		return group == CreativeModeTab.HOTBAR;
	}
}
