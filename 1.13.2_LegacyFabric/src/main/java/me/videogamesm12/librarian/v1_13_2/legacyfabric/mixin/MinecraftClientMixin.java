package me.videogamesm12.librarian.v1_13_2.legacyfabric.mixin;

import me.videogamesm12.librarian.Librarian;
import net.minecraft.class_3251;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin
{
	@Inject(method = "method_18221", at = @At("HEAD"), cancellable = true)
	public void redirect(CallbackInfoReturnable<class_3251> instance)
	{
		instance.setReturnValue((class_3251) Librarian.getInstance().getCurrentPage());
	}
}
