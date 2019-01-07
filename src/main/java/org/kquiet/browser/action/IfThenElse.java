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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import org.kquiet.browser.ActionComposer;
import org.kquiet.browser.action.exception.ActionException;

/**
 * {@link IfThenElse} is a subclass of {@link SinglePhaseAction} which performs actions according to the result of a predicate.
 * 
 * {@link IfThenElse} maintains two lists of actions internally:
 * <ul>
 * <li>positive action list - list of actions to perform for positive result</li>
 * <li>negative action list - list of actions to perform for negative result</li>
 * </ul>
 * 
 * @author Kimberly
 */
public class IfThenElse extends SinglePhaseAction implements Aggregatable {
    private final List<MultiPhaseAction> positiveActionList = new ArrayList<>();
    private final List<MultiPhaseAction> negativeActionList = new ArrayList<>();
    private final Predicate<ActionComposer> predicate;
    private volatile boolean predicateResult=true;
    
    /**
     * 
     * @param predicate the predicate to test
     * @param positiveActionList the actions to perform if the result is true
     * @param negativeActionList the actions to perform if the result is false
     */
    public IfThenElse(Predicate<ActionComposer> predicate, List<MultiPhaseAction> positiveActionList, List<MultiPhaseAction> negativeActionList){
        super(null);
        this.predicate = predicate;
        if (positiveActionList!=null) this.positiveActionList.addAll(positiveActionList);
        if (negativeActionList!=null) this.negativeActionList.addAll(negativeActionList);
        
        super.setInternalAction(()->{
            ActionComposer actionComposer = this.getComposer();
            try{
                predicateResult = testPredicate();
                if (predicateResult){
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
                throw new ActionException("Error: "+toString(), e);
            }
        });
    }
    
    private boolean testPredicate() throws Exception{
        ActionComposer actionComposer = this.getComposer();
        AtomicBoolean result = new AtomicBoolean();
        
        Future<Exception> future= getComposer().callBrowser(()->{
            result.set(predicate.test(actionComposer));
        });
        Exception actionException = future.get();
        if (actionException!=null) throw actionException;
        return result.get();
    }
    
    @Override
    public String toString(){
        return String.format("%s(%s) %s", ActionComposer.class.getSimpleName(), getComposer()==null?"":getComposer().getName(), IfThenElse.class.getSimpleName());
    }
    
    @Override
    public boolean isDone(){
        boolean isDoneFlag = super.isDone();
        List<MultiPhaseAction> actionList;
        if (predicateResult){
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