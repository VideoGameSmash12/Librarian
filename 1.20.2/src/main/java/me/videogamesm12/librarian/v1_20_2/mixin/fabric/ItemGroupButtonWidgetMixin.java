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

package me.videogamesm12.librarian.v1_20_2.mixin.fabric;

import me.videogamesm12.librarian.v1_20_2.mixin.CreativeInventoryScreenAccessor;
import net.fabricmc.fabric.impl.client.itemgroup.FabricCreativeGuiComponents;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("UnstableApiUsage")
@Mixin(FabricCreativeGuiComponents.ItemGroupButtonWidget.class)
public class ItemGroupButtonWidgetMixin
{
	// Alternative title: pleaseStopFuckingWithMyStuff
	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void avoidOverlapsWithMyButtons(DrawContext drawContext, int mouseX, int mouseY, float float_1, CallbackInfo ci)
	{
		if (CreativeInventoryScreenAccessor.getSelectedTab().getType() == ItemGroup.Type.HOTBAR)
		{
			ci.cancel();
		}
	}
}
