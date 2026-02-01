package me.brandonli.mineswap;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import org.bukkit.Server;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.ArrayDeque;
import java.util.Queue;

public final class BlockTransactionQueue implements TransactionQueue {

  private final MineSwap plugin;
  private final Queue<WorldTransaction> queue;

  public BlockTransactionQueue(final MineSwap plugin) {
    this.queue = new ArrayDeque<>();
    this.plugin = plugin;
    this.scheduleQueueProcessing(plugin);
  }

  private void scheduleQueueProcessing(final MineSwap plugin) {
    final Server server = plugin.getServer();
    final BukkitScheduler scheduler = server.getScheduler();
    scheduler.runTaskTimer(plugin, this::runBlockProcessing, 1L, 1L);
  }

  private void runBlockProcessing() {
    int n = 0;
    while (n < Page.BLOCKS_PER_TICK && !this.queue.isEmpty()) {
      this.setBlock();
      n++;
    }
  }

  private void setBlock() {
    final WorldTransaction transaction = this.queue.poll();
    if (transaction == null) {
      return;
    }
    final int x = transaction.getX();
    final int y = transaction.getY();
    final int z = transaction.getZ();
    final BlockVector3 vec = BlockVector3.at(x, y, z);
    final BlockType material = transaction.getMaterial();
    final BlockState state = material.getDefaultState();
    final EditSession session = this.plugin.getSession();
    try {
      session.setBlock(vec, state);
    } catch (final MaxChangedBlocksException e) {
      throw new AssertionError(e);
    }
  }

  @Override
  public void enqueue(final WorldTransaction transaction) {
    this.queue.add(transaction);
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