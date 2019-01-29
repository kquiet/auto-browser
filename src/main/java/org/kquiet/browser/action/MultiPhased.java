/*
 * Copyright 2019 kquiet.
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

/**
 * An interface which should be implemented by any browser action whose execution spans multiple phases.
 * 
 * @author Kimberly
 */
public interface MultiPhased extends Composable {

    /**
     * Signals that no more phases to execute
     */
    void noNextPhase();
    
    /**
     * 
     * @return {@code true} if has next phase to execute; {@code false} otherwise
     */
    boolean hasNextPhase();
}
