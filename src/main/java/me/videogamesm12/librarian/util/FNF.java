package me.videogamesm12.librarian.util;

import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FNF
{
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd 'at' HH.mm.ss z");
	private static File directory = null;
	private static File backupDirectory = null;

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

	public static String getPageFileName(BigInteger page)
	{
		return page.equals(BigInteger.ZERO) ? "hotbar.nbt" : String.format("hotbar.%s.nbt", page);
	}

	public static String getBackupFileName(BigInteger page)
	{
		return String.format("%s [%s].nbt", getPageFileName(page), dateFormat.format(new Date()));
	}
}
