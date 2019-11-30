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

/**
 * Supported browser type.
 * 
 * @author Kimberly
 */
public enum BrowserType {

  /**
   * Browser chrome / chromium.
   */
  CHROME("chrome"),

  /**
   * Browser firefox.
   */
  FIREFOX("firefox");

  private String text;

  private BrowserType(String text) {
    this.text = text;
  }

  @Override
  public String toString() {
    return String.valueOf(text);
  }

  /**
   * Get {@link BrowserType} represented by parsing its text(case insensitive).
   * 'Chromium' is considered as BrowserType.CHROME.
   * 
   * @param text text to be parsed
   * @return {@link BrowserType} represented by text; otherwise null
   */
  public static BrowserType fromString(String text) {
    if (text != null) {
      for (BrowserType browserType : BrowserType.values()) {
        if (text.equalsIgnoreCase(browserType.text)) {
          return browserType;
        }
      }
    }
    return null;
  }
}
