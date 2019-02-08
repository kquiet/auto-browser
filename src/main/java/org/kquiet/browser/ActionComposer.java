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
 * If any executed action fails, {@link ActionComposer} should mark itself failed as well.
 * 
 * <p>{@link ActionComposer} also works as a context across actions. It provides some methods to keep information of windows and variables for these actions to use.</p>
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
     * Set associated {@link ActionRunner}.
     * 
     * @param actionRunner action runner to set
     * @return self reference
     */
    ActionComposer setActionRunner(ActionRunner actionRunner);
    
    /**
     * Register a window to keep window identity.
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
     * @return all registered windows(register name and window identity)
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
     * Set variable value.
     * 
     * @param variableName variable name
     * @param value variable value
     * @return self reference
     */
    ActionComposer setVariable(String variableName, Object value);
    
    /**
     * Delegate the execution of {@link Runnable} to associated {@link ActionRunner} with this {@link ActionComposer}'s priority
     * 
     * @param runnable the object whose run method will be invoked
     * @return a {@link CompletableFuture} representing pending completion of given {@link Runnable}
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
     * 
     * @return error list from executed actions
     */
    List<Exception> getErrors();
    
    /**
     *
     * @return the url of the last page when this {@link ActionComposer} is marked as failed and {@link #keepFailInfo(boolean) keep fail info} is enabled; {@code null} otherwise
     */
    String getFailUrl();

    /**
     *
     * @return the content of the last page when this {@link ActionComposer} is marked as failed and {@link #keepFailInfo(boolean) keep fail info} is enabled; {@code null} otherwise
     */
    String getFailPage();
    
    /**
     * Enable/Disable the function of keeping fail information when this {@link ActionComposer} is marked as failed.
     * The function of keeping fail information takes about one second to complete, however this may seem wasteful in many applications,
     * hence this method can be used to determine keep or not.
     * 
     * @param flag {@code true} to enable; {@code false} to disable
     * @return self reference
     */
    ActionComposer keepFailInfo(boolean flag);
    
    /**
     * Set the callback function to be executed when this {@link ActionComposer} is marked as failed.
     * 
     * @param onFailFunc the callback function to be executed
     * @return self reference
     */
    ActionComposer onFail(Consumer<ActionComposer> onFailFunc);
    
    /**
     * Set the callback function to be executed when this {@link ActionComposer} is finished without being marked as failed.
     * 
     * @param onSuccessFunc the callback function to be executed
     * @return self reference
     */
    ActionComposer onSuccess(Consumer<ActionComposer> onSuccessFunc);
    
    /**
     * Set the callback function to be executed when this {@link ActionComposer} is done.
     * This callback function is executed after <i>fail function</i> and <i>success function</i>.
     * 
     * @param onDoneFunc the callback function to be executed
     * @return self reference
     */
    ActionComposer onDone(Consumer<ActionComposer> onDoneFunc);
    
    /**
     * Get {@link WebDriver} from associated {@link ActionRunner}.
     * Use this with caution because the associated {@link ActionRunner} use the same {@link WebDriver} when executing browser actions,
     * however {@link WebDriver} is <a href="https://github.com/SeleniumHQ/selenium/wiki/Frequently-Asked-Questions#q-is-webdriver-thread-safe" target="_blank">not thread-safe</a>.
     * 
     * <p>A safer way to use this is to encapsulate the process in a {@link Runnable}, or use built-in {@link Custom custom action},
     * and then execute it through {@link ActionRunner#executeAction(java.lang.Runnable, int)}.</p>
     * 
     * @return the {@link WebDriver} of associated {@link ActionRunner}.
     */
    WebDriver getWebDriver();
    
    /**
     * Add action to the head of the action sequence of execution context.
     * 
     * @param action action to add
     * @return execution context of actions
     */
    AddableActionSequence addToHeadByContext(Composable action);

    /**
     * Add action to the tail of the action sequence of execution context.
     * 
     * @param action action to add
     * @return execution context of actions
     */
    AddableActionSequence addToTailByContext(Composable action);
    
    /**
     * Add action to specified position of the action sequence of execution context.
     * 
     * @param action action to add
     * @param position the position(zero-based) to add given action
     * @return execution context of actions
     */
    AddableActionSequence addToPositionByContext(Composable action, int position);
}