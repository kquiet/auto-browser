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
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import org.openqa.selenium.WebDriver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.kquiet.utility.Stopwatch;
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
 * <ul>
 * <li>The action executed at the beginning is called as <i>Initial Action</i>, which opens a new browser window and set it as <i>focus window</i> with an empty register name.
 * All actions should be executed against this focus window to be isolated from other {@link ActionComposer}, however it could be changed by actions if necessary.
 * If no focus window is specified, it will use the root window of {@link ActionRunner} as its focus window.</li>
 * <li>The action executed at the end is called as <i>Final Action</i>, which closes all registered windows. </li>
 * </ul>
 *
 * @author Kimberly
 */
public class ActionComposer implements RunnableFuture<ActionComposer>, Prioritized {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActionComposer.class);
    
    private ActionRunner actionRunner = null;

    private final MultiPhaseAction initAction = new IfThenElse(ac->this.needOpenWindow(), Arrays.asList(new OpenWindow(true, "")), null).setContainingComposer(this);
    private final MultiPhaseAction finalAction = new IfThenElse(ac->this.needCloseWindow(), Arrays.asList(new CloseWindow(true)), null).setContainingComposer(this);
    private final Deque<MultiPhaseAction> mainActionList = new LinkedList<>();
    
    private Consumer<ActionComposer> onFailFunc = (bac)->{};
    private Consumer<ActionComposer> onSuccessFunc = (bac)->{};
    private Consumer<ActionComposer> onDoneFunc = (bac)->{};
    
    private final CountDownLatch checkDoneLatch = new CountDownLatch(1);

    private String name = null;
    private int priority = Integer.MIN_VALUE;
    private final Stopwatch totalCostWatch = new Stopwatch();
    
    private volatile boolean isFail = false;
    private volatile boolean isDone = false;
    private volatile boolean isCancelled = false;
    private volatile boolean skipAction = false;
    private volatile boolean skipResultFunction = false;
    private volatile boolean keepFailInfo = true;
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
            
            //run init action first
            try{
                initAction.run();
                anyActionFail = anyActionFail || initAction.isFail();
            }catch(Exception ex){
                LOGGER.warn("{}({}) init action error:{}", ActionComposer.class.getSimpleName(), getName(), initAction.toString(), ex);
                anyActionFail = true;
            }
            
            //run other action
            if (!isFail() && !anyActionFail){
                int index=0;
                List<MultiPhaseAction> actionList = new ArrayList<>(mainActionList);
                while(index<actionList.size() && !isFail() && !isSkipAction()){
                    MultiPhaseAction action = actionList.get(index);
                    try{
                        action.run();
                        anyActionFail = anyActionFail || action.isFail();
                    }catch(Exception ex){
                        LOGGER.warn("{}({}) action error:{}", ActionComposer.class.getSimpleName(), getName(), action.toString(), ex);
                        anyActionFail = true;
                    }
                    //break when any action fail
                    if (isFail() || anyActionFail) break;
                    actionList = new ArrayList<>(mainActionList);
                    index++;
                }
            }
            
            if (isFail() || anyActionFail) runFail();
            else runSuccess();
        }catch(Exception ex){
            LOGGER.warn("{}({}) run error", ActionComposer.class.getSimpleName(), getName(), ex);
            runFail();
        }finally{
            runDone();
        }
    }
    
    private void runFail(){
        skipToFail();
            
        //keep fail info=>this may take about one second to do;uses a flag to avoid when necessary
        if (keepFailInfo){
            try{
                setFailInfo(getWebDriver().getCurrentUrl(), getWebDriver().getPageSource());
            }catch(Exception e){
                LOGGER.warn("{}({}) set fail info error!", ActionComposer.class.getSimpleName(), getName(), e);
            }
        }
        
        //run final action after keeping fail info
        try{
            finalAction.run();
        }catch(Exception ex){
            LOGGER.warn("{}({}) final action error:{}", ActionComposer.class.getSimpleName(), getName(), finalAction.toString(), ex);
        }

        if (isSkipResultFunction()) return;
        try{
            onFailFunc.accept(this);
        }catch(Exception e){
            LOGGER.warn("{}({}) fail function error", ActionComposer.class.getSimpleName(), getName(), e);
        }
    }
    
    private void runSuccess(){
        try{
            finalAction.run();
        }catch(Exception ex){
            LOGGER.warn("{}({}) final action error", ActionComposer.class.getSimpleName(), getName(), finalAction.toString(), ex);
        }
                
        if (isSkipResultFunction()) return;
        try{
            onSuccessFunc.accept(this);
        }catch(Exception e){
            LOGGER.warn("{}({}) success function error", ActionComposer.class.getSimpleName(), getName(), e);
        }
    }
    
    private void runDone(){
        try{
            onDoneFunc.accept(this);
        }catch(Exception e){
            LOGGER.warn("{}({}) done function error", ActionComposer.class.getSimpleName(), getName(), e);
        }

        setDone();
        checkDoneLatch.countDown();
        if (LOGGER.isDebugEnabled()) LOGGER.debug("{}({}) costs {} milliseconds", ActionComposer.class.getSimpleName(), getName(), getCostTime().toMillis());
        
        //continue with child composer
        if (hasChild()){
            try{
                ActionComposer tempChild = getChild();
                //use parent's focus window as child's focus window when parent doesn't close window & child doesn't open window
                if (!this.needCloseWindow() && !tempChild.needOpenWindow()){
                    tempChild.setFocusWindow(this.getFocusWindow());
                    tempChild.registerWindow("", tempChild.getFocusWindow());
                }
                actionRunner.executeComposer(tempChild);
            }catch(Exception ex){
                LOGGER.warn("{}({}) execute child composer error", ActionComposer.class.getSimpleName(), getName(), ex);
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
        name = Optional.ofNullable(name).orElse("");
        if (windowIdentity==null || windowIdentity.isEmpty() || registeredWindows.containsKey(name)) return false;
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
     * @return all windows(register name and window identity) of registered windows
     */
    public Map<String, String> getRegisteredWindows(){
        return new LinkedHashMap<>(registeredWindows);
    }
    
    /**
     * Delegate the execution of {@link java.lang.Runnable runnable} to associated {@link ActionRunner} with invoking {@link ActionComposer}'s priority
     * 
     * @param runnable the object whose run method will be invoked
     * @return a {@link java.util.concurrent.Future Future} representing pending completion of given {@link java.lang.Runnable runnable}.
     * {@link java.util.concurrent.Future#get() Future's get methods} returns {@code null} if no exception occurred.
     */
    public Future<Exception> callBrowser(Runnable runnable){
        return actionRunner.executeAction(runnable, getPriority());
    }
    
    /**
     * Delegate the execution of given child {@link ActionComposer} to associated {@link ActionRunner} after invoking {@link ActionComposer} is done.
     * Every {@link ActionComposer} has at most one parent/child {@link ActionComposer}.
     * If invoking {@link ActionComposer} already has a child {@link ActionComposer}, the <i>original</i> child {@link ActionComposer} will be postponed.
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
     * @return {@code true} if invoking {@link ActionComposer} has child {@link ActionComposer}; {@code false} otherwise
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
     * @return {@code true} if invoking {@link ActionComposer} has parent {@link ActionComposer}; {@code false} otherwise
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
     * Switch browser's focus window to invoking {@link ActionComposer}'s focus window.
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
            getWebDriver().switchTo().window(windowIdentity);
            return true;
        }catch(Exception ex){
            LOGGER.warn("{} switchToWindow error", getName(), ex);
            return false;
        }
    }

    /**
     * Set the callback function to be executed when invoking {@link ActionComposer} is marked as failed.
     * 
     * @param onFailFunc the callback function to be executed
     * @return invoking {@link ActionComposer}
     */
    public ActionComposer setOnFailFunction(Consumer<ActionComposer> onFailFunc) {
        if (onFailFunc != null) this.onFailFunc = onFailFunc;
        return this;
    }
    
    /**
     * Set the callback function to be executed when invoking {@link ActionComposer} is finished without being marked as failed.
     * 
     * @param onSuccessFunc the callback function to be executed
     * @return invoking {@link ActionComposer}
     */
    public ActionComposer setOnSuccessFunction(Consumer<ActionComposer> onSuccessFunc) {
        if (onSuccessFunc != null) this.onSuccessFunc = onSuccessFunc;
        return this;
    }
    
    /**
     * Set the callback function to be executed when invoking {@link ActionComposer} is done.
     * This callback function is executed after <i>fail function</i> and <i>success function</i>.
     * 
     * @param onDoneFunc the callback function to be executed
     * @return invoking {@link ActionComposer}
     */
    public ActionComposer setOnDoneFunction(Consumer<ActionComposer> onDoneFunc) {
        if (onDoneFunc != null) this.onDoneFunc = onDoneFunc;
        return this;
    }
    
    /**
     *
     * @return the name of invoking {@link ActionComposer}
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of invoking {@link ActionComposer}.
     * 
     * @param name name
     * @return invoking {@link ActionComposer}
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
     * Set the priority of invoking {@link ActionComposer}.
     * 
     * @param priority priority
     * @return invoking {@link ActionComposer}
     */
    public ActionComposer setPriority(int priority) {
        this.priority = priority;
        return this;
    }    
    
    /**
     *
     * @return total execution time of invoking {@link ActionComposer}
     */
    public Duration getCostTime() {
        return totalCostWatch.getDuration();
    }
    
    private boolean isSkipAction(){
        return skipAction;
    }
    
    private boolean isSkipResultFunction(){
        return skipResultFunction;
    }
    
    /**
     *
     * @return {@code true} if invoking {@link ActionComposer} has been marked as failed; {@code false} otherwise
     */
    public boolean isFail(){
        return isFail;
    }
    
    /**
     *
     * @return {@code true} if invoking {@link ActionComposer} is done without being marked as failed; {@code false} otherwise
     */
    public boolean isSuccessfulDone(){
        return isDone && !isFail;
    }

    private void setDone() {
        isDone = true;
        totalCostWatch.stop();
    }

    /**
     *
     * @return the url of the last page when invoking {@link ActionComposer} is marked as failed and {@link #keepFailInfo(boolean) keep fail info} is enabled; {@code null} otherwise
     */
    public String getFailUrl() {
        return failUrl;
    }

    /**
     *
     * @return the content of the last page when invoking {@link ActionComposer} is marked as failed and {@link #keepFailInfo(boolean) keep fail info} is enabled; {@code null} otherwise
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
     * Enable/Disable the function of keeping fail information when invoking {@link ActionComposer} is marked as failed.
     * The function of keeping fail information takes about one second to complete, however this may seem wasteful in many applications,
     * hence this method can be used to determine keep or not.
     * 
     * @param flag {@code true} to enable; {@code false} to disable
     * @return invoking {@link ActionComposer}
     */
    public ActionComposer keepFailInfo(boolean flag){
        this.keepFailInfo = flag;
        return this;
    }

    /**
     * Get {@link org.openqa.selenium.WebDriver WebDriver} from associated {@link ActionRunner}.
     * Use it with caution because the associated {@link ActionRunner} use the same {@link org.openqa.selenium.WebDriver WebDriver} when executing browser actions,
     * however {@link org.openqa.selenium.WebDriver WebDriver} is <a href="https://github.com/SeleniumHQ/selenium/wiki/Frequently-Asked-Questions#q-is-webdriver-thread-safe" target="_blank">not thread-safe</a>.
     * 
     * <p>A safer way to use this is to encapsulate the process in a {@link java.lang.Runnable Runnable}, or use built-in {@link org.kquiet.browser.action.Custom custom action},
     * and then execute it through {@link ActionRunner#executeAction(java.lang.Runnable, int)}.</p>
     * 
     * @return the {@link org.openqa.selenium.WebDriver WebDriver} from associated {@link ActionRunner}.
     */
    public WebDriver getWebDriver() {
        return actionRunner.getWebDriver();
    }

    /**
     *
     * @return the window identity of focus window
     */
    public String getFocusWindow() {
        //set focus window to root window if not set
        if (focusWindowIdentity==null){
            focusWindowIdentity = getRootWindow();
        }
        return focusWindowIdentity;
    }

    /**
     * @param windowIdentity the window identity to set as the focus window
     * @return invoking {@link ActionComposer}
     */
    public ActionComposer setFocusWindow(String windowIdentity) {
        this.focusWindowIdentity = windowIdentity;
        return this;
    }
    
    /**
     * Add action to the last position of list.
     * 
     * @param action action to add
     */
    public void addActionToLast(MultiPhaseAction action){
        if (action==null) return;
        synchronized(this){
            mainActionList.addLast(action);
            action.setContainingComposer(this);
        }
    }
    
    /**
     * Add action to the first position of list.
     *
     * @param action action to add
     */
    public void addActionToFirst(MultiPhaseAction action){
        if (action==null) return;
        synchronized(this){
            mainActionList.addFirst(action);
            action.setContainingComposer(this);
        }
    }
    
    /**
     * Add action to the specified position of list.
     * 
     * @param action action to add
     * @param position the position(zero-based) to add action
     */
    public void addActionToIndex(MultiPhaseAction action, int position){
        if (action==null || position<0 || position>mainActionList.size()) return;
        synchronized(this){
            final Deque<MultiPhaseAction> temp = new LinkedList<>(mainActionList);
            final Deque<MultiPhaseAction> stack = new LinkedList<>();
            for (int i=0;i<position;i++){
                stack.addFirst(temp.removeFirst());
            }
            temp.addFirst(action);
            for (int i=0;i<position;i++){
                temp.addFirst(stack.removeFirst());
            }
            mainActionList.clear();
            mainActionList.addAll(temp);
            action.setContainingComposer(this);
        }
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
     * Skip the execution of remaining actions and mark invoking {@link ActionComposer} as failed.
     */
    public void skipToFail(){
        isFail = true;
        skipAction = true;
    }
    
    /**
     * Skip the execution of remaining actions.
     */
    public void skipToSuccess(){
        isFail = false;
        skipAction = true;
    }
    
    private void skipAll(){
        skipAction = true;
        skipResultFunction = true;
    }
    
    /**
     * 
     * @return exception list from executed actions
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
     * Determine whether open a window as focus window at the begining.
     * 
     * @param openWindowFlag {@code true}: open; {@code false}: not open
     * @return invoking {@link ActionComposer}
     */
    public ActionComposer setOpenWindow(boolean openWindowFlag) {
        this.openWindowFlag = openWindowFlag;
        return this;
    }

    private boolean needCloseWindow() {
        return closeWindowFlag;
    }

    /**
     * Determine whether close all registered windows at the end.
     * 
     * @param closeWindowFlag {@code true}: close; {@code false}: not close
     * @return invoking {@link ActionComposer}
     */
    public ActionComposer setCloseWindow(boolean closeWindowFlag) {
        this.closeWindowFlag = closeWindowFlag;
        return this;
    }
}