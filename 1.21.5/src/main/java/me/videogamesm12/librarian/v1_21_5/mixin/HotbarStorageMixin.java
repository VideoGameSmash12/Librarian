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

package me.videogamesm12.librarian.v1_21_5.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.DataFixer;
import me.videogamesm12.librarian.Librarian;
import me.videogamesm12.librarian.api.HotbarPageMetadata;
import me.videogamesm12.librarian.api.IWrappedHotbarStorage;
import me.videogamesm12.librarian.api.event.LoadFailureEvent;
import me.videogamesm12.librarian.api.event.SaveFailureEvent;
import me.videogamesm12.librarian.v1_21_5.WrappedHotbarStorage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.client.option.HotbarStorage;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mixin(HotbarStorage.class)
public abstract class HotbarStorageMixin implements IWrappedHotbarStorage
{
	@Unique
	private static final GsonComponentSerializer librarian$serializer = GsonComponentSerializer.gson();

	@Unique
	private HotbarPageMetadata metadata = null;

	/**
	 * <p>Hijacks what is used as the location by HotbarStorage on initialization.</p>
	 * @param ci        CallbackInfo
	 * @param dataFixer DataFixer
	 * @param file      Path
	 */
	@Inject(method = "<init>", at = @At(value = "TAIL"))
	private void hijackInitializer(Path file, DataFixer dataFixer, CallbackInfo ci)
	{
		if (WrappedHotbarStorage.class.isAssignableFrom(getClass()))
		{
			this.setFile(file);
		}
	}

	@Inject(method = "load", at = @At(value = "INVOKE",
			target = "Lorg/slf4j/Logger;error(Ljava/lang/String;Ljava/lang/Throwable;)V", shift = At.Shift.AFTER, remap = false))
	private void hookLoadFailure(CallbackInfo ci, @Local Exception ex)
	{
		if (WrappedHotbarStorage.class.isAssignableFrom(getClass()))
		{
			Librarian.getInstance().getEventBus().post(new LoadFailureEvent(this, ex));
		}
	}

	@Inject(method = "save", at = @At(value = "INVOKE",
			target = "Lorg/slf4j/Logger;error(Ljava/lang/String;Ljava/lang/Throwable;)V", shift = At.Shift.AFTER, remap = false))
	private void hookSaveFailure(CallbackInfo ci, @Local Exception ex)
	{
		if (WrappedHotbarStorage.class.isAssignableFrom(getClass()))
		{
			Librarian.getInstance().getEventBus().post(new SaveFailureEvent(this, ex));
		}
	}

	@Inject(method = "load", at = @At(value = "INVOKE", target = "Lnet/minecraft/datafixer/DataFixTypes;update(Lcom/mojang/datafixers/DataFixer;Lnet/minecraft/nbt/NbtCompound;I)Lnet/minecraft/nbt/NbtCompound;",
			shift = At.Shift.AFTER))
	private void fetchMetadata(CallbackInfo ci, @Local NbtCompound compound)
	{
		if (WrappedHotbarStorage.class.isAssignableFrom(getClass()))
		{
			Optional<NbtCompound> meta = compound.getCompound("librarian");

			meta.ifPresent(librarianMeta ->
			{
				int version = librarianMeta.getInt("version").orElse(HotbarPageMetadata.getCurrentVersion());
				String name = librarianMeta.getString("name").orElse(null);
				String description = librarianMeta.getString("description").orElse(null);
				List<String> authors = new ArrayList<>(librarianMeta.getListOrEmpty("authors").stream()
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
	}

	@Inject(method = "save", at = @At(value = "INVOKE", target =
			"Lnet/minecraft/nbt/NbtIo;write(Lnet/minecraft/nbt/NbtCompound;Ljava/nio/file/Path;)V",
			shift = At.Shift.BEFORE))
	private void addMetadata(CallbackInfo ci, @Local NbtCompound compound)
	{
		if (WrappedHotbarStorage.class.isAssignableFrom(getClass()) && metadata != null)
		{
			NbtCompound meta = new NbtCompound();

			meta.putInt("version", HotbarPageMetadata.getCurrentVersion());

			if (metadata.getName() != null)
				meta.putString("name", librarian$serializer.serialize(metadata.getName()));

			if (metadata.getDescription() != null)
				meta.putString("description", librarian$serializer.serialize(metadata.getDescription()));

			if (metadata.getAuthors() != null && !metadata.getAuthors().isEmpty())
			{
				final NbtList list = new NbtList();
				metadata.getAuthors().forEach(author -> list.add(NbtString.of(author)));
				meta.put("authors", list);
			}

			compound.put("librarian", meta);
		}
	}

	@Override
	public Optional<HotbarPageMetadata> getMetadata()
	{
		if (!this.isLoaded())
		{
			((HotbarStorageInvoker) this).invokeLoad();
			this.setLoaded(true);
		}

		return Optional.ofNullable(metadata);
	}

	@Override
	public void setMetadata(HotbarPageMetadata newMeta)
	{
		metadata = newMeta;
	}

	@Accessor
	public abstract void setFile(Path file);

	@Accessor
	public abstract boolean isLoaded();

	@Accessor
	public abstract void setLoaded(boolean loaded);
}
