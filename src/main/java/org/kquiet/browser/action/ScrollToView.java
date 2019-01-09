/*
 * Copyright 2019 kquiet.
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

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.kquiet.browser.ActionComposer;
import org.kquiet.browser.action.exception.ActionException;

/**
 * {@link ScrollToView} is a subclass of {@link MultiPhaseAction} which scrolls an element into visible area of the browser window.
 * {@link org.openqa.selenium.StaleElementReferenceException} may happen while {@link ScrollToView} tries to manipulate the element, so multi-phase is used to perform the action again.
 * 
 * @author Kimberly
 */
public class ScrollToView extends MultiPhaseAction {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScrollToView.class);
    
    private final By by;
    private final By frameBy;
    private final boolean toTop;

    /**
     *
     * @param by the element locating mechanism
     * @param frameBy the frame locating mechanism for the element resides in a frame
     * @param toTop {@code true}: scroll to top;{@code false}: scroll to bottom
     */
    public ScrollToView(By by, By frameBy, boolean toTop){
        super(null);
        this.by = by;
        this.frameBy = frameBy;
        this.toTop = toTop;
        super.setInternalAction(()->{
            ActionComposer actionComposer = this.getComposer();
            try{
                actionComposer.switchToFocusWindow();
                if (this.frameBy!=null){
                    actionComposer.getBrsDriver().switchTo().frame(actionComposer.getBrsDriver().findElement(this.frameBy));
                }
                WebElement element = actionComposer.getBrsDriver().findElement(this.by);
                try{
                    ((JavascriptExecutor) actionComposer.getBrsDriver()).executeScript("arguments[0].scrollIntoView(arguments[1]);", element, this.toTop);
                    noNextPhase();
                }catch(StaleElementReferenceException ignoreE){ //with next phase when StaleElementReferenceException is encountered
                    if (LOGGER.isDebugEnabled()) LOGGER.debug("{}:{}", StaleElementReferenceException.class.getSimpleName(), toString(), ignoreE);
                }
            }catch(Exception e){
                noNextPhase();
                throw new ActionException("Error: "+toString(), e);
            }
        });
    }
    
    @Override
    public String toString(){
        return String.format("%s(%s) %s:%s/%s/%s", ActionComposer.class.getSimpleName()
                , getComposer()==null?"":getComposer().getName(), ScrollToView.class.getSimpleName(), by.toString()
                , (frameBy!=null?frameBy.toString():""), String.valueOf(toTop));
    }
}
