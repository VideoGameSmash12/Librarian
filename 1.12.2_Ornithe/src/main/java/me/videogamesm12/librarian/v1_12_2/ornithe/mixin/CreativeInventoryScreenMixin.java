package me.videogamesm12.librarian.v1_12_2.ornithe.mixin;

import com.google.common.eventbus.Subscribe;
import me.videogamesm12.librarian.Librarian;
import me.videogamesm12.librarian.api.IMechanicFactory;
import me.videogamesm12.librarian.api.event.CacheClearEvent;
import me.videogamesm12.librarian.api.event.NavigationEvent;
import me.videogamesm12.librarian.api.event.ReloadPageEvent;
import me.videogamesm12.librarian.v1_12_2.ornithe.ILButtonWidget;
import me.videogamesm12.librarian.v1_12_2.ornithe.addon.OSLAddon;
import net.kyori.adventure.text.Component;
import net.minecraft.client.HotbarManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.menu.CreativeInventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
		nextButton = mechanic.createButton(x + 12, y, 12, 12, Component.text(">"),
				Component.text("Next page"), () -> Librarian.getInstance().nextPage());
		backupButton = mechanic.createButton(x, y, 12, 12, Component.text("✍"),
				Component.text("Make a backup of this page"), () -> Librarian.getInstance().getCurrentPage().backup());
		previousButton = mechanic.createButton(x - 12, y, 12, 12, Component.text("<"),
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

	@ModifyArg(method = "drawForeground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/TextRenderer;draw(Ljava/lang/String;III)I", ordinal = 0))
	public String setTitle(String string)
	{
		return selectedTab == CreativeModeTab.HOTBAR.getId() ? label : string;
	}

	@Inject(method = "keyPressed", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screen/inventory/menu/PlayerInventoryScreen;keyPressed(CI)V",
			shift = At.Shift.BEFORE), cancellable = true)
	public void inject(char chr, int key, CallbackInfo ci)
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
