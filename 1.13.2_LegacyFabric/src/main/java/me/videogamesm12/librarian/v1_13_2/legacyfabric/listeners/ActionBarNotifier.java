package me.videogamesm12.librarian.v1_13_2.legacyfabric.listeners;

import com.google.common.eventbus.Subscribe;
import me.videogamesm12.librarian.api.AbstractEventListener;
import me.videogamesm12.librarian.api.event.*;
import me.videogamesm12.librarian.util.FNF;
import net.minecraft.client.MinecraftClient;

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
			sendActionBar(event.getStorage().getLocation().getName() + " backed up to " + event.getPath().getName());
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

	private void sendActionBar(String message)
	{
		if (MinecraftClient.getInstance().inGameHud == null)
		{
			return;
		}

		MinecraftClient.getInstance().inGameHud.setOverlayMessage(message, false);
	}
}
