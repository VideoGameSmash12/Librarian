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
