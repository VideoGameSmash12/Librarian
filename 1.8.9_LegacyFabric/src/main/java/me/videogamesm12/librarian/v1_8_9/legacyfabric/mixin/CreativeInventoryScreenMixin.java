package me.videogamesm12.librarian.v1_8_9.legacyfabric.mixin;

import com.google.common.eventbus.Subscribe;
import me.videogamesm12.librarian.Librarian;
import me.videogamesm12.librarian.api.IMechanicFactory;
import me.videogamesm12.librarian.api.event.CacheClearEvent;
import me.videogamesm12.librarian.api.event.NavigationEvent;
import me.videogamesm12.librarian.api.event.ReloadPageEvent;
import me.videogamesm12.librarian.v1_8_9.legacyfabric.ILButtonWidget;
import me.videogamesm12.librarian.v1_8_9.legacyfabric.CursedLibrarian;
import net.kyori.adventure.text.Component;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.item.itemgroup.ItemGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin extends Screen
{
	@Shadow protected abstract void setSelectedTab(ItemGroup group);

	private IMechanicFactory mechanic;

	private ButtonWidget nextButton;
	private ButtonWidget backupButton;
	private ButtonWidget previousButton;

	private String label = I18n.translate("librarian.saved_toolbars.tab",
			Librarian.getInstance().getCurrentPageNumber().toString());

	@Inject(method = "init", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/screen/ScreenHandler;addListener(Lnet/minecraft/screen/ScreenHandlerListener;)V"))
	public void injectInit(CallbackInfo ci)
	{
		Librarian.getInstance().getEventBus().register(this);
		mechanic = Librarian.getInstance().getMechanic();

		// Offset
		int x = ((HandledScreenAccessor) this).getX() + 167;
		int y = ((HandledScreenAccessor) this).getY() + 4;

		// Initialize our buttons
		nextButton = mechanic.createButton(x + 12, y, 12, 12, Component.text(">"),
				Component.text("Next page"), () -> Librarian.getInstance().nextPage());
		backupButton = mechanic.createButton(x, y, 12, 12, Component.text("✍"),
				Component.text("Make a backup of this page"), () -> Librarian.getInstance().getCurrentPage().backup());
		previousButton = mechanic.createButton(x - 12, y, 12, 12, Component.text("<"),
				Component.text("Previous page"), () -> Librarian.getInstance().previousPage());

		// Determine visibility
		nextButton.visible = getSelectedTab() == CursedLibrarian.getGroup().getIndex();
		backupButton.visible = getSelectedTab() == CursedLibrarian.getGroup().getIndex();
		backupButton.active = Librarian.getInstance().getCurrentPage().exists();
		previousButton.visible = getSelectedTab() == CursedLibrarian.getGroup().getIndex();

		// Add the buttons
		this.buttons.add(nextButton);
		this.buttons.add(backupButton);
		this.buttons.add(previousButton);
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
		if (nextButton != null) nextButton.visible = group == CursedLibrarian.getGroup();
		if (backupButton != null)
		{
			backupButton.visible = group == CursedLibrarian.getGroup();
			backupButton.active = Librarian.getInstance().getCurrentPage().exists();
		}
		if (previousButton != null) previousButton.visible = group == CursedLibrarian.getGroup();
	}

	@Inject(method = "buttonClicked", at = @At("HEAD"))
	public void handleButtonClicks(ButtonWidget button, CallbackInfo ci)
	{
		((ILButtonWidget) button).librarian$onClick();
	}

	@ModifyArg(method = "drawForeground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/font/TextRenderer;draw(Ljava/lang/String;III)I", ordinal = 0))
	public String setTitle(String string)
	{
		return getSelectedTab() == CursedLibrarian.getGroup().getIndex() ? label : string;
	}


	@Subscribe
	@Unique
	public void onNavigation(NavigationEvent event)
	{
		// Refresh!
		if (getSelectedTab() == CursedLibrarian.getGroup().getIndex())
		{
			setSelectedTab(CursedLibrarian.getGroup());
		}

		label = I18n.translate("librarian.saved_toolbars.tab",
				Librarian.getInstance().getCurrentPageNumber().toString());
	}

	@Subscribe
	@Unique
	public void onReload(ReloadPageEvent event)
	{
		if (getSelectedTab() == CursedLibrarian.getGroup().getIndex())
		{
			setSelectedTab(CursedLibrarian.getGroup());
		}
	}

	@Subscribe
	@Unique
	public void onCacheClear(CacheClearEvent event)
	{
		if (getSelectedTab() == CursedLibrarian.getGroup().getIndex())
		{
			setSelectedTab(CursedLibrarian.getGroup());
		}
	}

	@Accessor
	public abstract int getSelectedTab();
}
