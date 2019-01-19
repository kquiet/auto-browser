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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.kquiet.browser.ActionComposer;
import org.kquiet.browser.ActionComposerBuilder;
import org.kquiet.browser.ActionRunner;

/**
 *
 * @author Kimberly
 */
public class CompositeTest {
    private static ActionRunner browserRunnerOne;
    private static ActionRunner browserRunnerTwo;
    
    /**
     *
     */
    public CompositeTest() {
    }
    
    /**
     *
     */
    @BeforeAll
    public static void setUpClass() {
        browserRunnerOne = TestHelper.createRunner(1);
        browserRunnerTwo = TestHelper.createRunner(2);
    }
    
    /**
     *
     */
    @AfterAll
    public static void tearDownClass() {
        browserRunnerOne.close();
        browserRunnerTwo.close();
    }
    
    /**
     *
     */
    @BeforeEach
    public void setUp(){
    }
    
    /**
     *
     */
    @AfterEach
    public void tearDown() {
    }
    
    private ActionComposerBuilder getEmptyActionComposerBuilder(){
        return new ActionComposerBuilder();
    }
    
    /**
     *
     * @throws Exception
     */
    @Test
    public void priority() throws Exception {
        final StringBuilder sb = new StringBuilder();
        CountDownLatch ready = new CountDownLatch(6);
        browserRunnerOne.executeAction(()->{
            sb.append("Q100");
            ready.countDown();
            for (int i=5;i>=1;i--){
                int priority = i;
                String str = "Q"+i;
                browserRunnerOne.executeAction(()->{
                    synchronized(sb){sb.append(str);}
                    ready.countDown();
                }, priority);
            }
        }, 100);
        
        AtomicBoolean waitResult = new AtomicBoolean(false);
        assertAll(
            ()->assertDoesNotThrow(()->waitResult.set(ready.await(3000, TimeUnit.MILLISECONDS)), "not complete in time"),
            ()->assertTrue(waitResult.get(), "count down timeout"),
            ()->assertEquals("Q100Q1Q2Q3Q4Q5", sb.toString(), "wrong priority sequence"));
    }
    
    /**
     *
     * @throws Exception
     */
    @Test
    public void singleComposeredRunner() throws Exception {
        StringBuilder sb = new StringBuilder();
        ActionComposer lowerPriorityComposer = getEmptyActionComposerBuilder()
            .prepareActionSequence()
                .prepareWaitUntil(driver->{
                    synchronized(sb){sb.append("L");}
                    return false;
                }, 300)
                    .withTimeoutCallback(ac->{})
                    .done()
                .returnToComposerBuilder()
            .build("singleComposeredRunner-lowerPriority")
            .setPriority(2);
        
        ActionComposer higherPriorityComposer = getEmptyActionComposerBuilder()
            .prepareActionSequence()
                .prepareWaitUntil(driver->{
                    synchronized(sb){sb.append("H");}
                    return false;
                }, 300)
                    .withTimeoutCallback(ac->{})
                    .done()
                .returnToComposerBuilder()
            .build("singleComposeredRunner-higherPriority")
            .setPriority(1);
        
        Future<?> lowerPriorityFuture = browserRunnerOne.executeComposer(lowerPriorityComposer);
        Future<?> higherPriorityFuture = browserRunnerOne.executeComposer(higherPriorityComposer);
        
        assertAll(
            ()->assertDoesNotThrow(()->{higherPriorityFuture.get(1000, TimeUnit.MILLISECONDS);lowerPriorityFuture.get(1000, TimeUnit.MILLISECONDS);}, "not complete in time"),
            ()->assertTrue(sb.toString().matches("(L+H+)|(H+L+)"), "expected:(L+H+)|(H+L+), actual:"+sb.toString()),
            ()->assertTrue(lowerPriorityComposer.isSuccessfulDone(), "lower priority composer fail"),
            ()->assertTrue(higherPriorityComposer.isSuccessfulDone(), "higher priority composer fail"));
    }
    
    /**
     *
     * @throws Exception
     */
    @Test
    public void interleave() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        ActionComposer composer1 = getEmptyActionComposerBuilder()
            .prepareActionSequence()
                .customMultiPhase(ps->ac->{
                    if (counter.get()%2==0 && counter.getAndIncrement()>10) ps.noNextPhase();
                    if (!ac.getWebDriver().getWindowHandle().equals(ac.getFocusWindow())){
                        ac.skipToFail();
                        ps.noNextPhase();
                    }
                })
                .returnToComposerBuilder()
            .build("interleave-1")
            .setPriority(1);
        
        ActionComposer composer2 = getEmptyActionComposerBuilder()
            .prepareActionSequence()
                .customMultiPhase(ps->ac->{
                    if (counter.get()%2==1 && counter.getAndIncrement()>10) ps.noNextPhase();
                    if (!ac.getWebDriver().getWindowHandle().equals(ac.getFocusWindow())){
                        ac.skipToFail();
                        ps.noNextPhase();
                    }
                })
                .returnToComposerBuilder()
            .build("interleave-2")
            .setPriority(1);
        
