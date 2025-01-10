package me.videogamesm12.librarian.v1_14_4;

import me.videogamesm12.librarian.api.IWrappedHotbarStorage;
import me.videogamesm12.librarian.util.FNF;
import me.videogamesm12.librarian.v1_14_4.mixin.HotbarStorageAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.options.HotbarStorage;

import java.io.File;
import java.math.BigInteger;

public class WrappedHotbarStorage extends HotbarStorage implements IWrappedHotbarStorage
{
	private final BigInteger page;
	private final File location;

	public WrappedHotbarStorage(BigInteger page)
	{
		super(new File(FNF.getHotbarFolder(), FNF.getPageFileName(page)), MinecraftClient.getInstance().getDataFixer());

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
		((HotbarStorageAccessor) this).invokeLoad();
	}
}
