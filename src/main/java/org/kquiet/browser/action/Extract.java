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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.kquiet.browser.ActionComposer;
import org.kquiet.browser.action.exception.ActionException;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link Extract} is a subclass of {@link MultiPhaseAction} which extract information from element
 * by phases to avoid blocking the execution of other browser actions.
 * 
 * @author Kimberly
 */
public class Extract extends MultiPhaseAction {
  private static final Logger LOGGER = LoggerFactory.getLogger(Extract.class);

  private final By by;
  private final List<By> frameBySequence = new ArrayList<>();
  private final String textVariableName;
  private final Map<String, String> attrVariableNames = new LinkedHashMap<>();

  /**
   * Create an action to extract information from element.
   * 
   * @param by the element locating mechanism
   * @param textVariableName text variable name; non-empty name means to get the visible (i.e. not
   *     hidden by CSS) text of the element(including sub-elements) as a variable of
   *     {@link ActionComposer}
   */
  public Extract(By by, String textVariableName) {
    this(by, null, textVariableName, null);
  }

  /**
   * Create an action to extract information from element.
   * 
   * @param by the element locating mechanism
   * @param frameBySequence the sequence of the frame locating mechanism for the element resides in
   *     frame(or frame in another frame and so on)
   * @param textVariableName text variable name; non-empty name means to get the visible (i.e. not
   *     hidden by CSS) text of the element(including sub-elements) as a variable of
   *     {@link ActionComposer}
   */
  public Extract(By by, List<By> frameBySequence, String textVariableName) {
    this(by, frameBySequence, textVariableName, null);
  }

  /**
   * Create an action to extract information from element.
   * 
   * @param by the element locating mechanism
   * @param attrVariableNames (attribute name, variable name) pairs to set as variables. For each
   *     pair, {@link Extract} sets the property value of the element as a variable of
   *     {@link ActionComposer} if the property exists. If property doesn't exists, {@link Extract}
   *     sets the attribute value of the element as a variable of {@link ActionComposer}. If neither
   *     exists, null-value variable is set.
   */
  public Extract(By by, Map<String, String> attrVariableNames) {
    this(by, null, null, attrVariableNames);
  }

  /**
   * Create an action to extract information from element.
   * 
   * @param by the element locating mechanism
   * @param frameBySequence the sequence of the frame locating mechanism for the element resides in
   *     frame(or frame in another frame and so on)
   * @param attrVariableNames (attribute name, variable name) pairs to set as variables. For each
   *     pair, {@link Extract} sets the property value of the element as a variable of
   *     {@link ActionComposer} if the property exists. If property doesn't exists, {@link Extract}
   *     sets the attribute value of the element as a variable of {@link ActionComposer}. If neither
   *     exists, null-value variable is set.
   */
  public Extract(By by, List<By> frameBySequence, Map<String, String> attrVariableNames) {
    this(by, frameBySequence, null, attrVariableNames);
  }

  /**
   * Create an action to extract information from element.
   * 
   * @param by the element locating mechanism
   * @param textVariableName text variable name; non-empty name means to get the visible (i.e. not
   *     hidden by CSS) text of the element(including sub-elements) as a variable of 
   *     {@link ActionComposer}
   * @param attrVariableNames (attribute name, variable name) pairs to set as variables. For each
   *     pair, {@link Extract} sets the property value of the element as a variable of
   *     {@link ActionComposer} if the property exists. If property doesn't exists, {@link Extract}
   *     sets the attribute value of the element as a variable of {@link ActionComposer}. If
   *     neither exists, null-value variable is set.
   */
  public Extract(By by, String textVariableName, Map<String, String> attrVariableNames) {
    this(by, null, textVariableName, attrVariableNames);
  }

  /**
   * Create an action to extract information from element.
   * 
   * @param by the element locating mechanism
   * @param frameBySequence the sequence of the frame locating mechanism for the element resides in
   *     frame(or frame in another frame and so on)
   * @param textVariableName text variable name; non-empty name means to get the visible (i.e. not
   *     hidden by CSS) text of the element(including sub-elements) as a variable of
   *     {@link ActionComposer}
   * @param attrVariableNames (attribute name, variable name) pairs to set as variables. For each
   *     pair, {@link Extract} sets the property value of the element as a variable of
   *     {@link ActionComposer} if the property exists. If property doesn't exists, {@link Extract}
   *     sets the attribute value of the element as a variable of {@link ActionComposer}. If
   *     neither exists, null-value variable is set.
   */
  public Extract(By by, List<By> frameBySequence, String textVariableName,
      Map<String, String> attrVariableNames) {
    this.by = by;
    if (frameBySequence != null) {
      this.frameBySequence.addAll(frameBySequence);
    }
    this.textVariableName = Optional.ofNullable(textVariableName).orElse("");
    if (attrVariableNames != null) {
      this.attrVariableNames.putAll(attrVariableNames);
    }
  }

  @Override
  protected void performMultiPhase() {
    ActionComposer actionComposer = this.getComposer();
    try {
      //firefox doesn't switch focus to top after switch to window,
      //so recovery step is required
      switchToTopForFirefox(); 
      actionComposer.switchToInnerFrame(this.frameBySequence);
      WebElement element = actionComposer.getWebDriver().findElement(this.by);

      //get text when necessary
      if (!this.textVariableName.isEmpty()) {
        actionComposer.setVariable(this.textVariableName, element.getText());
      }

      //get attribute when necessary
      for (Map.Entry<String, String> entry: this.attrVariableNames.entrySet()) {
        actionComposer.setVariable(entry.getValue(), element.getAttribute(entry.getKey()));
      }
      noNextPhase();
    } catch (StaleElementReferenceException ignoreE) {
      //with next phase when StaleElementReferenceException is encountered
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("{}({}): encounter stale element:{}", ActionComposer.class.getSimpleName(),
            actionComposer.getName(), toString(), ignoreE);
      }
    } catch (Exception e) {
      noNextPhase();
      throw new ActionException(e);
    }
  }

  @Override
  public String toString() {
    return String.format("%s:%s/%s/%s/%s", Extract.class.getSimpleName(), by.toString(),
        String.join(",",frameBySequence.stream().map(s -> s.toString())
            .collect(Collectors.toList())), textVariableName,
            String.join(",", attrVariableNames.entrySet().stream().map(
                s -> s.getKey() + ":" + s.getValue()).collect(Collectors.toList())));
  }
}