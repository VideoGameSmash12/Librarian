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

package me.videogamesm12.librarian.v1_13_2.ornithe.addon;

import lombok.Getter;
import me.videogamesm12.librarian.Librarian;
import me.videogamesm12.librarian.api.addon.AddonMeta;
import me.videogamesm12.librarian.api.addon.IAddon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.options.KeyBinding;
import net.ornithemc.osl.keybinds.api.KeyBindingEvents;
import net.ornithemc.osl.lifecycle.api.client.MinecraftClientEvents;
import org.lwjgl.glfw.GLFW;

@AddonMeta(requiredMods = "osl")
@Getter
public class OSLAddon implements IAddon
{
	private KeyBinding nextKey;
	private KeyBinding backupKey;
	private KeyBinding previousKey;

	@Override
	public void init()
	{
		KeyBindingEvents.REGISTER_KEYBINDS.register((listener) ->
		{
			nextKey = listener.register("librarian.key.next_page", GLFW.GLFW_KEY_RIGHT_BRACKET,
					"category.librarian.navigation");
			backupKey = listener.register("librarian.key.backup_current_page", GLFW.GLFW_KEY_B, "category.librarian.actions");
			previousKey = listener.register("librarian.key.previous_page", GLFW.GLFW_KEY_LEFT_BRACKET,
					"category.librarian.navigation");
		});

		MinecraftClientEvents.TICK_END.register((instance) ->
		{
			if (Minecraft.getInstance().interactionManager != null &&
					Minecraft.getInstance().interactionManager.hasCreativeInventory())
			{
				if (nextKey.consumeClick())
				{
					Librarian.getInstance().nextPage();
				}
				else if (backupKey.consumeClick())
				{
					Librarian.getInstance().getCurrentPage().librarian$backup();
				}
				else if (previousKey.consumeClick())
				{
					Librarian.getInstance().previousPage();
				}
			}
		});
	}
}
