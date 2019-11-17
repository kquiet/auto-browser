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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Phaser;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Pausable {@link ThreadPoolExecutor}. This class is for internal use.
 * 
 * @author Kimberly
 */
public class PausableThreadPoolExecutor extends ThreadPoolExecutor{
    private static final Logger LOGGER = LoggerFactory.getLogger(PausableThreadPoolExecutor.class);
    private volatile boolean isPaused = false;
    private final Phaser pausePhaser = new Phaser(1);
    private volatile int pausePhaseNumber = pausePhaser.getPhase();
    private final String poolPrefix;
    private final Consumer<Runnable> afterExecuteFunc;

    /**
     * Create a {@link PausableThreadPoolExecutor} with core/maximum pool size set to one
     */
    public PausableThreadPoolExecutor(){
        this("", 1, 1);
    }
    
    /**
     *
     * @param poolPrefix prefix name of thread pool
     * @param corePoolSize core pool size
     * @param maximumPoolSize maximum pool size
     */
    public PausableThreadPoolExecutor(String poolPrefix, int corePoolSize, int maximumPoolSize){
        this(poolPrefix, corePoolSize, maximumPoolSize, Integer.MAX_VALUE);
    }
    
    /**
     *
     * @param poolPrefix prefix name of thread pool
     * @param corePoolSize core pool size
     * @param maximumPoolSize maximum pool size
     * @param queueSize the maximum queue size used for holding tasks before they are executed
     */
    public PausableThreadPoolExecutor(String poolPrefix, int corePoolSize, int maximumPoolSize, int queueSize){
        this(poolPrefix, corePoolSize, maximumPoolSize, queueSize, null);
    }
    
    /**
     *
     * @param poolPrefix prefix name of thread pool
     * @param corePoolSize core pool size
     * @param maximumPoolSize maximum pool size
     * @param afterExecuteFunc the function to execute after any task is executed
     */
    public PausableThreadPoolExecutor(String poolPrefix, int corePoolSize, int maximumPoolSize, Consumer<Runnable> afterExecuteFunc){
        this(poolPrefix, corePoolSize, maximumPoolSize, Integer.MAX_VALUE, afterExecuteFunc);
    }
    
    /**
     *
     * @param poolPrefix prefix name of thread pool
     * @param corePoolSize core pool size
     * @param maximumPoolSize maximum pool size
     * @param queueSize the maximum queue size used for holding tasks before they are executed
     * @param afterExecuteFunc the function to execute after any task is executed
     */
    public PausableThreadPoolExecutor(String poolPrefix, int corePoolSize, int maximumPoolSize, int queueSize, Consumer<Runnable> afterExecuteFunc){
        this(poolPrefix, corePoolSize, maximumPoolSize, queueSize, 10L, TimeUnit.MINUTES, afterExecuteFunc);
    }
    
    /**
     *
     * @param poolPrefix prefix name of thread pool
     * @param corePoolSize core pool size
     * @param maximumPoolSize maximum pool size
     * @param queueSize the maximum queue size used for holding tasks before they are executed
     * @param keepAliveTime when the number of threads is greater than the core, this is the maximum time that excess idle threads will wait for new tasks before terminating.
     * @param unit the time unit for the {@code keepAliveTime} argument
     * @param afterExecuteFunc the function to execute after any task is executed
     */
    public PausableThreadPoolExecutor(String poolPrefix, int corePoolSize, int maximumPoolSize, int queueSize, long keepAliveTime, TimeUnit unit, Consumer<Runnable> afterExecuteFunc){
        this(poolPrefix, corePoolSize, maximumPoolSize, keepAliveTime, unit, (queueSize<=0? new SynchronousQueue<>():new LinkedBlockingQueue<>(queueSize)), new CommonThreadFactory(poolPrefix), afterExecuteFunc);
    }

    /**
     *
     * @param poolPrefix prefix name of thread pool
     * @param corePoolSize core pool size
     * @param maximumPoolSize maximum pool size
     * @param keepAliveTime when the number of threads is greater than the core, this is the maximum time that excess idle threads will wait for new tasks before terminating.
     * @param unit the time unit for the {@code keepAliveTime} argument
     * @param workQueue the queue used for holding tasks before they are executed
     * @param threadFactory the factory to use when the executor creates a new thread
     * @param afterExecuteFunc the function to execute after any task is executed
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
                LOGGER.info("{}: thread-{} is pending...", this.poolPrefix, t.getId());
                pausePhaser.awaitAdvanceInterruptibly(pausePhaseNumber);
                LOGGER.info("{}: thread-{} comes back to service.", this.poolPrefix, t.getId());
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
     * Pause the execution of invoking {@link PausableThreadPoolExecutor}. Any executing task is not affected.
     */
    public synchronized void pause() {
        isPaused = true;
    }

    /**
     * Resume the execution of invoking {@link PausableThreadPoolExecutor}.
     */
    public synchronized void resume() {
        isPaused = false;
        pausePhaser.arrive();
        pausePhaseNumber = pausePhaser.getPhase();
    }
    
    /**
     *
     * @return whether invoking {@link PausableThreadPoolExecutor} is paused.
     */
    public synchronized boolean isPaused(){
        return isPaused;
    }
}
