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

package me.videogamesm12.librarian.v1_13_2.legacyfabric;

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
		return (IWrappedHotbarStorage) new class_3251(FNF.getFileForPage(integer), MinecraftClient.getInstance().method_12142());
	}

	@Override
	public ButtonWidget createButton(int x, int y, int width, int height, Component label, Component tooltip, Runnable onClick)
	{
		return new ButtonWidget(startingId++, x, y, width, height, PLAIN_COMPONENT_SERIALIZER.serialize(label))
		{
			@Override
			public void method_18374(double d, double e)
			{
				onClick.run();
				super.method_18374(d, e);
			}
		};
	}

	@Override
	public Text createText(Component component)
	{
		return Text.Serializer.deserializeText(GSON_COMPONENT_SERIALIZER.serialize(component));
	}
}
