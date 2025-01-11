package me.videogamesm12.librarian.v1_20_6.listeners;

import com.google.common.eventbus.Subscribe;
import me.videogamesm12.librarian.api.AbstractEventListener;
import me.videogamesm12.librarian.api.event.*;
import me.videogamesm12.librarian.util.FNF;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@SuppressWarnings("unused")
public class ActionBarNotifier extends AbstractEventListener
{
	@Subscribe
	public void onNavigation(NavigationEvent event)
	{
		sendActionBar(Text.translatable("librarian.messages.navigation.action_bar",
				FNF.getPageFileName(event.getNewPage())));
	}

	@Subscribe
	public void onLoadFailure(LoadFailureEvent event)
	{
		final Throwable throwable = event.getError().getCause() != null ? event.getError().getCause() : event.getError();

		sendActionBar(Text.translatable("librarian.messages.load_failed.action_bar", throwable.getLocalizedMessage())
				.formatted(Formatting.RED));
	}

	@Subscribe
	public void onSaveFailure(SaveFailureEvent event)
	{
		final Throwable throwable = event.getError().getCause() != null ? event.getError().getCause() : event.getError();

		sendActionBar(Text.translatable("librarian.messages.save_failed.action_bar", throwable.getClass().getName())
				.formatted(Formatting.RED));
	}

	@Subscribe
	public void onBackupOutcome(BackupOutcomeEvent event)
	{
		if (event.getPath() != null)
		{
			sendActionBar(Text.translatable("librarian.messages.backup_success.action_bar",
					event.getStorage().getLocation().getName(), event.getPath().getName()));
		}
		else
		{
			sendActionBar(Text.translatable("librarian.messages.backup_failed.action_bar",
					event.getException().getClass().getName()).formatted(Formatting.RED));
		}
	}

	@Subscribe
	public void onCacheClear(CacheClearEvent event)
	{
		sendActionBar(Text.translatable("librarian.messages.cache_cleared.action_bar"));
	}

	private void sendActionBar(Text message)
	{
		if (MinecraftClient.getInstance().player == null)
		{
			return;
		}

		MinecraftClient.getInstance().player.sendMessage(message, true);
	}
}
