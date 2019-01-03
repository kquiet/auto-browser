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
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import org.kquiet.browser.action.OpenWindow;
import org.kquiet.concurrent.Prioritized;
import org.kquiet.browser.action.CloseWindow;
import org.kquiet.browser.action.IfThenElse;

/**
 * {@link ActionComposer} is responsible to maintain a list of actions, arrange them to be executed through associated {@link ActionRunner} and track their execution result.
 * If any executed action fails, {@link ActionComposer} marks itself failed as well.
 * 
 * <p>In addition to the actions added by add*() methods, {@link ActionComposer} has two extra/internal actions which are executed at the beginning and the end respectively:</p>
 * <ol>
 * <li>The action executed at the beginning is called as <i>Initial Action</i>, which opens a new browser window and set it as <i>focus window</i>.
 * All actions should be executed against this focus window to be isolated from other {@link ActionComposer}, however it could be changed by actions if necessary.
 * If no focus window is specified, it will use the root window of {@link ActionRunner} as its focus window.</li>
 * <li>The action executed at the end is called as <i>Final Action</i>, which closes all registered windows. </li>
 * </ol>
 *
 * @author Kimberly
 */
public class ActionComposer implements RunnableFuture<ActionComposer>, Prioritized {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActionComposer.class);
    
    private ActionRunner actionRunner = null;

    private final MultiPhaseAction initAction = new IfThenElse(ac->this.needOpenWindow(), Arrays.asList(new OpenWindow(true, UUID.randomUUID().toString())), null).setContainingComposer(this);
    private final MultiPhaseAction finalAction = new IfThenElse(ac->this.needCloseWindow(), Arrays.asList(new CloseWindow(true)), null).setContainingComposer(this);
    private final Deque<MultiPhaseAction> mainActionList = new LinkedList<>();
    
    private Consumer<ActionComposer> onFailFunc = (bac)->{};
    private Consumer<ActionComposer> onSuccessFunc = (bac)->{};
    private Consumer<ActionComposer> onDoneFunc = (bac)->{};
    
    private final CountDownLatch checkDoneLatch = new CountDownLatch(1);

    private String name = null;
    private int priority = Integer.MIN_VALUE;
    private final StopWatch totalCostWatch = new StopWatch();
    
    private volatile boolean isFail = false;
    private volatile boolean isDone = false;
    private volatile boolean isCancelled = false;
    private volatile boolean skipAction = false;
    private volatile boolean skipResultFunction = false;
    private volatile boolean cacheFailInfo = true;
    private String failUrl = null;
    private String failPage = null;
    
    private volatile String focusWindowIdentity = null;
    private final Map<String, String> registeredWindows = new LinkedHashMap<>();
    private volatile boolean openWindowFlag = true;
    private volatile boolean closeWindowFlag = true;
    
    private ActionComposer parent = null;
    private ActionComposer child = null;
    
    /**
     * Create an {@link ActionComposer}
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
     * @return window identity of the root window
     * @see ActionRunner#getRootWindowIdentity()
     */
    public String getRootWindow(){
        return actionRunner.getRootWindowIdentity();
    }
    
    /**
     * Register a window. The window identity of registered window can be obtained throught {@link #getRegisteredWindow(java.lang.String) getRegisteredWindow()}.
     * Registered windows will to be closed in <i>Final Action</i>.
     * 
     * @param name register name
     * @param windowIdentity window identity
     * @return {@code true} if register name and window identity are not empty and the register name isn't registered; {@code false} otherwise
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
     * @param registerName register name of window
     * @return window identity
     */
    public String getRegisteredWindow(String registerName){
        return registeredWindows.getOrDefault(registerName, "");
    }
    
    /**
     *
     * @return all window identities of registered windows
     */
    public List<String> getRegisteredWindows(){
        return registeredWindows.values().stream().collect(Collectors.toList());
    }
    
    /**
     * Delegate the execution of {@link java.lang.Runnable runnable} to associated {@link ActionRunner} with this {@link ActionComposer}'s priority
     * 
     * @param runnable the object whose run method will be invoked
     * @return a {@link java.util.concurrent.Future Future} representing pending completion of given {@link java.lang.Runnable runnable}.
     * {@link java.util.concurrent.Future#get() Future's get methods} returns {@code null} if no exception occurred.
     */
    public Future<Exception> callBrowser(Runnable runnable){
        return actionRunner.executeAction(runnable, getPriority());
    }
    
    /**
     * Delegate the execution of given child {@link ActionComposer} to associated {@link ActionRunner} after this {@link ActionComposer} is done.
     * Every {@link ActionComposer} has at most one parent/child {@link ActionComposer}.
     * If this {@link ActionComposer} already has a child {@link ActionComposer}, the <i>original</i> child {@link ActionComposer} will be postponed.
     * 
     * <p>For example, before calling this method: ComposerA-&gt;ChildOfComposerA-&gt;GrandChildOfComposerA;
     * after: ComposerA-&gt;NewChildOfComposerA-&gt;ChildOfComposerA-&gt;GrandChildOfComposerA.</p>
     * 
     * @param childActionComposer the {@link ActionComposer} to be executed
     * @return child {@link ActionComposer}
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
        return childActionComposer;
    }
    
    /**
     *
     * @return {@code true} if this {@link ActionComposer} has child {@link ActionComposer}; {@code false} otherwise
     */
    public boolean hasChild(){
        return child!=null;
    }
    
    /**
     *
     * @return child {@link ActionComposer} if exists; {@code null} otherwise
     */
    public ActionComposer getChild(){
        return child;
    }
    
    private void setChild(ActionComposer child){
        this.child = child;
    }
    
    /**
     *
     * @return {@code true} if this {@link ActionComposer} has parent {@link ActionComposer}; {@code false} otherwise
     */
    public boolean hasParent(){
        return parent!=null;
    }
    
    /**
     *
     * @return parent {@link ActionComposer} if exists; {@code null} otherwise
     */
    public ActionComposer getParent(){
        return parent;
    }
    
    private ActionComposer setParent(ActionComposer parent){
        return this.parent = parent;
    }
    
    /**
     * Switch browser's focus window to this {@link ActionComposer}'s focus window.
     * 
     * @return {@code true} if switch success; {@code false} otherwise
     */
    public boolean switchToFocusWindow(){
        return switchToWindow(getFocusWindow());
    }
    
    /**
     * Switch browser's focus window to specified window.
     * 
     * @param windowIdentity the window identity to switch to
     * @return {@code true} if switch success; {@code false} otherwise
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
     * Set the callback function to be executed when this {@link ActionComposer} is marked as failed.
     * 
     * @param onFailFunc the callback function to be executed
     * @return this {@link ActionComposer}
     */
    public ActionComposer setOnFailFunction(Consumer<ActionComposer> onFailFunc) {
        if (onFailFunc != null) this.onFailFunc = onFailFunc;
        return this;
    }
    
    /**
     * Set the callback function to be executed when this {@link ActionComposer} is finished without being marked as failed.
     * 
     * @param onSuccessFunc the callback function to be executed
     * @return this {@link ActionComposer}
     */
    public ActionComposer setOnSuccessFunction(Consumer<ActionComposer> onSuccessFunc) {
        if (onSuccessFunc != null) this.onSuccessFunc = onSuccessFunc;
        return this;
    }
    
    /**
     * Set the callback function to be executed when this {@link ActionComposer} is done.
     * This callback function is executed after <i>fail function</i> and <i>success function</i>.
     * 
     * @param onDoneFunc the callback function to be executed
     * @return this {@link ActionComposer}
     */
    public ActionComposer setOnDoneFunction(Consumer<ActionComposer> onDoneFunc) {
        if (onDoneFunc != null) this.onDoneFunc = onDoneFunc;
        return this;
    }
    
    /**
     *
     * @return the name of this {@link ActionComposer}
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of this {@link ActionComposer}.
     * @param name name
     * @return this {@link ActionComposer}
     */
    public ActionComposer setName(String name) {
        this.name = name;
        return this;
    }
    
    @Override
    public int getPriority() {
        return priority;
    }

    /**
     * Set the priority of this {@link ActionComposer}
     * @param priority priority
     * @return this {@link ActionComposer}
     */
    public ActionComposer setPriority(int priority) {
        this.priority = priority;
        return this;
    }    
    
    /**
     *
     * @return total execution time of this {@link ActionComposer}
     */
    public Duration getCostTime() {
        return totalCostWatch.getDuration();
    }
    
    /**
     *
     * @return {@code true} if this {@link ActionComposer} has been marked as failed; {@code false} otherwise
     */
    public boolean isFail(){
        return isFail;
    }
    
    /**
     *
     * @return {@code true} if this {@link ActionComposer} is done without being marked as failed; {@code false} otherwise
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
     * @return the url of the last page when this {@link ActionComposer} is marked as failed and {@link #cacheFailInfo(boolean) cache fail info} is enabled; {@code null} otherwise
     */
    public String getFailUrl() {
        return failUrl;
    }

    /**
     *
     * @return the content of the last page when this {@link ActionComposer} is marked as failed and {@link #cacheFailInfo(boolean) cache fail info} is enabled; {@code null} otherwise
     */
    public String getFailPage() {
        return failPage;
    }

    private ActionComposer setFailInfo(String failUrl, String failPage) {
        this.failUrl = failUrl;
        this.failPage = failPage;
        return this;
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
        //set focus window to root window if not set
        if (focusWindowIdentity==null){
            focusWindowIdentity = getRootWindow();
        }
        return focusWindowIdentity;
    }

    /**
     *
     * @param focusWindowIdentity
     * @return 
     */
    public ActionComposer setFocusWindow(String focusWindowIdentity) {
        this.focusWindowIdentity = focusWindowIdentity;
        return this;
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
    public List<Exception> getErrors(){
        List<MultiPhaseAction> actionList = getAllActionInSequence();
        List<Exception> result = new ArrayList<>();
        for(MultiPhaseAction action:actionList){
            if (action.isFail()&&action.getErrors()!=null){
                result.addAll(action.getErrors());
            }
        }
        return result;
    }

    private boolean needOpenWindow() {
        return openWindowFlag;
    }

    /**
     *
     * @param openWindowFlag
     * @return 
     */
    public ActionComposer setOpenWindow(boolean openWindowFlag) {
        this.openWindowFlag = openWindowFlag;
        return this;
    }

    private boolean needCloseWindow() {
        return closeWindowFlag;
    }

    /**
     *
     * @param closeWindowFlag
     * @return 
     */
    public ActionComposer setCloseWindow(boolean closeWindowFlag) {
        this.closeWindowFlag = closeWindowFlag;
        return this;
    }
}