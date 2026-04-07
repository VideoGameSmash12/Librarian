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

package me.videogamesm12.librarian.v1_16_5;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.font.MultilineText;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

/**
 * <h1>FixedConfirmScreen</h1>
 * <p>ariant of ConfirmScreen that fixes a weird alignment issue present with larger menus</p>
 */
public class FixedConfirmScreen extends ConfirmScreen
{
	private final Text message;
	private MultilineText messageSplit = null;
	private final BooleanConsumer callback;

	public FixedConfirmScreen(Text title, Text message, BooleanConsumer callback)
	{
		super(callback, title, message);
		this.message = message;
		this.callback = callback;
	}

	@Override
	public void init()
	{
		children.clear();
		buttons.clear();

		this.messageSplit = MultilineText.create(this.textRenderer, message, this.width - 50);

		int buttonY = MathHelper.clamp(this.getMessageY() + this.getMessagesHeight() + 20, this.height / 6 + 96, this.height - 24);
		addButton(new ButtonWidget(this.width / 2 - 155, buttonY, 150, 20, this.yesTranslated, (button) -> callback.accept(true)));
		addButton(new ButtonWidget(this.width / 2 - 155 + 160, buttonY, 150, 20, this.noTranslated, (button) -> callback.accept(false)));
	}

	@Override
	public void render(MatrixStack matrices, int mouseX, int mouseY, float tickDelta)
	{
		this.renderBackground(matrices);

		drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, getTitleY(), 16777215);
		messageSplit.drawCenterWithShadow(matrices, this.width / 2, getMessageY());

		buttons.forEach(drawable -> drawable.render(matrices, mouseX, mouseY, tickDelta));
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
		return messageSplit.count() * 9;
	}
}
