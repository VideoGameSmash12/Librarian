package me.videogamesm12.librarian.v1_21_4;

import me.videogamesm12.librarian.api.IWrappedHotbarStorage;
import me.videogamesm12.librarian.util.FNF;
import me.videogamesm12.librarian.v1_21_4.mixin.HotbarStorageInvoker;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.HotbarStorage;

import java.io.File;
import java.math.BigInteger;

public class WrappedHotbarStorage extends HotbarStorage implements IWrappedHotbarStorage
{
	private final File file;
	private final BigInteger page;

	public WrappedHotbarStorage(BigInteger page)
	{
		super(new File(page.equals(BigInteger.ZERO) ? FabricLoader.getInstance().getGameDir().toFile() :
				FNF.getHotbarFolder(), FNF.getPageFileName(page)).toPath(),
				MinecraftClient.getInstance().getDataFixer());

		this.file = new File(page.equals(BigInteger.ZERO) ? FabricLoader.getInstance().getGameDir().toFile() :
				FNF.getHotbarFolder(), FNF.getPageFileName(page));
		this.page = page;
	}

	@Override
	public File getLocation()
	{
		return file;
	}

	@Override
	public BigInteger getPageNumber()
	{
		return page;
	}

	@Override
	public void load()
	{
		((HotbarStorageInvoker) this).invokeLoad();
		setLoaded(true);
	}
}
