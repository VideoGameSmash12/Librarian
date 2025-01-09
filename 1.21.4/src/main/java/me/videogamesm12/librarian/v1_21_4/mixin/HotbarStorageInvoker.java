package me.videogamesm12.librarian.v1_21_4.mixin;

import net.minecraft.client.option.HotbarStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(HotbarStorage.class)
public interface HotbarStorageInvoker
{
	@Invoker
	void invokeLoad();
}
