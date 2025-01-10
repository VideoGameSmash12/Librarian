package me.videogamesm12.librarian.v1_18_2;

import lombok.NonNull;
import me.videogamesm12.librarian.api.IMechanicFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.client.MinecraftClient;
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
		final Text tooltipText = createText(tooltip);

		return new ButtonWidget(x, y, width, height, createText(label), (e) -> onClick.run(), (widget, stack, mx, my) -> {
			if (MinecraftClient.getInstance().currentScreen != null)
				MinecraftClient.getInstance().currentScreen.renderTooltip(stack, tooltipText, mx, my);
		});
	}

	@Override
	public Text createText(Component component)
	{
		return Text.Serializer.fromJson(GSON_COMPONENT_SERIALIZER.serializeToTree(component));
	}
}
