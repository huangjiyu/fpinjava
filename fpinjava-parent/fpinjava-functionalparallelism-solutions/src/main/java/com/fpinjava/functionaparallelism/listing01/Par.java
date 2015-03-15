package com.fpinjava.functionaparallelism.listing01;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import com.fpinjava.common.Function;
import com.fpinjava.common.List;
import com.fpinjava.common.Supplier;
import com.fpinjava.common.Tuple;

public interface Par<A> extends Function<ExecutorService, Future<A>> {

  public static Integer sum(List<Integer> ints) {
    if (ints.length() <= 1) {
      return ints.headOption().getOrElse(0);
    } else {
      final Tuple<List<Integer>, List<Integer>> tuple = ints.splitAt(ints.length() / 2);
      final Par<Integer> sumL = Par.unit(() -> sum(tuple._1));
      final Par<Integer> sumR = Par.unit(() -> sum(tuple._2));
      return Par.get(sumL) + Par.get(sumR);
    }
  }

  /*
   * `unit` is represented as a function that returns a `UnitFuture`, which is a
   * simple implementation of `Future` that just wraps a constant value. It
   * doesn't use the `ExecutorService` at all. It's always done and can't be
   * cancelled. Its `get` method simply returns the value that we gave it.;
   */
  public static <A> Par<A> unit(Supplier<A> a) {
    return (ExecutorService es) -> new UnitFuture<>(a.get());
 }
 
  public static <A> A get(Par<A> a) {
    throw new IllegalStateException("What could be the implementation?");
 }
 
  public static class UnitFuture<A> implements Future<A> {
    
    private final A get;
    
    public UnitFuture(A get) {
      super();
      this.get = get;
    }
    
    @Override
    public boolean cancel(boolean evenIfRunning) {
      return false;
    }
    @Override
    public boolean isCancelled() {
      return false;
    }
    @Override
    public boolean isDone() {
      return true;
    }
    @Override
    public A get(long timeout, TimeUnit unit) {
      return get();
    }
    
    @Override
    public A get() {
      return this.get;
    }
  }
}
