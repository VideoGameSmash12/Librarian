/*
 * Copyright (C) 2026 Video
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

package me.videogamesm12.librarian.v1_12_2.ornithe;

import me.videogamesm12.librarian.v1_12_2.ornithe.mixin.ConfirmScreenAccessor;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.ConfirmationListener;
import net.minecraft.client.gui.widget.OptionButtonWidget;
import net.minecraft.util.math.MathHelper;

/**
 * <h1>FixedConfirmScreen</h1>
 * <p>Partial backport of the modern confirm screen to workaround bugs caused by long text.</p>
 */
public class FixedConfirmScreen extends ConfirmScreen
{
	private final ConfirmScreenAccessor shortcut;

	public FixedConfirmScreen(ConfirmationListener parent, String title, String description, int id)
	{
		super(parent, title, description, id);
		shortcut = (ConfirmScreenAccessor) this;
	}

	@Override
	public void init()
	{
		shortcut.getLines().clear();
		shortcut.getLines().addAll(this.textRenderer.split(shortcut.getDescription(), this.width - 50));

		int buttonY = MathHelper.clamp(this.getMessageY() + this.getMessagesHeight() + 20, this.height / 6 + 96, this.height - 24);

		this.buttons.add(new OptionButtonWidget(0, this.width / 2 - 155, buttonY, this.confirmText));
		this.buttons.add(new OptionButtonWidget(1, this.width / 2 - 155 + 160, buttonY, this.abortText));
	}

	@Override
	public void render(int mouseX, int mouseY, float tickDelta)
	{
		this.renderBackground();
		this.drawCenteredString(this.textRenderer, this.title, this.width / 2, getTitleY(), 16777215);
		int i = getMessageY();

		for (String string : shortcut.getLines())
		{
			this.drawCenteredString(this.textRenderer, string, this.width / 2, i, 16777215);
			i += this.textRenderer.fontHeight;
		}

		buttons.forEach(button -> button.render(this.minecraft, mouseX, mouseY, tickDelta));
		labels.forEach(labelWidget -> labelWidget.render(this.minecraft, mouseX, mouseY));
	}

	private int getTitleY()
	{
		int centered = (this.height - this.getMessagesHeight()) / 2;
		int offset = centered - 20;
		return MathHelper.clamp(offset - 9, 10, 80);
	}

	private int getMessageY()
	{
		return this.getTitleY() + 20;
	}

	private int getMessagesHeight()
	{
		return shortcut.getLines().size() * 9;
	}
}
