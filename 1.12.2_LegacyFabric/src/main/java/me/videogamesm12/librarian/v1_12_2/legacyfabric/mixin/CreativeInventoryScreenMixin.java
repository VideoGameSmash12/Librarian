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

package me.videogamesm12.librarian.v1_12_2.legacyfabric.mixin;

import com.google.common.eventbus.Subscribe;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.videogamesm12.librarian.Librarian;
import me.videogamesm12.librarian.api.HotbarPageMetadata;
import me.videogamesm12.librarian.api.IMechanicFactory;
import me.videogamesm12.librarian.api.IWrappedHotbarStorage;
import me.videogamesm12.librarian.api.event.AsyncPageLoadEvent;
import me.videogamesm12.librarian.api.event.CacheClearEvent;
import me.videogamesm12.librarian.api.event.NavigationEvent;
import me.videogamesm12.librarian.api.event.ReloadPageEvent;
import me.videogamesm12.librarian.util.ComponentProcessor;
import me.videogamesm12.librarian.v1_12_2.legacyfabric.FixedConfirmScreen;
import me.videogamesm12.librarian.v1_12_2.legacyfabric.ILButtonWidget;
import me.videogamesm12.librarian.v1_12_2.legacyfabric.addon.LegacyFabricAPIAddon;
import me.videogamesm12.librarian.v1_12_2.legacyfabric.widget.FormattedTextFieldWidget;
import net.kyori.adventure.text.Component;
import net.minecraft.class_3251;
import net.minecraft.class_3297;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.itemgroup.ItemGroup;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import org.lwjgl.input.Keyboard;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

	@Shadow
	private TextFieldWidget searchField;
	@Shadow
	private float scrollPosition;

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
		renameHotbarField.setFocusUnlocked(true);
		renameHotbarField.setHasBorder(false);
		renameHotbarField.setMaxLength(65535);

		// Update the label if we are set to use the HOTBAR ItemGroup type
		// This primarily aims to emulate vanilla behavior and avoid lagspikes when opening the creative menu whilst
		// 	the current page isn't loaded
		if (tabIsHotbar(selectedTab))
		{
			renameHotbarField.setActualMessage(mechanic.createText(librarian.getCurrentPage().librarian$getMetadata()
					.map(HotbarPageMetadata::getName).orElse(Component.translatable("librarian.saved_toolbars.tab",
							Component.text(librarian.getCurrentPageNumber().toString())))));
		}
		renameHotbarField.setText(renameHotbarField.getActualMessage().asUnformattedString());

		// Even though we override the isVisible method, internally ClickableWidget still uses the variable themselves
		// 	to determine other characteristics, so we still need to set them
		renameHotbarField.setVisible(tabIsHotbar(selectedTab));

		// Initialize our buttons
		nextButton = mechanic.createButton(x + 12, y, 12, 12, Component.text(">"),
				Component.text("Next page"), () -> librarian.nextPage());
		backupButton = mechanic.createButton(x, y, 12, 12, Component.text("✍"),
				Component.text("Make a backup of this page"), () -> librarian.queue(() -> librarian.getCurrentPage().librarian$backup()));
		previousButton = mechanic.createButton(x - 12, y, 12, 12, Component.text("<"),
				Component.text("Previous page"), () -> librarian.previousPage());

		// Marks visibility and usability of the buttons
		nextButton.visible = tabIsHotbar(selectedTab);
		backupButton.visible = tabIsHotbar(selectedTab);
		backupButton.active = librarian.getCurrentPage().exists();
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
		librarian.getEventBus().unregister(this);
	}

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
				renameHotbarField.setActualMessage(mechanic.createText(librarian.getCurrentPage().librarian$getMetadata()
						.map(HotbarPageMetadata::getName).orElse(Component.translatable("librarian.saved_toolbars.tab",
								Component.text(librarian.getCurrentPageNumber().toString())))));
			}

			// Updates the text in the field to be the contents of the label so that it stays consistent in the
			// 	edit vs. non-edit modes
			renameHotbarField.setText(renameHotbarField.getActualMessage().asUnformattedString());

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

	@Inject(method = "setSelectedTab", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/collection/DefaultedList;clear()V"), cancellable = true)
	public void insertEmptyLoadingScreen(ItemGroup group, CallbackInfo ci)
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
						renameHotbarField.setActualMessage(new TranslatableText("librarian.messages.loading",
								page.librarian$getLocation().getName()));
						renameHotbarField.setFocused(false);
					}

					searchField.setFocusUnlocked(true);
					searchField.setFocused(false);
					searchField.setVisible(false);
					searchField.setText("");

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

	@Inject(method = "drawForeground", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/itemgroup/ItemGroup;hasTooltip()Z", shift = At.Shift.AFTER), cancellable = true)
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

	@Inject(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/InventoryScreen;mouseClicked(III)V", shift = At.Shift.BEFORE))
	public void makeTextFieldClickable(int mouseX, int mouseY, int mouseButton, CallbackInfo ci)
	{
		if (tabIsHotbar(selectedTab))
		{
			renameHotbarField.method_920(mouseX, mouseY, mouseButton);
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
				final IWrappedHotbarStorage page = librarian.getCurrentPage();

				// Abort changes if the user presses ESC
				if (key == Keyboard.KEY_ESCAPE)
				{
					renameHotbarField.setText(lastSuccessfulChange != null ? lastSuccessfulChange :
							renameHotbarField.getActualMessage().asUnformattedString());
					renameHotbarField.setFocused(false);
					renameHotbarField.setFocusUnlocked(true);
				}
				// Apply the changes if the user presses ENTER
				else if (key == Keyboard.KEY_RETURN)
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
					renameHotbarField.setFocusUnlocked(true);
					renameHotbarField.setActualMessage(mechanic.createText(newName));

					lastSuccessfulChange = renameHotbarField.getText();

					// Hacky fix, but oh well
					backupButton.active = librarian.getCurrentPage().exists();
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
	 * Redirect setSelectedTab's HotbarStorage instance to our own instances.
	 * Legacy Minecraft does not have a fancy method like getHotbarStorage that we can hook into, so we have to do this.
	 * @param instance	MinecraftClient
	 * @return			HotbarStorage
	 */
	@Redirect(method = "setSelectedTab", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;field_15872:Lnet/minecraft/class_3251;", opcode = Opcodes.GETFIELD))
	private class_3251 redirectHotbarManagerSST(MinecraftClient instance)
	{
		return (class_3251) librarian.getCurrentPage();
	}

	/**
	 * Redirect saveOrLoadToolbar's HotbarStorage instance to our own instances.
	 * Legacy Minecraft does not have a fancy method like getHotbarStorage that we can hook into, so we have to do this.
	 * @param instance	MinecraftClient
	 * @return			HotbarStorage
	 */
	@Redirect(method = "method_14550", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;field_15872:Lnet/minecraft/class_3251;", opcode = Opcodes.GETFIELD))
	private static class_3251 redirectHotbarManagerSOLT(MinecraftClient instance)
	{
		return (class_3251) librarian.getCurrentPage();
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
			target = "Lnet/minecraft/client/gui/screen/ingame/InventoryScreen;keyPressed(CI)V",
			shift = At.Shift.BEFORE), cancellable = true)
	public void handleNavigationKeys(char chr, int key, CallbackInfo ci)
	{
		// OSL keybinds
		final LegacyFabricAPIAddon osl = librarian.getAddon(LegacyFabricAPIAddon.class);
		if (osl.getNextKey().getCode() == key)
		{
			librarian.nextPage();
			ci.cancel();
		}
		else if (osl.getBackupKey().getCode() == key)
		{
			librarian.queue(() -> librarian.getCurrentPage().librarian$backup());
			ci.cancel();
		}
		else if (osl.getPreviousKey().getCode() == key)
		{
			librarian.previousPage();
			ci.cancel();
		}
	}

	@WrapMethod(method = "method_14550")
	private static void wrapHotbarSaving(MinecraftClient client, int index, boolean restore, boolean save,
										 Operation<Void> original)
	{
		if (librarian == null) librarian = Librarian.getInstance();

		final class_3251 storage = (class_3251) librarian.getCurrentPage();
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
					client.inGameHud.setOverlayMessage(new TranslatableText("librarian.messages.loading",
							wrappedStorage.librarian$getLocation().getName()), false);
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
					if (wrappedStorage.librarian$dataVersion() > 1343)
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
						title = I18n.translate("librarian.messages.issues." + issues.get(0) + ".title");
						description = I18n.translate("librarian.messages.issues." + issues.get(0) + ".description");
					}
					// Otherwise, use more brief versions instead
					else
					{
						title = I18n.translate("librarian.messages.possible_loss_scenario_detected.title");

						final StringBuilder builder = new StringBuilder(I18n.translate("librarian.messages.possible_loss_scenario_detected.description"));
						issues.forEach(issue -> builder.append(I18n.translate("librarian.messages.issues." + issue + ".summary")).append("\n\n"));
						builder.append(I18n.translate("librarian.messages.possible_loss_scenario_detected.footer"));

						description = builder.toString();
					}

					// This is unbelievably bad for an optimization hack, but if I don't run setScreen() on the game
					// 	thread, the entire game crashes. If I don't account for background saving in the nested block,
					// 	then the game runs the "save" code on the game thread when it shouldn't, causing lagspikes
					client.execute(() ->
					{
						client.setScreen(new FixedConfirmScreen((value, id) ->
						{
							if (value)
							{
								if (backgroundSaving)
									librarian.queue(() -> original.call(client, index, restore, save));
								else
									original.call(client, index, restore, save);
							}
							MinecraftClient.getInstance().setScreen(null);
						}, title, description, 1337));
						return null;
					});
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
		if (tabIsHotbar(selectedTab))
		{
			setSelectedTab(ItemGroup.field_15657);
		}
	}

	@Subscribe
	@Unique
	public void onReload(ReloadPageEvent event)
	{
		if (tabIsHotbar(selectedTab))
		{
			setSelectedTab(ItemGroup.field_15657);
		}
	}

	@Subscribe
	@Unique
	public void onCacheClear(CacheClearEvent event)
	{
		if (tabIsHotbar(selectedTab))
		{
			setSelectedTab(ItemGroup.field_15657);
		}
	}

	@Subscribe
	@Unique
	public void onPageLoad(AsyncPageLoadEvent event)
	{
		if (tabIsHotbar(selectedTab) && event.getPage().librarian$getPageNumber().equals(librarian.getCurrentPageNumber()))
		{
			MinecraftClient.getInstance().execute(() ->
			{
				setSelectedTab(ItemGroup.field_15657);
				return null;
			});
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
