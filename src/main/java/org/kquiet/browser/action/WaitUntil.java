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

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import org.kquiet.browser.ActionComposer;
import org.kquiet.browser.action.exception.ActionException;
import org.kquiet.utility.Stopwatch;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.FluentWait;

/**
 * {@link WaitUntil} is a subclass of {@link MultiPhaseAction} which waits the evaluation result of
 * condition function by phases to avoid blocking the execution of other browser actions.
 *
 * <p>{@link WaitUntil} waits until one of the following occurs:
 *
 * <ol>
 *   <li>the condition function returns neither null nor false
 *   <li>the condition function throws an unignored exception
 *   <li>the timeout expires
 *   <li>the execution thread of this {@link WaitUntil} is interrupted
 * </ol>
 *
 * <p>When timeout expires, it will throw an {@link
 * org.kquiet.browser.action.exception.ActionException ActionException} if no timeout callback
 * function is supplied; if a timeout callback function is supplied, it will execute the callback
 * function instead of throwing {@link org.kquiet.browser.action.exception.ActionException
 * ActionException}.
 *
 * @author Kimberly
 * @param <V> the expected return type of condition function
 */
public class WaitUntil<V> extends MultiPhaseAction {
  private static final int DEFAULT_PHASE_TIMEOUT = 10;
  private static final int DEFAULT_POLL_INTERVAL = 5;

  private final Stopwatch costWatch = new Stopwatch();

  private final int totalTimeout;
  private final int phaseTimeout;
  private final int pollInterval;
  private final Function<WebDriver, V> conditionFunc;
  private final Set<Class<? extends Throwable>> ignoreExceptions = new HashSet<>();
  private final Consumer<ActionComposer> timeoutCallback;

  /**
   * Create an action to wait with default phased timeout and polling interval until conditions are
   * met or timed out.
   *
   * @param conditionFunc the condition function for evaluation by phases
   * @param totalTimeout the maximum amount of time to wait totally
   */
  public WaitUntil(Function<WebDriver, V> conditionFunc, int totalTimeout) {
    this(conditionFunc, totalTimeout, DEFAULT_PHASE_TIMEOUT, DEFAULT_POLL_INTERVAL, null, null);
  }

  /**
   * Create an action to wait until conditions are met or timed out.
   *
   * @param conditionFunc the condition function for evaluation by phases
   * @param totalTimeout the maximum amount of time to wait totally
   * @param phaseTimeout the maximum amount of time to wait for each execution phase
   * @param pollInterval how often the condition function should be evaluated(the cost of actually
   *     evaluating the condition function is not factored in)
   */
  public WaitUntil(
      Function<WebDriver, V> conditionFunc, int totalTimeout, int phaseTimeout, int pollInterval) {
    this(conditionFunc, totalTimeout, phaseTimeout, pollInterval, null, null);
  }

  /**
   * Create an action to wait with default phased timeout and polling interval until conditions are
   * met or timed out.
   *
   * @param conditionFunc the condition function for evaluation by phases
   * @param totalTimeout the maximum amount of time to wait totally
   * @param timeoutCallback the callback function to be called when total timeout expires
   */
  public WaitUntil(
      Function<WebDriver, V> conditionFunc,
      int totalTimeout,
      Consumer<ActionComposer> timeoutCallback) {
    this(
        conditionFunc,
        totalTimeout,
        DEFAULT_PHASE_TIMEOUT,
        DEFAULT_POLL_INTERVAL,
        null,
        timeoutCallback);
  }

  /**
   * Create an action to wait with default phased timeout and polling interval until conditions are
   * met or timed out.
   *
   * @param conditionFunc the condition function for evaluation by phases
   * @param totalTimeout the maximum amount of time to wait totally
   * @param ignoreExceptions the types of exceptions to ignore when evaluating condition function;
   */
  public WaitUntil(
      Function<WebDriver, V> conditionFunc,
      int totalTimeout,
      Set<Class<? extends Throwable>> ignoreExceptions) {
    this(
        conditionFunc,
        totalTimeout,
        DEFAULT_PHASE_TIMEOUT,
        DEFAULT_POLL_INTERVAL,
        ignoreExceptions,
        null);
  }

