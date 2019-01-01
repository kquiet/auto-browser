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
package org.kquiet.test;

import org.openqa.selenium.PageLoadStrategy;

import org.kquiet.browser.ActionRunner;
import org.kquiet.browser.WebDriverType;

/**
 *
 * @author Kimberly
 */
public class TestHelper {

    /**
     *
     * @param maxConcurrentComposer
     * @return
     */
    public static ActionRunner createRunner(int maxConcurrentComposer){
        return new ActionRunner(PageLoadStrategy.NONE, WebDriverType.Chrome, "TestBrowser", maxConcurrentComposer);
    }
}
