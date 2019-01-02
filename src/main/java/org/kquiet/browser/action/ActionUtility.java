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

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.support.ui.FluentWait;

import org.kquiet.browser.action.Select.SelectBy;

/**
 *
 * @author Kimberly
 */
public class ActionUtility {

    /**
     *
     * @param driver
     * @param element
     */
    public static void click(WebDriver driver, WebElement element){
        element.click();
    }
    
    /**
     *
     * @param driver
     * @param element
     * @param clearBeforeSend
     * @param keysToSend
     */
    public static void clickToSendKeys(WebDriver driver, WebElement element, boolean clearBeforeSend, CharSequence... keysToSend){
        element.click();
        if (clearBeforeSend) element.sendKeys(Keys.chord(Keys.CONTROL+"a"));
        else  element.sendKeys(Keys.chord(Keys.CONTROL, Keys.END));
        element.sendKeys(keysToSend);
    }
    
    /**
     *
     * @param driver
     * @param element
     * @param selectBy
     * @param optionValue
     */
    public static void clickToSelect(WebDriver driver, WebElement element, SelectBy selectBy, Object... optionValue){
        element.click();
        Select elementToSelect = new Select(element);
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
    
    /**
     *
     * @param driver
     * @param element
     */
    public static void scrollIntoView(WebDriver driver, WebElement element){
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
    }
    
    /**
     *
     * @param <V>
     * @param driver
     * @param waitTimeoutMilliseconds
     * @param pollUnitMilliseconds
     * @param exceptionList
     * @param isTrueFunc
     */
    public static <V extends Object> void waitUntil(WebDriver driver, long waitTimeoutMilliseconds, long pollUnitMilliseconds, List<Class<? extends Throwable>> exceptionList, Function<? super WebDriver, V> isTrueFunc){
        FluentWait<WebDriver> wait = new FluentWait<>(driver)
                                    .withTimeout(Duration.ofMillis(waitTimeoutMilliseconds))
                                    .pollingEvery(Duration.ofMillis(pollUnitMilliseconds));
        if (exceptionList!=null && exceptionList.size()>0){
            wait = wait.ignoreAll(exceptionList);
        }
        wait.until(isTrueFunc);
    }
    
    /**
     *
     * @param sequence
     * @return
     */
    public static CharSequence[] purifyCharSequences(CharSequence... sequence){
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
     *
     * @param objs
     * @return
     */
    public static Object[] purifyObjects(Object... objs){
        if (objs==null){
            objs = new Object[]{};
        }
        return objs;
    }
}
