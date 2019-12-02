/*
 * Copyright 2019 P. Kimberly Chang
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
import java.util.Stack;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

import org.kquiet.browser.action.Composable;
import org.kquiet.utility.Stopwatch;

import org.openqa.selenium.By;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link AbstractActionComposer} implements most methods of {@link ActionComposer} to lay ground
 * works for possible subclasses.
 * {@link AbstractActionComposer} itself is a subclass of {@link CompletableFuture}, so any
 * subclass of {@link AbstractActionComposer} should complete itself explictly in {@link #run()}.
 *
 * @author Kimberly
 */
public abstract class AbstractActionComposer extends CompletableFuture<Void>
    implements ActionComposer {
  
  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractActionComposer.class);

  /**
   * The execution context stack of this {@link AbstractActionComposer}, the
   * {@link DynamicActionSequence} must be pushed into this stack before any action in it gets
   * executed to reflect execution context.
   */
  protected final Stack<DynamicActionSequence> executionContextStack = new Stack<>();

  /**
   * The stop watch used to measure run cost of this {@link AbstractActionComposer}.
   */
  protected final Stopwatch totalCostWatch = new Stopwatch();

  private final Deque<Composable> mainActionList = new LinkedList<>();
  private final Map<String, Object> variableMap = new ConcurrentHashMap<>();
  private final Map<String, String> registeredWindows = new LinkedHashMap<>();

  private int priority = Integer.MIN_VALUE;
  private Consumer<ActionComposer> onFailFunc = ac -> {};
  private Consumer<ActionComposer> onSuccessFunc = ac -> {};
  private Consumer<ActionComposer> onDoneFunc = ac -> {};
  private Function<ActionComposer, Consumer<Composable>> actionPerformedFunc = ac -> at -> {};
  private Function<ActionComposer, Consumer<Composable>> actionPerformingFunc = ac -> at -> {};

  private ActionRunner actionRunner = null;
  private String name = "";
  private String focusWindowIdentity = null;

  /**
   * Create an {@link AbstractActionComposer}.
   */
  public AbstractActionComposer() {
  }

  @Override
  public AbstractActionComposer setActionRunner(ActionRunner actionRunner) {
    this.actionRunner = actionRunner;
    return this;
  }

  /**
   * Get associated {@link ActionRunner}.
   * 
   * @return {@link ActionRunner}
   */
  protected ActionRunner getActionRunner() {
    return actionRunner;
  }


  @Override
  public String getRootWindow() {
    return actionRunner.getRootWindowIdentity();
  }

  @Override
  public boolean registerWindow(String name, String windowIdentity) {
    if (name == null || windowIdentity == null || windowIdentity.isEmpty()) {
      return false;
    }

    if (registeredWindows.containsKey(name)) {
      return false;
    } else {
      registeredWindows.put(name, windowIdentity);
      return true;
    }
  }

  @Override
  public String getRegisteredWindow(String registerName) {
    return registeredWindows.getOrDefault(registerName, "");
  }

  @Override
  public Map<String, String> getRegisteredWindows() {
    return new LinkedHashMap<>(registeredWindows);
  }

  @Override
  public Object getVariable(String variableName) {
    return variableMap.get(variableName);
  }

  @Override
  public AbstractActionComposer setVariable(String variableName, Object value) {
    variableMap.put(variableName, value);
    return this;
  }

  @Override
  public CompletableFuture<Void> callBrowser(Runnable runnable) {
    return actionRunner.executeAction(runnable, getPriority());
  }

  @Override
  public void perform(Composable action) {
    if (action == null) {
      return;
    }
    
    try {
      getActionPerformingFunction().apply(this).accept(action);
    } catch (Exception e) {
      LOGGER.warn("{}({}) action performing function error",
          getClass().getSimpleName(), getName(), e);
      throw e;
    }

    boolean isSequenceContainer = action instanceof DynamicActionSequence;
    try {
      if (isSequenceContainer) {
        executionContextStack.push((DynamicActionSequence)action);
      }
      action.setComposer(this);
      action.perform();
    } finally {
      if (isSequenceContainer) {
        executionContextStack.pop();
      }
    }
    
    try {
      getActionPerformedFunction().apply(this).accept(action);
    } catch (Exception e) {
      LOGGER.warn("{}({}) action performed function error",
          getClass().getSimpleName(), getName(), e);
      throw e;
    }
  }

  @Override
  public boolean switchToFocusWindow() {
    return switchToWindow(getFocusWindow());
  }

  @Override
  public boolean switchToWindow(String windowIdentity) {
    try {
      getWebDriver().switchTo().window(windowIdentity);
      return true;
    } catch (Exception ex) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("{} switchToWindow error", getName(), ex);
      }
      return false;
    }
  }

  @Override
  public void switchToInnerFrame(List<By> frameBySequence) {
    if (frameBySequence != null) {
      WebDriver driver = getWebDriver();
      for (By frameBy: frameBySequence) {
        driver.switchTo().frame(driver.findElement(frameBy));
      }
    }
  }

  @Override
  public boolean switchToTop() {
    try {
      getWebDriver().switchTo().defaultContent();
      return true;
    } catch (UnhandledAlertException ex) {
      //firefox raises an UnhandledAlertException when an alert box is presented, but chrome doesn't
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("{} switchToTop error", getName(), ex);
      }
      return false;
    }
  }

  @Override
  public ActionComposer onFail(Consumer<ActionComposer> onFailFunc) {
    if (onFailFunc != null) {
      this.onFailFunc = onFailFunc;
    } else {
      this.onFailFunc = ac -> {};
    }
    return this;
  }

  /**
   * Get the function executed when failed.
   * 
   * @return the function consuming {@link ActionComposer}
   */
  protected Consumer<ActionComposer> getFailFunction() {
    return onFailFunc;
  }

  @Override
  public ActionComposer onSuccess(Consumer<ActionComposer> onSuccessFunc) {
    if (onSuccessFunc != null) {
      this.onSuccessFunc = onSuccessFunc;
    } else {
      this.onSuccessFunc = ac -> {};
    }
    return this;
  }

  /**
   * Get the function executed when finished without fail.
   * 
   * @return the function consuming {@link ActionComposer}
   */
  protected Consumer<ActionComposer> getSuccessFunction() {
    return onSuccessFunc;
  }

  @Override
  public ActionComposer onDone(Consumer<ActionComposer> onDoneFunc) {
    if (onDoneFunc != null) {
      this.onDoneFunc = onDoneFunc;
    } else {
      this.onDoneFunc = ac -> {};
    }
    return this;
  }

  /**
   * Get the function to be executed after any managed action is performed.
   * 
   * @return the function consuming {@link ActionComposer}
   */
  protected Function<ActionComposer, Consumer<Composable>> getActionPerformedFunction() {
    return actionPerformedFunc;
  }
  
  @Override
  public ActionComposer actionPerformed(Function<ActionComposer, Consumer<Composable>> func) {
    if (func != null) {
      this.actionPerformedFunc = func;
    } else {
      this.actionPerformedFunc = ac -> at -> {};
    }
    return this;
  }
  
  /**
   * Get the function to be executed when any managed action is performed.
   * 
   * @return the function consuming {@link ActionComposer}
   */
  protected Function<ActionComposer, Consumer<Composable>> getActionPerformingFunction() {
    return actionPerformingFunc;
  }
  
  @Override
  public ActionComposer actionPerforming(Function<ActionComposer, Consumer<Composable>> func) {
    if (func != null) {
      this.actionPerformingFunc = func;
    } else {
      this.actionPerformingFunc = ac -> at -> {};
    }
    return this;
  }

  /**
   * Get the function to be executed when finished.
   * 
   * @return the function consuming {@link ActionComposer}
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
   * Get total execution time of this {@link AbstractActionComposer}.
   * 
   * @return the total execution time represented by {@link Duration}
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
    if (focusWindowIdentity == null) {
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
  public AbstractActionComposer addToHead(Composable action) {
    if (action == null) {
      return this;
    }
    synchronized (this) {
      mainActionList.addFirst(action);
    }
    return this;
  }

  @Override
  public AbstractActionComposer addToTail(Composable action) {
    if (action == null) {
      return this;
    }
    synchronized (this) {
      mainActionList.addLast(action);
    }
    return this;
  }

  @Override
  public AbstractActionComposer addToPosition(Composable action, int position) {
    if (action == null) {
      return this;
    }
    synchronized (this) {
      final List<Composable> temp = new ArrayList<>(mainActionList);
      temp.add(position, action);
      mainActionList.clear();
      mainActionList.addAll(temp);
    }
    return this;
  }

  @Override
  public DynamicActionSequence addToHeadByContext(Composable action) {
    DynamicActionSequence context = executionContextStack.peek();
    return context.addToHead(action);
  }

  @Override
  public DynamicActionSequence addToTailByContext(Composable action) {
    DynamicActionSequence context = executionContextStack.peek();
    return context.addToTail(action);
  }

  @Override
  public DynamicActionSequence addToPositionByContext(Composable action, int position) {
    DynamicActionSequence context = executionContextStack.peek();
    return context.addToPosition(action, position);
  }

  /**
   * Get all actions in the action sequence.
   * 
   * @return list of actions
   */
  protected List<Composable> getAllActionInSequence() {
    List<Composable> actionList = new ArrayList<>();
    synchronized (this) {
      actionList.addAll(mainActionList);
    }
    return actionList;
  }

  /**
   * Get the error list from executed actions.
   * 
   * @return a list of {@link Exception}
   */
  @Override
  public List<Exception> getErrors() {
    List<Composable> actionList = getAllActionInSequence();
    List<Exception> result = new ArrayList<>();
    for (Composable action:actionList) {
      if (action.isFail() && action.getErrors() != null) {
        result.addAll(action.getErrors());
      }
    }
    return result;
  }
}