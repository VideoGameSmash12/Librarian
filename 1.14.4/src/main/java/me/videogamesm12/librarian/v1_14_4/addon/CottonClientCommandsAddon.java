package me.videogamesm12.librarian.v1_14_4.addon;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.cottonmc.clientcommands.ArgumentBuilders;
import io.github.cottonmc.clientcommands.ClientCommandPlugin;
import io.github.cottonmc.clientcommands.CottonClientCommandSource;
import me.videogamesm12.librarian.Librarian;
import me.videogamesm12.librarian.api.HotbarPageMetadata;
import me.videogamesm12.librarian.api.IWrappedHotbarStorage;
import me.videogamesm12.librarian.util.ComponentProcessor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.client.options.HotbarStorage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;

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
										context.getSource().sendFeedback(Librarian.getInstance().getMechanic().createText(Component.translatable("librarian.messages.metadata.deleted").color(NamedTextColor.GRAY)));
									}
									else
									{
										context.getSource().sendError(Librarian.getInstance().getMechanic().createText(Component.translatable("librarian.messages.metadata.no_data_to_delete")));
									}

									return 1;
								}))
								.then(ArgumentBuilders.literal("name")
										.executes(context ->
										{
											IWrappedHotbarStorage page = Librarian.getInstance().getCurrentPage();

											if (page.getMetadata().isPresent() && page.getMetadata().get().getName() != null)
											{
												context.getSource().sendFeedback(Librarian.getInstance().getMechanic()
														.createText(Component.translatable("librarian.messages.metadata.name", page.getMetadata().get().getName()).color(NamedTextColor.GRAY)));
											}
											else
											{
												context.getSource().sendFeedback(Librarian.getInstance().getMechanic()
														.createText(Component.translatable("librarian.messages.metadata.name_not_set").color(NamedTextColor.GRAY)));
											}

											return 1;
										})
										.then(ArgumentBuilders.literal("set")
												.executes(context ->
												{
													IWrappedHotbarStorage page = Librarian.getInstance().getCurrentPage();
													page.getMetadata().ifPresent(meta -> meta.setName(null));
													context.getSource().sendFeedback(Librarian.getInstance().getMechanic().createText(Component.translatable("librarian.messages.metadata.name_reset").color(NamedTextColor.GRAY)));
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
															context.getSource().sendFeedback(Librarian.getInstance().getMechanic().createText(Component.translatable("librarian.messages.metadata.name_set", processed).color(NamedTextColor.GRAY)));
															return 1;
														})))))
								.then(ArgumentBuilders.literal("description")
										.executes(context ->
										{
											IWrappedHotbarStorage page = Librarian.getInstance().getCurrentPage();
											if (page.getMetadata().isPresent() && page.getMetadata().get().getDescription() != null)
											{
												context.getSource().sendFeedback(Librarian.getInstance().getMechanic()
														.createText(Component.translatable("librarian.messages.metadata.description", page.getMetadata().get().getDescription()).color(NamedTextColor.GRAY)));
											}
											else
											{
												context.getSource().sendFeedback(Librarian.getInstance().getMechanic()
														.createText(Component.translatable("librarian.messages.metadata.description_not_set")));
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
													context.getSource().sendFeedback(Librarian.getInstance().getMechanic().createText(Component.translatable("librarian.messages.metadata.description_reset").color(NamedTextColor.GRAY)));
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
															context.getSource().sendFeedback(Librarian.getInstance().getMechanic().createText(Component.translatable("librarian.messages.metadata.description_set", processed).color(NamedTextColor.GRAY)));
															return 1;
														})))))
								.then(ArgumentBuilders.literal("authors")
										.then(ArgumentBuilders.literal("list")
												.executes(context ->
												{
													IWrappedHotbarStorage page = Librarian.getInstance().getCurrentPage();
													if (page.getMetadata().isPresent() && !page.getMetadata().get().getAuthors().isEmpty())
													{
														context.getSource().sendFeedback(Librarian.getInstance().getMechanic()
																.createText(Component.translatable("librarian.messages.metadata.authors",
																		Component.join(JoinConfiguration.commas(true),
																				page.getMetadata().get().getAuthors().stream().map(name -> Component.text(name).color(NamedTextColor.WHITE)).collect(Collectors.toList()))).color(NamedTextColor.GRAY)));
													}
													else
													{
														context.getSource().sendFeedback(Librarian.getInstance().getMechanic()
																.createText(Component.translatable("librarian.messages.metadata.authors_empty").color(NamedTextColor.GRAY)));
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
													context.getSource().sendFeedback(Librarian.getInstance().getMechanic().createText(Component.translatable("librarian.messages.metadata.authors_cleared").color(NamedTextColor.GRAY)));

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
																	context.getSource().sendError(Librarian.getInstance().getMechanic().createText(Component.translatable("librarian.messages.metadata.authors_already_added", Component.text(value).color(NamedTextColor.WHITE)).color(NamedTextColor.GRAY)));
																	return 1;
																}
															}
															else
															{
																page.setMetadata(HotbarPageMetadata.builder().authors(new ArrayList<>(Collections.singletonList(value))).build());
																((HotbarStorage) page).save();
															}

															context.getSource().sendFeedback(Librarian.getInstance().getMechanic().createText(Component.translatable("librarian.messages.metadata.authors_added", Component.text(value).color(NamedTextColor.WHITE)).color(NamedTextColor.GRAY)));
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
																context.getSource().sendFeedback(Librarian.getInstance().getMechanic().createText(Component.translatable("librarian.messages.metadata.authors_removed", Component.text(value)).color(NamedTextColor.GRAY)));
															}
															else
															{
																context.getSource().sendError(Librarian.getInstance().getMechanic().createText(Component.translatable("librarian.messages.metadata.authors_not_included", Component.text(value))));
															}

															return 1;
														}))))));
	}
}
