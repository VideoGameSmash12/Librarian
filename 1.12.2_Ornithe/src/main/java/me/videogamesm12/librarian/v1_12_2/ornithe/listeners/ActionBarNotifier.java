package me.videogamesm12.librarian.v1_12_2.ornithe.listeners;

import com.google.common.eventbus.Subscribe;
import me.videogamesm12.librarian.api.AbstractEventListener;
import me.videogamesm12.librarian.api.event.*;
import me.videogamesm12.librarian.util.FNF;
import net.minecraft.client.Minecraft;
import net.minecraft.text.Formatting;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

public class ActionBarNotifier extends AbstractEventListener
{
	@Subscribe
	public void onNavigation(NavigationEvent event)
	{
		sendActionBar(new TranslatableText("librarian.messages.navigation.action_bar",
				FNF.getPageFileName(event.getNewPage())));
	}

	@Subscribe
	public void onLoadFailure(LoadFailureEvent event)
	{
		final Throwable throwable = event.getError().getCause() != null ? event.getError().getCause() : event.getError();

		sendActionBar(new TranslatableText("librarian.messages.load_failed.action_bar", throwable.getClass().getName())
				.setStyle(new Style().setColor(Formatting.RED)));
	}

	@Subscribe
	public void onSaveFailure(SaveFailureEvent event)
	{
		final Throwable throwable = event.getError().getCause() != null ? event.getError().getCause() : event.getError();

		sendActionBar(new TranslatableText("librarian.messages.save_failed.action_bar", throwable.getClass().getName())
				.setStyle(new Style().setColor(Formatting.RED)));
	}

	@Subscribe
	public void onBackupOutcome(BackupOutcomeEvent event)
	{
		if (event.getPath() != null)
		{
			sendActionBar(new TranslatableText("librarian.messages.backup_success.action_bar",
					event.getStorage().getLocation().getName(), event.getPath().getName()));
		}
		else
		{
			sendActionBar(new TranslatableText("librarian.messages.backup_failed.action_bar",
					event.getException().getClass().getName()).setStyle(new Style().setColor(Formatting.RED)));
		}
	}

	@Subscribe
	public void onCacheClear(CacheClearEvent event)
	{
		sendActionBar(new TranslatableText("librarian.messages.cache_cleared.action_bar"));
	}

	private void sendActionBar(Text message)
	{
		if (Minecraft.getInstance().gui == null)
		{
			return;
		}

		Minecraft.getInstance().gui.setOverlayMessage(message.getFormattedString(), false);
	}
}
