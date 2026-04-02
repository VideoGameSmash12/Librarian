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

package me.videogamesm12.librarian.v1_15_2.mixin;

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
import me.videogamesm12.librarian.v1_15_2.FixedConfirmScreen;
import me.videogamesm12.librarian.v1_15_2.addon.FabricAPIAddon;
import me.videogamesm12.librarian.v1_15_2.widget.FormattedTextFieldWidget;
import net.kyori.adventure.text.Component;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.options.HotbarStorage;
import net.minecraft.container.Slot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Mixin(CreativeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin extends Screen
{
	@Shadow protected abstract void setSelectedTab(ItemGroup group);

	@Shadow private static int selectedTab;

	@Unique
	private static Librarian librarian;

	@Unique
	private static int lastGroup;

	@Shadow
	private TextFieldWidget searchBox;
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

	protected CreativeInventoryScreenMixin(Text title)
	{
		super(title);
	}

	// Adds the buttons when the screen is initialized
	@Inject(method = "init", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/container/PlayerContainer;addListener(Lnet/minecraft/container/ContainerListener;)V"))
	public void injectInit(CallbackInfo ci)
	{
		if (librarian == null) librarian = Librarian.getInstance();
		librarian.getEventBus().register(this);
		mechanic = librarian.getMechanic();

		// Offset
		int x = ((ContainerScreenAccessor) this).getX() + 167;
		int y = ((ContainerScreenAccessor) this).getY() + 4;

		renameHotbarField = new FormattedTextFieldWidget(MinecraftClient.getInstance().textRenderer,
				((ContainerScreenAccessor) this).getX() + 8,
				((ContainerScreenAccessor) this).getY() + 6, 144, 12, "", new LiteralText(""))
		{
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
		renameHotbarField.setText(renameHotbarField.getActualMessage().getString());

		// Even though we override the isVisible and isActive methods, internally ClickableWidget still uses the
		// 	variable themselves to determine other characteristics, so we still need to set them
		renameHotbarField.active = tabIsHotbar(selectedTab);
		renameHotbarField.visible = tabIsHotbar(selectedTab);

		// Initialize buttons
		nextButton = mechanic.createButton(x + 12, y,12, 12, Component.text("→"), null,
				() -> librarian.nextPage());
		backupButton = mechanic.createButton(x, y, 12, 12, Component.text("✍"), null,
				() -> librarian.queue(() -> librarian.getCurrentPage().librarian$backup()));
		previousButton = mechanic.createButton(x - 12, y,12, 12, Component.text("←"), null,
				() -> librarian.previousPage());

		// Marks visibility and usability of the buttons
		nextButton.visible = tabIsHotbar(selectedTab);
		backupButton.visible = tabIsHotbar(selectedTab);
		backupButton.active = librarian.getCurrentPage().exists();
		previousButton.visible = tabIsHotbar(selectedTab);

		// Adds the "rename hotbar" text field
		addButton(renameHotbarField);

		// Adds the buttons to the screen
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
						.map(HotbarPageMetadata::getName).orElse(Component.translatable("librarian.saved_toolbars.tab",
								Component.text(librarian.getCurrentPageNumber().toString())))));
			}

			// Updates the text in the field to be the contents of the label so that it stays consistent in the
			// 	edit vs. non-edit modes
			renameHotbarField.setText(renameHotbarField.getActualMessage().getString());
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
	}

	@Inject(method = "setSelectedTab", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;getCreativeHotbarStorage()Lnet/minecraft/client/options/HotbarStorage;", shift = At.Shift.AFTER), cancellable = true)
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
					((CreativeInventoryScreen.CreativeContainer) ((ContainerScreenAccessor) this).getContainer()).itemList.clear();

					if (renameHotbarField != null)
					{
						renameHotbarField.setActualMessage(new TranslatableText("librarian.messages.loading",
								page.librarian$getLocation().getName()));
						renameHotbarField.setFocused(false);
						renameHotbarField.active = false;
					}

					searchBox.setVisible(false);
					searchBox.setFocusUnlocked(false);
					((AbstractButtonWidgetAccessor) searchBox).invokeSetFocused(false);
					searchBox.setText("");

					if (lastGroup == ItemGroup.INVENTORY.getIndex())
					{
						((ContainerScreenAccessor) this).getContainer().slots.clear();
						((ContainerScreenAccessor) this).getContainer().slots.addAll(Objects.requireNonNull(this.slots));
						this.slots = null;
					}

					scrollPosition = 0.0f;
					((CreativeInventoryScreen.CreativeContainer) ((ContainerScreenAccessor) this).getContainer()).scrollItems(0.0f);
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

	@Inject(method = "drawForeground", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemGroup;hasTooltip()Z", shift = At.Shift.AFTER), cancellable = true)
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
					((HotbarStorage) page).save();

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
			target = "Lnet/minecraft/client/gui/screen/ingame/AbstractInventoryScreen;keyPressed(III)Z",
			shift = At.Shift.BEFORE), cancellable = true)
	public void handleNavigationKeys(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir)
	{
		// Librarian-specific keybinds
		if (selectedTab == ItemGroup.HOTBAR.getIndex())
		{
			// Fabric API keybinds
			final FabricAPIAddon fabric = librarian.getAddon(FabricAPIAddon.class);
			if (fabric.getNextKey().matchesKey(keyCode, scanCode))
			{
				librarian.nextPage();
				cir.setReturnValue(true);
				return;
			}
			else if (fabric.getPreviousKey().matchesKey(keyCode, scanCode))
			{
				librarian.previousPage();
				cir.setReturnValue(true);
				return;
			}
			else if (fabric.getBackupKey().matchesKey(keyCode, scanCode))
			{
				librarian.queue(() -> librarian.getCurrentPage().librarian$backup());
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
					if (wrappedStorage.librarian$dataVersion() > SharedConstants.getGameVersion().getWorldVersion())
					{
						issues.add("downgrade");
						break downgradeCheck;
					}
				}

				overwriteCheck:
				{
					final List<ItemStack> storageEntry = storage.getSavedHotbar(index);

					if (storageEntry.isEmpty())
					{
						break overwriteCheck;
					}

					for (int i = 0; i < PlayerInventory.getHotbarSize(); i++)
					{
						ItemStack inventoryStack = Objects.requireNonNull(client.player).inventory.getInvStack(i);
						ItemStack hotbarEntry = storageEntry.get(i);

						if (!hotbarEntry.isEmpty() && inventoryStack.isItemEqual(hotbarEntry))
						{
							issues.add("nonmatching");
							break overwriteCheck;
						}
					}
				}

				if (!issues.isEmpty())
				{
					final Text title;
					final Text description;

					// Only one issue found, use more specific message for that
					if (issues.size() == 1)
					{
						title = new TranslatableText("librarian.messages.issues." + issues.get(0) + ".title");
						description = new TranslatableText("librarian.messages.issues." + issues.get(0) + ".description");
					}
					// Otherwise, use more brief versions instead
					else
					{
						title = new TranslatableText("librarian.messages.possible_loss_scenario_detected.title");
						description = new TranslatableText("librarian.messages.possible_loss_scenario_detected.description");
						issues.forEach(issue -> description.append(new TranslatableText("librarian.messages.issues." + issue + ".summary")).append("\n\n"));
						description.append(new TranslatableText("librarian.messages.possible_loss_scenario_detected.footer"));
					}

					// This is unbelievably bad for an optimization hack, but if I don't run setScreen() on the game
					// 	thread, the entire game crashes. If I don't account for background saving in the nested block,
					// 	then the game runs the "save" code on the game thread when it shouldn't, causing lagspikes
					client.execute(() -> client.openScreen(new FixedConfirmScreen(title, description, (value) ->
					{
						if (value)
						{
							if (backgroundSaving)
								librarian.queue(() -> original.call(client, index, restore, save));
							else
								original.call(client, index, restore, save);
						}
						MinecraftClient.getInstance().openScreen(null);
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
		if (selectedTab == ItemGroup.HOTBAR.getIndex())
		{
			setSelectedTab(ItemGroup.HOTBAR);
		}
	}

	@Subscribe
	@Unique
	public void onReload(ReloadPageEvent event)
	{
		if (selectedTab == ItemGroup.HOTBAR.getIndex())
		{
			setSelectedTab(ItemGroup.HOTBAR);
		}
	}

	@Subscribe
	@Unique
	public void onCacheClear(CacheClearEvent event)
	{
		if (selectedTab == ItemGroup.HOTBAR.getIndex())
		{
			setSelectedTab(ItemGroup.HOTBAR);
		}
	}

	@Subscribe
	@Unique
	public void onPageLoad(AsyncPageLoadEvent event)
	{
		if (selectedTab == ItemGroup.HOTBAR.getIndex() && event.getPage().librarian$getPageNumber().equals(librarian.getCurrentPageNumber()))
		{
			MinecraftClient.getInstance().execute(() -> setSelectedTab(ItemGroup.HOTBAR));
		}
	}

	@Unique
	private boolean tabIsHotbar(int group)
	{
		return group == ItemGroup.HOTBAR.getIndex();
	}

	@Unique
	private boolean tabIsHotbar(ItemGroup group)
	{
		return group == ItemGroup.HOTBAR;
	}
}
