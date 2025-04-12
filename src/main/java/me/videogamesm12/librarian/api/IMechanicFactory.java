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
package me.videogamesm12.librarian.api;

import lombok.NonNull;
import net.kyori.adventure.text.Component;

import java.math.BigInteger;

/**
 * <h1>IMechanicFactory</h1>
 * <p>Mechanics in Librarian are classes that create version-specific objects such as text components, button widgets,
 * saved hotbar and saved hotbar instances.</p>
 */
public interface IMechanicFactory
{
	/**
	 * Creates an instance of {@link IWrappedHotbarStorage} using the BigInteger provided as the page number.
	 * @param integer	BigInteger
	 * @return			An instance of a class that extends IWrappedHotbarStorage
	 */
	IWrappedHotbarStorage createHotbarStorage(final @NonNull BigInteger integer);

	/**
	 * Creates a clickable button widget for use in screens and GUIs. Some versions don't have tooltip support, so usage
	 * 	of {@code tooltip} isn't always guaranteed.
	 * @param x			int
	 * @param y			int
	 * @param width		int
	 * @param height	int
	 * @param label		{@link net.kyori.adventure.text.Component}
	 * @param tooltip	{@link net.kyori.adventure.text.Component}
	 * @param onClick	Runnable
	 * @return			A clickable button widget
	 * @param <T>		ButtonWidget
	 */
	<T> T createButton(int x, int y, int width, int height, Component label, Component tooltip, Runnable onClick);

	/**
	 * Converts an Adventure text component to a Minecraft-native text component.
	 * @param component	{@link net.kyori.adventure.text.Component}
	 * @return			A Minecraft-native text component
	 * @param <A>		Text
	 */
	<A> A createText(Component component);
}
