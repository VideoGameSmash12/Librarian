package me.videogamesm12.librarian.v1_12_2.ornithe.mixin;

import me.videogamesm12.librarian.v1_12_2.ornithe.ILButtonWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ButtonWidget.class)
public class ButtonWidgetMixin implements ILButtonWidget
{
	@Unique
	private Runnable onClick = null;

	@Override
	public void librarian$onClick()
	{
		if (onClick != null)
		{
			onClick.run();
		}
	}

	@Override
	public void librarian$setOnClick(Runnable onClick)
	{
		this.onClick = onClick;
	}
}
