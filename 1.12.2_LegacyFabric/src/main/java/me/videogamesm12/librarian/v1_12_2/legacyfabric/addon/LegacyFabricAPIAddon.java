package me.videogamesm12.librarian.v1_12_2.legacyfabric.addon;

import lombok.Getter;
import me.videogamesm12.librarian.Librarian;
import me.videogamesm12.librarian.api.addon.AddonMeta;
import me.videogamesm12.librarian.api.addon.IAddon;
import net.legacyfabric.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.legacyfabric.fabric.api.client.keybinding.v1.KeyBindingHelper;
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
		backupKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("librarian.key.backup", Keyboard.KEY_B,
				"category.librarian.actions"));
		previousKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("librarian.key.previous_page", Keyboard.KEY_LBRACKET,
				"category.librarian.navigation"));

		ClientTickEvents.END_CLIENT_TICK.register((client) ->
		{
			if (nextKey.wasPressed())
			{
				Librarian.getInstance().nextPage();
			}
			else if (backupKey.wasPressed())
			{
				Librarian.getInstance().getCurrentPage().backup();
			}
			else if (previousKey.wasPressed())
			{
				Librarian.getInstance().previousPage();
			}
		});
	}
}
