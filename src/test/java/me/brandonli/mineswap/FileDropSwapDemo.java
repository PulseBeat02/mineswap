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

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;
import me.brandonli.mineswap.api.MineSwapClient;
import me.brandonli.mineswap.api.MineSwapMemorySegment;

@SuppressWarnings("all")
public final class FileDropSwapDemo {

  private static final AtomicReference<String> status =
      new AtomicReference<>("Waiting for file...");
  private static final AtomicReference<String> fileName = new AtomicReference<>("");
  private static final AtomicLong fileSize = new AtomicLong(0);
  private static final AtomicLong bytesLoaded = new AtomicLong(0);
  private static final AtomicLong loadStartTime = new AtomicLong(0);
  private static final AtomicLong loadEndTime = new AtomicLong(0);
  private static final AtomicReference<State> state = new AtomicReference<>(State.IDLE);

  private static volatile MineSwapMemorySegment globalMem;
  private static volatile boolean isDragOver = false;

  private enum State {
    IDLE,
    LOADING,
    LOADED,
    ERROR
  }

  public static void main(final String[] args) {
    SwingUtilities.invokeLater(FileDropSwapDemo::createAndShowGUI);
  }

  private static void createAndShowGUI() {
    final JFrame frame = new JFrame("MineSwap Loader");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(800, 500);
    frame.setLocationRelativeTo(null);

    final DropPanel dropPanel = new DropPanel();
    frame.add(dropPanel);

    new DropTarget(
        dropPanel,
        DnDConstants.ACTION_COPY,
        new DropTargetListener() {
          @Override
          public void dragEnter(final DropTargetDragEvent dtde) {
            if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
              dtde.acceptDrag(DnDConstants.ACTION_COPY);
              isDragOver = true;
              dropPanel.repaint();
            } else {
              dtde.rejectDrag();
            }
          }

          @Override
          public void dragOver(final DropTargetDragEvent dtde) {}

          @Override
          public void dropActionChanged(final DropTargetDragEvent dtde) {}

          @Override
          public void dragExit(final DropTargetEvent dte) {
            isDragOver = false;
            dropPanel.repaint();
          }

          @Override
          public void drop(final DropTargetDropEvent dtde) {
            isDragOver = false;
            dropPanel.repaint();

            try {
              if (!dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                dtde.rejectDrop();
                return;
              }

              dtde.acceptDrop(DnDConstants.ACTION_COPY);
              final Transferable transferable = dtde.getTransferable();
              final List<File> files =
                  (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);

              if (files.isEmpty()) {
                dtde.dropComplete(false);
                return;
              }

              final File file = files.get(0);
              if (!file.isFile()) {
                status.set("Please drop a file, not a directory");
                state.set(State.ERROR);
                dtde.dropComplete(false);
                return;
              }

              dtde.dropComplete(true);
              new Thread(() -> loadFileToSwap(file), "FileLoaderThread").start();
            } catch (final Exception e) {
              status.set("Error: " + e.getMessage());
              state.set(State.ERROR);
              dtde.dropComplete(false);
            }
          }
        },
        true);

    final Timer refreshTimer = new Timer(50, e -> dropPanel.repaint());
    refreshTimer.start();

