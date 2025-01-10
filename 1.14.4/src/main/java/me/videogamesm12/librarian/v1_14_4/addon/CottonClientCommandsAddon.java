package me.videogamesm12.librarian.v1_14_4.addon;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import io.github.cottonmc.clientcommands.ArgumentBuilders;
import io.github.cottonmc.clientcommands.ClientCommandPlugin;
import io.github.cottonmc.clientcommands.CottonClientCommandSource;
import me.videogamesm12.librarian.Librarian;

public class CottonClientCommandsAddon implements ClientCommandPlugin
{
	@Override
	public void registerCommands(CommandDispatcher<CottonClientCommandSource> commandDispatcher)
	{
		commandDispatcher.register(
				ArgumentBuilders.literal("librarian")
						.then(ArgumentBuilders.literal("goto")
								.then(ArgumentBuilders.argument("page", LongArgumentType.longArg())
										.executes(context ->
										{
											Librarian.getInstance().setPage(LongArgumentType.getLong(context, "page"));
											return 1;
										})))
						.then(ArgumentBuilders.literal("next")
								.executes(context ->
								{
									Librarian.getInstance().nextPage();
									return 1;
								}))
						.then(ArgumentBuilders.literal("previous")
								.executes(context ->
								{
									Librarian.getInstance().previousPage();
									return 1;
								}))
						.then(ArgumentBuilders.literal("backup")
								.executes(context ->
								{
									Librarian.getInstance().getCurrentPage().backup();
									return 1;
								}))
						.then(ArgumentBuilders.literal("cache")
								.then(ArgumentBuilders.literal("list")
										.executes(context ->
										{
											return 0;
										}))
								.then(ArgumentBuilders.literal("clear")
										.executes(context ->
										{
											Librarian.getInstance().clearCache();
											return 1;
										}))));
	}
}
