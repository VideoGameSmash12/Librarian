package me.videogamesm12.librarian.v1_21_4.mixin;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PlayerInventory.class)
public interface PlayerInventoryAccessor
{
	@Invoker
	int invokeAddStack(int slot, ItemStack stack);
}
