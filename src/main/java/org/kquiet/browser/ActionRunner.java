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

package org.kquiet.browser;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;
import org.openqa.selenium.WebDriver;

/**
 * {@link ActionRunner} is resposible to run a browser through <a
 * href="https://github.com/SeleniumHQ/selenium" target="_blank">Selenium</a> and execute actions
 * against it. With its methods of {@link #executeComposer(org.kquiet.browser.ActionComposer)
 * executeComposer} and {@link #executeAction(java.lang.Runnable, int) executeAction}, users can run
 * {@link ActionComposer}, or {@link java.lang.Runnable customized actions} with priority.
 *
 * @author Kimberly
 */
public interface ActionRunner extends Closeable {
  /**
   * Get the name of this {@link ActionRunner}.
   *
   * @return name represented as {@link String}
   */
  String getName();

  /**
   * Set the name of this {@link ActionRunner}.
   *
   * @param name the name to set
   * @return this {@link ActionRunner}
   */
  ActionRunner setName(String name);

  /**
   * Get the identity of root window(the initial window as the browser started).
   *
   * @return identity represented as {@link String}
   */
  String getRootWindowIdentity();

  /**
   * Execute browser action with specified priority.
   *
   * @param browserAction browser action to execute
   * @param priority priority of action
   * @return a {@link CompletableFuture} representing the pending completion of given browser action
   */
  CompletableFuture<Void> executeAction(Runnable browserAction, int priority);

  /**
   * Execute given {@link ActionComposer}.
   *
   * @param actionComposer {@link ActionComposer} to execute
   * @return a {@link CompletableFuture} representing the pending completion of given {@link
   *     ActionComposer}
   */
  CompletableFuture<Void> executeComposer(ActionComposer actionComposer);

  /**
   * Get associated {@link WebDriver}.
   *
   * @return the {@link WebDriver} this {@link ActionRunner} is using
   */
  WebDriver getWebDriver();

  /**
   * Check whether browser is still running.
   *
   * @return {@code true} if the browser is still running; {@code false} otherwise
   */
  boolean isBrowserAlive();

  /**
   * Stop executing any newly incoming {@link ActionComposer} or browser action, but the running
   * ones may or may not keep running(depends on implementation).
   */
  void pause();

  /** Resume to accept {@link ActionComposer} or browser action (if any). */
  void resume();

  /**
   * Check whether this {@link ActionRunner} is paused or not.
   *
   * @return {@code true} if paused; {@code false} otherwise
   */
  boolean isPaused();

  @Override
  void close();
}
