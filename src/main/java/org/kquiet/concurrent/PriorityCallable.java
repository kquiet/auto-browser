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

/**
 * Prioritized {@link Callable}. This class is for internal use.
 *
 * @author Kimberly
 * @param <T> the expected return type of {@link Callable}
 */
public class PriorityCallable<T> implements Prioritized, Callable<T> {
  private final Callable<T> callable;
  private final int priority;

  /**
   * Crate a prioritized {@link Callable}.
   *
   * @param callable wrapped {@link Callable}
   * @param priority priority
   */
  public PriorityCallable(Callable<T> callable, int priority) {
    this.callable = callable;
    this.priority = priority;
  }

  @Override
  public T call() throws Exception {
    return callable.call();
  }

  @Override
  public int getPriority() {
    return priority;
  }
}
