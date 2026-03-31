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
import me.videogamesm12.librarian.api.LoadStatus;
import me.videogamesm12.librarian.api.event.AsyncPageLoadEvent;
import me.videogamesm12.librarian.api.event.CacheClearEvent;
import me.videogamesm12.librarian.api.event.NavigationEvent;
import me.videogamesm12.librarian.api.event.ReloadPageEvent;
import me.videogamesm12.librarian.util.ComponentProcessor;
import me.videogamesm12.librarian.v26_1.addon.FabricAPIAddon;
import net.fabricmc.fabric.impl.client.creativetab.FabricCreativeGuiComponents;
import net.kyori.adventure.key.Key;
import net.minecraft.SharedConstants;
import net.minecraft.client.HotbarManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin extends Screen
{
	@Shadow protected abstract void selectTab(CreativeModeTab group);

	@Unique
	private static Librarian librarian;

	@Shadow
	private EditBox searchBox;
	@Shadow
	private float scrollOffs;

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
		if (librarian == null) librarian = Librarian.getInstance();
		librarian.getEventBus().register(this);
		mechanic = librarian.getMechanic();

		// Offset
		int x = ((AbstractContainerScreenAccessor) this).getLeftPos() + 167;
		int y = ((AbstractContainerScreenAccessor) this).getTopPos() + 4;

		// Adds "rename hotbar" text field
		renameHotbarField = new EditBox(font, ((AbstractContainerScreenAccessor) this).getLeftPos() + 8,
				((AbstractContainerScreenAccessor) this).getTopPos() + 6, 144, 12, Component.empty())
		{
			@Override
			public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a)
			{
				if (isFocused())
				{
					super.extractWidgetRenderState(graphics, mouseX, mouseY, a);
				}
				else
				{
					graphics.text(font, getMessage(), this.getX(), this.getY(), -12566464, false);
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
				return tabIsHotbar(selectedTab);
			}

			@Override
			public boolean mouseReleased(MouseButtonEvent event)
			{
				// I don't know why, but onClick or mouseClicked don't fire anymore after you switch tabs or rename the
				// 	hotbar page. Why? Who knows! It wasn't like that in <=1.21.11 though!

				if (!isFocused())
				{
					setFocused(true);
					return true;
				}

				return super.mouseReleased(event);
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
			renameHotbarField.setMessage(mechanic.createText(librarian.getCurrentPage().librarian$getMetadata()
					.map(HotbarPageMetadata::getName).orElse(net.kyori.adventure.text.Component.translatable("librarian.saved_hotbars.tab",
							net.kyori.adventure.text.Component.text(librarian.getCurrentPageNumber().toString())))));
		}

		// Even though we override the isVisible and isActive methods, internally ClickableWidget still uses the
		// 	variable themselves to determine other characteristics, so we still need to set them
		renameHotbarField.active = tabIsHotbar(selectedTab);
		renameHotbarField.visible = tabIsHotbar(selectedTab);

		// Initialize buttons
		nextButton = mechanic.createButton(x + 12, y,12, 12, net.kyori.adventure.text.Component.text("→"),
				net.kyori.adventure.text.Component.text("Next page"), () -> librarian.nextPage());
		backupButton = mechanic.createButton(x, y,12, 12, net.kyori.adventure.text.Component.text("\uD83D\uDCBE")
						.font(Key.key("librarian", "default")), net.kyori.adventure.text.Component.text("Make a backup of this page"),
				() -> librarian.queue(() -> librarian.getCurrentPage().librarian$backup()));
		previousButton = mechanic.createButton(x - 12, y,12, 12, net.kyori.adventure.text.Component.text("←"),
				net.kyori.adventure.text.Component.text("Previous page"), () -> librarian.previousPage());

		// Marks visibility and usability of the buttons
		nextButton.visible = tabIsHotbar(selectedTab);
		backupButton.visible = tabIsHotbar(selectedTab);
		backupButton.active = librarian.getCurrentPage().exists();
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
		librarian.getEventBus().unregister(this);
	}

	@Inject(method = "selectTab", at = @At("HEAD"))
	public void hookTabSelected(CreativeModeTab tab, CallbackInfo ci)
	{
		boolean shouldShowElements = tabIsHotbar(tab);

		// Determine visibility and other stuff
		if (renameHotbarField != null)
		{
			if (shouldShowElements && librarian.getCurrentPage().librarian$getLoadStatus() == LoadStatus.LOADED)
			{
				// Updates the "message" which we use to display the formatted text in non-edit mode
				renameHotbarField.setMessage(mechanic.createText(librarian.getCurrentPage().librarian$getMetadata()
						.map(HotbarPageMetadata::getName).orElse(net.kyori.adventure.text.Component.translatable("librarian.saved_hotbars.tab",
								net.kyori.adventure.text.Component.text(librarian.getCurrentPageNumber().toString())))));
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
			backupButton.active = librarian.getCurrentPage().exists();
		}
		if (previousButton != null) previousButton.visible = shouldShowElements;

		// Avoid overlaps - https://github.com/FabricMC/fabric/pull/2742
		((ScreenAccessor) this).getRenderables().stream().filter(entry ->
				entry instanceof FabricCreativeGuiComponents.CreativeModeTabButton).forEach(button ->
				((Button) button).visible = !shouldShowElements);
	}

	@Inject(method = "selectTab", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getHotbarManager()Lnet/minecraft/client/HotbarManager;", shift = At.Shift.AFTER), cancellable = true)
	private void insertEmptyLoadingScreen(CreativeModeTab group, CallbackInfo ci)
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
					((CreativeModeInventoryScreen.ItemPickerMenu) ((AbstractContainerScreenAccessor) this).getMenu()).items.clear();

					if (renameHotbarField != null)
					{
						renameHotbarField.setMessage(net.minecraft.network.chat.Component.translatable("librarian.messages.loading",
								page.librarian$getLocation().getName()));
						renameHotbarField.setFocused(false);
						renameHotbarField.active = false;
					}

					searchBox.setVisible(false);
					searchBox.setCanLoseFocus(true);
					searchBox.setFocused(false);
					searchBox.setValue("");

					scrollOffs = 0.0f;
					((CreativeModeInventoryScreen.ItemPickerMenu) ((AbstractContainerScreenAccessor) this).getMenu()).scrollTo(0.0f);
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

	@Inject(method = "extractLabels", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/CreativeModeTab;showTitle()Z", shift = At.Shift.AFTER), cancellable = true)
	public void cancelForegroundTextRendering(GuiGraphicsExtractor graphics, int xm, int ym, CallbackInfo ci)
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
				final IWrappedHotbarStorage page = librarian.getCurrentPage();

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
			target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;keyPressed(Lnet/minecraft/client/input/KeyEvent;)Z",
			shift = At.Shift.BEFORE), cancellable = true)
	public void handleNavigationKeys(KeyEvent input, CallbackInfoReturnable<Boolean> cir)
	{
		// Librarian-specific keybinds
		if (tabIsHotbar(selectedTab))
		{
			// Fabric API keybinds
			final FabricAPIAddon fabric = librarian.getAddon(FabricAPIAddon.class);
			if (fabric.getNextKey().matches(input))
			{
				librarian.nextPage();
				cir.setReturnValue(true);
				return;
			}
			else if (fabric.getPreviousKey().matches(input))
			{
				librarian.previousPage();
				cir.setReturnValue(true);
				return;
			}
			else if (fabric.getBackupKey().matches(input))
			{
				librarian.getCurrentPage().librarian$backup();
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
						librarian.reloadCurrentPage();
						cir.setReturnValue(true);
					}
				}
				// ALT
				case GLFW.GLFW_MOD_ALT ->
				{
					// LEFT ARROW
					if (input.key() == GLFW.GLFW_KEY_LEFT)
					{
						librarian.previousPage();
						cir.setReturnValue(true);
					}
					// RIGHT ARROW
					else if (input.key() == GLFW.GLFW_KEY_RIGHT)
					{
						librarian.nextPage();
						cir.setReturnValue(true);
					}
				}
				// SHIFT
				case GLFW.GLFW_MOD_SHIFT ->
				{
					// LEFT ARROW
					if (input.key() == GLFW.GLFW_KEY_LEFT)
					{
						librarian.advanceBy(-5);
						cir.setReturnValue(true);
					}
					// RIGHT ARROW
					else if (input.key() == GLFW.GLFW_KEY_RIGHT)
					{
						librarian.advanceBy(5);
						cir.setReturnValue(true);
					}
				}
				// ALT + SHIFT
				case GLFW.GLFW_MOD_ALT + GLFW.GLFW_MOD_SHIFT ->
				{
					// LEFT ARROW
					if (input.key() == GLFW.GLFW_KEY_LEFT)
					{
						librarian.advanceBy(-10);
						cir.setReturnValue(true);
					}
					// RIGHT ARROW
					else if (input.key() == GLFW.GLFW_KEY_RIGHT)
					{
						librarian.advanceBy(10);
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
	private static void wrapHotbarSaving(Minecraft minecraft, int index, boolean isLoadPressed, boolean isSavePressed,
										 Operation<Void> original)
	{
		if (librarian == null) librarian = Librarian.getInstance();

		final HotbarManager storage = minecraft.getHotbarManager();
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
					Objects.requireNonNull(minecraft.player).sendOverlayMessage(net.minecraft.network.chat.Component.translatable("librarian.messages.loading",
							wrappedStorage.librarian$getLocation().getName()));
					return;
				}
				default:
				{
					break;
				}
			}
		}

		if (isSavePressed)
		{
			boolean backgroundSaving = librarian.getConfig().optimizations().backgroundSaving();

			Runnable operation = () ->
			{
				final HotbarManager str = minecraft.getHotbarManager();
				final IWrappedHotbarStorage wrapped = (IWrappedHotbarStorage) str;
				final List<String> issues = new ArrayList<>();

				downgradeCheck:
				{
					if (wrapped.librarian$dataVersion() > SharedConstants.getCurrentVersion().dataVersion().version())
					{
						issues.add("downgrade");
						break downgradeCheck;
					}
				}

				overwriteCheck:
				{
					final List<ItemStack> storageEntry = str.get(index).load(Objects.requireNonNull(minecraft.level)
							.registryAccess());

					if (storageEntry.isEmpty())
					{
						break overwriteCheck;
					}

					for (int i = 0; i < Inventory.getSelectionSize(); i++)
					{
						ItemStack inventoryStack = Objects.requireNonNull(minecraft.player).getInventory().getItem(i);
						ItemStack hotbarEntry = storageEntry.get(i);

						if (!hotbarEntry.isEmpty() && !ItemStack.isSameItemSameComponents(inventoryStack, hotbarEntry))
						{
							issues.add("nonmatching");
							break overwriteCheck;
						}
					}
				}

				if (!issues.isEmpty())
				{
					final MutableComponent title;
					final MutableComponent description;

					// Only one issue found, use more specific message for that
					if (issues.size() == 1)
					{
						title = Component.translatable("librarian.messages.issues." + issues.getFirst() + ".title");
						description = Component.translatable("librarian.messages.issues." + issues.getFirst() + ".description");
					}
					// Otherwise, use more brief versions instead
					else
					{
						title = Component.translatable("librarian.messages.possible_loss_scenario_detected.title");
						description = Component.translatable("librarian.messages.possible_loss_scenario_detected.description");
						issues.forEach(issue -> description.append(Component.translatable("librarian.messages.issues." + issue + ".summary")).append("\n\n"));
						description.append(Component.translatable("librarian.messages.possible_loss_scenario_detected.footer"));
					}

					// This is unbelievably bad for an optimization hack, but if I don't run setScreen() on the game
					// 	thread, the entire game crashes. If I don't account for background saving in the nested block,
					// 	then the game runs the "save" code on the game thread when it shouldn't, causing lagspikes
					minecraft.execute(() -> minecraft.setScreen(new ConfirmScreen((value) ->
					{
						if (value)
						{
							if (backgroundSaving)
								librarian.queue(() -> original.call(minecraft, index, isLoadPressed, isSavePressed));
							else
								original.call(minecraft, index, isLoadPressed, isSavePressed);
						}
						minecraft.setScreen(null);
					}, title, description)));
				}
				else
				{
					original.call(minecraft, index, isLoadPressed, isSavePressed);
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
			original.call(minecraft, index, isLoadPressed, isSavePressed);
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

	@Subscribe
	@Unique
	public void onPageLoad(AsyncPageLoadEvent event)
	{
		if (tabIsHotbar(selectedTab) && event.getPage().librarian$getPageNumber().equals(librarian.getCurrentPageNumber()))
		{
			Minecraft.getInstance().execute(() -> selectTab(BuiltInRegistries.CREATIVE_MODE_TAB.getValue(CreativeModeTabs.HOTBAR)));
		}
	}


	@Unique
	private boolean tabIsHotbar(CreativeModeTab group)
	{
		return group.getType() == CreativeModeTab.Type.HOTBAR;
	}
}
