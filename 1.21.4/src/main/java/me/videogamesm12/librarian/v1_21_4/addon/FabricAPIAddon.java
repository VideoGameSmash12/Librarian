package me.videogamesm12.librarian.v1_21_4.addon;

import com.mojang.brigadier.arguments.LongArgumentType;
import lombok.Getter;
import me.videogamesm12.librarian.Librarian;
import me.videogamesm12.librarian.api.addon.AddonMeta;
import me.videogamesm12.librarian.api.addon.IAddon;
import me.videogamesm12.librarian.util.FNF;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
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
					.then(ClientCommandManager.literal("goto").then(
							ClientCommandManager.argument("page", LongArgumentType.longArg()).executes((context) ->
							{
								Librarian.getInstance().setPage(LongArgumentType.getLong(context, "page"));
								return 0;
							})))
					.then(ClientCommandManager.literal("next").executes((context) ->
					{
						Librarian.getInstance().nextPage();
						return 0;
					}/*).then(
							ClientCommandManager.argument("amount", LongArgumentType.longArg()).executes(context ->
							{
								Librarian.getInstance().advanceBy(LongArgumentType.getLong(context, "amount"));
								return 0;
							})*/))
					.then(ClientCommandManager.literal("previous").executes((context) ->
					{
						Librarian.getInstance().previousPage();
						return 0;
					}))
					.then(ClientCommandManager.literal("backup").executes((context) ->
					{
						Librarian.getInstance().getCurrentPage().backup();
						return 0;
					}))
					.then(ClientCommandManager.literal("rebuild").executes((context ->
					{
						MinecraftClient.getInstance().setScreen(new ConfirmScreen((bool) -> {
							if (bool) Librarian.getInstance().getMechanic().overhaulHotbars(FNF.getAllPageFiles());
							MinecraftClient.getInstance().setScreen(null);
						}, Text.translatable("librarian.messages.rebuild.prompt.title"), Text.translatable("librarian.messages.rebuild.prompt.description")));
						return 0;
					})))
					.then(ClientCommandManager.literal("cache")
							.then(ClientCommandManager.literal("list"))
							.then(ClientCommandManager.literal("clear")
									.executes((context ->
									{
										Librarian.getInstance().clearCache();
										return 0;
									}))))
					/*.then(ClientCommandManager.literal("import")
							// An argument was provided
							.then(ClientCommandManager.argument("path", StringArgumentType.greedyString()).executes((context) ->
							{
								Librarian.getLogger().info("IMPORT {}", StringArgumentType.getString(context, "path"));
								return 0;
							}))
							// No arguments provided
							.executes((context) ->
							{
								final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

								Object object = ClipboardParser.readClipboard(clipboard, DataFlavor.javaFileListFlavor,
										DataFlavor.stringFlavor, DataFlavor.getTextPlainUnicodeFlavor());

								// Assume file list
								if (object instanceof List list)
								{
									final List<File> files = (List<File>) list;

								}
								else if (object instanceof String str)
								{

								}

								try
								{
								}
								catch (NullPointerException ex)
								{
									Librarian.getLogger().error("FAILED TO GET APPROPRIATE FLAVOR", ex);
								}

								//Librarian.getLogger().info("CLIPBOARD {}", MinecraftClient.getInstance().keyboard.getClipboard());
								return 0;
							}))*/
			);
		});
	}
}
