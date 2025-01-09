package me.videogamesm12.librarian.v1_12_2.ornithe.mixin;

import net.minecraft.client.gui.screen.inventory.menu.InventoryMenuScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(InventoryMenuScreen.class)
public interface InventoryMenuScreenAccessor
{
	@Accessor
	int getX();

	@Accessor
	int getY();
}
