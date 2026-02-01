package me.brandonli.mineswap;

import com.sk89q.worldedit.world.block.BlockType;
import org.bukkit.Material;
import org.bukkit.World;

public final class BlockWorldTransaction implements WorldTransaction {

  private final BlockType material;
  private final int x;
  private final int y;
  private final int z;

  public BlockWorldTransaction(final BlockType material, final int x, final int y, final int z) {
    this.material = material;
    this.x = x;
    this.y = y;
    this.z = z;
  }

  @Override
  public int getX() {
    return this.x;
  }

  @Override
  public int getY() {
    return this.y;
  }

  @Override
  public int getZ() {
    return this.z;
  }

  @Override
  public BlockType getMaterial() {
    return this.material;
  }
}