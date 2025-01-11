package me.videogamesm12.librarian.v1_20_4.listeners;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.videogamesm12.librarian.api.AbstractEventListener;
import me.videogamesm12.librarian.api.event.*;
import me.videogamesm12.librarian.util.FNF;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ToastNotifier extends AbstractEventListener
{
	private static final Identifier BACKGROUND = new Identifier("librarian", "textures/gui/sprites/toasts/background.png");
	private static final ItemStack BOOKSHELF = new ItemStack(Items.BOOKSHELF);

	@Subscribe
	public void onBackupOutcome(BackupOutcomeEvent event)
	{
		if (event.getPath() != null)
		{
			addOrUpdateNotification(Text.translatable("librarian.messages.backup_success.toast.title"),
					Text.literal(event.getStorage().getLocation().getName()), LibrarianToast.Type.BACKUP);
		}
		else
		{
			addOrUpdateNotification(Text.translatable("librarian.messages.backup_failed.toast.title"),
					Text.translatable("librarian.messages.backup_failed.toast.description"), LibrarianToast.Type.BACKUP_FAILURE);
		}
	}

	@Subscribe
	public void onNavigation(NavigationEvent event)
	{
		addOrUpdateNotification(Text.translatable("librarian.messages.navigation.toast.title"),
				Text.literal(FNF.getPageFileName(event.getNewPage())), LibrarianToast.Type.NAVIGATION);
	}

	@Subscribe
	public void onLoadFailure(LoadFailureEvent event)
	{
		addOrUpdateNotification(Text.translatable("librarian.messages.load_failed.toast.title"),
				Text.translatable("librarian.messages.load_failed.toast.description"), LibrarianToast.Type.LOAD_FAILURE);
	}

	@Subscribe
	public void onSaveFailure(SaveFailureEvent event)
	{
		addOrUpdateNotification(Text.translatable("librarian.messages.save_failed.toast.title"),
				Text.translatable("librarian.messages.save_failed.toast.description"), LibrarianToast.Type.SAVE_FAILURE);
	}

	@Subscribe
	public void onRefresh(ReloadPageEvent event)
	{
		addOrUpdateNotification(Text.translatable("librarian.messages.reload.toast.title"),
				Text.literal(FNF.getPageFileName(event.getCurrentPage())), LibrarianToast.Type.RELOAD);
	}

	@Subscribe
	public void onCacheClear(CacheClearEvent event)
	{
		addOrUpdateNotification(Text.translatable("librarian.messages.cache_cleared.toast.title"), null,
				LibrarianToast.Type.CACHE_CLEARED);
	}

	private void addOrUpdateNotification(Text title, Text description, LibrarianToast.Type type)
	{
		final LibrarianToast toast = MinecraftClient.getInstance().getToastManager().getToast(LibrarianToast.class, type);

		if (toast == null)
		{
			MinecraftClient.getInstance().getToastManager().add(new LibrarianToast(title, description, type));
		}
		else
		{
			toast.setTitle(title);
			toast.setDescription(description);
		}
	}

	@Getter
	public static class LibrarianToast implements Toast
	{
		@Setter
		private Text title;
		@Setter
		private Text description;
		private final Type type;

		private long time;
		private boolean justUpdated = true;

		public LibrarianToast(Text title, Text description, Type type)
		{
			this.title = title;
			this.description = description;
			this.type = type;
		}

		@Override
		public Visibility draw(DrawContext context, ToastManager manager, long startTime)
		{
			if (justUpdated)
			{
				this.time = startTime;
				justUpdated = false;
			}

			// Background comes first
			context.drawTexture(BACKGROUND, 0, 0, 0, 0, 160, 32, 160, 32);

			// Text comes second
			TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
			context.drawText(textRenderer, title, 30, description != null ? 7 : 12, type.getTextColor(), false);
			if (description != null)
			{
				context.drawText(textRenderer, description, 30, 18, 0xFFFFFF, false);
			}

			// Icon comes last
			type.draw(context);

			return startTime - this.time >= 5000L ? Visibility.HIDE : Visibility.SHOW;
		}

		@Getter
		@RequiredArgsConstructor
		public enum Type
		{
			BACKUP(0x00FF00, new Identifier("librarian", "toasts/backup")),
			BACKUP_FAILURE(0xFF0000, new Identifier("librarian", "toasts/backup_failure")),
			CACHE_CLEARED(0x00AAFF, new Identifier("librarian", "toasts/cache_cleared")),
			LOAD_FAILURE(0xFF0000, new Identifier("librarian", "toasts/failure")),
			SAVE_FAILURE(0xFF0000, new Identifier("librarian", "toasts/failure")),
			RELOAD(0x00AAFF, new Identifier("librarian", "toasts/reload")),
			NAVIGATION(0x00FF00, null);

			private final int textColor;
			private final Identifier texture;

			public void draw(DrawContext context)
			{
				if (texture != null)
				{
					RenderSystem.enableBlend();
					context.drawGuiTexture(texture,6 , 6, 20, 20);
					RenderSystem.enableBlend();
				}
				else
				{
					context.drawItem(BOOKSHELF, 9, 8);
				}
			}
		}
	}
}
