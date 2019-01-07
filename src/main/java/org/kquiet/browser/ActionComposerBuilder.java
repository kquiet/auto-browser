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

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.function.Predicate;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;

import org.kquiet.browser.action.MultiPhaseAction;
import org.kquiet.browser.action.Click;
import org.kquiet.browser.action.CloseWindow;
import org.kquiet.browser.action.Custom;
import org.kquiet.browser.action.GetUrl;
import org.kquiet.browser.action.OpenWindow;
import org.kquiet.browser.action.PostForm;
import org.kquiet.browser.action.JustWait;
import org.kquiet.browser.action.Select;
import org.kquiet.browser.action.SendKey;
import org.kquiet.browser.action.WaitUntil;
import org.kquiet.browser.action.Select.SelectBy;
import org.kquiet.browser.action.IfThenElse;
import org.kquiet.browser.action.ScrollToView;
import org.kquiet.browser.action.Upload;

/**
 * A builder to build {@link ActionComposer} in a fluent way.
 * <p>Below example constructs an {@link ActionComposer} that searches for the link to source code of {@link ActionComposerBuilder}, and then click it:</p>
 * <pre>
 * ActionComposer actionComposer = new ActionComposerBuilder()
 *      .prepareActionSequence()
 *          .getUrl("https://github.com/kquiet/auto-browser/find/master")
 *          .waitUntil(ExpectedConditions.elementToBeClickable(By.id("tree-finder-field")), 3000)
 *          .sendKey(By.id("tree-finder-field"), "ActionComposerBuilder")
 *          .waitUntil(ExpectedConditions.elementToBeClickable(By.xpath("//mark[text()='ActionComposerBuilder']")), 3000)
 *          .click(By.xpath("//mark[text()='ActionComposerBuilder']"))
 *          .returnToComposerBuilder()
 *      .build(true, false);
 * </pre>
 * 
 * More complicated {@link ActionComposer} can be built through built-in {@link org.kquiet.browser.action actions}.
 * {@link ActionComposerBuilder} includes {@link org.kquiet.browser.ActionComposerBuilder.ActionSequenceBuilder inner builders} for these actions as well.
 * 
 * @author Kimberly
 */
public class ActionComposerBuilder{
    private volatile ActionComposer actionComposer = null;
    
    /**
     * Create a new {@link ActionComposerBuilder}
     */
    public ActionComposerBuilder(){
        prepareNextActionComposer();
    }
    
    private void prepareNextActionComposer(){
        if (actionComposer==null){
            actionComposer = new ActionComposer();
        }
    }
    
    private void clearActionComposer(){
        this.actionComposer = null;
    }
    
    /**
     * Add action to the first position of the actoin list in {@link ActionComposer}.
     * 
     * @param action action to add
     * @return this {@link ActionComposerBuilder}
     */
    public ActionComposerBuilder addToFirst(MultiPhaseAction action){
        prepareNextActionComposer();
        actionComposer.addActionToFirst(action);
        return this;
    }
    
    /**
     * Add action to the last position of the actoin list in {@link ActionComposer}.
     * 
     * @param action action to add
     * @return this {@link ActionComposerBuilder}
     */
    public ActionComposerBuilder addToLast(MultiPhaseAction action){
        prepareNextActionComposer();
        actionComposer.addActionToLast(action);
        return this;
    }
    
    /**
     * Add action to the specified position of the actoin list in {@link ActionComposer}.
     * 
     * @param action action to add
     * @param position the position(zero-based) to add action
     * @return this {@link ActionComposerBuilder}
     */
    public ActionComposerBuilder addToIndex(MultiPhaseAction action, int position){
        prepareNextActionComposer();
        actionComposer.addActionToIndex(action, position);
        return this;
    }
    
    /**
     * Set the fail callback function of {@link ActionComposer}.
     * 
     * @param func callback function
     * @return this {@link ActionComposerBuilder}
     * @see ActionComposer#setOnFailFunction(java.util.function.Consumer) 
     */
    public ActionComposerBuilder onFail(Consumer<ActionComposer> func){
        actionComposer.setOnFailFunction(func);
        return this;
    }
    
    /**
     * Set the success callback function of {@link ActionComposer}.
     * 
     * @param func callback function
     * @return this {@link ActionComposerBuilder}
     * @see ActionComposer#setOnSuccessFunction(java.util.function.Consumer) 
     */
    public ActionComposerBuilder onSuccess(Consumer<ActionComposer> func){
        actionComposer.setOnSuccessFunction(func);
        return this;
    }
    
    /**
     * Set the done callback function of {@link ActionComposer}.
     * 
     * @param func callback function
     * @return this {@link ActionComposerBuilder}
     * @see ActionComposer#setOnDoneFunction(java.util.function.Consumer) 
     */
    public ActionComposerBuilder onDone(Consumer<ActionComposer> func){
        actionComposer.setOnDoneFunction(func);
        return this;
    }
    
    /**
     * Finish building the {@link ActionComposer}.
     * 
     * @return the built {@link ActionComposer}
     */
    public ActionComposer build(){
        return build(UUID.randomUUID().toString(), true, true);
    }
    
