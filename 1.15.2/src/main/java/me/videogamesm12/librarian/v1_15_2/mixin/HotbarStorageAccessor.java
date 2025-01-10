package me.videogamesm12.librarian.v1_15_2.mixin;

import net.minecraft.client.options.HotbarStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(HotbarStorage.class)
public interface HotbarStorageAccessor
{
	@Invoker
	void invokeLoad();
}
