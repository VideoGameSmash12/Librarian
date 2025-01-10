package me.videogamesm12.librarian.v1_14_4;

import lombok.NonNull;
import me.videogamesm12.librarian.api.IMechanicFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.math.BigInteger;

public class Mechanic implements IMechanicFactory
{
	private static final GsonComponentSerializer GSON_COMPONENT_SERIALIZER = GsonComponentSerializer.gson();

	@Override
	public WrappedHotbarStorage createHotbarStorage(@NonNull BigInteger integer)
	{
		return new WrappedHotbarStorage(integer);
	}

	@Override
	public ButtonWidget createButton(int x, int y, int width, int height, Component label, Component tooltip, Runnable onClick)
	{
		return new ButtonWidget(x, y, width, height, createText(label).asFormattedString(), (e) -> onClick.run());
	}

	@Override
	public Text createText(Component component)
	{
		return Text.Serializer.fromJson(GSON_COMPONENT_SERIALIZER.serializeToTree(component));
	}
}
