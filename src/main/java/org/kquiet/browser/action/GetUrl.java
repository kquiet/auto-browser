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

import org.kquiet.browser.ActionComposer;
import org.kquiet.browser.action.exception.ExecutionException;

/**
 * {@link GetUrl} is a subclass of {@link OneTimeAction} which loads a web page.
 * 
 * @author Kimberly
 */
public class GetUrl extends OneTimeAction {
    private final String url;
            
    /**
     *
     * @param url the url of web page
     */
    public GetUrl(String url){
        super(null);
        this.url = url;
        super.setInternalAction(()->{
            ActionComposer actionComposer = this.getComposer();
            try{
                actionComposer.switchToFocusWindow();
                actionComposer.getBrsDriver().get(this.url);
            }
            catch(Exception e){
                throw new ExecutionException("Error: "+toString(), e);
            }
        });
    }
    
    @Override
    public String toString(){
        return String.format("%s(%s) %s:%s", ActionComposer.class.getSimpleName(), getComposer()==null?"":getComposer().getName(), GetUrl.class.getSimpleName(), url);
    }
}
