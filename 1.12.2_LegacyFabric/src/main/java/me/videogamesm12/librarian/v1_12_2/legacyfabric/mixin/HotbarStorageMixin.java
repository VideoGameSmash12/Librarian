package me.videogamesm12.librarian.v1_12_2.legacyfabric.mixin;

import me.videogamesm12.librarian.v1_12_2.legacyfabric.WrappedHotbarStorage;
import net.minecraft.class_3251;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;

@Mixin(class_3251.class)
public abstract class HotbarStorageMixin
{
	/**
	 * <p>Hijacks what is used as the location by HotbarStorage on initialization.</p>
	 * @param instance  Minecraft
	 * @param file      File
	 * @param ci        CallbackInfo
	 */
	@Inject(method = "<init>", at = @At(value = "TAIL"))
	private void inject(MinecraftClient instance, File file, CallbackInfo ci)
	{
		if (WrappedHotbarStorage.class.isAssignableFrom(getClass()))
		{
			this.setFile(file);
		}
	}

	@Accessor("field_15864")
	public abstract void setFile(File file);
}
