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

package me.videogamesm12.librarian.v1_14_4.ornithe.mixin;

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
import me.videogamesm12.librarian.v1_14_4.ornithe.addon.OSLAddon;
import me.videogamesm12.librarian.v1_14_4.ornithe.widget.FormattedTextFieldWidget;
import net.kyori.adventure.text.Component;
import net.minecraft.client.Hotbar;
import net.minecraft.client.HotbarManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.menu.CreativeInventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.CreativeModeTab;
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
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
	@Shadow private static int selectedTab;

	protected CreativeInventoryScreenMixin(Text text)
	{
		super(text);
	}

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
			target = "Lnet/minecraft/inventory/menu/PlayerMenu;addListener(Lnet/minecraft/inventory/menu/InventoryMenuListener;)V"))
	public void injectInit(CallbackInfo ci)
	{
		Librarian.getInstance().getEventBus().register(this);
		mechanic = Librarian.getInstance().getMechanic();

		// Offset
		int x = ((InventoryMenuScreenAccessor) this).getX() + 167;
		int y = ((InventoryMenuScreenAccessor) this).getY() + 4;

		renameHotbarField = new FormattedTextFieldWidget(Minecraft.getInstance().textRenderer,
				((InventoryMenuScreenAccessor) this).getX() + 8,
				((InventoryMenuScreenAccessor) this).getY() + 6, 144, 12, "", new LiteralText(""))
		{
			@Override
			public boolean isVisible()
			{
				return tabIsHotbar(selectedTab);
			}

			@Override
			public void onRelease(double mouseX, double mouseY)
			{
				setFocused(true);
			}
		};

		renameHotbarField.setFocused(false);
		renameHotbarField.setHasBorder(false);
		renameHotbarField.setMaxLength(65535);

		// Update the label if we are set to use the HOTBAR ItemGroup type
		// This primarily aims to emulate vanilla behavior and avoid lagspikes when opening the creative menu whilst
		// 	the current page isn't loaded
		if (tabIsHotbar(selectedTab))
		{
			renameHotbarField.setActualMessage(mechanic.createText(Librarian.getInstance().getCurrentPage().librarian$getMetadata()
					.map(HotbarPageMetadata::getName).orElse(Component.translatable("librarian.saved_toolbars.tab",
							Component.text(Librarian.getInstance().getCurrentPageNumber().toString())))));
		}
		renameHotbarField.setText(renameHotbarField.getActualMessage().getString());

		// Even though we override the isVisible and isActive methods, internally ClickableWidget still uses the
		// 	variable themselves to determine other characteristics, so we still need to set them
		renameHotbarField.active = tabIsHotbar(selectedTab);
		renameHotbarField.visible = tabIsHotbar(selectedTab);

		// Initialize our buttons
		nextButton = mechanic.createButton(x + 12, y, 12, 12, Component.text("→"),
				Component.text("Next page"), () -> Librarian.getInstance().nextPage());
		backupButton = mechanic.createButton(x, y, 12, 12, Component.text("✍"),
				Component.text("Make a backup of this page"), () -> Librarian.getInstance().getCurrentPage().librarian$backup());
		previousButton = mechanic.createButton(x - 12, y, 12, 12, Component.text("←"),
				Component.text("Previous page"), () -> Librarian.getInstance().previousPage());

		// Marks visibility and usability of the buttons
		nextButton.visible = tabIsHotbar(selectedTab);
		backupButton.visible = tabIsHotbar(selectedTab);
		backupButton.active = Librarian.getInstance().getCurrentPage().exists();
		previousButton.visible = tabIsHotbar(selectedTab);

		// Adds the "rename hotbar" text field
		addButton(renameHotbarField);

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
				renameHotbarField.setActualMessage(mechanic.createText(Librarian.getInstance().getCurrentPage().librarian$getMetadata()
						.map(HotbarPageMetadata::getName).orElse(Component.translatable("librarian.saved_toolbars.tab",
								Component.text(Librarian.getInstance().getCurrentPageNumber().toString())))));
			}

			// Updates the text in the field to be the contents of the label so that it stays consistent in the
			// 	edit vs. non-edit modes
			renameHotbarField.setText(renameHotbarField.getActualMessage().getString());
			renameHotbarField.setFocused(false);

			// See above for why we still set these
			renameHotbarField.visible = shouldShowElements;
			renameHotbarField.active = shouldShowElements && renameHotbarField.isFocused();

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
							renameHotbarField.getActualMessage().getString());
					renameHotbarField.setFocused(false);
				}
				// Apply the changes if the user presses ENTER
				else if (keyCode == GLFW.GLFW_KEY_ENTER)
				{
					final Component newName = ComponentProcessor.findBestPick(renameHotbarField.getText())
							.processComponent(renameHotbarField.getText());

					if (page.librarian$getMetadata().isPresent())
					{
						page.librarian$getMetadata().get().setName(newName);
					}
					else
					{
						page.librarian$setMetadata(HotbarPageMetadata.builder().name(newName).build());
					}
					((HotbarManager) page).save();

					renameHotbarField.setFocused(false);
					renameHotbarField.setActualMessage(mechanic.createText(newName));

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
			target = "Lnet/minecraft/client/gui/screen/inventory/menu/PlayerInventoryScreen;keyPressed(III)Z",
			shift = At.Shift.BEFORE), cancellable = true)
	public void handleNavigationKeys(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir)
	{
		// OSL keybinds
		final OSLAddon osl = Librarian.getInstance().getAddon(OSLAddon.class);
		if (osl.getNextKey().matches(keyCode, scanCode))
		{
			Librarian.getInstance().nextPage();
			cir.setReturnValue(true);
			return;
		}
		else if (osl.getBackupKey().matches(keyCode, scanCode))
		{
			Librarian.getInstance().getCurrentPage().librarian$backup();
			cir.setReturnValue(true);
			return;
		}
		else if (osl.getPreviousKey().matches(keyCode, scanCode))
		{
			Librarian.getInstance().previousPage();
			cir.setReturnValue(true);
			return;
		}

		// Built-in key binds
		switch (modifiers)
		{
			// CTRL
			case GLFW.GLFW_MOD_CONTROL:
			{
				// R
				if (keyCode == GLFW.GLFW_KEY_R)
				{
					Librarian.getInstance().reloadCurrentPage();
					cir.setReturnValue(true);
				}
				break;
			}
			// ALT
			case GLFW.GLFW_MOD_ALT:
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
				break;
			}
			// SHIFT
			case GLFW.GLFW_MOD_SHIFT:
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
				break;
			}
			// ALT + SHIFT
			case GLFW.GLFW_MOD_ALT + GLFW.GLFW_MOD_SHIFT:
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
				break;
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

	@WrapMethod(method = "saveOrLoadToolbar")
	private static void checkForAccidentalOverwrites(Minecraft client, int index, boolean restore, boolean save, Operation<Void> original)
	{
		if (save)
		{
			final HotbarManager storage = client.m_7202825();
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
				Minecraft.getInstance().openScreen(new ConfirmScreen((value) ->
				{
					if (value) original.call(client, index, restore, save);
					Minecraft.getInstance().openScreen(null);
				}, new TranslatableText("librarian.messages.possible_overwrite_detected.title"), new TranslatableText("librarian.messages.possible_overwrite_detected.description")));
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
		if (selectedTab == CreativeModeTab.HOTBAR.getId())
		{
			setSelectedTab(CreativeModeTab.HOTBAR);
		}
	}

	@Subscribe
	@Unique
	public void onReload(ReloadPageEvent event)
	{
		if (selectedTab == CreativeModeTab.HOTBAR.getId())
		{
			setSelectedTab(CreativeModeTab.HOTBAR);
		}
	}

	@Subscribe
	@Unique
	public void onCacheClear(CacheClearEvent event)
	{
		if (selectedTab == CreativeModeTab.HOTBAR.getId())
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
