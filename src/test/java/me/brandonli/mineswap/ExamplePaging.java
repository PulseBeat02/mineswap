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

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import javax.imageio.ImageIO;
import javax.swing.*;
import me.brandonli.mineswap.api.MemorySegment;
import me.brandonli.mineswap.api.MineSwapClient;
import me.brandonli.mineswap.api.MineSwapMemorySegment;

public final class ExamplePaging {

  static void main(final String[] args) throws IOException {
    final Path imagePath = Path.of("rgb.png");
    final byte[] originalBytes = Files.readAllBytes(imagePath);
    final MineSwapClient client = new MineSwapClient("http://localhost:8080");
    try (final MemorySegment mem = new MineSwapMemorySegment(client, 64L * 1024 * 1024, 128)) {

      final long writeStart = System.currentTimeMillis();
      mem.setInt(0, originalBytes.length);
      mem.setBytes(4, originalBytes);
      final long writeEnd = System.currentTimeMillis();
      System.out.println("Write: " + (writeEnd - writeStart) + "ms");

      final long readStart = System.currentTimeMillis();
      final int storedLength = mem.getInt(0);
      final byte[] readBytes = mem.getBytes(4, storedLength);
      final long readEnd = System.currentTimeMillis();
      System.out.println("Read: " + (readEnd - readStart) + "ms");

      final boolean match = Arrays.equals(originalBytes, readBytes);
      System.out.println("Equal: " + (match ? "PASSED" : "FAILED"));

      final ByteArrayInputStream bais = new ByteArrayInputStream(readBytes);
      final BufferedImage image = ImageIO.read(bais);
      if (image == null) {
        return;
      }

      final JFrame frame = new JFrame("rgb.png");
      frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
      frame.add(new JLabel(new ImageIcon(image)));
      frame.pack();
      frame.setLocationRelativeTo(null);
      frame.setVisible(true);
    }
  }
}
