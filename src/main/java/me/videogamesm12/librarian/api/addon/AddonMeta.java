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

package me.videogamesm12.librarian.api.addon;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * <h1>AddonMeta</h1>
 * <p>Mandatory metadata interface for {@link IAddon} that is used to determine whether an add-on should be
 * 	initialized or not depending on the mods that have been loaded.</p>
 * <p>There are currently two parameters that can be set here:</p>
 * <ul>
 *     <li>{@code requiredMods} is an array of IDs for mods that must be loaded.</li>
 *     <li>{@code incompatibleMods} is an array of IDs for mods that must not be loaded. If any of the mods listed are
 *     	loaded, Librarian will refuse to initialize the add-on. This does not have to be set as it defaults to an empty
 *     	array.</li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface AddonMeta
{
	/**
	 * An array of IDs for mods that must be loaded.
	 * @return	String[]
	 */
	String[] requiredMods();

	/**
	 * An array of IDs for mods that are incompatible with this add-on and are thus a deal-breaker.
	 * @return	String[]
	 */
	String[] incompatibleMods() default {};
}
