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

package me.videogamesm12.librarian.v26_2.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.serialization.Dynamic;
import me.videogamesm12.librarian.Librarian;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.inventory.Hotbar;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Mixin(Hotbar.class)
public abstract class HotbarMixin
{
	@Shadow
	public abstract List<ItemStack> load(HolderLookup.Provider registries);

	@Unique
	private static final HolderLookup.Provider WRAPPER_LOOKUP = VanillaRegistries.createLookup();

	@Unique
	private final ItemStack[] cache =  new ItemStack[9];

	@Unique
	private int registryHash = 0;

	@Inject(method = "<init>(Ljava/util/List;)V", at = @At("TAIL"))
	private void preprocess(List<Dynamic<?>> stacks, CallbackInfo ci)
	{
		if (Librarian.getInstance().getConfig().optimizations().preprocessHotbarRows()
				&& !Librarian.getInstance().getConfig().optimizations().backgroundLoading())
		{
			Librarian.getInstance().queue(() ->
					load(Minecraft.getInstance().level != null ?
							Minecraft.getInstance().level.registryAccess() : WRAPPER_LOOKUP));
		}
	}

	@Inject(method = "storeFrom", at = @At("TAIL"))
	private void processAfterUpdate(Inventory inventory, RegistryAccess lookupProvider, CallbackInfo ci)
	{
		if (Librarian.getInstance().getConfig().optimizations().preprocessHotbarRows())
		{
			clear();
			load(Objects.requireNonNull(Minecraft.getInstance().level).registryAccess());
		}
	}

	@WrapMethod(method = "load")
	private List<ItemStack> process(HolderLookup.Provider registries, Operation<List<ItemStack>> original)
	{
		if (Librarian.getInstance().getConfig().optimizations().preprocessHotbarRows())
		{
			// Return cached entries if present
			if (registryHash != 0 && registryHash == registries.hashCode())
			{
				return Arrays.stream(cache).toList();
			}

			final List<ItemStack> processed = original.call(registries);
			for (int i = 0; i < processed.size(); i++)
			{
				cache[i] = processed.get(i);
			}

			registryHash = registries.hashCode();
			return processed;
		}

		return original.call(registries);
	}

	@Unique
	private void clear()
	{
		Arrays.fill(cache, null);
		registryHash = 0;
	}
}
