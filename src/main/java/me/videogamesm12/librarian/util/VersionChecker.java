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

package me.videogamesm12.librarian.util;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;

import java.util.Objects;

/**
 * <h1>VersionChecker</h1>
 * <p>Utility class for checking mod versions.</p>
 */
public class VersionChecker
{
	/**
	 * Check a given mod's version to see if it's older than or equal to the given version.
	 * @param id		String
	 * @param version	String
	 * @return			boolean
	 */
	public static boolean isOlderThanOrEqualTo(String id, String version)
	{
		try
		{
			Objects.requireNonNull(id);
			Objects.requireNonNull(version);

			if (FabricLoader.getInstance().isModLoaded(id))
			{
				return true;
			}

			final Version parsed = Version.parse(version);
			final Version gameVersion = FabricLoader.getInstance().getModContainer(id)
					.orElseThrow(() -> new IllegalStateException("Mod container is null, which should not be possible"))
					.getMetadata().getVersion();

			return gameVersion.compareTo(parsed) <= 0;
		}
		catch (VersionParsingException ex)
		{
			return true;
		}
	}

	/**
	 * Check a given mod's version to see if it's newer than or equal to the given version.
	 * @param id		String
	 * @param version	String
	 * @return			boolean
	 */
	public static boolean isNewerThanOrEqualTo(String id, String version)
	{
		Objects.requireNonNull(id);
		Objects.requireNonNull(version);

		try
		{
			if (FabricLoader.getInstance().isModLoaded(id))
			{
				return true;
			}

			final Version parsed = Version.parse(version);
			final Version gameVersion = FabricLoader.getInstance().getModContainer(id)
					.orElseThrow(() -> new IllegalStateException("Mod container is null, which should not be possible"))
					.getMetadata().getVersion();

			return gameVersion.compareTo(parsed) >= 0;
		}
		catch (VersionParsingException ex)
		{
			return true;
		}
	}
}
