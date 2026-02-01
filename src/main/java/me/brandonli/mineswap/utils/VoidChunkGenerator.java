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
package me.brandonli.mineswap.utils;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jspecify.annotations.NonNull;

public final class VoidChunkGenerator extends ChunkGenerator {

  @Override
  public void generateSurface(
      final @NonNull WorldInfo worldInfo,
      final @NonNull Random random,
      final int chunkX,
      final int chunkZ,
      final @NonNull ChunkData chunkData) {
    // empty
  }

  @Override
  public void generateBedrock(
      final @NonNull WorldInfo worldInfo,
      final @NonNull Random random,
      final int chunkX,
      final int chunkZ,
      final @NonNull ChunkData chunkData) {
    // empty
  }

  @Override
  public void generateCaves(
      final @NonNull WorldInfo worldInfo,
      final @NonNull Random random,
      final int chunkX,
      final int chunkZ,
      final @NonNull ChunkData chunkData) {
    // empty
  }

  @Override
  public @Nullable BiomeProvider getDefaultBiomeProvider(final @NonNull WorldInfo worldInfo) {
    return null;
  }

  @Override
  public @NonNull List<BlockPopulator> getDefaultPopulators(final @NonNull World world) {
    return Collections.emptyList();
  }

  @Override
  public @NonNull Location getFixedSpawnLocation(
      final @NonNull World world, final @NonNull Random random) {
    return new Location(world, 0, 64, 0);
  }
}
