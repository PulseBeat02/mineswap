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

import me.brandonli.mineswap.page.Page;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.checkerframework.checker.nullness.qual.Nullable;

import static java.util.Objects.requireNonNull;

public final class MapPageAccessor implements PageAccessor {

  private static final int MAP_SIZE = 128;
  private static final int MAP_BYTES = MAP_SIZE * MAP_SIZE;
  private static final int MAPS_PER_PAGE = (Page.PAGE_SIZE_BYTES + MAP_BYTES - 1) / MAP_BYTES;

  private final ServerLevel level;

  public MapPageAccessor() {
    final World world = requireNonNull(Bukkit.getWorld(Page.PAGE_WORLD_NAME));
    final CraftWorld craftWorld = (CraftWorld) world;
    this.level = craftWorld.getHandle();
  }

  @Override
  public void writePage(final long pageId, final byte[] data) {
    final int baseMapId = getBaseMapId(pageId);
    int dataOffset = 0;
    for (int i = 0; i < MAPS_PER_PAGE && dataOffset < data.length; i++) {
      final int mapId = baseMapId + i;
      final MapItemSavedData mapData = this.getOrCreateMapData(mapId);
      final byte[] colors = mapData.colors;
      final int bytesToWrite = Math.min(MAP_BYTES, data.length - dataOffset);
      System.arraycopy(data, dataOffset, colors, 0, bytesToWrite);
      mapData.setColorsDirty(0, 0);
      mapData.setColorsDirty(MAP_SIZE - 1, MAP_SIZE - 1);
      dataOffset += bytesToWrite;
    }
  }

  @Override
  public byte[] readPage(final long pageId) {
    final int baseMapId = getBaseMapId(pageId);
    final byte[] out = new byte[Page.PAGE_SIZE_BYTES];
    int dataOffset = 0;
    for (int i = 0; i < MAPS_PER_PAGE && dataOffset < out.length; i++) {
      final int mapId = baseMapId + i;
      final MapItemSavedData mapData = this.getMapData(mapId);
      if (mapData == null) {
        dataOffset += MAP_BYTES;
        continue;
      }

      final byte[] colors = mapData.colors;
      final int bytesToRead = Math.min(MAP_BYTES, out.length - dataOffset);
      System.arraycopy(colors, 0, out, dataOffset, bytesToRead);
      dataOffset += bytesToRead;
    }
    return out;
  }

  private MapItemSavedData getOrCreateMapData(final int mapId) {
    final MapId id = new MapId(mapId);
    MapItemSavedData mapData = this.level.getMapData(id);
    if (mapData == null) {
      final ResourceKey<Level> dimensionKey = this.level.dimension();
      mapData = MapItemSavedData.createFresh(0, 0, (byte) 0, false, false, dimensionKey);
      this.level.setMapData(id, mapData);
    }

    return mapData;
  }

  private @Nullable MapItemSavedData getMapData(final int mapId) {
    final MapId id = new MapId(mapId);
    return this.level.getMapData(id);
  }

  private static int getBaseMapId(final long pageId) {
    return (int) (pageId * MAPS_PER_PAGE);
  }
}
