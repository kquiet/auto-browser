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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author Kimberly
 * @param <T>
 */
public class PriorityRunnableFuture<T> implements RunnableFuture<T> {
    private static final AtomicLong CREATE_SEQUENCE = new AtomicLong(Long.MIN_VALUE);

    private final RunnableFuture<T> src;
    private final int priority;
    private final long seqNum;

    /**
     *
     * @param other
     * @param priority
     */
    public PriorityRunnableFuture(RunnableFuture<T> other, int priority) {
        this.src = other;
        this.priority = priority;
        this.seqNum = CREATE_SEQUENCE.getAndIncrement();
    }

    /**
     *
     * @return
     */
    public int getPriority() {
        return priority;
    }
    
    /**
     *
     * @return
     */
    public long getCreateSequence(){
        return seqNum;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return src.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return src.isCancelled();
    }

    @Override
    public boolean isDone() {
        return src.isDone();
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        return src.get();
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return src.get();
    }

    @Override
    public void run() {
        src.run();
    }
}