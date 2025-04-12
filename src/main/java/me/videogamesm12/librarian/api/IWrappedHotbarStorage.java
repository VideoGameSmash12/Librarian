package me.videogamesm12.librarian.api;

import me.videogamesm12.librarian.Librarian;
import me.videogamesm12.librarian.api.event.BackupOutcomeEvent;
import me.videogamesm12.librarian.util.FNF;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;

/**
 * <h1>IWrappedHotbarStorage</h1>
 * <p>Wrapper class for HotbarStorage (or, depending on the mappings being used, HotbarManager) instances with extra
 * code for Librarian-specific functions.</p>
 * <p>Because it is impossible to create constructors in classes through mixins, often times this will be implemented
 * into wrapper classes that extend the </p>
 */
public interface IWrappedHotbarStorage
{
	File getLocation();

	BigInteger getPageNumber();

	default boolean exists()
	{
		return getLocation().exists();
	}

	default void backup()
	{
		try
		{
			// If it doesn't exist, don't even try to back it up
			if (!getLocation().exists())
			{
				return;
			}

			final File backupFile = Files.copy(getLocation().toPath(), new File(FNF.getBackupFolder(),
					FNF.getBackupFileName(getPageNumber())).toPath()).toFile();

			Librarian.getInstance().getEventBus().post(new BackupOutcomeEvent(this, backupFile));
		}
		catch (IOException ex)
		{
			Librarian.getInstance().getEventBus().post(new BackupOutcomeEvent(this, ex));
		}
	}

	void load();
}
