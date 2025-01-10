package me.videogamesm12.librarian.v1_13_2.ornithe.mixin;

import net.minecraft.client.HotbarManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(HotbarManager.class)
public interface HotbarManagerAccessor
{
	@Invoker
	void invokeLoad();
}
