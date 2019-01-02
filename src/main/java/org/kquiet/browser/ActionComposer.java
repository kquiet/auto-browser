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
package org.kquiet.browser;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.openqa.selenium.WebDriver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.kquiet.utility.StopWatch;
import org.kquiet.browser.action.MultiPhaseAction;
import org.kquiet.concurrent.Prioritized;

/**
 *
 * @author Kimberly
 */
public class ActionComposer implements RunnableFuture<ActionComposer>, Prioritized {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActionComposer.class);
    
    private ActionRunner actionRunner = null;

    private MultiPhaseAction initAction = null;
    private MultiPhaseAction finalAction = null;
    private final Deque<MultiPhaseAction> mainActionList = new LinkedList<>();
    
    private Consumer<ActionComposer> onFailFunc = (bac)->{};
    private Consumer<ActionComposer> onSuccessFunc = (bac)->{};
    private Consumer<ActionComposer> onDoneFunc = (bac)->{};
    
    private final CountDownLatch checkDoneLatch = new CountDownLatch(1);

    private String name = null;
    private String message = null;
    private int priority = Integer.MIN_VALUE;
    private final StopWatch totalCostWatch = new StopWatch();
    
    private volatile boolean isFail = false;
    private volatile boolean isDone = false;
    private volatile boolean isCancelled = false;
    private volatile boolean skipAction = false;
    private volatile boolean skipResultFunction = false;
    private volatile boolean cacheFailInfo = true;
    private String failUrl = null;
    private String failPageContent = null;
    
    private volatile String focusWindowIdentity = null;
    private final Map<String, String> registeredWindows = new LinkedHashMap<>();
    private volatile boolean openWindowFlag = true;
    private volatile boolean closeWindowFlag = true;
    
    private ActionComposer parent = null;
    private ActionComposer child = null;
    
    /**
     *
     */
    public ActionComposer(){
    }
    
    @Override
    public boolean isCancelled() {
        return isCancelled;
    }
    
    @Override
    public boolean cancel(boolean mayInterruptIfRunning){
        if (isCancelled) return false;
        
        if (mayInterruptIfRunning){
            skipAll();
            isCancelled = true;
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean isDone() {
        return isDone;
    }
    
    @Override
    public ActionComposer get(){
        try{
            checkDoneLatch.await();
        }catch(InterruptedException ex){
            LOGGER.warn("{} interrupted", getClass().getSimpleName(), ex);
        }
        return this;
    }
    
    @Override
    public ActionComposer get(long timeout, TimeUnit unit)throws TimeoutException{
        try{
            boolean result = checkDoneLatch.await(timeout, unit);
            if (!result){
                throw new TimeoutException(getName()+" get() timeout!");
            }
        }catch(InterruptedException ex){
            LOGGER.warn("{} interrupted", getClass().getSimpleName(), ex);
        }
        return this;
    }
    
    @Override
    public void run(){
        try{
            totalCostWatch.start();
            boolean anyActionFail = false;
            
            //do all action
            int index=0;
            List<MultiPhaseAction> actionList = getAllActionInSequence();
            while(index<actionList.size() && !skipAction){
                MultiPhaseAction action = actionList.get(index);
                action.run();
                anyActionFail = anyActionFail || action.isFail();
                //break when any action fail
                if (anyActionFail) break;
                actionList = getAllActionInSequence();
                index++;
            }
            
            if (anyActionFail) runFail();
            else runSuccess();
        }catch(Exception ex){
            LOGGER.warn("{} run error", getName(), ex);
            runFail();
        }finally{
            runDone();
        }
    }
    
    private void runFail(){
        skipToFail();
            
        //cache fail info=>this may take about one second to do;uses a flag to avoid when necessary
        if (cacheFailInfo){
            try{
                setFailInfo(getBrsDriver().getCurrentUrl(), getBrsDriver().getPageSource());
            }catch(Exception e){
                LOGGER.warn("{}({}) set fail info error!", ActionComposer.class.getSimpleName(), getName(), e);
            }
        }

        if (skipResultFunction) return;
        try{
            onFailFunc.accept(this);
        }catch(Exception e){
            LOGGER.warn("{} fail function error", getName(), e);
        }
    }
    
    private void runSuccess(){
        if (skipResultFunction) return;
        try{
            onSuccessFunc.accept(this);
        }catch(Exception e){
            LOGGER.warn("{} success function error", getName(), e);
        }
    }
    
    private void runDone(){
        try{
            onDoneFunc.accept(this);
        }catch(Exception e){
            LOGGER.warn("{} done function error", getName(), e);
        }

        setDone();
        checkDoneLatch.countDown();
        if (LOGGER.isDebugEnabled()) LOGGER.debug("{} costs {} milliseconds", getName(), getCostTime().toMillis());
        
        //continue with child composer
        if (hasChild()){
            try{
                actionRunner.executeComposer(getChild());
            }catch(Exception ex){
                LOGGER.warn("{} execute child composer error", getName(), ex);
            }
        }
    }
    
    void setActionRunner(ActionRunner actionRunner){
        this.actionRunner = actionRunner;
    }
    
    /**
     *
     * @return
     */
    public String getRootWindow(){
        return actionRunner.getRootWindowIdentity();
    }
    
    /**
     *
     * @param name
     * @param windowIdentity
     * @return
     */
    public boolean registerWindow(String name, String windowIdentity){
        if (name==null || name.isEmpty() || windowIdentity==null || windowIdentity.isEmpty() || registeredWindows.containsKey(name)) return false;
        else{
            registeredWindows.put(name, windowIdentity);
            return true;
        }
    }
    
    /**
     *
     * @param registerName
     * @return
     */
    public String getRegisteredWindow(String registerName){
        return registeredWindows.getOrDefault(registerName, "");
    }
    
    /**
     *
     * @return
     */
    public List<String> getRegisteredWindows(){
        return registeredWindows.values().stream().collect(Collectors.toList());
    }
    
    /**
     *
     * @param runnable
     * @return
     */
    public Future<Exception> callBrowser(Runnable runnable){
        return actionRunner.executeAction(runnable, getPriority());
    }
    
    /**
     *
     * @param childActionComposer
     * @return
     */
    public ActionComposer continueWith(ActionComposer childActionComposer){
        if (childActionComposer==null) return this;
        
        //inspect if already has child
        ActionComposer oldChild = this.getChild();
        
        //continue with child
        this.setChild(childActionComposer);
        childActionComposer.setParent(this);
        
        //insert oldChild to the end of the action chain
        if (oldChild!=null){
            ActionComposer temp = this.getChild();
            while(temp.hasChild()){
                temp = temp.getChild();
            }
            temp.setChild(oldChild);
        }
        return this;
    }
    
    /**
     *
     * @return
     */
    public boolean hasChild(){
        return child!=null;
    }
    
    /**
     *
     * @return
     */
    public ActionComposer getChild(){
        return child;
    }
    
    private ActionComposer setChild(ActionComposer child){
        return this.child = child;
    }
    
    /**
     *
     * @return
     */
    public boolean hasParent(){
        return parent!=null;
    }
    
    /**
     *
     * @return
     */
    public ActionComposer getParent(){
        return parent;
    }
    
    private ActionComposer setParent(ActionComposer parent){
        return this.parent = parent;
    }
    
    /**
     *
     * @return
     */
    protected MultiPhaseAction getInitAction() {
        return initAction;
    }
    
    /**
     *
     * @param action
     */
    protected void setInitAction(MultiPhaseAction action) {
        initAction = action;
        action.setContainingComposer(this);
    }

    /**
     *
     * @return
     */
    protected MultiPhaseAction getFinalAction() {
        return finalAction;
    }

    /**
     *
     * @param action
     */
    protected void setFinalAction(MultiPhaseAction action) {
        finalAction = action;
        action.setContainingComposer(this);
    }
    
    /**
     *
     * @return
     */
    public boolean switchToFocusWindow(){
        //set focus window to root window if not set
        if (getFocusWindow()==null){
            setFocusWindow(actionRunner.getRootWindowIdentity());
        }
            
        return switchToWindow(getFocusWindow());
    }
    
    /**
     *
     * @param windowIdentity
     * @return
     */
    public boolean switchToWindow(String windowIdentity){
        try{
            getBrsDriver().switchTo().window(windowIdentity);
            return true;
        }catch(Exception ex){
            LOGGER.error("{} switchToWindow error", getName(), ex);
            return false;
        }
    }

    /**
     *
     * @param onFailFunc
     */
    public void setOnFailFunction(Consumer<ActionComposer> onFailFunc) {
        if (onFailFunc != null) this.onFailFunc = onFailFunc;
    }
    
    /**
     *
     * @param onSuccessFunc
     */
    public void setOnSuccessFunction(Consumer<ActionComposer> onSuccessFunc) {
        if (onSuccessFunc != null) this.onSuccessFunc = onSuccessFunc;
    }
    
    /**
     *
     * @param onDoneFunc
     */
    public void setOnDoneFunction(Consumer<ActionComposer> onDoneFunc) {
        if (onDoneFunc != null) this.onDoneFunc = onDoneFunc;
    }
    
    /**
     *
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     *
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     *
     * @return
     */
    public String getMessage() {
        return message;
    }

    /**
     *
     * @param message
     */
    public void setMessage(String message) {
        this.message = message;
    }
    
    /**
     *
     * @return
     */
    @Override
    public int getPriority() {
        return priority;
    }

    /**
     *
     * @param priority
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }    
    
    /**
     *
     * @return
     */
    public Duration getCostTime() {
        return totalCostWatch.getDuration();
    }
    
    /**
     *
     * @return
     */
    public boolean isFail(){
        return isFail;
    }
    
    /**
     *
     * @return
     */
    public boolean isSuccess(){
        return !isFail;
    }

    private void setDone() {
        isDone = true;
        totalCostWatch.stop();
    }

    /**
     *
     * @return
     */
    public String getFailUrl() {
        return failUrl;
    }

    /**
     *
     * @return
     */
    public String getFailPageContent() {
        return failPageContent;
    }
    
    /**
     *
     * @param failUrl
     * @param failPageContent
     */
    public void setFailInfo(String failUrl, String failPageContent) {
        this.failUrl = failUrl;
        this.failPageContent = failPageContent;
    }
    
    /**
     *
     * @param flag
     * @return
     */
    public ActionComposer cacheFailInfo(boolean flag){
        this.cacheFailInfo = flag;
        return this;
    }

    /**
     *
     * @return
     */
    public WebDriver getBrsDriver() {
        return actionRunner.getWebDriver();
    }

    /**
     *
     * @return
     */
    public String getFocusWindow() {
        return focusWindowIdentity;
    }

    /**
     *
     * @param focusWindowIdentity
     */
    public void setFocusWindow(String focusWindowIdentity) {
        this.focusWindowIdentity = focusWindowIdentity;
    }
    
    /**
     *
     * @param action
     * @return
     */
    public boolean addActionToTail(MultiPhaseAction action){
        if (action==null) return false;
        synchronized(this){
            mainActionList.addLast(action);
            action.setContainingComposer(this);
        }
        return true;
    }
    
    /**
     *
     * @param action
     * @return
     */
    public boolean addActionToHead(MultiPhaseAction action){
        if (action==null) return false;
        synchronized(this){
            mainActionList.addFirst(action);
            action.setContainingComposer(this);
        }
        return true;
    }
    
    /**
     *
     * @param action
     * @param index
     * @return
     */
    public boolean addActionToIndex(MultiPhaseAction action, int index){
        if (action==null || index<0 || index>mainActionList.size()) return false;
        
        synchronized(this){
            final Deque<MultiPhaseAction> temp = new LinkedList<>(mainActionList);
            final Deque<MultiPhaseAction> stack = new LinkedList<>();
            for (int i=0;i<index;i++){
                stack.addFirst(temp.removeFirst());
            }
            temp.addFirst(action);
            for (int i=0;i<index;i++){
                temp.addFirst(stack.removeFirst());
            }
            mainActionList.clear();
            mainActionList.addAll(temp);
            action.setContainingComposer(this);
        }
        return true;
    }
    
    private List<MultiPhaseAction> getAllActionInSequence(){
        List<MultiPhaseAction> actionList = new ArrayList<>();
        if (initAction!=null) actionList.add(initAction);
        synchronized(this){
            actionList.addAll(mainActionList);
        }
        if (finalAction!=null) actionList.add(finalAction);
        return actionList;
    }
    
    /**
     *
     */
    public void skipToFail(){
        isFail = true;
        skipAction = true;
    }
    
    /**
     *
     */
    public void skipToSuccess(){
        skipAction = true;
    }
    
    /**
     *
     */
    public void skipAll(){
        skipAction = true;
        skipResultFunction = true;
    }
    
    /**
     *
     * @return
     */
    public List<Exception> getExceptionFromFailAction(){
        List<MultiPhaseAction> actionList = getAllActionInSequence();
        List<Exception> result = new ArrayList<>();
        for(MultiPhaseAction action:actionList){
            if (action.isFail()&&action.getErrors()!=null){
                result.addAll(action.getErrors());
            }
        }
        return result;
    }

    /**
     *
     * @return
     */
    public boolean needOpenWindow() {
        return openWindowFlag;
    }

    /**
     *
     * @param openWindowFlag
     */
    public void setOpenWindow(boolean openWindowFlag) {
        this.openWindowFlag = openWindowFlag;
    }

    /**
     *
     * @return
     */
    public boolean needCloseWindow() {
        return closeWindowFlag;
    }

    /**
     *
     * @param closeWindowFlag
     */
    public void setCloseWindow(boolean closeWindowFlag) {
        this.closeWindowFlag = closeWindowFlag;
    }
}