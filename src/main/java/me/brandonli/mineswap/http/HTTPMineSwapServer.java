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

import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import me.brandonli.mineswap.MineSwap;
import me.brandonli.mineswap.page.BlockPagedStore;
import me.brandonli.mineswap.page.Page;
import me.brandonli.mineswap.utils.IOUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class HTTPMineSwapServer implements MineSwapServer {

  private final MineSwap plugin;
  private final BlockPagedStore store;

  private @Nullable Javalin app;

  public HTTPMineSwapServer(final MineSwap plugin, final BlockPagedStore store) {
    this.plugin = plugin;
    this.store = store;
  }

  public void start() {
    Preconditions.checkArgument(this.app == null, "Server is already running");
    final Javalin app = Javalin.create().start(Page.PAGE_PORT);
    app.get("/", this::handleDefaultMessage);
    app.get("/page", this::handlePageGet);
    app.put("/page", this::handlePagePut);
    this.app = app;
  }

  public void stop() {
    Preconditions.checkNotNull(this.app, "Server is already stopped");
    final Javalin app = requireNonNull(this.app);
    app.stop();
    this.app = null;
  }

  private void handleDefaultMessage(final Context context) {
    context.result(RUNNING_MESSAGE);
  }

  private void handlePagePut(final Context context) {
    final OptionalInt optionalId = this.validatePageId(context);
    if (optionalId.isEmpty()) {
      return;
    }

    final int id = optionalId.getAsInt();
    final String body = context.body();
    final byte[] res;
    try {
      res = IOUtils.decodeFromBase64(body);
    } catch (final IllegalArgumentException e) {
      this.error(context, ERROR_INVALID_BASE64);
      return;
    }

    if (res.length != Page.PAGE_SIZE_BYTES) {
      this.error(context, ERROR_INVALID_PAGE_SIZE);
      return;
    }

    final CompletableFuture<Void> future = this.store.queueWritePage(id, res);
    context.future(() -> future.thenRun(() -> context.status(HttpStatus.OK)));
  }

  private void handlePageGet(final Context context) {
    final OptionalInt optionalId = this.validatePageId(context);
    if (optionalId.isEmpty()) {
      return;
    }

    final int id = optionalId.getAsInt();
    final CompletableFuture<String> future =
        this.store.readPageAsync(id).thenApply(IOUtils::encodeIntoBase64);
    context.future(() -> future.thenAccept(context::result));
  }

  private OptionalInt validatePageId(final Context context) {
    final String pageId = context.queryParam("id");
    if (pageId == null) {
      this.error(context, ERROR_MISSING_ID);
      return OptionalInt.empty();
    }

    final Integer id = Ints.tryParse(pageId);
    if (id == null) {
      this.error(context, ERROR_INVALID_ID);
      return OptionalInt.empty();
    }

    return OptionalInt.of(id);
  }

  @Override
  public MineSwap getPlugin() {
    return this.plugin;
  }

  @Override
  public @Nullable Javalin getApp() {
    return this.app;
  }
}
