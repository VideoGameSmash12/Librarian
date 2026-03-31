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

package me.videogamesm12.librarian.v1_21_8.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.serialization.Dynamic;
import me.videogamesm12.librarian.Librarian;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.HotbarStorageEntry;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Mixin(HotbarStorageEntry.class)
public abstract class HotbarStorageEntryMixin
{
	@Unique
	private static final RegistryWrapper.WrapperLookup WRAPPER_LOOKUP = BuiltinRegistries.createWrapperLookup();

	@Shadow
	public abstract List<ItemStack> deserialize(RegistryWrapper.WrapperLookup par1);

	@Unique
	private final ItemStack[] cache =  new ItemStack[9];

	@Unique
	private boolean alreadyProcessed;

	@Inject(method = "<init>(Ljava/util/List;)V", at = @At("TAIL"))
	private void preprocess(List<Dynamic<?>> stacks, CallbackInfo ci)
	{
		if (Librarian.getInstance().getConfig().optimizations().preprocessHotbarRows()
				&& !Librarian.getInstance().getConfig().optimizations().backgroundLoading())
		{
			Librarian.getInstance().queue(() ->
					deserialize(MinecraftClient.getInstance().world != null ?
							MinecraftClient.getInstance().world.getRegistryManager() : WRAPPER_LOOKUP));
		}
	}

	@Inject(method = "serialize", at = @At("TAIL"))
	private void processAfterUpdate(PlayerInventory playerInventory, DynamicRegistryManager registryManager, CallbackInfo ci)
	{
		if (Librarian.getInstance().getConfig().optimizations().preprocessHotbarRows())
		{
			clear();
			deserialize(Objects.requireNonNull(MinecraftClient.getInstance().world).getRegistryManager());
		}
	}

	@WrapMethod(method = "deserialize")
	private List<ItemStack> process(RegistryWrapper.WrapperLookup registries, Operation<List<ItemStack>> original)
	{
		if (Librarian.getInstance().getConfig().optimizations().preprocessHotbarRows())
		{
			// Return cached entries if present
			if (alreadyProcessed)
			{
				return Arrays.stream(cache).toList();
			}

			final List<ItemStack> processed = original.call(registries);
			for (int i = 0; i < processed.size(); i++)
			{
				cache[i] = processed.get(i);
			}
			alreadyProcessed = true;

			return processed;
		}

		return original.call(registries);
	}

	@Unique
	private void clear()
	{
		Arrays.fill(cache, null);
		alreadyProcessed = false;
	}
}
