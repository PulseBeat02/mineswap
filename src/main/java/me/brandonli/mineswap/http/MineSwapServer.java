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
package me.brandonli.mineswap.http;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import me.brandonli.mineswap.MineSwap;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface MineSwapServer {

  String RUNNING_MESSAGE = "MineSwap is running";
  String ERROR_MISSING_ID = "Missing page ID";
  String ERROR_INVALID_ID = "Invalid page ID";
  String ERROR_INVALID_BASE64 = "Invalid base64 data";
  String ERROR_INVALID_PAGE_SIZE = "Invalid page size";

  @Nullable
  Javalin getApp();

  MineSwap getPlugin();

  default void error(final Context context, final String message) {
    context.status(HttpStatus.BAD_REQUEST).result(message);
  }
}
