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

package org.kquiet.test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.kquiet.browser.ActionComposer;
import org.kquiet.browser.ActionComposerBuilder;
import org.kquiet.browser.ActionRunner;
import org.kquiet.browser.BasicActionComposer;

/**
 * Test composite of supported actions.
 * @author Kimberly
 */
public class CompositeTest {
  private static ActionRunner browserOne;
  private static ActionRunner browserRunnerTwo;

  public CompositeTest() {
  }

  /**
   * Create two browsers for testing.
   */
  @BeforeAll
  public static void setUpClass() {
    browserOne = TestHelper.createRunner(1);
    browserRunnerTwo = TestHelper.createRunner(2);
  }

  /**
   * Release related resources.
   */
  @AfterAll
  public static void tearDownClass() {
    browserOne.close();
    browserRunnerTwo.close();
  }

  @BeforeEach
  public void setUp(){
  }

  @AfterEach
  public void tearDown() {
  }

  private ActionComposerBuilder getEmptyActionComposerBuilder() {
    return new ActionComposerBuilder();
  }

  @Test
  public void priority() throws Exception {
    final StringBuilder sb = new StringBuilder();
    CountDownLatch ready = new CountDownLatch(6);
    browserOne.executeAction(() -> {
      sb.append("Q100");
      ready.countDown();
      for (int i = 5;i >= 1;i--) {
        int priority = i;
        String str = "Q" + i;
        browserOne.executeAction(() -> {
          synchronized (sb) {
            sb.append(str);
          }
          ready.countDown();
        }, priority);
      }
    }, 100);

    AtomicBoolean waitResult = new AtomicBoolean(false);
    assertAll(
        () -> assertDoesNotThrow(
            () -> waitResult.set(ready.await(3000, TimeUnit.MILLISECONDS)), "not complete in time"),
        () -> assertTrue(waitResult.get(), "count down timeout"),
        () -> assertEquals("Q100Q1Q2Q3Q4Q5", sb.toString(), "wrong priority sequence"));
  }

  @Test
  public void singleComposeredRunner() throws Exception {
    StringBuilder sb = new StringBuilder();
    ActionComposer lowerPriorityComposer = getEmptyActionComposerBuilder()
        .prepareActionSequence()
          .prepareWaitUntil(driver -> {
            synchronized (sb) {
              sb.append("L");
            }
            return false;
          }, 300)
          .withTimeoutCallback(ac -> {})
          .done()
        .returnToComposerBuilder()
        .buildBasic("singleComposeredRunner-lowerPriority")
        .setOpenWindow(false).setCloseWindow(false)
        .setPriority(2);

    ActionComposer higherPriorityComposer = getEmptyActionComposerBuilder()
        .prepareActionSequence()
          .prepareWaitUntil(driver -> {
            synchronized (sb) {
              sb.append("H");
            }
            return false;
          }, 300)
          .withTimeoutCallback(ac -> {})
          .done()
        .returnToComposerBuilder()
        .buildBasic("singleComposeredRunner-higherPriority")
        .setOpenWindow(false).setCloseWindow(false)
        .setPriority(1);

    CompletableFuture<Void> lowerPriorityFuture = browserOne
        .executeComposer(lowerPriorityComposer);
    CompletableFuture<Void> higherPriorityFuture = browserOne
        .executeComposer(higherPriorityComposer);

    assertAll(
        () -> assertDoesNotThrow(() -> {
          CompletableFuture.allOf(higherPriorityFuture, lowerPriorityFuture)
            .get(3000, TimeUnit.MILLISECONDS);
        }, "not complete in time"),
        () -> assertTrue(sb.toString().matches("(L+H+)|(H+L+)"),
            "expected:(L+H+)|(H+L+), actual:" + sb.toString()),
        () -> assertTrue(lowerPriorityComposer.isSuccessfulDone(),
            "lower priority composer fail"),
        () -> assertTrue(higherPriorityComposer.isSuccessfulDone(),
            "higher priority composer fail"));
  }

  @Test
  public void interleave() throws Exception {
    AtomicInteger counter = new AtomicInteger(0);
    ActionComposer composer1 = getEmptyActionComposerBuilder()
        .prepareActionSequence()
          .customMultiPhase(ps -> ac -> {
            if (counter.get() % 2 == 0 && counter.getAndIncrement() > 10) {
              ps.noNextPhase();
            }
            if (!ac.getWebDriver().getWindowHandle().equals(ac.getFocusWindow())) {
              ac.skipToFail();
              ps.noNextPhase();
            }
          })
        .returnToComposerBuilder()
        .buildBasic("interleave-1")
        .setOpenWindow(false).setCloseWindow(false)
        .setPriority(1);

    ActionComposer composer2 = getEmptyActionComposerBuilder()
        .prepareActionSequence()
          .customMultiPhase(ps -> ac -> {
            if (counter.get() % 2 == 1 && counter.getAndIncrement() > 10) {
              ps.noNextPhase();
            }
            if (!ac.getWebDriver().getWindowHandle().equals(ac.getFocusWindow())) {
              ac.skipToFail();
              ps.noNextPhase();
            }
          })
        .returnToComposerBuilder()
        .buildBasic("interleave-2")
        .setOpenWindow(false).setCloseWindow(false)
        .setPriority(1);

    CompletableFuture<Void> future1 = browserRunnerTwo.executeComposer(composer1);
    CompletableFuture<Void> future2 = browserRunnerTwo.executeComposer(composer2);
    assertAll(
        () -> assertDoesNotThrow(
            () -> CompletableFuture.allOf(future2, future1).get(3000, TimeUnit.MILLISECONDS),
            "not complete in time"),
        () -> assertTrue(composer1.isSuccessfulDone(), "composer1 fail"),
        () -> assertTrue(composer2.isSuccessfulDone(), "composer2 fail"));
  }

