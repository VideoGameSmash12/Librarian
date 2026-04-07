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
import me.videogamesm12.librarian.api.LoadStatus;
import me.videogamesm12.librarian.api.event.AsyncPageLoadEvent;
import me.videogamesm12.librarian.api.event.CacheClearEvent;
import me.videogamesm12.librarian.api.event.NavigationEvent;
import me.videogamesm12.librarian.api.event.ReloadPageEvent;
import me.videogamesm12.librarian.util.ComponentProcessor;
import me.videogamesm12.librarian.v1_13_2.legacyfabric.FixedConfirmScreen;
import me.videogamesm12.librarian.v1_13_2.legacyfabric.Resources;
import me.videogamesm12.librarian.v1_13_2.legacyfabric.widget.FormattedTextFieldWidget;
import net.kyori.adventure.text.Component;
import net.minecraft.class_3251;
import net.minecraft.class_3297;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.slot.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.itemgroup.ItemGroup;
import net.minecraft.text.LiteralText;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Mixin(CreativeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin extends Screen
{
	@Shadow protected abstract void setSelectedTab(ItemGroup tab);

	@Shadow private static int selectedTab;

	@Unique
	private static Librarian librarian;

	@Unique
	private static int lastGroup;

	@Shadow
	private TextFieldWidget searchField;
	@Shadow
	private float scrollPosition;

	@Shadow
	private @Nullable List<Slot> slots;
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
		if (librarian == null) librarian = Librarian.getInstance();
		librarian.getEventBus().register(this);
		mechanic = librarian.getMechanic();

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
			renameHotbarField.setActualMessage(mechanic.createText(librarian.getCurrentPage().librarian$getMetadata()
					.map(HotbarPageMetadata::getName).orElse(Component.text(String.format("Saved Toolbars (%1$s)",
							librarian.getCurrentPageNumber().toString())))));
		}
		renameHotbarField.setText(renameHotbarField.getActualMessage().getString());

		// Even though we override the isVisible method, internally ClickableWidget still uses the variable themselves
		// 	to determine other characteristics, so we still need to set them
		renameHotbarField.setVisible(tabIsHotbar(selectedTab));

		// Initialize our buttons
		nextButton = mechanic.createButton(x + 12, y, 12, 12, Component.text("→"),
				Component.translatable("librarian.button.next_page.tooltip"), () -> librarian.nextPage());
		backupButton = mechanic.createButton(x, y, 12, 12, Component.text("✍"),
				Component.translatable("librarian.button.backup.tooltip"), () -> librarian.getCurrentPage().librarian$backup());
		previousButton = mechanic.createButton(x - 12, y, 12, 12, Component.text("←"),
				Component.translatable("librarian.button.previous_page.tooltip"), () -> librarian.previousPage());

		// Marks visibility and usability of the buttons
		nextButton.visible = tabIsHotbar(selectedTab);
		backupButton.visible = tabIsHotbar(selectedTab);
		backupButton.active = librarian.getCurrentPage().exists();
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
		librarian.getEventBus().unregister(this);
	}

	@Inject(method = "setSelectedTab", at = @At("HEAD"))
	public void hookTabSelected(ItemGroup group, CallbackInfo ci)
	{
		// Keep track of the last group prior for later use
		lastGroup = selectedTab;

		// Determine visibility and other stuff
		boolean shouldShowElements = tabIsHotbar(group);

		if (renameHotbarField != null)
		{
			if (shouldShowElements && librarian.getCurrentPage().librarian$getLoadStatus() == LoadStatus.LOADED)
			{
				// Updates the "message" which we use to display the formatted text in non-edit mode
				renameHotbarField.setActualMessage(mechanic.createText(librarian.getCurrentPage().librarian$getMetadata()
						.map(HotbarPageMetadata::getName).orElse(Component.text(String.format("Saved Toolbars (%1$s)",
								librarian.getCurrentPageNumber().toString())))));
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
			backupButton.active = librarian.getCurrentPage().exists();
		}
		if (previousButton != null) previousButton.visible = shouldShowElements;
	}

	@Inject(method = "setSelectedTab", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;method_18221()Lnet/minecraft/class_3251;", shift = At.Shift.AFTER), cancellable = true)
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
					((CreativeInventoryScreen.CreativeScreenHandler) ((HandledScreenAccessor) this).getScreenHandler()).field_15251.clear();

					if (renameHotbarField != null)
					{
						renameHotbarField.setActualMessage(new LiteralText(Resources.translate("librarian.messages.loading",
								page.librarian$getLocation().getName())));
						renameHotbarField.setFocused(false);
					}

					searchField.setFocusUnlocked(false);
					searchField.setFocused(false);
					searchField.setVisible(false);
					searchField.setText("");

					if (lastGroup == ItemGroup.INVENTORY.getIndex())
					{
						((HandledScreenAccessor) this).getScreenHandler().slots.clear();
						((HandledScreenAccessor) this).getScreenHandler().slots.addAll(Objects.requireNonNull(this.slots));
						this.slots = null;
					}

					scrollPosition = 0.0f;
					((CreativeInventoryScreen.CreativeScreenHandler) ((HandledScreenAccessor) this).getScreenHandler()).scrollItems(0.0f);
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

	@Inject(method = "drawForeground", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/itemgroup/ItemGroup;hasTooltip()Z", shift = At.Shift.AFTER), cancellable = true)
	public void cancelForegroundTextRendering(int mouseX, int mouseY, CallbackInfo ci)
	{
		if (tabIsHotbar(selectedTab))
		{
			ci.cancel();
		}
	}

	@Inject(method = "drawBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;method_18385(IIF)V", shift = At.Shift.AFTER))
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
				final IWrappedHotbarStorage page = librarian.getCurrentPage();

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
					((class_3251) page).method_14451();

					renameHotbarField.setFocused(false);
					renameHotbarField.setActualMessage(mechanic.createText(newName));

					lastSuccessfulChange = renameHotbarField.getText();

					// Hacky fix, but oh well
					backupButton.active = librarian.getCurrentPage().exists();
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
					librarian.reloadCurrentPage();
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
					librarian.previousPage();
					cir.setReturnValue(true);
				}
				// RIGHT ARROW
				else if (keyCode == GLFW.GLFW_KEY_RIGHT)
				{
					librarian.nextPage();
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
					librarian.advanceBy(-5);
					cir.setReturnValue(true);
				}
				// RIGHT ARROW
				else if (keyCode == GLFW.GLFW_KEY_RIGHT)
				{
					librarian.advanceBy(5);
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
					librarian.advanceBy(-10);
					cir.setReturnValue(true);
				}
				// RIGHT ARROW
				else if (keyCode == GLFW.GLFW_KEY_RIGHT)
				{
					librarian.advanceBy(10);
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
	private static void wrapHotbarSaving(MinecraftClient client, int index, boolean restore, boolean save,
										 Operation<Void> original)
	{
		if (librarian == null) librarian = Librarian.getInstance();

		final class_3251 storage = client.method_18221();
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
					client.inGameHud.setOverlayMessage(new LiteralText(Resources.translate("librarian.messages.loading",
							wrappedStorage.librarian$getLocation().getName())), false);
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
			boolean backgroundSaving = librarian.getConfig().optimizations().backgroundSaving();

			Runnable operation = () ->
			{
				final List<String> issues = new ArrayList<>();

				downgradeCheck:
				{
					if (wrappedStorage.librarian$dataVersion() > 1631)
					{
						issues.add("downgrade");
						break downgradeCheck;
					}
				}

				overwriteCheck:
				{
					final class_3297 storageEntry = storage.method_14450(index);

					if (storageEntry.isEmpty())
					{
						break overwriteCheck;
					}

					for (int i = 0; i < PlayerInventory.getHotbarSize(); i++)
					{
						ItemStack inventoryStack = Objects.requireNonNull(client.player).inventory.getInvStack(i);
						ItemStack hotbarEntry = storageEntry.get(i);

						if (!hotbarEntry.isEmpty() && !inventoryStack.equals(hotbarEntry))
						{
							issues.add("nonmatching");
							break overwriteCheck;
						}
					}
				}

				if (!issues.isEmpty())
				{
					final String title;
					final String description;

					// Only one issue found, use more specific message for that
					if (issues.size() == 1)
					{
						title = Resources.translate("librarian.messages.issues." + issues.get(0) + ".title");
						description = Resources.translate("librarian.messages.issues." + issues.get(0) + ".description");
					}
					// Otherwise, use more brief versions instead
					else
					{
						title = Resources.translate("librarian.messages.possible_loss_scenario_detected.title");

						final StringBuilder builder = new StringBuilder(Resources.translate("librarian.messages.possible_loss_scenario_detected.description"));
						issues.forEach(issue -> builder.append(Resources.translate("librarian.messages.issues." + issue + ".summary")).append("\n\n"));
						builder.append(Resources.translate("librarian.messages.possible_loss_scenario_detected.footer"));

						description = builder.toString();
					}

					// This is unbelievably bad for an optimization hack, but if I don't run setScreen() on the game
					// 	thread, the entire game crashes. If I don't account for background saving in the nested block,
					// 	then the game runs the "save" code on the game thread when it shouldn't, causing lagspikes
					client.submit(() -> client.setScreen(new FixedConfirmScreen(title, description, 1337, (value, id) ->
					{
						if (value)
						{
							if (backgroundSaving)
								librarian.queue(() -> original.call(client, index, restore, save));
							else
								original.call(client, index, restore, save);
						}
						MinecraftClient.getInstance().setScreen(null);
					})));
				}
				else
				{
					original.call(client, index, restore, save);
				}
			};

			if (backgroundSaving)
			{
				librarian.queue(operation);
			}
			else
			{
				operation.run();
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

	@Subscribe
	@Unique
	public void onPageLoad(AsyncPageLoadEvent event)
	{
		if (selectedTab == ItemGroup.field_15657.getIndex() && event.getPage().librarian$getPageNumber().equals(librarian.getCurrentPageNumber()))
		{
			MinecraftClient.getInstance().submit(() -> setSelectedTab(ItemGroup.field_15657));
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
