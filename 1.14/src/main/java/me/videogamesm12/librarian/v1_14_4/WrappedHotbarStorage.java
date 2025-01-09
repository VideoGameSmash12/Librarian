package me.videogamesm12.librarian.v1_14_4;

import me.videogamesm12.librarian.Librarian;
import me.videogamesm12.librarian.api.IWrappedHotbarStorage;
import me.videogamesm12.librarian.util.FNF;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.options.HotbarStorage;

import java.io.File;
import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;

public class WrappedHotbarStorage extends HotbarStorage implements IWrappedHotbarStorage
{
	public WrappedHotbarStorage(BigInteger page)
	{
		super(new File(FNF.getHotbarFolder(), FNF.getPageFileName(page)), MinecraftClient.getInstance().getDataFixer());
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
		throw new RuntimeException("lol");
	}
}
