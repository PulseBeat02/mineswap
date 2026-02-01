package me.brandonli.mineswap;

public interface Page {
  int PAGE_PORT = 8080;
  int PAGE_SIZE = 4096;
  int BLOCKS_PER_TICK = 1000;
  int BASE_X = 0;
  int BASE_Y = 100;
  int BASE_Z = 0;
  int PAGES_PER_ROW = 8;
  int PAGE_SIZE_BYTES = 4096;
  int GAP_X = 2;
  int GAP_Z = 2;
  int PAGE_WIDTH = 128;
  int PAGE_HEIGHT = PAGE_SIZE_BYTES / PAGE_WIDTH;
}
