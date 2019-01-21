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

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.StaleElementReferenceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.kquiet.browser.ActionComposer;
import org.kquiet.browser.action.exception.ActionException;

/**
 * {@link Click} is a subclass of {@link MultiPhaseAction} which clicks an element.
 * {@link org.openqa.selenium.StaleElementReferenceException} may happen while {@link Click} tries to manipulate the element, so multi-phase is used to perform the action again.
 * 
 * @author Kimberly
 */
public class Click extends MultiPhaseAction {
    private static final Logger LOGGER = LoggerFactory.getLogger(Click.class);
    
    private final By by;
    private final List<By> frameBySequence;

    /**
     *
     * @param by the element locating mechanism
     * @param frameBySequence the sequence of the frame locating mechanism for the element resides in frame(or frame in another frame and so on)
     */
    public Click(By by, List<By> frameBySequence){
        this.by = by;
        this.frameBySequence = frameBySequence;
    }

    @Override
    protected void perform() {
        ActionComposer actionComposer = this.getComposer();
        try{
            switchToTopForFirefox(); //firefox doesn't switch focus to top after switch to window, so recovery step is required                
            actionComposer.switchToInnerFrame(this.frameBySequence);
            WebElement element = actionComposer.getWebDriver().findElement(this.by);
            element.click();
            noNextPhase();
        }catch(StaleElementReferenceException ignoreE){ //with next phase when StaleElementReferenceException is encountered
            if (LOGGER.isDebugEnabled()) LOGGER.debug("{}({}): encounter stale element:{}", ActionComposer.class.getSimpleName(), actionComposer.getName(), toString(), ignoreE);
        }catch(Exception e){
            noNextPhase();
            throw new ActionException(e);
        }
    }
    
    @Override
    public String toString(){
        return String.format("%s:%s/%s", Click.class.getSimpleName(), by.toString()
                , (frameBySequence!=null?String.join(",",frameBySequence.toString()):""));
    }
}
