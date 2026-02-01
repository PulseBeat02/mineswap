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
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitScheduler;

public final class BlockPagedStore {

  private final MineSwap plugin;
  private final NMSChunkAccessor accessor;
  private final ParallelPageQueue writeQueue;

  public BlockPagedStore(final MineSwap plugin, final NMSChunkAccessor accessor) {
    this.plugin = plugin;
    this.accessor = accessor;
    this.writeQueue = new ParallelPageQueue(plugin, accessor);
  }

  public CompletableFuture<Void> queueWritePage(final long pageId, final byte[] data) {
    Preconditions.checkArgument(
        data.length == Page.PAGE_SIZE_BYTES,
        "Expected " + Page.PAGE_SIZE_BYTES + " bytes, got " + data.length);
    return this.writeQueue.queueWrite(pageId, data);
  }

  public CompletableFuture<byte[]> readPageAsync(final long pageId) {
    if (Bukkit.isPrimaryThread()) {
      final byte[] read = this.accessor.readPage(pageId);
      return CompletableFuture.completedFuture(read);
    }

    final CompletableFuture<byte[]> future = new CompletableFuture<>();
    final BukkitScheduler scheduler = Bukkit.getScheduler();
    scheduler.runTask(this.plugin, () -> {
      try {
        final byte[] result = this.accessor.readPage(pageId);
        future.complete(result);
      } catch (final Exception e) {
        future.completeExceptionally(e);
      }
    });
    return future;
  }

  public byte[] readPage(final long pageId) {
    return this.readPageAsync(pageId).join();
  }

  public int getQueueSize() {
    return this.writeQueue.size();
  }

  public void shutdown() {
    this.writeQueue.shutdown();
  }
}
