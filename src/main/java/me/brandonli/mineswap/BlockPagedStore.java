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

import com.google.common.base.Preconditions;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.bukkit.Material;

public final class BlockPagedStore {

  private static final BlockType[] PALETTE = IOUtils.getLinesFromResource("palette.txt").stream()
      .map(Material::valueOf)
      .map(BukkitAdapter::asBlockType)
      .filter(Objects::nonNull)
      .toArray(BlockType[]::new);
  private static final Map<BlockType, Integer> INDEX_PALETTE = Arrays.stream(PALETTE)
      .collect(Collectors.toMap(type -> type, type -> Arrays.asList(PALETTE).indexOf(type)));

  private final MineSwap plugin;
  private final BlockTransactionQueue queue;

  public BlockPagedStore(final MineSwap plugin, final BlockTransactionQueue queue) {
    this.plugin = plugin;
    this.queue = queue;
  }

  private int getChunkIndex(final long pageId) {
    return (int) (pageId / Page.PAGES_PER_CHUNK);
  }

  private int getPageInChunk(final long pageId) {
    return (int) (pageId % Page.PAGES_PER_CHUNK);
  }

  private int getChunkX(final int chunkIndex) {
    return (chunkIndex % Page.CHUNKS_PER_ROW) * Page.CHUNK_SIZE;
  }

  private int getChunkZ(final int chunkIndex) {
    return (chunkIndex / Page.CHUNKS_PER_ROW) * Page.CHUNK_SIZE;
  }

  private int getBaseY(final int pageInChunk) {
    return Page.MIN_Y + (pageInChunk * Page.LAYERS_PER_PAGE);
  }

  public void queueWritePage(final long pageId, final byte[] data) {
    Preconditions.checkArgument(
        data.length == Page.PAGE_SIZE_BYTES,
        "Expected " + Page.PAGE_SIZE_BYTES + " bytes, got " + data.length);
    final int chunkIndex = this.getChunkIndex(pageId);
    final int pageInChunk = this.getPageInChunk(pageId);
    final int chunkX = this.getChunkX(chunkIndex);
    final int chunkZ = this.getChunkZ(chunkIndex);
    final int baseY = this.getBaseY(pageInChunk);
    for (int i = 0; i < data.length; i++) {
      final int v = data[i] & 0xFF;
      final int localX = i % Page.CHUNK_SIZE;
      final int localZ = (i / Page.CHUNK_SIZE) % Page.CHUNK_SIZE;
      final int localY = i / Page.CHUNK_BLOCKS_PER_LAYER;
      final int x = chunkX + localX;
      final int y = baseY + localY;
      final int z = chunkZ + localZ;
      final BlockType type = PALETTE[v];
      final BlockWorldTransaction transaction =
          new BlockWorldTransaction(type, x, y, z, chunkIndex);
      this.queue.enqueue(transaction);
    }
  }

  public byte[] readPage(final long pageId) {
    final int chunkIndex = this.getChunkIndex(pageId);
    final int pageInChunk = this.getPageInChunk(pageId);
    final int chunkX = this.getChunkX(chunkIndex);
    final int chunkZ = this.getChunkZ(chunkIndex);
    final int baseY = this.getBaseY(pageInChunk);
    final byte[] out = new byte[Page.PAGE_SIZE_BYTES];
    try (final EditSession session = this.plugin.createEditSession()) {
      for (int i = 0; i < out.length; i++) {
        final int localX = i % Page.CHUNK_SIZE;
        final int localZ = (i / Page.CHUNK_SIZE) % Page.CHUNK_SIZE;
        final int localY = i / Page.CHUNK_BLOCKS_PER_LAYER;
        final int x = chunkX + localX;
        final int y = baseY + localY;
        final int z = chunkZ + localZ;
        final BlockVector3 vec = BlockVector3.at(x, y, z);
        final BlockState state = session.getBlock(vec);
        final BlockType type = state.getBlockType();
        out[i] = (byte) INDEX_PALETTE.getOrDefault(type, 0).intValue();
      }
    }
    return out;
  }
}
