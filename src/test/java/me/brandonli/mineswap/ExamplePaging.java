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

import me.brandonli.mineswap.api.MineSwapClient;
import me.brandonli.mineswap.api.MineSwapMemorySegment;
import me.brandonli.mineswap.page.Page;

import javax.swing.*;
import java.awt.*;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

// this example is AI geneerated cuz im too lazy to write it myself :(
// but the main code is handwritten by me
@SuppressWarnings("all")
public final class ExamplePaging {

  private static final long ONE_GIGABYTE = 1L * 1024 * 1024 * 1024;
  private static final int NUM_ELEMENTS = (int) (ONE_GIGABYTE / 4);
  private static final long MEMORY_SIZE = ONE_GIGABYTE + 4096;
  private static final int SAMPLE_COUNT = 1024;

  private static final AtomicInteger phase = new AtomicInteger(0);
  private static final AtomicLong progress = new AtomicLong(0);
  private static final AtomicLong swapCount = new AtomicLong(0);
  private static final AtomicLong startTime = new AtomicLong(0);
  private static final AtomicLong writeTime = new AtomicLong(0);
  private static final AtomicLong sortTime = new AtomicLong(0);
  private static final AtomicLong verifyTime = new AtomicLong(0);
  private static final AtomicReference<String> status = new AtomicReference<>("Initializing...");
  private static final AtomicReference<Boolean> sortVerified = new AtomicReference<>(null);

  private static volatile int[] visualSamples = new int[SAMPLE_COUNT];
  private static volatile int highlightIndex = -1;
  private static volatile MineSwapMemorySegment globalMem;

  public static void main(final String[] args) {
    SwingUtilities.invokeLater(ExamplePaging::createAndShowGUI);
  }

  private static void createAndShowGUI() {
    final JFrame frame = new JFrame("MineSwap Sort Demo - 1 GB Integer Sort");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(1400, 800);
    frame.setLocationRelativeTo(null);

    final SortVisualizationPanel vizPanel = new SortVisualizationPanel();
    frame.add(vizPanel);
    frame.setVisible(true);

    final Timer refreshTimer = new Timer(50, e -> {
      updateVisualSamples();
      vizPanel.repaint();
    });
    refreshTimer.start();

    new Thread(() -> runSort(), "SortThread").start();
  }

  private static void runSort() {
    final MineSwapClient client = new MineSwapClient("http://localhost:8080");
    try (final MineSwapMemorySegment mem = new MineSwapMemorySegment(client, MEMORY_SIZE, 512)) {
      globalMem = mem;

      phase.set(1);
      status.set("Generating and writing random integers...");
      startTime.set(System.currentTimeMillis());

      final Random random = new Random(42);
      for (int i = 0; i < NUM_ELEMENTS; i++) {
        final int value = random.nextInt();
        mem.setInt((long) i * 4, value);
        progress.set(i + 1);
        if (i < SAMPLE_COUNT) {
          visualSamples[i] = value;
        }
      }
      writeTime.set(System.currentTimeMillis() - startTime.get());
      updateVisualSamples();

      phase.set(2);
      status.set("Sorting in Minecraft memory using QuickSort...");
      progress.set(0);
      swapCount.set(0);
      startTime.set(System.currentTimeMillis());

      quickSort(mem, 0, NUM_ELEMENTS - 1);

      sortTime.set(System.currentTimeMillis() - startTime.get());
      updateVisualSamples();

      phase.set(3);
      status.set("Verifying sort...");
      progress.set(0);
      startTime.set(System.currentTimeMillis());

      boolean sorted = true;
      int prev = mem.getInt(0);
      for (int i = 1; i < NUM_ELEMENTS; i++) {
        final int current = mem.getInt((long) i * 4);
        if (current < prev) {
          sorted = false;
          break;
        }
        prev = current;
        progress.set(i + 1);
      }

      verifyTime.set(System.currentTimeMillis() - startTime.get());
      sortVerified.set(sorted);
      updateVisualSamples();

      phase.set(4);
      status.set(
              "Complete! " + (sorted ? "Sort verified successfully." : "Sort verification FAILED!"));
      globalMem = null;
    }
  }