    /**
     * Finish building the {@link ActionComposer} with window-control parameters.
     * 
     * @param openWindowFlag {@code true}: open window; {@code false}: not open
     * @param closeWindowFlag {@code true}: close window; {@code false}: not close
     * @return the built {@link ActionComposer}
     * @see ActionComposer#setOpenWindow(boolean) 
     * @see ActionComposer#setCloseWindow(boolean) 
     */
    public ActionComposer build(boolean openWindowFlag, boolean closeWindowFlag){
        return build(UUID.randomUUID().toString(), openWindowFlag, closeWindowFlag);
    }
    
    /**
     * Finish building the {@link ActionComposer} with its name.
     * 
     * @param name name of {@link ActionComposer}
     * @return the built {@link ActionComposer}
     */
    public ActionComposer build(String name){
        return build(name, true, true);
    }
    
    /**
     * Finish building the {@link ActionComposer} with its name and window-control parameters.
     * 
     * @param name name of {@link ActionComposer}
     * @param openWindowFlag {@code true}: open window; {@code false}: not open
     * @param closeWindowFlag {@code true}: close window; {@code false}: not close
     * @return the built {@link ActionComposer}
     * @see ActionComposer#setOpenWindow(boolean) 
     * @see ActionComposer#setCloseWindow(boolean) 
     */
    public ActionComposer build(String name, boolean openWindowFlag, boolean closeWindowFlag){
        final ActionComposer currentActionComposer = actionComposer;
        currentActionComposer.setName(name);
        currentActionComposer.setOpenWindow(openWindowFlag);
        currentActionComposer.setCloseWindow(closeWindowFlag);
        
        //clear and prepare for next build
        clearActionComposer();
        prepareNextActionComposer();
        return currentActionComposer;
    }
    
    /**
     * Start building a sequence of actions.
     * 
     * @return {@link ActionSequenceBuilder}
     */
    public ActionSequenceBuilder prepareActionSequence(){
        return new ActionSequenceBuilder(this);
    }
    
    /**
     * A builder to build the {@link org.kquiet.browser.action actions} into a sequence.
     */
    public class ActionSequenceBuilder{
        private final ActionComposerBuilder parentComposerBuilder;
        private final IfThenElseBuilder parentIfThenElseBuilder;
        private final List<MultiPhaseAction> actionList = new ArrayList<>();
        
        /**
         * Create a new {@link ActionSequenceBuilder} with an {@link ActionComposerBuilder} as its parent builder.
         * 
         * @param parentComposerBuilder parent builder
         */
        public ActionSequenceBuilder(ActionComposerBuilder parentComposerBuilder){
            if (parentComposerBuilder==null) throw new IllegalArgumentException("No parent builder");
            this.parentComposerBuilder = parentComposerBuilder;
            this.parentIfThenElseBuilder = null;
        }
        
        /**
         * Create a new {@link ActionSequenceBuilder} with an {@link IfThenElseBuilder} as its parent builder.
         * 
         * @param parentIfThenElseBuilder parent builder
         */
        public ActionSequenceBuilder(IfThenElseBuilder parentIfThenElseBuilder){
            if (parentIfThenElseBuilder==null) throw new IllegalArgumentException("No parent builder");
            this.parentComposerBuilder = null;
            this.parentIfThenElseBuilder = parentIfThenElseBuilder;
        }
        
        /**
         * Add an action to sequence.
         * 
         * @param action action to add
         * @return invoking {@link ActionSequenceBuilder}
         */
        public ActionSequenceBuilder add(MultiPhaseAction action){
            if (action!=null) actionList.add(action);
            return this;
        }
        
        /**
         * Finish building the sequence of actions so far and return control to root builder({@link ActionComposerBuilder}).
         * 
         * @return root builder({@link ActionComposerBuilder})
         */
        public ActionComposerBuilder returnToComposerBuilder(){
            if (parentIfThenElseBuilder!=null){
                actionList.forEach(action->{
                    parentIfThenElseBuilder.add(action);
                });
                return parentIfThenElseBuilder.returnToComposerBuilder();
            }
            else{
                actionList.forEach(action->{
                    parentComposerBuilder.addToLast(action);
                });
                return parentComposerBuilder;
            }
        }
        
        /**
         * Finish building the sequence of actions so far and return control to parent builder({@link IfThenElseBuilder}).
         * 
         * @return parent builder({@link IfThenElseBuilder})
         * @throws UnsupportedOperationException if parent builder is not {@link IfThenElseBuilder}.
         */
        public IfThenElseBuilder endActionSequence(){
            if (parentIfThenElseBuilder==null) throw new UnsupportedOperationException("Parent builder is not IfThenElseBuilder");
            actionList.forEach(action->{
                parentIfThenElseBuilder.add(action);
            });
            return parentIfThenElseBuilder;
        }
        
        /**
         * Add a {@link CloseWindow} to the sequence of actions.
         * 
         * @param closeAllRegistered {@code true}: close all regisetered windows; {@code false}: close only the focus window
         * @return invoking {@link ActionSequenceBuilder}
         */
        public ActionSequenceBuilder closeWindow(boolean closeAllRegistered){
            return new CloseWindowBuilder(this, closeAllRegistered).done();
        }

        /**
         * Start building a {@link CloseWindow}.
         * 
         * @param closeAllRegistered {@code true}: close all regisetered windows; {@code false}: close only the focus window
         * @return a new {@link CloseWindowBuilder} with invoking {@link ActionSequenceBuilder} as parent builder
         */
        public CloseWindowBuilder prepareCloseWindow(boolean closeAllRegistered){
            return new CloseWindowBuilder(this, closeAllRegistered);
        }

