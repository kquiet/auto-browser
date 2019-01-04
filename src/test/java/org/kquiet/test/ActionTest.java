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
package org.kquiet.test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Optional;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.JavascriptExecutor;

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

import org.kquiet.browser.ActionComposer;
import org.kquiet.browser.ActionComposerBuilder;
import org.kquiet.browser.ActionRunner;

/**
 *
 * @author Kimberly
 */
public class ActionTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActionTest.class);
    private static final String HTML_FILE_NAME = "ActionTest.html";
    
    private static ActionRunner browserRunner;
    private static URL htmlFileUrl;
    
    /**
     *
     */
    public ActionTest() {
    }
    
    /**
     *
     */
    @BeforeClass
    public static void setUpClass() {
        browserRunner = TestHelper.createRunner(5);
        htmlFileUrl = ActionTest.class.getResource(HTML_FILE_NAME);
    }
    
    /**
     *
     */
    @AfterClass
    public static void tearDownClass() {
        browserRunner.close();
    }
    
    /**
     *
     */
    @Before
    public void setUp(){
    }
    
    /**
     *
     */
    @After
    public void tearDown() {
    }
    
    private ActionComposerBuilder getDefinedActionComposerBuilder(){
        return new ActionComposerBuilder()
                .prepareActionSequence()
                    .getUrl(htmlFileUrl.toString())
                .returnToComposerBuilder();
    }
    
    private ActionComposerBuilder getEmptyActionComposerBuilder(){
        return new ActionComposerBuilder();
    }
    
    /**
     *
     * @throws Exception
     */
    @Test
    public void waitUntilSuccess() throws Exception {
        ActionComposer actionComposer = getEmptyActionComposerBuilder()
            .prepareActionSequence()
                .waitUntil(driver->true, 500)
            .returnToComposerBuilder()
            .build("waitUntilSuccess", false, false);
        long startTime = System.nanoTime();
        browserRunner.executeComposer(actionComposer).get(3000, TimeUnit.MILLISECONDS);
        long endTime = System.nanoTime();
        long elapsedWaitingMillisecond = (endTime-startTime)/1000000;
        assertTrue("elapsedWaitingMillisecond:"+elapsedWaitingMillisecond, !actionComposer.isFail() && elapsedWaitingMillisecond>=0 && elapsedWaitingMillisecond<100);
    }
    
    /**
     *
     * @throws Exception
     */
    @Test
    public void waitUntilTimeout() throws Exception {
        ActionComposer actionComposer = getEmptyActionComposerBuilder()
            .prepareActionSequence()
                .waitUntil(driver->false, 300)
            .returnToComposerBuilder()
            .build("waitUntilTimeout", false, false)
            .keepFailInfo(false);
        long startTime = System.nanoTime();
        browserRunner.executeComposer(actionComposer).get(3000, TimeUnit.MILLISECONDS);
        long endTime = System.nanoTime();
        long elapsedWaitingMillisecond = (endTime-startTime)/1000000;
        assertTrue("elapsedWaitingMillisecond:"+elapsedWaitingMillisecond, actionComposer.isFail() && elapsedWaitingMillisecond>200 && elapsedWaitingMillisecond<400);
    }
    
    /**
     *
     * @throws Exception
     */
    @Test
    public void justWait() throws Exception {
        ActionComposer actionComposer = getEmptyActionComposerBuilder()
            .prepareActionSequence()
                .justWait(300)
            .returnToComposerBuilder()
            .build("justWait", false, false);
        long startTime = System.nanoTime();
        browserRunner.executeComposer(actionComposer).get(3000, TimeUnit.MILLISECONDS);
        long endTime = System.nanoTime();
        long elapsedWaitingMillisecond = (endTime-startTime)/1000000;
        assertTrue("elapsedWaitingMillisecond:"+elapsedWaitingMillisecond, !actionComposer.isFail() && elapsedWaitingMillisecond>200 && elapsedWaitingMillisecond<400);
    }
    
    /**
     *
     * @throws Exception
     */
    @Test
    public void custom() throws Exception {
        AtomicReference<String> text = new AtomicReference<>("original");
        String customText = "custom";
        ActionComposer actionComposer = getEmptyActionComposerBuilder()
            .prepareActionSequence()
                .custom(ac->{
                    text.set(customText);
                })
            .returnToComposerBuilder()
            .build("custom", false, false);
        browserRunner.executeComposer(actionComposer).get(3000, TimeUnit.MILLISECONDS);
        assertTrue(!actionComposer.isFail() && customText.equals(text.get()));
    }
    
    /**
     *
     * @throws Exception
     */
    @Test
    public void ifThenElse() throws Exception {
        StringBuilder sb = new StringBuilder();
        ActionComposer actionComposer = getEmptyActionComposerBuilder()
            .prepareActionSequence()
                .prepareIfThenElse(s->true)
                    .then()
                        .custom(ac->{
                            sb.append("1");
                        })
                        .prepareIfThenElse(s->false)
                            .then()
                                .custom(ac->{
                                    sb.append("2");
                                })
                            .endActionSequence()
                            .otherwise()
                                .prepareIfThenElse(s->true)
                                    .then()
                                        .custom(ac->{
                                            sb.append("3");
                                        })
                                    .endActionSequence()
                                    .endIf()
                                .custom(ac->{
                                    sb.append("4");
                                })
                            .endActionSequence()
                        .endIf()
                    .endActionSequence()
                    .otherwise()
                        .custom(t -> {
                            sb.append("5");
                        })
                        .prepareIfThenElse(s->true)
                            .then()
                                .custom(ac->{
                                    sb.append("6");
                                })
                            .endActionSequence()
                            .otherwise()
                                .custom(ac->{
                                    sb.append("7");
                                })
                            .endActionSequence()
                        .endIf()
                    .endActionSequence()
                .endIf()
            .returnToComposerBuilder()
            .build("ifThenElse", false, false);
        browserRunner.executeComposer(actionComposer).get(3000, TimeUnit.MILLISECONDS);
        assertTrue(!actionComposer.isFail() && "134".equals(sb.toString()));
    }

    /**
     *
     * @throws Exception
     */
    @Test
    public void getUrl() throws Exception {
        ActionComposer actionComposer = getDefinedActionComposerBuilder()
            .prepareActionSequence()
                .waitUntil(ExpectedConditions.titleIs("ActionTest"), 3000)
            .returnToComposerBuilder()  
            .build("getUrl", true, true);
        browserRunner.executeComposer(actionComposer).get(4000, TimeUnit.MILLISECONDS);
        assertTrue(actionComposer.isSuccess());
    }
    
    /**
     *
     * @throws Exception
     */
    @Test
    public void postForm() throws Exception {
        List<SimpleImmutableEntry<String, String>> formData = new ArrayList<>();
        formData.add(new SimpleImmutableEntry<>("key1", "value1"));formData.add(new SimpleImmutableEntry<>("key2", "value2"));
        ActionComposer actionComposer = getEmptyActionComposerBuilder()
            .prepareActionSequence()
                .postForm("http://127.0.0.1:62226/postform", formData)
                .waitUntil(ExpectedConditions.and(
                    ExpectedConditions.urlContains("postform")
                    , ExpectedConditions.textToBePresentInElementLocated(By.tagName("html"), "value1:value2")
                ), 1000)
            .returnToComposerBuilder()
            .build("postForm", true, true);
        
        AtomicBoolean exitFlag = new AtomicBoolean(false);
        tempWebServer4PostForm(exitFlag);
        browserRunner.executeComposer(actionComposer).get(3000, TimeUnit.MILLISECONDS);
        exitFlag.set(true);
        assertTrue(actionComposer.isSuccess());
    }
    //create an temporary web server for testing
    private void tempWebServer4PostForm(AtomicBoolean exitFlag){
        new Thread(()->{
            try {
                new FtBasic(
                    new TkFork(
                        new FkRegex("/postform", 
                            (Request req) -> {
                                RqGreedy cachedReq = new RqGreedy(req);
                                String method = new RqMethod.Base(cachedReq).method();
                                if (RqMethod.POST.equals(method)){
                                    RqFormSmart rfs = new RqFormSmart(cachedReq);
                                    String value1 = rfs.single("key1");
                                    String value2 = rfs.single("key2");
                                    return new RsWithStatus(new RsWithType(
                                            new RsWithBody("<html>"+value1+":"+value2+"</html>")
                                            , "text/html"), 200);
                                }
                                else return new RsWithStatus(new RsWithBody(""), 200);
                            })
                    ), 62226
                ).start(() -> {
                    return exitFlag.get();
                });
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }).start();
    }
    
    /**
     *
     * @throws Exception
     */
    @Test
    public void click() throws Exception {
        ActionComposer actionComposer = getDefinedActionComposerBuilder()
            .prepareActionSequence()
                .waitUntil(ExpectedConditions.visibilityOfElementLocated(By.id("btnClick")), 3000)
                .click(By.id("btnClick"))
                .waitUntil(ExpectedConditions.visibilityOfElementLocated(By.id("divClickResult")), 1000)
            .returnToComposerBuilder()
            .build("click", true, true);
        browserRunner.executeComposer(actionComposer).get(5000, TimeUnit.MILLISECONDS);
        assertTrue(actionComposer.isSuccess());
    }
    
    /**
     *
     * @throws Exception
     */
    @Test
    public void sendKeys() throws Exception {
        AtomicReference<String> actualValue = new AtomicReference<>("original");
        ActionComposer actionComposer = getDefinedActionComposerBuilder()
            .prepareActionSequence()
                .waitUntil(ExpectedConditions.visibilityOfElementLocated(By.id("txtSendKey")), 3000)
                .prepareSendKey(By.id("txtSendKey"), "clearbeforesend")
                    .withClearBeforeSend()
                    .done()
                .prepareWaitUntil(ExpectedConditions.attributeToBe(By.id("txtSendKey"), "value", "clearbeforesend"), 1000)
                    .withTimeoutCallback(ac->{
                        try{
                            WebDriver driver = ac.getBrsDriver();
                            WebElement inputE = driver.findElements(By.id("txtSendKey")).stream().findFirst().orElse(null);
                            actualValue.set(Optional.ofNullable(inputE==null?null:inputE.getAttribute("value")).orElse(""));
                            ac.skipToFail();
                        }catch(Exception ex){LOGGER.trace("sendKey error", ex);throw ex;}
                    })
                    .done()
                .justWait(100)
                .sendKey(By.id("txtSendKey"), "sendwithoutclear")
                .prepareWaitUntil(ExpectedConditions.attributeToBe(By.id("txtSendKey"), "value", "clearbeforesendsendwithoutclear"), 1000)
                    .withTimeoutCallback(ac->{
                        try{
                            WebDriver driver = ac.getBrsDriver();
                            WebElement inputE = driver.findElements(By.id("txtSendKey")).stream().findFirst().orElse(null);
                            actualValue.set(Optional.ofNullable(inputE==null?null:inputE.getAttribute("value")).orElse(""));
                            ac.skipToFail();
                        }catch(Exception ex){LOGGER.trace("sendKey error", ex);throw ex;}
                    })
                    .done()
            .returnToComposerBuilder()
            .build("input", true, true);
        browserRunner.executeComposer(actionComposer).get(5000, TimeUnit.MILLISECONDS);
        assertTrue("actualValue:"+actualValue.get(), actionComposer.isSuccess());
    }
    
    /**
     *
     * @throws Exception
     */
    @Test
    public void select() throws Exception {
        AtomicReference<String> actualValue = new AtomicReference<>("original");
        ActionComposer actionComposer = getDefinedActionComposerBuilder()
            .prepareActionSequence()
                .waitUntil(ExpectedConditions.visibilityOfElementLocated(By.id("slcSelect")), 3000)
                .selectByText(By.id("slcSelect"), "option2")
                .prepareWaitUntil(ExpectedConditions.attributeToBe(By.id("slcSelect"), "value", "optionValue2"), 1000)
                    .withTimeoutCallback(ac->{
                        try{
                            WebDriver driver = ac.getBrsDriver();
                            WebElement inputE = driver.findElements(By.id("slcSelect")).stream().findFirst().orElse(null);
                            actualValue.set(Optional.ofNullable(inputE==null?null:inputE.getAttribute("value")).orElse(""));
                            ac.skipToFail();
                        }catch(Exception ex){LOGGER.trace("select error", ex);throw ex;}
                    })
                    .done()
            .returnToComposerBuilder()
            .build("select", true, true);
        browserRunner.executeComposer(actionComposer).get(5000, TimeUnit.MILLISECONDS);
        
        assertTrue("actualValue:"+actualValue.get(), actionComposer.isSuccess());
    }
    
    /**
     *
     * @throws Exception
     */
    @Test
    public void openAndCloseWindow() throws Exception {
        ActionComposer actionComposer = getEmptyActionComposerBuilder()
            .prepareActionSequence()
                .prepareOpenWindow()
                    .registerAs("newwindow")
                .done()
                .custom(ac->{
                    if (!ac.switchToWindow(ac.getRegisteredWindow("newwindow"))) ac.skipToFail();
                })
                .closeWindow(true)
                .custom(ac->{
                    if (ac.switchToWindow(ac.getRegisteredWindow("newwindow"))) ac.skipToFail();
                })
            .returnToComposerBuilder()
            .build("openAndCloseWindow", false, false);
        browserRunner.executeComposer(actionComposer).get(3000, TimeUnit.MILLISECONDS);
        assertTrue(actionComposer.isSuccess());
    }
    
    /**
     *
     * @throws Exception
     */
    @Test
    public void scrollToView() throws Exception {
        AtomicLong initial = new AtomicLong(-1);
        AtomicLong after = new AtomicLong(-1);
        ActionComposer actionComposer = getDefinedActionComposerBuilder()
            .prepareActionSequence()
                .waitUntil(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.id("divScroll")), 1000)
                .custom(ac->{
                    WebElement element = ac.getBrsDriver().findElements(By.id("divScroll")).stream().findFirst().orElse(null);
                    Long top = (Long)((JavascriptExecutor) ac.getBrsDriver()).executeScript("return arguments[0].getBoundingClientRect().top;", element);
                    initial.set(top);
                })
                .scrollToView(By.id("divScroll"), true)
                .custom(ac->{
                    WebElement element = ac.getBrsDriver().findElements(By.id("divScroll")).stream().findFirst().orElse(null);
                    Long top = (Long)((JavascriptExecutor) ac.getBrsDriver()).executeScript("return arguments[0].getBoundingClientRect().top;", element);
                    after.set(top);
                })
            .returnToComposerBuilder()
            .build("scrollToView", true, true);
        browserRunner.executeComposer(actionComposer).get(500000, TimeUnit.MILLISECONDS);
        assertTrue(String.format("scroll error, initial:%s, after:%s", initial.get(), after.get()), actionComposer.isSuccess() && initial.get()>0 && after.get()==0);
    }
    
    /**
     *
     * @throws Exception
     */
    @Test
    public void upload() throws Exception {
        AtomicReference<String> fileName = new AtomicReference<>("");
        ActionComposer actionComposer = getDefinedActionComposerBuilder()
            .prepareActionSequence()
                .waitUntil(ExpectedConditions.presenceOfElementLocated(By.id("flUpload")), 1000)
                .upload(By.id("flUpload"), new File(htmlFileUrl.getFile()).getAbsolutePath())
                .waitUntil(ExpectedConditions.visibilityOfElementLocated(By.id("flUpload")), 1000)
                .custom(ac->{
                    WebElement element = ac.getBrsDriver().findElements(By.id("flUpload")).stream().findFirst().orElse(null);
                    String name = (String)((JavascriptExecutor) ac.getBrsDriver()).executeScript("return arguments[0].files[0].name;", element);
                    fileName.set(name);
                })
            .returnToComposerBuilder()
            .build("upload", true, true);
        browserRunner.executeComposer(actionComposer).get(500000, TimeUnit.MILLISECONDS);
        assertTrue("upload error", actionComposer.isSuccess() && HTML_FILE_NAME.equalsIgnoreCase(fileName.get()));
    }
    
    /**
     *
     * @throws Exception
     */
    @Test
    public void onFail() throws Exception {
        StringBuilder sb = new StringBuilder();
        ActionComposer actionComposer = getEmptyActionComposerBuilder()
            .prepareActionSequence()
                .custom(ac->{
                    sb.append("custom");
                    throw new RuntimeException("some error");
                })
            .returnToComposerBuilder()
            .onFail(ac->{
                sb.append("fail");
            })
            .onSuccess(ac->{
                sb.append("success");
            })
            .build("onFail", false, false)
            .keepFailInfo(false);
        browserRunner.executeComposer(actionComposer).get(3000, TimeUnit.MILLISECONDS);
        assertTrue("customfail".equals(sb.toString()) && actionComposer.isFail());
    }
    
    /**
     *
     * @throws Exception
     */
    @Test
    public void onSuccess() throws Exception {
        StringBuilder sb = new StringBuilder();
        ActionComposer actionComposer = getEmptyActionComposerBuilder()
            .prepareActionSequence()
                .custom(ac->{
                    sb.append("custom");
                })
            .returnToComposerBuilder()
            .onFail(ac->{
                sb.append("fail");
            })
            .onSuccess(ac->{
                sb.append("success");
            })
            .build("onSuccess", false, false);
        browserRunner.executeComposer(actionComposer).get(3000, TimeUnit.MILLISECONDS);
        assertTrue("customsuccess".equals(sb.toString()) && actionComposer.isSuccess());
    }
    
    /**
     *
     * @throws Exception
     */
    @Test
    public void onDone() throws Exception {
        StringBuilder sb = new StringBuilder();
        ActionComposer actionComposer = getEmptyActionComposerBuilder()
            .prepareActionSequence()
                .custom(ac->{
                    sb.append("custom");
                })
            .returnToComposerBuilder()
            .onDone(ac->{
                sb.append("done");
            })
            .build("onDone", false, false);
        browserRunner.executeComposer(actionComposer).get(3000, TimeUnit.MILLISECONDS);
        assertTrue("customdone".equals(sb.toString()) && actionComposer.isDone());
    }
}
