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

package me.videogamesm12.librarian.api.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.videogamesm12.librarian.api.IWrappedHotbarStorage;

/**
 * <h1>LoadFailureEvent</h1>
 * <p>Event for if a saved hotbar page fails to load (usually a consequence of data corruption).</p>
 */
@Getter
@RequiredArgsConstructor
public class LoadFailureEvent extends LibrarianEvent
{
	/**
	 * The saved hotbar page that failed to load.
	 */
	private final IWrappedHotbarStorage storage;

	/**
	 * The exception that was thrown when the page failed to load
	 */
	private final Throwable error;
}
