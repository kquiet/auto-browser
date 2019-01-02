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

import java.util.LinkedHashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.JavascriptExecutor;

import org.kquiet.browser.ActionComposer;
import org.kquiet.browser.action.exception.ExecutionException;

/**
 *
 * @author Kimberly
 */
public class OpenWindow extends OneTimeAction {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenWindow.class);
    
    private final boolean asComposerFocusWindow;
    private final String registerName;
    
    /**
     *
     * @param asComposerFocusWindow
     * @param registerName
     */
    public OpenWindow(boolean asComposerFocusWindow, String registerName){
        super(null);
        this.asComposerFocusWindow = asComposerFocusWindow;
        this.registerName = registerName;
        this.setInternalAction(()->{
            ActionComposer actionComposer = this.getComposer();
            WebDriver brsDriver = actionComposer.getBrsDriver();
            //find existing windows before open new one
            LinkedHashSet<String> beforeWindowSet = new LinkedHashSet<>();
            for(String winHandle: brsDriver.getWindowHandles()){
                beforeWindowSet.add(winHandle);
            }
            final String initWindow = beforeWindowSet.stream().findFirst().orElse(null);
            try{
                ((JavascriptExecutor)actionComposer.getBrsDriver().switchTo().window(initWindow)).executeScript("window.open('about:blank','_blank');");
            }catch(Exception ex){
                LOGGER.warn("[{}] open new window script error!", actionComposer.getName(), ex);
            }

            String actualHandle=null;
            LinkedHashSet<String> afterWindowSet = new LinkedHashSet<>();
            for(String winHandle: brsDriver.getWindowHandles()){
                afterWindowSet.add(winHandle);
            }
            for(String winHandle: afterWindowSet){
                //got new window
                if (!winHandle.equals(initWindow) && !beforeWindowSet.contains(winHandle)){
                    actualHandle=winHandle;
                    if (this.asComposerFocusWindow){
                        actionComposer.setFocusWindow(actualHandle);
                    }
                    if (!actionComposer.registerWindow(this.registerName, actualHandle)){
                        throw new ExecutionException(String.format("%s(%s) can't register new window:%s %s", ActionComposer.class.getSimpleName(), actionComposer.getName(), this.registerName, toString()));
                    }
                    break;
                }
            }

            if (actualHandle==null){
                throw new ExecutionException(String.format("%s(%s) can't find new window! %s", ActionComposer.class.getSimpleName(), actionComposer.getName(), toString()));
            }
        });
    }
    
    @Override
    public String toString(){
        return String.format("%s(%s) %s:%s/%s", ActionComposer.class.getSimpleName(), getComposer()==null?"":getComposer().getName(), OpenWindow.class.getSimpleName(), String.valueOf(asComposerFocusWindow), registerName);
    }
}