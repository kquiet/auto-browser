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

import org.kquiet.browser.ActionState;

/**
 *
 * @author Kimberly
 */
public class OneTimeAction extends MultiPhaseAction{
    
    /**
     *
     * @param action
     */
    public OneTimeAction(Runnable action){
        super(null);
        super.setInternalAction(makeOnetimeRunnable(action));
    }
    
    private Runnable makeOnetimeRunnable(Runnable action){
        return ()->{
            try{
                action.run();
            }finally{
                unregisterNextPhase();
            }
        };
    }
    
    /**
     *
     * @param action
     */
    @Override
    public void setInternalAction(Runnable action) {
        this.internalAction = makeOnetimeRunnable(action);
    }
    
    /**
     *
     * @return
     */
    @Override
    public boolean isDone(){
        return Arrays.asList(ActionState.CompleteWithError, ActionState.Complete).contains(actionState);
    }
}
