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

import java.math.BigInteger;

/**
 * <h1>CacheClearEvent</h1>
 * <p>Event for when the saved hotbar cache is cleared.</p>
 * <p>This is primarily used to refresh anything like the creative inventory menu.</p>
 */
@Getter
@RequiredArgsConstructor
public class CacheClearEvent extends LibrarianEvent
{
	/**
	 * The current page at the time of the event.
	 */
	private final BigInteger currentPage;
}
