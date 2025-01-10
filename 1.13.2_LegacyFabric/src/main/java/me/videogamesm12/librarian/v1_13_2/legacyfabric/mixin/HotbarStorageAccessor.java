package me.videogamesm12.librarian.v1_13_2.legacyfabric.mixin;

import net.minecraft.class_3251;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(class_3251.class)
public interface HotbarStorageAccessor
{
	@Invoker("method_18153")
	void invokeLoad();
}
