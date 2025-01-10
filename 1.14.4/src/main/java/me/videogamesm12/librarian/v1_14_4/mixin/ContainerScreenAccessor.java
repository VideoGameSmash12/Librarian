package me.videogamesm12.librarian.v1_14_4.mixin;

import net.minecraft.client.gui.screen.ingame.ContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ContainerScreen.class)
public interface ContainerScreenAccessor
{
	@Accessor
	int getX();

	@Accessor
	int getY();
}
