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
import me.videogamesm12.librarian.Librarian;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
public class HotbarPageMetadata
{
	@Getter
	private static final int currentVersion = 0;

	@Builder.Default
	private int version = getCurrentVersion();

	@Builder.Default
	private Component name = null;

	@Builder.Default
	private Component description = null;

	@Builder.Default
	private List<String> authors = new ArrayList<>();

	public <A> A getUserFriendlyName()
	{
		return Librarian.getInstance().getMechanic().createText(name != null ? name : Component.empty());
	}

	public <A> A getUserFriendlyDescription()
	{
		return Librarian.getInstance().getMechanic().createText(description != null ? description : Component.empty());
	}

	public void addAuthor(String name)
	{
		authors.add(name);
	}

	public void removeAuthor(String name)
	{
		authors.remove(name);
	}
}
