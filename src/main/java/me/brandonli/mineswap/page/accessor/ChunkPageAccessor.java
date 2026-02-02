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
package me.brandonli.mineswap.page.accessor;

import static java.util.Objects.requireNonNull;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import me.brandonli.mineswap.page.Page;
import me.brandonli.mineswap.utils.IOUtils;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;

public final class ChunkPageAccessor implements PageAccessor {

  private static final int CHUNK_SIZE = 16;
  private static final int CHUNK_BLOCKS_PER_LAYER = CHUNK_SIZE * CHUNK_SIZE;
  private static final int LAYERS_PER_PAGE = Page.PAGE_SIZE_BYTES / CHUNK_BLOCKS_PER_LAYER;
  private static final int MIN_Y = -64;
  private static final int PAGES_PER_CHUNK = 1;
  private static final int CHUNKS_PER_ROW = 128;

  private static final BlockState[] INDEX_TO_PALETTE;
  private static final Map<BlockState, Integer> PALETTE_TO_INDEX;

  static {
    INDEX_TO_PALETTE = IOUtils.getLinesFromResource("palette.txt").stream()
        .map(Material::valueOf)
        .map(CraftMagicNumbers::getBlock)
        .filter(Objects::nonNull)
        .map(Block::defaultBlockState)
        .toArray(BlockState[]::new);
    PALETTE_TO_INDEX = new IdentityHashMap<>(INDEX_TO_PALETTE.length);
    for (int i = 0; i < INDEX_TO_PALETTE.length; i++) {
      PALETTE_TO_INDEX.put(INDEX_TO_PALETTE[i], i);
    }
  }

  private final ServerLevel level;

  public ChunkPageAccessor() {
    final World world = requireNonNull(Bukkit.getWorld(Page.PAGE_WORLD_NAME));
    final CraftWorld craftWorld = (CraftWorld) world;
    this.level = craftWorld.getHandle();
  }

  @Override
  public void writePage(final long pageId, final byte[] data) {
    final int chunkIndex = getChunkIndex(pageId);
    final int pageInChunk = getPageInChunk(pageId);
    final int chunkX = getChunkX(chunkIndex);
    final int chunkZ = getChunkZ(chunkIndex);
    final int baseY = getBaseY(pageInChunk);
    final int nmsChunkX = chunkX >> 4;
    final int nmsChunkZ = chunkZ >> 4;
    final LevelChunk chunk = this.level.getChunk(nmsChunkX, nmsChunkZ);
    final LevelChunkSection[] sections = chunk.getSections();
    for (int i = 0; i < data.length; i++) {
      final int v = data[i] & 0xFF;
      final int localX = i % CHUNK_SIZE;
      final int localZ = (i / CHUNK_SIZE) % CHUNK_SIZE;
      final int layerInPage = i / CHUNK_BLOCKS_PER_LAYER;
      final int y = baseY + layerInPage;
      final int sectionIndex = this.level.getSectionIndex(y);
      if (sectionIndex < 0 || sectionIndex >= sections.length) {
        continue;
      }

      final LevelChunkSection section = sections[sectionIndex];
      final BlockState state = INDEX_TO_PALETTE[v];
      final int sectionY = y & 15;
      section.setBlockState(localX, sectionY, localZ, state, false);
    }
    chunk.markUnsaved();
    this.sendChunkUpdate(chunk);
  }

  @Override
  public byte[] readPage(final long pageId) {
    final int chunkIndex = getChunkIndex(pageId);
    final int pageInChunk = getPageInChunk(pageId);
    final int chunkX = getChunkX(chunkIndex);
    final int chunkZ = getChunkZ(chunkIndex);
    final int baseY = getBaseY(pageInChunk);
    final int nmsChunkX = chunkX >> 4;
    final int nmsChunkZ = chunkZ >> 4;
    final LevelChunk chunk = this.level.getChunk(nmsChunkX, nmsChunkZ);
    final LevelChunkSection[] sections = chunk.getSections();
    final byte[] out = new byte[Page.PAGE_SIZE_BYTES];
    for (int i = 0; i < out.length; i++) {
      final int localX = i % CHUNK_SIZE;
      final int localZ = (i / CHUNK_SIZE) % CHUNK_SIZE;
      final int layerInPage = i / CHUNK_BLOCKS_PER_LAYER;
      final int y = baseY + layerInPage;
      final int sectionIndex = this.level.getSectionIndex(y);
      if (sectionIndex < 0 || sectionIndex >= sections.length) {
        out[i] = 0;
        continue;
      }

      final LevelChunkSection section = sections[sectionIndex];
      final int sectionY = y & 15;
      final BlockState state = section.getBlockState(localX, sectionY, localZ);
      out[i] = (byte) PALETTE_TO_INDEX.getOrDefault(state, 0).intValue();
    }
    return out;
  }

  private void sendChunkUpdate(final LevelChunk chunk) {

    final List<ServerPlayer> players = this.level.players();
    if (players.isEmpty()) {
      return;
    }

    final LevelLightEngine lightEngine = this.level.getLightEngine();
    final ClientboundLevelChunkWithLightPacket packet =
        new ClientboundLevelChunkWithLightPacket(chunk, lightEngine, null, null, true);
    players.forEach(player -> player.connection.send(packet));
  }

  private static int getChunkIndex(final long pageId) {
    return (int) (pageId / PAGES_PER_CHUNK);
  }

  private static int getPageInChunk(final long pageId) {
    return (int) (pageId % PAGES_PER_CHUNK);
  }

  private static int getChunkX(final int chunkIndex) {
    return (chunkIndex % CHUNKS_PER_ROW) * CHUNK_SIZE;
  }

  private static int getChunkZ(final int chunkIndex) {
    return (chunkIndex / CHUNKS_PER_ROW) * CHUNK_SIZE;
  }

  private static int getBaseY(final int pageInChunk) {
    return MIN_Y + (pageInChunk * LAYERS_PER_PAGE);
  }
}
