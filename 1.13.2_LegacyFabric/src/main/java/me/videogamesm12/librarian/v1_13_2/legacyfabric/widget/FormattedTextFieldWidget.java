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

package me.videogamesm12.librarian.v1_13_2.legacyfabric.widget;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class FormattedTextFieldWidget extends TextFieldWidget
{
	private final TextRenderer renderer;
	@Getter
	@Setter
	private Text actualMessage;

	public FormattedTextFieldWidget(int id, TextRenderer textRenderer, int x, int y, int width, int height, String text, Text formatted)
	{
		super(id, textRenderer, x, y, width, height);
		this.setText(text);
		this.actualMessage = formatted;
		this.renderer = textRenderer;
	}

	@Override
	public void method_18385(int mouseX, int mouseY, float tickDelta)
	{
		if (isFocused())
		{
			super.method_18385(mouseX, mouseY, tickDelta);
		}
		else
		{
			renderer.method_18355(actualMessage.asFormattedString(), this.x, this.y, 0x404040);
		}
	}

	@Override
	public void setFocused(boolean focused)
	{
		super.setFocused(focused);
	}
}
