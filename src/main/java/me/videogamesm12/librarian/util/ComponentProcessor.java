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

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

public abstract class ComponentProcessor
{
	@Getter
	private static final List<ComponentProcessor> formatters = new ArrayList<>();
	private static final ComponentProcessor legacyAmpersand;
	private static final ComponentProcessor legacySection;
	private static final ComponentProcessor miniMessage;
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
		formatters.add(miniMessage);
		formatters.add(legacySection);
		formatters.add(legacyAmpersand);
	}

	public abstract <T extends Component, A extends Component> ComponentSerializer<T, A, String> getSerializer();

	public abstract boolean shouldProcessComponent(String input);

	public Component processComponent(String input)
	{
		return getSerializer().deserialize(input);
	}

	public static ComponentProcessor findBestPick(String input)
	{
		return formatters.stream().filter(formatter -> formatter.shouldProcessComponent(input))
				.max(Comparator.comparingInt(formatters::indexOf)).orElse(plainText);
	}
}
