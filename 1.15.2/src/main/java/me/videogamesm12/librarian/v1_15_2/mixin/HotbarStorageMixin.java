package me.videogamesm12.librarian.v1_15_2.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.DataFixer;
import me.videogamesm12.librarian.Librarian;
import me.videogamesm12.librarian.api.HotbarPageMetadata;
import me.videogamesm12.librarian.api.IWrappedHotbarStorage;
import me.videogamesm12.librarian.api.event.LoadFailureEvent;
import me.videogamesm12.librarian.api.event.SaveFailureEvent;
import me.videogamesm12.librarian.v1_15_2.WrappedHotbarStorage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.client.options.HotbarStorage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Mixin(HotbarStorage.class)
public abstract class HotbarStorageMixin implements IWrappedHotbarStorage
{
	@Unique
	private static final GsonComponentSerializer librarian$serializer = GsonComponentSerializer.colorDownsamplingGson();

	@Unique
	private HotbarPageMetadata metadata = null;

	/**
	 * <p>Hijacks what is used as the location by HotbarStorage on initialization.</p>
	 * @param ci        CallbackInfo
	 * @param dataFixer DataFixer
	 * @param file      Path
	 */
	@Inject(method = "<init>", at = @At(value = "TAIL"))
	private void hijackInitializer(File file, DataFixer dataFixer, CallbackInfo ci)
	{
		if (WrappedHotbarStorage.class.isAssignableFrom(getClass()))
		{
			this.setFile(file);
		}
	}

	@Inject(method = "load", at = @At(value = "INVOKE",
			target = "Lorg/apache/logging/log4j/Logger;error(Ljava/lang/String;Ljava/lang/Throwable;)V", shift = At.Shift.AFTER, remap = false))
	private void hookLoadFailure(CallbackInfo ci, @Local Exception ex)
	{
		if (WrappedHotbarStorage.class.isAssignableFrom(getClass()))
		{
			Librarian.getInstance().getEventBus().post(new LoadFailureEvent((IWrappedHotbarStorage) this, ex));
		}
	}

	@Inject(method = "save", at = @At(value = "INVOKE",
			target = "Lorg/apache/logging/log4j/Logger;error(Ljava/lang/String;Ljava/lang/Throwable;)V", shift = At.Shift.AFTER, remap = false))
	private void hookSaveFailure(CallbackInfo ci, @Local Exception ex)
	{
		if (WrappedHotbarStorage.class.isAssignableFrom(getClass()))
		{
			Librarian.getInstance().getEventBus().post(new SaveFailureEvent((IWrappedHotbarStorage) this, ex));
		}
	}

	@Inject(method = "load", at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/NbtHelper;update(Lcom/mojang/datafixers/DataFixer;Lnet/minecraft/datafixer/DataFixTypes;Lnet/minecraft/nbt/CompoundTag;I)Lnet/minecraft/nbt/CompoundTag;",
			shift = At.Shift.AFTER))
	private void fetchMetadata(CallbackInfo ci, @Local CompoundTag compound)
	{
		if (WrappedHotbarStorage.class.isAssignableFrom(getClass()))
		{
			CompoundTag meta = compound.getCompound("librarian");

			if (meta != null)
			{
				int version = meta.getInt("version");
				String name = meta.getString("name");
				String description = meta.getString("description");
				List<String> authors = new ArrayList<>(meta.getList("authors", 8).stream()
						.map(Tag::asString).collect(Collectors.toList()));

				if (version > HotbarPageMetadata.getCurrentVersion())
				{
					Librarian.getLogger().error("Hotbar metadata rejected - data is intended for a newer version of " +
									"Librarian than what we are currently running (current version {}, file version {})",
							HotbarPageMetadata.getCurrentVersion(), version);

					metadata = HotbarPageMetadata.builder().build();
				}
				else
				{
					Librarian.getLogger().info("Debug! Loaded metadata - Version {}, Name {}, Description {}", version,
							name, description);
					metadata = HotbarPageMetadata.builder()
							.version(version)
							.name(name != null && !name.isEmpty() ? librarian$serializer.deserializeOrNull(name) : null)
							.description(description != null && !description.isEmpty() ?
									librarian$serializer.deserializeOrNull(description) : null)
							.authors(authors)
							.build();
				}
			}
		}
	}

	@Inject(method = "save", at = @At(value = "INVOKE", target =
			"Lnet/minecraft/nbt/NbtIo;write(Lnet/minecraft/nbt/CompoundTag;Ljava/io/File;)V",
			shift = At.Shift.BEFORE))
	private void addMetadata(CallbackInfo ci, @Local CompoundTag compound)
	{
		if (WrappedHotbarStorage.class.isAssignableFrom(getClass()) && metadata != null)
		{
			CompoundTag meta = new CompoundTag();

			meta.putInt("version", HotbarPageMetadata.getCurrentVersion());

			if (metadata.getName() != null)
				meta.putString("name", librarian$serializer.serialize(metadata.getName()));

			if (metadata.getDescription() != null)
				meta.putString("description", librarian$serializer.serialize(metadata.getDescription()));

			if (metadata.getAuthors() != null && !metadata.getAuthors().isEmpty())
			{
				final ListTag list = new ListTag();
				metadata.getAuthors().forEach(author -> list.add(StringTag.of(author)));
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
			((HotbarStorageAccessor) this).invokeLoad();
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
	public abstract void setFile(File file);

	@Accessor
	public abstract boolean isLoaded();

	@Accessor
	public abstract void setLoaded(boolean loaded);
}