        Future<?> future1 = browserRunnerTwo.executeComposer(composer1);
        Future<?> future2 = browserRunnerTwo.executeComposer(composer2);
        future2.get(1000, TimeUnit.MILLISECONDS);future1.get(1000, TimeUnit.MILLISECONDS);
        assertAll(
            ()->assertDoesNotThrow(()->{future2.get(1000, TimeUnit.MILLISECONDS);future1.get(1000, TimeUnit.MILLISECONDS);}, "not complete in time"),
            ()->assertTrue(composer1.isSuccessfulDone(), "composer1 fail"),
            ()->assertTrue(composer2.isSuccessfulDone(), "composer2 fail"));
    }
    
    /**
     *
     * @throws Exception
     */
    @Test
    public void continuous() throws Exception {
        StringBuilder sb = new StringBuilder();
        ActionComposer parentComposer = getEmptyActionComposerBuilder()
            .prepareActionSequence()
                .custom(ac->sb.append("parent"))
                .returnToComposerBuilder()
            .build("continuousTest-parent");
        
        ActionComposer childComposer = getEmptyActionComposerBuilder()
            .prepareActionSequence()
                .custom(ac->sb.append("child"))
                .returnToComposerBuilder()
            .build("continuousTest-child");
        
        parentComposer.continueWith(childComposer);
        browserRunnerTwo.executeComposer(parentComposer);
        
        assertAll(
            ()->assertDoesNotThrow(()->childComposer.get(1000, TimeUnit.MILLISECONDS), "not complete in time"),
            ()->assertEquals("parentchild", sb.toString(), "composer not executed in order"),
            ()->assertTrue(parentComposer.isSuccessfulDone(), "parent composer composer fail"),
            ()->assertTrue(childComposer.isSuccessfulDone(), "child composer fail"));
    }
    
    /**
     *
     * @throws Exception
     */
    @Test    
    public void skipToSuccessAndClose() throws Exception{
        StringBuilder sb = new StringBuilder();
        AtomicBoolean result = new AtomicBoolean(true);
        ActionComposer actionComposer = getEmptyActionComposerBuilder()
            .prepareActionSequence()
                .custom(ac->ac.skipToSuccess())
                .returnToComposerBuilder()
            .onSuccess(ac->{
                sb.append("success");
            })
            .onFail(ac->{
                sb.append("fail");
            })
            .onDone(ac->{
                sb.append("done");
                String focusWindow = ac.getRegisteredWindow("");
                result.set(ac.getWebDriver().getWindowHandles().contains(focusWindow));
            })
            .build("skipToSuccessAndClose", true, true);
        
        assertAll(
            ()->assertDoesNotThrow(()->browserRunnerOne.executeComposer(actionComposer).get(3000, TimeUnit.MILLISECONDS), "not complete in time"),
            ()->assertFalse(result.get(), "window not closed"),
            ()->assertEquals("successdone", sb.toString(), "on success/done not triggered"),
            ()->assertTrue(actionComposer.isSuccessfulDone(), "composer fail"));
    }
    
    /**
     *
     * @throws Exception
     */
    @Test    
    public void skipToFailAndClose() throws Exception{
        StringBuilder sb = new StringBuilder();
        AtomicBoolean result = new AtomicBoolean(true);
        ActionComposer actionComposer = getEmptyActionComposerBuilder()
            .prepareActionSequence()
                .custom(ac->ac.skipToFail())
                .returnToComposerBuilder()
            .onSuccess(ac->{
                sb.append("success");
            })
            .onFail(ac->{
                sb.append("fail");
            })
            .onDone(ac->{
                sb.append("done");
                String focusWindow = ac.getRegisteredWindow("");
                result.set(ac.getWebDriver().getWindowHandles().contains(focusWindow));
            })
            .build("skipToFailAndClose", true, true);

        assertAll(
            ()->assertDoesNotThrow(()->browserRunnerOne.executeComposer(actionComposer).get(3000, TimeUnit.MILLISECONDS), "not complete in time"),
            ()->assertFalse(result.get(), "window not closed"),
            ()->assertEquals("faildone", sb.toString(), "on fail/done not triggered"),
            ()->assertTrue(actionComposer.isFail(), "composer not fail"));
    }
    
    /**
     *
     * @throws Exception
     */
    @Test    
    public void failAndClose() throws Exception{
        StringBuilder sb = new StringBuilder();
        AtomicBoolean result = new AtomicBoolean(true);
        ActionComposer actionComposer = getEmptyActionComposerBuilder()
            .prepareActionSequence()
                .custom(ac->{throw new RuntimeException("custom exception for failAndClose");})
                .returnToComposerBuilder()
            .onSuccess(ac->{
                sb.append("success");
            })
            .onFail(ac->{
                sb.append("fail");
            })
            .onDone(ac->{
                sb.append("done");
                String focusWindow = ac.getRegisteredWindow("");
                result.set(ac.getWebDriver().getWindowHandles().contains(focusWindow));
            })
            .build("failAndClose", true, true);

        assertAll(
            ()->assertDoesNotThrow(()->browserRunnerOne.executeComposer(actionComposer).get(3000, TimeUnit.MILLISECONDS), "not complete in time"),
            ()->assertFalse(result.get(), "window not closed"),
            ()->assertEquals("faildone", sb.toString(), "on fail/done not triggered"),
            ()->assertTrue(actionComposer.isFail(), "composer not fail"));
    }
}
