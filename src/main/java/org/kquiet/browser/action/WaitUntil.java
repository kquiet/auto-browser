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
import java.util.HashSet;
import java.util.function.Function;
import java.util.Set;
import java.util.function.Consumer;

import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.NoSuchElementException;

import org.kquiet.browser.ActionComposer;
import org.kquiet.browser.action.exception.ActionException;
import org.kquiet.utility.StopWatch;

/**
 * {@link WaitUntil} is a subclass of {@link MultiPhaseAction} which waits the evaluation result of condition function by phases to avoid blocking the execution of other browser actions.
 * 
 * <p>{@link WaitUntil} waits until one of the following occurs:</p>
 * <ol>
 * <li>the condition function returns neither null nor false</li>
 * <li>the condition function throws an unignored exception</li>
 * <li>the timeout expires</li>
 * <li>the execution thread of this {@link WaitUntil} is interrupted</li>
 * </ol>
 * 
 * <p>When timeout expires, it will throw an {@link org.kquiet.browser.action.exception.ActionException ActionException} if no timeout callback function is supplied;
 * if a timeout callback function is supplied, it will execute the callback function instead of throwing {@link org.kquiet.browser.action.exception.ActionException ActionException}.</p>
 * 
 * @author Kimberly
 * @param <V> the expected return type of condition function
 */
public class WaitUntil<V> extends MultiPhaseAction {
    private final StopWatch costWatch = new StopWatch();
    
    private final int totalTimeout;
    private final int phaseTimeout;
    private final int pollInterval;
    private final Function<WebDriver,V> conditionFunc;
    private final Set<Class<? extends Throwable>> ignoreExceptions = new HashSet<>();
    private final Consumer<ActionComposer> timeoutCallback;
    
    /**
     *
     * @param conditionFunc the condition function for evaluation by phases
     * @param totalTimeout the maximum amount of time to wait totally
     * @param phaseTimeout the maximum amount of time to wait for each execution phase
     * @param pollInterval how often the condition function should be evaluated(the cost of actually evaluating the condition function is not factored in)
     * @param ignoreExceptions the types of exceptions to ignore when evaluating condition function;
     * {@link org.openqa.selenium.StaleElementReferenceException} and {@link org.openqa.selenium.NoSuchElementException} are ignored by default.
     * @param timeoutCallback the callback function to be called when total timeout expires
     */
    public WaitUntil(Function<WebDriver,V> conditionFunc, int totalTimeout, int phaseTimeout, int pollInterval, Set<Class<? extends Throwable>> ignoreExceptions, Consumer<ActionComposer> timeoutCallback){
        super(null);
        this.totalTimeout = totalTimeout;
        this.phaseTimeout = phaseTimeout;
        this.pollInterval = pollInterval;
        this.conditionFunc = conditionFunc;
        this.ignoreExceptions.add(StaleElementReferenceException.class);
        this.ignoreExceptions.add(NoSuchElementException.class);
        if (ignoreExceptions!=null) this.ignoreExceptions.addAll(ignoreExceptions);
        this.timeoutCallback = timeoutCallback;
        super.setInternalAction(multiPhaseWaitUntil());
    }
    
    private Runnable multiPhaseWaitUntil(){
        return ()->{
            costWatch.start();
            
            if (isTimeout()){
                timeoutToDo();
                return;
            }

            ActionComposer actionComposer = this.getComposer();
            FluentWait<WebDriver> wait = new FluentWait<>(actionComposer.getBrsDriver())
            .withTimeout(Duration.ofMillis(phaseTimeout))
            .pollingEvery(Duration.ofMillis(pollInterval))
            .ignoreAll(ignoreExceptions);
            
            V result=null;
            try{
                actionComposer.switchToFocusWindow();
                result = wait.until(conditionFunc);
            }
            catch(TimeoutException e){
                if (isTimeout()){
                    timeoutToDo();
                    return;
                }
            }
            
            //condition not met
            if (result==null || ((result instanceof Boolean) && ((Boolean)result)==false)){
                if (isTimeout()){
                    timeoutToDo();
                }
            }
            else{
                //condition met => no next phase
                this.noNextPhase();
            }
        };
    }
    
    private void timeoutToDo(){
        this.noNextPhase();
        if (timeoutCallback!=null){
            ActionComposer actionComposer = this.getComposer();
            actionComposer.switchToFocusWindow();
            timeoutCallback.accept(actionComposer);
        }
        else{
            throw new ActionException("Timeout! "+toString());
        }
    }
    
    private boolean isTimeout(){
        return costWatch.getElapsedMilliSecond()>=totalTimeout;
    }
    
    @Override
    public String toString(){
        return String.format("%s(%s) %s:%s/%s/%s"
                , ActionComposer.class.getSimpleName(), getComposer()==null?"":getComposer().getName()
                , WaitUntil.class.getSimpleName(), String.valueOf(totalTimeout), String.valueOf(phaseTimeout), String.valueOf(pollInterval));
    }
}
