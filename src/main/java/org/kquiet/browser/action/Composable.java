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

import java.util.List;

import org.kquiet.browser.ActionComposer;

/**
 * An interface which should be implemented by any browser action which delegates its execution to {@link ActionComposer}.
 * 
 * @author Kimberly
 */
public interface Composable extends Runnable {
    
    /**
     * Get containing composer.
     * 
     * @return containing composer
     */
    public ActionComposer getComposer();

    /**
     * Set containing composer.
     * 
     * @param composer the containing composer to set
     * @return invoking {@link Composable}
     */
    public Composable setComposer(ActionComposer composer);

    /**
     *
     * @return the errors occurred during execution
     */
    public List<Exception> getErrors();

    /**
     *
     * @return {@code true} if the action is done; {@code false} otherwise
     */
    public boolean isDone();


    /**
     * 
     * @return {@code true} if the action is failed; {@code false} otherwise
     */
    public boolean isFail();
}
