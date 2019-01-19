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

import java.util.List;
import java.util.function.Consumer;

import org.openqa.selenium.By;

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
    private final List<By> frameBySequence;
    
    /**
     *
     * @param customAction custom action
     * @param frameBySequence the sequence of the frame locating mechanism for the frame where the custom action to be performed against
     * @param actAsSinglePhase flags whether acts as a {@link SinglePhaseAction}
     */
    public Custom(Consumer<ActionComposer> customAction, List<By> frameBySequence, boolean actAsSinglePhase){
        super(null);
        this.customFunc = customAction;
        this.frameBySequence = frameBySequence;
        this.actAsSinglePhase = actAsSinglePhase;
        super.setInternalAction(()->{
            ActionComposer actionComposer = this.getComposer();
            try{
                switchToTopForFirefox(); //firefox doesn't switch focus to top after switch to window, so recovery step is required
                actionComposer.switchToInnerFrame(this.frameBySequence);
                this.customFunc.accept(actionComposer);
                if(this.actAsSinglePhase){
                    noNextPhase();
                }
            }catch(Exception e){
                noNextPhase();
                throw new ActionException(e);
            }
        });
    }
    
    @Override
    public String toString(){
        return Custom.class.getSimpleName();
    }
}