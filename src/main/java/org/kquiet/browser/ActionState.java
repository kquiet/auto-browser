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
package org.kquiet.browser;

/**
 * The state of a browser action
 * @author Kimberly
 */
public enum ActionState {

    /**
     * Indicate the browser action is created.
     */
    Created
    ,

    /**
     * Indicate the browser action is running.
     */
    Running
    ,

    /**
     * Indicate the browser action has finished an execution phase with no error and is waiting for the execution of next phase.
     */
    CompleteWithNextPhase
    ,

    /**
     * Indicate the browser action has finished with error(s).
     */
    CompleteWithError
    ,

    /**
     * Indicate the browser action has finished with no error.
     */
    Complete
}
