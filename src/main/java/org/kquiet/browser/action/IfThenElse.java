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
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.kquiet.browser.ActionComposer;
import org.kquiet.browser.ActionSequenceContainer;
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
public class IfThenElse extends SinglePhaseAction implements ActionSequenceContainer{
    private static final Logger LOGGER = LoggerFactory.getLogger(IfThenElse.class);
    
    private final Deque<Composable> positiveActionList = new LinkedList<>();
    private final Deque<Composable> negativeActionList = new LinkedList<>();
    private final Function<ActionComposer, ?> evalFunction;
    private volatile Boolean evaluationResult=null;
    
    /**
     * 
     * @param evalFunction the function to evaluate
     * @param positiveActionList the actions to perform if the result is true
     * @param negativeActionList the actions to perform if the result is false
     */
    public IfThenElse(Function<ActionComposer, ?> evalFunction, List<Composable> positiveActionList, List<Composable> negativeActionList){
        this.evalFunction = evalFunction;
        if (positiveActionList!=null) this.positiveActionList.addAll(positiveActionList);
        if (negativeActionList!=null) this.negativeActionList.addAll(negativeActionList);
    }
    
    private boolean evaluate() throws Exception{
        AtomicBoolean evalResult = new AtomicBoolean(true);
        AtomicReference<MultiPhaseAction> customRef = new AtomicReference<>(null);
        MultiPhaseAction customAction = new Custom(ps->ac->{
            Object obj =null;
            try{
                obj = evalFunction.apply(ac);
                ps.noNextPhase();
            }catch(StaleElementReferenceException ignoreE){
                if (LOGGER.isDebugEnabled()) LOGGER.debug("{}({}): encounter stale element:{}", ActionComposer.class.getSimpleName(), ac.getName(), ps, ignoreE);
            }catch(NoSuchElementException skipE){
                if (LOGGER.isDebugEnabled()) LOGGER.debug("{}({}): no such element:{}", ActionComposer.class.getSimpleName(), ac.getName(), ps, skipE);
                obj = null;
                ps.noNextPhase();
            }catch(Exception e){
                ps.noNextPhase();
                throw new ActionException(e);
            }
            evalResult.set(obj!=null && (Boolean.class!=obj.getClass() || Boolean.TRUE.equals(obj)));
        }, null);
        customRef.set(customAction);
        getComposer().perform(customAction);

        List<Exception> errors = customAction.getErrors();
        if (!errors.isEmpty()) throw errors.get(errors.size()-1);
        return evalResult.get();
    }
    
    @Override
    protected void performSinglePhase() {
        try{
            evaluationResult = evaluate();
            Deque<Composable> actualActionList = evaluationResult?positiveActionList:negativeActionList;
            List<Composable> temp = new ArrayList<>(actualActionList);
            boolean anyActionFail = false;
            int index=0;
            while(index<temp.size()){
                Composable action = temp.get(index);
                getComposer().perform(action);
                anyActionFail = anyActionFail || action.isFail();
                if (anyActionFail) break;
                temp = new ArrayList<>(actualActionList);
                index++;
            }
        }catch(Exception e){
            throw new ActionException(e);
        }
    }
    
    @Override
    public String toString(){
        return IfThenElse.class.getSimpleName();
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

    /**
     * Add action to the head of positive or negative list. It depends on the evaluation result and does nothing before evaulation.
     */
    @Override
    public ActionSequenceContainer addToHead(Composable action) {
        if (action!=null && evaluationResult!=null){
            Deque<Composable> actualActionList = evaluationResult?positiveActionList:negativeActionList;
            synchronized(actualActionList){
                actualActionList.addFirst(action);
            }
        }
        return this;
    }

    /**
     * Add action to the tail of positive or negative list. It depends on the evaluation result and does nothing before evaulation.
     */
    @Override
    public ActionSequenceContainer addToTail(Composable action) {
        if (action!=null && evaluationResult!=null){
            Deque<Composable> actualActionList = evaluationResult?positiveActionList:negativeActionList;
            synchronized(actualActionList){
                actualActionList.addLast(action);
            }
        }
        return this;
    }

    /**
     * Add action to the the specified position of positive or negative list. It depends on the evaluation result and does nothing before evaulation.
     */
    @Override
    public ActionSequenceContainer addToPosition(Composable action, int position) {
        if (action!=null && evaluationResult!=null){
            Deque<Composable> actualActionList = evaluationResult?positiveActionList:negativeActionList;
            synchronized(actualActionList){
                List<Composable> temp = new ArrayList<>(actualActionList);
                temp.add(position, action);
                actualActionList.clear();
                actualActionList.addAll(temp);
            }
        }
        return this;
    }
}