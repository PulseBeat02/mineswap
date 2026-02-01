package me.brandonli.mineswap;

import me.brandonli.mineswap.api.MemorySegment;
import me.brandonli.mineswap.api.MineSwapClient;
import me.brandonli.mineswap.api.MineSwapMemorySegment;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ExamplePaging {

  static void main(final String[] args) throws IOException {
    final Path imagePath = Path.of("rgb.png");
    final byte[] originalBytes = Files.readAllBytes(imagePath);
    final MineSwapClient client = new MineSwapClient("http://localhost:8080");
    try (final MemorySegment mem = new MineSwapMemorySegment(client, 64L * 1024 * 1024, 64)) {

      mem.setInt(0, originalBytes.length);
      for (int i = 0; i < originalBytes.length; i++) {
        mem.setByte(4 + i, originalBytes[i]);
      }

      final int storedLength = mem.getInt(0);
      final byte[] readBytes = new byte[storedLength];
      for (int i = 0; i < storedLength; i++) {
        readBytes[i] = mem.getByte(4 + i);
      }

      boolean match = originalBytes.length == readBytes.length;
      if (match) {
        for (int i = 0; i < originalBytes.length; i++) {
          if (originalBytes[i] != readBytes[i]) {
            match = false;
            break;
          }
        }
      }

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
