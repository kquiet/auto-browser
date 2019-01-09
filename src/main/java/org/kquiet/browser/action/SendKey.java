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

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.kquiet.browser.ActionComposer;
import org.kquiet.browser.action.exception.ActionException;

/**
 * {@link SendKey} is a subclass of {@link SinglePhaseAction} which types into an element.
 * 
 * @author Kimberly
 */
public class SendKey extends SinglePhaseAction {
    private static final Logger LOGGER = LoggerFactory.getLogger(SendKey.class);
    
    private final By by;
    private final By frameBy;
    private final CharSequence[] keysToSend;
    private final boolean clearBeforeSend;
    
    /**
     *
     * @param by by the element locating mechanism
     * @param frameBy the frame locating mechanism for the element resides in a frame
     * @param clearBeforeSend {@code true}: clear before sending; {@code false}: send without clearing first
     * @param keysToSend character sequence to send to the element
     */
    public SendKey(By by, By frameBy, boolean clearBeforeSend, CharSequence... keysToSend){
        super(null);
        this.by = by;
        this.frameBy = frameBy;
        this.clearBeforeSend = clearBeforeSend;
        this.keysToSend = purifyCharSequences(keysToSend);
        super.setInternalAction(()->{
            ActionComposer actionComposer = this.getComposer();
            try{
                actionComposer.switchToFocusWindow();
                if (this.frameBy!=null){
                    actionComposer.getBrsDriver().switchTo().frame(actionComposer.getBrsDriver().findElement(this.frameBy));
                }
                while(true){ //loop when StaleElementReferenceException is encountered
                    WebElement element = actionComposer.getBrsDriver().findElement(this.by);
                    try{
                        clickToSendKeys(element, this.clearBeforeSend, this.keysToSend);
                        break;
                    }catch(StaleElementReferenceException ignoreE){
                        if (LOGGER.isDebugEnabled()) LOGGER.debug("{}:{}", StaleElementReferenceException.class.getSimpleName(), this, ignoreE);
                    }
                }
            }catch(Exception e){
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
                        , SendKey.class.getSimpleName(), by.toString(), String.join(",", keysToSend), (frameBy!=null?frameBy.toString():""), String.valueOf(clearBeforeSend));
    }
}