        /**
         * A builder to build {@link CloseWindow} in a fluent way.
         */
        public class CloseWindowBuilder extends InnerBuilderBase{
            private final boolean closeAllRegistered;

            /**
             * Create a new {@link CloseWindowBuilder} with specified {@link ActionSequenceBuilder} as parent builder.
             * 
             * @param parentActionSequenceBuilder parent builder({@link ActionSequenceBuilder})
             * @param closeAllRegistered {@code true}: close all regisetered windows; {@code false}: close only the focus window
             */
            public CloseWindowBuilder(ActionSequenceBuilder parentActionSequenceBuilder, boolean closeAllRegistered){
                super(parentActionSequenceBuilder);
                this.closeAllRegistered = closeAllRegistered;
            }

            /**
             * Finish building {@link CloseWindow} and add it to parent builder.
             * 
             * @return parent builder({@link ActionSequenceBuilder})
             */
            public ActionSequenceBuilder done(){
                MultiPhaseAction action = new CloseWindow(closeAllRegistered);
                return parentActionSequenceBuilder.add(action);
            }
        }
        
        /**
         * Add a {@link OpenWindow} to the sequence of actions.
         * 
         * @return invoking {@link ActionSequenceBuilder}
         */
        public ActionSequenceBuilder openWindow(){
            return new OpenWindowBuilder(this).done();
        }

        /**
         * Start building a {@link OpenWindow}.
         * 
         * @return a new {@link OpenWindowBuilder} with invoking {@link ActionSequenceBuilder} as parent builder
         */
        public OpenWindowBuilder prepareOpenWindow(){
            return new OpenWindowBuilder(this);
        }

        /**
         * A builder to build {@link OpenWindow} in a fluent way.
         */
        public class OpenWindowBuilder extends InnerBuilderBase{
            private boolean asComposerFocusWindow = false;
            private String registerName = UUID.randomUUID().toString();

            /**
             * Create a new {@link OpenWindowBuilder} with specified {@link ActionSequenceBuilder} as parent builder.
             * 
             * @param parentActionSequenceBuilder parent builder({@link ActionSequenceBuilder})
             */
            public OpenWindowBuilder(ActionSequenceBuilder parentActionSequenceBuilder){
                super(parentActionSequenceBuilder);
            }

            /**
             * Set opened window as focus window.
             * 
             * @return invoking {@link OpenWindowBuilder}
             */
            public OpenWindowBuilder withComposerFocus(){
                this.asComposerFocusWindow = true;
                return this;
            }

            /**
             * Register opened window with name.
             * 
             * @param registerName name to register
             * @return invoking {@link OpenWindowBuilder}
             */
            public OpenWindowBuilder registerAs(String registerName){
                this.registerName = registerName;
                return this;
            }

            /**
             * Finish building {@link OpenWindow} and add it to parent builder.
             * 
             * @return parent builder({@link ActionSequenceBuilder})
             */
            public ActionSequenceBuilder done(){
                MultiPhaseAction action = new OpenWindow(asComposerFocusWindow, registerName);
                return parentActionSequenceBuilder.add(action);
            }
        }
        
        /**
         * Add a {@link WaitUntil} to the sequence of actions.
         * 
         * @param <V> the expected return type of condition function
         * @param conditionFunc the condition function for evaluation by phases
         * @param totalTimeout the maximum amount of time to wait totally
         * @return invoking {@link ActionSequenceBuilder}
         */
        public <V> ActionSequenceBuilder waitUntil(Function<WebDriver,V> conditionFunc, int totalTimeout){
            return new WaitUntilBuilder<>(this, conditionFunc, totalTimeout).done();
        }

        /**
         * Start building a {@link WaitUntil}.
         * 
         * @param <V> the expected return type of condition function
         * @param conditionFunc the condition function for evaluation by phases
         * @param totalTimeout the maximum amount of time to wait totally
         * @return a new {@link WaitUntilBuilder} with invoking {@link ActionSequenceBuilder} as parent builder
         */
        public <V> WaitUntilBuilder<V> prepareWaitUntil(Function<WebDriver,V> conditionFunc, int totalTimeout){
            return new WaitUntilBuilder<>(this, conditionFunc, totalTimeout);
        }

        /**
         * A builder to build {@link WaitUntil} in a fluent way.
         * 
         * @param <V> the expected return type of condition function
         */
        public class WaitUntilBuilder<V> extends InnerBuilderBase{
            private final int totalTimeout;
            private final Function<WebDriver,V> conditionFunc;
            private int phaseTimeout = 10;
            private int pollInterval = 5;
            private List<Class<? extends Throwable>> ignoreExceptionList = Arrays.asList(NoSuchElementException.class, StaleElementReferenceException.class);
            private Consumer<ActionComposer> timeoutCallback;

            /**
             * Create a new {@link WaitUntilBuilder} with specified {@link ActionSequenceBuilder} as parent builder.
             * 
             * @param parentActionSequenceBuilder parent builder({@link ActionSequenceBuilder})
             * @param conditionFunc the condition function for evaluation by phases
             * @param totalTimeout the maximum amount of time to wait totally
             */
            public WaitUntilBuilder(ActionSequenceBuilder parentActionSequenceBuilder, Function<WebDriver,V> conditionFunc, int totalTimeout){
                super(parentActionSequenceBuilder);
                if (conditionFunc==null) throw new IllegalArgumentException("No evaluate function to build");
                if (totalTimeout<=0) throw new IllegalArgumentException("Illegal timeout to build");
                this.conditionFunc = conditionFunc;
                this.totalTimeout = totalTimeout;
                
            }

