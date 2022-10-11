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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.kquiet.browser.ActionComposer;
import org.kquiet.browser.ActionComposerBuilder;
import org.kquiet.browser.ActionRunner;
import org.kquiet.browser.action.Custom;
import org.kquiet.browser.action.ReplyAlert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.takes.Request;
import org.takes.facets.fork.FkRegex;
import org.takes.facets.fork.TkFork;
import org.takes.http.FtBasic;
import org.takes.rq.RqGreedy;
import org.takes.rq.RqMethod;
import org.takes.rq.form.RqFormSmart;
import org.takes.rs.RsWithBody;
import org.takes.rs.RsWithStatus;
import org.takes.rs.RsWithType;

/**
 * Test supported actions.
 *
 * @author Kimberly
 */
public class ActionTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(ActionTest.class);
  private static final String ENTRY_HTML_FILE_NAME = "ActionTest.html";
  private static final String FRAMESET_HTML_FILE_NAME = "ActionTest_frameset.html";
  private static final String INNER_HTML_FILE_NAME = "ActionTest_inner.html";
  private static final AtomicBoolean TEST_SERVER_EXIT_FLAG = new AtomicBoolean(false);

  private static ActionRunner browserRunner;
  private static URL entryHtmlFileUrl;
  private static URL framesetHtmlFileUrl;
  private static URL innerHtmlFileUrl;

  public ActionTest() {}

  /** Set up a browser, resources files, and a web server for testing. */
  @BeforeAll
  public static void setUpClass() {
    browserRunner = TestHelper.createRunner(5);
    entryHtmlFileUrl = ActionTest.class.getResource(ENTRY_HTML_FILE_NAME);
    framesetHtmlFileUrl = ActionTest.class.getResource(FRAMESET_HTML_FILE_NAME);
    innerHtmlFileUrl = ActionTest.class.getResource(INNER_HTML_FILE_NAME);
    createTempWebServer(TEST_SERVER_EXIT_FLAG);
  }

  /** Release various resources. */
  @AfterAll
  public static void tearDownClass() {
    browserRunner.close();
    TEST_SERVER_EXIT_FLAG.set(true);
  }

  @BeforeEach
  public void setUp() {}

  @AfterEach
  public void tearDown() {}

  private static List<By> getFrameSequence(boolean withInFrame) {
    if (withInFrame) {
      return new ArrayList<>(Arrays.asList(By.id("frame-level1-01"), By.id("frame-level2-01")));
    } else {
      return new ArrayList<>();
    }
  }

  private static ExpectedCondition<?> mergeFrameExpectedCondition(
      List<By> frameSequence, ExpectedCondition<?> condition) {
    if (frameSequence.size() == 2) {
      return ExpectedConditions.and(
          ExpectedConditions.frameToBeAvailableAndSwitchToIt(frameSequence.get(0)),
          ExpectedConditions.frameToBeAvailableAndSwitchToIt(frameSequence.get(1)),
          condition);
    } else {
      return condition;
    }
  }

  private static String frameIdentifier(boolean flag) {
    return (flag ? "(withInFrame)" : "");
  }

  private static FkRegex getStaticHtml(String pattern, URL fileUrl) {
    return new FkRegex(
        pattern,
        (Request req) -> {
          RqGreedy cachedReq = new RqGreedy(req);
          String method = new RqMethod.Base(cachedReq).method();
          if (RqMethod.GET.equalsIgnoreCase(method)) {
            return new RsWithStatus(
                new RsWithType(
                    new RsWithBody(
                        new String(
                            Files.readAllBytes(
                                Paths.get(new File(fileUrl.getFile()).getAbsolutePath())))),
                    "text/html"),
                200);
          } else {
            return new RsWithStatus(new RsWithBody("method is not GET:" + method), 200);
          }
        });
  }

  // create an temporary web server for testing
  private static void createTempWebServer(AtomicBoolean exitFlag) {
    new Thread(
            () -> {
              try {
                new FtBasic(
                        new TkFork(
                            new FkRegex(
                                "/postform",
                                (Request req) -> {
                                  RqGreedy cachedReq = new RqGreedy(req);
                                  String method = new RqMethod.Base(cachedReq).method();
                                  if (RqMethod.POST.equalsIgnoreCase(method)) {
                                    RqFormSmart rfs = new RqFormSmart(cachedReq);
                                    String value1 = rfs.single("key1");
                                    String value2 = rfs.single("key2");
                                    return new RsWithStatus(
                                        new RsWithType(
                                            new RsWithBody(
                                                "<html>" + value1 + ":" + value2 + "</html>"),
                                            "text/html"),
                                        200);
                                  } else {
                                    return new RsWithStatus(
                                        new RsWithBody("method is not POST:" + method), 200);
                                  }
                                }),
                            getStaticHtml("/" + ENTRY_HTML_FILE_NAME, entryHtmlFileUrl),
                            getStaticHtml("/" + FRAMESET_HTML_FILE_NAME, framesetHtmlFileUrl),
                            getStaticHtml("/" + INNER_HTML_FILE_NAME, innerHtmlFileUrl)),
                        62226)
                    .start(
                        () -> {
                          return exitFlag.get();
                        });
              } catch (IOException ex) {
                throw new RuntimeException(ex);
              }
            })
        .start();
  }

  private ActionComposerBuilder getDefinedActionComposerBuilder() {
    return new ActionComposerBuilder()
        .prepareActionSequence()
        .getUrl("http://127.0.0.1:62226/ActionTest.html")
        .returnToComposerBuilder();
  }

  private ActionComposerBuilder getEmptyActionComposerBuilder() {
    return new ActionComposerBuilder();
  }

  @Test
  void waitUntilSuccess() {
    ActionComposer actionComposer =
        getEmptyActionComposerBuilder()
            .prepareActionSequence()
            .waitUntil(driver -> true, 500)
            .returnToComposerBuilder()
            .buildBasic("waitUntilSuccess")
            .setOpenWindow(false)
            .setCloseWindow(false);

    assertAll(
        () ->
            assertDoesNotThrow(
                () ->
                    browserRunner.executeComposer(actionComposer).get(1000, TimeUnit.MILLISECONDS),
                "not complete in time"),
        () -> assertTrue(actionComposer.isSuccessfulDone(), "composer fail"));
  }

  @Test
  void waitUntilTimeout() {
    AtomicBoolean result = new AtomicBoolean(false);
    ActionComposer actionComposer =
        getEmptyActionComposerBuilder()
            .prepareActionSequence()
            .prepareWaitUntil(driver -> false, 300)
            .withTimeoutCallback(
                ac -> {
                  result.set(true);
                })
            .done()
            .returnToComposerBuilder()
            .buildBasic("waitUntilTimeout")
            .setOpenWindow(false)
            .setCloseWindow(false)
            .keepFailInfo(false);

    assertAll(
        () ->
            assertDoesNotThrow(
                () -> browserRunner.executeComposer(actionComposer).get(600, TimeUnit.MILLISECONDS),
                "not complete in time"),
        () -> assertTrue(actionComposer.isSuccessfulDone(), "composer fail"),
        () -> assertTrue(result.get(), "timeout callback not fired"));
  }

  @Test
  void justWait() {
    ActionComposer actionComposer =
        getEmptyActionComposerBuilder()
            .prepareActionSequence()
            .justWait(300)
            .returnToComposerBuilder()
            .buildBasic("justWait")
            .setOpenWindow(false)
            .setCloseWindow(false);

    assertAll(
        () ->
            assertDoesNotThrow(
                () -> browserRunner.executeComposer(actionComposer).get(600, TimeUnit.MILLISECONDS),
                "not complete in time"),
        () -> assertTrue(actionComposer.isSuccessfulDone(), "composer fail"));
  }

  @Test
  void custom() {
    AtomicReference<String> text = new AtomicReference<>("original");
    String customText = "custom";
    ActionComposer actionComposer =
        getEmptyActionComposerBuilder()
            .prepareActionSequence()
            .custom(ac -> text.set(customText))
            .returnToComposerBuilder()
            .buildBasic("custom")
            .setOpenWindow(false)
            .setCloseWindow(false);

    assertAll(
        () ->
            assertDoesNotThrow(
                () ->
                    browserRunner.executeComposer(actionComposer).get(1000, TimeUnit.MILLISECONDS),
                "not complete in time"),
        () -> assertEquals(customText, text.get()),
        () -> assertTrue(actionComposer.isSuccessfulDone(), "composer fail"));
  }

  @Test
  void ifThenElse() {
    StringBuilder sb = new StringBuilder();
    ActionComposer actionComposer =
        getEmptyActionComposerBuilder()
            .prepareActionSequence()
            .prepareIfThenElse(s -> true)
            .then()
            .custom(
                ac -> {
                  sb.append("1");
                  ac.addToTailByContext(
                      new Custom((Consumer<ActionComposer>) iac -> sb.append("A"), null));
                  ac.addToTail(new Custom((Consumer<ActionComposer>) iac -> sb.append("D"), null));
                })
            .prepareIfThenElse(s -> false)
            .then()
            .custom(ac -> sb.append("2"))
            .endActionSequence()
            .otherwise()
            .prepareIfThenElse(s -> true)
            .then()
            .custom(
                ac -> {
                  sb.append("3");
                  ac.addToPositionByContext(
                      new Custom((Consumer<ActionComposer>) iac -> sb.append("B"), null), 1);
                  ac.addToPosition(
                      new Custom((Consumer<ActionComposer>) iac -> sb.append("E"), null), 1);
                })
            .custom(ac -> sb.append("C"))
            .endActionSequence()
            .endIf()
            .custom(ac -> sb.append("4"))
            .endActionSequence()
            .endIf()
            .endActionSequence()
            .otherwise()
            .custom(ac -> sb.append("5"))
            .prepareIfThenElse(s -> true)
            .then()
            .custom(ac -> sb.append("6"))
            .endActionSequence()
            .otherwise()
            .custom(ac -> sb.append("7"))
            .endActionSequence()
            .endIf()
            .endActionSequence()
            .endIf()
            .returnToComposerBuilder()
            .buildBasic("ifThenElse")
            .setOpenWindow(false)
            .setCloseWindow(false);

    assertAll(
        () ->
            assertDoesNotThrow(
                () ->
                    browserRunner.executeComposer(actionComposer).get(3000, TimeUnit.MILLISECONDS),
                "not complete in time"),
        () -> assertEquals("13BC4AED", sb.toString(), "condition sequence not match"),
        () -> assertTrue(actionComposer.isSuccessfulDone(), "composer fail"));
  }

  @Test
  void getUrl() {
    ActionComposer actionComposer =
        getDefinedActionComposerBuilder()
            .prepareActionSequence()
            .waitUntil(ExpectedConditions.titleIs("ActionTest"), 3000)
            .returnToComposerBuilder()
            .buildBasic("getUrl")
            .setOpenWindow(true)
            .setCloseWindow(true);

    assertAll(
        () ->
            assertDoesNotThrow(
                () ->
                    browserRunner.executeComposer(actionComposer).get(4000, TimeUnit.MILLISECONDS),
                "not complete in time"),
        () -> assertTrue(actionComposer.isSuccessfulDone(), "composer fail"));
  }

  @Test
  void postForm() {
    List<SimpleImmutableEntry<String, String>> formData = new ArrayList<>();
    formData.add(new SimpleImmutableEntry<>("key1", "value1"));
    formData.add(new SimpleImmutableEntry<>("key2", "value2"));
    ActionComposer actionComposer =
        getEmptyActionComposerBuilder()
            .prepareActionSequence()
            .postForm("http://127.0.0.1:62226/postform", formData)
            .waitUntil(
                ExpectedConditions.and(
                    ExpectedConditions.urlContains("postform"),
                    ExpectedConditions.textToBePresentInElementLocated(
                        By.tagName("html"), "value1:value2")),
                1000)
            .returnToComposerBuilder()
            .buildBasic("postForm")
            .setOpenWindow(true)
            .setCloseWindow(true);

    assertAll(
        () ->
            assertDoesNotThrow(
                () ->
                    browserRunner.executeComposer(actionComposer).get(3000, TimeUnit.MILLISECONDS),
                "not complete in time"),
        () -> assertTrue(actionComposer.isSuccessfulDone(), "composer fail"));
  }

  @Test
  void click() {
    final AtomicReference<List<By>> frameSequence = new AtomicReference<>();

    Function<Boolean, ActionComposer> composerSupplier =
        (flag) ->
            getDefinedActionComposerBuilder()
                .prepareActionSequence()
                .waitUntil(
                    mergeFrameExpectedCondition(
                        frameSequence.get(),
                        ExpectedConditions.visibilityOfElementLocated(By.id("btnClick"))),
                    3000)
                .prepareClick(By.id("btnClick"))
                .withInFrame(frameSequence.get())
                .done()
                .waitUntil(
                    mergeFrameExpectedCondition(
                        frameSequence.get(),
                        ExpectedConditions.visibilityOfElementLocated(By.id("divClickResult"))),
                    1000)
                .returnToComposerBuilder()
                .buildBasic("click" + frameIdentifier(flag))
                .setOpenWindow(true)
                .setCloseWindow(true);

    BiConsumer<Boolean, ActionComposer> assertConsumer =
        (flag, ac) ->
            assertAll(
                () ->
                    assertDoesNotThrow(
                        () -> browserRunner.executeComposer(ac).get(5000, TimeUnit.MILLISECONDS),
                        "not complete in time" + frameIdentifier(flag)),
                () -> assertTrue(ac.isSuccessfulDone(), "composer fail" + frameIdentifier(flag)));

    // 1:test without frame; 2: test within frame
    for (int i = 1; i <= 2; i++) {
      boolean withFrame = (i == 2);
      frameSequence.set(getFrameSequence(withFrame));
      assertConsumer.accept(withFrame, composerSupplier.apply(withFrame));
    }
  }

  @Test
  void mouseOver() {
    final AtomicReference<List<By>> frameSequence = new AtomicReference<>();

    Function<Boolean, ActionComposer> composerSupplier =
        (flag) ->
            getDefinedActionComposerBuilder()
                .prepareActionSequence()
                .waitUntil(
                    mergeFrameExpectedCondition(
                        frameSequence.get(),
                        ExpectedConditions.and(
                            ExpectedConditions.visibilityOfElementLocated(By.id("divMouseOver")),
                            ExpectedConditions.invisibilityOfElementLocated(By.id("spMouseOver")))),
                    3000)
                .prepareScrollToView(By.id("divMouseOver"), true)
                .withInFrame(frameSequence.get())
                .done()
                .prepareMouseOver(By.id("divMouseOver"))
                .withInFrame(frameSequence.get())
                .done()
                .waitUntil(
                    mergeFrameExpectedCondition(
                        frameSequence.get(),
                        ExpectedConditions.visibilityOfElementLocated(By.id("spMouseOver"))),
                    1000)
                .returnToComposerBuilder()
                .buildBasic("mouseOver" + frameIdentifier(flag))
                .setOpenWindow(true)
                .setCloseWindow(true);

    BiConsumer<Boolean, ActionComposer> assertConsumer =
        (flag, ac) ->
            assertAll(
                () ->
                    assertDoesNotThrow(
                        () -> browserRunner.executeComposer(ac).get(5000, TimeUnit.MILLISECONDS),
                        "not complete in time" + frameIdentifier(flag)),
                () -> assertTrue(ac.isSuccessfulDone(), "composer fail" + frameIdentifier(flag)));

    // 1:test without frame; 2: test within frame
    for (int i = 1; i <= 2; i++) {
      boolean withFrame = (i == 2);
      frameSequence.set(getFrameSequence(withFrame));
      assertConsumer.accept(withFrame, composerSupplier.apply(withFrame));
    }
  }

  @Test
  void sendKeys() {
    final AtomicReference<String> actualValue = new AtomicReference<>();
    final AtomicReference<List<By>> frameSequence = new AtomicReference<>();

    Function<Boolean, ActionComposer> composerSupplier =
        (flag) ->
            getDefinedActionComposerBuilder()
                .prepareActionSequence()
                .waitUntil(
                    mergeFrameExpectedCondition(
                        frameSequence.get(),
                        ExpectedConditions.visibilityOfElementLocated(By.id("txtSendKey"))),
                    3000)
                .prepareSendKey(By.id("txtSendKey"), "clearbeforesend")
                .withClearBeforeSend()
                .withInFrame(frameSequence.get())
                .done()
                .prepareWaitUntil(
                    mergeFrameExpectedCondition(
                        frameSequence.get(),
                        ExpectedConditions.attributeToBe(
                            By.id("txtSendKey"), "value", "clearbeforesend")),
                    1000)
                .withTimeoutCallback(
                    ac -> {
                      try {
                        WebDriver driver = ac.getWebDriver();
                        WebElement inputE =
                            driver
                                .findElements(By.id("txtSendKey"))
                                .stream()
                                .findFirst()
                                .orElse(null);
                        actualValue.set(
                            Optional.ofNullable(
                                    inputE == null ? null : inputE.getAttribute("value"))
                                .orElse(""));
                        ac.skipToFail();
                      } catch (Exception ex) {
                        LOGGER.trace("sendKey error", ex);
                        throw ex;
                      }
                    })
                .done()
                .justWait(100)
                .prepareSendKey(By.id("txtSendKey"), "sendwithoutclear")
                .withInFrame(frameSequence.get())
                .done()
                .prepareWaitUntil(
                    mergeFrameExpectedCondition(
                        frameSequence.get(),
                        ExpectedConditions.attributeToBe(
                            By.id("txtSendKey"), "value", "clearbeforesendsendwithoutclear")),
                    1000)
                .withTimeoutCallback(
                    ac -> {
                      try {
                        WebDriver driver = ac.getWebDriver();
                        WebElement inputE =
                            driver
                                .findElements(By.id("txtSendKey"))
                                .stream()
                                .findFirst()
                                .orElse(null);
                        actualValue.set(
                            Optional.ofNullable(
                                    inputE == null ? null : inputE.getAttribute("value"))
                                .orElse(""));
                        ac.skipToFail();
                      } catch (Exception ex) {
                        LOGGER.trace("sendKey error", ex);
                        throw ex;
                      }
                    })
                .done()
                .returnToComposerBuilder()
                .buildBasic("sendKeys" + frameIdentifier(flag))
                .setOpenWindow(true)
                .setCloseWindow(true);

    BiConsumer<Boolean, ActionComposer> assertConsumer =
        (flag, ac) ->
            assertAll(
                () ->
                    assertDoesNotThrow(
                        () -> browserRunner.executeComposer(ac).get(5000, TimeUnit.MILLISECONDS),
                        "not complete in time" + frameIdentifier(flag)),
                () ->
                    assertTrue(
                        ac.isSuccessfulDone(),
                        "composer fail,"
                            + "actualValue:"
                            + actualValue.get()
                            + frameIdentifier(flag)));

    // 1:test without frame; 2: test within frame
    for (int i = 1; i <= 2; i++) {
      boolean withFrame = (i == 2);
      actualValue.set("original");
      frameSequence.set(getFrameSequence(withFrame));
      assertConsumer.accept(withFrame, composerSupplier.apply(withFrame));
    }
  }

  @Test
  void select() {
    final AtomicReference<String> actualValue = new AtomicReference<>();
    final AtomicReference<List<By>> frameSequence = new AtomicReference<>();

    Function<Boolean, ActionComposer> composerSupplier =
        (flag) ->
            getDefinedActionComposerBuilder()
                .prepareActionSequence()
                .waitUntil(
                    mergeFrameExpectedCondition(
                        frameSequence.get(),
                        ExpectedConditions.visibilityOfElementLocated(By.id("slcSelect"))),
                    3000)
                .prepareSelect(By.id("slcSelect"))
                .selectByText("option2")
                .withInFrame(frameSequence.get())
                .done()
                .prepareWaitUntil(
                    mergeFrameExpectedCondition(
                        frameSequence.get(),
                        ExpectedConditions.attributeToBe(
                            By.id("slcSelect"), "value", "optionValue2")),
                    1000)
                .withTimeoutCallback(
                    ac -> {
                      try {
                        WebDriver driver = ac.getWebDriver();
                        WebElement inputE =
                            driver
                                .findElements(By.id("slcSelect"))
                                .stream()
                                .findFirst()
                                .orElse(null);
                        actualValue.set(
                            Optional.ofNullable(
                                    inputE == null ? null : inputE.getAttribute("value"))
                                .orElse(""));
                        ac.skipToFail();
                      } catch (Exception ex) {
                        LOGGER.trace("select error", ex);
                        throw ex;
                      }
                    })
                .done()
                .returnToComposerBuilder()
                .buildBasic("select" + frameIdentifier(flag))
                .setOpenWindow(true)
                .setCloseWindow(true);

    BiConsumer<Boolean, ActionComposer> assertConsumer =
        (flag, ac) ->
            assertAll(
                () ->
                    assertDoesNotThrow(
                        () -> browserRunner.executeComposer(ac).get(5000, TimeUnit.MILLISECONDS),
                        "not complete in time" + frameIdentifier(flag)),
                () ->
                    assertTrue(
                        ac.isSuccessfulDone(),
                        "composer fail,"
                            + "actualValue:"
                            + actualValue.get()
                            + frameIdentifier(flag)));

    // 1:test without frame; 2: test within frame
    for (int i = 1; i <= 2; i++) {
      boolean withFrame = (i == 2);
      actualValue.set("original");
      frameSequence.set(getFrameSequence(withFrame));
      assertConsumer.accept(withFrame, composerSupplier.apply(withFrame));
    }
  }

  @Test
  void openAndCloseWindow() {
    ActionComposer actionComposer =
        getEmptyActionComposerBuilder()
            .prepareActionSequence()
            .prepareOpenWindow()
            .registerAs("newwindow")
            .done()
            .custom(
                ac -> {
                  if (!ac.switchToWindow(ac.getRegisteredWindow("newwindow"))) {
                    ac.skipToFail();
                  }
                })
            .closeWindow(true)
            .custom(
                ac -> {
                  if (ac.getWebDriver()
                      .getWindowHandles()
                      .contains(ac.getRegisteredWindow("newwindow"))) {
                    ac.skipToFail();
                  }
                })
            .returnToComposerBuilder()
            .buildBasic("openAndCloseWindow")
            .setOpenWindow(false)
            .setCloseWindow(false);

    assertAll(
        () ->
            assertDoesNotThrow(
                () ->
                    browserRunner.executeComposer(actionComposer).get(3000, TimeUnit.MILLISECONDS),
                "not complete in time"),
        () -> assertTrue(actionComposer.isSuccessfulDone(), "composer fail"));
  }

  @Test
  void scrollToView() {
    final AtomicLong initPosition = new AtomicLong(-1);
    final AtomicLong afterPosition = new AtomicLong(-1);
    final AtomicReference<List<By>> frameSequence = new AtomicReference<>();

    Function<Boolean, ActionComposer> composerSupplier =
        (flag) ->
            getDefinedActionComposerBuilder()
                .prepareActionSequence()
                .waitUntil(
                    mergeFrameExpectedCondition(
                        frameSequence.get(),
                        ExpectedConditions.visibilityOfAllElementsLocatedBy(By.id("divScroll"))),
                    1000)
                .prepareCustom(
                    ac -> {
                      WebElement element =
                          ac.getWebDriver()
                              .findElements(By.id("divScroll"))
                              .stream()
                              .findFirst()
                              .orElse(null);
                      Object tmp = ((JavascriptExecutor) ac.getWebDriver())
                          .executeScript(
                              "return arguments[0].getBoundingClientRect().top;", element);
                      Long top = -1L;
                      if (tmp instanceof Double d) {
                        top = d.longValue();
                      } else if (tmp instanceof Long l) {
                        top = l;
                      }
                      initPosition.set(top);
                    })
                .withInFrame(frameSequence.get())
                .done()
                .prepareScrollToView(By.id("divScroll"), true)
                .withInFrame(frameSequence.get())
                .done()
                .prepareCustom(
                    ac -> {
                      WebElement element =
                          ac.getWebDriver()
                              .findElements(By.id("divScroll"))
                              .stream()
                              .findFirst()
                              .orElse(null);
                      Object tmp = ((JavascriptExecutor) ac.getWebDriver())
                          .executeScript(
                              "return arguments[0].getBoundingClientRect().top;", element);
                      Long top = -1L;
                      if (tmp instanceof Double d) {
                        top = d.longValue();
                      } else if (tmp instanceof Long l) {
                        top = l;
                      }
                      afterPosition.set(top);
                    })
                .withInFrame(frameSequence.get())
                .done()
                .returnToComposerBuilder()
                .buildBasic("scrollToView" + frameIdentifier(flag))
                .setOpenWindow(true)
                .setCloseWindow(true);

    BiConsumer<Boolean, ActionComposer> assertConsumer =
        (flag, ac) ->
            assertAll(
                () ->
                    assertDoesNotThrow(
                        () -> browserRunner.executeComposer(ac).get(5000, TimeUnit.MILLISECONDS),
                        "not complete in time"),
                () ->
                    assertTrue(
                        initPosition.get() > 0 && afterPosition.get() == 0,
                        String.format(
                            "scroll error, initial:%s, after:%s",
                            initPosition.get(), afterPosition.get())),
                () -> assertTrue(ac.isSuccessfulDone(), "composer fail"));

    // 1:test without frame; 2: test within frame
    for (int i = 1; i <= 2; i++) {
      boolean withFrame = (i == 2);
      initPosition.set(-1);
      afterPosition.set(-1);
      frameSequence.set(getFrameSequence(withFrame));
      assertConsumer.accept(withFrame, composerSupplier.apply(withFrame));
    }
  }

  @Test
  void upload() {
    final AtomicReference<String> fileName = new AtomicReference<>();
    final AtomicReference<List<By>> frameSequence = new AtomicReference<>();

    Function<Boolean, ActionComposer> composerSupplier =
        (flag) ->
            getDefinedActionComposerBuilder()
                .prepareActionSequence()
                .waitUntil(
                    mergeFrameExpectedCondition(
                        frameSequence.get(),
                        ExpectedConditions.presenceOfElementLocated(By.id("flUpload"))),
                    1000)
                .prepareUpload(
                    By.id("flUpload"), new File(entryHtmlFileUrl.getFile()).getAbsolutePath())
                .withInFrame(frameSequence.get())
                .done()
                .waitUntil(
                    mergeFrameExpectedCondition(
                        frameSequence.get(),
                        ExpectedConditions.visibilityOfElementLocated(By.id("flUpload"))),
                    1000)
                .prepareCustom(
                    ac -> {
                      WebElement element =
                          ac.getWebDriver()
                              .findElements(By.id("flUpload"))
                              .stream()
                              .findFirst()
                              .orElse(null);
                      String name =
                          (String)
                              ((JavascriptExecutor) ac.getWebDriver())
                                  .executeScript("return arguments[0].files[0].name;", element);
                      fileName.set(name);
                    })
                .withInFrame(frameSequence.get())
                .done()
                .returnToComposerBuilder()
                .buildBasic("upload" + frameIdentifier(flag))
                .setOpenWindow(true)
                .setCloseWindow(true);

    BiConsumer<Boolean, ActionComposer> assertConsumer =
        (flag, ac) ->
            assertAll(
                () ->
                    assertDoesNotThrow(
                        () -> browserRunner.executeComposer(ac).get(5000, TimeUnit.MILLISECONDS),
                        "not complete in time" + frameIdentifier(flag)),
                () ->
                    assertEquals(
                        ENTRY_HTML_FILE_NAME,
                        fileName.get(),
                        "filename not match" + frameIdentifier(flag)),
                () -> assertTrue(ac.isSuccessfulDone(), "composer fail" + frameIdentifier(flag)));

    // 1:test without frame; 2: test within frame
    for (int i = 1; i <= 2; i++) {
      boolean withFrame = (i == 2);
      fileName.set("");
      frameSequence.set(getFrameSequence(withFrame));
      assertConsumer.accept(withFrame, composerSupplier.apply(withFrame));
    }
  }

  @Test
  void replyAlert() {
    ActionComposer actionComposer =
        getDefinedActionComposerBuilder()
            .prepareActionSequence()
            .waitUntil(
                ExpectedConditions.and(
                    ExpectedConditions.visibilityOfElementLocated(By.id("btnAlert")),
                    ExpectedConditions.visibilityOfElementLocated(By.id("txtAlert"))),
                3000)
            .click(By.id("btnAlert"))
            .waitUntil(ExpectedConditions.alertIsPresent(), 1000)
            .prepareReplyAlert(ReplyAlert.Decision.Accept)
            .withTextAsVariable("AlertMessage")
            .withKeysToSend("AlertInput")
            .done()
            .waitUntil(
                ExpectedConditions.and(
                    ExpectedConditions.not(ExpectedConditions.alertIsPresent()),
                    ExpectedConditions.attributeToBe(By.id("txtAlert"), "value", "AlertInput")),
                1000)
            .returnToComposerBuilder()
            .buildBasic("replyAlert")
            .setOpenWindow(true)
            .setCloseWindow(true);

    assertAll(
        () ->
            assertDoesNotThrow(
                () ->
                    browserRunner.executeComposer(actionComposer).get(5000, TimeUnit.MILLISECONDS),
                "not complete in time"),
        () ->
            assertEquals(
                "PromptMessage",
                actionComposer.getVariable("AlertMessage"),
                "alert message not saved as variable or not equal"),
        () -> assertTrue(actionComposer.isSuccessfulDone(), "composer fail"));
  }

  @Test
  void extract() {
    final AtomicReference<List<By>> frameSequence = new AtomicReference<>();

    Function<Boolean, ActionComposer> composerSupplier =
        (flag) ->
            getDefinedActionComposerBuilder()
                .prepareActionSequence()
                .waitUntil(
                    mergeFrameExpectedCondition(
                        frameSequence.get(),
                        ExpectedConditions.and(
                            ExpectedConditions.visibilityOfElementLocated(By.id("divMouseOver")),
                            ExpectedConditions.invisibilityOfElementLocated(By.id("spMouseOver")),
                            ExpectedConditions.visibilityOfElementLocated(By.id("txtSendKey")))),
                    3000)
                .prepareExtract(By.id("divMouseOver"))
                .withTextAsVariable("varMouseOver1" + frameIdentifier(flag))
                .withInFrame(frameSequence.get())
                .done()
                .prepareExtract(By.id("txtSendKey"))
                .withAttributeAsVariable(
                    Stream.of(
                            new String[][] {
                              {"id", "varSendKey1" + frameIdentifier(flag)},
                              {"value", "varSendKey2" + frameIdentifier(flag)}
                            })
                        .collect(Collectors.toMap(s -> s[0], s -> s[1])))
                .withInFrame(frameSequence.get())
                .done()
                .returnToComposerBuilder()
                .buildBasic("extract" + frameIdentifier(flag))
                .setOpenWindow(true)
                .setCloseWindow(true);

    BiConsumer<Boolean, ActionComposer> assertConsumer =
        (flag, ac) ->
            assertAll(
                () ->
                    assertDoesNotThrow(
                        () -> browserRunner.executeComposer(ac).get(5000, TimeUnit.MILLISECONDS),
                        "not complete in time" + frameIdentifier(flag)),
                () ->
                    assertEquals(
                        "6. mouseover",
                        ac.getVariable("varMouseOver1" + frameIdentifier(flag)),
                        "text of #divMouseOver not saved as variable or not equal"
                            + frameIdentifier(flag)),
                () ->
                    assertEquals(
                        "txtSendKey",
                        ac.getVariable("varSendKey1" + frameIdentifier(flag)),
                        "id of #txtSendKey not saved as variable or not equal"
                            + frameIdentifier(flag)),
                () ->
                    assertEquals(
                        "pre-populated",
                        ac.getVariable("varSendKey2" + frameIdentifier(flag)),
                        "value of #txtSendKey not saved as variable or not equal"
                            + frameIdentifier(flag)),
                () -> assertTrue(ac.isSuccessfulDone(), "composer fail" + frameIdentifier(flag)));

    // 1:test without frame; 2: test within frame
    for (int i = 1; i <= 2; i++) {
      boolean withFrame = (i == 2);
      frameSequence.set(getFrameSequence(withFrame));
      assertConsumer.accept(withFrame, composerSupplier.apply(withFrame));
    }
  }

  @Test
  void onFail() {
    StringBuilder sb = new StringBuilder();
    ActionComposer actionComposer =
        getEmptyActionComposerBuilder()
            .prepareActionSequence()
            .custom(
                ac -> {
                  sb.append("custom");
                  throw new RuntimeException("some error");
                })
            .returnToComposerBuilder()
            .onFail(ac -> sb.append("fail"))
            .onSuccess(ac -> sb.append("success"))
            .buildBasic("onFail")
            .setOpenWindow(false)
            .setCloseWindow(false)
            .keepFailInfo(false);

    assertAll(
        () ->
            assertDoesNotThrow(
                () ->
                    browserRunner.executeComposer(actionComposer).get(3000, TimeUnit.MILLISECONDS),
                "not complete in time"),
        () -> assertEquals("customfail", sb.toString(), "on fail not triggered"),
        () -> assertTrue(actionComposer.isFail(), "composer not fail"));
  }

  @Test
  void onSuccess() {
    StringBuilder sb = new StringBuilder();
    ActionComposer actionComposer =
        getEmptyActionComposerBuilder()
            .prepareActionSequence()
            .custom(
                ac -> {
                  sb.append("custom");
                })
            .returnToComposerBuilder()
            .onFail(ac -> sb.append("fail"))
            .onSuccess(ac -> sb.append("success"))
            .buildBasic("onSuccess")
            .setOpenWindow(false)
            .setCloseWindow(false);

    assertAll(
        () ->
            assertDoesNotThrow(
                () ->
                    browserRunner.executeComposer(actionComposer).get(3000, TimeUnit.MILLISECONDS),
                "not complete in time"),
        () -> assertEquals("customsuccess", sb.toString(), "on success not triggered"),
        () -> assertTrue(actionComposer.isSuccessfulDone(), "composer fail"));
  }

  @Test
  void onDone() {
    StringBuilder sb = new StringBuilder();
    ActionComposer actionComposer =
        getEmptyActionComposerBuilder()
            .prepareActionSequence()
            .custom(ac -> sb.append("custom"))
            .returnToComposerBuilder()
            .onDone(ac -> sb.append("done"))
            .buildBasic("onDone")
            .setOpenWindow(false)
            .setCloseWindow(false);

    assertAll(
        () ->
            assertDoesNotThrow(
                () ->
                    browserRunner.executeComposer(actionComposer).get(3000, TimeUnit.MILLISECONDS),
                "not complete in time"),
        () -> assertEquals("customdone", sb.toString(), "on done not triggered"),
        () -> assertTrue(actionComposer.isDone(), "composer not done"));
  }

  @Test
  @Tag("new")
  void actionPerform() {
    StringBuilder sb = new StringBuilder();
    AtomicInteger ai = new AtomicInteger(0);
    AtomicInteger ai2 = new AtomicInteger(10);
    ActionComposer actionComposer =
        getEmptyActionComposerBuilder()
            .prepareActionSequence()
            .justWait(300)
            .prepareIfThenElse(ac -> true)
            .then()
            .custom(
                ac -> {
                  sb.append("custom");
                })
            .endActionSequence()
            .returnToComposerBuilder()
            .actionPerforming(
                ac ->
                    at -> {
                      sb.append(ai.addAndGet(1));
                    })
            .actionPerformed(
                ac ->
                    at -> {
                      sb.append(ai2.addAndGet(-1));
                    })
            .buildBasic("actionPerforming")
            .setOpenWindow(false)
            .setCloseWindow(false);

    assertAll(
        () ->
            assertDoesNotThrow(
                () ->
                    browserRunner.executeComposer(actionComposer).get(3000, TimeUnit.MILLISECONDS),
                "not complete in time"),
        () ->
            assertEquals(
                "192384custom76",
                sb.toString(),
                "action perform function not triggered corrrectly"));
  }
}
