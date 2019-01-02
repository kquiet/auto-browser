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
import org.kquiet.browser.BrowserActionException;

/**
 *
 * @author Kimberly
 */
public class Custom extends OneTimeAction {
    
    /**
     *
     * @param customFunc
     */
    public Custom(Consumer<ActionComposer> customFunc){
        super(null);
        this.setInternalAction(()->{
            ActionComposer actionComposer = this.getComposer();
            try{
                actionComposer.switchToFocusWindow();
                customFunc.accept(actionComposer);
            }catch(Exception e){
                throw new BrowserActionException("Error: "+toString(), e);
            }
        });
    }
    
    @Override
    public String toString(){
        return String.format("%s(%s) %s", ActionComposer.class.getSimpleName(), getComposer()==null?"":getComposer().getName(), Custom.class.getSimpleName());
    }
}