package me.videogamesm12.librarian.v1_13_2.ornithe;

import me.videogamesm12.librarian.api.IWrappedHotbarStorage;
import me.videogamesm12.librarian.util.FNF;
import me.videogamesm12.librarian.v1_13_2.ornithe.mixin.HotbarManagerAccessor;
import net.minecraft.client.HotbarManager;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.math.BigInteger;

public class WrappedHotbarStorage extends HotbarManager implements IWrappedHotbarStorage
{
	private final BigInteger page;
	private final File location;

	public WrappedHotbarStorage(BigInteger page)
	{
		super(new File(FNF.getHotbarFolder(), FNF.getPageFileName(page)), Minecraft.getInstance().getDataFixerUpper());

		this.page = page;
		this.location = new File(FNF.getHotbarFolder(), FNF.getPageFileName(page));
	}

	@Override
	public File getLocation()
	{
		return location;
	}

	@Override
	public BigInteger getPageNumber()
	{
		return page;
	}

	@Override
	public void load()
	{
		((HotbarManagerAccessor) this).invokeLoad();
	}
}
