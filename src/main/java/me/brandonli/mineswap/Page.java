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

public interface Page {
  int PAGE_PORT = 8080;
  int PAGE_SIZE = 4096;
  int BLOCKS_PER_TICK = 100000;
  int PAGE_SIZE_BYTES = 4096;

  int CHUNK_SIZE = 16;
  int CHUNK_BLOCKS_PER_LAYER = CHUNK_SIZE * CHUNK_SIZE;
  int LAYERS_PER_PAGE = PAGE_SIZE_BYTES / CHUNK_BLOCKS_PER_LAYER;
  int MIN_Y = -64;
  int MAX_Y = 319;
  int WORLD_HEIGHT = MAX_Y - MIN_Y + 1;
  int PAGES_PER_CHUNK = WORLD_HEIGHT / LAYERS_PER_PAGE;
  int CHUNKS_PER_ROW = 16;
}
