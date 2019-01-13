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

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

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
    @BeforeClass
    public static void setUpClass() {
        browserRunnerOne = TestHelper.createRunner(1);
        browserRunnerTwo = TestHelper.createRunner(2);
    }
    
    /**
     *
     */
    @AfterClass
    public static void tearDownClass() {
        browserRunnerOne.close();
        browserRunnerTwo.close();
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
        boolean waitResult = ready.await(3000, TimeUnit.MILLISECONDS);
        assertTrue("actual:"+sb.toString(), waitResult && sb.toString().matches("Q100Q1Q2Q3Q4Q5"));
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
            .build("priorityTest-lowerPriority")
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
            .build("priorityTest-higherPriority")
            .setPriority(1);
        
        Future<?> lowerPriorityFuture = browserRunnerOne.executeComposer(lowerPriorityComposer);
        Future<?> higherPriorityFuture = browserRunnerOne.executeComposer(higherPriorityComposer);
        higherPriorityFuture.get(1000, TimeUnit.MILLISECONDS);
        lowerPriorityFuture.get(1000, TimeUnit.MILLISECONDS);
        
        assertTrue("actual:"+sb.toString(), sb.toString().matches("(L+H+)|(H+L+)"));
    }
    
    /**
     *
     * @throws Exception
     */
    @Test
    public void interleave() throws Exception {
        StringBuilder sb = new StringBuilder();
        ActionComposer composer1 = getEmptyActionComposerBuilder()
            .prepareActionSequence()
                .prepareWaitUntil(driver->{
                    synchronized(sb){sb.append("L");}
                    return false;
                }, 300)
                    .withTimeoutCallback(ac->{})
                    .done()
                .returnToComposerBuilder()
            .build("interleaveTest-1")
            .keepFailInfo(false)
            .setPriority(1);
        
        ActionComposer composer2 = getEmptyActionComposerBuilder()
            .prepareActionSequence()
                .prepareWaitUntil(driver->{
                    synchronized(sb){sb.append("H");}
                    return false;
                }, 300)
                    .withTimeoutCallback(ac->{})
                    .done()
                .returnToComposerBuilder()
            .build("interleaveTest-2")
            .keepFailInfo(false)
            .setPriority(1);
        
        Future<?> future1 = browserRunnerTwo.executeComposer(composer1);
        Future<?> future2 = browserRunnerTwo.executeComposer(composer2);
        future2.get(1000, TimeUnit.MILLISECONDS);
        future1.get(1000, TimeUnit.MILLISECONDS);
        
        assertTrue("actual:"+sb.toString(), !sb.toString().matches("(L+H+)|(H+L+)"));
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
        childComposer.get(1000, TimeUnit.MILLISECONDS);
        
        assertEquals("parentchild", sb.toString());
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
        browserRunnerOne.executeComposer(actionComposer).get(3000, TimeUnit.MILLISECONDS);
        assertTrue("actual:"+sb.toString(), actionComposer.isSuccess() && "successdone".equals(sb.toString()) && !result.get());
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
        browserRunnerOne.executeComposer(actionComposer).get(3000, TimeUnit.MILLISECONDS);
        assertTrue("actual:"+sb.toString(), actionComposer.isFail() && "faildone".equals(sb.toString()) && !result.get());
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
            .build("skipToFailAndClose", true, true);
        browserRunnerOne.executeComposer(actionComposer).get(3000, TimeUnit.MILLISECONDS);
        assertTrue("actual:"+sb.toString(), actionComposer.isFail() && "faildone".equals(sb.toString()) && !result.get());
    }
}
