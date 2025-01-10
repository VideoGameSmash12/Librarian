package me.videogamesm12.librarian.v1_19.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.DataFixer;
import me.videogamesm12.librarian.Librarian;
import me.videogamesm12.librarian.api.IWrappedHotbarStorage;
import me.videogamesm12.librarian.api.event.LoadFailureEvent;
import me.videogamesm12.librarian.api.event.SaveFailureEvent;
import me.videogamesm12.librarian.v1_19.WrappedHotbarStorage;
import net.minecraft.client.option.HotbarStorage;
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
			target = "Lorg/slf4j/Logger;error(Ljava/lang/String;Ljava/lang/Throwable;)V", shift = At.Shift.AFTER))
	private void hookLoadFailure(CallbackInfo ci, @Local Exception ex)
	{
		if (WrappedHotbarStorage.class.isAssignableFrom(getClass()))
		{
			Librarian.getInstance().getEventBus().post(new LoadFailureEvent((IWrappedHotbarStorage) this, ex));
		}
	}

	@Inject(method = "save", at = @At(value = "INVOKE",
			target = "Lorg/slf4j/Logger;error(Ljava/lang/String;Ljava/lang/Throwable;)V", shift = At.Shift.AFTER))
	private void hookSaveFailure(CallbackInfo ci, @Local Exception ex)
	{
		if (WrappedHotbarStorage.class.isAssignableFrom(getClass()))
		{
			Librarian.getInstance().getEventBus().post(new SaveFailureEvent((IWrappedHotbarStorage) this, ex));
		}
	}

	@Accessor
	public abstract void setFile(File file);
}
