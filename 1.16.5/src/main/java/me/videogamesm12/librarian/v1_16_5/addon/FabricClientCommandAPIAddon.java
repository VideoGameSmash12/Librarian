package me.videogamesm12.librarian.v1_16_5.addon;

import com.mojang.brigadier.arguments.LongArgumentType;
import me.videogamesm12.librarian.Librarian;
import me.videogamesm12.librarian.api.addon.AddonMeta;
import me.videogamesm12.librarian.api.addon.IAddon;
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;

@AddonMeta(requiredMods = "fabric-command-api-v1")
public class FabricClientCommandAPIAddon implements IAddon
{
	@Override
	public void init()
	{
		ClientCommandManager.DISPATCHER.register(
				ClientCommandManager.literal("librarian")
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
	}
}
