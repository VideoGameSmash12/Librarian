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

package me.videogamesm12.librarian.v26_1.listeners;

import com.google.common.eventbus.Subscribe;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.videogamesm12.librarian.Librarian;
import me.videogamesm12.librarian.api.AbstractEventListener;
import me.videogamesm12.librarian.api.event.*;
import me.videogamesm12.librarian.util.FNF;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public class ToastNotifier extends AbstractEventListener
{
	private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath("librarian", "toasts/background");

	@Subscribe
	public void onBackupOutcome(BackupOutcomeEvent event)
	{
		if (event.getPath() != null)
		{
			addOrUpdateNotification(Component.translatable("librarian.messages.backup_success.toast.title"),
					Component.literal(event.getStorage().librarian$getLocation().getName()), LibrarianToast.Type.BACKUP);
		}
		else
		{
			addOrUpdateNotification(Component.translatable("librarian.messages.backup_failed.toast.title"),
					Component.translatable("librarian.messages.backup_failed.toast.description"), LibrarianToast.Type.BACKUP_FAILURE);
		}
	}

	@Subscribe
	public void onNavigation(NavigationEvent event)
	{
		addOrUpdateNotification(Component.translatable("librarian.messages.navigation.toast.title"),
				Component.literal(FNF.getPageFileName(event.getNewPage())), LibrarianToast.Type.NAVIGATION);
	}

	@Subscribe
	public void onLoadFailure(LoadFailureEvent event)
	{
		addOrUpdateNotification(Component.translatable("librarian.messages.load_failed.toast.title"),
				Component.translatable("librarian.messages.load_failed.toast.description"), LibrarianToast.Type.LOAD_FAILURE);
	}

	@Subscribe
	public void onSaveFailure(SaveFailureEvent event)
	{
		addOrUpdateNotification(Component.translatable("librarian.messages.save_failed.toast.title"),
				Component.translatable("librarian.messages.save_failed.toast.description"), LibrarianToast.Type.SAVE_FAILURE);
	}

	@Subscribe
	public void onRefresh(ReloadPageEvent event)
	{
		addOrUpdateNotification(Component.translatable("librarian.messages.reload.toast.title"),
				Component.literal(FNF.getPageFileName(event.getCurrentPage())), LibrarianToast.Type.RELOAD);
	}

	@Subscribe
	public void onCacheClear(CacheClearEvent event)
	{
		addOrUpdateNotification(Component.translatable("librarian.messages.cache_cleared.toast.title"), null,
				LibrarianToast.Type.CACHE_CLEARED);
	}

	private void addOrUpdateNotification(Component title, Component description, LibrarianToast.Type type)
	{
		Librarian.getLogger().info("Debug - updating or adding toast");
		Librarian.getLogger().info("Debug - Toast type {}", type);
		final LibrarianToast toast = Minecraft.getInstance().getToastManager().getToast(LibrarianToast.class, type);

		if (toast == null)
		{
			Librarian.getLogger().info("Toast is null");
			Minecraft.getInstance().getToastManager().addToast(new LibrarianToast(title, description, type));
		}
		else
		{
			Librarian.getLogger().info("Toast is not null");
			toast.setTitle(title);
			toast.setDescription(description);
			toast.setJustUpdated(true);
		}
	}

	public static class LibrarianToast implements Toast
	{
		@Getter
		@Setter
		private Component title;
		@Getter
		@Setter
		private Component description;
		@Getter
		private final Type token;
		//-
		private long startTime;
		@Setter
		private boolean justUpdated = true;
		@Getter
		private Visibility wantedVisibility = Visibility.HIDE;

		public LibrarianToast(Component title, Component description, Type token)
		{
			this.title = title;
			this.description = description;
			this.token = token;
		}

		@Override
		public void update(ToastManager manager, long time)
		{
			if (this.justUpdated)
			{
				this.startTime = time;
				this.justUpdated = false;
			}

			this.wantedVisibility = time - this.startTime >= 5000 ? Visibility.HIDE : Visibility.SHOW;
		}

		@Override
		public void render(@NotNull GuiGraphics graphics, @NotNull Font font, long fullyVisibleForMs)
		{
			// Draw images
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, 0, 0, width(), height());
			if (token.getIcon() != null)
			{
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, token.getIcon(), 6, 6, 20, 20);
			}

			// Draw text
			graphics.drawString(font, title, 30, description != null ? 7 : 12, token.getTextColor(), false);
			if (description != null)
				graphics.drawString(font, description, 30, 18, 0xFFFFFFFF, false);
		}

		@Getter
		@RequiredArgsConstructor
		public enum Type
		{
			BACKUP(-16711936, Identifier.fromNamespaceAndPath("librarian", "toasts/backup")),
			BACKUP_FAILURE(-16776961, Identifier.fromNamespaceAndPath("librarian", "toasts/backup_failure")),
			CACHE_CLEARED(-22016, Identifier.fromNamespaceAndPath("librarian", "toasts/cache_cleared")),
			LOAD_FAILURE(-16776961, Identifier.fromNamespaceAndPath("librarian", "toasts/failure")),
			SAVE_FAILURE(-16776961, Identifier.fromNamespaceAndPath("librarian", "toasts/failure")),
			RELOAD(-22016, Identifier.fromNamespaceAndPath("librarian", "toasts/reload")),
			NAVIGATION(-16711936, Identifier.fromNamespaceAndPath("librarian", "toasts/page_selected"));

			private final int textColor;
			private final Identifier icon;
		}
	}
}
