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

package org.kquiet.test;

import org.kquiet.browser.ActionRunner;
import org.kquiet.browser.BasicActionRunner;
import org.kquiet.browser.BrowserType;
import org.openqa.selenium.PageLoadStrategy;

/**
 * Helper class for testing.
 *
 * @author Kimberly
 */
public class TestHelper {

  /**
   * Create a browser.
   *
   * @param maxConcurrentComposer concurrency of browser
   * @return created browser
   */
  public static ActionRunner createRunner(int maxConcurrentComposer) {
    BrowserType browserType = BrowserType.CHROME; // use chrome as default test browser
    BrowserType temp = BrowserType.fromString(System.getProperty("test.browser"));
    if (temp != null) {
      browserType = temp;
    }
    return new BasicActionRunner(PageLoadStrategy.NONE, browserType, maxConcurrentComposer)
        .setName("TestBrowser");
  }
}
