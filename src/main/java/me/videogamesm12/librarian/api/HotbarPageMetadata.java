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

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * <h1>HotbarPageMetadata</h1>
 * <p>Metadata class for storing optional metadata that is loaded from and saved to pages. This is used to allow the
 * 	user to customize their saved hotbar pages more thoroughly.</p>
 */
@Getter
@Setter
@Builder
public class HotbarPageMetadata
{
	/**
	 * The version of the metadata system that this mod uses for sanity checks.
	 */
	@Getter
	private static final int currentVersion = 0;

	/**
	 * The version of the metadata system this hotbar page was saved for. By default, this is set to whatever the
	 * 	current version is in the mod.
	 */
	@Builder.Default
	private int version = getCurrentVersion();

	/**
	 * The custom name set for the saved hotbar page. This can be set with either in-game commands, by clicking the
	 * 	Saved Hotbars text in the creative inventory menu, or programmatically by setting the value with the provided
	 * 	setter method.
	 */
	@Builder.Default
	@Nullable
	private Component name = null;

	/**
	 * The description for the saved hotbar page. This can be set with either in-game commands or by setting the value
	 * 	programmatically with the provided setter method.
	 */
	@Builder.Default
	@Nullable
	private Component description = null;

	/**
	 * A list of authors that the saved hotbar page is attributed to.
	 */
	@Builder.Default
	@NotNull
	private List<String> authors = new ArrayList<>();

	/**
	 * Adds a username to the list of authors for the saved hotbar page.
	 * @param name	String
	 */
	public void addAuthor(String name)
	{
		authors.add(name);
	}

	/**
	 * Removes a username from the list of authors for the saved hotbar page, if present.
	 * @param name	String
	 */
	public void removeAuthor(String name)
	{
		authors.remove(name);
	}
}
