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
import java.util.List;
import java.util.stream.Collectors;
import org.kquiet.browser.ActionComposer;
import org.kquiet.browser.action.exception.ActionException;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link SendKey} is a subclass of {@link MultiPhaseAction} which types into an element. {@link
 * org.openqa.selenium.StaleElementReferenceException} may happen while {@link Select} tries to
 * manipulate the element, so multi-phase is used to perform the action again.
 *
 * @author Kimberly
 */
public class SendKey extends MultiPhaseAction {
  private static final Logger LOGGER = LoggerFactory.getLogger(SendKey.class);

  private final By by;
  private final List<By> frameBySequence = new ArrayList<>();
  private final CharSequence[] keysToSend;
  private final boolean clearBeforeSend;

  /**
   * Create an action to input keys to a specified element.
   *
   * @param by the element locating mechanism
   * @param clearBeforeSend {@code true}: clear before sending; {@code false}: send without clearing
   *     first
   * @param keysToSend character sequence to send to the element
   */
  public SendKey(By by, boolean clearBeforeSend, CharSequence... keysToSend) {
    this(by, null, clearBeforeSend, keysToSend);
  }

  /**
   * Create an action to input keys to a specified element.
   *
   * @param by the element locating mechanism
   * @param frameBySequence the sequence of the frame locating mechanism for the element resides in
   *     frame(or frame in another frame and so on)
   * @param clearBeforeSend {@code true}: clear before sending; {@code false}: send without clearing
   *     first
   * @param keysToSend character sequence to send to the element
   */
  public SendKey(
      By by, List<By> frameBySequence, boolean clearBeforeSend, CharSequence... keysToSend) {
    this.by = by;
    if (frameBySequence != null) {
      this.frameBySequence.addAll(frameBySequence);
    }
    this.clearBeforeSend = clearBeforeSend;
    this.keysToSend = purifyCharSequences(keysToSend);
  }

  private CharSequence[] purifyCharSequences(CharSequence... sequence) {
    if (sequence == null) {
      sequence = new CharSequence[] {};
    } else {
      for (int i = 0; i < sequence.length; i++) {
        if (sequence[i] == null) {
          sequence[i] = "";
        }
      }
    }
    return sequence;
  }

  /**
   * Clicks an element and then types into it.
   *
   * @param element the element to send keys to
   * @param clearBeforeSend {@code true}: clear before sending; {@code false}: send without clearing
   *     first
   * @param keysToSend character sequence to send to the element
   */
  public static void clickToSendKeys(
      WebElement element, boolean clearBeforeSend, CharSequence... keysToSend) {
    // click before send key
    element.click();
    if (clearBeforeSend) {
      element.clear();
      element.sendKeys(Keys.BACK_SPACE);
    } else {
      element.sendKeys(Keys.END);
    }
    element.sendKeys(keysToSend);
  }

  @Override
  protected void performMultiPhase() {
    ActionComposer actionComposer = this.getComposer();
    try {
      // firefox doesn't switch focus to top after switch to window, so recovery step is required
      switchToTopForFirefox();
      actionComposer.switchToInnerFrame(this.frameBySequence);
      WebElement element = actionComposer.getWebDriver().findElement(this.by);
      clickToSendKeys(element, this.clearBeforeSend, this.keysToSend);
      noNextPhase();
    } catch (StaleElementReferenceException ignoreE) {
      // with next phase when StaleElementReferenceException is encountered
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(
            "{}({}): encounter stale element:{}",
            ActionComposer.class.getSimpleName(),
            actionComposer.getName(),
            toString(),
            ignoreE);
      }
    } catch (Exception e) {
      noNextPhase();
      throw new ActionException(toString(), e);
    }
  }

  @Override
  public String toString() {
    return String.format(
        "%s:%s/%s/%s",
        SendKey.class.getSimpleName(),
        by.toString(),
        String.join(",", keysToSend),
        String.join(
            ",", frameBySequence.stream().map(s -> s.toString()).collect(Collectors.toList())));
  }
}
