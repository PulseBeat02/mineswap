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

import java.io.Serial;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedHashMap;
import java.util.Map;
import me.brandonli.mineswap.Page;

public final class MineSwapMemorySegment implements MemorySegment {

  private final LinkedHashMap<Long, PageFrame> cache;
  private final MineSwapClient client;
  private final long sizeBytes;
  private final int maxPages;

  private class CacheMap extends LinkedHashMap<Long, PageFrame> {

    public CacheMap() {
      super(16, 0.75f, true);
    }

    @Serial
    private static final long serialVersionUID = 7224225958330701419L;

    @Override
    protected boolean removeEldestEntry(final Map.Entry<Long, PageFrame> eldest) {
      if (this.size() <= MineSwapMemorySegment.this.maxPages) {
        return false;
      }
      final long key = eldest.getKey();
      final PageFrame value = eldest.getValue();
      MineSwapMemorySegment.this.flush(key, value);
      return true;
    }
  }

  private static final class PageFrame {
    private final byte[] data;
    private boolean dirty;

    PageFrame(final byte[] data) {
      this.data = data;
    }
  }

  public MineSwapMemorySegment(
      final MineSwapClient client, final long sizeBytes, final int maxCachedPages) {
    this.client = client;
    this.sizeBytes = sizeBytes;
    this.maxPages = maxCachedPages;
    this.cache = new CacheMap();
  }

  private PageFrame page(final long pageId) {
    PageFrame frame = this.cache.get(pageId);
    if (frame != null) {
      return frame;
    }
    final byte[] data = this.client.readPage(pageId);
    frame = new PageFrame(data);
    this.cache.put(pageId, frame);
    return frame;
  }

  private void flush(final long id, final PageFrame frame) {
    if (!frame.dirty) {
      return;
    }
    try {
      this.client.writePage(id, frame.data);
      frame.dirty = false;
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void flushAll() {
    this.cache.forEach(this::flush);
  }

  @Override
  public void close() {
    this.flushAll();
  }

  private void check(final long address, final int bytes) {
    if (address < 0 || address + bytes > this.sizeBytes) {
      throw new IndexOutOfBoundsException("address=" + address);
    }
  }

  private long pageId(final long address) {
    return address / Page.PAGE_SIZE_BYTES;
  }

  private int offset(final long address) {
    return (int) (address % Page.PAGE_SIZE_BYTES);
  }

  @Override
  public byte getByte(final long addr) {
    this.check(addr, 1);
    final PageFrame frame = this.page(this.pageId(addr));
    return frame.data[this.offset(addr)];
  }

  @Override
  public void setByte(final long address, final byte value) {
    this.check(address, 1);
    final PageFrame frame = this.page(this.pageId(address));
    frame.data[this.offset(address)] = value;
    frame.dirty = true;
  }

  @Override
  public byte[] getBytes(final long address, final int length) {
    this.check(address, length);
    final byte[] result = new byte[length];
    int remaining = length;
    int destOffset = 0;
    long currentAddr = address;
    while (remaining > 0) {
      final PageFrame frame = this.page(this.pageId(currentAddr));
      final int pageOffset = this.offset(currentAddr);
      final int bytesInPage = Math.min(remaining, Page.PAGE_SIZE_BYTES - pageOffset);
      System.arraycopy(frame.data, pageOffset, result, destOffset, bytesInPage);
      remaining -= bytesInPage;
      destOffset += bytesInPage;
      currentAddr += bytesInPage;
    }
    return result;
  }

  @Override
  public void setBytes(final long address, final byte[] data) {
    this.check(address, data.length);
    int remaining = data.length;
    int srcOffset = 0;
    long currentAddr = address;
    while (remaining > 0) {
      final PageFrame frame = this.page(this.pageId(currentAddr));
      final int pageOffset = this.offset(currentAddr);
      final int bytesInPage = Math.min(remaining, Page.PAGE_SIZE_BYTES - pageOffset);
      System.arraycopy(data, srcOffset, frame.data, pageOffset, bytesInPage);
      frame.dirty = true;
      remaining -= bytesInPage;
      srcOffset += bytesInPage;
      currentAddr += bytesInPage;
    }
  }

  private ByteBuffer buf(final byte[] data) {
    return ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
  }

  private boolean crossesPageBoundary(final long address, final int bytes) {
    return this.pageId(address) != this.pageId(address + bytes - 1);
  }

  private byte[] readBytes(final long address, final int count) {
    final byte[] result = new byte[count];
    for (int i = 0; i < count; i++) {
      result[i] = this.getByte(address + i);
    }
    return result;
  }

  private void writeBytes(final long address, final byte[] bytes) {
    for (int i = 0; i < bytes.length; i++) {
      this.setByte(address + i, bytes[i]);
    }
  }

  @Override
  public int getInt(final long address) {
    this.check(address, 4);
    if (this.crossesPageBoundary(address, 4)) {
      return ByteBuffer.wrap(this.readBytes(address, 4))
          .order(ByteOrder.LITTLE_ENDIAN)
          .getInt();
    }
    final PageFrame frame = this.page(this.pageId(address));
    return this.buf(frame.data).getInt(this.offset(address));
  }

  @Override
  public void setInt(final long address, final int value) {
    this.check(address, 4);
    if (this.crossesPageBoundary(address, 4)) {
      final byte[] bytes = new byte[4];
      ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putInt(value);
      this.writeBytes(address, bytes);
      return;
    }
    final PageFrame frame = this.page(this.pageId(address));
    this.buf(frame.data).putInt(this.offset(address), value);
    frame.dirty = true;
  }

  @Override
  public char getChar(final long address) {
    this.check(address, 2);
    if (this.crossesPageBoundary(address, 2)) {
      return ByteBuffer.wrap(this.readBytes(address, 2))
          .order(ByteOrder.LITTLE_ENDIAN)
          .getChar();
    }
    final PageFrame frame = this.page(this.pageId(address));
    return this.buf(frame.data).getChar(this.offset(address));
  }

  @Override
  public void setChar(final long address, final char value) {
    this.check(address, 2);
    if (this.crossesPageBoundary(address, 2)) {
      final byte[] bytes = new byte[2];
      ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putChar(value);
      this.writeBytes(address, bytes);
      return;
    }
    final PageFrame frame = this.page(this.pageId(address));
    this.buf(frame.data).putChar(this.offset(address), value);
    frame.dirty = true;
  }

  @Override
  public long getLong(final long address) {
    this.check(address, 8);
    if (this.crossesPageBoundary(address, 8)) {
      return ByteBuffer.wrap(this.readBytes(address, 8))
          .order(ByteOrder.LITTLE_ENDIAN)
          .getLong();
    }
    final PageFrame frame = this.page(this.pageId(address));
    return this.buf(frame.data).getLong(this.offset(address));
  }

  @Override
  public void setLong(final long address, final long value) {
    this.check(address, 8);
    if (this.crossesPageBoundary(address, 8)) {
      final byte[] bytes = new byte[8];
      ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putLong(value);
      this.writeBytes(address, bytes);
      return;
    }
    final PageFrame frame = this.page(this.pageId(address));
    this.buf(frame.data).putLong(this.offset(address), value);
    frame.dirty = true;
  }
}
