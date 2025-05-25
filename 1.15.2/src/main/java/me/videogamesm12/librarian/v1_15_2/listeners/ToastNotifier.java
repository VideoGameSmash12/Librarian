/*
 * Copyright (C) 2025 Video
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

package me.videogamesm12.librarian.v1_15_2.listeners;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.platform.GlStateManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.videogamesm12.librarian.api.AbstractEventListener;
import me.videogamesm12.librarian.api.event.*;
import me.videogamesm12.librarian.util.FNF;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.lwjgl.opengl.GL11;

public class ToastNotifier extends AbstractEventListener
{
	private static final Identifier BACKGROUND = new Identifier("librarian", "textures/gui/legacy_toasts.png");
	private static final ItemStack BOOKSHELF = new ItemStack(Registry.ITEM.get(new Identifier("minecraft:bookshelf")));

	@Subscribe
	public void onBackupOutcome(BackupOutcomeEvent event)
	{
		if (event.getPath() != null)
		{
			addOrUpdateNotification(new TranslatableText("librarian.messages.backup_success.toast.title"),
					new LiteralText(event.getStorage().librarian$getLocation().getName()), LibrarianToast.Type.BACKUP);
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
		public Visibility draw(ToastManager manager, long startTime)
		{
			// Background comes first
			MinecraftClient.getInstance().getTextureManager().bindTexture(BACKGROUND);
			GL11.glColor3f(1.0F, 1.0F, 1.0F);
			manager.blit(0, 0, 0, 0, 160, 32);

			// Icons come second
			type.draw(manager);

			// Text comes last
			TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
			textRenderer.draw(title.getString(), 30, description != null ? 7 : 12, type.getTextColor());
			if (description != null)
				textRenderer.draw(description.getString(), 30, 18, 0xFFFFFF);

			return startTime - this.time >= 5000L ? Visibility.HIDE : Visibility.SHOW;
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
			NAVIGATION(0x00FF00, true, 1, 1);

			private final int textColor;
			private final boolean hasIcon;
			private final int gridX;
			private final int gridY;

			public void draw(DrawableHelper element)
			{
				if (hasIcon)
				{
					GlStateManager.enableBlend();
					element.blit(6, 6, 176 + gridX * 20, gridY * 20, 20, 20);
					GlStateManager.enableBlend();
				}
				else
				{
					DiffuseLighting.enable();
					MinecraftClient.getInstance().getItemRenderer().renderGuiItemIcon(BOOKSHELF, 9, 8);
				}
			}
		}
	}
}
