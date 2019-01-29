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
package org.kquiet.browser;

import org.kquiet.browser.action.Composable;

/**
 * 
 * @author Kimberly
 */
public interface ActionSequenceContainer {
    /**
     * Add action to the head of the action sequence.
     * 
     * @param action action to add
     * @return self reference
     */
    ActionSequenceContainer addToHead(Composable action);
    
    /**
     * Add action to the tail of the action sequence.
     * 
     * @param action action to add
     * @return self reference
     */
    ActionSequenceContainer addToTail(Composable action);
    
    /**
     * Add action to the specified position of the action sequence.
     * 
     * @param action action to add
     * @param position the position(zero-based) to add given action
     * @return self reference
     */
    ActionSequenceContainer addToPosition(Composable action, int position);
    
    /**
     * Add action to the tail of the action sequence.
     * 
     * @param action action to add
     * @return self reference
     */
    default ActionSequenceContainer add(Composable action){
        return addToTail(action);
    }
}
