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
package org.kquiet.browser.action;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.firefox.FirefoxDriver;

import org.kquiet.utility.Stopwatch;
import org.kquiet.browser.ActionComposer;
import org.kquiet.browser.action.exception.ActionException;

/**
 * {@link MultiPhaseAction} models a browser action with multiple phases which is executed through {@link ActionComposer} or {@link org.kquiet.browser.ActionRunner}. It will be executed for multiple times and works just like a loop.
 * {@link #noNextPhase()} needs to be invoked to signal that there is no more phase to execute.
 * 
 * <p>Why multi-phase is required is because <a href="https://github.com/SeleniumHQ/selenium/wiki/Frequently-Asked-Questions#q-is-webdriver-thread-safe" target="_blank">WebDriver is not thread-safe</a>.
 * Therefore, {@link org.kquiet.browser.ActionRunner} uses a single thread to control the concurrency between different brwoser actions, and the browser thread shouldn't be occupied for a long time by any single browser action.</p>
 * 
 * @author Kimberly
 */
public abstract class MultiPhaseAction implements Composable, MultiPhased{
    private static final Logger LOGGER = LoggerFactory.getLogger(MultiPhaseAction.class);
    
    private ActionComposer containingComposer;
    private volatile ActionState actionState = ActionState.CREATED;
    private final List<Exception> errorList = new ArrayList<>();
    private final Stopwatch stopWatch = new Stopwatch();
    private volatile boolean hasNextPhase = true; //flags whether has next phase or not

    @Override
    public void noNextPhase(){
        this.hasNextPhase = false;
    }
    
    @Override
    public boolean hasNextPhase(){
        return hasNextPhase;
    }
    
    @Override
    public void run(){
        while(hasNextPhase()){
            try{
                stopWatch.start();
                setActionState(ActionState.RUNNING);

                //only browserable action is gonna run at browser to avoid blocking browser unnecessarily
                if (this.getClass().isAnnotationPresent(Nonbrowserable.class)){
                    perform();
                }
                else{
                    CompletableFuture<Void> future= getComposer().callBrowser(()->{
                        //switch to focus window before execute internal action
                        if (!getComposer().switchToFocusWindow()){
                            throw new ActionException("can't switch to focus window");
                        }

                        perform();
                    });
                    future.get();
                }

                if (hasNextPhase()) setActionState(ActionState.WAIT_FOR_NEXT_PHASE);
                else setActionState(ActionState.COMPLETE);
            }catch(Exception e){
                noNextPhase();
                setActionState(ActionState.COMPLETE_WITH_ERROR);
                errorList.add(e);
                LOGGER.warn("{}({}) {} error:{}", ActionComposer.class.getSimpleName(), getComposer().getName(), getClass().getSimpleName(), toString(), e);
            }finally{
                stopWatch.stop();
            }
        }
    }
    
    /**
     * Perform multiple-phased browser action. {@link #noNextPhase()} needs to be invoked to signal that there is no more phase to execute.
     */
    protected abstract void perform();
    
    
    /**
     * When the state of action is one of the following, then it's called <i>done</i>:
     * <ul>
     * <li>{@link ActionState#COMPLETE}</li>
     * <li>{@link ActionState#COMPLETE_WITH_ERROR}</li>
     * </ul>
     * 
     * @return {@code true} if the action is done; {@code false} otherwise
     */
    @Override
    public boolean isDone(){
        return Arrays.asList(ActionState.COMPLETE_WITH_ERROR, ActionState.COMPLETE).contains(getActionState());
    }
    
    /**
     * When the action is marked as failed({@link ActionState#COMPLETE_WITH_ERROR}), then it's called <i>fail</i>.
     * 
     * @return {@code true} if the action is failed; {@code false} otherwise
     */
    @Override
    public boolean isFail(){
        return getActionState()==ActionState.COMPLETE_WITH_ERROR;
    }
    
    @Override
    public List<Exception> getErrors() {
        return errorList;
    }
    
    @Override
    public String toString(){
        return MultiPhaseAction.class.getSimpleName();
    }

    /**
     *
     * @return the total cost time of execution
     */
    public Duration getCostTime() {
        return stopWatch.getDuration();
    }

    @Override
    public ActionComposer getComposer() {
        return containingComposer;
    }

    @Override
    public MultiPhaseAction setComposer(ActionComposer containingComposer) {
        this.containingComposer = containingComposer;
        return this;
    }

    /**
     * @return state of action
     */
    public ActionState getActionState() {
        return actionState;
    }

    /**
     * @param actionState the action state to set
     */
    protected void setActionState(ActionState actionState) {
        this.actionState = actionState;
    }
    
    /**
     * Switch to top for firefox. Firefox doesn't switch focus to top after switching to a window, so this method is required for firefox.
     */
    protected void switchToTopForFirefox(){
        WebDriver driver = getComposer().getWebDriver();
        if (driver instanceof FirefoxDriver){
            //if alert box exists, don't switch to top because this will dismiss the alert box(firefox only) which is not expected.
            try {
                driver.switchTo().alert();
            } catch (NoAlertPresentException e) {
                getComposer().switchToTop();
            }
        }
    }
}
