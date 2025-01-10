package me.videogamesm12.librarian.v1_14_4.addon;

import lombok.Getter;
import me.videogamesm12.librarian.Librarian;
import me.videogamesm12.librarian.api.addon.AddonMeta;
import me.videogamesm12.librarian.api.addon.IAddon;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

@AddonMeta(requiredMods = "fabric")
@Getter
public class FabricAPIAddon implements IAddon
{
	private KeyBinding nextKey = null;
	private KeyBinding previousKey = null;
	private KeyBinding backupKey = null;

	@Override
	public void init()
	{
		nextKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("librarian.key.next_page",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_RIGHT_BRACKET,
				"category.librarian.navigation"));
		previousKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("librarian.key.previous_page",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_LEFT_BRACKET,
				"category.librarian.navigation"));
		backupKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("librarian.key.backup_current_page",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_B,
				"category.librarian.actions"));

		ClientTickEvents.END_CLIENT_TICK.register(client ->
		{
			if (nextKey.wasPressed())
			{
				Librarian.getInstance().nextPage();
			}
			else if (previousKey.wasPressed())
			{
				Librarian.getInstance().previousPage();
			}
			else if (backupKey.wasPressed())
			{
				Librarian.getInstance().getCurrentPage().backup();
			}
		});
	}
}