  @Test
  public void continuous() throws Exception {
    StringBuilder sb = new StringBuilder();
    BasicActionComposer parentComposer = getEmptyActionComposerBuilder()
        .prepareActionSequence()
          .custom(ac -> sb.append("parent"))
        .returnToComposerBuilder()
        .buildBasic("continuousTest-parent")
        .setOpenWindow(false).setCloseWindow(false);


    BasicActionComposer childComposer = getEmptyActionComposerBuilder()
        .prepareActionSequence()
          .custom(ac -> sb.append("child"))
        .returnToComposerBuilder()
        .buildBasic("continuousTest-child")
        .setOpenWindow(false).setCloseWindow(false);

    parentComposer.continueWith(childComposer);
    browserRunnerTwo.executeComposer(parentComposer);

    assertAll(
        () -> assertDoesNotThrow(
            () -> childComposer.get(1000, TimeUnit.MILLISECONDS), "not complete in time"),
        () -> assertEquals("parentchild", sb.toString(), "composer not executed in order"),
        () -> assertTrue(parentComposer.isSuccessfulDone(), "parent composer composer fail"),
        () -> assertTrue(childComposer.isSuccessfulDone(), "child composer fail"));
  }

  @Test    
  public void skipToSuccessAndClose() throws Exception {
    StringBuilder sb = new StringBuilder();
    AtomicBoolean result = new AtomicBoolean(true);
    ActionComposer actionComposer = getEmptyActionComposerBuilder()
        .prepareActionSequence()
          .prepareIfThenElse(ac -> true)
          .then()
            .custom(ac -> ac.skipToSuccess())
            .custom(ac -> sb.append("unexpected"))
        .returnToComposerBuilder()
        .onSuccess(ac -> {
          sb.append("success");
        })
        .onFail(ac -> {
          sb.append("fail");
        })
        .onDone(ac -> {
          sb.append("done");
        })
        .buildBasic("skipToSuccessAndClose")
        .setOpenWindow(true).setCloseWindow(true);

    assertAll(
        () -> assertDoesNotThrow(
            () -> browserOne.executeComposer(actionComposer).get(3000, TimeUnit.MILLISECONDS),
            "not complete in time"),
        () -> assertEquals("successdone", sb.toString(), "on success/done not triggered"),
        () -> assertTrue(actionComposer.isSuccessfulDone(), "composer fail"));

    result.set(actionComposer.getWebDriver().getWindowHandles()
        .contains(actionComposer.getRegisteredWindow("")));
    assertFalse(result.get(), "window not closed");
  }

  @Test    
  public void skipToFailAndClose() throws Exception {
    StringBuilder sb = new StringBuilder();
    AtomicBoolean result = new AtomicBoolean(true);
    ActionComposer actionComposer = getEmptyActionComposerBuilder()
        .prepareActionSequence()
          .custom(ac -> ac.skipToFail())
          .custom(ac -> sb.append("unexpected"))
        .returnToComposerBuilder()
        .onSuccess(ac -> {
          sb.append("success");
        })
        .onFail(ac -> {
          sb.append("fail");
        })
        .onDone(ac -> {
          sb.append("done");
        })
        .buildBasic("skipToFailAndClose")
        .setOpenWindow(true).setCloseWindow(true);

    assertAll(
        () -> assertDoesNotThrow(
            () -> browserOne.executeComposer(actionComposer).get(3000, TimeUnit.MILLISECONDS),
            "not complete in time"),
        () -> assertEquals("faildone", sb.toString(), "on fail/done not triggered"),
        () -> assertTrue(actionComposer.isFail(), "composer not fail"));

    result.set(actionComposer.getWebDriver().getWindowHandles()
        .contains(actionComposer.getRegisteredWindow("")));
    assertFalse(result.get(), "window not closed");
  }

  @Test    
  public void failAndClose() throws Exception {
    StringBuilder sb = new StringBuilder();
    AtomicBoolean result = new AtomicBoolean(true);
    ActionComposer actionComposer = getEmptyActionComposerBuilder()
        .prepareActionSequence()
          .custom(ac -> {
            throw new RuntimeException("custom exception for failAndClose");
          })
        .returnToComposerBuilder()
        .onSuccess(ac -> {
          sb.append("success");
        })
        .onFail(ac -> {
          sb.append("fail");
        })
        .onDone(ac -> {
          sb.append("done");
        })
        .buildBasic("failAndClose")
        .setOpenWindow(true).setCloseWindow(true);

    assertAll(
        () -> assertDoesNotThrow(
            () -> browserOne.executeComposer(actionComposer).get(3000, TimeUnit.MILLISECONDS),
            "not complete in time"),
        () -> assertEquals("faildone", sb.toString(), "on fail/done not triggered"),
        () -> assertTrue(actionComposer.isFail(), "composer not fail"));

    result.set(actionComposer.getWebDriver().getWindowHandles()
        .contains(actionComposer.getRegisteredWindow("")));
    assertFalse(result.get(), "window not closed");
  }
}
