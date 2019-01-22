/*
 * Copyright 2019 kquiet.
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

/**
 * Prioritized {@link Runnable}. This class is for internal use.
 * 
 * @author Kimberly
 */
public class PriorityRunnable implements Prioritized, Runnable{
    private final Runnable runnable;
    private final int priority;
    
    /**
     *
     * @param runnable wrapped {@link Runnable}
     * @param priority priority
     */
    public PriorityRunnable(Runnable runnable, int priority){
        this.runnable = runnable;
        this.priority = priority;
    }
    
    @Override
    public int getPriority(){
        return priority;
    }

    @Override
    public void run() {
        runnable.run();
    }
}
