package me.videogamesm12.librarian.v1_12_2.legacyfabric.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HandledScreen.class)
public interface HandledScreenAccessor
{
	@Accessor
	int getX();

	@Accessor
	int getY();
}
