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

public interface MemorySegment extends AutoCloseable {
  void flushAll();

  @Override
  void close();

  byte getByte(long addr);

  void setByte(long address, byte value);

  byte[] getBytes(long address, int length);

  void setBytes(long address, byte[] data);

  int getInt(long address);

  void setInt(long address, int value);

  char getChar(long address);

  void setChar(long address, char value);

  long getLong(long address);

  void setLong(long address, long value);
}
