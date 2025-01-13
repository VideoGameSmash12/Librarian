package me.videogamesm12.librarian.v1_8_9.legacyfabric;

import lombok.Getter;
import me.videogamesm12.librarian.Librarian;
import net.fabricmc.api.ClientModInitializer;
import net.legacyfabric.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.legacyfabric.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.legacyfabric.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.legacyfabric.fabric.api.util.Identifier;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.itemgroup.ItemGroup;
import net.minecraft.world.level.LevelInfo;
import org.lwjgl.input.Keyboard;

public class CursedLibrarian implements ClientModInitializer
{
	@Getter
	private static ItemGroup group = null;
	@Getter
	private static CursedLibrarian instance;

	private KeyBinding saveKey;
	private KeyBinding restoreKey;
	//private KeyBinding[] hotbarKeys = new KeyBinding[9];

	@Override
	public void onInitializeClient()
	{
		instance = this;

		group = FabricItemGroupBuilder.create(new Identifier("librarian", "tab")).iconWithItem(() -> Item.fromBlock(Blocks.BOOKSHELF)).appendItems(stacks ->
		{
			HotbarStorage storage = (HotbarStorage) Librarian.getInstance().getCurrentPage();
			stacks.addAll(storage.getAllItems());
		}).build();

		saveKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("librarian.save", Keyboard.KEY_C, "category.librarian.actions"));
		restoreKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("librarian.restore", Keyboard.KEY_X, "category.librarian.actions"));

		ClientTickEvents.END_CLIENT_TICK.register((instance) ->
		{
			if (instance.player != null && instance.getNetworkHandler().getPlayerListEntry(instance.player.getUuid()).getGameMode() == LevelInfo.GameMode.CREATIVE)
			{
				for (int i = 0; i < 9; i++)
				{
					if (instance.options.hotbarKeys[i].isPressed())
					{
						handleKeyPress(instance.player, instance.player.inventory, i, restoreKey.isPressed(), saveKey.isPressed(), false);
					}
				}
			}
		});
	}

	public void handleKeyPress(ClientPlayerEntity player, PlayerInventory inventory, int index, boolean restore, boolean save, boolean confirmed)
	{
		HotbarStorage storage = (HotbarStorage) Librarian.getInstance().getCurrentPage();
		HotbarStorage.HotbarStorageEntry entry = storage.getEntry(index);

		if (save)
		{
			// Prevent accidental overwriting
			if (!entry.isEmpty() && !confirmed)
			{
				boolean confirm = false;

				for (int i = 0; i < PlayerInventory.getHotbarSize(); i++)
				{
					ItemStack inventoryEntry = inventory.getInvStack(i);
					ItemStack hotbarEntry = entry.get(i);

					if (hotbarEntry != null && (inventoryEntry == null || !hotbarEntry.equalsAllClient(inventoryEntry)))
					{
						confirm = true;
						break;
					}
				}

				if (confirm)
				{
					MinecraftClient.getInstance().setScreen(new ConfirmScreen((bool, ass) ->
					{
						if (bool) handleKeyPress(player, inventory, index, restore, true, true);
						MinecraftClient.getInstance().setScreen(null);
					}, I18n.translate("librarian.messages.possible_overwrite_detected.title"),
							I18n.translate("librarian.messages.possible_overwrite_detected.description"), 1337));
					return;
				}
			}

			for (int i = 0; i < PlayerInventory.getHotbarSize(); i++)
			{
				entry.set(i, inventory.getInvStack(i) != null ? inventory.getInvStack(i).copy() : null);
			}

			storage.save();
		}
		else if (restore)
		{
			for (int s = 0; s < 9; s++)
			{
				player.inventory.setInvStack(s, entry.get(s) != null ? entry.get(s).copy() : null);
			}
		}
	}
}
