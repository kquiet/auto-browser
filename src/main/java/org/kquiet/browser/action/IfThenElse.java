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
import java.util.function.Predicate;

import org.kquiet.browser.ActionComposer;
import org.kquiet.browser.action.exception.ExecutionException;

/**
 *
 * @author Kimberly
 */
public class IfThenElse extends OneTimeAction implements Aggregatable {
    private final List<MultiPhaseAction> thenActionList = new ArrayList<>();
    private final List<MultiPhaseAction> elseActionList = new ArrayList<>();
    private volatile boolean predicateResult=true;
    
    /**
     *
     * @param predicate
     * @param thenActionList
     * @param elseActionList
     */
    public IfThenElse(Predicate<ActionComposer> predicate, List<MultiPhaseAction> thenActionList, List<MultiPhaseAction> elseActionList){
        super(null);
        if (thenActionList!=null) this.thenActionList.addAll(thenActionList);
        if (elseActionList!=null) this.elseActionList.addAll(elseActionList);
        
        this.setInternalAction(()->{
            ActionComposer actionComposer = this.getComposer();
            try{
                predicateResult = predicate.test(actionComposer);
                if (predicateResult){
                    this.thenActionList.forEach((action) -> {
                        action.run();
                    });
                }
                else{
                    this.elseActionList.forEach((action) -> {
                        action.run();
                    });
                }
            }catch(Exception e){
                throw new ExecutionException("Error: "+toString(), e);
            }
        });
    }
    
    @Override
    public String toString(){
        return String.format("%s(%s) %s", ActionComposer.class.getSimpleName(), getComposer()==null?"":getComposer().getName(), IfThenElse.class.getSimpleName());
    }
    
    @Override
    public boolean isDone(){
        boolean isDoneFlag = super.isDone();
        if (predicateResult){
            for (MultiPhaseAction action: thenActionList){
                isDoneFlag = isDoneFlag && action.isDone() && !action.hasNextPhase();
                if (!isDoneFlag) break;
            }
        }
        else {
            for (MultiPhaseAction action: elseActionList){
                isDoneFlag = isDoneFlag && action.isDone() && !action.hasNextPhase();
                if (!isDoneFlag) break;
            }
        }
        return isDoneFlag;
    }
    
    @Override
    public boolean isFail(){
        return super.isFail() || thenActionList.stream().anyMatch(s->s.isFail()) || elseActionList.stream().anyMatch(s->s.isFail());
    }
    
    @Override
    public List<Exception> getErrors() {
        List<Exception> errList = new ArrayList<>(this.errorList);
        thenActionList.forEach(action->{
            errList.addAll(action.getErrors());
        });
        elseActionList.forEach(action->{
            errList.addAll(action.getErrors());
        });
        return errList;
    }
    
    @Override
    public IfThenElse setContainingComposer(ActionComposer containingComposer) {
        super.setContainingComposer(containingComposer);
        thenActionList.forEach(action->{
            action.setContainingComposer(containingComposer);
        });
        elseActionList.forEach(action->{
            action.setContainingComposer(containingComposer);
        });
        return this;
    }
}