package me.videogamesm12.librarian.v1_13_2.legacyfabric;

import me.videogamesm12.librarian.api.IWrappedHotbarStorage;
import me.videogamesm12.librarian.util.FNF;
import me.videogamesm12.librarian.v1_13_2.legacyfabric.mixin.HotbarStorageAccessor;
import net.minecraft.class_3251;
import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.math.BigInteger;

public class WrappedHotbarStorage extends class_3251 implements IWrappedHotbarStorage
{
	private final BigInteger page;
	private final File location;

	public WrappedHotbarStorage(BigInteger page)
	{
		super(new File(FNF.getHotbarFolder(), FNF.getPageFileName(page)), MinecraftClient.getInstance().method_12142());

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