  /**
   * Create an action to wait with default phased timeout and polling interval until conditions are
   * met or timed out.
   *
   * @param conditionFunc the condition function for evaluation by phases
   * @param totalTimeout the maximum amount of time to wait totally
   * @param ignoreExceptions the types of exceptions to ignore when evaluating condition function;
   * @param timeoutCallback the callback function to be called when total timeout expires
   */
  public WaitUntil(
      Function<WebDriver, V> conditionFunc,
      int totalTimeout,
      Set<Class<? extends Throwable>> ignoreExceptions,
      Consumer<ActionComposer> timeoutCallback) {
    this(
        conditionFunc,
        totalTimeout,
        DEFAULT_PHASE_TIMEOUT,
        DEFAULT_POLL_INTERVAL,
        ignoreExceptions,
        timeoutCallback);
  }

  /**
   * Create an action to wait until conditions are met or timed out.
   *
   * @param conditionFunc the condition function for evaluation by phases
   * @param totalTimeout the maximum amount of time to wait totally
   * @param phaseTimeout the maximum amount of time to wait for each execution phase
   * @param pollInterval how often the condition function should be evaluated(the cost of actually
   *     evaluating the condition function is not factored in)
   * @param ignoreExceptions the types of exceptions to ignore when evaluating condition function.
   *     If this parameter value is null or empty, then {@link
   *     org.openqa.selenium.StaleElementReferenceException} and {@link
   *     org.openqa.selenium.NoSuchElementException} are ignored; otherwise the given types of
   *     exceptions are used.
   * @param timeoutCallback the callback function to be called when total timeout expires
   */
  public WaitUntil(
      Function<WebDriver, V> conditionFunc,
      int totalTimeout,
      int phaseTimeout,
      int pollInterval,
      Set<Class<? extends Throwable>> ignoreExceptions,
      Consumer<ActionComposer> timeoutCallback) {
    this.totalTimeout = totalTimeout;
    this.phaseTimeout = phaseTimeout;
    this.pollInterval = pollInterval;
    this.conditionFunc = conditionFunc;
    if (ignoreExceptions != null && !ignoreExceptions.isEmpty()) {
      this.ignoreExceptions.addAll(ignoreExceptions);
    } else {
      this.ignoreExceptions.add(StaleElementReferenceException.class);
      this.ignoreExceptions.add(NoSuchElementException.class);
    }
    this.timeoutCallback = timeoutCallback;
  }

  private void timeoutToDo() {
    this.noNextPhase();
    if (timeoutCallback != null) {
      ActionComposer actionComposer = this.getComposer();
      if (!actionComposer.switchToFocusWindow()) {
        throw new ActionException("can't switch to focus window");
      }
      // firefox doesn't switch focus to top after switch to window, so recovery step is required
      switchToTopForFirefox();
      timeoutCallback.accept(actionComposer);
    } else {
      throw new ActionException("Timeout!");
    }
  }

  private boolean isTimeout() {
    return costWatch.getElapsedMilliSecond() >= totalTimeout;
  }

  @Override
  protected void performMultiPhase() {
    costWatch.start();

    if (isTimeout()) {
      timeoutToDo();
      return;
    }

    ActionComposer actionComposer = this.getComposer();
    FluentWait<WebDriver> wait =
        new FluentWait<>(actionComposer.getWebDriver())
            .withTimeout(Duration.ofMillis(phaseTimeout))
            .pollingEvery(Duration.ofMillis(pollInterval))
            .ignoreAll(ignoreExceptions);

    V result = null;
    try {
      // firefox doesn't switch focus to top after switch to window, so recovery step is required
      switchToTopForFirefox();
      result = wait.until(conditionFunc);
    } catch (TimeoutException e) {
      if (isTimeout()) {
        timeoutToDo();
        return;
      }
    }

    // condition not met
    if (result == null || (Boolean.class == result.getClass() && Boolean.FALSE.equals(result))) {
      if (isTimeout()) {
        timeoutToDo();
      }
    } else {
      // condition met => no next phase
      this.noNextPhase();
    }
  }

  @Override
  public String toString() {
    return String.format(
        "%s:%s/%s/%s",
        WaitUntil.class.getSimpleName(),
        String.valueOf(totalTimeout),
        String.valueOf(phaseTimeout),
        String.valueOf(pollInterval));
  }
}
