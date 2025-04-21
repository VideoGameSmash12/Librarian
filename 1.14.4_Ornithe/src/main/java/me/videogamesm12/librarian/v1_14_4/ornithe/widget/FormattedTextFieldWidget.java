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

package me.videogamesm12.librarian.v1_14_4.ornithe.widget;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.TextRenderer;
import net.minecraft.text.Text;

public class FormattedTextFieldWidget extends TextFieldWidget
{
	private final TextRenderer renderer;
	@Getter
	@Setter
	private Text actualMessage;

	public FormattedTextFieldWidget(TextRenderer textRenderer, int x, int y, int width, int height, String text, Text formatted)
	{
		super(textRenderer, x, y, width, height, text);
		this.actualMessage = formatted;
		this.renderer = textRenderer;
	}

	@Override
	public void renderButton(int mouseX, int mouseY, float delta)
	{
		if (isFocused())
		{
			super.renderButton(mouseX, mouseY, delta);
		}
		else
		{
			renderer.draw(actualMessage.getFormattedString(), this.x, this.y, 0x404040);
		}
	}

	@Override
	public void setFocused(boolean focused)
	{
		super.setFocused(focused);
	}
}