            /**
             * Set the maximum amount of time to wait for each execution phase.
             * 
             * @param phaseTimeout phase timeout
             * @return invoking {@link WaitUntilBuilder}
             */
            public WaitUntilBuilder<V> withPhaseTimeout(int phaseTimeout){
                if (phaseTimeout<=0) throw new IllegalArgumentException("Illegal phase timeout to build");
                this.phaseTimeout = phaseTimeout;
                return this;
            }

            /**
             * Set how often the condition function should be evaluated.
             * 
             * @param pollInterval evaluation interval
             * @return invoking {@link WaitUntilBuilder}
             */
            public WaitUntilBuilder<V> withPollInterval(int pollInterval){
                if (pollInterval<=0) throw new IllegalArgumentException("Illegal poll interval to build");
                this.pollInterval = pollInterval;
                return this;
            }

            /**
             * Set the types of exceptions to ignore when evaluating condition function.
             * 
             * @param ignoreExceptionList exception list
             * @return invoking {@link WaitUntilBuilder}
             */
            public WaitUntilBuilder<V> withIgnoredException(List<Class<? extends Throwable>> ignoreExceptionList){
                if (ignoreExceptionList==null || ignoreExceptionList.isEmpty()) throw new IllegalArgumentException("Illegal ignore exception list to build");
                this.ignoreExceptionList = ignoreExceptionList;
                return this;
            }

            /**
             * Set the callback function to be called when total timeout expires.
             * 
             * @param timeoutCallback timeout callback function
             * @return invoking {@link WaitUntilBuilder}
             */
            public WaitUntilBuilder<V> withTimeoutCallback(Consumer<ActionComposer> timeoutCallback){
                if (timeoutCallback==null) throw new IllegalArgumentException("Illegal timeout callback to build");
                this.timeoutCallback = timeoutCallback;
                return this;
            }

            /**
             * Finish building {@link WaitUntil} and add it to parent builder.
             * 
             * @return parent builder({@link ActionSequenceBuilder})
             */
            public ActionSequenceBuilder done(){
                MultiPhaseAction action = new WaitUntil<>(conditionFunc, totalTimeout, phaseTimeout, pollInterval, ignoreExceptionList, timeoutCallback);
                return parentActionSequenceBuilder.add(action);
            }
        }
        
        /**
         * Add a {@link JustWait} to the sequence of actions.
         * 
         * @param totalTimeout the maximum amount of time to wait totally
         * @return invoking {@link ActionSequenceBuilder}
         */
        public ActionSequenceBuilder justWait(int totalTimeout){
            return new JustWaitBuilder(this, totalTimeout).done();
        }

        /**
         * Start building a {@link JustWait}.
         * 
         * @param totalTimeout the maximum amount of time to wait totally
         * @return a new {@link JustWaitBuilder} with invoking {@link ActionSequenceBuilder} as parent builder
         */
        public JustWaitBuilder prepareJustWait(int totalTimeout){
            return new JustWaitBuilder(this, totalTimeout);
        }

        /**
         * A builder to build {@link JustWait} in a fluent way.
         */
        public class JustWaitBuilder extends InnerBuilderBase{
            private final int totalTimeout;
            private int phaseTimeout = 10;

            /**
             * Create a new {@link JustWaitBuilder} with specified {@link ActionSequenceBuilder} as parent builder.
             * 
             * @param parentActionSequenceBuilder parent builder({@link ActionSequenceBuilder})
             * @param totalTimeout the maximum amount of time to wait totally
             */
            public JustWaitBuilder(ActionSequenceBuilder parentActionSequenceBuilder, int totalTimeout){
                super(parentActionSequenceBuilder);
                if (totalTimeout<=0) throw new IllegalArgumentException("Illegal timeout to build");
                this.totalTimeout = totalTimeout;
            }

            /**
             * Set the maximum amount of time to wait for each execution phase.
             * 
             * @param phaseTimeout phase timeout
             * @return invoking {@link JustWaitBuilder}
             */
            public JustWaitBuilder withPhaseTimeout(int phaseTimeout){
                if (phaseTimeout<=0) throw new IllegalArgumentException("Illegal phase timeout to build");
                this.phaseTimeout = phaseTimeout;
                return this;
            }

            /**
             * Finish building {@link JustWait} and add it to parent builder.
             * 
             * @return parent builder({@link ActionSequenceBuilder})
             */
            public ActionSequenceBuilder done(){
                MultiPhaseAction action = new JustWait(totalTimeout, phaseTimeout);
                return parentActionSequenceBuilder.add(action);
            }
        }
        
        /**
         * Add a {@link PostForm} to the sequence of actions.
         * 
         * @param url
         * @param simpleFormData
         * @return invoking {@link ActionSequenceBuilder}
         */
        public ActionSequenceBuilder postForm(String url, List<SimpleImmutableEntry<String,String>> simpleFormData){
            return new PostFormBuilder(this, url).withSimpleFormData(simpleFormData).done();
        }

