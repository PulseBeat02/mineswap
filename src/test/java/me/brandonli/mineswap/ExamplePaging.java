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

import java.util.Random;
import me.brandonli.mineswap.api.MineSwapClient;
import me.brandonli.mineswap.api.MineSwapMemorySegment;
import me.brandonli.mineswap.page.Page;

public final class ExamplePaging {

  private static final int NUM_ELEMENTS = 5_000_000;
  private static final long MEMORY_SIZE = (long) NUM_ELEMENTS * 4 + 1024;

  static void main(final String[] args) {
    System.out.println("MineSwap Sort Demo - Sorting " + NUM_ELEMENTS + " integers in Minecraft!");
    System.out.println("Memory required: " + (MEMORY_SIZE / 1024 / 1024) + " MB");
    System.out.println("Pages required: " + (MEMORY_SIZE / Page.PAGE_SIZE_BYTES + 1));
    System.out.println();

    final MineSwapClient client = new MineSwapClient("http://localhost:8080");
    try (final MineSwapMemorySegment mem = new MineSwapMemorySegment(client, MEMORY_SIZE, 256)) {

      System.out.println("Generating random numbers...");
      final Random random = new Random(42);
      final int[] original = new int[NUM_ELEMENTS];
      for (int i = 0; i < NUM_ELEMENTS; i++) {
        original[i] = random.nextInt();
      }

      System.out.println("Writing " + NUM_ELEMENTS + " integers to Minecraft world...");
      long start = System.currentTimeMillis();
      for (int i = 0; i < NUM_ELEMENTS; i++) {
        mem.setInt((long) i * 4, original[i]);
        if (i % 1_000_000 == 0 && i > 0) {
          System.out.println("  Written: " + i + " (" + ((i * 100L) / NUM_ELEMENTS) + "%)");
        }
      }
      final long writeTime = System.currentTimeMillis() - start;
      System.out.println("Write complete in " + writeTime + "ms");
      System.out.println("Write speed: " + (NUM_ELEMENTS * 1000L / writeTime) + " integers/sec");
      System.out.println();

      System.out.println("Sorting in Minecraft memory using QuickSort...");
      start = System.currentTimeMillis();
      quickSort(mem, 0, NUM_ELEMENTS - 1);
      final long sortTime = System.currentTimeMillis() - start;
      System.out.println("Sort complete in " + sortTime + "ms");
      System.out.println();

      System.out.println("Verifying sort...");
      start = System.currentTimeMillis();
      boolean sorted = true;
      int prev = mem.getInt(0);
      for (int i = 1; i < NUM_ELEMENTS; i++) {
        final int current = mem.getInt((long) i * 4);
        if (current < prev) {
          sorted = false;
          System.out.println("ERROR: Not sorted at index " + i);
          break;
        }
        prev = current;
        if (i % 1_000_000 == 0) {
          System.out.println("  Verified: " + i + " (" + (i * 100 / NUM_ELEMENTS) + "%)");
        }
      }
      final long verifyTime = System.currentTimeMillis() - start;

      System.out.println();
      System.out.println("=== RESULTS ===");
      System.out.println("Elements sorted: " + NUM_ELEMENTS);
      System.out.println("Write time:  " + writeTime + "ms");
      System.out.println("Sort time:   " + sortTime + "ms");
      System.out.println("Verify time: " + verifyTime + "ms");
      System.out.println("Total time:  " + (writeTime + sortTime + verifyTime) + "ms");
      System.out.println("Sort verified: " + (sorted ? "PASSED" : "FAILED"));
      System.out.println();

      System.out.println("First 10 sorted values:");
      for (int i = 0; i < 10; i++) {
        System.out.println("  [" + i + "] = " + mem.getInt((long) i * 4));
      }

      System.out.println();
      System.out.println("Last 10 sorted values:");
      for (int i = NUM_ELEMENTS - 10; i < NUM_ELEMENTS; i++) {
        System.out.println("  [" + i + "] = " + mem.getInt((long) i * 4));
      }

      System.out.println();
      System.out.println("Flushing to world...");
    }

    System.out.println("Done! Your sorted numbers are now stored as blocks in Minecraft.");
  }

  private static void quickSort(final MineSwapMemorySegment mem, final int low, final int high) {
    if (low < high) {
      final int pivotIndex = partition(mem, low, high);
      quickSort(mem, low, pivotIndex - 1);
      quickSort(mem, pivotIndex + 1, high);
    }
  }

  private static int partition(final MineSwapMemorySegment mem, final int low, final int high) {
    final int pivot = mem.getInt((long) high * 4);
    int i = low - 1;

    for (int j = low; j < high; j++) {
      final int current = mem.getInt((long) j * 4);
      if (current <= pivot) {
        i++;
        swap(mem, i, j);
      }
    }

    swap(mem, i + 1, high);
    return i + 1;
  }

  private static void swap(final MineSwapMemorySegment mem, final int i, final int j) {
    if (i == j) {
      return;
    }
    final int a = mem.getInt((long) i * 4);
    final int b = mem.getInt((long) j * 4);
    mem.setInt((long) i * 4, b);
    mem.setInt((long) j * 4, a);
  }
}
