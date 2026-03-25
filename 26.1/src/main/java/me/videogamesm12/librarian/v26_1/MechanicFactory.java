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

package me.videogamesm12.librarian.v26_1;

import com.google.gson.JsonParseException;
import com.mojang.serialization.JsonOps;
import lombok.NonNull;
import me.videogamesm12.librarian.api.IMechanicFactory;
import me.videogamesm12.librarian.api.IWrappedHotbarStorage;
import me.videogamesm12.librarian.util.FNF;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.client.HotbarManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.ComponentSerialization;

import java.math.BigInteger;

public class MechanicFactory implements IMechanicFactory
{
	private static final GsonComponentSerializer GSON_COMPONENT_SERIALIZER = GsonComponentSerializer.gson();

	@Override
	public IWrappedHotbarStorage createHotbarStorage(@NonNull BigInteger integer)
	{
		return (IWrappedHotbarStorage) new HotbarManager(FNF.getFileForPage(integer).toPath(), Minecraft.getInstance().getFixerUpper());
	}

	@Override
	public Button createButton(int x, int y, int width, int height, Component label, Component tooltip, Runnable onClick)
	{
		return Button.builder(createText(label), _ -> onClick.run()).bounds(x, y,
				width, height).tooltip(tooltip != null ? Tooltip.create(createText(tooltip)) : null).build();
	}

	@Override
	public net.minecraft.network.chat.Component createText(Component component)
	{
		return ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, GSON_COMPONENT_SERIALIZER.serializeToTree(component))
				.getOrThrow(JsonParseException::new);
	}
}
