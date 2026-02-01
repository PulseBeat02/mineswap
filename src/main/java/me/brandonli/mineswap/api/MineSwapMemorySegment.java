package me.brandonli.mineswap.api;

import me.brandonli.mineswap.Page;

import java.io.Serial;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MineSwapMemorySegment implements AutoCloseable {

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

  public MineSwapMemorySegment(final MineSwapClient client, final long sizeBytes, final int maxCachedPages) {
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

  public byte getByte(final long addr) {
    this.check(addr, 1);
    final PageFrame frame = this.page(this.pageId(addr));
    return frame.data[this.offset(addr)];
  }

  public void setByte(final long address, final byte value) {
    this.check(address, 1);
    final PageFrame frame = this.page(this.pageId(address));
    frame.data[this.offset(address)] = value;
    frame.dirty = true;
  }

  private ByteBuffer buf(final byte[] data) {
    return ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
  }

  public int getInt(final long address) {
    this.check(address, 4);
    final PageFrame frame = this.page(this.pageId(address));
    return this.buf(frame.data).getInt(this.offset(address));
  }

  public void setInt(final long address, final int value) {
    this.check(address, 4);
    final PageFrame frame = this.page(this.pageId(address));
    this.buf(frame.data).putInt(this.offset(address), value);
    frame.dirty = true;
  }

  public char getChar(final long address) {
    this.check(address, 2);
    final PageFrame frame = this.page(this.pageId(address));
    return this.buf(frame.data).getChar(this.offset(address));
  }

  public void setChar(final long address, final char value) {
    this.check(address, 2);
    final PageFrame frame = this.page(this.pageId(address));
    this.buf(frame.data).putChar(this.offset(address), value);
    frame.dirty = true;
  }

  public long getLong(final long address) {
    this.check(address, 8);
    final PageFrame frame = this.page(this.pageId(address));
    return this.buf(frame.data).getLong(this.offset(address));
  }

  public void setLong(final long address, final long value) {
    this.check(address, 8);
    final PageFrame frame = this.page(this.pageId(address));
    this.buf(frame.data).putLong(this.offset(address), value);
    frame.dirty = true;
  }
}
