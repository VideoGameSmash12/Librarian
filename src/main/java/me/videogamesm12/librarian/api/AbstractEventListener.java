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

import me.videogamesm12.librarian.Librarian;

/**
 * <h1>AbstractEventListener</h1>
 * <p>Entrypoint class that allows any mod to listen in on Librarian's events when they occur and process them
 * 	accordingly.</p>
 * <p>To register an event listener, the listener class must extend this one and be added to the {@code fabric.mod.json}
 * 	with the {@code librarian-listener} entrypoint.</p>
 * <p>Event listeners are loaded and registered when the mod initializes after the {@link IMechanicFactory} is
 * initialized but before {@link me.videogamesm12.librarian.api.addon.IAddon}s are loaded.</p>
 * <p>This is used for user-facing notifications like toasts and action bar messages, but that isn't strictly what you
 * 	should use it for.</p>
 */
public abstract class AbstractEventListener
{
	public AbstractEventListener()
	{
		Librarian.getInstance().getEventBus().register(this);
	}
}
