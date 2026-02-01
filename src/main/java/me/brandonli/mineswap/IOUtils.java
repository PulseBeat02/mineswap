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

import static java.util.Objects.requireNonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Base64;
import java.util.List;

public final class IOUtils {

  private IOUtils() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  public static String encodeIntoBase64(final byte[] data) {
    final Base64.Encoder encoder = Base64.getEncoder();
    return encoder.encodeToString(data);
  }

  public static byte[] decodeFromBase64(final String base64) {
    final Base64.Decoder decoder = Base64.getDecoder();
    return decoder.decode(base64);
  }

  public static List<String> getLinesFromResource(final String resourcePath) {
    try (final InputStream inputStream = getResourceAsStream(resourcePath);
        final InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        final BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
      return bufferedReader.lines().toList();
    } catch (final IOException e) {
      throw new AssertionError(e);
    }
  }

  public static InputStream getResourceAsStream(final String resourcePath) {
    final ClassLoader loader = requireNonNull(IOUtils.class.getClassLoader());
    return requireNonNull(loader.getResourceAsStream(resourcePath));
  }
}
