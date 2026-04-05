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

package me.videogamesm12.librarian.api;

import me.videogamesm12.librarian.Librarian;
import me.videogamesm12.librarian.api.event.AsyncPageLoadEvent;
import me.videogamesm12.librarian.api.event.BackupOutcomeEvent;
import me.videogamesm12.librarian.util.FNF;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * <h1>IWrappedHotbarStorage</h1>
 * <p>Wrapper class for HotbarStorage (or, depending on the mappings being used, HotbarManager) instances with extra
 * code for Librarian-specific functions.</p>
 * <p>Because it is impossible to create constructors in classes through mixins, often times this will be implemented
 * into wrapper classes that extend the </p>
 */
public interface IWrappedHotbarStorage
{
	File librarian$getLocation();

	BigInteger librarian$getPageNumber();

	default boolean exists()
	{
		return librarian$getLocation().exists();
	}

	default void librarian$backup()
	{
		try
		{
			// If it doesn't exist, don't even try to back it up
			if (!librarian$getLocation().exists())
			{
				return;
			}

			final File backupFile = Files.copy(librarian$getLocation().toPath(), new File(FNF.getBackupFolder(),
					FNF.getBackupFileName(librarian$getPageNumber())).toPath()).toFile();

			Librarian.getInstance().getEventBus().post(new BackupOutcomeEvent(this, backupFile));
		}
		catch (IOException ex)
		{
			Librarian.getInstance().getEventBus().post(new BackupOutcomeEvent(this, ex));
		}
	}

	void librarian$load();

	default CompletableFuture<IWrappedHotbarStorage> librarian$loadAsync()
	{
		final IWrappedHotbarStorage page = this;

		return CompletableFuture.supplyAsync(() ->
		{
			librarian$load();
			return page;
		}).thenApply((storage) ->
		{
			if (Librarian.getInstance().getConfig().optimizations().preprocessHotbarRows())
			{
				storage.librarian$preprocess();
			}

			return storage;
		}).whenCompleteAsync((storage, throwable) ->
		{
			if (throwable != null)
			{
				Librarian.getLogger().fatal("Somehow this threw an exception! Wtf?", throwable);
			}
			else
			{
				Librarian.getInstance().getEventBus().post(new AsyncPageLoadEvent(storage));
			}
		});
	}

	default void librarian$preprocess()
	{
	}

	default LoadStatus librarian$getLoadStatus()
	{
		return LoadStatus.NOT_LOADED;
	}

	int librarian$getRowCount();

	@Deprecated
	default boolean isLoaded()
	{
		return librarian$getLoadStatus() == LoadStatus.LOADED;
	}

	@Deprecated
	default void setLoaded(boolean newValue)
	{
		// Don't do anything, this should be implemented when implementing in HotbarStorage as a mixin
	}

	default int librarian$dataVersion()
	{
		return 0;
	}

	default Optional<HotbarPageMetadata> librarian$getMetadata()
	{
		return Optional.empty();
	}

	default void librarian$setMetadata(HotbarPageMetadata metadata)
	{
		// Do nothing
	}
}
