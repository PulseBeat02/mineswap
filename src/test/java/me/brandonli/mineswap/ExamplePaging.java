package me.brandonli.mineswap;

import me.brandonli.mineswap.api.MineSwapClient;
import me.brandonli.mineswap.api.MineSwapMemorySegment;

public final class ExamplePaging {
  static void main() {
    final MineSwapClient client = new MineSwapClient("http://localhost:8080");
    try (final MineSwapMemorySegment mem =
                 new MineSwapMemorySegment(client, 64L * 1024 * 1024, 32)) {
      mem.setInt(1234567, 42);
      final int x = mem.getInt(1234567);
      System.out.println(x);
    }
  }
}
