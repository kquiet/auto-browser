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

import org.kquiet.browser.action.Composable;

/**
 * An {@link DynamicActionSequence} is a sequence of actions which supports add operations.
 * 
 * @author Kimberly
 */
public interface DynamicActionSequence {
  /**
   * Add action to the head of the action sequence.
   * 
   * @param action action to add
   * @return self reference
   */
  DynamicActionSequence addToHead(Composable action);

  /**
   * Add action to the tail of the action sequence.
   * 
   * @param action action to add
   * @return self reference
   */
  DynamicActionSequence addToTail(Composable action);

  /**
   * Add action to the specified position of the action sequence.
   * 
   * @param action action to add
   * @param position the position(zero-based) to add given action
   * @return self reference
   */
  DynamicActionSequence addToPosition(Composable action, int position);

  /**
   * Add action to the tail of the action sequence. This method is a short cut of
   * {@link #addToTail(org.kquiet.browser.action.Composable) }.
   * 
   * @param action action to add
   * @return self reference
   */
  default DynamicActionSequence add(Composable action) {
    return addToTail(action);
  }
}
