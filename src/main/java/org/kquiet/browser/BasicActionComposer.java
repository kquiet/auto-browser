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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.kquiet.browser.action.CloseWindow;
import org.kquiet.browser.action.Composable;
import org.kquiet.browser.action.OpenWindow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link BasicActionComposer} provides basic implementation of {@link ActionComposer}.
 * In addition to the actions added by add*() methods, {@link BasicActionComposer} has two
 * extra/internal actions which are executed at the beginning and the end respectively:
 * <ul>
 * <li>The action executed at the beginning is called as <i>Initial Action</i>, which opens a new
 * browser window and set it as <i>focus window</i> with an empty register name.
 * All actions should be executed against this focus window to be isolated from other
 * {@link ActionComposer}, however it could be changed by actions if necessary.
 * If no focus window is specified, it will use the root window of {@link ActionRunner} as its focus
 * window.</li>
 * <li>The action executed at the end is called as <i>Final Action</i>, which closes all registered
 * windows. </li>
 * </ul>
 *
 * @author Kimberly
 */
public class BasicActionComposer extends AbstractActionComposer {
  private static final Logger LOGGER = LoggerFactory.getLogger(BasicActionComposer.class);

  private final Composable initAction = new OpenWindow(true, "");
  private final Composable finalAction = new CloseWindow(true);

  private volatile boolean runOnce = false;
  private volatile boolean isFail = false;
  private volatile boolean skipAction = false;
  private volatile boolean keepFailInfo = true;
  private volatile boolean openWindowFlag = true;
  private volatile boolean closeWindowFlag = true;

  private String failUrl = null;
  private String failPage = null;
  private BasicActionComposer parent = null;
  private BasicActionComposer child = null;

  /**
   * Create an {@link BasicActionComposer}.
   */
  public BasicActionComposer(){
  }

  @Override
  public void run() {
    //ensure run at most once
    if (!runOnce) {
      synchronized (this) {
        if (runOnce) {
          return;
        } else {
          //set this {@link BasicActionComposer} as root context
          executionContextStack.push(this);
          runOnce = true;
        }
      }
    }

    boolean successful = true;
    try {
      totalCostWatch.start();
      boolean anyActionFail = false;

      //run init action first
      if (needOpenWindow()) {
        try {
          perform(initAction);
          anyActionFail = anyActionFail || initAction.isFail();
        } catch (Exception ex) {
          LOGGER.warn("{}({}) init action error:{}", getClass().getSimpleName(), getName(),
              initAction.toString(), ex);
          anyActionFail = true;
        }
      }

      //run main actions
      if (!isFail() && !anyActionFail) {
        int index = 0;
        List<Composable> actionList = new ArrayList<>(super.getAllActionInSequence());
        while (index < actionList.size() && !isFail() && !skipped()) {
          Composable action = actionList.get(index);
          try {
            perform(action);
            anyActionFail = anyActionFail || action.isFail();
          } catch (Exception ex) {
            LOGGER.warn("{}({}) action error:{}", getClass().getSimpleName(), getName(),
                action.toString(), ex);
            anyActionFail = true;
          }
          //break when any action fail
          if (anyActionFail || skipped()) {
            break;
          }
          actionList = new ArrayList<>(super.getAllActionInSequence());
          index++;
        }
      }

      if (isFail() || anyActionFail) {
        successful = false;
      }
    } catch (Exception ex) {
      successful = false;
      LOGGER.warn("{}({}) run error", getClass().getSimpleName(), getName(), ex);
    }

    //run final action & result functions
    try {
      if (successful) {
        runSuccess();
      } else {
        runFail();
      }
      runDone();
      complete(null);
    } catch (Exception ex) {
      completeExceptionally(ex);
      LOGGER.warn("{}({}) run error", getClass().getSimpleName(), getName(), ex);
    }
  }

  private void runFail() {
    skipToFail();

    //keep fail info=>this may take about one second to do;uses a flag to avoid when necessary
    if (keepFailInfo) {
      try {
        setFailInfo(getWebDriver().getCurrentUrl(), getWebDriver().getPageSource());
      } catch (Exception e) {
        LOGGER.warn("{}({}) set fail info error!", getClass().getSimpleName(), getName(), e);
      }
    }

    try {
      getFailFunction().accept(this);
    } catch (Exception e) {
      LOGGER.warn("{}({}) fail function error", getClass().getSimpleName(), getName(), e);
    }
  }

