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

import java.util.stream.Collectors;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openqa.selenium.JavascriptExecutor;

import org.kquiet.browser.ActionComposer;
import org.kquiet.browser.action.exception.ActionException;

/**
 * {@link PostForm} is a subclass of {@link SinglePhaseAction} which creates a form and submits it through http post.
 * Currently only supports enctype 'application/x-www-form-urlencoded'.
 * 
 * @author Kimberly
 */
public class PostForm extends SinglePhaseAction {
    private static final Logger LOGGER = LoggerFactory.getLogger(PostForm.class);

    private final String url;
    private final List<SimpleImmutableEntry<String,String>> formData;
    private final String acceptCharset;
    
    /**
     * 
     * @param url the address where to submit the form
     * @param formData the form data to submit
     * @param acceptCharset the charset used in the submitted form
     */
    public PostForm(String url, List<SimpleImmutableEntry<String,String>> formData, String acceptCharset){
        super(null);
        this.url = url;
        this.formData = formData;
        this.acceptCharset = acceptCharset;
        String formId = UUID.randomUUID().toString();
        
        super.setInternalAction(()->{
            ActionComposer actionComposer = this.getComposer();
            try{
                String scriptStr = String.format("var formE = document.createElement('form');formE.setAttribute('id','%s');formE.setAttribute('name','%s');formE.setAttribute('method','post');formE.setAttribute('enctype','application/x-www-form-urlencoded');formE.setAttribute('action','%s');", formId.replaceAll("'", "\'"), formId.replaceAll("'", "\'"), this.url);
                if (this.acceptCharset!=null && !this.acceptCharset.isEmpty()){
                    scriptStr+="formE.setAttribute('accept-charset','"+this.acceptCharset.replaceAll("'", "\'")+"');";
                }
                
                if (this.formData!=null){
                    List<SimpleImmutableEntry<String,String>> formDataList = (List<SimpleImmutableEntry<String,String>>) this.formData;
                    if (!formDataList.isEmpty()){
                        scriptStr+="var customE;";
                        for(SimpleImmutableEntry<String, String> input:formDataList){
                            scriptStr+=String.format("customE = document.createElement('input');customE.setAttribute('type','hidden');customE.setAttribute('name','%s');customE.setAttribute('value','%s');formE.appendChild(customE);", input.getKey().replaceAll("'", "\'"), input.getValue().replaceAll("'", "\'"));
                        }
                    }
                }
                scriptStr+=String.format("document.body.appendChild(formE);document.getElementById('%s').submit();", formId.replaceAll("'", "\'"));
                try{
                    actionComposer.switchToFocusWindow();
                    ((JavascriptExecutor)actionComposer.getBrsDriver()).executeScript(scriptStr);
                }catch(Exception ex){
                    LOGGER.warn("Execute javascript error:{}", toString(), ex);
                }
            }
            catch(Exception e){
                throw new ActionException("Error: "+toString(), e);
            }
        });
    }
    
    @Override
    public String toString(){
        return String.format("%s(%s) %s:%s/%s/%s", ActionComposer.class.getSimpleName(), getComposer()==null?"":getComposer().getName(), PostForm.class.getSimpleName(), url, String.join(",", formData.stream().map(s->s.getKey()+"="+s.getValue()).collect(Collectors.toList())), acceptCharset);
    }
}