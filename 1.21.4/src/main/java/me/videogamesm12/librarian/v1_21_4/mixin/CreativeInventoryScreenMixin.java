package me.videogamesm12.librarian.v1_21_4.mixin;

import com.google.common.eventbus.Subscribe;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.videogamesm12.librarian.Librarian;
import me.videogamesm12.librarian.api.IMechanicFactory;
import me.videogamesm12.librarian.api.event.CacheClearEvent;
import me.videogamesm12.librarian.api.event.NavigationEvent;
import me.videogamesm12.librarian.api.event.ReloadPageEvent;
import me.videogamesm12.librarian.util.FNF;
import me.videogamesm12.librarian.v1_21_4.addon.FabricAPIAddon;
import net.fabricmc.fabric.impl.client.itemgroup.FabricCreativeGuiComponents;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
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

	private IMechanicFactory mechanic;

	private Text label = Text.translatable("librarian.saved_hotbars.tab",
			Librarian.getInstance().getCurrentPageNumber());
	private ButtonWidget nextButton;
	private ButtonWidget backupButton;
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

		// Initialize buttons
		nextButton = mechanic.createButton(x + 12, y,12, 12, Component.text("→"),
				Component.text("Next page"), () -> Librarian.getInstance().nextPage());
		backupButton = mechanic.createButton(x, y,12, 12, Component.text("\uD83D\uDCBE")
				.font(Key.key("librarian", "default")), Component.text("Make a backup of this page"),
				() -> Librarian.getInstance().getCurrentPage().backup());
		previousButton = mechanic.createButton(x - 12, y,12, 12, Component.text("←"),
				Component.text("Previous page"), () -> Librarian.getInstance().previousPage());

		//
		nextButton.visible = getSelectedTab().getType() == ItemGroup.Type.HOTBAR;
		backupButton.visible = getSelectedTab().getType() == ItemGroup.Type.HOTBAR;
		backupButton.active = Librarian.getInstance().getCurrentPage().exists();
		previousButton.visible = getSelectedTab().getType() == ItemGroup.Type.HOTBAR;

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

	@ModifyArg(method = "drawForeground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIIZ)I", ordinal = 0))
	private Text getLabel(Text text)
	{
		return getSelectedTab().getType() == ItemGroup.Type.HOTBAR ? label : text;
	}


	@Inject(method = "keyPressed", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;keyPressed(III)Z",
			shift = At.Shift.BEFORE), cancellable = true)
	public void inject(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir)
	{
		System.out.println(keyCode);
		System.out.println(scanCode);
		System.out.println(modifiers);

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
				case 2 ->
				{
					// R
					if (keyCode == GLFW.GLFW_KEY_R)
					{
						Librarian.getInstance().reloadCurrentPage();
						cir.setReturnValue(true);
					}
				}
				// CTRL + SHIFT
				case 3 ->
				{
					if (keyCode == GLFW.GLFW_KEY_R)
					{
						MinecraftClient.getInstance().setScreen(new ConfirmScreen((bool) -> {
							if (bool) Librarian.getInstance().getMechanic().overhaulHotbars(FNF.getAllPageFiles());
							MinecraftClient.getInstance().setScreen(null);
						}, Text.translatable("librarian.messages.rebuild.prompt.title"), Text.translatable("librarian.messages.rebuild.prompt.description")));
					}
				}
				// ALT
				case 4 ->
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
				case 1 ->
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
				case 5 ->
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

	@WrapMethod(method = "onHotbarKeyPress")
	private static void checkForAccidentalOverwrites(MinecraftClient client, int index, boolean restore, boolean save, Operation<Void> original)
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

		label = Text.translatable("librarian.saved_hotbars.tab", event.getNewPage());
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
