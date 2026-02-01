/*
 * This file is part of MineSwap, a poor guy's RAM alternative
 * Copyright (C) Brandon Li <https://brandonli.me/>
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package me.brandonli.mineswap;

import com.sk89q.worldedit.world.block.BlockType;

public final class BlockWorldTransaction implements WorldTransaction {

  private final BlockType material;
  private final int x;
  private final int y;
  private final int z;
  private final int chunkIndex;

  public BlockWorldTransaction(final BlockType material, final int x, final int y, final int z, final int chunkIndex) {
    this.material = material;
    this.x = x;
    this.y = y;
    this.z = z;
    this.chunkIndex = chunkIndex;
  }

  @Override
  public int getX() {
    return this.x;
  }

  @Override
  public int getY() {
    return this.y;
  }

  @Override
  public int getZ() {
    return this.z;
  }

  @Override
  public int getChunkIndex() {
    return this.chunkIndex;
  }

  @Override
  public BlockType getMaterial() {
    return this.material;
  }
}
