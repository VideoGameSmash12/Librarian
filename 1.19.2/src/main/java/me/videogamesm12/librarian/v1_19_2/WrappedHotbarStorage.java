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

package me.videogamesm12.librarian.v1_19_2;

import me.videogamesm12.librarian.api.IWrappedHotbarStorage;
import me.videogamesm12.librarian.util.FNF;
import me.videogamesm12.librarian.v1_19_2.mixin.HotbarStorageAccessor;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.HotbarStorage;

import java.io.File;
import java.math.BigInteger;

public class WrappedHotbarStorage extends HotbarStorage implements IWrappedHotbarStorage
{
	private final BigInteger page;
	private final File location;

	public WrappedHotbarStorage(BigInteger page)
	{
		super(new File(page.equals(BigInteger.ZERO) ? FabricLoader.getInstance().getGameDir().toFile() :
				FNF.getHotbarFolder(), FNF.getPageFileName(page)), MinecraftClient.getInstance().getDataFixer());

		this.page = page;
		this.location = new File(page.equals(BigInteger.ZERO) ? FabricLoader.getInstance().getGameDir().toFile() :
				FNF.getHotbarFolder(), FNF.getPageFileName(page));
	}

	@Override
	public File getLocation()
	{
		return location;
	}

	@Override
	public BigInteger getPageNumber()
	{
		return page;
	}

	@Override
	public void load()
	{
		((HotbarStorageAccessor) this).invokeLoad();
		setLoaded(true);
	}
}
