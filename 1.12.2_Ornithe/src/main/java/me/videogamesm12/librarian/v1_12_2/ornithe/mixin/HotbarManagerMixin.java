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

package me.videogamesm12.librarian.v1_12_2.ornithe.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.videogamesm12.librarian.Librarian;
import me.videogamesm12.librarian.api.HotbarPageMetadata;
import me.videogamesm12.librarian.api.IWrappedHotbarStorage;
import me.videogamesm12.librarian.api.LoadStatus;
import me.videogamesm12.librarian.api.event.LoadFailureEvent;
import me.videogamesm12.librarian.api.event.SaveFailureEvent;
import me.videogamesm12.librarian.util.FNF;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.client.Hotbar;
import net.minecraft.client.HotbarManager;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.EOFException;
import java.io.File;
import java.io.UTFDataFormatException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

@Mixin(HotbarManager.class)
public abstract class HotbarManagerMixin implements IWrappedHotbarStorage
{
	@Shadow @Final private File file;

	@Shadow @Final private Hotbar[] hotbars;

	@Unique
	private static final GsonComponentSerializer librarian$serializer = GsonComponentSerializer.colorDownsamplingGson();

	@Unique
	private BigInteger pageNumber;

	@Unique
	private HotbarPageMetadata metadata = null;

	@Unique
	private int dataVersion = 1343;

	@Unique
	private LoadStatus status = LoadStatus.NOT_LOADED;

	@Unique
	private int rowCount = 0;

	/**
	 * <p>Hijacks what is used as the location by HotbarStorage on initialization.</p>
	 * @param instance  Minecraft
	 * @param file      File
	 * @param ci        CallbackInfo
	 */
	@Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/HotbarManager;load()V", shift = At.Shift.BEFORE))
	private void change(Minecraft instance, File file, CallbackInfo ci)
	{
		this.pageNumber = FNF.getNumberFromFileName(file.getName());
		this.setFile(file);
		this.rowCount = hotbars.length;
	}

	@WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/HotbarManager;load()V"))
	private void doNotLoadOnInit(HotbarManager instance, Operation<Void> original)
	{
		// Nuh uh. Not gonna do it.
	}

	/**
	 * Reimplements the hotbar page loading logic using existing optimizations and features.
	 * @author Video
	 * @reason Mod already heavily modifies game behavior, might as well just do it to keep things organized
	 */
	@Overwrite
	public void load()
	{
		// Indicate that we are now loading
		this.status = LoadStatus.LOADING;

		try
		{
			NbtCompound tag;

			// Try to load the page as a regular hotbar.nbt file, otherwise treat it as compressed
			try
			{
				tag = NbtIo.read(librarian$getLocation());
			}
			catch (UTFDataFormatException | EOFException ex)
			{
				tag = NbtIo.readCompressed(Files.newInputStream(librarian$getLocation().toPath()));
			}

			// Usually means the file doesn't exist
			if (tag == null || tag.isEmpty())
			{
				status = LoadStatus.LOADED;
				setLoaded(true);
				return;
			}

			// Get and update the page's data version
			this.dataVersion = tag.getInt("DataVersion") != 0 ? tag.getInt("DataVersion") : 1343;

			// Fetch our metadata
			NbtCompound meta = tag.getCompound("librarian");

			if (meta != null)
			{
				int version = meta.getInt("version");
				String name = meta.getString("name");
				String description = meta.getString("description");
				List<String> authors = new ArrayList<>();

				NbtList nbtList = meta.getList("authors", 8);
				for (int i = 0; i < nbtList.size(); i++)
				{
					authors.add(nbtList.getString(i));
				}

				if (version > HotbarPageMetadata.getCurrentVersion())
				{
					Librarian.getLogger().error("Hotbar metadata rejected - data is intended for a newer version of " +
									"Librarian than what we are currently running (current version {}, file version {})",
							HotbarPageMetadata.getCurrentVersion(), version);

					metadata = HotbarPageMetadata.builder().build();
				}
				else
				{
					metadata = HotbarPageMetadata.builder()
							.version(version)
							.name(name != null && !name.isEmpty() ? librarian$serializer.deserializeOrNull(name) : null)
							.description(description != null && !description.isEmpty() ?
									librarian$serializer.deserializeOrNull(description) : null)
							.authors(authors)
							.build();
				}
			}

			final NbtCompound shutUpIntellij = tag;

			rowCount = Math.max(Math.toIntExact(tag.getKeys().stream().filter(key ->
					shutUpIntellij.contains(key, 9)).count()), 9);
			setHotbars(new Hotbar[rowCount]);

			IntStream.range(0, rowCount).forEach(i ->
			{
				this.hotbars[i] = new Hotbar();
				this.hotbars[i].readNbt(shutUpIntellij.getList(String.valueOf(i), 10));
			});
		}
		catch (Throwable ex)
		{
			Librarian.getLogger().error("Failed to load saved hotbar page {}", librarian$getLocation().getName(), ex);
			Librarian.getInstance().getEventBus().post(new LoadFailureEvent(this, ex));
		}

		// Ok, we're done
		status = LoadStatus.LOADED;
		setLoaded(true);
	}

