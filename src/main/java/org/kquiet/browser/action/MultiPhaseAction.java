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
import java.util.concurrent.Future;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import org.kquiet.utility.StopWatch;
import org.kquiet.browser.ActionComposer;
import org.kquiet.browser.ActionState;

/**
 *
 * @author Kimberly
 */
public class MultiPhaseAction implements Runnable{
    private static final Logger LOGGER = LoggerFactory.getLogger(MultiPhaseAction.class);
    
    /**
     *
     */
    protected ActionComposer containingComposer;

    /**
     *
     */
    protected Runnable internalAction;

    /**
     *
     */
    protected volatile ActionState actionState = ActionState.CREATED;

    /**
     *
     */
    protected final List<Exception> errorList = new ArrayList<>();

    /**
     *
     */
    protected final StopWatch stopWatch = new StopWatch();

    //flags whether registered for next phase or not

    /**
     *
     */
    protected volatile boolean registerNextPhase = true;

    /**
     *
     * @param action
     */
    protected MultiPhaseAction(Runnable action){
        this.internalAction = action;
    }
    
    /**
     *
     */
    public void registerNextPhase(){
        this.registerNextPhase = true;
    }
    
    /**
     *
     */
    public void unregisterNextPhase(){
        this.registerNextPhase = false;
    }
    
    /**
     *
     * @return
     */
    public boolean hasNextPhase(){
        return registerNextPhase;
    }
    
    @Override
    public void run(){
        while(hasNextPhase()){
            try{
                stopWatch.start();
                actionState = ActionState.RUNNING;

                //only browserable action is gonna run at browser to avoid blocking browser unnecessarily
                if (this instanceof Aggregatable || this instanceof Nonbrowserable){
                    getInternalAction().run();
                }
                else{
                    Future<Exception> future= getComposer().callBrowser(getInternalAction());
                    Exception actionException = future.get();
                    if (actionException!=null) throw actionException;
                }

                if (hasNextPhase()) actionState = ActionState.COMPLETE_WITH_NEXT_PHASE;
                else actionState = ActionState.COMPLETE;
            }catch(Exception e){
                actionState = ActionState.COMPLETE_WITH_ERROR;
                errorList.add(e);
                LOGGER.warn("{} fail", getClass().getSimpleName(), e);
            }finally{
                stopWatch.stop();
            }
        }
    }
    
    /**
     *
     * @return
     */
    public boolean isDone(){
        return Arrays.asList(ActionState.COMPLETE_WITH_ERROR, ActionState.COMPLETE, ActionState.COMPLETE_WITH_NEXT_PHASE).contains(actionState);
    }
    
    /**
     *
     * @return
     */
    public boolean isFail(){
        return actionState==ActionState.COMPLETE_WITH_ERROR;
    }

    /**
     *
     * @return
     */
    protected Runnable getInternalAction() {
        return internalAction;
    }

    /**
     *
     * @param runnable
     */
    public void setInternalAction(Runnable runnable) {
        this.internalAction = runnable;
    }
    
    /**
     *
     * @return
     */
    public List<Exception> getErrors() {
        return errorList;
    }
    
    @Override
    public String toString(){
        return getClass().getSimpleName();
    }

    /**
     *
     * @return
     */
    public Duration getCostTime() {
        return stopWatch.getDuration();
    }

    /**
     * @return the containingComposer
     */
    protected ActionComposer getComposer() {
        return containingComposer;
    }

    /**
     * @param containingComposer the containingComposer to set
     */
    public void setContainingComposer(ActionComposer containingComposer) {
        this.containingComposer = containingComposer;
    }
}
