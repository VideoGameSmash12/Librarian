package me.videogamesm12.librarian.api;

import me.videogamesm12.librarian.Librarian;

public abstract class AbstractEventListener
{
	public AbstractEventListener()
	{
		Librarian.getInstance().getEventBus().register(this);
	}
}
