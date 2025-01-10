package me.videogamesm12.librarian.v1_19_2.mixin;

import com.google.common.eventbus.Subscribe;
import me.videogamesm12.librarian.Librarian;
import me.videogamesm12.librarian.api.IMechanicFactory;
import me.videogamesm12.librarian.api.event.CacheClearEvent;
import me.videogamesm12.librarian.api.event.NavigationEvent;
import me.videogamesm12.librarian.api.event.ReloadPageEvent;
import me.videogamesm12.librarian.v1_19_2.addon.FabricAPIAddon;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemGroup;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
		nextButton.visible = getSelectedTab() == ItemGroup.HOTBAR.getIndex();
		backupButton.visible = getSelectedTab() == ItemGroup.HOTBAR.getIndex();
		backupButton.active = Librarian.getInstance().getCurrentPage().exists();
		previousButton.visible = getSelectedTab() == ItemGroup.HOTBAR.getIndex();

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
		if (nextButton != null) nextButton.visible = group == ItemGroup.HOTBAR;
		if (backupButton != null)
		{
			backupButton.visible = group == ItemGroup.HOTBAR;
			backupButton.active = Librarian.getInstance().getCurrentPage().exists();
		}
		if (previousButton != null) previousButton.visible = group == ItemGroup.HOTBAR;
	}

	@ModifyArg(method = "drawForeground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/font/TextRenderer;draw(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/text/Text;FFI)I", ordinal = 0))
	private Text getLabel(Text text)
	{
		return getSelectedTab() == ItemGroup.HOTBAR.getIndex() ? label : text;
	}


	@Inject(method = "keyPressed", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screen/ingame/AbstractInventoryScreen;keyPressed(III)Z",
			shift = At.Shift.BEFORE), cancellable = true)
	public void inject(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir)
	{
		System.out.println(keyCode);
		System.out.println(scanCode);
		System.out.println(modifiers);

		// Librarian-specific keybinds
		if (getSelectedTab() == ItemGroup.HOTBAR.getIndex())
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
	}

	@Subscribe
	@Unique
	public void onNavigation(NavigationEvent event)
	{
		// Refresh!
		if (getSelectedTab() == ItemGroup.HOTBAR.getIndex())
		{
			setSelectedTab(ItemGroup.HOTBAR);
		}

		label = Text.translatable("librarian.saved_hotbars.tab", event.getNewPage());
	}

	@Subscribe
	@Unique
	public void onReload(ReloadPageEvent event)
	{
		if (getSelectedTab() == ItemGroup.HOTBAR.getIndex())
		{
			setSelectedTab(ItemGroup.HOTBAR);
		}
	}

	@Subscribe
	@Unique
	public void onCacheClear(CacheClearEvent event)
	{
		if (getSelectedTab() == ItemGroup.HOTBAR.getIndex())
		{
			setSelectedTab(ItemGroup.HOTBAR);
		}
	}

	@Accessor
	public abstract int getSelectedTab();
}
