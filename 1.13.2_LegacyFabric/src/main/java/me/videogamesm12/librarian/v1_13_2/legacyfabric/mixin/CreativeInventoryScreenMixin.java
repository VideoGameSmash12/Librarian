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

package me.videogamesm12.librarian.v1_13_2.legacyfabric.mixin;

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
import me.videogamesm12.librarian.v1_13_2.legacyfabric.widget.FormattedTextFieldWidget;
import net.kyori.adventure.text.Component;
import net.minecraft.class_3251;
import net.minecraft.class_3297;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.itemgroup.ItemGroup;
import net.minecraft.text.LiteralText;
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

	@Shadow protected abstract void setSelectedTab(ItemGroup tab);

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
			target = "Lnet/minecraft/screen/ScreenHandler;addListener(Lnet/minecraft/screen/ScreenHandlerListener;)V"))
	public void injectInit(CallbackInfo ci)
	{
		Librarian.getInstance().getEventBus().register(this);
		mechanic = Librarian.getInstance().getMechanic();

		// Offset
		int x = ((HandledScreenAccessor) this).getX() + 167;
		int y = ((HandledScreenAccessor) this).getY() + 4;

		renameHotbarField = new FormattedTextFieldWidget(2021, MinecraftClient.getInstance().textRenderer,
				((HandledScreenAccessor) this).getX() + 8,
				((HandledScreenAccessor) this).getY() + 6, 144, 12, "", new LiteralText(""))
		{
			@Override
			public boolean isVisible()
			{
				return tabIsHotbar(selectedTab);
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
			renameHotbarField.setActualMessage(mechanic.createText(Librarian.getInstance().getCurrentPage().getMetadata()
					.map(HotbarPageMetadata::getName).orElse(Component.text(String.format("Saved Toolbars (%1$s)",
							Librarian.getInstance().getCurrentPageNumber().toString())))));
		}
		renameHotbarField.setText(renameHotbarField.getActualMessage().getString());

		// Even though we override the isVisible method, internally ClickableWidget still uses the variable themselves
		// 	to determine other characteristics, so we still need to set them
		renameHotbarField.setVisible(tabIsHotbar(selectedTab));

		// Initialize our buttons
		nextButton = mechanic.createButton(x + 12, y, 12, 12, Component.text("→"),
				Component.text("Next page"), () -> Librarian.getInstance().nextPage());
		backupButton = mechanic.createButton(x, y, 12, 12, Component.text("✍"),
				Component.text("Make a backup of this page"), () -> Librarian.getInstance().getCurrentPage().backup());
		previousButton = mechanic.createButton(x - 12, y, 12, 12, Component.text("←"),
				Component.text("Previous page"), () -> Librarian.getInstance().previousPage());

		// Marks visibility and usability of the buttons
		nextButton.visible = tabIsHotbar(selectedTab);
		backupButton.visible = tabIsHotbar(selectedTab);
		backupButton.active = Librarian.getInstance().getCurrentPage().exists();
		previousButton.visible = tabIsHotbar(selectedTab);

		// Adds the "rename hotbar" text field
		this.field_20307.add(renameHotbarField);

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
	public void hookTabSelected(ItemGroup group, CallbackInfo ci)
	{
		// Determine visibility and other stuff
		boolean shouldShowElements = tabIsHotbar(group);

		if (renameHotbarField != null)
		{
			if (shouldShowElements)
			{
				// Updates the "message" which we use to display the formatted text in non-edit mode
				renameHotbarField.setActualMessage(mechanic.createText(Librarian.getInstance().getCurrentPage().getMetadata()
						.map(HotbarPageMetadata::getName).orElse(Component.text(String.format("Saved Toolbars (%1$s)",
								Librarian.getInstance().getCurrentPageNumber().toString())))));
			}

			// Updates the text in the field to be the contents of the label so that it stays consistent in the
			// 	edit vs. non-edit modes
			renameHotbarField.setText(renameHotbarField.getActualMessage().getString());
			renameHotbarField.setFocused(false);

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

	@Inject(method = "drawForeground", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/itemgroup/ItemGroup;hasTooltip()Z", shift = At.Shift.AFTER), cancellable = true)
	public void cancelForegroundTextRendering(int mouseX, int mouseY, CallbackInfo ci)
	{
		if (tabIsHotbar(selectedTab))
		{
			ci.cancel();
		}
	}

	@Inject(method = "drawBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;method_18385(IIF)V", shift = At.Shift.AFTER), cancellable = true)
	public void renderOurText(float tickDelta, int mouseX, int mouseY, CallbackInfo ci)
	{
		if (tabIsHotbar(selectedTab))
		{
			renameHotbarField.method_18385(mouseX, mouseY, tickDelta);
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

					if (page.getMetadata().isPresent())
					{
						page.getMetadata().get().setName(newName);
					}
					else
					{
						page.setMetadata(HotbarPageMetadata.builder().name(newName).build());
					}
					((class_3251) page).method_14451();

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
			target = "Lnet/minecraft/client/gui/screen/ingame/InventoryScreen;keyPressed(III)Z",
			shift = At.Shift.BEFORE), cancellable = true)
	public void handleNavigationKeys(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir)
	{
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

	@WrapMethod(method = "method_14550")
	private static void checkForAccidentalOverwrites(MinecraftClient client, int index, boolean restore, boolean save, Operation<Void> original)
	{
		if (save)
		{
			final class_3251 storage = client.method_18221();
			final class_3297 storageEntry = storage.method_14450(index);

			if (storageEntry.isEmpty())
			{
				original.call(client, index, restore, save);
				return;
			}

			boolean confirm = false;

			for (int i = 0; i < PlayerInventory.getHotbarSize(); i++)
			{
				ItemStack inventoryStack = Objects.requireNonNull(client.player).inventory.getInvStack(i);
				ItemStack hotbarEntry = storageEntry.get(i);

				if (!hotbarEntry.isEmpty() && !inventoryStack.equals(hotbarEntry))
				{
					confirm = true;
					break;
				}
			}

			if (confirm)
			{
				MinecraftClient.getInstance().setScreen(new ConfirmScreen((value, id) ->
				{
					if (value) original.call(client, index, restore, save);
					MinecraftClient.getInstance().setScreen(null);
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
		if (selectedTab == ItemGroup.field_15657.getIndex())
		{
			setSelectedTab(ItemGroup.field_15657);
		}
	}

	@Subscribe
	@Unique
	public void onReload(ReloadPageEvent event)
	{
		if (selectedTab == ItemGroup.field_15657.getIndex())
		{
			setSelectedTab(ItemGroup.field_15657);
		}
	}

	@Subscribe
	@Unique
	public void onCacheClear(CacheClearEvent event)
	{
		if (selectedTab == ItemGroup.field_15657.getIndex())
		{
			setSelectedTab(ItemGroup.field_15657);
		}
	}

	@Unique
	private boolean tabIsHotbar(int group)
	{
		return group == ItemGroup.field_15657.getIndex();
	}

	@Unique
	private boolean tabIsHotbar(ItemGroup group)
	{
		return group == ItemGroup.field_15657;
	}
}
