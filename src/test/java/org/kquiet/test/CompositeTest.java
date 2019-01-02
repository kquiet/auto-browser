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
    public void concurrentOne() throws Exception {
        StringBuilder sb = new StringBuilder();
        ActionComposer lowerPriorityComposer = getEmptyActionComposerBuilder()
            .prepareActionSequence()
                .waitUntil(driver->{
                    synchronized(sb){sb.append("L");}
                    return false;
                }, 300)
            .returnToComposerBuilder()
            .build("priorityTest-lowerPriority")
            .cacheFailInfo(false);
        lowerPriorityComposer.setPriority(2);
        
        ActionComposer higherPriorityComposer = getEmptyActionComposerBuilder()
            .prepareActionSequence()
                .waitUntil(driver->{
                    synchronized(sb){sb.append("H");}
                    return false;
                }, 300)
            .returnToComposerBuilder()
            .build("priorityTest-higherPriority")
            .cacheFailInfo(false);
        higherPriorityComposer.setPriority(1);
        
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
    public void continuous() throws Exception {
        StringBuilder sb = new StringBuilder();
        ActionComposer parentComposer = getEmptyActionComposerBuilder()
            .prepareActionSequence()
                .custom(ac->{
                    sb.append("parent");
                })
            .returnToComposerBuilder()
            .build("continuousTest-parent");
        
        ActionComposer childComposer = getEmptyActionComposerBuilder()
            .prepareActionSequence()
                .custom(ac->{
                    sb.append("child");
                })
            .returnToComposerBuilder()
            .build("continuousTest-child");
        
        parentComposer.continueWith(childComposer);
        
        browserRunnerTwo.executeComposer(parentComposer);
        childComposer.get(1000, TimeUnit.MILLISECONDS);
        
        assertEquals("parentchild", sb.toString());
    }
}
