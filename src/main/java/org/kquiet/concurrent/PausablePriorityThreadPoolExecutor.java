/*
 * Copyright 2019 P. Kimberly Chang
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kquiet.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * {@link PausableThreadPoolExecutor} which execute tasks by their priority. This class is for
 * internal use.
 *
 * @author Kimberly
 */
public class PausablePriorityThreadPoolExecutor extends PausableThreadPoolExecutor {

  /** Create a {@link PausablePriorityThreadPoolExecutor} with core/maximum pool size set to one. */
  public PausablePriorityThreadPoolExecutor() {
    this("", 1, 1);
  }

  /**
   * Create a {@link PausablePriorityThreadPoolExecutor} with specified parameters and max pool size
   * to five.
   *
   * @param poolPrefix prefix name of thread pool
   * @param corePoolSize core pool size
   * @param maximumPoolSize maximum pool size
   */
  public PausablePriorityThreadPoolExecutor(
      String poolPrefix, int corePoolSize, int maximumPoolSize) {
    this(poolPrefix, corePoolSize, maximumPoolSize, 5);
  }

  /**
   * Create a {@link PausablePriorityThreadPoolExecutor} with specified parameters.
   *
   * @param poolPrefix prefix name of thread pool
   * @param corePoolSize core pool size
   * @param maximumPoolSize maximum pool size
   * @param queueSize the maximum queue size used for holding tasks before they are executed
   */
  public PausablePriorityThreadPoolExecutor(
      String poolPrefix, int corePoolSize, int maximumPoolSize, int queueSize) {
    this(poolPrefix, corePoolSize, maximumPoolSize, queueSize, null);
  }

  /**
   * Create a {@link PausablePriorityThreadPoolExecutor} with specified parameters.
   *
   * @param poolPrefix prefix name of thread pool
   * @param corePoolSize core pool size
   * @param maximumPoolSize maximum pool size
   * @param afterExecuteFunc the function to execute after any task is executed
   */
  public PausablePriorityThreadPoolExecutor(
      String poolPrefix,
      int corePoolSize,
      int maximumPoolSize,
      Consumer<Runnable> afterExecuteFunc) {
    this(poolPrefix, corePoolSize, maximumPoolSize, 5, afterExecuteFunc);
  }

  /**
   * Create a {@link PausablePriorityThreadPoolExecutor} with specified parameters.
   *
   * @param poolPrefix prefix name of thread pool
   * @param corePoolSize core pool size
   * @param maximumPoolSize maximum pool size
   * @param queueSize the maximum queue size used for holding tasks before they are executed
   * @param afterExecuteFunc the function to execute after any task is executed
   */
  public PausablePriorityThreadPoolExecutor(
      String poolPrefix,
      int corePoolSize,
      int maximumPoolSize,
      int queueSize,
      Consumer<Runnable> afterExecuteFunc) {
    this(
        poolPrefix,
        corePoolSize,
        maximumPoolSize,
        queueSize,
        10L,
        TimeUnit.MINUTES,
        afterExecuteFunc);
  }

  /**
   * Create a {@link PausablePriorityThreadPoolExecutor} with specified parameters.
   *
   * @param poolPrefix prefix name of thread pool
   * @param corePoolSize core pool size
   * @param maximumPoolSize maximum pool size
   * @param queueSize the maximum queue size used for holding tasks before they are executed
   * @param keepAliveTime when the number of threads is greater than the core, this is the maximum
   *     time that excess idle threads will wait for new tasks before terminating.
   * @param unit the time unit for the {@code keepAliveTime} argument
   * @param afterExecuteFunc the function to execute after any task is executed
   */
  public PausablePriorityThreadPoolExecutor(
      String poolPrefix,
      int corePoolSize,
      int maximumPoolSize,
      int queueSize,
      long keepAliveTime,
      TimeUnit unit,
      Consumer<Runnable> afterExecuteFunc) {
    super(
        poolPrefix,
        corePoolSize,
        maximumPoolSize,
        keepAliveTime,
        unit,
        new PriorityBlockingQueue<Runnable>(queueSize, new PriorityRunnableFutureComparator()),
        new CommonThreadFactory(poolPrefix),
        afterExecuteFunc);
  }

  @Override
  protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
    RunnableFuture<T> newTaskFor = super.newTaskFor(callable);
    return new PriorityRunnableFuture<>(newTaskFor, ((Prioritized) callable).getPriority());
  }

  @Override
  protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
    RunnableFuture<T> newTaskFor = super.newTaskFor(runnable, value);
    return new PriorityRunnableFuture<>(newTaskFor, ((Prioritized) runnable).getPriority());
  }
}
