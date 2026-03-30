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

package me.videogamesm12.librarian.v1_21_11.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.serialization.Dynamic;
import me.videogamesm12.librarian.Librarian;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Mixin(HotbarStorageEntry.class)
public abstract class HotbarStorageEntryMixin
{
	@Unique
	private static final RegistryWrapper.WrapperLookup WRAPPER_LOOKUP = BuiltinRegistries.createWrapperLookup();

	@Shadow
	public abstract List<ItemStack> deserialize(RegistryWrapper.WrapperLookup par1);

	@Unique
	private final List<ItemStack> processed = new ArrayList<>();

	@Inject(method = "<init>(Ljava/util/List;)V", at = @At("TAIL"))
	private void preprocess(List<Dynamic<?>> stacks, CallbackInfo ci)
	{
		CompletableFuture.runAsync(() ->
		{
			processed.clear();
			deserialize(WRAPPER_LOOKUP);
		});
	}

	@Inject(method = "serialize", at = @At("TAIL"))
	private void processAfterUpdate(PlayerInventory playerInventory, DynamicRegistryManager registryManager, CallbackInfo ci)
	{
		CompletableFuture.runAsync(() ->
		{
			processed.clear();
			deserialize(WRAPPER_LOOKUP);
		});
	}

	@WrapMethod(method = "deserialize")
	private List<ItemStack> process(RegistryWrapper.WrapperLookup registries, Operation<List<ItemStack>> original)
	{
		if (processed.isEmpty())
		{
			processed.addAll(original.call(registries));
		}

		return processed;
	}
}
