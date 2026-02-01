package me.brandonli.mineswap;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitScheduler;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

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

    if (res.length != Page.PAGE_SIZE) {
      this.error(context, ERROR_INVALID_PAGE_SIZE);
      return;
    }

    final CompletableFuture<String> future = new CompletableFuture<>();
    final BukkitScheduler scheduler = Bukkit.getScheduler();
    scheduler.runTask(this.plugin, () -> {
      this.store.queueWritePage(id, res);
      future.complete("OK");
    });
    context.future(() -> future.thenAccept(_ -> context.status(HttpStatus.OK)));
  }

  private void handlePageGet(final Context context) {
    final OptionalInt optionalId = this.validatePageId(context);
    if (optionalId.isEmpty()) {
      return;
    }

    final int id = optionalId.getAsInt();
    final CompletableFuture<String> future = new CompletableFuture<>();
    final BukkitScheduler scheduler = Bukkit.getScheduler();
    scheduler.runTask(this.plugin, () -> {
      final byte[] result = this.store.readPage(id);
      final String base64 = IOUtils.encodeIntoBase64(result);
      future.complete(base64);
    });
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
