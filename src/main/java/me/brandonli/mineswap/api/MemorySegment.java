package me.brandonli.mineswap.api;

public interface MemorySegment extends AutoCloseable {
  void flushAll();

  @Override
  void close();

  byte getByte(long addr);

  void setByte(long address, byte value);

  int getInt(long address);

  void setInt(long address, int value);

  char getChar(long address);

  void setChar(long address, char value);

  long getLong(long address);

  void setLong(long address, long value);
}