  private static void updateVisualSamples() {
    final MineSwapMemorySegment mem = globalMem;
    if (mem == null) {
      return;
    }
    try {
      final int step = NUM_ELEMENTS / SAMPLE_COUNT;
      for (int i = 0; i < SAMPLE_COUNT; i++) {
        final int idx = i * step;
        if (idx < NUM_ELEMENTS) {
          visualSamples[i] = mem.getInt((long) idx * 4);
        }
      }
    } catch (final Exception ignored) {
    }
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

    final long count = swapCount.incrementAndGet();
    if (count % 100_000 == 0) {
      progress.set(count);
      highlightIndex = (i * SAMPLE_COUNT) / NUM_ELEMENTS;
    }
  }

  private static class SortVisualizationPanel extends JPanel {

    private static final Color BG_COLOR = new Color(24, 24, 32);
    private static final Color BAR_COLOR = new Color(100, 200, 255);
    private static final Color HIGHLIGHT_COLOR = new Color(255, 100, 100);
    private static final Color TEXT_COLOR = new Color(220, 220, 240);
    private static final Color ACCENT_COLOR = new Color(150, 255, 150);
    private static final Color PROGRESS_BG = new Color(50, 50, 60);
    private static final Color PROGRESS_FG = new Color(100, 180, 255);

    SortVisualizationPanel() {
      setBackground(BG_COLOR);
      setDoubleBuffered(true);
    }

