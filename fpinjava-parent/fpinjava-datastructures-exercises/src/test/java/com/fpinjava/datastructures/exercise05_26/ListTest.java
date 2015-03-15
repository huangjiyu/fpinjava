package com.fpinjava.datastructures.exercise05_26;

import static org.junit.Assert.*;

import org.junit.Test;

import com.fpinjava.datastructures.exercise05_26.List;

public class ListTest {

  @Test
  public void testZipWith() {
    assertEquals("[2, 6, 12, NIL]", List.zipWith(List.list(1, 2, 3), List.list(2, 3, 4), x -> y -> x * y).toString());
    assertEquals("[2, 6, 12, NIL]", List.zipWith(List.list(1, 2, 3, 4), List.list(2, 3, 4), x -> y -> x * y).toString());
    assertEquals("[2, 6, 12, NIL]", List.zipWith(List.list(1, 2, 3), List.list(2, 3, 4, 5), x -> y -> x * y).toString());
  }

}
