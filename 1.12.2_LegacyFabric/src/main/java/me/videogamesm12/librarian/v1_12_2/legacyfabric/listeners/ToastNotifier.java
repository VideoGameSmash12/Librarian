package me.videogamesm12.librarian.v1_12_2.legacyfabric.listeners;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.GLX;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.videogamesm12.librarian.api.AbstractEventListener;
import me.videogamesm12.librarian.api.event.*;
import me.videogamesm12.librarian.util.FNF;
import net.minecraft.class_3262;
import net.minecraft.class_3264;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;

public class ToastNotifier extends AbstractEventListener
{
	private static final Identifier BACKGROUND = new Identifier("librarian", "textures/gui/legacy_toasts.png");
	private static final ItemStack BOOKSHELF = new ItemStack(Item.getFromId("minecraft:bookshelf"));

	@Subscribe
	public void onBackupOutcome(BackupOutcomeEvent event)
	{
		if (event.getPath() != null)
		{
			addOrUpdateNotification(new TranslatableText("librarian.messages.backup_success.toast.title"),
					new LiteralText(event.getStorage().getLocation().getName()), LibrarianToast.Type.BACKUP);
		}
		else
		{
			addOrUpdateNotification(new TranslatableText("librarian.messages.backup_failed.toast.title"),
					new TranslatableText("librarian.messages.backup_failed.toast.description"), LibrarianToast.Type.BACKUP_FAILURE);
		}
	}

	@Subscribe
	public void onNavigation(NavigationEvent event)
	{
		addOrUpdateNotification(new TranslatableText("librarian.messages.navigation.toast.title"),
				new LiteralText(FNF.getPageFileName(event.getNewPage())), LibrarianToast.Type.NAVIGATION);
	}

	@Subscribe
	public void onLoadFailure(LoadFailureEvent event)
	{
		addOrUpdateNotification(new TranslatableText("librarian.messages.load_failed.toast.title"),
				new TranslatableText("librarian.messages.load_failed.toast.description"), LibrarianToast.Type.LOAD_FAILURE);
	}

	@Subscribe
	public void onSaveFailure(SaveFailureEvent event)
	{
		addOrUpdateNotification(new TranslatableText("librarian.messages.save_failed.toast.title"),
				new TranslatableText("librarian.messages.save_failed.toast.description"), LibrarianToast.Type.SAVE_FAILURE);
	}

	@Subscribe
	public void onRefresh(ReloadPageEvent event)
	{
		addOrUpdateNotification(new TranslatableText("librarian.messages.reload.toast.title"),
				new LiteralText(FNF.getPageFileName(event.getCurrentPage())), LibrarianToast.Type.RELOAD);
	}

	@Subscribe
	public void onCacheClear(CacheClearEvent event)
	{
		addOrUpdateNotification(new TranslatableText("librarian.messages.cache_cleared.toast.title"), null,
				LibrarianToast.Type.CACHE_CLEARED);
	}

	private void addOrUpdateNotification(Text title, Text description, LibrarianToast.Type type)
	{
		final LibrarianToast toast = MinecraftClient.getInstance().method_14462().method_14493(LibrarianToast.class, type);

		if (toast == null)
		{
			MinecraftClient.getInstance().method_14462().method_14491(new LibrarianToast(title, description, type));
		}
		else
		{
			toast.setTitle(title);
			toast.setDescription(description);
		}
	}

	@Getter
	public static class LibrarianToast implements class_3262
	{
		@Setter
		private Text title;
		@Setter
		private Text description;
		private final Type type;

		public LibrarianToast(Text title, Text description, Type type)
		{
			this.title = title;
			this.description = description;
			this.type = type;
		}

		@Override
		public class_3263 method_14486(class_3264 toasts, long animationTime)
		{
			// Background comes first
			MinecraftClient.getInstance().getTextureManager().bindTexture(BACKGROUND);
			GlStateManager.color(1.0F, 1.0F, 1.0F);
			toasts.drawTexture(0, 0, 0, 0, 160, 32);

			// Icons come second
			type.draw(toasts);

			// Text comes last
			TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
			textRenderer.draw(title.asUnformattedString(), 30, description != null ? 7 : 12, type.getTextColor());
			if (description != null)
				textRenderer.draw(description.asUnformattedString(), 30, 18, 0xFFFFFF);

			return animationTime >= 5000L ? class_3263.HIDE : class_3263.SHOW;
		}

		@Override
		public Type method_14487()
		{
			return type;
		}

		@Getter
		@RequiredArgsConstructor
		public enum Type
		{
			BACKUP(0x00FF00, true, 0, 0),
			BACKUP_FAILURE(0xFF0000, true, 1, 0),
			CACHE_CLEARED(0x00AAFF, true, 2, 0),
			LOAD_FAILURE(0xFF0000, true, 3, 0),
			SAVE_FAILURE(0xFF0000, true, 3, 0),
			RELOAD(0x00AAFF, true, 0, 1),
			NAVIGATION(0x00FF00, false, 0, 0);

			private final int textColor;
			private final boolean hasIcon;
			private final int gridX;
			private final int gridY;

			public void draw(DrawableHelper element)
			{
				if (hasIcon)
				{
					GlStateManager.enableBlend();
					element.drawTexture(6, 6, 176 + gridX * 20, gridY * 20, 20, 20);
					GlStateManager.enableBlend();
				}
				else
				{
					DiffuseLighting.enable();
					MinecraftClient.getInstance().getItemRenderer().method_12455(BOOKSHELF, 9, 8);
				}
			}
		}
	}
}
