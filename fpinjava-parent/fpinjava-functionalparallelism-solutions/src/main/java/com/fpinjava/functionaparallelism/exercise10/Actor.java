package com.fpinjava.functionaparallelism.exercise10;


import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import com.fpinjava.common.Function;

/*
* Implementation is taken from `scalaz` library, with only minor changes. See:
*
* https://github.com/scalaz/scalaz/blob/scalaz-seven/concurrent/src/main/scala/scalaz/concurrent/Actor.scala
*
* This code is copyright Andriy Plokhotnyuk, Runar Bjarnason, and other contributors,
* and is licensed using 3-clause BSD, see LICENSE file at:
*
* https://github.com/scalaz/scalaz/blob/scalaz-seven/etc/LICENCE
*/
/**
* Processes messages of type `A`, one at a time. Messages are submitted to
* the actor with the method `!`. Processing is typically performed asynchronously,
* this is controlled by the provided `strategy`.
*
* Memory consistency guarantee: when each message is processed by the `handler`, any memory that it
* mutates is guaranteed to be visible by the `handler` when it processes the next message, even if
* the `strategy` runs the invocations of `handler` on separate threads. This is achieved because
* the `Actor` reads a volatile memory location before entering its event loop, and writes to the same
* location before suspending.
*
* Implementation based on non-intrusive MPSC node-based queue, described by Dmitriy Vyukov:
* [[http://www.1024cores.net/home/lock-free-algorithms/queues/non-intrusive-mpsc-node-based-queue]]
*
* @see scalaz.concurrent.Promise for a use case.
*
* @param handler The message handler
* @param onError Exception handler, called if the message handler throws any `Throwable`.
* @param strategy Execution strategy, for example, a strategy that is backed by an `ExecutorService`
* @tparam A The type of messages accepted by this actor.
*/
public class Actor<A> {
  
  private AtomicReference<Node<A>> tail = new AtomicReference<>(new Node<A>());
  private AtomicInteger suspended = new AtomicInteger(1);
  private AtomicReference<Node<A>> head = new AtomicReference<>(tail.get());
  private final Strategy strategy;
  private final Consumer<A> handler;
  private final Consumer<Throwable> onError;

  public Actor(Strategy strategy, Consumer<A> handler, Consumer<Throwable> onError) {
    this.strategy = strategy;
    this.handler = handler;
    this.onError = onError;
  }
  
  /** Alias for `apply` */
  public void tell(A a) {
    Node<A> n = new Node<A>(a);
    head.getAndSet(n).lazySet(n);
    trySchedule();
  }
  
  /** Pass the message `a` to the mailbox of this actor */
  public void apply(A a) {
    this.tell(a);
  }
  
  public <B> Actor<B> contramap(Function<B, A> f) {
    Consumer<B> cb = (B b) -> this.tell(f.apply(b));
    return new Actor<B>(strategy, cb, onError);
  }
  
  private void trySchedule() {
    if (suspended.compareAndSet(1, 0)) schedule();
  }
  
  private void schedule() {
    act();
    strategy.apply(() -> null);
  }
  
  private void act() {
    Node<A> t = tail.get();
    Node<A> n = batchHandle(t, 1024);
    if (n != t) {
      n.a = null;
      tail.lazySet(n);
      schedule();
    } else {
      suspended.set(1);
      if (n.get() != null) trySchedule();
    }
  }
  
  //@tailrec
  private Node<A> batchHandle(Node<A> t, int i) {
    Node<A> n = t.get();
    if (n != null) {
      try {
        handler.accept(n.getA());
      } catch (Throwable ex) {
        onError.accept(ex);
      }
      if (i > 0) {
        return batchHandle(n, i - 1);
      } else {
        return n;
      }
    } else {
      return t;
    }
  }

  @SuppressWarnings("serial")
  static class Node<A> extends AtomicReference<Node<A>> {
    
    private A a;

    public Node() {
      super();
    }

    public Node(A a) {
      super();
      this.a = a;
    }

    public A getA() {
      return a;
    }

    public void setA(A a) {
      this.a = a;
    }
    
  }
      
  /*
   *  Create an `Actor` backed by the given `ExecutorService`. 
   */
  public static <A> Actor<A> apply(ExecutorService es, Consumer<A> handler, Consumer<Throwable> onError) {
    return new Actor<>(Strategy.fromExecutorService(es), handler, onError);
  }
}