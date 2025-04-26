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

package me.videogamesm12.librarian.api.event;

import lombok.Getter;
import me.videogamesm12.librarian.api.IWrappedHotbarStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Objects;

/**
 * <h1>BackupOutcomeEvent</h1>
 * <p>An event for the end result of a previously requested backup. This includes both successful backups and failed
 * 	backups, with a constructor provided for both cases.</p>
 */
@Getter
public final class BackupOutcomeEvent extends LibrarianEvent
{
	/**
	 * The exception that was thrown if the backup failed. If the backup was successful, this is null.
	 */
	@Nullable
	private final Throwable exception;
	/**
	 * The page that we tried to back up.
	 */
	@NotNull
	private final IWrappedHotbarStorage storage;
	/**
	 * The path for the destination file if the backup was successful. If the backup failed, this is null.
	 */
	@Nullable
	private final File path;

	/**
	 * Constructor for a backup failure.
	 * @param storage	{@link IWrappedHotbarStorage}
	 * @param ex		{@link Throwable}
	 */
	public BackupOutcomeEvent(@NotNull IWrappedHotbarStorage storage, @NotNull Throwable ex)
	{
		Objects.requireNonNull(storage);
		Objects.requireNonNull(ex);

		this.storage = storage;
		this.exception = ex;
		this.path = null;
	}

	/**
	 * Constructor for a successful backup.
	 * @param storage	{@link IWrappedHotbarStorage}
	 * @param path		{@link File}
	 */
	public BackupOutcomeEvent(@NotNull IWrappedHotbarStorage storage, @NotNull File path)
	{
		Objects.requireNonNull(storage);
		Objects.requireNonNull(path);

		this.storage = storage;
		this.exception = null;
		this.path = path;
	}
}
