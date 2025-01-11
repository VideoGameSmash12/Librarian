package me.videogamesm12.librarian.v1_20_4.addon;

import com.mojang.brigadier.arguments.LongArgumentType;
import lombok.Getter;
import me.videogamesm12.librarian.Librarian;
import me.videogamesm12.librarian.api.addon.AddonMeta;
import me.videogamesm12.librarian.api.addon.IAddon;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
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

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) ->
		{
			dispatcher.register(ClientCommandManager.literal("librarian")
					.then(ClientCommandManager.literal("goto")
							.then(ClientCommandManager.argument("page", LongArgumentType.longArg())
									.executes(context ->
									{
										Librarian.getInstance().setPage(LongArgumentType.getLong(context, "page"));
										return 0;
									})))
					.then(ClientCommandManager.literal("next")
							.executes(context ->
							{
								Librarian.getInstance().nextPage();
								return 0;
							}))
					.then(ClientCommandManager.literal("previous")
							.executes(context ->
							{
								Librarian.getInstance().previousPage();
								return 0;
							}))
					.then(ClientCommandManager.literal("backup")
							.executes(context ->
							{
								Librarian.getInstance().getCurrentPage().backup();
								return 0;
							}))
					.then(ClientCommandManager.literal("cache")
							.then(ClientCommandManager.literal("list")
									.executes(context ->
									{
										return 0;
									}))
							.then(ClientCommandManager.literal("clear")
									.executes(context ->
									{
										Librarian.getInstance().clearCache();
										return 0;
									}))));
		});
	}
}
