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

package org.kquiet.browser.action;

import org.kquiet.browser.action.exception.ActionException;
import org.kquiet.utility.Stopwatch;

/**
 * {@link JustWait} is a subclass of {@link MultiPhaseAction} which just waits by phases to avoid
 * blocking the execution of other browser actions.
 * @author Kimberly
 */
public class JustWait extends MultiPhaseAction {
  private final Stopwatch costWatch = new Stopwatch();

  private final int totalTimeout;
  private final int phaseTimeout;

  /**
   * With phase timeout defaulted to 10.
   * 
   * @param totalTimeout the maximum amount of time to wait totally
   */
  public JustWait(int totalTimeout) {
    this(totalTimeout, 10);
  }

  /**
   * Create an action representing just waiting.
   * 
   * @param totalTimeout the maximum amount of time to wait totally
   * @param phaseTimeout the maximum amount of time to wait for each execution phase
   */
  public JustWait(int totalTimeout, int phaseTimeout) {
    this.totalTimeout = totalTimeout;
    this.phaseTimeout = phaseTimeout;
  }

  private boolean isTimeout() {
    return costWatch.getElapsedMilliSecond() >= totalTimeout;
  }

  @Override
  protected void performMultiPhase() {
    costWatch.start();

    if (isTimeout()) {
      noNextPhase();
      return;
    }

    try {
      Thread.sleep(phaseTimeout);
    } catch (Exception e) {
      throw new ActionException(e);
    }

    //add sub-action to wait until element is found or timeout
    if (isTimeout()) {
      noNextPhase();
    }
  }

  @Override
  public String toString() {
    return String.format("%s:%s/%s", JustWait.class.getSimpleName(), String.valueOf(totalTimeout),
        String.valueOf(phaseTimeout));
  }
}