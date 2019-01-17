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

import java.util.Optional;

import org.openqa.selenium.Alert;
import org.openqa.selenium.StaleElementReferenceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.kquiet.browser.ActionComposer;
import org.kquiet.browser.action.exception.ActionException;

/**
 * {@link ReplyAlert} is a subclass of {@link MultiPhaseAction} which interacts with the alert box.
 * {@link org.openqa.selenium.StaleElementReferenceException} may happen while {@link ReplyAlert} tries to manipulate the element, so multi-phase is used to perform the action again.
 * 
 * @author Kimberly
 */
public class ReplyAlert extends MultiPhaseAction {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReplyAlert.class);
    
    /**
     * The way to deal with alert box.
     */
    public static enum Decision {

        /**
         * Accept the alert box.
         */
        Accept,

        /**
         * Dismiss the alert box.
         */
        Dismiss,

        /**
         * Neither accept nor dismiss the alert box.
         */
        None
    }
    
    private final Decision decision;
    private final String textVariableName;
    private final String keysToSend;
    
    /**
     *
     * @param decision the way to deal with alert box
     * @param textVariableName text variable name; non-empty name means to set the text of alert box as a variable of {@link ActionComposer}
     * @param keysToSend characters to send to alert box
     */
    public ReplyAlert(Decision decision, String textVariableName, String keysToSend){
        super(null);
        this.decision = decision;
        this.textVariableName = Optional.ofNullable(textVariableName).orElse("");
        this.keysToSend = Optional.ofNullable(keysToSend).orElse("");
        super.setInternalAction(()->{
            ActionComposer actionComposer = this.getComposer();
            try{
                Alert alertBox = actionComposer.getWebDriver().switchTo().alert();

                //get text when necessary
                if (!this.textVariableName.isEmpty()) actionComposer.setVariable(this.textVariableName, alertBox.getText());

                //send keys when necessary
                if (!this.keysToSend.isEmpty()) alertBox.sendKeys(this.keysToSend);

                //deal with alert box
                switch(this.decision){
                    case Accept:
                        alertBox.accept();
                        break;
                    case Dismiss:
                        alertBox.dismiss();
                        break;
                    default:
                        break;
                }
                noNextPhase();
            }catch(StaleElementReferenceException ignoreE){ //with next phase when StaleElementReferenceException is encountered
                if (LOGGER.isDebugEnabled()) LOGGER.debug("{}({}): encounter stale element:{}", ActionComposer.class.getSimpleName(), actionComposer.getName(), toString(), ignoreE);
            }catch(Exception e){
                noNextPhase();
                throw new ActionException(e);
            }
        });
    }
    
    @Override
    public String toString(){
        return String.format("%s:%s/%s/%s", ReplyAlert.class.getSimpleName(), decision.toString(), textVariableName, keysToSend);
    }
}