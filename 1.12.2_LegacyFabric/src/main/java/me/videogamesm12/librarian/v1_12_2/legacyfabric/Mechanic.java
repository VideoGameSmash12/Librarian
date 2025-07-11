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

package me.videogamesm12.librarian.v1_12_2.legacyfabric;

import lombok.NonNull;
import me.videogamesm12.librarian.api.IMechanicFactory;
import me.videogamesm12.librarian.api.IWrappedHotbarStorage;
import me.videogamesm12.librarian.util.FNF;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minecraft.class_3251;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.math.BigInteger;

public class Mechanic implements IMechanicFactory
{
	private static final GsonComponentSerializer GSON_COMPONENT_SERIALIZER = GsonComponentSerializer.colorDownsamplingGson();
	private static final PlainTextComponentSerializer PLAIN_COMPONENT_SERIALIZER = PlainTextComponentSerializer.plainText();

	private int startingId = -1337;

	@Override
	public IWrappedHotbarStorage createHotbarStorage(@NonNull BigInteger integer)
	{
		// Use the one that the game already loaded on startup if the page is 0. This isn't as big of an issue on newer
		//	versions of the game because they load everything *as they need it* in those versions, but 1.12.2 is a
		//	special snowflake who likes to load everything during the initialization process, and since I can't tell it
		//	not to without using some additional hacky fixes, we might as well just bite the bullet and deal with it.
		//
		// This sucks.
		if (integer.equals(BigInteger.ZERO))
		{
			return (IWrappedHotbarStorage) MinecraftClient.getInstance().field_15872;
		}
		else
		{
			return (IWrappedHotbarStorage) new class_3251(MinecraftClient.getInstance(), FNF.getFileForPage(integer));
		}
	}

	@Override
	public ButtonWidget createButton(int x, int y, int width, int height, Component label, Component tooltip, Runnable onClick)
	{
		final ButtonWidget widget = new ButtonWidget(startingId++, x, y, width, height, PLAIN_COMPONENT_SERIALIZER.serialize(label));
		((ILButtonWidget) widget).librarian$setOnClick(onClick);

		return widget;
	}

	@Override
	public Text createText(Component component)
	{
		return Text.Serializer.deserializeText(GSON_COMPONENT_SERIALIZER.serialize(component));
	}
}
