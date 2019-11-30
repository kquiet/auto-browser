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

package org.kquiet.utility;

import java.time.Duration;

/**
 * Stopwatch.
 * 
 * @author Kimberly
 */
public class Stopwatch {
  private volatile long startTime = 0;
  private volatile long stopTime = 0;
  private volatile boolean running = false;

  /**
   * Start this {@link Stopwatch} without reset.
   */
  public void start() {
    this.start(false);
  }

  /**
   * Start this {@link Stopwatch} with the option to reset.
   * 
   * @param resetFlag {@code true}: reset; {@code false}: not reset
   */
  public void start(boolean resetFlag) {
    if (resetFlag) {
      startTime = 0;
      stopTime = 0;
    }
    if (startTime == 0) {
      this.startTime = System.nanoTime();
    }
    this.running = true;
  }

  /**
   * Stop this {@link Stopwatch}.
   */
  public void stop() {
    if (this.running) {
      this.stopTime = System.nanoTime();
      this.running = false;
    }
  }

  /**
   * Get elapsed time in milliseconds.
   * 
   * @return elapsed time in milliseconds
   */
  public long getElapsedMilliSecond() {
    long elapsed;
    if (running) {
      elapsed = (System.nanoTime() - startTime) / 1000000;
    } else {
      elapsed = (stopTime - startTime) / 1000000;
    }
    return elapsed;
  }

  /**
   * Get elapsed time in nanoseconds.
   * 
   * @return elapsed time in nanoseconds
   */
  public long getElapsedNanoSecond() {
    long elapsed;
    if (running) {
      elapsed = (System.nanoTime() - startTime);
    } else {
      elapsed = (stopTime - startTime);
    }
    return elapsed;
  }

  /**
   * Get elapsed time.
   * 
   * @return elapsed time represented by {@link Duration}
   */
  public Duration getDuration() {
    return Duration.ofNanos(getElapsedNanoSecond());
  }
}