        /**
         * Start building a {@link PostForm}.
         * 
         * @param url
         * @return a new {@link PostFormBuilder} with invoking {@link ActionSequenceBuilder} as parent builder
         */
        public PostFormBuilder preparePostForm(String url){
            return new PostFormBuilder(this, url);
        }

        /**
         * A builder to build {@link PostForm} in a fluent way.
         */
        public class PostFormBuilder extends InnerBuilderBase{
            private final String url;
            private List<SimpleImmutableEntry<String,String>> simpleFormData;
            private String acceptCharset;

            /**
             * Create a new {@link PostFormBuilder} with specified {@link ActionSequenceBuilder} as parent builder.
             * 
             * @param parentActionSequenceBuilder parent builder({@link ActionSequenceBuilder})
             * @param url
             */
            public PostFormBuilder(ActionSequenceBuilder parentActionSequenceBuilder, String url){
                super(parentActionSequenceBuilder);
                if (url==null || url.isEmpty()) throw new IllegalArgumentException("No url specified to build");
                this.url = url;
            }

            /**
             *
             * @param simpleFormData
             * @return invoking {@link PostFormBuilder}
             */
            public PostFormBuilder withSimpleFormData(List<SimpleImmutableEntry<String,String>> simpleFormData){
                if (simpleFormData==null) throw new IllegalArgumentException("Illegal form data to build");
                this.simpleFormData = simpleFormData;
                return this;
            }

            /**
             *
             * @param acceptCharset
             * @return invoking {@link PostFormBuilder}
             */
            public PostFormBuilder withAcceptCharset(String acceptCharset){
                if (acceptCharset==null || acceptCharset.isEmpty()) throw new IllegalArgumentException("Illegal accept charset to build");
                this.acceptCharset = acceptCharset;
                return this;
            }

            /**
             * Finish building {@link PostForm} and add it to parent builder.
             * 
             * @return parent builder({@link ActionSequenceBuilder})
             */
            public ActionSequenceBuilder done(){
                MultiPhaseAction action = new PostForm(url, simpleFormData, acceptCharset);
                return parentActionSequenceBuilder.add(action);
            }
        }
        
        /**
         * Add a {@link GetUrl} to the sequence of actions.
         * 
         * @param url
         * @return invoking {@link ActionSequenceBuilder}
         */
        public ActionSequenceBuilder getUrl(String url){
            return new GetUrlBuilder(this, url).done();
        }

        /**
         * Start building a {@link GetUrl}.
         * 
         * @param url
         * @return a new {@link GetUrlBuilder} with invoking {@link ActionSequenceBuilder} as parent builder
         */
        public GetUrlBuilder prepareGetUrl(String url){
            return new GetUrlBuilder(this, url);
        }

        /**
         * A builder to build {@link GetUrl} in a fluent way.
         */
        public class GetUrlBuilder extends InnerBuilderBase{
            private final String url;

            /**
             * Create a new {@link GetUrlBuilder} with specified {@link ActionSequenceBuilder} as parent builder.
             * 
             * @param parentActionSequenceBuilder parent builder({@link ActionSequenceBuilder})
             * @param url
             */
            public GetUrlBuilder(ActionSequenceBuilder parentActionSequenceBuilder, String url){
                super(parentActionSequenceBuilder);
                if (url==null || url.isEmpty()) throw new IllegalArgumentException("No url specified to build");
                this.url = url;
            }

            /**
             * Finish building {@link GetUrl} and add it to parent builder.
             * 
             * @return parent builder({@link ActionSequenceBuilder})
             */
            public ActionSequenceBuilder done(){
                MultiPhaseAction action = new GetUrl(url);
                return parentActionSequenceBuilder.add(action);
            }
        }
        
        /**
         * Add a {@link Select} to the sequence of actions, which select by index.
         * 
         * @param by the element locating mechanism
         * @param options
         * @return invoking {@link ActionSequenceBuilder}
         */
        public ActionSequenceBuilder selectByIndex(By by, Integer... options){
            return new SelectBuilder(this, by).selectByIndex(options).done();
        }

        /**
         * Add a {@link Select} to the sequence of actions, which select by text.
         * 
         * @param by the element locating mechanism
         * @param options
         * @return invoking {@link ActionSequenceBuilder}
         */
        public ActionSequenceBuilder selectByText(By by, String... options){
            return new SelectBuilder(this, by).selectByText(options).done();
        }

        /**
         * Add a {@link Select} to the sequence of actions, which select by value.
         * 
         * @param by the element locating mechanism
         * @param options
         * @return invoking {@link ActionSequenceBuilder}
         */
        public ActionSequenceBuilder selectByValue(By by, String... options){
            return new SelectBuilder(this, by).selectByValue(options).done();
        }

        /**
         * Start building a {@link Select}.
         * 
         * @param by the element locating mechanism
         * @return a new {@link SelectBuilder} with invoking {@link ActionSequenceBuilder} as parent builder
         */
        public SelectBuilder prepareSelect(By by){
            return new SelectBuilder(this, by);
        }

        /**
         * A builder to build {@link Select} in a fluent way.
         */
        public class SelectBuilder extends InnerBuilderBase{
            private final By by;
            private By frameBy;
            private SelectBy selectBy;
            private Object[] options;

