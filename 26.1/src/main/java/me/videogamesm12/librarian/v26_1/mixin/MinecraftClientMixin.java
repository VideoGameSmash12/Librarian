/*
 * Copyright (C) 2026 Video
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.videogamesm12.librarian.v26_1.mixin;

import me.videogamesm12.librarian.Librarian;
import net.minecraft.client.HotbarManager;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftClientMixin
{
	@Inject(method = "getHotbarManager", at = @At("HEAD"), cancellable = true)
	public void getCreativeHotbarStorage(CallbackInfoReturnable<HotbarManager> cir)
	{
		final Librarian instance = Librarian.getInstance();
		cir.setReturnValue((HotbarManager) instance.getHotbarPage(instance.getCurrentPageNumber()));
	}
}
