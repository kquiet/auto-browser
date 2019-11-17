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

import java.io.Serializable;
import java.util.Comparator;

/**
 * Comparator for {@link PriorityRunnableFuture}. This is for internal use.
 * 
 * @author Kimberly
 */
class PriorityRunnableFutureComparator implements Comparator<Runnable>, Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
    public int compare(Runnable o1, Runnable o2) {
        if (o1 == null && o2 == null) return 0;
        else if (o1 == null) return -1;
        else if (o2 == null) return 1;
        else {
            PriorityRunnableFuture<?> p1Obj = ((PriorityRunnableFuture<?>) o1);
            PriorityRunnableFuture<?> p2Obj = ((PriorityRunnableFuture<?>) o2);
            int p1 = p1Obj.getPriority();
            int p2 = p2Obj.getPriority();
            if (p1<p2) return -1;
            else if (p1>p2) return 1;
            else{
                long s1 = p1Obj.getCreateSequence();
                long s2 = p2Obj.getCreateSequence();
                if (s1<s2) return -1;
                else return 1;
            }
        }
    }
}
