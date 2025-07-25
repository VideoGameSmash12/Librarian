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

package me.videogamesm12.librarian.v1_21_8;

import com.google.gson.JsonParseException;
import com.mojang.serialization.JsonOps;
import lombok.NonNull;
import me.videogamesm12.librarian.api.IMechanicFactory;
import me.videogamesm12.librarian.api.IWrappedHotbarStorage;
import me.videogamesm12.librarian.util.FNF;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.HotbarStorage;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;

import java.math.BigInteger;

public class MechanicFactory implements IMechanicFactory
{
	private static final GsonComponentSerializer GSON_COMPONENT_SERIALIZER = GsonComponentSerializer.gson();
	private static final RegistryWrapper.WrapperLookup WRAPPER_LOOKUP = BuiltinRegistries.createWrapperLookup();

	@Override
	public IWrappedHotbarStorage createHotbarStorage(@NonNull BigInteger integer)
	{
		return (IWrappedHotbarStorage) new HotbarStorage(FNF.getFileForPage(integer).toPath(), MinecraftClient.getInstance().getDataFixer());
	}

	@Override
	public ButtonWidget createButton(int x, int y, int width, int height, Component label, Component tooltip, Runnable onClick)
	{
		return ButtonWidget.builder(createText(label), button -> onClick.run()).dimensions(x, y,
				width, height).tooltip(tooltip != null ? Tooltip.of(createText(tooltip)) : null).build();
	}

	@Override
	public Text createText(Component component)
	{
		return TextCodecs.CODEC.parse(WRAPPER_LOOKUP.getOps(JsonOps.INSTANCE),
				GSON_COMPONENT_SERIALIZER.serializeToTree(component)).getOrThrow(JsonParseException::new);
	}
}
