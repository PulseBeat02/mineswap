package me.brandonli.mineswap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Base64;
import java.util.List;

import static java.util.Objects.requireNonNull;

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
