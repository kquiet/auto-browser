/*
 * Copyright 2018 kquiet.
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
 *
 * @author Kimberly
 */
public enum WebDriverType {

    /**
     *
     */
    Chrome,

    /**
     *
     */
    Firefox;
    
    /**
     *
     * @param text
     * @return
     */
    public static WebDriverType fromString(String text) {
        for (WebDriverType driverType : WebDriverType.values()) {
            if ("gecko".equalsIgnoreCase(text)){
                return WebDriverType.Firefox;
            }
            else if (driverType.toString().equalsIgnoreCase(text)) {
                return driverType;
            }
        }
        return null;
    }
}
