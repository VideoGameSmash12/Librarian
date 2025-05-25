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

import java.io.File;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <h1>FNF</h1>
 * <p>Utility class handling <b>F</b>iles '<b>n</b> <b>F</b>olders.</p>
 */
public class FNF
{
	/**
	 * Date format for use in backups.
	 */
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd 'at' HH.mm.ss z");
	private static final Pattern fileNameRegex = Pattern.compile("^hotbar\\.([-0-9]+)\\.?nbt$");
	/**
	 * The folder where additional saved hotbar files are stored.
	 */
	private static File directory = null;
	/**
	 * The folder where backups of saved hotbars are stored.
	 */
	private static File backupDirectory = null;

	/**
	 * Gets the location of the folder where additional saved hotbars are stored.
	 * @return	File
	 */
	public static File getHotbarFolder()
	{
		if (directory == null)
		{
			directory = new File(FabricLoader.getInstance().getGameDir().toFile(), "hotbars");

			if (!directory.isDirectory())
			{
				directory.mkdir();
			}
		}

		return directory;
	}

	/**
	 * Gets the location of the folder where backups of saved hotbars are stored.
	 * @return	File
	 */
	public static File getBackupFolder()
	{
		if (backupDirectory == null)
		{
			backupDirectory = new File(getHotbarFolder(), "backups");

			if (!backupDirectory.isDirectory())
			{
				backupDirectory.mkdir();
			}
		}

		return backupDirectory;
	}

	/**
	 * Get the file name for hotbar NBT files depending on the page number provided
	 * @param page	BigInteger
	 * @return		String
	 */
	public static String getPageFileName(BigInteger page)
	{
		return page.equals(BigInteger.ZERO) ? "hotbar.nbt" : String.format("hotbar.%s.nbt", page);
	}

	/**
	 * Get the file name for backups of hotbar NBT files depending on the page number provided
	 * @param page	BigInteger
	 * @return		String
	 */
	public static String getBackupFileName(BigInteger page)
	{
		return String.format("%s [%s].nbt", getPageFileName(page), dateFormat.format(new Date()));
	}

	public static File getFileForPage(BigInteger page)
	{
		return new File(page.equals(BigInteger.ZERO) ? FabricLoader.getInstance().getGameDir().toFile() :
				getHotbarFolder(), getPageFileName(page));
	}

	public static BigInteger getNumberFromFileName(String name)
	{
		final Matcher matcher = fileNameRegex.matcher(name);

		if (matcher.find())
		{
			String page = matcher.group(1);

			if (page != null)
			{
				return new BigInteger(page);
			}
		}

		return BigInteger.ZERO;
	}
}
