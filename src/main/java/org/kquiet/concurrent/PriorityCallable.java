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

/**
 *
 * @author Kimberly
 * @param <T>
 */
public class PriorityCallable<T> implements Prioritized, Callable<T>{
    private final Callable<T> callable;
    private final int priority;
    
    /**
     *
     * @param callable
     * @param priority
     */
    public PriorityCallable(Callable<T> callable, int priority){
        this.callable = callable;
        this.priority = priority;
    }

    @Override
    public T call() throws Exception {
        return this.callable.call();
    }
    
    /**
     *
     * @return
     */
    @Override
    public int getPriority(){
        return this.priority;
    }
}
