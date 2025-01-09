package me.videogamesm12.librarian.v1_14_4.mixin;

import com.mojang.datafixers.DataFixer;
import me.videogamesm12.librarian.v1_14_4.WrappedHotbarStorage;
import net.minecraft.client.options.HotbarStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;

@Mixin(HotbarStorage.class)
public abstract class HotbarStorageMixin
{
	/**
	 * <p>Hijacks what is used as the location by HotbarStorage on initialization.</p>
	 * @param ci        CallbackInfo
	 * @param dataFixer DataFixer
	 * @param file      File
	 */
	@Inject(method = "<init>", at = @At(value = "TAIL"))
	private void inject(File file, DataFixer dataFixer, CallbackInfo ci)
	{
		if (WrappedHotbarStorage.class.isAssignableFrom(getClass()))
		{
			this.setFile(file);
		}
	}

	@Accessor
	public abstract void setFile(File file);
}
