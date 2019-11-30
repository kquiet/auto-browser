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

import java.util.List;

import org.kquiet.browser.ActionComposer;

/**
 * An interface which should be implemented by any action which delegates its execution to
 * {@link ActionComposer}.
 * 
 * @author Kimberly
 */
public interface Composable {

  /**
   * Get containing composer.
   * 
   * @return containing composer
   */
  ActionComposer getComposer();

  /**
   * Set containing composer.
   * 
   * @param composer the containing composer to set
   * @return invoking {@link Composable}
   */
  Composable setComposer(ActionComposer composer);

  /**
   * Perform action.
   */
  void perform();

  /**
   * Get the errors occurred during execution.
   * 
   * @return the errors as a list of {@link Exception}
   */
  List<Exception> getErrors();

  /**
   * Check if this action is done.
   * 
   * @return {@code true} if this action is done; {@code false} otherwise
   */
  boolean isDone();


  /**
   * Check if this action is failed.
   * 
   * @return {@code true} if this action is failed; {@code false} otherwise
   */
  boolean isFail();
}
