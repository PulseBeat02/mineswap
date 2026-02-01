package me.brandonli.mineswap;

import com.google.common.base.Preconditions;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class BlockPagedStore {

  private static final BlockType[] PALETTE = IOUtils.getLinesFromResource("palette.txt")
          .stream()
          .map(Material::valueOf)
          .map(BukkitAdapter::asBlockType).filter(Objects::nonNull).toArray(BlockType[]::new);
  private static final Map<BlockType, Integer> INDEX_PALETTE = Arrays.stream(PALETTE).collect(Collectors.toMap(type -> type, type -> Arrays.asList(PALETTE).indexOf(type)));

  private final MineSwap plugin;
  private final BlockTransactionQueue queue;

  public BlockPagedStore(final MineSwap plugin, final BlockTransactionQueue queue) {
    this.plugin = plugin;
    this.queue = queue;
  }

  private int pageOriginX(final long pageId) {
    final long col = Math.floorMod(pageId, Page.PAGES_PER_ROW);
    return Page.BASE_X + (int) col * (Page.PAGE_WIDTH + Page.GAP_X);
  }

  private int pageOriginZ(final long pageId) {
    final long row = Math.floorDiv(pageId, Page.PAGES_PER_ROW);
    return Page.BASE_Z + (int) row * (Page.PAGE_HEIGHT + Page.GAP_Z);
  }

  public void queueWritePage(final long pageId, final byte[] data) {
    Preconditions.checkArgument(data.length == Page.PAGE_SIZE_BYTES, "Expected " + Page.PAGE_SIZE_BYTES + " bytes, got " + data.length);
    final int ox = this.pageOriginX(pageId);
    final int oz = this.pageOriginZ(pageId);
    for (int i = 0; i < data.length; i++) {
      final int v = data[i] & 0xFF;
      this.enqueueValue(ox, oz, i, v);
    }
  }

  public byte[] readPage(final long pageId) {
    final int ox = this.pageOriginX(pageId);
    final int oz = this.pageOriginZ(pageId);
    final byte[] out = new byte[Page.PAGE_SIZE_BYTES];
    for (int i = 0; i < out.length; i++) {
      out[i] = (byte) this.readValue(ox, oz, i);
    }
    return out;
  }

  private void enqueueValue(final int ox, final int oz, final int index, final int value0to255) {
    final int x = ox + (index % Page.PAGE_WIDTH);
    final int z = oz + (index / Page.PAGE_WIDTH);
    final BlockType type = PALETTE[value0to255];
    final BlockWorldTransaction transaction = new BlockWorldTransaction(type, x, Page.BASE_Y, z);
    this.queue.enqueue(transaction);
  }

  private int readValue(final int ox, final int oz, final int index) {
    final int x = ox + (index % Page.PAGE_WIDTH);
    final int z = oz + (index / Page.PAGE_WIDTH);
    final BlockVector3 vec = BlockVector3.at(x, Page.BASE_Y, z);
    final EditSession session = this.plugin.getSession();
    final BlockState state = session.getBlock(vec);
    final BlockType type = state.getBlockType();
    return INDEX_PALETTE.getOrDefault(type, 0);
  }
}

