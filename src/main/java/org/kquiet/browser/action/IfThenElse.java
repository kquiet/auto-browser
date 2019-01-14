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

import java.util.function.Function;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.openqa.selenium.StaleElementReferenceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.kquiet.browser.ActionComposer;
import org.kquiet.browser.action.exception.ActionException;

/**
 * {@link IfThenElse} is a subclass of {@link SinglePhaseAction} which performs actions according to the evaluation result of specified function.
 * If this function returns something different from null or false then the result is positive; otherwise negative.
 * 
 * {@link IfThenElse} maintains two lists of actions internally:
 * <ul>
 * <li>positive action list - list of actions to perform for positive result</li>
 * <li>negative action list - list of actions to perform for negative result</li>
 * </ul>
 * 
 * @author Kimberly
 */
@Nonbrowserable
public class IfThenElse extends SinglePhaseAction{
    private static final Logger LOGGER = LoggerFactory.getLogger(IfThenElse.class);
    
    private final List<MultiPhaseAction> positiveActionList = new ArrayList<>();
    private final List<MultiPhaseAction> negativeActionList = new ArrayList<>();
    private final Function<ActionComposer, ?> evalFunction;
    private volatile boolean evaluationResult=true;
    
    /**
     * 
     * @param evalFunction the function to evaluate
     * @param positiveActionList the actions to perform if the result is true
     * @param negativeActionList the actions to perform if the result is false
     */
    public IfThenElse(Function<ActionComposer, ?> evalFunction, List<MultiPhaseAction> positiveActionList, List<MultiPhaseAction> negativeActionList){
        super(null);
        this.evalFunction = evalFunction;
        if (positiveActionList!=null) this.positiveActionList.addAll(positiveActionList);
        if (negativeActionList!=null) this.negativeActionList.addAll(negativeActionList);
        
        super.setInternalAction(()->{
            ActionComposer actionComposer = this.getComposer();
            try{
                evaluationResult = evaluate();
                if (evaluationResult){
                    this.positiveActionList.forEach((action) -> {
                        action.run();
                    });
                }
                else{
                    this.negativeActionList.forEach((action) -> {
                        action.run();
                    });
                }
            }catch(Exception e){
                throw new ActionException(e);
            }
        });
    }
    
    private boolean evaluate() throws Exception{
        AtomicBoolean evalResult = new AtomicBoolean(true);
        AtomicReference<MultiPhaseAction> customRef = new AtomicReference<>(null);
        MultiPhaseAction customAction = new Custom(ac->{
            Object obj =null;
            try{
                obj = evalFunction.apply(ac);
                customRef.get().noNextPhase();
            }catch(StaleElementReferenceException ignoreE){
                if (LOGGER.isDebugEnabled()) LOGGER.debug("{}({}): encounter stale element:{}", ActionComposer.class.getSimpleName(), ac.getName(), customRef.get().toString(), ignoreE);
            }catch(Exception e){
                customRef.get().noNextPhase();
                throw new ActionException(e);
            }
            evalResult.set(obj!=null && (Boolean.class!=obj.getClass() || Boolean.TRUE.equals(obj)));
        }, false).setContainingComposer(getComposer());
        customRef.set(customAction);
        customAction.run();

        List<Exception> errors = customAction.getErrors();
        if (!errors.isEmpty()) throw errors.get(errors.size()-1);
        return evalResult.get();
    }
    
    @Override
    public String toString(){
        return IfThenElse.class.getSimpleName();
    }
    
    @Override
    public boolean isDone(){
        boolean isDoneFlag = super.isDone();
        List<MultiPhaseAction> actionList;
        if (evaluationResult){
            actionList = positiveActionList;
        }
        else {
            actionList = negativeActionList;
        }
        for (MultiPhaseAction action: actionList){
            isDoneFlag = isDoneFlag && action.isDone() && !action.hasNextPhase();
            if (!isDoneFlag) break;
        }
        return isDoneFlag;
    }
    
    @Override
    public boolean isFail(){
        return super.isFail() || positiveActionList.stream().anyMatch(s->s.isFail()) || negativeActionList.stream().anyMatch(s->s.isFail());
    }
    
    @Override
    public List<Exception> getErrors() {
        List<Exception> errList = new ArrayList<>(super.getErrors());
        positiveActionList.forEach(action->{
            errList.addAll(action.getErrors());
        });
        negativeActionList.forEach(action->{
            errList.addAll(action.getErrors());
        });
        return errList;
    }
    
    @Override
    public IfThenElse setContainingComposer(ActionComposer containingComposer) {
        super.setContainingComposer(containingComposer);
        positiveActionList.forEach(action->{
            action.setContainingComposer(containingComposer);
        });
        negativeActionList.forEach(action->{
            action.setContainingComposer(containingComposer);
        });
        return this;
    }
}