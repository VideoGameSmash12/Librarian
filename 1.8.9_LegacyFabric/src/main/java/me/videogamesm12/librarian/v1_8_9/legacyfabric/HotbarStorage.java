package me.videogamesm12.librarian.v1_8_9.legacyfabric;

import me.videogamesm12.librarian.Librarian;
import me.videogamesm12.librarian.api.IWrappedHotbarStorage;
import me.videogamesm12.librarian.util.FNF;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.math.BigInteger;

public class HotbarStorage implements IWrappedHotbarStorage
{
	private final File location;

	public HotbarStorage(BigInteger page)
	{
		this.location = new File(page.equals(BigInteger.ZERO) ? FabricLoader.getInstance().getGameDir().toFile() :
				FNF.getHotbarFolder(), FNF.getPageFileName(page));
	}

	@Override
	public File getLocation()
	{
		return null;
	}

	@Override
	public BigInteger getPageNumber()
	{
		return null;
	}

	@Override
	public void load()
	{

	}
}
