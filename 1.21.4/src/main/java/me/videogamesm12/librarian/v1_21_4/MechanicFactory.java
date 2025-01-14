package me.videogamesm12.librarian.v1_21_4;

import com.google.common.collect.Lists;
import com.mojang.serialization.Dynamic;
import lombok.NonNull;
import me.videogamesm12.librarian.Librarian;
import me.videogamesm12.librarian.api.IMechanicFactory;
import me.videogamesm12.librarian.api.IWrappedHotbarStorage;
import me.videogamesm12.librarian.util.FNF;
import net.fabricmc.loader.api.FabricLoader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.MinecraftVersion;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.HotbarStorage;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MechanicFactory implements IMechanicFactory
{
	private static final GsonComponentSerializer GSON_COMPONENT_SERIALIZER = GsonComponentSerializer.gson();
	private static final RegistryWrapper.WrapperLookup WRAPPER_LOOKUP = BuiltinRegistries.createWrapperLookup();

	@Override
	public IWrappedHotbarStorage createHotbarStorage(@NonNull BigInteger integer)
	{
		return new WrappedHotbarStorage(integer);
	}

	@Override
	public ButtonWidget createButton(int x, int y, int width, int height, Component label, Component tooltip, Runnable onClick)
	{
		return ButtonWidget.builder(createText(label), button -> onClick.run()).dimensions(x, y,
				width, height).tooltip(tooltip != null ? Tooltip.of(createText(tooltip)) : null).build();
	}

	@Override
	public Text createText(Component component)
	{
		return Text.Serialization.fromJsonTree(GSON_COMPONENT_SERIALIZER.serializeToTree(component), WRAPPER_LOOKUP);
	}

	@Override
	public void overhaulHotbars(List<File> files)
	{
		Librarian.getLogger().info("Starting hotbar overhaul. This process may take a while depending on how many pages you have.");

		// Clear the cache
		Librarian.getInstance().clearCache();

		// Backup the files beforehand
		final File backupFolder = new File(FNF.getBackupFolder(), String.valueOf(new Date().getTime()));
		if (!backupFolder.exists())
		{
			backupFolder.mkdirs();
		}

		Librarian.getLogger().info("Backup folder created. It is located at {}.", backupFolder.getAbsolutePath());

		for (File file : files)
		{
			if (file.exists())
			{
				try
				{
					Librarian.getLogger().info("Backing up file {} ({} of {})...", file.getName(), files.indexOf(file) + 1, files.size());
					Files.copy(file.toPath(), new File(backupFolder, file.getName()).toPath());
				}
				catch (IOException ex)
				{
					Librarian.getLogger().error("Aborting overhaul - backing up file {} ({} of {}) failed with exception", file.getName(), files.indexOf(file) + 1, files.size(), ex);
					return;
				}
			}
			else
			{
				Librarian.getLogger().error("Aborting overhaul - hotbar file {} does not exist.", file.getName());
				return;
			}
		}

		Librarian.getLogger().info("All files have been backed up successfully. Preparing to load hotbar pages now...");
		final List<ItemStack> items = new ArrayList<>();

		for (File file : files)
		{
			try
			{
				Librarian.getLogger().info("Reading hotbar file {}", file.getName());
				NbtCompound compound = NbtIo.read(file.toPath());

				for (int i = 0; i < HotbarStorage.STORAGE_ENTRY_COUNT; i++)
				{
					NbtList list = Objects.requireNonNull(compound).getList(String.valueOf(i), 10);

					for (int column = 0; column < PlayerInventory.getHotbarSize(); column++)
					{
						ItemStack.fromNbt(WRAPPER_LOOKUP, list.getCompound(column)).ifPresent(itemStack ->
						{
							if (items.stream().noneMatch(stack -> ItemStack.areItemsAndComponentsEqual(stack, itemStack)))
							{
								items.add(itemStack.copy());
							}
						});
					}
				}

				Librarian.getLogger().info("Imported items added from file {}", file.getName());
			}
			// File is probablty corrupt and can be ignored
			catch (EOFException ex)
			{
				continue;
			}
			catch (Throwable ex)
			{
				Librarian.getLogger().error("Aborting overhaul - exception occurred whilst attempting to load file {}", file.getName(), ex);
				return;
			}
		}

		Librarian.getLogger().info("All hotbar items have been loaded. Deleting...");
		files.forEach(File::delete);

		Librarian.getLogger().info("Now we rebuild");
		Librarian.getInstance().setPage(0);

		NbtCompound pageCompound = new NbtCompound();
		int rowNum = 0;
		Librarian.getLogger().info("Rebuilding page {}", Librarian.getInstance().getCurrentPageNumber().add(BigInteger.ONE));
		for (List<ItemStack> row : Lists.partition(items, 9))
		{
			if (rowNum == 9)
			{
				pageCompound.putInt("DataVersion", MinecraftVersion.CURRENT.getSaveVersion().getId());
				try
				{
					NbtIo.write(pageCompound, new File(Librarian.getInstance().getCurrentPageNumber().equals(BigInteger.ZERO) ?
							FabricLoader.getInstance().getGameDir().toFile() : FNF.getHotbarFolder(), FNF.getPageFileName(Librarian.getInstance().getCurrentPageNumber())).toPath());
				}
				catch (Throwable ex)
				{
					Librarian.getLogger().error("Failed to write new hotbar page", ex);
				}
				//--
				Librarian.getInstance().nextPage();
				rowNum = 0;
				pageCompound = new NbtCompound();
				Librarian.getLogger().info("Rebuilding page {}", Librarian.getInstance().getCurrentPageNumber().add(BigInteger.ONE));
			}

			final NbtList nbtList = new NbtList();
			for (ItemStack entry : row)
			{
				nbtList.add(entry.toNbt(WRAPPER_LOOKUP));
			}
			pageCompound.put(String.valueOf(rowNum), nbtList);

			rowNum++;
		}

		/*while (!partitions.isEmpty())
		{
			Librarian.getInstance().nextPage();
			Librarian.getLogger().info("Rebuilding page {} of {}", Librarian.getInstance().getCurrentPageNumber().add(BigInteger.ONE), partitions.size());

			final List<List<ItemStack>> page = partitions.poll();

			final NbtCompound pageCompound = new NbtCompound();

			for (int row = 0; row < Objects.requireNonNull(page).size(); row++)
			{
				NbtList list = new NbtList();
				List<ItemStack> r = page.get(row);

				for (ItemStack itemStack : r)
				{
					list.add(itemStack.toNbtAllowEmpty(WRAPPER_LOOKUP));
				}

				pageCompound.put(String.valueOf(row), list);
			}

			pageCompound.putInt("DataVersion", MinecraftVersion.CURRENT.getSaveVersion().getId());


		}*/

		Librarian.getLogger().info("Rebuild complete");
	}
}
