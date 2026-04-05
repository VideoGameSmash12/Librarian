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

package me.videogamesm12.librarian.v1_21_10.mixin;

import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.DataResult;
import me.videogamesm12.librarian.Librarian;
import me.videogamesm12.librarian.api.HotbarPageMetadata;
import me.videogamesm12.librarian.api.IWrappedHotbarStorage;
import me.videogamesm12.librarian.api.LoadStatus;
import me.videogamesm12.librarian.api.event.LoadFailureEvent;
import me.videogamesm12.librarian.api.event.SaveFailureEvent;
import me.videogamesm12.librarian.util.FNF;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.HotbarStorage;
import net.minecraft.client.option.HotbarStorageEntry;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.IntStream;

@Mixin(HotbarStorage.class)
public abstract class HotbarStorageMixin implements IWrappedHotbarStorage
{
	@Shadow @Final private Path file;

	@Shadow @Final private HotbarStorageEntry[] entries;

	@Shadow @Final private DataFixer dataFixer;

	@Shadow public abstract HotbarStorageEntry getSavedHotbar(int i);

	@Unique
	private static final GsonComponentSerializer librarian$serializer = GsonComponentSerializer.gson();

	@Unique
	private BigInteger pageNumber;

	@Unique
	private HotbarPageMetadata metadata = null;

	@Unique
	private int dataVersion = 0;

	@Unique
	private LoadStatus status = LoadStatus.NOT_LOADED;

	@Unique
	private int rowCount;

	/**
	 * <p>Hijacks what is used as the location by HotbarStorage on initialization.</p>
	 * @param ci        CallbackInfo
	 * @param dataFixer DataFixer
	 * @param file      Path
	 */
	@Inject(method = "<init>", at = @At(value = "TAIL"))
	private void hijackInitializer(Path file, DataFixer dataFixer, CallbackInfo ci)
	{
		this.pageNumber = FNF.getNumberFromFileName(file.toFile().getName());
		this.setFile(file);
		this.rowCount = entries.length;
	}

	/**
	 * Reimplements the hotbar page loading logic using existing optimizations and features.
	 * @author	Video
	 * @reason	Mod already heavily modifies game behavior, might as well just do it to keep things organized
	 */
	@Overwrite
	private void load()
	{
		// Indicate that we are now loading
		this.status = LoadStatus.LOADING;

		try
		{
			NbtCompound tag;

			// Try to load the page as a regular hotbar.nbt file, otherwise treat it as compressed
			try
			{
				tag = NbtIo.read(librarian$getLocation().toPath());
			}
			catch (NbtCrashException ex)
			{
				tag = NbtIo.readCompressed(Files.newInputStream(librarian$getLocation().toPath()), NbtSizeTracker.ofUnlimitedBytes());
			}

			// Usually means the file doesn't exist
			if (tag == null || tag.isEmpty())
			{
				status = LoadStatus.LOADED;
				setLoaded(true);
				return;
			}

			// Get and update the page's data version
			this.dataVersion = NbtHelper.getDataVersion(tag, 1343);
			tag = DataFixTypes.HOTBAR.update(dataFixer, tag, dataVersion);

			// Fetch our metadata
			tag.getCompound("librarian").ifPresent(meta ->
			{
				int version = meta.getInt("version").orElse(HotbarPageMetadata.getCurrentVersion());
				String name = meta.getString("name").orElse(null);
				String description = meta.getString("description").orElse(null);
				List<String> authors = new ArrayList<>(meta.getListOrEmpty("authors").stream()
						.map(NbtElement::asString).filter(Optional::isPresent).map(Optional::get).toList());

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
							.name(name != null && !name.isBlank() ? librarian$serializer.deserializeOrNull(name) : null)
							.description(description != null && !description.isBlank() ?
									librarian$serializer.deserializeOrNull(description) : null)
							.authors(authors)
							.build();
				}
			});

			final NbtCompound shutUpIntellij = tag;

			rowCount = Math.max(Math.toIntExact(tag.getKeys().stream().filter(key ->
					Objects.requireNonNull(shutUpIntellij.get(key)).asNbtList().isPresent()).count()), 9);
			setEntries(new HotbarStorageEntry[rowCount]);

			IntStream.range(0, rowCount).forEach(i -> loadRow(i, shutUpIntellij));
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
	 * @author	Video
	 * @reason	Same reason as load.
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
		dataVersion = SharedConstants.getGameVersion().dataVersion().id();

		// Actual operation, specified here as a Runnable so to avoid duplicate code
		final Runnable operation = () ->
		{
			try
			{
				// Create base compound tag
				final NbtCompound tag = NbtHelper.putDataVersion(new NbtCompound());

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
						metadata.getAuthors().forEach(author -> list.add(NbtString.of(author)));
						meta.put("authors", list);
					}

					tag.put("librarian", meta);
				}

				// Convert the items and add them to the tag
				IntStream.range(0, rowCount).forEach(i ->
				{
					final HotbarStorageEntry entry = getSavedHotbar(i);
					final DataResult<NbtElement> dataResult = HotbarStorageEntry.CODEC.encodeStart(NbtOps.INSTANCE, entry);
					tag.put(String.valueOf(i), dataResult.getOrThrow());
				});

				// Use file compression if enabled
				if (Librarian.getInstance().getConfig().optimizations().useFileCompression())
				{
					NbtIo.writeCompressed(tag, librarian$getLocation().toPath());
				}
				else
				{
					NbtIo.write(tag, librarian$getLocation().toPath());
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

	@Inject(method = "getSavedHotbar", at = @At("HEAD"), cancellable = true)
	private void preventPrematureHotbarGrabbing(int i, CallbackInfoReturnable<HotbarStorageEntry> cir)
	{
		if (Librarian.getInstance().getConfig().optimizations().backgroundLoading())
		{
			if (status == LoadStatus.NOT_LOADED)
			{
				librarian$loadAsync();
				cir.setReturnValue(new HotbarStorageEntry());
			}
			else if (status == LoadStatus.LOADING)
			{
				cir.setReturnValue(new HotbarStorageEntry());
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
		return file.toFile();
	}

	@Override
	public void librarian$load()
	{
		load();
	}

	@Override
	public void librarian$preprocess()
	{
		Arrays.stream(entries).forEach(entry ->
				entry.deserialize(Objects.requireNonNull(MinecraftClient.getInstance().world).getRegistryManager()));
	}

	@Unique
	private void loadRow(int row, NbtCompound source)
	{
		this.entries[row] = HotbarStorageEntry.CODEC.parse(NbtOps.INSTANCE, source.get(String.valueOf(row)))
				.resultOrPartial(error -> Librarian.getLogger().warn("Failed to parse hotbar: {}", error))
				.orElseGet(HotbarStorageEntry::new);
	}

	@Accessor
	public abstract void setFile(Path file);

	@Accessor
	public abstract void setLoaded(boolean loaded);

	@Accessor
	public abstract void setEntries(HotbarStorageEntry[] entries);
}