	/**
	 * Reimplements the hotbar page loading logic using existing optimizations and features.
	 * @author Video
	 * @reason Same reason as load.
	 */
	@Overwrite
	public void save()
	{
		// Do nothing if the page isn't even loaded yet and we're loading asynchronously
		if (status != LoadStatus.LOADED && Librarian.getInstance().getConfig().optimizations().backgroundLoading())
		{
			return;
		}

		// Update the data version
		dataVersion = 1343;

		// Actual operation, specified here as a Runnable so to avoid duplicate code
		final Runnable operation = () ->
		{
			try
			{
				// Create base compound tag
				final NbtCompound tag = new NbtCompound();

				// Write our metadata
				if (metadata != null)
				{
					NbtCompound meta = new NbtCompound();

					meta.putInt("version", HotbarPageMetadata.getCurrentVersion());

					if (metadata.getName() != null)
						meta.putString("name", librarian$serializer.serialize(metadata.getName()));

					if (metadata.getDescription() != null)
						meta.putString("description", librarian$serializer.serialize(metadata.getDescription()));

					if (!metadata.getAuthors().isEmpty())
					{
						final NbtList list = new NbtList();
						metadata.getAuthors().forEach(author -> list.add(new NbtString(author)));
						meta.put("authors", list);
					}

					tag.put("librarian", meta);
				}

				// Convert the items and add them to the tag
				IntStream.range(0, rowCount).forEach(i -> tag.put(String.valueOf(i), hotbars[i].toNbt()));

				// Use file compression if enabled
				if (Librarian.getInstance().getConfig().optimizations().useFileCompression())
				{
					NbtIo.writeCompressed(tag, Files.newOutputStream(librarian$getLocation().toPath()));
				}
				else
				{
					NbtIo.write(tag, librarian$getLocation());
				}
			}
			catch (Throwable ex)
			{
				Librarian.getLogger().error("Failed to save hotbar page {}", librarian$getLocation().getName(), ex);
				Librarian.getInstance().getEventBus().post(new SaveFailureEvent(this, ex));
			}
		};

		if (Thread.currentThread().getName().startsWith("pool-") || !Librarian.getInstance().getConfig().optimizations().backgroundSaving())
		{
			operation.run();
		}
		else
		{
			Librarian.getInstance().queue(operation);
		}
	}

	@Inject(method = "get", at = @At("HEAD"), cancellable = true)
	private void preventPrematureHotbarGrabbing(int i, CallbackInfoReturnable<Hotbar> cir)
	{
		if (Librarian.getInstance().getConfig().optimizations().backgroundLoading())
		{
			if (status == LoadStatus.NOT_LOADED)
			{
				librarian$loadAsync();
				cir.setReturnValue(new Hotbar());
			}
			else if (status == LoadStatus.LOADING)
			{
				cir.setReturnValue(new Hotbar());
			}
		}
		else
		{
			if (status == LoadStatus.NOT_LOADED)
			{
				load();
			}
		}
	}

	@Override
	public LoadStatus librarian$getLoadStatus()
	{
		return status;
	}

	@Override
	public int librarian$getRowCount()
	{
		return rowCount;
	}

	@Override
	public int librarian$dataVersion()
	{
		return dataVersion;
	}

	@Override
	public Optional<HotbarPageMetadata> librarian$getMetadata()
	{
		return Optional.ofNullable(metadata);
	}

	@Override
	public void librarian$setMetadata(HotbarPageMetadata newMeta)
	{
		metadata = newMeta;
	}

	@Override
	public BigInteger librarian$getPageNumber()
	{
		return pageNumber;
	}

	@Override
	public File librarian$getLocation()
	{
		return file;
	}

	@Override
	public void librarian$load()
	{
		load();
	}

	@Accessor
	public abstract void setFile(File file);

	@Override
	public void setLoaded(boolean loaded)
	{
		// We don't use this in 1.12.2 lmao
	}

	@Accessor
	public abstract void setHotbars(Hotbar[] entries);
}
