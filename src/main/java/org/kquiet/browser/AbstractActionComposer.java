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
package org.kquiet.browser;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.By;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.kquiet.utility.Stopwatch;
import org.kquiet.browser.action.Composable;

/**
 * {@link AbstractActionComposer} implements most methods of {@link ActionComposer} to lay ground works for possible subclasses.
 * {@link AbstractActionComposer} itself is a subclass of {@link CompletableFuture}, so any subclass of {@link AbstractActionComposer} should complete itself explictly in {@link #run()}.
 *
 * @author Kimberly
 */
public abstract class AbstractActionComposer extends CompletableFuture<Void> implements ActionComposer {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractActionComposer.class);
    
    /**
     * The execution context stack of this {@link AbstractActionComposer}, the {@link AddableActionSequence} must be pushed into this stack before any action in it gets executed to reflect execution context.
     */
    protected final Stack<AddableActionSequence> executionContextStack = new Stack<>();

    /**
     * The stop watch used to measure run cost of this {@link AbstractActionComposer}
     */
    protected final Stopwatch totalCostWatch = new Stopwatch();
    
    private final Deque<Composable> mainActionList = new LinkedList<>();
    private final Map<String, Object> variableMap = Collections.synchronizedMap(new LinkedHashMap<>());
    private final Map<String, String> registeredWindows = new LinkedHashMap<>();
    
    private int priority = Integer.MIN_VALUE;
    private Consumer<ActionComposer> onFailFunc = (bac)->{};
    private Consumer<ActionComposer> onSuccessFunc = (bac)->{};
    private Consumer<ActionComposer> onDoneFunc = (bac)->{};

    private ActionRunner actionRunner = null;    
    private String name = "";
    private String focusWindowIdentity = null;
    
    /**
     * Create an {@link AbstractActionComposer}
     */
    public AbstractActionComposer(){
    }
    
    @Override
    public AbstractActionComposer setActionRunner(ActionRunner actionRunner){
        this.actionRunner = actionRunner;
        return this;
    }
    
    /**
     * 
     * @return associated {@link ActionRunner}
     */
    protected ActionRunner getActionRunner(){
        return actionRunner;
    }
   
    
    @Override
    public String getRootWindow(){
        return actionRunner.getRootWindowIdentity();
    }
    
    @Override
    public boolean registerWindow(String name, String windowIdentity){
        if (name==null || windowIdentity==null || windowIdentity.isEmpty()) return false;
        
        if (registeredWindows.containsKey(name)) return false;
        else{
            registeredWindows.put(name, windowIdentity);
            return true;
        }
    }
    
    @Override
    public String getRegisteredWindow(String registerName){
        return registeredWindows.getOrDefault(registerName, "");
    }
    
    @Override
    public Map<String, String> getRegisteredWindows(){
        return new LinkedHashMap<>(registeredWindows);
    }
    
    @Override
    public Object getVariable(String variableName){
        return variableMap.get(variableName);
    }
    
    @Override
    public AbstractActionComposer setVariable(String variableName, Object value){
        variableMap.put(variableName, value);
        return this;
    }
    
    @Override
    public CompletableFuture<Void> callBrowser(Runnable runnable){
        return actionRunner.executeAction(runnable, getPriority());
    }
    
    @Override
    public void perform(Composable action){
        if (action==null) return;
        
        boolean isSequenceContainer = action instanceof AddableActionSequence;
        try{
            if (isSequenceContainer) executionContextStack.push((AddableActionSequence)action);
            action.setComposer(this);
            action.perform();
        }finally{
            if (isSequenceContainer) executionContextStack.pop();
        }
    }
    
    @Override
    public boolean switchToFocusWindow(){
        return switchToWindow(getFocusWindow());
    }
    
    @Override
    public boolean switchToWindow(String windowIdentity){
        try{
            getWebDriver().switchTo().window(windowIdentity);
            return true;
        }catch(Exception ex){
            if (LOGGER.isDebugEnabled()) LOGGER.debug("{} switchToWindow error", getName(), ex);
            return false;
        }
    }
    
    @Override
    public void switchToInnerFrame(List<By> frameBySequence){
        if (frameBySequence!=null){
            WebDriver driver = getWebDriver();
            for (By frameBy: frameBySequence){
                driver.switchTo().frame(driver.findElement(frameBy));
            }
        }
    }
    
    @Override
    public boolean switchToTop(){
        try{
            getWebDriver().switchTo().defaultContent();
            return true;
        }catch(UnhandledAlertException ex){//firefox raises an UnhandledAlertException when an alert box is presented, but chrome doesn't
            if (LOGGER.isDebugEnabled()) LOGGER.debug("{} switchToTop error", getName(), ex);
            return false;
        }
    }

    @Override
    public ActionComposer onFail(Consumer<ActionComposer> onFailFunc) {
        if (onFailFunc != null) this.onFailFunc = onFailFunc;
        return this;
    }
    
    /**
     * 
     * @return the callback function to be executed when failed
     */
    protected Consumer<ActionComposer> getFailFunction() {
        return onFailFunc;
    }
    
    @Override
    public ActionComposer onSuccess(Consumer<ActionComposer> onSuccessFunc) {
        if (onSuccessFunc != null) this.onSuccessFunc = onSuccessFunc;
        return this;
    }
    
    /**
     * 
     * @return the callback function to be executed when finished without fail
     */
    protected Consumer<ActionComposer> getSuccessFunction() {
        return onSuccessFunc;
    }
    
    @Override
    public ActionComposer onDone(Consumer<ActionComposer> onDoneFunc) {
        if (onDoneFunc != null) this.onDoneFunc = onDoneFunc;
        return this;
    }
    
    /**
     * 
     * @return the callback function to be executed when finished
     */
    protected Consumer<ActionComposer> getDoneFunction() {
        return onDoneFunc;
    }
    
    @Override
    public String getName() {
        return name;
    }

    @Override
    public AbstractActionComposer setName(String name) {
        this.name = name;
        return this;
    }
    
    @Override
    public int getPriority() {
        return priority;
    }

    /**
     * Set the priority of this {@link AbstractActionComposer}.
     * 
     * @param priority priority
     * @return self reference
     */
    public AbstractActionComposer setPriority(int priority) {
        this.priority = priority;
        return this;
    }    
    
    /**
     *
     * @return total execution time of this {@link AbstractActionComposer}
     */
    public Duration getCostTime() {
        return totalCostWatch.getDuration();
    }

    @Override
    public WebDriver getWebDriver() {
        return actionRunner.getWebDriver();
    }

    @Override
    public String getFocusWindow() {
        //set focus window to root window if not set
        if (focusWindowIdentity==null){
            focusWindowIdentity = getRootWindow();
        }
        return focusWindowIdentity;
    }

    @Override
    public AbstractActionComposer setFocusWindow(String windowIdentity) {
        this.focusWindowIdentity = windowIdentity;
        return this;
    }
    
    @Override
    public AbstractActionComposer addToHead(Composable action){
        if (action==null) return this;
        synchronized(this){
            mainActionList.addFirst(action);
        }
        return this;
    }
    
    @Override
    public AbstractActionComposer addToTail(Composable action){
        if (action==null) return this;
        synchronized(this){
            mainActionList.addLast(action);
        }
        return this;
    }

    @Override
    public AbstractActionComposer addToPosition(Composable action, int position){
        if (action==null) return this;
        synchronized(this){
            final List<Composable> temp = new ArrayList<>(mainActionList);
            temp.add(position, action);
            mainActionList.clear();
            mainActionList.addAll(temp);
        }
        return this;
    }

    @Override
    public AddableActionSequence addToHeadByContext(Composable action){
        AddableActionSequence context = executionContextStack.peek();
        return context.addToHead(action);
    }

    @Override
    public AddableActionSequence addToTailByContext(Composable action){
        AddableActionSequence context = executionContextStack.peek();
        return context.addToTail(action);
    }
    
    @Override
    public AddableActionSequence addToPositionByContext(Composable action, int position){
        AddableActionSequence context = executionContextStack.peek();
        return context.addToPosition(action, position);
    }
    
    /**
     * Get all actions in the action sequence.
     * 
     * @return list of actions
     */
    protected List<Composable> getAllActionInSequence(){
        List<Composable> actionList = new ArrayList<>();
        synchronized(this){
            actionList.addAll(mainActionList);
        }
        return actionList;
    }
    
    /**
     * 
     * @return error list from executed actions
     */
    @Override
    public List<Exception> getErrors(){
        List<Composable> actionList = getAllActionInSequence();
        List<Exception> result = new ArrayList<>();
        for(Composable action:actionList){
            if (action.isFail()&&action.getErrors()!=null){
                result.addAll(action.getErrors());
            }
        }
        return result;
    }
}