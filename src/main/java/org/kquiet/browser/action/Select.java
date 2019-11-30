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

package org.kquiet.browser.action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.kquiet.browser.ActionComposer;
import org.kquiet.browser.action.exception.ActionException;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link Select} is a subclass of {@link MultiPhaseAction} which selects/deselects options on a
 * SELECT element.
 * {@link org.openqa.selenium.StaleElementReferenceException} may happen while {@link Select} tries
 * to manipulate the element, so multi-phase is used to perform the action again.
 * 
 * @author Kimberly
 */
public class Select extends MultiPhaseAction {
  private static final Logger LOGGER = LoggerFactory.getLogger(Select.class);

  /**
   * The way to perform the selecting.
   */
  public static enum SelectBy {

    /**
     * Select/deselect the option by index.
     */
    Index,

    /**
     * Select/deselect the option by value.
     */
    Value,

    /**
     * Select/deselect the option by text.
     */
    Text
  }

  private final By by;
  private final List<By> frameBySequence = new ArrayList<>();
  private final SelectBy selectBy;
  private final Object[] options;

  /**
   * Create an action to perform selection against a specific element.
   * 
   * @param by the element locating mechanism
   * @param selectBy the way to perform the selecting
   * @param options the option to select; all options are deselected when no option is supplied and
   *     the SELECT element supports selecting multiple options
   */
  public Select(By by, SelectBy selectBy, Object... options) {
    this(by, null, selectBy, options);
  }

  /**
   * Create an action to perform selection against a specific element.
   * 
   * @param by the element locating mechanism
   * @param frameBySequence the sequence of the frame locating mechanism for the element resides in
   *     frame(or frame in another frame and so on)
   * @param selectBy the way to perform the selecting
   * @param options the option to select; all options are deselected when no option is supplied and
   *     the SELECT element supports selecting multiple options
   */
  public Select(By by, List<By> frameBySequence, SelectBy selectBy, Object... options) {
    this.by = by;
    if (frameBySequence != null) {
      this.frameBySequence.addAll(frameBySequence);
    }
    this.selectBy = selectBy;
    this.options = options;
  }

  /**
   * Clicks an element and then performs selecting on it.
   * 
   * @param element the element to perform the selecting
   * @param selectBy the way to perform the selecting
   * @param options the option to select; all options are deselected when no option is supplied and
   *     the SELECT element supports selecting multiple options
   */
  public static void clickToSelect(WebElement element, SelectBy selectBy, Object... options) {
    element.click();
    org.openqa.selenium.support.ui.Select elementToSelect = 
        new org.openqa.selenium.support.ui.Select(element);
    if ((options == null || options.length == 0) && elementToSelect.isMultiple()) {
      elementToSelect.deselectAll();
    } else if (options != null) {
      switch (selectBy) {
        case Index:
          for (Object obj: options) {
            elementToSelect.selectByIndex((Integer)obj);
          }
          break;
        case Value:
          for (Object obj: options) {
            elementToSelect.selectByValue((String)obj);
          }
          break;
        default:
        case Text:
          for (Object obj: options) {
            elementToSelect.selectByVisibleText((String)obj);
          }
          break;
      }
    }
  }

  @Override
  protected void performMultiPhase() {
    ActionComposer actionComposer = this.getComposer();
    try {
      //firefox doesn't switch focus to top after switch to window, so recovery step is required
      switchToTopForFirefox(); 
      actionComposer.switchToInnerFrame(this.frameBySequence);
      WebElement element = actionComposer.getWebDriver().findElement(this.by);
      clickToSelect(element, this.selectBy, this.options);
      noNextPhase();
    } catch (StaleElementReferenceException ignoreE) {
      //with next phase when StaleElementReferenceException is encountered
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("{}({}): encounter stale element:{}", ActionComposer.class.getSimpleName(),
            actionComposer.getName(), toString(), ignoreE);
      }
    } catch (Exception e) {
      noNextPhase();
      throw new ActionException(toString(), e);
    }
  }

  @Override
  public String toString() {
    return String.format("%s:%s/%s/%s/%s",
        Select.class.getSimpleName(), by.toString(), selectBy.toString(),
        String.join(",", Arrays.asList(options).stream().map(
            s -> s.toString()).collect(Collectors.toList())),
        String.join(",",frameBySequence.stream().map(
            s -> s.toString()).collect(Collectors.toList())));
  }
}