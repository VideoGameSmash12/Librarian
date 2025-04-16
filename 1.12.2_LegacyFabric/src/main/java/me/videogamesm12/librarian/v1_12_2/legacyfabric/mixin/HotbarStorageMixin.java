package me.videogamesm12.librarian.v1_12_2.legacyfabric.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import me.videogamesm12.librarian.Librarian;
import me.videogamesm12.librarian.api.HotbarPageMetadata;
import me.videogamesm12.librarian.api.IWrappedHotbarStorage;
import me.videogamesm12.librarian.v1_12_2.legacyfabric.WrappedHotbarStorage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.class_3251;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mixin(class_3251.class)
public abstract class HotbarStorageMixin implements IWrappedHotbarStorage
{
	@Unique
	private static final GsonComponentSerializer librarian$serializer = GsonComponentSerializer.colorDownsamplingGson();

	@Unique
	private HotbarPageMetadata metadata = null;

	/**
	 * <p>Hijacks what is used as the location by HotbarStorage on initialization.</p>
	 * @param instance  Minecraft
	 * @param file      File
	 * @param ci        CallbackInfo
	 */
	@Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_3251;method_14449()V", shift = At.Shift.BEFORE))
	private void inject(MinecraftClient instance, File file, CallbackInfo ci)
	{
		if (WrappedHotbarStorage.class.isAssignableFrom(getClass()))
		{
			this.setFile(file);
		}
	}

	@ModifyVariable(method = "method_14449", at = @At(value = "STORE", ordinal = 0))
	private NbtCompound fetchMetadata(NbtCompound compound)
	{
		if (WrappedHotbarStorage.class.isAssignableFrom(getClass()))
		{
			// Ignore if null/no data
			if (compound == null)
			{
				return compound;
			}

			NbtCompound meta = compound.getCompound("librarian");

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
		}
		else
		{
			Librarian.getLogger().info("HUH");
		}

		return compound;
	}

	@Inject(method = "method_14451", at = @At(value = "INVOKE", target =
			"Lnet/minecraft/nbt/NbtIo;write(Lnet/minecraft/nbt/NbtCompound;Ljava/io/File;)V",
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
				meta.putString("description", librarian$serializer.serialize(metadata.getUserFriendlyDescription()));

			if (metadata.getAuthors() != null && !metadata.getAuthors().isEmpty())
			{
				final NbtList list = new NbtList();
				metadata.getAuthors().forEach(author -> list.add(new NbtString(author)));
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
			load();
		}

		return Optional.ofNullable(metadata);
	}

	@Override
	public void setMetadata(HotbarPageMetadata newMeta)
	{
		metadata = newMeta;
	}

	@Accessor("field_15864")
	public abstract void setFile(File file);

	@Override
	public boolean isLoaded()
	{
		return true;
	}

	@Override
	public void setLoaded(boolean loaded)
	{
		// We already loaded, lmao
	}
}
