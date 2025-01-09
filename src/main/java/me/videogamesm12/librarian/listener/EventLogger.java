package me.videogamesm12.librarian.listener;

import com.google.common.eventbus.Subscribe;
import me.videogamesm12.librarian.Librarian;
import me.videogamesm12.librarian.api.AbstractEventListener;
import me.videogamesm12.librarian.api.event.*;

/**
 * <h1>EventLogger</h1>
 * <p>Logs all events within Librarian.</p>
 */
public class EventLogger extends AbstractEventListener
{
	@Subscribe
	public void onBackupOutcome(BackupOutcomeEvent event)
	{
		// The backup was successful
		if (event.getPath() != null)
		{
			Librarian.getLogger().info("Successfully backed up {} to file {}",
					event.getStorage().getLocation().getName(), event.getPath().getName());
		}
		// The backup failed
		else
		{
			Librarian.getLogger().error("Failed to back up hotbar file {}", event.getStorage().getLocation().getName(),
					event.getException());
		}
	}

	@Subscribe
	public void onLoadFailure(LoadFailureEvent event)
	{
		// The exception was already covered earlier
		Librarian.getLogger().error("Failed to load hotbar file {}", event.getStorage().getLocation().getName());
	}

	@Subscribe
	public void onSaveFailure(SaveFailureEvent event)
	{
		// Exception already covered
		Librarian.getLogger().error("Hotbar file {} could not be saved", event.getStorage().getLocation().getName());
	}

	@Subscribe
	public void onNavigation(NavigationEvent event)
	{
		Librarian.getLogger().info("Navigated from hotbar page {} to page {}", event.getCurrentPage().toString(),
				event.getNewPage().toString());
	}

	@Subscribe
	public void onReload(ReloadPageEvent event)
	{
		Librarian.getLogger().info("Hotbar page {} was reloaded", event.getCurrentPage().toString());
	}

	@Subscribe
	public void onCacheClear(CacheClearEvent event)
	{
		Librarian.getLogger().info("All loaded hotbar pages have been cleared from the cache");
	}
}
