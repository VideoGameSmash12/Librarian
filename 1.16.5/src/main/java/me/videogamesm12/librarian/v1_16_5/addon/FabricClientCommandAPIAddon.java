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

package me.videogamesm12.librarian.v1_16_5.addon;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import me.videogamesm12.librarian.Librarian;
import me.videogamesm12.librarian.api.HotbarPageMetadata;
import me.videogamesm12.librarian.api.IMechanicFactory;
import me.videogamesm12.librarian.api.IWrappedHotbarStorage;
import me.videogamesm12.librarian.api.addon.AddonMeta;
import me.videogamesm12.librarian.api.addon.IAddon;
import me.videogamesm12.librarian.util.ComponentProcessor;
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.client.option.HotbarStorage;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@AddonMeta(requiredMods = "fabric-command-api-v1")
public class FabricClientCommandAPIAddon implements IAddon
{
	private final IMechanicFactory mechanic = Librarian.getInstance().getMechanic();

	@Override
	public void init()
	{
		final LiteralCommandNode<FabricClientCommandSource> mainCommand = ClientCommandManager.DISPATCHER.register(
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
												feedback(context.getSource(), Component.translatable("librarian.messages.cache_list", Component.join(JoinConfiguration.commas(true), pages.stream().map(big -> Component.text(big.toString())).collect(Collectors.toList()))));
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
									if (page.librarian$getMetadata().isPresent())
									{
										page.librarian$setMetadata(null);
										feedback(context.getSource(), Component.translatable("librarian.messages.metadata.deleted").color(NamedTextColor.GRAY));
									}
									else
									{
										error(context.getSource(), Component.translatable("librarian.messages.metadata.no_data_to_delete"));
									}

									return 0;
								}))
								.then(ClientCommandManager.literal("name")
										.executes(context ->
										{
											IWrappedHotbarStorage page = Librarian.getInstance().getCurrentPage();

											if (page.librarian$getMetadata().isPresent() && page.librarian$getMetadata().get().getName() != null)
											{
												feedback(context.getSource(), Component.translatable("librarian.messages.metadata.name", Objects.requireNonNull(page.librarian$getMetadata().get().getName())).color(NamedTextColor.GRAY));
											}
											else
											{
												feedback(context.getSource(), Component.translatable("librarian.messages.metadata.name_not_set").color(NamedTextColor.GRAY));
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

															if (page.librarian$getMetadata().isPresent())
															{
																page.librarian$getMetadata().get().setName(processed);
															}
															else
															{
																page.librarian$setMetadata(HotbarPageMetadata.builder().name(processed).build());
															}

															((HotbarStorage) page).save();
															feedback(context.getSource(), Component.translatable("librarian.messages.metadata.name_set", processed).color(NamedTextColor.GRAY));
															return 0;
														})))))
								.then(ClientCommandManager.literal("description")
										.executes(context ->
										{
											IWrappedHotbarStorage page = Librarian.getInstance().getCurrentPage();
											if (page.librarian$getMetadata().isPresent() && page.librarian$getMetadata().get().getDescription() != null)
											{
												feedback(context.getSource(), Component.translatable("librarian.messages.metadata.description", Objects.requireNonNull(page.librarian$getMetadata().get().getDescription())).color(NamedTextColor.GRAY));
											}
											else
											{
												feedback(context.getSource(), Component.translatable("librarian.messages.metadata.description_not_set"));
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
															if (page.librarian$getMetadata().isPresent())
															{
																page.librarian$getMetadata().get().setDescription(processed);
															}
															else
															{
																page.librarian$setMetadata(HotbarPageMetadata.builder().description(processed).build());
															}

															((HotbarStorage) page).save();
															feedback(context.getSource(), Component.translatable("librarian.messages.metadata.description_set", processed).color(NamedTextColor.GRAY));
															return 0;
														})))))
								.then(ClientCommandManager.literal("authors")
										.then(ClientCommandManager.literal("list")
												.executes(context ->
												{
													IWrappedHotbarStorage page = Librarian.getInstance().getCurrentPage();
													if (page.librarian$getMetadata().isPresent() && !page.librarian$getMetadata().get().getAuthors().isEmpty())
													{
														feedback(context.getSource(), Component.translatable("librarian.messages.metadata.authors",
																Component.join(JoinConfiguration.commas(true),
																		page.librarian$getMetadata().get().getAuthors().stream().map(name -> Component.text(name).color(NamedTextColor.WHITE)).collect(Collectors.toList()))).color(NamedTextColor.GRAY));
													}
													else
													{
														feedback(context.getSource(), Component.translatable("librarian.messages.metadata.authors_empty").color(NamedTextColor.GRAY));
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

															if (page.librarian$getMetadata().isPresent())
															{
																HotbarPageMetadata meta = page.librarian$getMetadata().get();
																if (!meta.getAuthors().contains(value))
																{
																	meta.addAuthor(value);
																	((HotbarStorage) page).save();
																}
																else
																{
																	error(context.getSource(), Component.translatable("librarian.messages.metadata.authors_already_added", Component.text(value).color(NamedTextColor.WHITE)).color(NamedTextColor.GRAY));
																	return 0;
																}
															}
															else
															{
																page.librarian$setMetadata(HotbarPageMetadata.builder().authors(new ArrayList<>(Collections.singletonList(value))).build());
																((HotbarStorage) page).save();
															}

															feedback(context.getSource(), Component.translatable("librarian.messages.metadata.authors_added", Component.text(value).color(NamedTextColor.WHITE)).color(NamedTextColor.GRAY));
															return 0;
														})))
										.then(ClientCommandManager.literal("remove")
												.then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
														.executes(context ->
														{
															final String value = StringArgumentType.getString(context, "name");
															IWrappedHotbarStorage page = Librarian.getInstance().getCurrentPage();
															if (page.librarian$getMetadata().isPresent() && page.librarian$getMetadata().get().getAuthors().contains(value))
															{
																page.librarian$getMetadata().get().removeAuthor(value);
																((HotbarStorage) page).save();
																feedback(context.getSource(), Component.translatable("librarian.messages.metadata.authors_removed", Component.text(value)).color(NamedTextColor.GRAY));
															}
															else
															{
																error(context.getSource(), Component.translatable("librarian.messages.metadata.authors_not_included", Component.text(value)));
															}

															return 0;
														}))))));

		ClientCommandManager.DISPATCHER.register(ClientCommandManager.literal("lb").redirect(mainCommand));
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
