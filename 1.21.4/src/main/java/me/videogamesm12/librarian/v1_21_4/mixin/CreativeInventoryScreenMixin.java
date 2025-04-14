package me.videogamesm12.librarian.v1_21_4.mixin;

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
import me.videogamesm12.librarian.v1_21_4.addon.FabricAPIAddon;
import net.fabricmc.fabric.impl.client.itemgroup.FabricCreativeGuiComponents;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.option.HotbarStorage;
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
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Objects;

@Mixin(CreativeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin extends Screen
{
	@Shadow protected abstract void setSelectedTab(ItemGroup group);

	@Unique
	private static boolean inEditMode = false;

	@Unique
	private IMechanicFactory mechanic;

	@Unique
	private Text label = Text.translatable("librarian.saved_hotbars.tab",
			Librarian.getInstance().getCurrentPageNumber());
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
		Librarian.getInstance().getCurrentPage().getMetadata().map(HotbarPageMetadata::getName).ifPresentOrElse(
				name -> label = mechanic.createText(name),
				() -> label = Text.translatable("librarian.saved_hotbars.tab",
						Librarian.getInstance().getCurrentPageNumber()));
		renameHotbarField = new TextFieldWidget(textRenderer, ((HandledScreenAccessor) this).getX() + 8,
				((HandledScreenAccessor) this).getY() + 6, 144, 12, label)
		{
			@Override
			public boolean isVisible()
			{
				return getSelectedTab().getType() == ItemGroup.Type.HOTBAR && inEditMode;
			}

			@Override
			public void onRelease(double mouseX, double mouseY)
			{
				setFocused(true);
				inEditMode = true;
			}
		};
		renameHotbarField.setFocused(inEditMode);
		renameHotbarField.setText(label.getString());
		renameHotbarField.setDrawsBackground(false);
		renameHotbarField.setMaxLength(65535);
		renameHotbarField.active = getSelectedTab().getType() == ItemGroup.Type.HOTBAR;

		// Initialize buttons
		nextButton = mechanic.createButton(x + 12, y,12, 12, Component.text("→"),
				Component.text("Next page"), () -> Librarian.getInstance().nextPage());
		backupButton = mechanic.createButton(x, y,12, 12, Component.text("\uD83D\uDCBE")
				.font(Key.key("librarian", "default")), Component.text("Make a backup of this page"),
				() -> Librarian.getInstance().getCurrentPage().backup());
		previousButton = mechanic.createButton(x - 12, y,12, 12, Component.text("←"),
				Component.text("Previous page"), () -> Librarian.getInstance().previousPage());

		// Marks visibility and usability of the buttons
		nextButton.visible = getSelectedTab().getType() == ItemGroup.Type.HOTBAR;
		backupButton.visible = getSelectedTab().getType() == ItemGroup.Type.HOTBAR;
		backupButton.active = Librarian.getInstance().getCurrentPage().exists();
		previousButton.visible = getSelectedTab().getType() == ItemGroup.Type.HOTBAR;

		// Adds the "rename hotbar" text field
		addDrawableChild(renameHotbarField);

		// Adds the buttons to the screen
		addDrawableChild(nextButton);
		addDrawableChild(backupButton);
		addDrawableChild(previousButton);
	}

	@Inject(method = "removed", at = @At(value = "RETURN"))
	public void hookRemoved(CallbackInfo ci)
	{
		Librarian.getInstance().getEventBus().unregister(this);
	}

	// Upon setting the current tab,
	@Inject(method = "setSelectedTab", at = @At("HEAD"))
	public void hookTabSelected(ItemGroup group, CallbackInfo ci)
	{
		// Determine visibility and other stuff
		if (renameHotbarField != null)
		{
			renameHotbarField.active = group.getType() == ItemGroup.Type.HOTBAR;
			Librarian.getInstance().getCurrentPage().getMetadata().map(HotbarPageMetadata::getName).ifPresentOrElse(
					name -> label = mechanic.createText(name),
					() -> label = Text.translatable("librarian.saved_hotbars.tab",
							Librarian.getInstance().getCurrentPageNumber()));
			renameHotbarField.setText(label.getString());
			renameHotbarField.setFocused(false);
			inEditMode = false;
			lastSuccessfulChange = null;
		}
		if (nextButton != null) nextButton.visible = group.getType() == ItemGroup.Type.HOTBAR;
		if (backupButton != null)
		{
			backupButton.visible = group.getType() == ItemGroup.Type.HOTBAR;
			backupButton.active = Librarian.getInstance().getCurrentPage().exists();
		}
		if (previousButton != null) previousButton.visible = group.getType() == ItemGroup.Type.HOTBAR;

		// Avoid overlaps - https://github.com/FabricMC/fabric/pull/2742
		((ScreenAccessor) this).getDrawables().stream().filter(entry ->
				entry instanceof FabricCreativeGuiComponents.ItemGroupButtonWidget).forEach(button ->
				((ButtonWidget) button).visible = group != Registries.ITEM_GROUP.get(ItemGroups.HOTBAR));
	}

	@ModifyArg(method = "drawForeground", at = @At(value = "INVOKE", target ="Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIIZ)I", ordinal = 0))
	private Text getLabel(Text text)
	{
		return getSelectedTab().getType() == ItemGroup.Type.HOTBAR ? inEditMode ? Text.empty() : label : text;
	}

	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	public void injectKeyPressedHead(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir)
	{
		if (getSelectedTab().getType() == ItemGroup.Type.HOTBAR)
		{
			// Special key was pressed
			if (renameHotbarField.keyPressed(keyCode, scanCode, modifiers))
			{
				//cir.setReturnValue(true);
				Librarian.getLogger().info("Debug! Special key press detected - Keycode {}, Scancode {}, Modifiers {}", keyCode, scanCode, modifiers);
			}

			if (renameHotbarField.isFocused())
			{
				final IWrappedHotbarStorage page = Librarian.getInstance().getCurrentPage();

				if (keyCode == GLFW.GLFW_KEY_ESCAPE)
				{
					Librarian.getLogger().info("Debug! Code that would reset the custom name goes here!");
					renameHotbarField.setText(lastSuccessfulChange != null ? lastSuccessfulChange : label.getString());
					renameHotbarField.setFocused(false);
					inEditMode = false;
				}
				else if (keyCode == GLFW.GLFW_KEY_ENTER)
				{
					Librarian.getLogger().info("Debug! Code that would set the custom name goes here!");
					final Component newName = ComponentProcessor.findBestPick(renameHotbarField.getText())
							.processComponent(renameHotbarField.getText());

					page.getMetadata().ifPresentOrElse(metadata ->
							metadata.setName(newName), () ->
							page.setMetadata(HotbarPageMetadata.builder().name(newName).build()));
					((HotbarStorage) page).save();

					renameHotbarField.setFocused(false);
					renameHotbarField.setMessage(mechanic.createText(newName));
					lastSuccessfulChange = renameHotbarField.getText();
					inEditMode = false;
					label = renameHotbarField.getMessage();

					// Hacky fix, but oh well
					backupButton.active = Librarian.getInstance().getCurrentPage().exists();
				}

				cir.setReturnValue(true);
			}
			else
			{
				super.keyPressed(keyCode, scanCode, modifiers);
			}

			Librarian.getLogger().info("Debug! Key press detected - {}", keyCode);
		}
	}

	@Inject(method = "keyPressed", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;keyPressed(III)Z",
			shift = At.Shift.BEFORE), cancellable = true)
	public void inject(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir)
	{
		// Librarian-specific keybinds
		if (getSelectedTab().getType() == ItemGroup.Type.HOTBAR)
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
				Librarian.getInstance().getCurrentPage().backup();
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
					// R
					// Code is commented out as it registers a rogue "R" press when done this way and that looks stupid
					/*else if (keyCode == GLFW.GLFW_KEY_R)
					{
						ignoreTypedCharacter = true;
						renameHotbarField.setFocused(true);
						inEditMode = true;
						cir.setReturnValue(true);
					}*/
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
		if (getSelectedTab().getType() == ItemGroup.Type.HOTBAR)
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
	private static void checkForAccidentalOverwrites(MinecraftClient client, int index, boolean restore, boolean save,
													 Operation<Void> original)
	{
		if (save)
		{
			final HotbarStorage storage = client.getCreativeHotbarStorage();
			final List<ItemStack> storageEntry = storage.getSavedHotbar(index).deserialize(Objects.requireNonNull(client.player)
					.getWorld().getRegistryManager());

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

				if (!hotbarEntry.isEmpty() && !ItemStack.areItemsAndComponentsEqual(inventoryStack, hotbarEntry))
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
		if (getSelectedTab().getType() == ItemGroup.Type.HOTBAR)
		{
			setSelectedTab(Registries.ITEM_GROUP.get(ItemGroups.HOTBAR));
		}
	}

	@Subscribe
	@Unique
	public void onReload(ReloadPageEvent event)
	{
		if (getSelectedTab().getType() == ItemGroup.Type.HOTBAR)
		{
			setSelectedTab(Registries.ITEM_GROUP.get(ItemGroups.HOTBAR));
		}
	}

	@Subscribe
	@Unique
	public void onCacheClear(CacheClearEvent event)
	{
		if (getSelectedTab().getType() == ItemGroup.Type.HOTBAR)
		{
			setSelectedTab(Registries.ITEM_GROUP.get(ItemGroups.HOTBAR));
		}
	}

	@Accessor
	public abstract ItemGroup getSelectedTab();
}
