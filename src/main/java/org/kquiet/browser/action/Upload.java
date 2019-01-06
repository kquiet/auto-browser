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

import org.kquiet.browser.ActionComposer;
import org.kquiet.browser.action.exception.ExecutionException;

/**
 * {@link Upload} is a subclass of {@link OneTimeAction} which scroll an element into visible area of the browser window.
 * 
 * @author Kimberly
 */
public class Upload extends OneTimeAction {
    private final By by;
    private final By frameBy;
    private final String pathOfFile;

    /**
     *
     * @param by the element locating mechanism
     * @param frameBy the frame locating mechanism for the element resides in a frame
     * @param pathOfFile the path of file to upload
     */
    public Upload(By by, By frameBy, String pathOfFile){
        super(null);
        this.by = by;
        this.frameBy = frameBy;
        this.pathOfFile = pathOfFile;
        super.setInternalAction(()->{
            ActionComposer actionComposer = this.getComposer();
            try{
                actionComposer.switchToFocusWindow();
                if (this.frameBy!=null){
                    actionComposer.getBrsDriver().switchTo().frame(actionComposer.getBrsDriver().findElement(this.frameBy));
                }
                List<WebElement> elementList = actionComposer.getBrsDriver().findElements(this.by);
                WebElement element = elementList.isEmpty()?null:elementList.get(0);
                if (element==null) throw new ExecutionException("can't find the element to set upload file path");
                else {
                    ((JavascriptExecutor)actionComposer.getBrsDriver()).executeScript("arguments[0].style.display = ''; arguments[0].style.visibility = 'visible'; arguments[0].style.height = '1px'; arguments[0].style.width = '1px'; arguments[0].style.opacity = 1", element);
                    element.sendKeys(this.pathOfFile);
                }
            }catch(Exception e){
                throw new ExecutionException("Error: "+toString(), e);
            }
        });
    }
    
    @Override
    public String toString(){
        return String.format("%s(%s) %s:%s/%s/%s", ActionComposer.class.getSimpleName()
                , getComposer()==null?"":getComposer().getName(), Upload.class.getSimpleName(), by.toString()
                , (frameBy!=null?frameBy.toString():""), pathOfFile);
    }
}
