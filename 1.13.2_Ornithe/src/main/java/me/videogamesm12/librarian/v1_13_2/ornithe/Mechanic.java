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

package me.videogamesm12.librarian.v1_13_2.ornithe;

import lombok.NonNull;
import me.videogamesm12.librarian.api.IMechanicFactory;
import me.videogamesm12.librarian.api.IWrappedHotbarStorage;
import me.videogamesm12.librarian.util.FNF;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minecraft.client.HotbarManager;
import net.minecraft.client.Minecraft;
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
		return (IWrappedHotbarStorage) new HotbarManager(FNF.getFileForPage(integer), Minecraft.getInstance().getDataFixerUpper());
	}

	@Override
	public ButtonWidget createButton(int x, int y, int width, int height, Component label, Component tooltip, Runnable onClick)
	{
		final ButtonWidget widget = new ButtonWidget(startingId++, x, y, width, height, PLAIN_COMPONENT_SERIALIZER.serialize(label))
		{
			@Override
			public void click(double mouseX, double mouseY)
			{
				onClick.run();
			}
		};

		return widget;
	}

	@Override
	public Text createText(Component component)
	{
		return Text.Serializer.fromJson(GSON_COMPONENT_SERIALIZER.serialize(component));
	}
}
