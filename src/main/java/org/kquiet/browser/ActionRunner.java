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

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;

import org.openqa.selenium.WebDriver;

/**
 * {@link ActionRunner} is resposible to run a browser through <a href="https://github.com/SeleniumHQ/selenium" target="_blank">Selenium</a> and execute actions against it.
 * With its methods of {@link #executeComposer(org.kquiet.browser.ActionComposer) executeComposer} and {@link #executeAction(java.lang.Runnable, int) executeAction},
 * users can run {@link ActionComposer}, or {@link java.lang.Runnable customized actions} with priority.
 * 
 * @author Kimberly
 */
public interface ActionRunner extends Closeable {

    /**
     * 
     * @return name of this {@link ActionRunner}
     */
    String getName();
    
    /**
     * Set the name of this {@link ActionRunner}.
     * 
     * @param name the name to set
     * @return this {@link ActionRunner}
     */
    ActionRunner setName(String name);
    
    /**
     * 
     * @return the identity of root window(the initial window as the browser started)
     */
    String getRootWindowIdentity();
    
    /**
     *
     * @param browserAction browser action to execute
     * @param priority priority of action
     * @return a {@link CompletableFuture} representing the pending completion of given browser action
     */
    CompletableFuture<Void> executeAction(Runnable browserAction, int priority);
    
    /**
     *
     * @param actionComposer {@link ActionComposer} to execute
     * @return a {@link CompletableFuture} representing the pending completion of given {@link ActionComposer}
     */
    CompletableFuture<Void> executeComposer(ActionComposer actionComposer);
    
    /**
     *
     * @return the {@link WebDriver} this {@link ActionRunner} is using
     */
    WebDriver getWebDriver();
    
    /**
     * 
     * @return {@code true} if the browser is still running; {@code false} otherwise
     */
    boolean isBrowserAlive();
    
    /**
     * Stop executing any newly incoming {@link ActionComposer} or browser action, but the running ones may or may not keep running(depends on implementation).
     */
    void pause();
    
    /**
     * Resume to accept {@link ActionComposer} or browser action (if any).
     */
    void resume();
    
    /**
     *
     * @return {@code true} if paused; {@code false} otherwise
     */
    boolean isPaused();
    
    @Override
    void close();
}
