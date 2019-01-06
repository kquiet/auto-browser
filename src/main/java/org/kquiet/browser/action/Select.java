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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import org.kquiet.browser.ActionComposer;
import org.kquiet.browser.action.exception.ExecutionException;

/**
 *
 * @author Kimberly
 */
public class Select extends OneTimeAction {

    /**
     *
     */
    public static enum SelectBy {

        /**
         *
         */
        Index,

        /**
         *
         */
        Value,

        /**
         *
         */
        Text
    }
    
    private final By by;
    private final By frameBy;
    private final SelectBy selectBy;
    private final Object[] optionValue;
    
    /**
     *
     * @param by by the element locating mechanism
     * @param frameBy the frame locating mechanism for the element resides in a frame
     * @param selectBy
     * @param optionValue
     */
    public Select(By by, By frameBy, SelectBy selectBy, Object... optionValue){
        super(null);
        this.by = by;
        this.frameBy = frameBy;
        this.selectBy = selectBy;
        this.optionValue = optionValue;
        super.setInternalAction(()->{
            ActionComposer actionComposer = this.getComposer();
            try{
                actionComposer.switchToFocusWindow();
                if (this.frameBy!=null){
                    actionComposer.getBrsDriver().switchTo().frame(actionComposer.getBrsDriver().findElement(this.frameBy));
                }
                List<WebElement> elementList = actionComposer.getBrsDriver().findElements(this.by);
                WebElement element = elementList.isEmpty()?null:elementList.get(0);
                if (element==null) throw new ExecutionException("can't find the element to select");
                else clickToSelect(element, this.selectBy, this.optionValue);
            }catch(Exception e){
                throw new ExecutionException("Error: "+toString(), e);
            }
        });
    }
    
    /**
     *
     * @param element
     * @param selectBy
     * @param optionValue
     */
    public static void clickToSelect(WebElement element, SelectBy selectBy, Object... optionValue){
        element.click();
        org.openqa.selenium.support.ui.Select elementToSelect = new org.openqa.selenium.support.ui.Select(element);
        if ((optionValue==null||optionValue.length==0)&&elementToSelect.isMultiple()){
            elementToSelect.deselectAll();
        }
        else if (optionValue!=null){
            switch(selectBy){
                case Index:
                    for (Object obj: optionValue){
                        elementToSelect.selectByIndex((Integer)obj);
                    }
                    break;
                case Value:
                    for (Object obj: optionValue){
                        elementToSelect.selectByValue((String)obj);
                    }
                    break;
                default:
                case Text:
                    for (Object obj: optionValue){
                        elementToSelect.selectByVisibleText((String)obj);
                    }
                    break;
            }
        }
    }
    
    @Override
    public String toString(){
        return String.format("%s(%s) %s:%s/%s/%s/%s"
                , ActionComposer.class.getSimpleName(), getComposer()==null?"":getComposer().getName()
                , Select.class.getSimpleName(), by.toString(), selectBy.toString(), String.join(",", Arrays.asList(optionValue).stream().map(s->s.toString()).collect(Collectors.toList())), (frameBy!=null?frameBy.toString():""));
    }
}