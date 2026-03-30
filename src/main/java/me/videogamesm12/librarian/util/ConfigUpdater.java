/*
 * Copyright (C) 2026 Video
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

package me.videogamesm12.librarian.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.videogamesm12.librarian.Librarian;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * <h1>ConfigUpdater</h1>
 * <p>Utility class to update older Librarian configurations incrementally. Somewhat similar to what Minecraft does with
 * their DataFixerUpper.</p>
 */
@RequiredArgsConstructor
@Getter
public class ConfigUpdater
{
	private static final List<ConfigUpdater> updaters = new ArrayList<>();

	static
	{
		// Version 2 (publicly released development builds between 3/27/2026 and 3/28/2026):
		//	[+]	optimizations.useFileCompression
		//	[+]	optimizations.saveAsynchronously
		//	[+]	optimizations.pagesToPreload
		updaters.add(new ConfigUpdater(2, root ->
		{
			final JsonObject optimization = new JsonObject();

			optimization.addProperty("useFileCompression", false);
			optimization.addProperty("saveAsynchronously", true);
			optimization.add("pagesToPreload", new JsonArray());

			root.add("optimizations", optimization);
			root.addProperty("version", 2);

			Librarian.getLogger().warn("Version 2 -> {}", root);
		}));
		// Version 3 (publicly released development builds between 3/28/2026 and 3/29/2026):
		//	[>] optimizations.saveAsynchronously -> optimizations.backgroundSaving
		//	[>]	optimizations.pagesToPreload	 -> optimizations.bookmarks
		updaters.add(new ConfigUpdater(3, root ->
		{
			final JsonObject optimization = root.getAsJsonObject("optimizations");

			boolean async = optimization.getAsJsonPrimitive("saveAsynchronously").getAsBoolean();
			optimization.remove("saveAsynchronously");
			optimization.addProperty("backgroundSaving", async);

			JsonArray pagesToPreload = optimization.getAsJsonArray("pagesToPreload");
			optimization.add("bookmarks", pagesToPreload);
			optimization.remove("pagesToPreload");

			root.addProperty("version", 3);

			Librarian.getLogger().warn("Version 3 -> {}", root);
		}));
		// Version 4:
		//	[+] optimizations.preprocessHotbarRows
		//	[+] optimizations.readHotbarRowsInParallel
		updaters.add(new ConfigUpdater(4, root ->
		{
			final JsonObject optimization = root.getAsJsonObject("optimizations");

			optimization.addProperty("preprocessHotbarRows", true);
			optimization.addProperty("readHotbarRowsInParallel", true);

			root.addProperty("version", 4);

			Librarian.getLogger().warn("Version 4 -> {}", root);
		}));
	}

	protected final int version;
	protected final Consumer<JsonObject> updater;

	public static void update(JsonObject config)
	{
		final int version = config.get("version").getAsInt();
		updaters.stream().filter(configUpdater -> configUpdater.version > version)
				.forEach(updater -> updater.updater.accept(config));
	}
}
