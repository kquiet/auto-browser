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
package org.kquiet.concurrent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Kimberly
 */
public class PausableScheduledThreadPoolExecutor extends ScheduledThreadPoolExecutor{
    private static final Logger LOGGER = LoggerFactory.getLogger(PausableThreadPoolExecutor.class);
    private volatile boolean isPaused = false;
    private volatile CountDownLatch pauseLatch = new CountDownLatch(1);
    private final String poolPrefix;
    private final Consumer<Runnable> afterExecuteFunc;

    /**
     *
     */
    public PausableScheduledThreadPoolExecutor(){
        this("", 1);
    }
    
    /**
     *
     * @param poolPrefix
     * @param corePoolSize
     */
    public PausableScheduledThreadPoolExecutor(String poolPrefix, int corePoolSize){
        this(poolPrefix, corePoolSize, null);
    }
    
    /**
     *
     * @param poolPrefix
     * @param corePoolSize
     * @param afterExecuteFunc
     */
    public PausableScheduledThreadPoolExecutor(String poolPrefix, int corePoolSize, Consumer<Runnable> afterExecuteFunc){
        super(corePoolSize, new CommonThreadFactory(poolPrefix));
        this.poolPrefix = poolPrefix;
        this.afterExecuteFunc = afterExecuteFunc;
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);
        try {
            while (isPaused){
                LOGGER.info("{} is pending...", this.poolPrefix);
                pauseLatch.await();
                LOGGER.info("{} comes back to service.", this.poolPrefix);
            }
        } catch (InterruptedException ie) {
            t.interrupt();
        }
    }
    
    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        try{
            super.afterExecute(r, t);
        }
        finally{
            if (this.afterExecuteFunc!=null){
                this.afterExecuteFunc.accept(r);
            }
        }
    }

    /**
     *
     */
    public synchronized void pause() {
        isPaused = true;
    }

    /**
     *
     */
    public synchronized void resume() {
        isPaused = false;
        pauseLatch.countDown();
        pauseLatch = new CountDownLatch(1);// renew for next pause to use
    }
    
    /**
     *
     * @return
     */
    public synchronized boolean isPaused(){
        return isPaused;
    }
}
