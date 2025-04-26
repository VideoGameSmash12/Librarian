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

/**
 * <h1>IAddon</h1>
 * <p>Interface for optional add-ons that can be enabled when certain mods are initialized. These are the last to be
 * 	set up when Librarian initializes and due to how the Fabric Loader handles entrypoints are always loaded even when
 * 	the mods it requires aren't present. As such, any code that uses code from other mods should ideally be contained
 * 	and only initialized in the provided {@code init} method.</p>
 * <p>Add-on instances must have the {@link AddonMeta} annotation in order to have the {@code init} method called, as
 * 	otherwise it will simply refuse to do so.</p>
 * <p>To register an add-on, it must implement this interface and be added to the {@code fabric.mod.json} with the
 * 	{@code librarian-addon} entrypoint.</p>
 */
public interface IAddon
{
	void init();
}
