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

package me.videogamesm12.librarian.v1_20_2.addon;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import lombok.Getter;
import me.videogamesm12.librarian.Config;
import me.videogamesm12.librarian.Librarian;
import me.videogamesm12.librarian.api.HotbarPageMetadata;
import me.videogamesm12.librarian.api.IMechanicFactory;
import me.videogamesm12.librarian.api.IWrappedHotbarStorage;
import me.videogamesm12.librarian.api.addon.AddonMeta;
import me.videogamesm12.librarian.api.addon.IAddon;
import me.videogamesm12.librarian.util.ComponentProcessor;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.HotbarStorage;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

@AddonMeta(requiredMods = "fabric")
@Getter
public class FabricAPIAddon implements IAddon
{
	private final IMechanicFactory mechanic = Librarian.getInstance().getMechanic();

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
			if (MinecraftClient.getInstance().interactionManager != null &&
					MinecraftClient.getInstance().interactionManager.hasCreativeInventory())
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
					Librarian.getInstance().getCurrentPage().librarian$backup();
				}
			}
		});

		final Config.CommandSystemSettings commandSystem = Librarian.getInstance().getConfig().commandSystem();
		if (commandSystem.isEnabled())
		{
			ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) ->
			{
				final LiteralCommandNode<FabricClientCommandSource> mainCommand = dispatcher.register(ClientCommandManager.literal("librarian")
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
									Librarian.getInstance().getCurrentPage().librarian$backup();
									return 0;
								}))
						.then(ClientCommandManager.literal("cache")
								.then(ClientCommandManager.literal("list")
										.executes(context ->
										{
											Set<BigInteger> pages = Librarian.getInstance().getMap().keySet();

											if (pages.isEmpty())
											{
												feedback(context.getSource(), Component.translatable("librarian.messages.cache_list.empty"));
											}
											else
											{
												feedback(context.getSource(), Component.translatable("librarian.messages.cache_list", Component.join(JoinConfiguration.commas(true), pages.stream().map(big -> Component.text(big.toString())).toList())));
											}

											return 0;
										}))
								.then(ClientCommandManager.literal("clear")
										.executes(context ->
										{
											Librarian.getInstance().clearCache();
											return 0;
										})))
						.then(ClientCommandManager.literal("meta")
								.then(ClientCommandManager.literal("delete").executes(context ->
								{
									IWrappedHotbarStorage page = Librarian.getInstance().getCurrentPage();
									page.librarian$getMetadata().ifPresentOrElse(meta ->
									{
										page.librarian$setMetadata(null);
										feedback(context.getSource(), Component.translatable("librarian.messages.metadata.deleted").color(NamedTextColor.GRAY));
									}, () -> error(context.getSource(), Component.translatable("librarian.messages.metadata.no_data_to_delete")));

									return 0;
								}))
								.then(ClientCommandManager.literal("name")
										.executes(context ->
										{
											IWrappedHotbarStorage page = Librarian.getInstance().getCurrentPage();
											if (page.librarian$getMetadata().isEmpty() || page.librarian$getMetadata().isPresent() && page.librarian$getMetadata().get().getName() == null)
											{
												feedback(context.getSource(), Component.translatable("librarian.messages.metadata.name_not_set").color(NamedTextColor.GRAY));
											}
											else
											{
												feedback(context.getSource(), Component.translatable("librarian.messages.metadata.name",
														Objects.requireNonNull(page.librarian$getMetadata().get().getName())).color(NamedTextColor.GRAY));
											}

											return 0;
										})
										.then(ClientCommandManager.literal("set")
												.executes(context ->
												{
													IWrappedHotbarStorage page = Librarian.getInstance().getCurrentPage();
													page.librarian$getMetadata().ifPresent(meta -> meta.setName(null));
													feedback(context.getSource(), Component.translatable("librarian.messages.metadata.name_reset").color(NamedTextColor.GRAY));
													return 0;
												})
												.then(ClientCommandManager.argument("value", StringArgumentType.greedyString())
														.executes((context ->
														{
															final String value = StringArgumentType.getString(context, "value");
															final Component processed = ComponentProcessor.findBestPick(value)
																	.processComponent(value);

															IWrappedHotbarStorage page = Librarian.getInstance().getCurrentPage();
															page.librarian$getMetadata().ifPresentOrElse(
																	meta -> meta.setName(processed),
																	() -> page.librarian$setMetadata(HotbarPageMetadata.builder().name(processed).build()));

															((HotbarStorage) page).save();
															feedback(context.getSource(), Component.translatable("librarian.messages.metadata.name_set", processed).color(NamedTextColor.GRAY));
															return 0;
														})))))
								.then(ClientCommandManager.literal("description")
										.executes(context ->
										{
											IWrappedHotbarStorage page = Librarian.getInstance().getCurrentPage();
											if (page.librarian$getMetadata().isEmpty() || page.librarian$getMetadata().isPresent() && page.librarian$getMetadata().get().getDescription() == null)
											{
												feedback(context.getSource(), Component.translatable("librarian.messages.metadata.description_not_set"));
											}
											else
											{
												feedback(context.getSource(), Component.translatable("librarian.messages.metadata.description",
														Objects.requireNonNull(page.librarian$getMetadata().get().getDescription())).color(NamedTextColor.GRAY));
											}

											return 0;
										})
										.then(ClientCommandManager.literal("set")
												.executes(context ->
												{
													IWrappedHotbarStorage page = Librarian.getInstance().getCurrentPage();
													page.librarian$getMetadata().ifPresent(meta ->
													{
														meta.setDescription(null);
														((HotbarStorage) page).save();
													});
													feedback(context.getSource(), Component.translatable("librarian.messages.metadata.description_reset").color(NamedTextColor.GRAY));
													return 0;
												})
												.then(ClientCommandManager.argument("value", StringArgumentType.greedyString())
														.executes((context ->
														{
															final String value = StringArgumentType.getString(context, "value");
															final Component processed = ComponentProcessor.findBestPick(value)
																	.processComponent(value);

															IWrappedHotbarStorage page = Librarian.getInstance().getCurrentPage();
															page.librarian$getMetadata().ifPresentOrElse(
																	meta -> meta.setDescription(processed),
																	() -> page.librarian$setMetadata(HotbarPageMetadata.builder().description(processed).build()));

															((HotbarStorage) page).save();
															feedback(context.getSource(), Component.translatable("librarian.messages.metadata.description_set", processed).color(NamedTextColor.GRAY));
															return 0;
														})))))
								.then(ClientCommandManager.literal("authors")
										.then(ClientCommandManager.literal("list")
												.executes(context ->
												{
													IWrappedHotbarStorage page = Librarian.getInstance().getCurrentPage();
													if (page.librarian$getMetadata().isEmpty() || page.librarian$getMetadata().isPresent() && page.librarian$getMetadata().get().getAuthors().isEmpty())
													{
														feedback(context.getSource(), Component.translatable("librarian.messages.metadata.authors_empty").color(NamedTextColor.GRAY));
													}
													else
													{
														feedback(context.getSource(), Component.translatable("librarian.messages.metadata.authors",
																Component.join(JoinConfiguration.commas(true),
																		page.librarian$getMetadata().get().getAuthors().stream().map(name -> Component.text(name).color(NamedTextColor.WHITE)).toList())).color(NamedTextColor.GRAY));
													}

													return 0;
												}))
										.then(ClientCommandManager.literal("clear")
												.executes(context ->
												{
													IWrappedHotbarStorage page = Librarian.getInstance().getCurrentPage();
													page.librarian$getMetadata().ifPresent(meta ->
													{
														meta.getAuthors().clear();
														((HotbarStorage) page).save();
													});

													feedback(context.getSource(), Component.translatable("librarian.messages.metadata.authors_cleared").color(NamedTextColor.GRAY));
													return 0;
												}))
										.then(ClientCommandManager.literal("add")
												.then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
														.executes(context ->
														{
															final String value = StringArgumentType.getString(context, "name");
															IWrappedHotbarStorage page = Librarian.getInstance().getCurrentPage();
															page.librarian$getMetadata().ifPresentOrElse(meta ->
															{
																if (!meta.getAuthors().contains(value))
																{
																	meta.addAuthor(value);
																	((HotbarStorage) page).save();
																	feedback(context.getSource(), Component.translatable("librarian.messages.metadata.authors_added", Component.text(value).color(NamedTextColor.WHITE)).color(NamedTextColor.GRAY));
																}
																else
																{
																	error(context.getSource(), Component.translatable("librarian.messages.metadata.authors_already_added", Component.text(value).color(NamedTextColor.WHITE)).color(NamedTextColor.GRAY));
																}

															}, () ->
															{
																page.librarian$setMetadata(HotbarPageMetadata.builder().authors(new ArrayList<>(Collections.singletonList(value))).build());
																((HotbarStorage) page).save();
																feedback(context.getSource(), Component.translatable("librarian.messages.metadata.authors_added", Component.text(value).color(NamedTextColor.WHITE)).color(NamedTextColor.GRAY));
															});
															return 0;
														})))
										.then(ClientCommandManager.literal("remove")
												.then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
														.executes(context ->
														{
															final String value = StringArgumentType.getString(context, "name");
															IWrappedHotbarStorage page = Librarian.getInstance().getCurrentPage();
															if (page.librarian$getMetadata().isEmpty() || page.librarian$getMetadata().isPresent() && !page.librarian$getMetadata().get().getAuthors().contains(value))
															{
																error(context.getSource(), Component.translatable("librarian.messages.metadata.authors_not_included", Component.text(value)));
															}
															else
															{
																page.librarian$getMetadata().get().removeAuthor(value);
																((HotbarStorage) page).save();
																feedback(context.getSource(), Component.translatable("librarian.messages.metadata.authors_removed", Component.text(value)).color(NamedTextColor.GRAY));
															}

															return 0;
														}))))));

				commandSystem.getAliases().stream().map(alias -> ClientCommandManager.literal(alias)
						.redirect(mainCommand)).forEach(dispatcher::register);
			});
		}
	}

	private void feedback(FabricClientCommandSource source, Component message)
	{
		source.sendFeedback(mechanic.createText(message));
	}

	private void error(FabricClientCommandSource source, Component message)
	{
		source.sendError(mechanic.createText(message));
	}
}
