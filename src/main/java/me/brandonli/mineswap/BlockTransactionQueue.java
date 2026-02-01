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
import org.checkerframework.checker.initialization.qual.UnderInitialization;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

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
    return Executors.newFixedThreadPool(coreCount);
  }

  private void startAsyncProcessing() {
    final Thread thread = new Thread(this::processLoop, "MineSwap-Processor");
    thread.setDaemon(true);
    thread.start();
  }

  private void processLoop() {
    while (this.running) {
      try {
        if (!this.available.tryAcquire(100, TimeUnit.MILLISECONDS)) {
          continue;
        }

        final List<WorldTransaction> batch = new ArrayList<>(Page.BLOCKS_PER_TICK);
        WorldTransaction transaction;
        while (batch.size() < Page.BLOCKS_PER_TICK && (transaction = this.queue.poll()) != null) {
          this.available.tryAcquire();
          batch.add(transaction);
        }

        this.executor.submit(() -> this.processBatch(batch));
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }
  }

  private void processBatch(final List<WorldTransaction> batch) {
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
      final Thread thread = Thread.currentThread();
      thread.interrupt();
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
