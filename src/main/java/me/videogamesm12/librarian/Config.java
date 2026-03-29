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

package me.videogamesm12.librarian;

import com.google.common.eventbus.Subscribe;
import com.google.gson.*;
import com.google.gson.stream.JsonWriter;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import me.videogamesm12.librarian.api.event.NavigationEvent;
import me.videogamesm12.librarian.util.ConfigUpdater;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <h1>Config</h1>
 * <p>Class for storing and managing Librarian's configuration.</p>
 */
@Builder
public class Config
{
	@Getter
	private static final int currentVersion = 3;

	private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private static final File configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), "librarian.json");

	@Builder.Default
	private int version = currentVersion;

	@Builder.Default
	private MemorizerSettings memorizer = MemorizerSettings.builder().build();

	@Builder.Default
	private CommandSystemSettings commandSystem = CommandSystemSettings.builder().build();

	@Builder.Default
	private OptimizationSettings optimizations = OptimizationSettings.builder().build();

	/**
	 * Gets this configuration's version.
	 * @return	The numerical version of this configuration.
	 */
	public int version()
	{
		return version;
	}

	/**
	 * Gets the configuration for the Memorizer.
	 * @return	{@link MemorizerSettings}
	 */
	public MemorizerSettings memorizer()
	{
		if (memorizer == null)
			memorizer = MemorizerSettings.builder().build();

		return memorizer;
	}

	/**
	 * Gets the configuration for the command system.
	 * @return	{@link CommandSystemSettings}
	 */
	public CommandSystemSettings commandSystem()
	{
		if (commandSystem == null)
			commandSystem = CommandSystemSettings.builder().build();

		return commandSystem;
	}

	/**
	 * Gets the configuration for the command system.
	 * @return	{@link CommandSystemSettings}
	 */
	public OptimizationSettings optimizations()
	{
		if (optimizations == null)
			optimizations = OptimizationSettings.builder().build();

		return optimizations;
	}

	/**
	 * Memorizes the last page the user was at upon navigating to another page.
	 * @param event	{@link NavigationEvent}
	 */
	@Subscribe
	public void memorizeLastPage(NavigationEvent event)
	{
		if (memorizer.isEnabled())
		{
			memorizer.setPage(event.getNewPage());
			save();
		}
	}

	/**
	 * <h2>MemorizerSettings</h2>
	 * <p>All configuration options for the Memorizer feature, which keeps track of the last page the user was on in a
	 * 	session so that the next time the client starts up, they will start at that page.</p>
	 */
	@Builder
	public static class MemorizerSettings
	{
		/**
		 * Controls whether this feature is enabled.
		 */
		@Getter
		@Setter
		@Builder.Default
		private boolean enabled = true;

		/**
		 * Determines the page the user starts at on start-up if the feature is enabled. It gets automatically updated
		 * 	every time a page navigation occurs.
		 */
		@Getter
		@Setter
		@Builder.Default
		@NonNull
		private BigInteger page = BigInteger.ZERO;
	}

	/**
	 * <h2>CommandSystemSettings</h2>
	 * <p>All configuration options for the mod's client commands.</p>
	 */
	@Builder
	public static class CommandSystemSettings
	{
		/**
		 * Controls whether to register our commands on startup.
		 */
		@Getter
		@Builder.Default
		private boolean enabled = true;

		/**
		 * A list of aliases that get registered as aliases of the /librarian command.
		 */
		@Getter
		@Builder.Default
		@NonNull
		private List<String> aliases = Collections.singletonList("lb");
	}

	/**
	 * <h2>OptimizationSettings</h2>
	 * <p>All configuration options for the mod's optimization features.</p>
	 */
	@Builder
	public static class OptimizationSettings
	{
		/**
		 * Controls whether to use file compression when saving hotbar files.
		 */
		@Builder.Default
		private boolean useFileCompression = false;

		/**
		 * Controls whether to save pages in the background.
		 */
		@Builder.Default
		private boolean backgroundSaving = true;

		/**
		 * A list of page numbers for pages that should get automatically loaded on startup.
		 */
		@Getter
		@Builder.Default
		@NonNull
		private List<BigInteger> bookmarks = new ArrayList<>();

		public boolean useFileCompression()
		{
			return useFileCompression;
		}

		public boolean backgroundSaving()
		{
			return backgroundSaving;
		}
	}

	/**
	 * Create an instance by either loading it from disk or by generating a fresh one on the fly, depending on if a file
	 * 	exists already and if it could be loaded.
	 * @return	Config
	 */
	public static Config load()
	{
		Config config;

		if (configFile.exists())
		{
			try (final BufferedReader reader = Files.newBufferedReader(configFile.toPath()))
			{
				final JsonObject object = gson.fromJson(reader, JsonObject.class);
				if (!object.has("version"))
				{
					throw new JsonParseException("Missing version entry!");
				}

				final int version = object.get("version").getAsInt();

				// Reject configurations from newer versions of Librarian
				if (version > currentVersion)
				{
					throw new IllegalStateException("Configuration file is for a newer version of Librarian (expected " + currentVersion + ", got " + version + ")");
				}

				// If the configuration is outdated, update it
				if (version < currentVersion)
				{
					ConfigUpdater.update(object);
				}

				config = gson.fromJson(object, Config.class);
			}
			catch (JsonParseException ex)
			{
				Librarian.getLogger().warn("The configuration file is corrupted and could not be read.", ex);
				config = builder().build();
			}
			catch (Exception ex)
			{
				Librarian.getLogger().error("Failed to read configuration file", ex);
				config = builder().build();
			}
		}
		else
		{
			config = builder().build();
		}

		Librarian.getInstance().getEventBus().register(config);

		return config;
	}

	/**
	 * Saves this configuration to disk.
	 */
	public final void save()
	{
		try (final BufferedWriter writer = Files.newBufferedWriter(configFile.toPath()))
		{
			this.version = currentVersion;

			gson.toJson(this, Config.class, new JsonWriter(writer));
		}
		catch (IOException ex)
		{
			Librarian.getLogger().error("Failed to write configuration file", ex);
		}
	}
}
