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

/**
 * {@link Prioritized} is for internal use and should be implemented by the classes whose instances are intended to be executed by {@link PausablePriorityThreadPoolExecutor}.
 * 
 * @author Kimberly
 */
public interface Prioritized {

    /**
     * Get priority. Smaller value means higher priority.
     * 
     * @return priority
     */
    int getPriority();
}
