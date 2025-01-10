package me.videogamesm12.librarian.v1_14_4.ornithe.addon;

import lombok.Getter;
import me.videogamesm12.librarian.Librarian;
import me.videogamesm12.librarian.api.addon.AddonMeta;
import me.videogamesm12.librarian.api.addon.IAddon;
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

	private boolean keyPressed = false;

	@Override
	public void init()
	{
		KeyBindingEvents.REGISTER_KEYBINDS.register((listener) ->
		{
			nextKey = listener.register("librarian.key.next_page", GLFW.GLFW_KEY_RIGHT_BRACKET,
					"category.librarian.navigation");
			backupKey = listener.register("librarian.key.backup", GLFW.GLFW_KEY_B, "category.librarian.actions");
			previousKey = listener.register("librarian.key.previous_page", GLFW.GLFW_KEY_LEFT_BRACKET,
					"category.librarian.navigation");
		});

		MinecraftClientEvents.TICK_END.register((instance) ->
		{
			if (keyPressed && (nextKey.isPressed() || backupKey.isPressed() || previousKey.isPressed()))
			{
				return;
			}

			keyPressed = false;

			if (nextKey.isPressed())
			{
				Librarian.getInstance().nextPage();
				keyPressed = true;
			}
			else if (backupKey.isPressed())
			{
				Librarian.getInstance().getCurrentPage().backup();
				keyPressed = true;
			}
			else if (previousKey.isPressed())
			{
				Librarian.getInstance().previousPage();
				keyPressed = true;
			}
		});
	}
}
