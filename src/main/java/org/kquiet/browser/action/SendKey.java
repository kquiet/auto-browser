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
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.kquiet.browser.ActionComposer;
import org.kquiet.browser.action.exception.ActionException;

/**
 * {@link SendKey} is a subclass of {@link MultiPhaseAction} which types into an element.
 * {@link org.openqa.selenium.StaleElementReferenceException} may happen while {@link Select} tries to manipulate the element, so multi-phase is used to perform the action again.
 * 
 * @author Kimberly
 */
public class SendKey extends MultiPhaseAction {
    private static final Logger LOGGER = LoggerFactory.getLogger(SendKey.class);
    
    private final By by;
    private final List<By> frameBySequence;
    private final CharSequence[] keysToSend;
    private final boolean clearBeforeSend;
    
    /**
     *
     * @param by by the element locating mechanism
     * @param frameBySequence the sequence of the frame locating mechanism for the element resides in frame(or frame in another frame and so on)
     * @param clearBeforeSend {@code true}: clear before sending; {@code false}: send without clearing first
     * @param keysToSend character sequence to send to the element
     */
    public SendKey(By by, List<By> frameBySequence, boolean clearBeforeSend, CharSequence... keysToSend){
        super(null);
        this.by = by;
        this.frameBySequence = frameBySequence;
        this.clearBeforeSend = clearBeforeSend;
        this.keysToSend = purifyCharSequences(keysToSend);
        super.setInternalAction(()->{
            ActionComposer actionComposer = this.getComposer();
            try{
                switchToInnerFrame(frameBySequence);
                WebElement element = actionComposer.getWebDriver().findElement(this.by);
                try{
                    clickToSendKeys(element, this.clearBeforeSend, this.keysToSend);
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
    
    private CharSequence[] purifyCharSequences(CharSequence... sequence){
        if (sequence==null){
            sequence = new CharSequence[]{};
        }
        else{
            for (int i=0;i<sequence.length;i++){
                if (sequence[i]==null){
                    sequence[i]="";
                }
            }
        }
        return sequence;
    }
    
    /**
     * Clicks an element and then types into it.
     * 
     * @param element the element to send keys to
     * @param clearBeforeSend {@code true}: clear before sending; {@code false}: send without clearing first
     * @param keysToSend character sequence to send to the element
     */
    public static void clickToSendKeys(WebElement element, boolean clearBeforeSend, CharSequence... keysToSend){
        //click before send key
        element.click();
        if (clearBeforeSend) element.sendKeys(Keys.chord(Keys.CONTROL+"a"));
        else element.sendKeys(Keys.chord(Keys.CONTROL, Keys.END));
        element.sendKeys(keysToSend);
    }
    
    @Override
    public String toString(){
        return String.format("%s(%s) %s:%s/%s/%s/%s"
                , ActionComposer.class.getSimpleName(), getComposer()==null?"":getComposer().getName()
                , SendKey.class.getSimpleName(), by.toString(), String.join(",", keysToSend), (frameBySequence!=null?String.join(",",frameBySequence.toString()):""), String.valueOf(clearBeforeSend));
    }
}