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
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.kquiet.browser.ActionComposer;
import org.kquiet.browser.action.exception.ActionException;

/**
 * {@link CloseWindow} is a subclass of {@link SinglePhaseAction} which closes window(s).
 * 
 * @author Kimberly
 */
public class CloseWindow extends SinglePhaseAction {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloseWindow.class);
    
    private final boolean closeAllRegistered;
                
    /**
     *
     * @param closeAllRegistered {@code true}: close all regisetered windows; {@code false}: close only the focus window
     */
    public CloseWindow(boolean closeAllRegistered){
        this.closeAllRegistered = closeAllRegistered;
    }

    @Override
    protected void performSinglePhase() {
        ActionComposer actionComposer = getComposer();
        //root window should never be closed
        String rootWindow = actionComposer.getRootWindow();
        if (this.closeAllRegistered){
            List<String> windowList = actionComposer.getRegisteredWindows().values().stream().distinct().collect(Collectors.toList());
            List<String> failList = new ArrayList<>();
            for (String window: windowList){
                if (!window.equals(rootWindow)){
                    if (actionComposer.switchToWindow(window)){
                        actionComposer.getWebDriver().close();
                    }
                    else failList.add(window);
                }
                else{
                    if (LOGGER.isDebugEnabled()) LOGGER.debug("{}({}): root window({}) can't be closed:{}", ActionComposer.class.getSimpleName(), actionComposer.getName(), toString(), window);
                }
            }
            if (!failList.isEmpty()) throw new ActionException(String.format("close registered windows(%s) fail; it may have been closed or equal to the root window", String.join(",", failList)));
        }
        else{
            String focusWindow = actionComposer.getFocusWindow();
            if (!focusWindow.equals(rootWindow) && actionComposer.switchToWindow(focusWindow)) actionComposer.getWebDriver().close();
            else throw new ActionException(String.format("close focus window(%s) fail; it may have been closed or is the root window", focusWindow));
        }
    }
    
    @Override
    public String toString(){
        return String.format("%s:%s", CloseWindow.class.getSimpleName(), String.valueOf(closeAllRegistered));
    }
}