package me.videogamesm12.librarian.v1_8_9.legacyfabric.mixin;

import me.videogamesm12.librarian.v1_8_9.legacyfabric.ILButtonWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ButtonWidget.class)
public class ButtonWidgetMixin implements ILButtonWidget
{
	@Unique
	private Runnable librarian$onClick = null;

	@Override
	public void librarian$onClick()
	{
		if (librarian$onClick != null)
		{
			librarian$onClick.run();
		}
	}

	@Override
	public void librarian$setOnClick(Runnable onClick)
	{
		librarian$onClick = onClick;
	}
}
