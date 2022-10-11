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

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link CommonThreadFactory} is for internal use to prefix thread pools.
 *
 * @author Kimberly
 */
class CommonThreadFactory implements ThreadFactory {
  private static final AtomicInteger POOL_NUMBER = new AtomicInteger(1);
  private final AtomicInteger threadNumber = new AtomicInteger(1);
  private final String namePrefix;

  /**
   * Create a {@link ThreadFactory} with specified pool name.
   *
   * @param poolName pool name used in prefix
   */
  public CommonThreadFactory(String poolName) {
    if (poolName != null && !poolName.isEmpty()) {
      this.namePrefix = poolName + "(" + POOL_NUMBER.getAndIncrement() + ")-";
    } else {
      this.namePrefix = "pool(" + POOL_NUMBER.getAndIncrement() + ")-";
    }
  }

  @Override
  public Thread newThread(Runnable r) {
    Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
    if (t.isDaemon()) {
      t.setDaemon(false);
    }
    if (t.getPriority() != Thread.NORM_PRIORITY) {
      t.setPriority(Thread.NORM_PRIORITY);
    }
    return t;
  }
}
