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

package me.videogamesm12.librarian.util;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

/**
 * <h1>ClipboardParser</h1>
 * <p>Currently unused utility class that handles data from the clipboard.</p>
 */
public class ClipboardParser
{
	/**
	 * Reads the user's clipboard and tries to get data based on the provided {@link DataFlavor}s. If nothing could be
	 * 	extracted, this returns null.
	 * @param clipboard	{@link Clipboard}
	 * @param flavors	{@link DataFlavor}[]
	 * @return			Object
	 */
	public static Object readClipboard(Clipboard clipboard, DataFlavor... flavors)
	{
		for (DataFlavor flavor : flavors)
		{
			try
			{
				return clipboard.getData(flavor);
			}
			catch (IOException | UnsupportedFlavorException ignored)
			{
			}
		}

		return null;
	}
}
