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

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import org.checkerframework.checker.initialization.qual.UnderInitialization;

public final class BlockTransactionQueue implements TransactionQueue {

  private final ConcurrentLinkedQueue<WorldTransaction> queue;
  private final MineSwap plugin;
  private final ExecutorService executor;
  private final Semaphore available;
  private volatile boolean running;

  public BlockTransactionQueue(final MineSwap plugin) {
    this.queue = new ConcurrentLinkedQueue<>();
    this.available = new Semaphore(0);
    this.plugin = plugin;
    this.running = true;
    this.executor = this.createExecutor();
    this.startAsyncProcessing();
  }

  private ExecutorService createExecutor(@UnderInitialization BlockTransactionQueue this) {
    final Runtime runtime = Runtime.getRuntime();
    final int coreCount = runtime.availableProcessors();
    return Executors.newWorkStealingPool(coreCount * 2);
  }

  private void startAsyncProcessing() {
    for (int i = 0; i < Runtime.getRuntime().availableProcessors(); i++) {
      final Thread thread = new Thread(this::processLoop, "MineSwap-Processor-" + i);
      thread.setDaemon(true);
      thread.start();
    }
  }

  private void processLoop() {
    while (this.running) {
      try {
        if (!this.available.tryAcquire(50, TimeUnit.MILLISECONDS)) {
          continue;
        }

        final Map<Long, List<WorldTransaction>> chunkBatches = new HashMap<>();
        int totalCount = 0;
        WorldTransaction transaction;

        while (totalCount < Page.BLOCKS_PER_TICK && (transaction = this.queue.poll()) != null) {
          this.available.tryAcquire();
          final long chunkKey = getChunkKey(transaction);
          chunkBatches.computeIfAbsent(chunkKey, k -> new ArrayList<>(4096)).add(transaction);
          totalCount++;
        }

        final List<CompletableFuture<Void>> futures = new ArrayList<>(chunkBatches.size());
        for (final Map.Entry<Long, List<WorldTransaction>> entry : chunkBatches.entrySet()) {
          futures.add(
              CompletableFuture.runAsync(() -> this.processBatch(entry.getValue()), this.executor));
        }
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }
  }

  private static long getChunkKey(final WorldTransaction tx) {
    final int chunkX = tx.getX() >> 4;
    final int chunkZ = tx.getZ() >> 4;
    return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
  }

  private void processBatch(final List<WorldTransaction> batch) {
    if (batch.isEmpty()) {
      return;
    }
    try (final EditSession session = this.plugin.createEditSession()) {
      for (final WorldTransaction transaction : batch) {
        final int x = transaction.getX();
        final int y = transaction.getY();
        final int z = transaction.getZ();
        final BlockType material = transaction.getMaterial();
        final BlockState state = material.getDefaultState();
        try {
          session.setBlock(x, y, z, state);
        } catch (final MaxChangedBlocksException ignored) {
        }
      }
    }
  }

  public void shutdown() {
    this.running = false;
    this.executor.shutdown();
    try {
      if (!this.executor.awaitTermination(5, TimeUnit.SECONDS)) {
        this.executor.shutdownNow();
      }
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public void enqueue(final WorldTransaction transaction) {
    this.queue.add(transaction);
    this.available.release();
  }

  @Override
  public MineSwap getPlugin() {
    return this.plugin;
  }

  @Override
  public int size() {
    return this.queue.size();
  }
}
