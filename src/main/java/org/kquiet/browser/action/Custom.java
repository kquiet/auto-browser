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

import java.util.function.Consumer;

import org.kquiet.browser.ActionComposer;
import org.kquiet.browser.action.exception.ActionException;

/**
 * {@link Custom} is a subclass of {@link MultiPhaseAction} which performs custom action by phases to avoid blocking the execution of other browser actions.
 * 
 * <p>There is a boolean parameter {@code actAsSinglePhase} in constructor {@link #Custom(java.util.function.Consumer, boolean)},
 * which is used to make {@link Custom} acts as a {@link SinglePhaseAction}. If this is not the case,
 * {@link MultiPhaseAction#noNextPhase()} must be called explicitly before ending custom action.</p>
 *
 * @author Kimberly
 */
public class Custom extends MultiPhaseAction {
    private final Consumer<ActionComposer> customFunc;
    private final boolean actAsSinglePhase;
    
    /**
     *
     * @param customAction custom action
     * @param actAsSinglePhase flags whether acts as a {@link SinglePhaseAction}
     */
    public Custom(Consumer<ActionComposer> customAction, boolean actAsSinglePhase){
        super(null);
        this.customFunc = customAction;
        this.actAsSinglePhase = actAsSinglePhase;
        super.setInternalAction(()->{
            try{
                ActionComposer actionComposer = this.getComposer();
                this.customFunc.accept(actionComposer);
            }catch(Exception e){
                throw new ActionException(e);
            }finally{
                if(this.actAsSinglePhase){
                    noNextPhase();
                }
            }
        });
    }
    
    @Override
    public String toString(){
        return Custom.class.getSimpleName();
    }
}