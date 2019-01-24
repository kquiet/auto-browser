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

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.kquiet.browser.ActionComposer;
import org.kquiet.browser.action.exception.ActionException;

/**
 * {@link Upload} is a subclass of {@link MultiPhaseAction} which types path of file into the file upload element.
 * {@link org.openqa.selenium.StaleElementReferenceException} may happen while {@link Select} tries to manipulate the element, so multi-phase is used to perform the action again.
 * 
 * <p>{@link Upload} needs to make the file upload element visible first, so below javascript will be executed before typing into it:</p>
 * <pre>
 * fileUploadElement.style.display = '';
 * fileUploadElement.style.visibility = 'visible';
 * fileUploadElement.style.height = '1px';
 * fileUploadElement.style.width = '1px';
 * fileUploadElement.style.opacity = 1;
 * </pre>
 * 
 * @author Kimberly
 */
public class Upload extends MultiPhaseAction {
    private static final Logger LOGGER = LoggerFactory.getLogger(Upload.class);
    
    private final By by;
    private final List<By> frameBySequence;
    private final String pathOfFile;

    /**
     *
     * @param by the element locating mechanism
     * @param frameBySequence the sequence of the frame locating mechanism for the element resides in frame(or frame in another frame and so on)
     * @param pathOfFile the path of file to upload
     */
    public Upload(By by, List<By> frameBySequence, String pathOfFile){
        this.by = by;
        this.frameBySequence = frameBySequence;
        this.pathOfFile = pathOfFile;
    }

    @Override
    protected void perform() {
        ActionComposer actionComposer = this.getComposer();
        try{
            switchToTopForFirefox(); //firefox doesn't switch focus to top after switch to window, so recovery step is required
            actionComposer.switchToInnerFrame(this.frameBySequence);
            WebElement element = actionComposer.getWebDriver().findElement(this.by);
            ((JavascriptExecutor)actionComposer.getWebDriver()).executeScript("arguments[0].style.display = 'inline-block'; arguments[0].style.visibility = 'visible'; arguments[0].style.height = '1px'; arguments[0].style.width = '1px'; arguments[0].style.opacity = 1;", element);
            if (element.isDisplayed() && element.isEnabled()){
                element.sendKeys(this.pathOfFile);
                noNextPhase();
            }
            else{
                if (LOGGER.isDebugEnabled()) LOGGER.debug("{}({}): continue to wait upload element to be clickable:{}", ActionComposer.class.getSimpleName(), actionComposer.getName(), toString());
            }
        }catch(StaleElementReferenceException ignoreE){ //with next phase when StaleElementReferenceException is encountered
            if (LOGGER.isDebugEnabled()) LOGGER.debug("{}({}): encounter stale element:{}", ActionComposer.class.getSimpleName(), actionComposer.getName(), toString(), ignoreE);
        }catch(Exception e){
            noNextPhase();
            throw new ActionException(toString(), e);
        }
    }
    
    @Override
    public String toString(){
        return String.format("%s:%s/%s/%s", Upload.class.getSimpleName(), by.toString()
                , (frameBySequence!=null?String.join(",",frameBySequence.toString()):""), pathOfFile);
    }
}
