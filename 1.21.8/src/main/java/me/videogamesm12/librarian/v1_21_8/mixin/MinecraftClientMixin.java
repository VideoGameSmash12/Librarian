/*
 * Copyright (C) 2025 Video
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

package me.videogamesm12.librarian.v1_21_8.mixin;

import me.videogamesm12.librarian.Librarian;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.HotbarStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin
{
	@Inject(method = "getCreativeHotbarStorage", at = @At("HEAD"), cancellable = true)
	public void getCreativeHotbarStorage(CallbackInfoReturnable<HotbarStorage> cir)
	{
		final Librarian instance = Librarian.getInstance();
		cir.setReturnValue((HotbarStorage) instance.getHotbarPage(instance.getCurrentPageNumber()));
	}
}
