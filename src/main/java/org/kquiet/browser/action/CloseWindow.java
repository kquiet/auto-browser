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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.kquiet.browser.ActionComposer;
import org.kquiet.browser.action.exception.ExecutionException;

/**
 * {@link CloseWindow} is a subclass of {@link OneTimeAction} which closes window(s).
 * 
 * @author Kimberly
 */
public class CloseWindow extends OneTimeAction {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloseWindow.class);
    
    private final boolean closeAllRegistered;
                
    /**
     *
     * @param closeAllRegistered {@code true}: close all regisetered windows; {@code false}: close only the focus window
     */
    public CloseWindow(boolean closeAllRegistered){
        super(null);
        this.closeAllRegistered = closeAllRegistered;
        super.setInternalAction(()->{
            ActionComposer actionComposer = getComposer();
            //root window should never be closed
            String rootWindow = actionComposer.getRootWindow();
            if (this.closeAllRegistered){
                List<String> windowList = actionComposer.getRegisteredWindows();
                List<String> failList = new ArrayList<>();
                for (String window: windowList){
                    if (!window.equals(rootWindow)){
                        if (actionComposer.switchToWindow(window)){
                            actionComposer.getBrsDriver().close();
                        }
                        else failList.add(window);
                    }
                    else{
                        LOGGER.info("{}({}): root window({}) can't be closed", ActionComposer.class.getSimpleName(), actionComposer.getName(), window);
                    }
                }
                if (!failList.isEmpty()) throw new ExecutionException(String.format("%s(%s) close registered windows(%s) fail; it may have been closed or equal to the root window", ActionComposer.class.getSimpleName(), actionComposer.getName(), String.join(",", failList)));
            }
            else{
                String focusWindow = actionComposer.getFocusWindow();
                if (!focusWindow.equals(rootWindow) && actionComposer.switchToWindow(focusWindow)) actionComposer.getBrsDriver().close();
                else throw new ExecutionException(String.format("%s(%s) close focus window(%s) fail; it may have been closed or is the root window", ActionComposer.class.getSimpleName(), actionComposer.getName(), focusWindow));
            }
        });
    }
    
    @Override
    public String toString(){
        return String.format("%s(%s) %s:%s", ActionComposer.class.getSimpleName(), getComposer()==null?"":getComposer().getName(), CloseWindow.class.getSimpleName(), String.valueOf(closeAllRegistered));
    }
}