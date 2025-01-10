package me.videogamesm12.librarian.v1_13_2.legacyfabric;

import lombok.NonNull;
import me.videogamesm12.librarian.api.IMechanicFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.math.BigInteger;

public class Mechanic implements IMechanicFactory
{
	private static final GsonComponentSerializer GSON_COMPONENT_SERIALIZER = GsonComponentSerializer.colorDownsamplingGson();
	private static final PlainTextComponentSerializer PLAIN_COMPONENT_SERIALIZER = PlainTextComponentSerializer.plainText();

	private int startingId = -1337;

	@Override
	public WrappedHotbarStorage createHotbarStorage(@NonNull BigInteger integer)
	{
		return new WrappedHotbarStorage(integer);
	}

	@Override
	public ButtonWidget createButton(int x, int y, int width, int height, Component label, Component tooltip, Runnable onClick)
	{
		return new ButtonWidget(startingId++, x, y, width, height, PLAIN_COMPONENT_SERIALIZER.serialize(label))
		{
			@Override
			public void method_18374(double d, double e)
			{
				onClick.run();
				super.method_18374(d, e);
			}
		};
	}

	@Override
	public Text createText(Component component)
	{
		return Text.Serializer.deserializeText(GSON_COMPONENT_SERIALIZER.serialize(component));
	}
}