            /**
             * Create a new {@link SelectBuilder} with specified {@link ActionSequenceBuilder} as parent builder.
             * 
             * @param parentActionSequenceBuilder parent builder({@link ActionSequenceBuilder})
             * @param by the element locating mechanism
             */
            public SelectBuilder(ActionSequenceBuilder parentActionSequenceBuilder, By by){
                super(parentActionSequenceBuilder);
                if (by==null) throw new IllegalArgumentException("No locator specified to build");
                this.by = by;
            }

            /**
             * Set the frame locating mechanism for the element resides in a frame.
             * 
             * @param frameBy the locating mechanism for the frame
             * @return invoking {@link SelectBuilder}
             */
            public SelectBuilder withInFrame(By frameBy){
                if (frameBy==null)  throw new IllegalArgumentException("Illegal frame locator to build");
                this.frameBy = frameBy;
                return this;
            }
            private SelectBuilder selectBy(SelectBy selectBy, Object... options){
                if (options==null || options.length==0) throw new IllegalArgumentException("No options to build");
                this.selectBy = selectBy;
                this.options = options;
                return this;
            }

            /**
             *
             * @param options
             * @return invoking {@link SelectBuilder}
             */
            public SelectBuilder selectByIndex(Integer... options){
                return selectBy(SelectBy.Index, (Object[]) options);
            }

            /**
             *
             * @param options
             * @return invoking {@link SelectBuilder}
             */
            public SelectBuilder selectByText(String... options){
                return selectBy(SelectBy.Text, (Object[]) options);
            }

            /**
             *
             * @param options
             * @return invoking {@link SelectBuilder}
             */
            public SelectBuilder selectByValue(String... options){
                return selectBy(SelectBy.Value, (Object[]) options);
            }

            /**
             * Finish building {@link Select} and add it to parent builder.
             * 
             * @return parent builder({@link ActionSequenceBuilder})
             */
            public ActionSequenceBuilder done(){
                MultiPhaseAction action = new Select(by, frameBy, selectBy, options);
                return parentActionSequenceBuilder.add(action);
            }
        }
        
        /**
         * Add a {@link SendKey} to the sequence of actions.
         * 
         * @param by the element locating mechanism
         * @param keysToSend
         * @return invoking {@link ActionSequenceBuilder}
         */
        public ActionSequenceBuilder sendKey(By by, CharSequence... keysToSend){
            return new SendKeyBuilder(this, by, keysToSend).done();
        }

        /**
         * Start building a {@link SendKey}.
         * 
         * @param by the element locating mechanism
         * @param keysToSend
         * @return a new {@link SendKeyBuilder} with invoking {@link ActionSequenceBuilder} as parent builder
         */
        public SendKeyBuilder prepareSendKey(By by, CharSequence... keysToSend){
            return new SendKeyBuilder(this, by, keysToSend);
        }

        /**
         * A builder to build {@link SendKey} in a fluent way.
         */
        public class SendKeyBuilder extends InnerBuilderBase{
            private final By by;
            private final CharSequence[] keysToSend;
            private volatile By frameBy;
            private volatile boolean clearBeforeSend=false;

            /**
             * Create a new {@link SendKeyBuilder} with specified {@link ActionSequenceBuilder} as parent builder.
             * 
             * @param parentActionSequenceBuilder parent builder({@link ActionSequenceBuilder})
             * @param by the element locating mechanism
             * @param keysToSend
             */
            public SendKeyBuilder(ActionSequenceBuilder parentActionSequenceBuilder, By by, CharSequence... keysToSend){
                super(parentActionSequenceBuilder);
                if (by==null) throw new IllegalArgumentException("No locator specified to build");
                if (keysToSend==null) throw new IllegalArgumentException("No keys specified to build");
                this.by = by;
                this.keysToSend = keysToSend;
            }

            /**
             * Set the frame locating mechanism for the element resides in a frame.
             * 
             * @param frameBy the locating mechanism for the frame
             * @return invoking {@link SendKeyBuilder}
             */
            public SendKeyBuilder withInFrame(By frameBy){
                if (frameBy==null)  throw new IllegalArgumentException("Illegal frame locator to build");
                this.frameBy = frameBy;
                return this;
            }

            /**
             *
             * @return invoking {@link SendKeyBuilder}
             */
            public SendKeyBuilder withClearBeforeSend(){
                this.clearBeforeSend = true;
                return this;
            }

            /**
             * Finish building {@link SendKey} and add it to parent builder.
             * 
             * @return parent builder({@link ActionSequenceBuilder})
             */
            public ActionSequenceBuilder done(){
                MultiPhaseAction action = new SendKey(by, frameBy, clearBeforeSend, keysToSend);
                return parentActionSequenceBuilder.add(action);
            }
        }
        
        /**
         * Add a {@link Click} to the sequence of actions.
         * 
         * @param by the element locating mechanism
         * @return invoking {@link ActionSequenceBuilder}
         */
        public ActionSequenceBuilder click(By by){
            return new ClickBuilder(this, by).done();
        }

        /**
         * Start building a {@link Click}.
         * 
         * @param by the element locating mechanism
         * @return a new {@link ClickBuilder} with invoking {@link ActionSequenceBuilder} as parent builder
         */
        public ClickBuilder prepareClick(By by){
            return new ClickBuilder(this, by);
        }