    @Override
    protected void paintComponent(final Graphics g) {
      super.paintComponent(g);
      final Graphics2D g2 = (Graphics2D) g;
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(
              RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

      final int width = getWidth();
      final int height = getHeight();

      drawHeader(g2, width);
      drawStats(g2, width);
      drawProgressBar(g2, width);
      drawVisualization(g2, width, height);
    }

    private void drawHeader(final Graphics2D g2, final int width) {
      g2.setFont(new Font("SansSerif", Font.BOLD, 28));
      g2.setColor(TEXT_COLOR);
      final String title = "MineSwap Sort Demo - Sorting 1 GB of Integers in Minecraft";
      final FontMetrics fm = g2.getFontMetrics();
      final int titleWidth = fm.stringWidth(title);
      g2.drawString(title, (width - titleWidth) / 2, 40);

      g2.setFont(new Font("SansSerif", Font.PLAIN, 16));
      g2.setColor(new Color(150, 150, 170));
      final String subtitle = String.format(
              "%,d integers | %,d pages | Memory: 1 GB",
              NUM_ELEMENTS, MEMORY_SIZE / Page.PAGE_SIZE_BYTES + 1);
      final int subWidth = g2.getFontMetrics().stringWidth(subtitle);
      g2.drawString(subtitle, (width - subWidth) / 2, 65);
    }

    private void drawStats(final Graphics2D g2, final int width) {
      g2.setFont(new Font("Monospaced", Font.PLAIN, 14));

      final int currentPhase = phase.get();
      final long prog = progress.get();
      final long swaps = swapCount.get();

      final int leftX = 40;
      final int rightX = width / 2 + 20;
      int y = 100;

      g2.setColor(ACCENT_COLOR);
      g2.drawString("Status: " + status.get(), leftX, y);
      y += 25;

      g2.setColor(TEXT_COLOR);
      final String phaseStr =
              switch (currentPhase) {
                case 0 -> "Initializing";
                case 1 -> "Phase 1: Writing Data";
                case 2 -> "Phase 2: Sorting (QuickSort)";
                case 3 -> "Phase 3: Verification";
                case 4 -> "Complete";
                default -> "Unknown";
              };
      g2.drawString("Current Phase: " + phaseStr, leftX, y);
      y += 25;

      if (currentPhase == 1) {
        final double pct = (prog * 100.0) / NUM_ELEMENTS;
        g2.drawString(
                String.format("Write Progress: %,d / %,d (%.2f%%)", prog, NUM_ELEMENTS, pct), leftX, y);
      } else if (currentPhase == 2) {
        g2.drawString(String.format("Swap Operations: %,d", swaps), leftX, y);
      } else if (currentPhase == 3) {
        final double pct = (prog * 100.0) / NUM_ELEMENTS;
        g2.drawString(
                String.format("Verify Progress: %,d / %,d (%.2f%%)", prog, NUM_ELEMENTS, pct),
                leftX,
                y);
      }
      y = 100;

      g2.setColor(new Color(180, 180, 200));
      final long wt = writeTime.get();
      final long st = sortTime.get();
      final long vt = verifyTime.get();

      g2.drawString(String.format("Write Time:  %,d ms", wt), rightX, y);
      y += 25;
      g2.drawString(String.format("Sort Time:   %,d ms", st), rightX, y);
      y += 25;
      g2.drawString(String.format("Verify Time: %,d ms", vt), rightX, y);
      y += 25;

      if (currentPhase >= 4) {
        final long total = wt + st + vt;
        g2.setColor(ACCENT_COLOR);
        g2.drawString(String.format("Total Time:  %,d ms", total), rightX, y);
        y += 25;

        final Boolean verified = sortVerified.get();
        if (verified != null) {
          g2.setColor(verified ? new Color(100, 255, 100) : new Color(255, 100, 100));
          g2.drawString("Verification: " + (verified ? "PASSED" : "FAILED"), rightX, y);
        }
      }
    }

    private void drawProgressBar(final Graphics2D g2, final int width) {
      final int barX = 40;
      final int barY = 175;
      final int barWidth = width - 80;
      final int barHeight = 24;

      g2.setColor(PROGRESS_BG);
      g2.fillRoundRect(barX, barY, barWidth, barHeight, 10, 10);

      final int currentPhase = phase.get();
      double pct = 0;
      if (currentPhase == 1 || currentPhase == 3) {
        pct = (progress.get() * 1.0) / NUM_ELEMENTS;
      } else if (currentPhase == 2) {
        pct = Math.min(1.0, swapCount.get() / (NUM_ELEMENTS * 2.0));
      } else if (currentPhase >= 4) {
        pct = 1.0;
      }

      final int filledWidth = (int) (barWidth * pct);
      if (filledWidth > 0) {
        final GradientPaint gradient = new GradientPaint(
                barX, barY, PROGRESS_FG, barX + barWidth, barY, new Color(150, 100, 255));
        g2.setPaint(gradient);
        g2.fillRoundRect(barX, barY, filledWidth, barHeight, 10, 10);
      }

      g2.setColor(TEXT_COLOR);
      g2.setFont(new Font("SansSerif", Font.BOLD, 12));
      final String pctStr = String.format("%.1f%%", pct * 100);
      final FontMetrics fm = g2.getFontMetrics();
      g2.drawString(pctStr, barX + (barWidth - fm.stringWidth(pctStr)) / 2, barY + 17);
    }

    private void drawVisualization(final Graphics2D g2, final int width, final int height) {
      final int vizX = 40;
      final int vizY = 220;
      final int vizWidth = width - 80;
      final int vizHeight = height - vizY - 40;

      g2.setColor(new Color(35, 35, 45));
      g2.fillRoundRect(vizX, vizY, vizWidth, vizHeight, 10, 10);

      g2.setColor(new Color(60, 60, 70));
      g2.drawRoundRect(vizX, vizY, vizWidth, vizHeight, 10, 10);

      final int[] samples = visualSamples;
      if (samples == null || samples.length == 0) {
        return;
      }

      long minVal = Long.MAX_VALUE;
      long maxVal = Long.MIN_VALUE;
      for (final int sample : samples) {
        if (sample < minVal) minVal = sample;
        if (sample > maxVal) maxVal = sample;
      }

      if (maxVal == minVal) {
        maxVal = minVal + 1;
      }

      final int barPadding = 2;
      final int totalBarWidth = vizWidth - 20;
      final float barWidth = (float) totalBarWidth / SAMPLE_COUNT;
      final int maxBarHeight = vizHeight - 30;

      final int hl = highlightIndex;

      for (int i = 0; i < SAMPLE_COUNT; i++) {
        final long value = samples[i];
        final double normalized = (double) (value - minVal) / (maxVal - minVal);
        final int barHeight = (int) (normalized * maxBarHeight);

        final int x = vizX + 10 + (int) (i * barWidth);
        final int y = vizY + vizHeight - 15 - barHeight;

        if (i == hl) {
          g2.setColor(HIGHLIGHT_COLOR);
        } else {
          final float hue = (float) normalized * 0.6f;
          g2.setColor(Color.getHSBColor(hue, 0.7f, 0.9f));
        }

        final int drawWidth = Math.max(1, (int) barWidth - barPadding);
        g2.fillRect(x, y, drawWidth, barHeight);
      }

      g2.setColor(new Color(100, 100, 120));
      g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
      g2.drawString("Array visualization (1024 evenly-spaced samples)", vizX + 10, vizY + 15);
    }
  }
}
