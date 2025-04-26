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

package me.videogamesm12.librarian.util;

import com.google.gson.JsonParseException;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * <h1>ComponentProcessor</h1>
 * <p>Utility system for processing user-provided inputs and converting them to Adventure components.</p>
 * <p>In the future, this may be fleshed out to also add support for taking Adventure components and converting them
 * back to a String. For now though, it only supports going one-way.</p>
 */
public abstract class ComponentProcessor
{
	@Getter
	private static final List<ComponentProcessor> formatters = new ArrayList<>();
	//
	private static final ComponentProcessor legacyAmpersand;
	private static final ComponentProcessor legacySection;
	private static final ComponentProcessor miniMessage;
	private static final ComponentProcessor json;
	private static final ComponentProcessor plainText;

	static
	{
		legacyAmpersand = new ComponentProcessor()
		{
			private final LegacyComponentSerializer ampersand = LegacyComponentSerializer.legacyAmpersand();
			private final Pattern pattern = Pattern.compile("(?i)&([0-9A-FK-ORX]|#([A-F0-9]){2,6})");

			@Override
			public LegacyComponentSerializer getSerializer()
			{
				return ampersand;
			}

			@Override
			public boolean shouldProcessComponent(String input)
			{
				return pattern.matcher(input).find();
			}
		};
		legacySection = new ComponentProcessor()
		{
			private final LegacyComponentSerializer section = LegacyComponentSerializer.legacySection();
			private final Pattern pattern = Pattern.compile("(?i)§[0-9A-FK-OR]");

			@Override
			public LegacyComponentSerializer getSerializer()
			{
				return section;
			}

			@Override
			public boolean shouldProcessComponent(String input)
			{
				return pattern.matcher(input).find();
			}
		};
		miniMessage = new ComponentProcessor()
		{
			private final MiniMessage miniMessage = MiniMessage.miniMessage();

			@Override
			public MiniMessage getSerializer()
			{
				return miniMessage;
			}

			@Override
			public boolean shouldProcessComponent(String input)
			{
				return !miniMessage.stripTags(input).equalsIgnoreCase(input);
			}
		};
		json = new ComponentProcessor()
		{
			private final GsonComponentSerializer gson = VersionChecker.isNewerThanOrEqualTo("minecraft", "1.16.5") ?
					GsonComponentSerializer.gson() : GsonComponentSerializer.colorDownsamplingGson();

			@Override
			public GsonComponentSerializer getSerializer()
			{
				return gson;
			}

			@Override
			public boolean shouldProcessComponent(String input)
			{
				return isValidJson(input) && !input.startsWith("\"");
			}

			private boolean isValidJson(String input)
			{
				try
				{
					gson.deserialize(Objects.requireNonNull(input));
					return true;
				}
				catch (JsonParseException parseException)
				{
					return false;
				}
			}
		};
		plainText = new ComponentProcessor()
		{
			@Override
			public ComponentSerializer<Component, Component, String> getSerializer()
			{
				return null;
			}

			@Override
			public boolean shouldProcessComponent(String input)
			{
				return true;
			}

			@Override
			public Component processComponent(String input)
			{
				return Component.text(input);
			}
		};

		formatters.add(plainText);
		formatters.add(json);
		formatters.add(miniMessage);
		formatters.add(legacySection);
		formatters.add(legacyAmpersand);
	}

	/**
	 * Get the {@link ComponentSerializer} used by this {@link ComponentProcessor} (if one is being used). This can be
	 * 	null, but in such a case you must override {@code processComponent} to do your own handling.
	 * @return		{@link ComponentSerializer}
	 * @param <T>	{@link Component}
	 * @param <A>	{@link Component}
	 */
	public abstract <T extends Component, A extends Component> ComponentSerializer<T, A, String> getSerializer();

	/**
	 * Checks the provided input to determine if this {@link ComponentProcessor} is the right tool for the job. This is
	 * 	used internally by {@code ComponentSerializer.findBestPick} to find the best fitting processor for what the user
	 * 	aims to do.
	 * @param input	String
	 * @return		boolean
	 */
	public abstract boolean shouldProcessComponent(String input);

	/**
	 * Takes the input and converts it into an Adventure {@link Component}.
	 * @param input	String
	 * @return		{@link Component}
	 */
	public Component processComponent(String input)
	{
		return getSerializer().deserialize(input);
	}

	/**
	 * Reads the input and attempts to find the best {@link ComponentProcessor} based on how the input is formatted. If
	 * 	nothing fits, it defaults to just plain text.
	 * @param input	String
	 * @return		{@link ComponentProcessor}
	 */
	public static ComponentProcessor findBestPick(String input)
	{
		return formatters.stream().filter(formatter -> formatter.shouldProcessComponent(input))
				.max(Comparator.comparingInt(formatters::indexOf)).orElse(plainText);
	}
}
