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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 *
 * @author Kimberly
 */
public class PausableThreadPoolExecutor extends ThreadPoolExecutor{
    private static final Logger LOGGER = LoggerFactory.getLogger(PausableThreadPoolExecutor.class);
    private volatile boolean isPaused = false;
    private volatile CountDownLatch pauseLatch = new CountDownLatch(1);
    private final String poolPrefix;
    private final Consumer<Runnable> afterExecuteFunc;

    /**
     *
     */
    public PausableThreadPoolExecutor(){
        this("", 1, 1);
    }
    
    /**
     *
     * @param poolPrefix
     * @param corePoolSize
     * @param maximumPoolSize
     */
    public PausableThreadPoolExecutor(String poolPrefix, int corePoolSize, int maximumPoolSize){
        this(poolPrefix, corePoolSize, maximumPoolSize, Integer.MAX_VALUE);
    }
    
    /**
     *
     * @param poolPrefix
     * @param corePoolSize
     * @param maximumPoolSize
     * @param queueSize
     */
    public PausableThreadPoolExecutor(String poolPrefix, int corePoolSize, int maximumPoolSize, int queueSize){
        this(poolPrefix, corePoolSize, maximumPoolSize, queueSize, null);
    }
    
    /**
     *
     * @param poolPrefix
     * @param corePoolSize
     * @param maximumPoolSize
     * @param afterExecuteFunc
     */
    public PausableThreadPoolExecutor(String poolPrefix, int corePoolSize, int maximumPoolSize, Consumer<Runnable> afterExecuteFunc){
        this(poolPrefix, corePoolSize, maximumPoolSize, Integer.MAX_VALUE, afterExecuteFunc);
    }
    
    /**
     *
     * @param poolPrefix
     * @param corePoolSize
     * @param maximumPoolSize
     * @param queueSize
     * @param afterExecuteFunc
     */
    public PausableThreadPoolExecutor(String poolPrefix, int corePoolSize, int maximumPoolSize, int queueSize, Consumer<Runnable> afterExecuteFunc){
        this(poolPrefix, corePoolSize, maximumPoolSize, queueSize, 10L, TimeUnit.MINUTES, afterExecuteFunc);
    }
    
    /**
     *
     * @param poolPrefix
     * @param corePoolSize
     * @param maximumPoolSize
     * @param queueSize
     * @param keepAliveTime
     * @param unit
     * @param afterExecuteFunc
     */
    public PausableThreadPoolExecutor(String poolPrefix, int corePoolSize, int maximumPoolSize, int queueSize, long keepAliveTime, TimeUnit unit, Consumer<Runnable> afterExecuteFunc){
        this(poolPrefix, corePoolSize, maximumPoolSize, keepAliveTime, unit, (queueSize<=0? new SynchronousQueue<>():new LinkedBlockingQueue<>(queueSize)), new CommonThreadFactory(poolPrefix), afterExecuteFunc);
    }

    /**
     *
     * @param poolPrefix
     * @param corePoolSize
     * @param maximumPoolSize
     * @param keepAliveTime
     * @param unit
     * @param workQueue
     * @param threadFactory
     * @param afterExecuteFunc
     */
    public PausableThreadPoolExecutor(String poolPrefix, int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, Consumer<Runnable> afterExecuteFunc){
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
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
