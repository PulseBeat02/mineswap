package me.brandonli.mineswap;

import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockType;
import org.bukkit.Material;

public interface WorldTransaction {

  int getX();

  int getY();

  int getZ();

  BlockType getMaterial();
}
