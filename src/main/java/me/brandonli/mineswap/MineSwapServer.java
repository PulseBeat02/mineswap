package me.brandonli.mineswap;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface MineSwapServer {

  String RUNNING_MESSAGE = "MineSwap is running";
  String ERROR_MISSING_ID = "Missing page ID";
  String ERROR_INVALID_ID = "Invalid page ID";
  String ERROR_INVALID_BASE64 = "Invalid base64 data";
  String ERROR_INVALID_PAGE_SIZE = "Invalid page size";

  @Nullable Javalin getApp();

  MineSwap getPlugin();

  default void error(final Context context, final String message) {
    context.status(HttpStatus.BAD_REQUEST).result(message);
  }
}
