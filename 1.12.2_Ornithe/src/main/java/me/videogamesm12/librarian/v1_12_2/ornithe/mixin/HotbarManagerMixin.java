package me.videogamesm12.librarian.v1_12_2.ornithe.mixin;

import me.videogamesm12.librarian.v1_12_2.ornithe.WrappedHotbarStorage;
import net.minecraft.client.HotbarManager;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;

@Mixin(HotbarManager.class)
public abstract class HotbarManagerMixin
{
	/**
	 * <p>Hijacks what is used as the location by HotbarStorage on initialization.</p>
	 * @param instance  Minecraft
	 * @param file      File
	 * @param ci        CallbackInfo
	 */
	@Inject(method = "<init>", at = @At(value = "TAIL"))
	private void inject(Minecraft instance, File file, CallbackInfo ci)
	{
		if (WrappedHotbarStorage.class.isAssignableFrom(getClass()))
		{
			this.setFile(file);
		}
	}

	@Accessor
	public abstract void setFile(File file);
}
