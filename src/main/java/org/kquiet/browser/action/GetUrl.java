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

import org.kquiet.browser.ActionComposer;
import org.kquiet.browser.action.exception.ActionException;

/**
 * {@link GetUrl} is a subclass of {@link SinglePhaseAction} which loads a web page.
 * 
 * @author Kimberly
 */
public class GetUrl extends SinglePhaseAction {
  private final String url;

  /**
   * Create an action to get current page's url.
   * 
   * @param url the url of page
   */
  public GetUrl(String url) {
    this.url = url;
  }

  @Override
  protected void performSinglePhase() {
    ActionComposer actionComposer = this.getComposer();
    try {
      actionComposer.getWebDriver().get(this.url);
    } catch (Exception e) {
      throw new ActionException(e);
    }
  }

  @Override
  public String toString() {
    return String.format("%s:%s", GetUrl.class.getSimpleName(), url);
  }
}