  private void runSuccess() {
    try {
      getSuccessFunction().accept(this);
    } catch (Exception e) {
      LOGGER.warn("{}({}) success function error", getClass().getSimpleName(), getName(), e);
    }
  }

  private void runDone() {
    try {
      getDoneFunction().accept(this);
    } catch (Exception e) {
      LOGGER.warn("{}({}) done function error", getClass().getSimpleName(), getName(), e);
    }

    //run final action to close window
    if (needCloseWindow()) {
      try {
        perform(finalAction);
      } catch (Exception ex) {
        LOGGER.warn("{}({}) final action error:{}", getClass().getSimpleName(), getName(),
            finalAction.toString(), ex);
      }
    }

    totalCostWatch.stop();
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("{}({}) costs {} milliseconds", getClass().getSimpleName(), getName(),
          getCostTime().toMillis());
    }

    //continue with child composer
    if (hasChild()) {
      try {
        BasicActionComposer tempChild = getChild();
        //use parent's focus window as child's when parent doesn't close window but child needs one
        if (!this.needCloseWindow() && !tempChild.needOpenWindow()) {
          tempChild.setFocusWindow(this.getFocusWindow());
          tempChild.registerWindow("", tempChild.getFocusWindow());
        }
        getActionRunner().executeComposer(tempChild);
      } catch (Exception ex) {
        LOGGER.warn("{}({}) execute child composer error", getClass().getSimpleName(),
            getName(), ex);
      }
    }
  }

  @Override
  public BasicActionComposer setActionRunner(ActionRunner actionRunner) {
    super.setActionRunner(actionRunner);
    return this;
  }


  @Override
  public BasicActionComposer setVariable(String variableName, Object value) {
    super.setVariable(variableName, value);
    return this;
  }

  /**
   * Delegate the execution of given child {@link BasicActionComposer} to associated
   * {@link ActionRunner} after this {@link BasicActionComposer} is done.
   * Every {@link BasicActionComposer} has at most one parent/child {@link BasicActionComposer}.
   * If this {@link BasicActionComposer} already has a child {@link BasicActionComposer}, the
   * <i>original</i> child {@link BasicActionComposer} will be postponed.
   * 
   * <p>For example, before calling this method:
   * ComposerA-&gt;ChildOfComposerA-&gt;GrandChildOfComposerA;
   * after: ComposerA-&gt;NewChildOfComposerA-&gt;ChildOfComposerA-&gt;GrandChildOfComposerA.</p>
   * 
   * <p>This method works differently as methods of {@link java.util.concurrent.CompletableFuture}.
   * It just keeps the reference of child {@link BasicActionComposer} and this
   * {@link BasicActionComposer} will hand it to associated {@link ActionRunner} when finished, and
   * then let the {@link ActionRunner} schedule the execution.</p>
   * 
   * @param childActionComposer the {@link BasicActionComposer} to be executed
   * @return child {@link BasicActionComposer}
   */
  public BasicActionComposer continueWith(BasicActionComposer childActionComposer) {
    if (childActionComposer == null) {
      return this;
    }

    //inspect if already has child
    BasicActionComposer oldChild = this.getChild();

    //continue with child
    this.setChild(childActionComposer);
    childActionComposer.setParent(this);

    //insert oldChild to the end of the action chain
    if (oldChild != null) {
      BasicActionComposer temp = this.getChild();
      while (temp.hasChild()) {
        temp = temp.getChild();
      }
      temp.setChild(oldChild);
    }
    return childActionComposer;
  }

  /**
   * Check whether this {@link BasicActionComposer} has child {@link BasicActionComposer}.
   * 
   * @return {@code true} whether this {@link BasicActionComposer} has child
   *     {@link BasicActionComposer}; {@code false} otherwise
   */
  public boolean hasChild() {
    return child != null;
  }

  /**
   * Get child {@link BasicActionComposer}.
   * 
   * @return child {@link BasicActionComposer} if exists; {@code null} otherwise
   */
  public BasicActionComposer getChild() {
    return child;
  }

  private void setChild(BasicActionComposer child) {
    this.child = child;
  }

  /**
   * Check whether this {@link BasicActionComposer} has parent {@link BasicActionComposer}.
   * 
   * @return {@code true} whether this {@link BasicActionComposer} has parent
   *     {@link BasicActionComposer}; {@code false} otherwise
   */
  public boolean hasParent() {
    return parent != null;
  }

  /**
   * Get parent {@link BasicActionComposer}.
   * 
   * @return parent {@link BasicActionComposer} if exists; {@code null} otherwise
   */
  public BasicActionComposer getParent() {
    return parent;
  }

  private void setParent(BasicActionComposer parent) {
    this.parent = parent;
  }

  @Override
  public BasicActionComposer onFail(Consumer<ActionComposer> onFailFunc) {
    super.onFail(onFailFunc);
    return this;
  }

  @Override
  public BasicActionComposer onSuccess(Consumer<ActionComposer> onSuccessFunc) {
    super.onSuccess(onSuccessFunc);
    return this;
  }

  @Override
  public BasicActionComposer onDone(Consumer<ActionComposer> onDoneFunc) {
    super.onDone(onDoneFunc);
    return this;
  }

  @Override
  public BasicActionComposer setName(String name) {
    super.setName(name);
    return this;
  }

  @Override
  public BasicActionComposer setPriority(int priority) {
    super.setPriority(priority);
    return this;
  }    

  @Override
  public boolean skipped() {
    return skipAction;
  }

  /**
   * Check if this {@link BasicActionComposer} has been marked as failed.
   * 
   * @return {@code true} if this {@link BasicActionComposer} has been marked as failed;
   *     {@code false} otherwise
   */
  @Override
  public boolean isFail() {
    return isFail;
  }

  /**
   * Check if this {@link BasicActionComposer} is done without being marked as failed.
   * 
   * @return {@code true} if this {@link BasicActionComposer} is done without being marked as
   *     failed; {@code false} otherwise
   */
  @Override
  public boolean isSuccessfulDone() {
    return isDone() && !isFail;
  }

  @Override
  public String getFailUrl() {
    return failUrl;
  }

  @Override
  public String getFailPage() {
    return failPage;
  }

  private void setFailInfo(String failUrl, String failPage) {
    this.failUrl = failUrl;
    this.failPage = failPage;
  }

  @Override
  public BasicActionComposer keepFailInfo(boolean flag) {
    this.keepFailInfo = flag;
    return this;
  }

  @Override
  public BasicActionComposer setFocusWindow(String windowIdentity) {
    super.setFocusWindow(windowIdentity);
    return this;
  }

  @Override
  public BasicActionComposer addToHead(Composable action) {
    super.addToHead(action);
    return this;
  }

  @Override
  public BasicActionComposer addToTail(Composable action) {
    super.addToTail(action);
    return this;
  }

  @Override
  public BasicActionComposer addToPosition(Composable action, int position) {
    super.addToPosition(action, position);
    return this;
  }

  @Override
  protected List<Composable> getAllActionInSequence() {
    List<Composable> actionList = new ArrayList<>();
    if (initAction != null) {
      actionList.add(initAction);
    }
    actionList.addAll(super.getAllActionInSequence());
    if (finalAction != null) {
      actionList.add(finalAction);
    }
    return actionList;
  }

  /**
   * Skip the execution of remaining actions and mark this {@link BasicActionComposer} as failed.
   */
  @Override
  public void skipToFail() {
    isFail = true;
    skipAction = true;
  }

  /**
   * Skip the execution of remaining actions.
   */
  @Override
  public void skipToSuccess() {
    isFail = false;
    skipAction = true;
  }

  private boolean needOpenWindow() {
    return openWindowFlag;
  }

  /**
   * Determine whether open a window as focus window at the begining.
   * 
   * @param openWindowFlag {@code true}: open; {@code false}: not open
   * @return self reference
   */
  public BasicActionComposer setOpenWindow(boolean openWindowFlag) {
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
   * @return self reference
   */
  public BasicActionComposer setCloseWindow(boolean closeWindowFlag) {
    this.closeWindowFlag = closeWindowFlag;
    return this;
  }
}