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

package me.videogamesm12.librarian.v1_13_2.legacyfabric.listeners;

import com.google.common.eventbus.Subscribe;
import me.videogamesm12.librarian.Librarian;
import me.videogamesm12.librarian.api.AbstractEventListener;
import me.videogamesm12.librarian.api.event.*;
import me.videogamesm12.librarian.util.FNF;
import me.videogamesm12.librarian.v1_13_2.legacyfabric.Resources;
import me.videogamesm12.librarian.v1_13_2.legacyfabric.mixin.CreativeInventoryScreenAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.item.itemgroup.ItemGroup;
import net.minecraft.text.TranslatableText;

public class ActionBarNotifier extends AbstractEventListener
{
	@Subscribe
	public void onNavigation(NavigationEvent event)
	{
		sendActionBar("Hotbar selected: " + FNF.getPageFileName(event.getNewPage()));
	}

	@Subscribe
	public void onLoadFailure(LoadFailureEvent event)
	{
		final Throwable throwable = event.getError().getCause() != null ? event.getError().getCause() : event.getError();
		sendActionBar("§cFailed to load hotbar page: " + throwable.getClass().getName());
	}

	@Subscribe
	public void onSaveFailure(SaveFailureEvent event)
	{
		final Throwable throwable = event.getError().getCause() != null ? event.getError().getCause() : event.getError();
		sendActionBar("§cFailed to save hotbar page: " + throwable.getClass().getName());
	}

	@Subscribe
	public void onBackupOutcome(BackupOutcomeEvent event)
	{
		if (event.getPath() != null)
		{
			sendActionBar(event.getStorage().librarian$getLocation().getName() + " backed up to " + event.getPath().getName());
		}
		else
		{
			sendActionBar("§cFailed to back up hotbar page: " + event.getException().getClass().getName());
		}
	}

	@Subscribe
	public void onCacheClear(CacheClearEvent event)
	{
		sendActionBar("The hotbar cache has been cleared");
	}

	@Subscribe
	public void onPageLoad(AsyncPageLoadEvent event)
	{
		if (!(MinecraftClient.getInstance().currentScreen instanceof CreativeInventoryScreen
				&& CreativeInventoryScreenAccessor.getSelectedTab() == ItemGroup.field_15657.getIndex()
				&& Librarian.getInstance().getCurrentPageNumber().equals(event.getPage().librarian$getPageNumber())))
		{
			sendActionBar(Resources.translate("librarian.messages.loaded.action_bar",
					Librarian.getInstance().getCurrentPage().librarian$getLocation().getName()));
		}
	}

	private void sendActionBar(String message)
	{
		if (MinecraftClient.getInstance().inGameHud == null)
		{
			return;
		}

		MinecraftClient.getInstance().inGameHud.setOverlayMessage(message, false);
	}
}
