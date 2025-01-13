package me.videogamesm12.librarian.v1_8_9.legacyfabric;

import lombok.NonNull;
import me.videogamesm12.librarian.api.IMechanicFactory;
import me.videogamesm12.librarian.api.IWrappedHotbarStorage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.math.BigInteger;

public class Mechanic implements IMechanicFactory
{
	private static final GsonComponentSerializer gson = GsonComponentSerializer.colorDownsamplingGson();
	private int id = 1337;

	@Override
	public HotbarStorage createHotbarStorage(@NonNull BigInteger integer)
	{
		return new HotbarStorage(integer);
	}

	@Override
	public ButtonWidget createButton(int x, int y, int width, int height, Component label, Component tooltip, Runnable onClick)
	{
		ButtonWidget widget = new ButtonWidget(1337, x, y, width, height, createText(label).asFormattedString());
		((ILButtonWidget) widget).librarian$setOnClick(onClick);
		return widget;
	}

	@Override
	public Text createText(Component component)
	{
		return Text.Serializer.deserialize(gson.serializeToTree(component).toString());
	}
}
