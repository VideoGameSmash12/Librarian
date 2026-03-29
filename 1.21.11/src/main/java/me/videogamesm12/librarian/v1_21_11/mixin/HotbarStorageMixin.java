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

package me.videogamesm12.librarian.v1_21_11.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.DataFixer;
import me.videogamesm12.librarian.Librarian;
import me.videogamesm12.librarian.api.HotbarPageMetadata;
import me.videogamesm12.librarian.api.IWrappedHotbarStorage;
import me.videogamesm12.librarian.api.event.LoadFailureEvent;
import me.videogamesm12.librarian.api.event.SaveFailureEvent;
import me.videogamesm12.librarian.util.FNF;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.HotbarStorage;
import net.minecraft.nbt.*;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mixin(HotbarStorage.class)
public abstract class HotbarStorageMixin implements IWrappedHotbarStorage
{
	@Shadow @Final private Path file;

	@Shadow protected abstract void load();

	@Shadow private boolean loaded;

	@Unique
	private static final GsonComponentSerializer librarian$serializer = GsonComponentSerializer.gson();

	@Unique
	private BigInteger pageNumber;

	@Unique
	private HotbarPageMetadata metadata = null;

	@Unique
	private int dataVersion = 0;

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
	}

	@Inject(method = "load", at = @At(value = "INVOKE",
			target = "Lorg/slf4j/Logger;error(Ljava/lang/String;Ljava/lang/Throwable;)V", shift = At.Shift.AFTER, remap = false))
	private void hookLoadFailure(CallbackInfo ci, @Local Exception ex)
	{
		Librarian.getInstance().getEventBus().post(new LoadFailureEvent(this, ex));
	}

	@Inject(method = "save", at = @At(value = "INVOKE",
			target = "Lorg/slf4j/Logger;error(Ljava/lang/String;Ljava/lang/Throwable;)V", shift = At.Shift.AFTER, remap = false))
	private void hookSaveFailure(CallbackInfo ci, @Local Exception ex)
	{
		Librarian.getInstance().getEventBus().post(new SaveFailureEvent(this, ex));
	}

	@Inject(method = "load", at = @At("RETURN"))
	private void markLoaded(CallbackInfo ci)
	{
		setLoaded(true);
	}

	@Inject(method = "load", at = @At(value = "INVOKE", target = "Lnet/minecraft/datafixer/DataFixTypes;update(Lcom/mojang/datafixers/DataFixer;Lnet/minecraft/nbt/NbtCompound;I)Lnet/minecraft/nbt/NbtCompound;",
			shift = At.Shift.AFTER))
	private void fetchData(CallbackInfo ci, @Local int dataVersion, @Local NbtCompound compound)
	{
		// Store the dataVersion of the hotbar from disk
		this.dataVersion = dataVersion;

		// If present, fetch our own metadata as well
		compound.getCompound("librarian").ifPresent(meta ->
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
	}

	@Inject(method = "save", at = @At(value = "INVOKE", target =
			"Lnet/minecraft/nbt/NbtIo;write(Lnet/minecraft/nbt/NbtCompound;Ljava/nio/file/Path;)V",
			shift = At.Shift.BEFORE))
	private void addData(CallbackInfo ci, @Local NbtCompound compound)
	{
		// Update the data version
		dataVersion = SharedConstants.getGameVersion().dataVersion().id();

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

			compound.put("librarian", meta);
		}
	}

	@WrapOperation(method = "load", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/nbt/NbtIo;read(Ljava/nio/file/Path;)Lnet/minecraft/nbt/NbtCompound;"))
	public NbtCompound addSupportForLoadingCompressedPages(Path path, Operation<NbtCompound> original) throws IOException
	{
		// The way this works is rather simple. NbtIo.read will throw an exception if it fails to read a file. We can
		// 	leverage this by putting the call for NbtIo.read into a try-catch block to make it try to read the file as a
		//	compressed file if it fails to read it as a regular file. This effectively makes the game support both
		//	regular hotbar files and compressed files.

		try
		{
			return original.call(path);
		}
		catch (NbtCrashException ex)
		{
			return NbtIo.readCompressed(Files.newInputStream(path), NbtSizeTracker.ofUnlimitedBytes());
		}
	}

	@WrapOperation(method = "save", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/nbt/NbtIo;write(Lnet/minecraft/nbt/NbtCompound;Ljava/nio/file/Path;)V"))
	public void addSupportForSavingCompressedPages(NbtCompound nbtCompound, Path path, Operation<Void> original) throws IOException
	{
		if (Librarian.getInstance().getConfig().optimizations().useFileCompression())
		{
			NbtIo.writeCompressed(nbtCompound, path);
		}
		else
		{
			original.call(nbtCompound, path);
		}
	}

	@WrapMethod(method = "save")
	public void savesAsyncIfEnabled(Operation<Void> original)
	{
		if (Librarian.getInstance().getConfig().optimizations().saveAsynchronously())
		{
			Librarian.getInstance().queue(() ->
			{
				long startTime = System.currentTimeMillis();
				original.call();
				long endTime = System.currentTimeMillis();

				MinecraftClient mc = MinecraftClient.getInstance();
				mc.execute(() ->
				{
					if (mc.world != null)
					{
						final Text message = Text.translatable("librarian.messages.saving.completed",
								endTime - startTime);

						mc.inGameHud.setOverlayMessage(message, false);
					}
				});
			});
		}
		else
		{
			original.call();
		}
	}

	@Override
	public int librarian$dataVersion()
	{
		return dataVersion;
	}

	@Override
	public Optional<HotbarPageMetadata> librarian$getMetadata()
	{
		if (!loaded)
		{
			load();
		}

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

	@Accessor
	public abstract void setFile(Path file);

	@Accessor
	public abstract void setLoaded(boolean loaded);
}
