package io.github.suunxh.housingteleporthelper.core;

public interface PlateSearch {
    void advance(int positionBudget, long nanosecondBudget);
    boolean isDone();
    Destination result();
}
