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

package me.videogamesm12.librarian.v1_13_2.legacyfabric;

import me.videogamesm12.librarian.v1_13_2.legacyfabric.mixin.ConfirmScreenAccessor;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.IdentifiableBooleanConsumer;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * <h1>FixedConfirmScreen</h1>
 * <p>ariant of ConfirmScreen that fixes a weird alignment issue present with larger menus</p>
 */
public class FixedConfirmScreen extends ConfirmScreen
{
	private final String message;
	private final List<String> messageSplit = new ArrayList<>();
	private final IdentifiableBooleanConsumer callback;
	private final ConfirmScreenAccessor shortcut;

	public FixedConfirmScreen(String title, String message, int id, IdentifiableBooleanConsumer callback)
	{
		super(callback, title, message, id);
		this.message = message;
		this.callback = callback;
		this.shortcut = (ConfirmScreenAccessor) this;
	}

	@Override
	public void init()
	{
		field_20307.clear();
		buttons.clear();

		messageSplit.clear();
		messageSplit.addAll(this.textRenderer.wrapLines(message, this.width - 50));

		int buttonY = MathHelper.clamp(getButtonY(), this.height / 6 + 96, this.height - 24);
		this.addButton(new ButtonWidget(0, this.width / 2 - 155, buttonY, 150, 20, this.yesText)
		{
			@Override
			public void method_18374(double mouseX, double mouseY)
			{
				shortcut.consumer().confirmResult(true, this.id);
			}
		});
		this.addButton(new ButtonWidget(1, this.width / 2 - 155 + 160, buttonY, 150, 20, this.noText)
		{
			@Override
			public void method_18374(double mouseX, double mouseY)
			{
				shortcut.consumer().confirmResult(false, this.id);
			}
		});
	}

	@Override
	public void render(int mouseX, int mouseY, float tickDelta)
	{
		this.renderBackground();
		this.drawCenteredString(this.textRenderer, this.title, this.width / 2, getTitleY(), 16777215);
		int i = getMessageY();

		for (String string : messageSplit)
		{
			this.drawCenteredString(this.textRenderer, string, this.width / 2, i, 16777215);
			i += this.textRenderer.fontHeight;
		}

		buttons.forEach(b -> b.method_891(mouseX, mouseY, tickDelta));
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
		return messageSplit.size() * 9;
	}

	private int getButtonY()
	{
		return getMessageY() + (this.textRenderer.fontHeight * messageSplit.size()) + 20;
	}
}
