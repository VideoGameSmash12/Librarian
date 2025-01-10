package me.videogamesm12.librarian.v1_17_1.mixin;

import me.videogamesm12.librarian.Librarian;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.HotbarStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin
{
	@Inject(method = "getCreativeHotbarStorage", at = @At("HEAD"), cancellable = true)
	public void getCreativeHotbarStorage(CallbackInfoReturnable<HotbarStorage> cir)
	{
		final Librarian instance = Librarian.getInstance();
		cir.setReturnValue((HotbarStorage) instance.getHotbarPage(instance.getCurrentPageNumber()));
	}
}
