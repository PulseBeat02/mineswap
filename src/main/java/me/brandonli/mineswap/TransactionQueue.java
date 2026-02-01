package me.brandonli.mineswap;

public interface TransactionQueue {

  int size();

  void enqueue(final WorldTransaction transaction);

  MineSwap getPlugin();
}