    frame.setVisible(true);
  }

  private static void loadFileToSwap(final File file) {
    state.set(State.LOADING);
    fileName.set(file.getName());
    fileSize.set(file.length());
    bytesLoaded.set(0);
    loadStartTime.set(System.currentTimeMillis());
    loadEndTime.set(0);
    status.set("Reading file...");

    final byte[] fileData;
    try {
      fileData = Files.readAllBytes(file.toPath());
    } catch (final IOException e) {
      status.set("Failed to read file: " + e.getMessage());
      state.set(State.ERROR);
      return;
    }

    status.set("Connecting to server...");
    final MineSwapClient client = new MineSwapClient("http://localhost:8080");

    final long memorySize = fileData.length + 4096L;
    status.set("Allocating swap memory...");

    try (final MineSwapMemorySegment mem = new MineSwapMemorySegment(client, memorySize, 256)) {
      globalMem = mem;

      status.set("Loading file into Minecraft swap...");

      final int chunkSize = 4096;
      for (int offset = 0; offset < fileData.length; offset += chunkSize) {
        final int length = Math.min(chunkSize, fileData.length - offset);
        final byte[] chunk = new byte[length];
        System.arraycopy(fileData, offset, chunk, 0, length);
        mem.setBytes(offset, chunk);
        bytesLoaded.set(offset + length);
      }

      mem.flushAll();
      loadEndTime.set(System.currentTimeMillis());

      final long duration = loadEndTime.get() - loadStartTime.get();
      status.set(String.format("File loaded successfully in %,d ms!", duration));
      state.set(State.LOADED);

      Thread.sleep(5000);
      globalMem = null;
    } catch (final Exception e) {
      status.set("Error: " + e.getMessage());
      state.set(State.ERROR);
    }

    state.set(State.IDLE);
    fileName.set("");
    fileSize.set(0);
    bytesLoaded.set(0);
    status.set("Waiting for file...");
  }

  private static class DropPanel extends JPanel {

    private static final Color BG_COLOR = new Color(30, 30, 40);
    private static final Color DROP_ZONE_COLOR = new Color(50, 50, 70);
    private static final Color DROP_ZONE_HOVER = new Color(70, 90, 120);
    private static final Color DROP_ZONE_BORDER = new Color(100, 140, 200);
    private static final Color TEXT_COLOR = new Color(220, 220, 240);
    private static final Color ACCENT_COLOR = new Color(100, 200, 255);
    private static final Color SUCCESS_COLOR = new Color(100, 255, 150);
    private static final Color ERROR_COLOR = new Color(255, 100, 100);
    private static final Color PROGRESS_BG = new Color(50, 50, 60);
    private static final Color PROGRESS_FG = new Color(100, 180, 255);

    DropPanel() {
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
      drawDropZone(g2, width, height);
      drawStatus(g2, width, height);
    }

    private void drawHeader(final Graphics2D g2, final int width) {
      g2.setFont(new Font("SansSerif", Font.BOLD, 24));
      g2.setColor(TEXT_COLOR);
      final String title = "MineSwap File Loader";
      final FontMetrics fm = g2.getFontMetrics();
      final int titleWidth = fm.stringWidth(title);
      g2.drawString(title, (width - titleWidth) / 2, 40);

      g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
      g2.setColor(new Color(150, 150, 170));
      final String subtitle = "Drag and drop a file into Minecraft swap memory";
      final int subWidth = g2.getFontMetrics().stringWidth(subtitle);
      g2.drawString(subtitle, (width - subWidth) / 2, 65);
    }

    private void drawDropZone(final Graphics2D g2, final int width, final int height) {
      final int zoneX = 60;
      final int zoneY = 90;
      final int zoneWidth = width - 120;
      final int zoneHeight = height - 200;

      g2.setColor(isDragOver ? DROP_ZONE_HOVER : DROP_ZONE_COLOR);
      g2.fillRoundRect(zoneX, zoneY, zoneWidth, zoneHeight, 20, 20);

      final Stroke oldStroke = g2.getStroke();
      g2.setStroke(new BasicStroke(
          2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[] {10, 10}, 0));
      g2.setColor(isDragOver ? ACCENT_COLOR : DROP_ZONE_BORDER);
      g2.drawRoundRect(zoneX + 10, zoneY + 10, zoneWidth - 20, zoneHeight - 20, 15, 15);
      g2.setStroke(oldStroke);

      final State currentState = state.get();

      if (currentState == State.IDLE) {
        drawIdleState(g2, zoneX, zoneY, zoneWidth, zoneHeight);
      } else if (currentState == State.LOADING) {
        drawLoadingState(g2, zoneX, zoneY, zoneWidth, zoneHeight);
      } else if (currentState == State.LOADED) {
        drawLoadedState(g2, zoneX, zoneY, zoneWidth, zoneHeight);
      } else if (currentState == State.ERROR) {
        drawErrorState(g2, zoneX, zoneY, zoneWidth, zoneHeight);
      }
    }

    private void drawIdleState(
        final Graphics2D g2, final int x, final int y, final int w, final int h) {
      final int iconSize = 60;
      final int iconX = x + (w - iconSize) / 2;
      final int iconY = y + h / 2 - 50;

      g2.setColor(isDragOver ? ACCENT_COLOR : new Color(120, 120, 140));
      g2.setStroke(new BasicStroke(3));
      g2.drawRoundRect(iconX, iconY, iconSize, iconSize - 10, 8, 8);
      g2.drawLine(iconX + iconSize / 2, iconY - 15, iconX + iconSize / 2, iconY + 15);
      final int arrowY = iconY - 15;
      g2.drawLine(iconX + iconSize / 2 - 10, arrowY + 10, iconX + iconSize / 2, arrowY);
      g2.drawLine(iconX + iconSize / 2 + 10, arrowY + 10, iconX + iconSize / 2, arrowY);

      g2.setFont(new Font("SansSerif", Font.BOLD, 18));
      g2.setColor(isDragOver ? ACCENT_COLOR : TEXT_COLOR);
      final String dropText = isDragOver ? "Release to load file" : "Drop file here";
      final FontMetrics fm = g2.getFontMetrics();
      g2.drawString(dropText, x + (w - fm.stringWidth(dropText)) / 2, iconY + iconSize + 30);
    }

    private void drawLoadingState(
        final Graphics2D g2, final int x, final int y, final int w, final int h) {
      final String name = fileName.get();
      final long size = fileSize.get();
      final long loaded = bytesLoaded.get();

      g2.setFont(new Font("SansSerif", Font.BOLD, 16));
      g2.setColor(ACCENT_COLOR);
      final String fileText = String.format("Loading: %s", name);
      final FontMetrics fm = g2.getFontMetrics();
      g2.drawString(fileText, x + (w - fm.stringWidth(fileText)) / 2, y + h / 2 - 40);

      g2.setFont(new Font("Monospaced", Font.PLAIN, 14));
      g2.setColor(TEXT_COLOR);
      final String sizeText = String.format("%,d / %,d bytes", loaded, size);
      g2.drawString(
          sizeText, x + (w - g2.getFontMetrics().stringWidth(sizeText)) / 2, y + h / 2 - 15);

      final int barX = x + 60;
      final int barY = y + h / 2 + 10;
      final int barWidth = w - 120;
      final int barHeight = 24;

      g2.setColor(PROGRESS_BG);
      g2.fillRoundRect(barX, barY, barWidth, barHeight, 10, 10);

      final double pct = size > 0 ? (loaded * 1.0) / size : 0;
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
      g2.drawString(
          pctStr, barX + (barWidth - g2.getFontMetrics().stringWidth(pctStr)) / 2, barY + 17);
    }

    private void drawLoadedState(
        final Graphics2D g2, final int x, final int y, final int w, final int h) {
      g2.setColor(SUCCESS_COLOR);
      g2.setFont(new Font("SansSerif", Font.BOLD, 48));
      final String check = "\u2713";
      final FontMetrics fm = g2.getFontMetrics();
      g2.drawString(check, x + (w - fm.stringWidth(check)) / 2, y + h / 2 - 10);

      g2.setFont(new Font("SansSerif", Font.BOLD, 16));
      final String successText = "File loaded into Minecraft!";
      g2.drawString(
          successText, x + (w - g2.getFontMetrics().stringWidth(successText)) / 2, y + h / 2 + 30);

      final long duration = loadEndTime.get() - loadStartTime.get();
      final long size = fileSize.get();
      g2.setFont(new Font("Monospaced", Font.PLAIN, 14));
      g2.setColor(TEXT_COLOR);
      final String statsText = String.format("%,d bytes in %,d ms", size, duration);
      g2.drawString(
          statsText, x + (w - g2.getFontMetrics().stringWidth(statsText)) / 2, y + h / 2 + 55);
    }

    private void drawErrorState(
        final Graphics2D g2, final int x, final int y, final int w, final int h) {
      g2.setColor(ERROR_COLOR);
      g2.setFont(new Font("SansSerif", Font.BOLD, 48));
      final String xMark = "\u2717";
      final FontMetrics fm = g2.getFontMetrics();
      g2.drawString(xMark, x + (w - fm.stringWidth(xMark)) / 2, y + h / 2 - 10);

      g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
      final String errorText = status.get();
      g2.drawString(
          errorText, x + (w - g2.getFontMetrics().stringWidth(errorText)) / 2, y + h / 2 + 30);
    }

    private void drawStatus(final Graphics2D g2, final int width, final int height) {
      final int statusY = height - 60;

      g2.setColor(new Color(45, 45, 55));
      g2.fillRoundRect(40, statusY, width - 80, 45, 10, 10);

      g2.setFont(new Font("Monospaced", Font.PLAIN, 13));
      final State currentState = state.get();
      if (currentState == State.LOADED) {
        g2.setColor(SUCCESS_COLOR);
      } else if (currentState == State.ERROR) {
        g2.setColor(ERROR_COLOR);
      } else {
        g2.setColor(TEXT_COLOR);
      }

      final String statusText = "Status: " + status.get();
      g2.drawString(statusText, 55, statusY + 28);
    }
  }
}
