package me.videogamesm12.librarian.v1_8_9.legacyfabric;

import lombok.Getter;
import me.videogamesm12.librarian.Librarian;
import me.videogamesm12.librarian.api.IWrappedHotbarStorage;
import me.videogamesm12.librarian.util.FNF;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HotbarStorage implements IWrappedHotbarStorage
{
	private static final ItemStack EMPTY = new ItemStack(Blocks.AIR);

	@Getter
	private final File location;
	@Getter
	private final BigInteger pageNumber;
	//--
	private HotbarStorageEntry[] entries = new HotbarStorageEntry[9];
	private boolean isLoaded;

	public HotbarStorage(BigInteger page)
	{
		this.location = new File(page.equals(BigInteger.ZERO) ? FabricLoader.getInstance().getGameDir().toFile() :
				FNF.getHotbarFolder(), FNF.getPageFileName(page));
		this.pageNumber = page;

		for (int i = 0; i < 9; i++)
		{
			entries[i] = new HotbarStorageEntry();
		}
	}

	public List<ItemStack> getAllItems()
	{
		if (!isLoaded)
		{
			load();
			isLoaded = true;
		}

		final List<ItemStack> list = new ArrayList<>();
		Arrays.stream(entries).forEach(list::addAll);
		return list;
	}

	public HotbarStorageEntry getEntry(int index)
	{
		if (!isLoaded)
		{
			load();
			isLoaded = true;
		}

		return entries[index];
	}

	@Override
	public void load()
	{
		// Nothing to load, nothing to bother with
		if (!this.location.exists())
		{
			return;
		}

		try
		{
			final NbtCompound nbt = NbtIo.read(this.location);

			for (int i = 0; i < 9; i++)
			{
				entries[i].load(nbt.getList(String.valueOf(i), 10));
			}
		}
		catch (IOException ex)
		{
			Librarian.getLogger().error("Failed to load saved hotbar page {}", pageNumber, ex);
		}
	}

	public void save()
	{
		NbtCompound compound = new NbtCompound();

		for (int i = 0; i < 9; i++)
		{
			compound.put(String.valueOf(i), entries[i].save());
		}

		compound.putInt("DataVersion", 0);

		try
		{
			NbtIo.safeWrite(compound, this.location);
		}
		catch (IOException ex)
		{
			Librarian.getLogger().error("Failed to write hotbar page {}", pageNumber, ex);
		}
	}

	public static class HotbarStorageEntry extends ArrayList<ItemStack>
	{
		public HotbarStorageEntry()
		{
			for (int i = 0; i < 9; i++)
			{
				add(null);
				//add(new ItemStack(Blocks.AIR));
			}
		}

		public void load(NbtList list)
		{
			clear();

			for (int i = 0; i < 9; i++)
			{
				NbtElement element = list.get(i);

				if (element instanceof NbtCompound)
				{
					NbtCompound compound = list.getCompound(i);
					add(ItemStack.fromNbt(compound));
				}
				else
				{
					add(new ItemStack(Blocks.AIR));
				}
			}
		}

		public NbtList save()
		{
			final NbtList list = new NbtList();

			for (ItemStack item : this)
			{
				NbtCompound compound = new NbtCompound();

				if (item != null)
				{
					item.toNbt(compound);
				}
				else
				{
					EMPTY.toNbt(compound);
				}

				list.add(compound);
			}

			return list;
		}
	}
}
