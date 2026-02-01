package me.brandonli.mineswap;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class MineSwap extends JavaPlugin {

  private HTTPMineSwapServer server;
  private EditSession session;

  private EditSession getEditSession() {
    final List<World> worlds = Bukkit.getWorlds();
    final org.bukkit.World world = worlds.getFirst();
    final com.sk89q.worldedit.world.World wrapped = BukkitAdapter.adapt(world);
    final WorldEdit worldEdit = WorldEdit.getInstance();
    return worldEdit.newEditSession(wrapped);
  }

  @Override
  public void onEnable() {
    this.session = this.getEditSession();
    final BlockTransactionQueue queue = new BlockTransactionQueue(this);
    final BlockPagedStore store = new BlockPagedStore(this, queue);
    this.server = new HTTPMineSwapServer(this, store);
    this.server.start();
  }

  @Override
  public void onDisable() {
    if (this.session != null) {
      this.session.close();
    }
    if (this.server != null) {
      this.server.stop();
    }
  }

  public EditSession getSession() {
    return this.session;
  }
}
