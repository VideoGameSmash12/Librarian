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

package me.videogamesm12.librarian.v1_14_4.addon;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.cottonmc.clientcommands.ArgumentBuilders;
import io.github.cottonmc.clientcommands.ClientCommandPlugin;
import io.github.cottonmc.clientcommands.CottonClientCommandSource;
import me.videogamesm12.librarian.Librarian;
import me.videogamesm12.librarian.api.HotbarPageMetadata;
import me.videogamesm12.librarian.api.IMechanicFactory;
import me.videogamesm12.librarian.api.IWrappedHotbarStorage;
import me.videogamesm12.librarian.util.ComponentProcessor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.client.options.HotbarStorage;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class CottonClientCommandsAddon implements ClientCommandPlugin
{
	private final IMechanicFactory mechanic = Librarian.getInstance().getMechanic();
	
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
											Set<BigInteger> pages = Librarian.getInstance().getMap().keySet();

											if (pages.isEmpty())
											{
												feedback(context.getSource(), Component.translatable("librarian.messages.cache_list.empty"));
											}
											else
											{
												feedback(context.getSource(), Component.translatable("librarian.messages.cache_list", Component.join(JoinConfiguration.commas(true), pages.stream().map(big -> Component.text(big.toString())).collect(Collectors.toList()))));
											}

											return 1;
										}))
								.then(ArgumentBuilders.literal("clear")
										.executes(context ->
										{
											Librarian.getInstance().clearCache();
											return 1;
										})))
						.then(ArgumentBuilders.literal("meta")
								.then(ArgumentBuilders.literal("delete").executes(context ->
								{
									IWrappedHotbarStorage page = Librarian.getInstance().getCurrentPage();
									if (page.getMetadata().isPresent())
									{
										page.setMetadata(null);
										feedback(context.getSource(), Component.translatable("librarian.messages.metadata.deleted").color(NamedTextColor.GRAY));
									}
									else
									{
										error(context.getSource(), Component.translatable("librarian.messages.metadata.no_data_to_delete"));
									}

									return 1;
								}))
								.then(ArgumentBuilders.literal("name")
										.executes(context ->
										{
											IWrappedHotbarStorage page = Librarian.getInstance().getCurrentPage();

											if (page.getMetadata().isPresent() && page.getMetadata().get().getName() != null)
											{
												feedback(context.getSource(), Component.translatable("librarian.messages.metadata.name", Objects.requireNonNull(page.getMetadata().get().getName())).color(NamedTextColor.GRAY));
											}
											else
											{
												feedback(context.getSource(), Component.translatable("librarian.messages.metadata.name_not_set").color(NamedTextColor.GRAY));
											}

											return 1;
										})
										.then(ArgumentBuilders.literal("set")
												.executes(context ->
												{
													IWrappedHotbarStorage page = Librarian.getInstance().getCurrentPage();
													page.getMetadata().ifPresent(meta -> meta.setName(null));
													feedback(context.getSource(), Component.translatable("librarian.messages.metadata.name_reset").color(NamedTextColor.GRAY));
													return 1;
												})
												.then(ArgumentBuilders.argument("value", StringArgumentType.greedyString())
														.executes((context ->
														{
															final String value = StringArgumentType.getString(context, "value");
															final Component processed = ComponentProcessor.findBestPick(value)
																	.processComponent(value);

															IWrappedHotbarStorage page = Librarian.getInstance().getCurrentPage();

															if (page.getMetadata().isPresent())
															{
																page.getMetadata().get().setName(processed);
															}
															else
															{
																page.setMetadata(HotbarPageMetadata.builder().name(processed).build());
															}

															((HotbarStorage) page).save();
															feedback(context.getSource(), Component.translatable("librarian.messages.metadata.name_set", processed).color(NamedTextColor.GRAY));
															return 1;
														})))))
								.then(ArgumentBuilders.literal("description")
										.executes(context ->
										{
											IWrappedHotbarStorage page = Librarian.getInstance().getCurrentPage();
											if (page.getMetadata().isPresent() && page.getMetadata().get().getDescription() != null)
											{
												feedback(context.getSource(), Component.translatable("librarian.messages.metadata.description", Objects.requireNonNull(page.getMetadata().get().getDescription())).color(NamedTextColor.GRAY));
											}
											else
											{
												feedback(context.getSource(), Component.translatable("librarian.messages.metadata.description_not_set"));
											}

											return 1;
										})
										.then(ArgumentBuilders.literal("set")
												.executes(context ->
												{
													IWrappedHotbarStorage page = Librarian.getInstance().getCurrentPage();
													page.getMetadata().ifPresent(meta ->
													{
														meta.setDescription(null);
														((HotbarStorage) page).save();
													});
													feedback(context.getSource(), Component.translatable("librarian.messages.metadata.description_reset").color(NamedTextColor.GRAY));
													return 1;
												})
												.then(ArgumentBuilders.argument("value", StringArgumentType.greedyString())
														.executes((context ->
														{
															final String value = StringArgumentType.getString(context, "value");
															final Component processed = ComponentProcessor.findBestPick(value)
																	.processComponent(value);

															IWrappedHotbarStorage page = Librarian.getInstance().getCurrentPage();
															if (page.getMetadata().isPresent())
															{
																page.getMetadata().get().setDescription(processed);
															}
															else
															{
																page.setMetadata(HotbarPageMetadata.builder().description(processed).build());
															}

															((HotbarStorage) page).save();
															feedback(context.getSource(), Component.translatable("librarian.messages.metadata.description_set", processed).color(NamedTextColor.GRAY));
															return 1;
														})))))
								.then(ArgumentBuilders.literal("authors")
										.then(ArgumentBuilders.literal("list")
												.executes(context ->
												{
													IWrappedHotbarStorage page = Librarian.getInstance().getCurrentPage();
													if (page.getMetadata().isPresent() && !page.getMetadata().get().getAuthors().isEmpty())
													{
														feedback(context.getSource(), Component.translatable("librarian.messages.metadata.authors", Component.join(JoinConfiguration.commas(true),
																page.getMetadata().get().getAuthors().stream().map(name -> Component.text(name).color(NamedTextColor.WHITE)).collect(Collectors.toList()))).color(NamedTextColor.GRAY));
													}
													else
													{
														feedback(context.getSource(), Component.translatable("librarian.messages.metadata.authors_empty").color(NamedTextColor.GRAY));
													}

													return 1;
												}))
										.then(ArgumentBuilders.literal("clear")
												.executes(context ->
												{
													IWrappedHotbarStorage page = Librarian.getInstance().getCurrentPage();
													page.getMetadata().ifPresent(meta ->
													{
														meta.getAuthors().clear();
														((HotbarStorage) page).save();
													});
													feedback(context.getSource(), Component.translatable("librarian.messages.metadata.authors_cleared").color(NamedTextColor.GRAY));

													return 1;
												}))
										.then(ArgumentBuilders.literal("add")
												.then(ArgumentBuilders.argument("name", StringArgumentType.greedyString())
														.executes(context ->
														{
															final String value = StringArgumentType.getString(context, "name");
															IWrappedHotbarStorage page = Librarian.getInstance().getCurrentPage();

															if (page.getMetadata().isPresent())
															{
																HotbarPageMetadata meta = page.getMetadata().get();
																if (!meta.getAuthors().contains(value))
																{
																	meta.addAuthor(value);
																	((HotbarStorage) page).save();
																}
																else
																{
																	error(context.getSource(), Component.translatable("librarian.messages.metadata.authors_already_added", Component.text(value).color(NamedTextColor.WHITE)).color(NamedTextColor.GRAY));
																	return 1;
																}
															}
															else
															{
																page.setMetadata(HotbarPageMetadata.builder().authors(new ArrayList<>(Collections.singletonList(value))).build());
																((HotbarStorage) page).save();
															}

															feedback(context.getSource(), Component.translatable("librarian.messages.metadata.authors_added", Component.text(value).color(NamedTextColor.WHITE)).color(NamedTextColor.GRAY));
															return 1;
														})))
										.then(ArgumentBuilders.literal("remove")
												.then(ArgumentBuilders.argument("name", StringArgumentType.greedyString())
														.executes(context ->
														{
															final String value = StringArgumentType.getString(context, "name");
															IWrappedHotbarStorage page = Librarian.getInstance().getCurrentPage();
															if (page.getMetadata().isPresent() && page.getMetadata().get().getAuthors().contains(value))
															{
																page.getMetadata().get().removeAuthor(value);
																((HotbarStorage) page).save();
																feedback(context.getSource(), Component.translatable("librarian.messages.metadata.authors_removed", Component.text(value)).color(NamedTextColor.GRAY));
															}
															else
															{
																error(context.getSource(), Component.translatable("librarian.messages.metadata.authors_not_included", Component.text(value)));
															}

															return 1;
														}))))));
	}

	private void feedback(CottonClientCommandSource source, Component message)
	{
		source.sendFeedback(mechanic.createText(message));
	}

	private void error(CottonClientCommandSource source, Component message)
	{
		source.sendError(mechanic.createText(message));
	}
}
