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
import org.kquiet.browser.action.exception.ActionException;

/**
 * {@link Select} is a subclass of {@link SinglePhaseAction} which selects/deselects options on a SELECT element.
 * 
 * @author Kimberly
 */
public class Select extends SinglePhaseAction {

    /**
     * The way to perform the selecting.
     */
    public static enum SelectBy {

        /**
         * Select/deselect the option by index
         */
        Index,

        /**
         * Select/deselect the option by value
         */
        Value,

        /**
         * Select/deselect the option by text
         */
        Text
    }
    
    private final By by;
    private final By frameBy;
    private final SelectBy selectBy;
    private final Object[] options;
    
    /**
     *
     * @param by by the element locating mechanism
     * @param frameBy the frame locating mechanism for the element resides in a frame
     * @param selectBy the way to perform the selecting
     * @param options the option to select; all options are deselected when no option is supplied and the SELECT element supports selecting multiple options
     */
    public Select(By by, By frameBy, SelectBy selectBy, Object... options){
        super(null);
        this.by = by;
        this.frameBy = frameBy;
        this.selectBy = selectBy;
        this.options = options;
        super.setInternalAction(()->{
            ActionComposer actionComposer = this.getComposer();
            try{
                actionComposer.switchToFocusWindow();
                if (this.frameBy!=null){
                    actionComposer.getBrsDriver().switchTo().frame(actionComposer.getBrsDriver().findElement(this.frameBy));
                }
                List<WebElement> elementList = actionComposer.getBrsDriver().findElements(this.by);
                WebElement element = elementList.isEmpty()?null:elementList.get(0);
                if (element==null) throw new ActionException("can't find the element to select");
                else clickToSelect(element, this.selectBy, this.options);
            }catch(Exception e){
                throw new ActionException("Error: "+toString(), e);
            }
        });
    }
    
    /**
     * Clicks an element and then performs selecting on it.
     * 
     * @param element the element to perform the selecting
     * @param selectBy the way to perform the selecting
     * @param options the option to select; all options are deselected when no option is supplied and the SELECT element supports selecting multiple options
     */
    public static void clickToSelect(WebElement element, SelectBy selectBy, Object... options){
        element.click();
        org.openqa.selenium.support.ui.Select elementToSelect = new org.openqa.selenium.support.ui.Select(element);
        if ((options==null||options.length==0)&&elementToSelect.isMultiple()){
            elementToSelect.deselectAll();
        }
        else if (options!=null){
            switch(selectBy){
                case Index:
                    for (Object obj: options){
                        elementToSelect.selectByIndex((Integer)obj);
                    }
                    break;
                case Value:
                    for (Object obj: options){
                        elementToSelect.selectByValue((String)obj);
                    }
                    break;
                default:
                case Text:
                    for (Object obj: options){
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
                , Select.class.getSimpleName(), by.toString(), selectBy.toString(), String.join(",", Arrays.asList(options).stream().map(s->s.toString()).collect(Collectors.toList())), (frameBy!=null?frameBy.toString():""));
    }
}