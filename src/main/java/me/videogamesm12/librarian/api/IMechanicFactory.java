package me.videogamesm12.librarian.api;

import lombok.NonNull;
import net.kyori.adventure.text.Component;

import java.io.File;
import java.math.BigInteger;
import java.util.List;

public interface IMechanicFactory
{
	IWrappedHotbarStorage createHotbarStorage(final @NonNull BigInteger integer);

	<T> T createButton(int x, int y, int width, int height, Component label, Component tooltip, Runnable onClick);

	<A> A createText(Component component);

	default void overhaulHotbars(List<File> files)
	{
		throw new UnsupportedOperationException("Not implemented for this version yet");
	}
}
