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

import java.util.Arrays;


/**
 * {@link SinglePhaseAction} is a subclass of {@link MultiPhaseAction} which has only one phase.
 * 
 * @author Kimberly
 */
public abstract class SinglePhaseAction extends MultiPhaseAction{
    
    /**
     *
     * @param internalAction the internal action to perform against the browser
     */
    public SinglePhaseAction(Runnable internalAction){
        super(null);
        super.setInternalAction(makeSinglePhaseRunnable(internalAction));
    }
    
    private Runnable makeSinglePhaseRunnable(Runnable action){
        return ()->{
            try{
                action.run();
            }finally{
                noNextPhase();
            }
        };
    }
    
    /**
     * Set the internal action.
     * 
     * @param action the internal action
     * @return invoking {@link SinglePhaseAction}
     */
    @Override
    protected SinglePhaseAction setInternalAction(Runnable action) {
        super.setInternalAction(makeSinglePhaseRunnable(action));
        return this;
    }
    
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
}
