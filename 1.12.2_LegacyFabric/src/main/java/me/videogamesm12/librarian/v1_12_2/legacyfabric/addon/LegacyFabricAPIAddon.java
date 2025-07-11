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

package me.videogamesm12.librarian.v1_12_2.legacyfabric.addon;

import lombok.Getter;
import me.videogamesm12.librarian.Librarian;
import me.videogamesm12.librarian.api.addon.AddonMeta;
import me.videogamesm12.librarian.api.addon.IAddon;
import net.legacyfabric.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.legacyfabric.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.input.Keyboard;

@AddonMeta(requiredMods = "legacy-fabric-api")
@Getter
public class LegacyFabricAPIAddon implements IAddon
{
	private KeyBinding nextKey;
	private KeyBinding backupKey;
	private KeyBinding previousKey;

	@Override
	public void init()
	{
		nextKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("librarian.key.next_page", Keyboard.KEY_RBRACKET,
				"category.librarian.navigation"));
		backupKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("librarian.key.backup_current_page", Keyboard.KEY_B,
				"category.librarian.actions"));
		previousKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("librarian.key.previous_page", Keyboard.KEY_LBRACKET,
				"category.librarian.navigation"));

		ClientTickEvents.END_CLIENT_TICK.register((client) ->
		{
			if (MinecraftClient.getInstance().interactionManager != null &&
					MinecraftClient.getInstance().interactionManager.hasCreativeInventory())
			{
				if (nextKey.wasPressed())
				{
					Librarian.getInstance().nextPage();
				}
				else if (backupKey.wasPressed())
				{
					Librarian.getInstance().getCurrentPage().librarian$backup();
				}
				else if (previousKey.wasPressed())
				{
					Librarian.getInstance().previousPage();
				}
			}
		});
	}
}