        /**
         * A builder to build {@link Click} in a fluent way.
         */
        public class ClickBuilder extends InnerBuilderBase{
            private final By by;
            private volatile By frameBy;

            /**
             * Create a new {@link ClickBuilder} with specified {@link ActionSequenceBuilder} as parent builder.
             * 
             * @param parentActionSequenceBuilder parent builder({@link ActionSequenceBuilder})
             * @param by the element locating mechanism
             */
            public ClickBuilder(ActionSequenceBuilder parentActionSequenceBuilder, By by){
                super(parentActionSequenceBuilder);
                if (by==null) throw new IllegalArgumentException("No locator specified to build");
                this.by = by;
            }

            /**
             * Set the frame locating mechanism for the element resides in a frame.
             * 
             * @param frameBy the locating mechanism for the frame
             * @return invoking {@link ClickBuilder}
             */
            public ClickBuilder withInFrame(By frameBy){
                if (frameBy==null)  throw new IllegalArgumentException("Illegal frame locator to build");
                this.frameBy = frameBy;
                return this;
            }

            /**
             * Finish building {@link Click} and add it to parent builder.
             * 
             * @return parent builder({@link ActionSequenceBuilder})
             */
            public ActionSequenceBuilder done(){
                MultiPhaseAction action = new Click(by, frameBy);
                return parentActionSequenceBuilder.add(action);
            }
        }
        
        /**
         * Add a {@link Custom} to the sequence of actions.
         * 
         * @param composerConsumer
         * @return invoking {@link ActionSequenceBuilder}
         */
        public ActionSequenceBuilder custom(Consumer<ActionComposer> composerConsumer){
            return new CustomBuilder(this, composerConsumer).done();
        }

        /**
         * Start building a {@link Custom}.
         * 
         * @param composerConsumer
         * @return a new {@link CustomBuilder} with invoking {@link ActionSequenceBuilder} as parent builder
         */
        public CustomBuilder prepareCustom(Consumer<ActionComposer> composerConsumer){
            return new CustomBuilder(this, composerConsumer);
        }

        /**
         * A builder to build {@link Custom} in a fluent way.
         */
        public class CustomBuilder extends InnerBuilderBase{
            private final Consumer<ActionComposer> composerConsumer;

            /**
             * Create a new {@link CustomBuilder} with specified {@link ActionSequenceBuilder} as parent builder.
             * 
             * @param parentActionSequenceBuilder parent builder({@link ActionSequenceBuilder})
             * @param composerConsumer
             */
            public CustomBuilder(ActionSequenceBuilder parentActionSequenceBuilder, Consumer<ActionComposer> composerConsumer){
                super(parentActionSequenceBuilder);
                if (composerConsumer==null) throw new IllegalArgumentException("No composer consumer specified to build");
                this.composerConsumer = composerConsumer;
            }

            /**
             * Finish building {@link Custom} and add it to parent builder.
             * 
             * @return parent builder({@link ActionSequenceBuilder})
             */
            public ActionSequenceBuilder done(){
                MultiPhaseAction action = new Custom(composerConsumer);
                return parentActionSequenceBuilder.add(action);
            }
        }
        
        /**
         * Add a {@link ScrollToView} to the sequence of actions.
         * 
         * @param by the element locating mechanism
         * @param toTop {@code true}: scroll to top;{@code false}: scroll to bottom
         * @return invoking {@link ActionSequenceBuilder}
         */
        public ActionSequenceBuilder scrollToView(By by, boolean toTop){
            return new ScrollToViewBuilder(this, by, toTop).done();
        }

        /**
         * Start building a {@link ScrollToView}.
         * 
         * @param by the element locating mechanism
         * @param toTop {@code true}: scroll to top;{@code false}: scroll to bottom
         * @return a new {@link ScrollToViewBuilder} with invoking {@link ActionSequenceBuilder} as parent builder
         */
        public ScrollToViewBuilder prepareScrollToView(By by, boolean toTop){
            return new ScrollToViewBuilder(this, by, toTop);
        }

        /**
         *
         */
        public class ScrollToViewBuilder extends InnerBuilderBase{
            private final By by;
            private final boolean toTop;
            private volatile By frameBy;
            

            /**
             * Create a new {@link ScrollToViewBuilder} with specified {@link ActionSequenceBuilder} as parent builder.
             * 
             * @param parentActionSequenceBuilder parent builder({@link ActionSequenceBuilder})
             * @param by the element locating mechanism
             * @param toTop {@code true}: scroll to top;{@code false}: scroll to bottom
             */
            public ScrollToViewBuilder(ActionSequenceBuilder parentActionSequenceBuilder, By by, boolean toTop){
                super(parentActionSequenceBuilder);
                if (by==null) throw new IllegalArgumentException("No locator specified to build");
                this.by = by;
                this.toTop = toTop;
            }

            /**
             * Set the frame locating mechanism for the element resides in a frame.
             * 
             * @param frameBy the locating mechanism for the frame
             * @return invoking {@link ScrollToViewBuilder}
             */
            public ScrollToViewBuilder withInFrame(By frameBy){
                if (frameBy==null)  throw new IllegalArgumentException("Illegal frame locator to build");
                this.frameBy = frameBy;
                return this;
            }

            /**
             *
             * @return
             */
            public ActionSequenceBuilder done(){
                MultiPhaseAction action = new ScrollToView(by, frameBy, toTop);
                return parentActionSequenceBuilder.add(action);
            }
        }
        
