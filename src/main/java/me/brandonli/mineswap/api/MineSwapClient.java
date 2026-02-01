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
package me.brandonli.mineswap.api;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import me.brandonli.mineswap.IOUtils;
import me.brandonli.mineswap.Page;

public final class MineSwapClient {

  private static final String REQUEST_FORMAT = "%s/page?id=%d";

  private final HttpClient client;
  private final String url;

  public MineSwapClient(final String url) {
    this.url = url;
    this.client = HttpClient.newHttpClient();
  }

  public byte[] readPage(final long id) {
    Preconditions.checkArgument(id >= 0, "Page ID cannot be negative");

    final String request = REQUEST_FORMAT.formatted(this.url, id);
    final URI uri = URI.create(request);
    final HttpRequest req =
        HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10)).GET().build();
    final HttpResponse.BodyHandler<String> handler = HttpResponse.BodyHandlers.ofString();
    final HttpResponse<String> res;
    try {
      res = this.client.send(req, handler);
    } catch (final IOException | InterruptedException e) {
      throw new AssertionError(e);
    }

    final int code = res.statusCode();
    if (code != 200) {
      return new byte[Page.PAGE_SIZE_BYTES];
    }

    final String base64 = res.body().trim();
    final byte[] data = IOUtils.decodeFromBase64(base64);
    Preconditions.checkState(data.length == Page.PAGE_SIZE_BYTES, "Decoded page size is incorrect");

    return data;
  }

  public void writePage(final long id, final byte[] data) {
    Preconditions.checkArgument(id >= 0, "Page ID cannot be negative");
    Preconditions.checkNotNull(data, "Page data cannot be null");
    Preconditions.checkArgument(
        data.length == Page.PAGE_SIZE_BYTES,
        "Page data must be exactly " + Page.PAGE_SIZE_BYTES + " bytes");

    final String base64 = IOUtils.encodeIntoBase64(data);
    final String request = REQUEST_FORMAT.formatted(this.url, id);
    final URI uri = URI.create(request);
    final HttpRequest.BodyPublisher publisher = HttpRequest.BodyPublishers.ofString(base64);
    final HttpRequest req = HttpRequest.newBuilder(uri)
        .timeout(Duration.ofSeconds(10))
        .PUT(publisher)
        .build();
    final HttpResponse.BodyHandler<String> handler = HttpResponse.BodyHandlers.ofString();
    final HttpResponse<String> res;
    try {
      res = this.client.send(req, handler);
    } catch (final IOException | InterruptedException e) {
      throw new AssertionError(e);
    }

    final int code = res.statusCode();
    Preconditions.checkState(
        code == 200 || code == 202, "WritePage failed: HTTP " + code + " body=" + res.body());
  }
}
