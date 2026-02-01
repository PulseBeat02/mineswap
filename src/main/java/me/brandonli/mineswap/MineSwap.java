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

import me.brandonli.mineswap.http.HTTPMineSwapServer;
import me.brandonli.mineswap.page.BlockPagedStore;
import me.brandonli.mineswap.page.Page;
import me.brandonli.mineswap.page.accessor.MapPageAccessor;
import me.brandonli.mineswap.page.accessor.PageAccessor;
import me.brandonli.mineswap.utils.VoidChunkGenerator;
import org.bukkit.Bukkit;
import org.bukkit.GameRules;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import static java.util.Objects.requireNonNull;

public final class MineSwap extends JavaPlugin {

  private PageAccessor accessor;
  private HTTPMineSwapServer server;
  private BlockPagedStore store;
  private World swapWorld;

  @Override
  public void onEnable() {
    this.swapWorld = this.getOrCreateSwapWorld();
    this.store = new BlockPagedStore(this);
    this.server = new HTTPMineSwapServer(this, this.store);
    this.accessor = new MapPageAccessor();
    this.server.start();
  }

  @Override
  public void onDisable() {
    if (this.server != null) {
      this.server.stop();
    }
    if (this.store != null) {
      this.store.shutdown();
    }
  }

  private World getOrCreateSwapWorld() {
    final World check = Bukkit.getWorld(Page.PAGE_WORLD_NAME);
    if (check != null) {
      return check;
    }

    final WorldCreator creator = this.createGenerator();
    return this.createWorld(creator);
  }

  private WorldCreator createGenerator() {
    final WorldCreator creator = new WorldCreator(Page.PAGE_WORLD_NAME);
    final ChunkGenerator generator = new VoidChunkGenerator();
    creator.generator(generator);
    creator.generateStructures(false);
    return creator;
  }

  private World createWorld(final WorldCreator creator) {
    final World world = requireNonNull(creator.createWorld());
    world.setGameRule(GameRules.SPAWN_MOBS, false);
    world.setGameRule(GameRules.ADVANCE_TIME, false);
    world.setGameRule(GameRules.ADVANCE_WEATHER, false);
    world.setGameRule(GameRules.FIRE_DAMAGE, false);
    world.setGameRule(GameRules.RANDOM_TICK_SPEED, 0);
    world.setGameRule(GameRules.BLOCK_DROPS, false);
    world.setGameRule(GameRules.ENTITY_DROPS, false);
    world.setGameRule(GameRules.MOB_DROPS, false);
    world.setGameRule(GameRules.MOB_GRIEFING, false);
    world.setGameRule(GameRules.RAIDS, false);
    world.setGameRule(GameRules.SPAWN_PHANTOMS, false);
    world.setGameRule(GameRules.SPAWN_PATROLS, false);
    world.setGameRule(GameRules.SPAWN_WANDERING_TRADERS, false);
    world.setGameRule(GameRules.SPAWN_WARDENS, false);
    world.setAutoSave(true);
    return world;
  }

  public PageAccessor getPageAccessor() {
    return this.accessor;
  }

  public World getSwapWorld() {
    return this.swapWorld;
  }
}
