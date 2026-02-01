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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

public final class ParallelPageQueue {

  private final ConcurrentLinkedQueue<PageWriteTask> queue;
  private final NMSChunkAccessor accessor;
  private final BukkitTask processorTask;
  private final MineSwap plugin;

  private volatile boolean running;

  @SuppressWarnings("all")
  public ParallelPageQueue(final MineSwap plugin, final NMSChunkAccessor accessor) {
    this.queue = new ConcurrentLinkedQueue<>();
    this.plugin = plugin;
    this.accessor = accessor;
    this.running = true;
    this.processorTask = this.startProcessing();
  }

  private BukkitTask startProcessing() {
    final BukkitScheduler scheduler = Bukkit.getScheduler();
    return scheduler.runTaskTimer(this.plugin, this::processBatch, 1L, 1L);
  }

  private void processBatch() {
    if (!this.running || this.queue.isEmpty()) {
      return;
    }

    final long startTime = System.nanoTime();
    int processed = 0;
    while (processed < Page.PAGES_PER_TICK && !this.queue.isEmpty()) {
      final PageWriteTask task = this.queue.poll();
      if (task == null) {
        break;
      }

      final CompletableFuture<Void> future = task.future();
      try {
        final long id = task.pageId();
        final byte[] data = task.data();
        this.accessor.writePage(id, data);
        future.complete(null);
      } catch (final Exception e) {
        future.completeExceptionally(e);
      }

      processed++;
      if (System.nanoTime() - startTime > Page.MAX_PAGE_WRITE_NANOS) {
        break;
      }
    }
  }

  public CompletableFuture<Void> queueWrite(final long pageId, final byte[] data) {
    final CompletableFuture<Void> future = new CompletableFuture<>();
    this.queue.add(new PageWriteTask(pageId, data, future));
    return future;
  }

  public int size() {
    return this.queue.size();
  }

  public void shutdown() {
    this.running = false;

    if (this.processorTask != null) {
      this.processorTask.cancel();
    }

    if (!Bukkit.isPrimaryThread()) {
      PageWriteTask task;
      final AssertionError error = new AssertionError("Not on main thread during shutdown");
      while ((task = this.queue.poll()) != null) {
        final CompletableFuture<Void> future = task.future();
        future.completeExceptionally(error);
      }
      return;
    }

    PageWriteTask task;
    while ((task = this.queue.poll()) != null) {
      final CompletableFuture<Void> future = task.future();
      final long id = task.pageId();
      final byte[] data = task.data();
      try {
        this.accessor.writePage(id, data);
        future.complete(null);
      } catch (final Exception e) {
        future.completeExceptionally(e);
      }
    }
  }
}
