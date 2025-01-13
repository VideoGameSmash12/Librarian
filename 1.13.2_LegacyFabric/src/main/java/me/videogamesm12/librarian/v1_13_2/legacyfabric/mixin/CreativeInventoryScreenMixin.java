package me.videogamesm12.librarian.v1_13_2.legacyfabric.mixin;

import com.google.common.eventbus.Subscribe;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.videogamesm12.librarian.Librarian;
import me.videogamesm12.librarian.api.IMechanicFactory;
import me.videogamesm12.librarian.api.event.CacheClearEvent;
import me.videogamesm12.librarian.api.event.NavigationEvent;
import me.videogamesm12.librarian.api.event.ReloadPageEvent;
import net.kyori.adventure.text.Component;
import net.minecraft.class_3251;
import net.minecraft.class_3297;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.itemgroup.ItemGroup;
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

	@Shadow protected abstract void setSelectedTab(ItemGroup tab);

	private IMechanicFactory mechanic;

	private ButtonWidget nextButton;
	private ButtonWidget backupButton;
	private ButtonWidget previousButton;

	private String label = String.format("Saved Toolbars (%s)",
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
		nextButton = mechanic.createButton(x + 12, y, 12, 12, Component.text("→"),
				Component.text("Next page"), () -> Librarian.getInstance().nextPage());
		backupButton = mechanic.createButton(x, y, 12, 12, Component.text("✍"),
				Component.text("Make a backup of this page"), () -> Librarian.getInstance().getCurrentPage().backup());
		previousButton = mechanic.createButton(x - 12, y, 12, 12, Component.text("←"),
				Component.text("Previous page"), () -> Librarian.getInstance().previousPage());

		// Determine visibility
		nextButton.visible = selectedTab == ItemGroup.field_15657.getIndex();
		backupButton.visible = selectedTab == ItemGroup.field_15657.getIndex();
		backupButton.active = Librarian.getInstance().getCurrentPage().exists();
		previousButton.visible = selectedTab == ItemGroup.field_15657.getIndex();

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
	public void hookTabSelected(ItemGroup group, CallbackInfo ci)
	{
		// Determine visibility and other stuff
		if (nextButton != null) nextButton.visible = group == ItemGroup.field_15657;
		if (backupButton != null)
		{
			backupButton.visible = group == ItemGroup.field_15657;
			backupButton.active = Librarian.getInstance().getCurrentPage().exists();
		}
		if (previousButton != null) previousButton.visible = group == ItemGroup.field_15657;
	}

	@ModifyArg(method = "drawForeground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/font/TextRenderer;method_18355(Ljava/lang/String;FFI)I", ordinal = 0))
	public String setTitle(String string)
	{
		return selectedTab == ItemGroup.field_15657.getIndex() ? label : string;
	}

	@Inject(method = "keyPressed", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screen/ingame/InventoryScreen;keyPressed(III)Z",
			shift = At.Shift.BEFORE), cancellable = true)
	public void hookKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir)
	{
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

	@WrapMethod(method = "method_14550")
	private static void checkForAccidentalOverwrites(MinecraftClient client, int index, boolean restore, boolean save, Operation<Void> original)
	{
		if (save)
		{
			final class_3251 storage = client.method_18221();
			final class_3297 storageEntry = storage.method_14450(index);

			if (storageEntry.isEmpty())
			{
				original.call(client, index, restore, save);
				return;
			}

			boolean confirm = false;

			for (int i = 0; i < PlayerInventory.getHotbarSize(); i++)
			{
				ItemStack inventoryStack = Objects.requireNonNull(client.player).inventory.getInvStack(i);
				ItemStack hotbarEntry = storageEntry.get(i);

				if (!hotbarEntry.isEmpty() && !inventoryStack.equals(hotbarEntry))
				{
					confirm = true;
					break;
				}
			}

			if (confirm)
			{
				MinecraftClient.getInstance().setScreen(new ConfirmScreen((value, id) ->
				{
					if (value) original.call(client, index, restore, save);
					MinecraftClient.getInstance().setScreen(null);
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
		if (selectedTab == ItemGroup.field_15657.getIndex())
		{
			setSelectedTab(ItemGroup.field_15657);
		}

		label = String.format("Saved Toolbars (%s)", Librarian.getInstance().getCurrentPageNumber().toString());
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
}
