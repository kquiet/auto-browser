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
import java.util.function.Function;

import org.openqa.selenium.By;

import org.kquiet.browser.ActionComposer;
import org.kquiet.browser.action.exception.ActionException;

/**
 * {@link Custom} is a subclass of {@link MultiPhaseAction} which performs custom action by phases to avoid blocking the execution of other browser actions.
 * 
 * <p>There are two constructors of {@link Custom}:
 * <ol>
 * <li>{@link Custom#Custom(java.util.function.Function, java.util.List) } is used to make {@link Custom} act as a {@link MultiPhaseAction},
 * therefore {@link PhaseStoppable#noNextPhase()} must be called explicitly before ending custom action.</li>
 * <li>{@link Custom#Custom(java.util.function.Consumer, java.util.List) } is used to make {@link Custom} act as a {@link SinglePhaseAction}.</li>
 * </ol>
 *
 * @author Kimberly
 */
public class Custom extends MultiPhaseAction {
    private final Consumer<ActionComposer> customFunc;
    private final boolean actAsSinglePhase;
    private final List<By> frameBySequence;
    
    /**
     * Create a {@link Custom} acting like a {@link MultiPhaseAction}.
     * 
     * @param customAction phase-stoppable custom action
     * @param frameBySequence the sequence of the frame locating mechanism for the frame where the custom action to be performed against
     */    
    public Custom(Function<PhaseStoppable, Consumer<ActionComposer>> customAction, List<By> frameBySequence){
        super(null);
        this.customFunc = customAction.apply((PhaseStoppable)this);
        this.frameBySequence = frameBySequence;
        this.actAsSinglePhase = false;
        super.setInternalAction(multiPhaseCustom());
    }
    
    /**
     * Create a {@link Custom} acting like a {@link SinglePhaseAction}.
     * 
     * @param customAction custom action
     * @param frameBySequence the sequence of the frame locating mechanism for the frame where the custom action to be performed against
     */
    public Custom(Consumer<ActionComposer> customAction, List<By> frameBySequence){
        super(null);
        this.customFunc = customAction;
        this.frameBySequence = frameBySequence;
        this.actAsSinglePhase = true;
        super.setInternalAction(multiPhaseCustom());
    }
    
    private Runnable multiPhaseCustom(){
        return ()->{
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
        };
    }
    
    @Override
    public String toString(){
        return String.format("%s(%s):%s", Custom.class.getSimpleName(), String.valueOf(actAsSinglePhase), (frameBySequence!=null?String.join(",",frameBySequence.toString()):""));
    }
}