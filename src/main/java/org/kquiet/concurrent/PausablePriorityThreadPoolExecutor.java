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

import java.util.concurrent.Callable;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 *
 * @author Kimberly
 */
public class PausablePriorityThreadPoolExecutor extends PausableThreadPoolExecutor {

    /**
     *
     */
    public PausablePriorityThreadPoolExecutor(){
        this("", 1, 1);
    }
    
    /**
     *
     * @param poolPrefix
     * @param corePoolSize
     * @param maximumPoolSize
     */
    public PausablePriorityThreadPoolExecutor(String poolPrefix, int corePoolSize, int maximumPoolSize){
        this(poolPrefix, corePoolSize, maximumPoolSize, 5);
    }
    
    /**
     *
     * @param poolPrefix
     * @param corePoolSize
     * @param maximumPoolSize
     * @param queueSize
     */
    public PausablePriorityThreadPoolExecutor(String poolPrefix, int corePoolSize, int maximumPoolSize, int queueSize){
        this(poolPrefix, corePoolSize, maximumPoolSize, queueSize, null);
    }
    
    /**
     *
     * @param poolPrefix
     * @param corePoolSize
     * @param maximumPoolSize
     * @param afterExecuteFunc
     */
    public PausablePriorityThreadPoolExecutor(String poolPrefix, int corePoolSize, int maximumPoolSize, Consumer<Runnable> afterExecuteFunc){
        this(poolPrefix, corePoolSize, maximumPoolSize, 5, afterExecuteFunc);
    }
    
    /**
     *
     * @param poolPrefix
     * @param corePoolSize
     * @param maximumPoolSize
     * @param queueSize
     * @param afterExecuteFunc
     */
    public PausablePriorityThreadPoolExecutor(String poolPrefix, int corePoolSize, int maximumPoolSize, int queueSize, Consumer<Runnable> afterExecuteFunc){
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
    public PausablePriorityThreadPoolExecutor(String poolPrefix, int corePoolSize, int maximumPoolSize, int queueSize, long keepAliveTime, TimeUnit unit, Consumer<Runnable> afterExecuteFunc){
        super(poolPrefix, corePoolSize, maximumPoolSize, keepAliveTime, unit, new PriorityBlockingQueue<Runnable>(queueSize, new PriorityRunnableFutureComparator()), new CommonThreadFactory(poolPrefix), afterExecuteFunc);
    }
    
    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        RunnableFuture<T> newTaskFor = super.newTaskFor(callable);
        return new PriorityRunnableFuture<>(newTaskFor, ((Prioritized) callable).getPriority());
    }
    
    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        RunnableFuture<T> newTaskFor = super.newTaskFor(runnable, value);
        return new PriorityRunnableFuture<>(newTaskFor, ((Prioritized) runnable).getPriority());
    }
}
