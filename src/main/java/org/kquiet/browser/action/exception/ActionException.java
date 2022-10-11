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

package org.kquiet.browser.action.exception;

/**
 * Thrown to indicate that a browser action encounters a non-recoverable problem during execution.
 *
 * @author Kimberly
 */
public class ActionException extends RuntimeException {
  static final long serialVersionUID = 1L;

  /**
   * Constructs an {@link ActionException} with the specified detail message.
   *
   * @param message the detail message
   */
  public ActionException(String message) {
    super(message);
  }

  /**
   * Constructs an {@link ActionException} with the specified cause.
   *
   * @param cause the cause
   */
  public ActionException(Throwable cause) {
    super(cause);
  }

  /**
   * Constructs a new exception with the specified detail message and cause.
   *
   * @param message the detail message
   * @param cause the cause
   */
  public ActionException(String message, Throwable cause) {
    super(message, cause);
  }
}