        /**
         * Add a {@link Upload} to the sequence of actions.
         * 
         * @param by the element locating mechanism
         * @param pathOfFile the path of file to upload
         * @return invoking {@link ActionSequenceBuilder}
         */
        public ActionSequenceBuilder upload(By by, String pathOfFile){
            return new UploadBuilder(this, by, pathOfFile).done();
        }

        /**
         * Start building a {@link Upload}.
         * 
         * @param by the element locating mechanism
         * @param pathOfFile the path of file to upload
         * @return a new {@link UploadBuilder} with invoking {@link ActionSequenceBuilder} as parent builder
         */
        public UploadBuilder prepareScrollToView(By by, String pathOfFile){
            return new UploadBuilder(this, by, pathOfFile);
        }

        /**
         *
         */
        public class UploadBuilder extends InnerBuilderBase{
            private final By by;
            private final String pathOfFile;
            private volatile By frameBy;
            

            /**
             * Create a new {@link UploadBuilder} with specified {@link ActionSequenceBuilder} as parent builder.
             * 
             * @param parentActionSequenceBuilder parent builder({@link ActionSequenceBuilder})
             * @param by the element locating mechanism
             * @param pathOfFile the path of file to upload
             */
            public UploadBuilder(ActionSequenceBuilder parentActionSequenceBuilder, By by, String pathOfFile){
                super(parentActionSequenceBuilder);
                if (by==null) throw new IllegalArgumentException("No locator specified to build");
                this.by = by;
                this.pathOfFile = pathOfFile;
            }

            /**
             * Set the frame locating mechanism for the element resides in a frame.
             * 
             * @param frameBy the locating mechanism for the frame
             * @return invoking {@link UploadBuilder}
             */
            public UploadBuilder withInFrame(By frameBy){
                if (frameBy==null)  throw new IllegalArgumentException("Illegal frame locator to build");
                this.frameBy = frameBy;
                return this;
            }

            /**
             *
             * @return
             */
            public ActionSequenceBuilder done(){
                MultiPhaseAction action = new Upload(by, frameBy, pathOfFile);
                return parentActionSequenceBuilder.add(action);
            }
        }        
        
        /**
         *
         * Start building a {@link IfThenElse}.
         * 
         * @param predicate
         * @return a new {@link IfThenElseBuilder} with invoking {@link ActionSequenceBuilder} as parent builder
         */
        public IfThenElseBuilder prepareIfThenElse(Predicate<ActionComposer> predicate){
            return new IfThenElseBuilder(this, predicate);
        }

        /**
         * A builder to build {@link IfThenElse} in a fluent way.
         */
        public class IfThenElseBuilder extends InnerBuilderBase{
            private final Predicate<ActionComposer> predicate;
            private volatile boolean isPrepareThenAction = true;
            private final List<MultiPhaseAction> thenActionList = new ArrayList<>();
            private final List<MultiPhaseAction> elseActionList = new ArrayList<>();

            /**
             * Create a new {@link IfThenElseBuilder} with specified {@link ActionSequenceBuilder} as parent builder.
             * 
             * @param parentActionSequenceBuilder parent builder({@link ActionSequenceBuilder})
             * @param predicate
             */
            public IfThenElseBuilder(ActionSequenceBuilder parentActionSequenceBuilder, Predicate<ActionComposer> predicate){
                super(parentActionSequenceBuilder);
                if (predicate==null) throw new IllegalArgumentException("No predicate specified to build");
                this.predicate = predicate;
            }
            
            /**
             *
             * @param action
             * @return invoking {@link IfThenElseBuilder}
             */
            public IfThenElseBuilder add(MultiPhaseAction action){
                if (action==null) throw new IllegalArgumentException("No action to accept");
                if (isPrepareThenAction) thenActionList.add(action);
                else elseActionList.add(action);
                return this;
            }
            
            /**
             *
             * @return
             */
            public ActionComposerBuilder returnToComposerBuilder(){
                parentActionSequenceBuilder.add(new IfThenElse(predicate, thenActionList, elseActionList));
                return parentActionSequenceBuilder.returnToComposerBuilder();
            }
            
            /**
             *
             * @return
             */
            public ActionSequenceBuilder then(){
                isPrepareThenAction = true;
                return new ActionSequenceBuilder(this);
            }
            
            /**
             *
             * @return
             */
            public ActionSequenceBuilder otherwise(){
                isPrepareThenAction = false;
                return new ActionSequenceBuilder(this);
            }
            
            /**
             * Finish building {@link IfThenElse} and add it to parent builder.
             * 
             * @return parent builder({@link ActionSequenceBuilder})
             */
            public ActionSequenceBuilder endIf(){
                parentActionSequenceBuilder.add(new IfThenElse(predicate, thenActionList, elseActionList));
                return parentActionSequenceBuilder;
            }
        }
        
        private class InnerBuilderBase{
            final ActionSequenceBuilder parentActionSequenceBuilder;
            private InnerBuilderBase(ActionSequenceBuilder parentActionSequenceBuilder){
                if (parentActionSequenceBuilder==null) throw new IllegalArgumentException("No parent builder");
                this.parentActionSequenceBuilder = parentActionSequenceBuilder;
            }
        }
    }
}
