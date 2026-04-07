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
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin
{
	@Inject(method = "handleLogin", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/Minecraft;setLevel(Lnet/minecraft/client/multiplayer/ClientLevel;)V"))
	private void preprocessAllHotbarsOnJoin(ClientboundLoginPacket packet, CallbackInfo ci)
	{
		if (Librarian.getInstance().getConfig().optimizations().preprocessHotbarRows())
		{
			Librarian.getInstance().queue(() ->
					Librarian.getInstance().getMap().forEach((_, page) ->
							page.librarian$preprocess()));
		}
	}

	@Inject(method = "handleRespawn", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/Minecraft;setLevel(Lnet/minecraft/client/multiplayer/ClientLevel;)V"))
	private void preprocessAllHotbarsOnWorldChange(ClientboundRespawnPacket packet, CallbackInfo ci)
	{
		if (Librarian.getInstance().getConfig().optimizations().preprocessHotbarRows())
		{
			Librarian.getInstance().queue(() ->
					Librarian.getInstance().getMap().forEach((_, page) ->
							page.librarian$preprocess()));
		}
	}
}
