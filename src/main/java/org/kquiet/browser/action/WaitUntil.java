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
import java.util.function.Function;
import java.util.List;
import java.util.function.Consumer;

import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.FluentWait;

import org.kquiet.browser.ActionComposer;
import org.kquiet.browser.BrowserActionException;
import org.kquiet.utility.StopWatch;

/**
 *
 * @author Kimberly
 * @param <V>
 */
public class WaitUntil<V> extends MultiPhaseAction {
    private final StopWatch costWatch = new StopWatch();
    
    private final int totalTimeout;
    private final int phaseTimeout;
    private final int pollInterval;
    private final Function<WebDriver,V> evaluateFunc;
    private final List<Class<? extends Throwable>> ignoreExceptionList;
    private final Consumer<ActionComposer> timeoutCallback;
    
    /**
     *
     * @param evaluationFunc
     * @param totalTimeout
     * @param phaseTimeout
     * @param pollInterval
     * @param ignoreExceptionList
     * @param timeoutCallback
     */
    public WaitUntil(Function<WebDriver,V> evaluationFunc, int totalTimeout, int phaseTimeout, int pollInterval, List<Class<? extends Throwable>> ignoreExceptionList, Consumer<ActionComposer> timeoutCallback){
        super(null);
        this.totalTimeout = totalTimeout;
        this.phaseTimeout = phaseTimeout;
        this.pollInterval = pollInterval;
        this.evaluateFunc = evaluationFunc;
        this.ignoreExceptionList = ignoreExceptionList;
        this.timeoutCallback = timeoutCallback;
        this.setInternalAction(multiPhaseWaitUntil());
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
            .pollingEvery(Duration.ofMillis(pollInterval));
            if (ignoreExceptionList!=null && ignoreExceptionList.size()>0){
                wait = wait.ignoreAll(ignoreExceptionList);
            }
            V result=null;
            try{
                actionComposer.switchToFocusWindow();
                result = wait.until(evaluateFunc);
            }
            catch(TimeoutException e){
                if (isTimeout()){
                    timeoutToDo();
                    return;
                }
            }
            catch(Exception e){
                throw new BrowserActionException(toString(), e);
            }
            
            //condition not met
            if (result==null || ((result instanceof Boolean) && ((Boolean)result)==false)){
                if (isTimeout()){
                    timeoutToDo();
                }
            }
            else{
                //condition met => no next phase
                this.unregisterNextPhase();
            }
        };
    }
    
    private void timeoutToDo(){
        this.unregisterNextPhase();
        if (timeoutCallback!=null){
            ActionComposer actionComposer = this.getComposer();
            actionComposer.switchToFocusWindow();
            timeoutCallback.accept(actionComposer);
        }
        else{
            throw new BrowserActionException("Timeout! "+toString());
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
