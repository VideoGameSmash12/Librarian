package me.videogamesm12.librarian.v1_13_2.ornithe.mixin;

import com.google.common.eventbus.Subscribe;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.videogamesm12.librarian.Librarian;
import me.videogamesm12.librarian.api.IMechanicFactory;
import me.videogamesm12.librarian.api.event.CacheClearEvent;
import me.videogamesm12.librarian.api.event.NavigationEvent;
import me.videogamesm12.librarian.api.event.ReloadPageEvent;
import me.videogamesm12.librarian.v1_13_2.ornithe.addon.OSLAddon;
import net.kyori.adventure.text.Component;
import net.minecraft.client.Hotbar;
import net.minecraft.client.HotbarManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.ConfirmationListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.menu.CreativeInventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.CreativeModeTab;
import net.minecraft.item.ItemStack;
import net.minecraft.text.TranslatableText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(CreativeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin extends Screen
{
	@Shadow private static int selectedTab;

	@Shadow protected abstract void setSelectedTab(CreativeModeTab tab);

	private IMechanicFactory mechanic;

	private ButtonWidget nextButton;
	private ButtonWidget backupButton;
	private ButtonWidget previousButton;

	private String label = I18n.translate("librarian.saved_toolbars.tab",
			Librarian.getInstance().getCurrentPageNumber().toString());

	@Inject(method = "init", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/inventory/menu/InventoryMenu;addListener(Lnet/minecraft/inventory/menu/InventoryMenuListener;)V"))
	public void injectInit(CallbackInfo ci)
	{
		Librarian.getInstance().getEventBus().register(this);
		mechanic = Librarian.getInstance().getMechanic();

		// Offset
		int x = ((InventoryMenuScreenAccessor) this).getX() + 167;
		int y = ((InventoryMenuScreenAccessor) this).getY() + 4;

		// Initialize our buttons
		nextButton = mechanic.createButton(x + 12, y, 12, 12, Component.text("→"),
				Component.text("Next page"), () -> Librarian.getInstance().nextPage());
		backupButton = mechanic.createButton(x, y, 12, 12, Component.text("✍"),
				Component.text("Make a backup of this page"), () -> Librarian.getInstance().getCurrentPage().backup());
		previousButton = mechanic.createButton(x - 12, y, 12, 12, Component.text("←"),
				Component.text("Previous page"), () -> Librarian.getInstance().previousPage());

		// Determine visibility
		nextButton.visible = selectedTab == CreativeModeTab.HOTBAR.getId();
		backupButton.visible = selectedTab == CreativeModeTab.HOTBAR.getId();
		backupButton.active = Librarian.getInstance().getCurrentPage().exists();
		previousButton.visible = selectedTab == CreativeModeTab.HOTBAR.getId();

		// Add the buttons
		addButton(nextButton);
		addButton(backupButton);
		addButton(previousButton);
	}

	@Inject(method = "removed", at = @At(value = "RETURN"))
	public void hookRemoved(CallbackInfo ci)
	{
		Librarian.getInstance().getEventBus().unregister(this);
	}

	// Upon setting the current tab,
	@Inject(method = "setSelectedTab", at = @At("HEAD"))
	public void hookTabSelected(CreativeModeTab group, CallbackInfo ci)
	{
		// Determine visibility and other stuff
		if (nextButton != null) nextButton.visible = group == CreativeModeTab.HOTBAR;
		if (backupButton != null)
		{
			backupButton.visible = group == CreativeModeTab.HOTBAR;
			backupButton.active = Librarian.getInstance().getCurrentPage().exists();
		}
		if (previousButton != null) previousButton.visible = group == CreativeModeTab.HOTBAR;
	}

	@ModifyArg(method = "drawForeground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/TextRenderer;draw(Ljava/lang/String;FFI)I", ordinal = 0))
	public String setTitle(String string)
	{
		return selectedTab == CreativeModeTab.HOTBAR.getId() ? label : string;
	}

	@Inject(method = "keyPressed", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screen/inventory/menu/PlayerInventoryScreen;keyPressed(III)Z",
			shift = At.Shift.BEFORE), cancellable = true)
	public void hookKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir)
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
			Librarian.getInstance().getCurrentPage().backup();
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
			case 2:
			{
				// R
				if (keyCode == 82)
				{
					Librarian.getInstance().reloadCurrentPage();
					cir.setReturnValue(true);
				}
				break;
			}
			// ALT
			case 4:
			{
				// LEFT ARROW
				if (keyCode == 263)
				{
					Librarian.getInstance().previousPage();
					cir.setReturnValue(true);
				}
				// RIGHT ARROW
				else if (keyCode == 262)
				{
					Librarian.getInstance().nextPage();
					cir.setReturnValue(true);
				}
				break;
			}
			// SHIFT
			case 1:
			{
				// LEFT ARROW
				if (keyCode == 263)
				{
					Librarian.getInstance().advanceBy(-5);
					cir.setReturnValue(true);
				}
				// RIGHT ARROW
				else if (keyCode == 262)
				{
					Librarian.getInstance().advanceBy(5);
					cir.setReturnValue(true);
				}
				break;
			}
			// ALT + SHIFT
			case 5:
			{
				// LEFT ARROW
				if (keyCode == 263)
				{
					Librarian.getInstance().advanceBy(-10);
					cir.setReturnValue(true);
				}
				// RIGHT ARROW
				else if (keyCode == 262)
				{
					Librarian.getInstance().advanceBy(10);
					cir.setReturnValue(true);
				}
				break;
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
		if (selectedTab == CreativeModeTab.HOTBAR.getId())
		{
			setSelectedTab(CreativeModeTab.HOTBAR);
		}

		label = I18n.translate("librarian.saved_toolbars.tab",
				Librarian.getInstance().getCurrentPageNumber().toString());
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
}
