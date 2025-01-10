package me.videogamesm12.librarian.v1_14_4.ornithe.mixin;

import me.videogamesm12.librarian.Librarian;
import net.minecraft.client.HotbarManager;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftMixin
{
	@Inject(method = "m_7202825", at = @At("HEAD"), cancellable = true)
	public void redirect(CallbackInfoReturnable<HotbarManager> cir)
	{
		cir.setReturnValue((HotbarManager) Librarian.getInstance().getCurrentPage());
	}
}
