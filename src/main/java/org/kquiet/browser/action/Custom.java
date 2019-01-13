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
 * {@link Custom} is a subclass of {@link SinglePhaseAction} which performs custom action
 * 
 * @author Kimberly
 */
public class Custom extends SinglePhaseAction {
    private final Consumer<ActionComposer> customFunc;
    
    /**
     *
     * @param customAction custom action
     */
    public Custom(Consumer<ActionComposer> customAction){
        super(null);
        this.customFunc = customAction;
        super.setInternalAction(()->{
            ActionComposer actionComposer = this.getComposer();
            try{
                this.customFunc.accept(actionComposer);
            }catch(Exception e){
                throw new ActionException(e);
            }
        });
    }
    
    @Override
    public String toString(){
        return Custom.class.getSimpleName();
    }
}