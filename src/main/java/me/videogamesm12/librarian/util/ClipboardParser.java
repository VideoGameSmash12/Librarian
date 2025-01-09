package me.videogamesm12.librarian.util;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

public class ClipboardParser
{
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
