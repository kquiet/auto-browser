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
import java.util.Optional;

import org.kquiet.browser.ActionComposer;
import org.kquiet.browser.action.exception.ExecutionException;

/**
 *
 * @author Kimberly
 */
public class CloseWindow extends OneTimeAction {
    private final boolean closeAllRegistered;
    private final String registeredName;    
                
    /**
     *
     */
    public CloseWindow(){
        this(false, null);
    }
    
    /**
     *
     * @param closeAll
     */
    public CloseWindow(boolean closeAll){
        this(closeAll, null);
    }
    
    /**
     *
     * @param registeredName
     */
    public CloseWindow(String registeredName){
        this(false, registeredName);
    }
    
    private CloseWindow(boolean closeAllRegistered, String registeredName){
        super(null);
        this.closeAllRegistered = closeAllRegistered;
        this.registeredName = registeredName;
        this.setInternalAction(()->{
            ActionComposer actionComposer = getComposer();
            if (!Optional.ofNullable(registeredName).orElse("").isEmpty()){
                String windowIdentity = actionComposer.getRegisteredWindow(registeredName);
                if (windowIdentity.isEmpty()) throw new ExecutionException(String.format("%s(%s) close registered window fail, not found:%s", ActionComposer.class.getSimpleName(), actionComposer.getName(), registeredName));
                else actionComposer.getBrsDriver().close();
            }
            else if (this.closeAllRegistered){
                List<String> windowList = actionComposer.getRegisteredWindows();
                for (String window: windowList){
                    if (actionComposer.switchToWindow(window)){
                        actionComposer.getBrsDriver().close();
                    }
                }
            }
            else{
                if (actionComposer.switchToFocusWindow()) actionComposer.getBrsDriver().close();
                else  throw new ExecutionException(String.format("%s(%s) close focus window fail", ActionComposer.class.getSimpleName(), actionComposer.getName()));
            }
        });
    }
    
    @Override
    public String toString(){
        return String.format("%s(%s) %s:%s/%s", ActionComposer.class.getSimpleName(), getComposer()==null?"":getComposer().getName(), CloseWindow.class.getSimpleName(), String.valueOf(closeAllRegistered), registeredName);
    }
}