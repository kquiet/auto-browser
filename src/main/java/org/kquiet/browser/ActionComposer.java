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

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.By;

import org.kquiet.concurrent.Prioritized;
import org.kquiet.browser.action.Composable;

/**
 * {@link ActionComposer} is responsible to maintain a sequence of actions, arrange them to be executed and track their execution result.
 * If any executed action fails, {@link ActionComposer} marks itself failed as well.
 *
 * @author Kimberly
 */
public interface ActionComposer extends Runnable, Prioritized, AddableActionSequence {
    
    /**
     *
     * @return the name of this {@link ActionComposer}
     */
    String getName();
    
    /**
     * Set the name of this {@link ActionComposer}.
     * 
     * @param name name
     * @return self reference
     */
    ActionComposer setName(String name);
    
    /**
     *
     * @param actionRunner action runner to set
     * @return self reference
     */
    ActionComposer setActionRunner(ActionRunner actionRunner);
    
    /**
     * Register a window.
     * 
     * @param name register name
     * @param windowIdentity window identity
     * @return {@code true} if register name is not null and window identity are not empty and the register name isn't registered; {@code false} otherwise
     */
    boolean registerWindow(String name, String windowIdentity);
    
    /**
     *
     * @param registerName register name of window
     * @return window identity
     */
    String getRegisteredWindow(String registerName);
    
    /**
     *
     * @return all windows(register name and window identity) of registered windows
     */
    Map<String, String> getRegisteredWindows();
    
    /**
     * Get the value of specified variable.
     * 
     * @param variableName variable name
     * @return variable value
     */
    Object getVariable(String variableName);
    
    /**
     * remove specified variable.
     * 
     * @param variableName variable name
     */
    void removeVariable(String variableName);
    
    /**
     * Set variable value.
     * 
     * @param variableName variable name
     * @param value variable value
     * @return self reference
     */
    ActionComposer setVariable(String variableName, Object value);
    
    /**
     * Delegate the execution of {@link java.lang.Runnable runnable} to associated {@link ActionRunner} with this {@link ActionComposer}'s priority
     * 
     * @param runnable the object whose run method will be invoked
     * @return a {@link CompletableFuture} representing pending completion of given {@link java.lang.Runnable runnable}
     */
    CompletableFuture<Void> callBrowser(Runnable runnable);
    
    /**
     * Perform action. Any {@link Composable} should be performed by this method.
     * 
     * @param action action to perform
     */
    void perform(Composable action);
    
    /**
     *
     * @return the window identity of root window
     */
    String getRootWindow();
    
    /**
     * Switch browser's focus window to this {@link ActionComposer}'s focus window.
     * 
     * @return {@code true} if switch success; {@code false} otherwise
     */
    boolean switchToFocusWindow();
    
    /**
     *
     * @return the window identity of focus window
     */
    String getFocusWindow();

    /**
     * @param windowIdentity the window identity to set as the focus window
     * @return self reference
     */
    ActionComposer setFocusWindow(String windowIdentity);
    
    /**
     * Switch browser's focus window to specified window.
     * 
     * @param windowIdentity the window identity to switch to
     * @return {@code true} if switch success; {@code false} otherwise
     */
    boolean switchToWindow(String windowIdentity);
    
    /**
     * Switch to send future commands to a frame.
     * 
     * @param frameBySequence the sequence of the frame locating mechanism
     */
    void switchToInnerFrame(List<By> frameBySequence);
    
    /**
     * Switch the focus of future commands to either the first frame on the page, or the main document when a page contains iframes.
     * 
     * @return {@code true} if switch success; {@code false} otherwise
     */
    boolean switchToTop();
    
    /**
     *
     * @return {@code true} if this {@link ActionComposer} has been marked as failed; {@code false} otherwise
     */
    boolean isFail();
    
    /**
     *
     * @return {@code true} if this {@link ActionComposer} is done without being marked as failed; {@code false} otherwise
     */
    boolean isSuccessfulDone();
    
    /**
     *
     * @return {@code true} if this {@link ActionComposer} is done; {@code false} otherwise
     */
    boolean isDone();

    /**
     * Skip the execution of remaining actions and mark this {@link ActionComposer} as failed.
     */
    void skipToFail();
    
    /**
     * Skip the execution of remaining actions.
     */
    void skipToSuccess();
    
    /**
     * Set the callback function to be executed when this {@link ActionComposer} is marked as failed.
     * 
     * @param onFailFunc the callback function to be executed
     * @return self reference
     */
    public ActionComposer onFail(Consumer<ActionComposer> onFailFunc);
    
    /**
     * Set the callback function to be executed when this {@link ActionComposer} is finished without being marked as failed.
     * 
     * @param onSuccessFunc the callback function to be executed
     * @return self reference
     */
    public ActionComposer onSuccess(Consumer<ActionComposer> onSuccessFunc);
    
    /**
     * Set the callback function to be executed when this {@link ActionComposer} is done.
     * This callback function is executed after <i>fail function</i> and <i>success function</i>.
     * 
     * @param onDoneFunc the callback function to be executed
     * @return self reference
     */
    public ActionComposer onDone(Consumer<ActionComposer> onDoneFunc);
    
    /**
     * Get {@link org.openqa.selenium.WebDriver WebDriver} from associated {@link ActionRunner}.
     * Use this with caution because the associated {@link ActionRunner} use the same {@link org.openqa.selenium.WebDriver WebDriver} when executing browser actions,
     * however {@link org.openqa.selenium.WebDriver WebDriver} is <a href="https://github.com/SeleniumHQ/selenium/wiki/Frequently-Asked-Questions#q-is-webdriver-thread-safe" target="_blank">not thread-safe</a>.
     * 
     * <p>A safer way to use this is to encapsulate the process in a {@link java.lang.Runnable Runnable}, or use built-in {@link org.kquiet.browser.action.Custom custom action},
     * and then execute it through {@link ActionRunner#executeAction(java.lang.Runnable, int)}.</p>
     * 
     * @return the {@link org.openqa.selenium.WebDriver WebDriver} from associated {@link ActionRunner}.
     */
    WebDriver getWebDriver();
    
    /**
     * Add action to the head of the action sequence of execution context.
     * 
     * @param action action to add
     * @return execution context represented by {@link AddableActionSequence}
     */
    AddableActionSequence addToHeadByContext(Composable action);

    /**
     * Add action to the tail of the action sequence of execution context.
     * 
     * @param action action to add
     * @return execution context represented by {@link AddableActionSequence}
     */
    AddableActionSequence addToTailByContext(Composable action);
    
    /**
     * Add action to specified position of the action sequence of execution context.
     * 
     * @param action action to add
     * @param position the position(zero-based) to add given action
     * @return execution context represented by {@link AddableActionSequence}
     */
    AddableActionSequence addToPositionByContext(Composable action, int position);
}