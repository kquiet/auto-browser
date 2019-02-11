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
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.By;

import org.kquiet.browser.action.Click;
import org.kquiet.browser.action.CloseWindow;
import org.kquiet.browser.action.Custom;
import org.kquiet.browser.action.Extract;
import org.kquiet.browser.action.GetUrl;
import org.kquiet.browser.action.OpenWindow;
import org.kquiet.browser.action.PostForm;
import org.kquiet.browser.action.JustWait;
import org.kquiet.browser.action.Select;
import org.kquiet.browser.action.SendKey;
import org.kquiet.browser.action.WaitUntil;
import org.kquiet.browser.action.Select.SelectBy;
import org.kquiet.browser.action.IfThenElse;
import org.kquiet.browser.action.MouseOver;
import org.kquiet.browser.action.ReplyAlert;
import org.kquiet.browser.action.ReplyAlert.Decision;
import org.kquiet.browser.action.ScrollToView;
import org.kquiet.browser.action.Upload;
import org.kquiet.browser.action.Composable;
import org.kquiet.browser.action.MultiPhased;

/**
 * {@link ActionComposerBuilder} is resposible to build a {@link ActionComposer} in a fluent way.
 * New {@link ActionComposerBuilder} instance is required before building a {@link ActionComposer}.
 * 
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
 *      .buildBasic();
 * </pre>
 * 
 * More complicated {@link ActionComposer} can be built through built-in {@link org.kquiet.browser.action actions}.
 * {@link ActionComposerBuilder} includes {@link ActionComposerBuilder.ActionSequenceBuilder inner builders} for these actions as well.
 * 
 * @author Kimberly
 */
public class ActionComposerBuilder{
    private final Deque<Composable> actionList = new LinkedList<>();
    private Consumer<ActionComposer> failFunc, successFunc, doneFunc;
    
    private volatile ActionComposer actionComposer;
    
    /**
     * Create a new {@link ActionComposerBuilder}
     */
    public ActionComposerBuilder(){
    }

    /**
     * Add action to the tail of building {@link ActionComposer}.
     * 
     * @param action action to add
     * @return self reference
     */    
    private ActionComposerBuilder add(Composable action){
        actionList.addLast(action);
        return this;
    }
    
    /**
     * Set the fail callback function of {@link ActionComposer}.
     * 
     * @param func callback function
     * @return this {@link ActionComposerBuilder}
     * @see ActionComposer#onFail(java.util.function.Consumer)
     */
    public ActionComposerBuilder onFail(Consumer<ActionComposer> func){
        failFunc = func;
        return this;
    }
    
    /**
     * Set the success callback function of {@link ActionComposer}.
     * 
     * @param func callback function
     * @return this {@link ActionComposerBuilder}
     * @see ActionComposer#onSuccess(java.util.function.Consumer)
     */
    public ActionComposerBuilder onSuccess(Consumer<ActionComposer> func){
        successFunc = func;
        return this;
    }
    
    /**
     * Set the done callback function of {@link ActionComposer}.
     * 
     * @param func callback function
     * @return this {@link ActionComposerBuilder}
     * @see ActionComposer#onDone(java.util.function.Consumer)
     */
    public ActionComposerBuilder onDone(Consumer<ActionComposer> func){
        doneFunc = func;
        return this;
    }
    
    private void commonBuild(ActionComposer actionComposer, String name){
        actionComposer.setName(name).onFail(failFunc).onSuccess(successFunc).onDone(doneFunc);
        actionList.forEach(s->actionComposer.addToTail(s));        
    }
    
    /**
     * Finish building the {@link BasicActionComposer}.
     * 
     * @return the built {@link BasicActionComposer}
     */
    public BasicActionComposer buildBasic(){
        return buildBasic(UUID.randomUUID().toString());
    }
    
    /**
     * Finish building the {@link BasicActionComposer} with its name.
     * 
     * @param name name of {@link BasicActionComposer}
     * @return the built {@link BasicActionComposer}
     */
    public BasicActionComposer buildBasic(String name){
        if (this.actionComposer==null){
            BasicActionComposer actionComposer = new BasicActionComposer();
            commonBuild(actionComposer, name);
            this.actionComposer = actionComposer;
            return actionComposer;
        }
        else return (BasicActionComposer) this.actionComposer;
    }
    
    /**
     * Finish building the {@link ActionComposer}.
     * 
     * @param <T> the type of {@link ActionComposer} to build
     * @param composerType the type of {@link ActionComposer} to build
     * @return the built {@link ActionComposer}
     * @throws java.lang.ClassNotFoundException if the class of composer type cannot be located
     * @throws java.lang.InstantiationException if the class of composer type represents an abstract class, an interface, an array class, a primitive type, or void; or if the class has no nullary constructor; or if the instantiation fails for some other reason
     * @throws java.lang.IllegalAccessException if the class of composer type or its nullary constructor is not accessible
     */
    public <T extends ActionComposer> T build(Class<T> composerType) throws ClassNotFoundException, InstantiationException, IllegalAccessException{
        return build(composerType, UUID.randomUUID().toString());
    }
    
    /**
     * Finish building the {@link ActionComposer} with its name.
     * 
     * @param <T> the type of {@link ActionComposer} to build
     * @param composerType the type of {@link ActionComposer} to build
     * @param name name of {@link ActionComposer}
     * @return the built {@link ActionComposer}
     * @throws java.lang.ClassNotFoundException if the class of composer type cannot be located
     * @throws java.lang.InstantiationException if the class of composer type represents an abstract class, an interface, an array class, a primitive type, or void; or if the class has no nullary constructor; or if the instantiation fails for some other reason
     * @throws java.lang.IllegalAccessException if the class of composer type or its nullary constructor is not accessible
     */
    public <T extends ActionComposer> T build(Class<T> composerType, String name) throws ClassNotFoundException, InstantiationException, IllegalAccessException{
        if (this.actionComposer==null){
            @SuppressWarnings("unchecked") final T actionComposer = (T)Class.forName(composerType.getName()).newInstance();
            commonBuild(actionComposer, name);
            this.actionComposer = actionComposer;
            return actionComposer;
        }
        else {
            @SuppressWarnings("unchecked") T obj = (T) this.actionComposer;
            return obj;
        }
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
        private final List<Composable> actionList = new ArrayList<>();
        
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
         * Add an action to the tail of building action sequence.
         * 
         * @param action action to add
         * @return self reference
         */
        private ActionSequenceBuilder add(Composable action){
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
                    parentComposerBuilder.add(action);
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
         * @return self reference
         */
        public ActionSequenceBuilder closeWindow(boolean closeAllRegistered){
            return new CloseWindowBuilder(this, closeAllRegistered).done();
        }

        /**
         * Start building a {@link CloseWindow}.
         * 
         * @param closeAllRegistered {@code true}: close all regisetered windows; {@code false}: close only the focus window
         * @return a new {@link CloseWindowBuilder} with this {@link ActionSequenceBuilder} as parent builder
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
                Composable action = new CloseWindow(closeAllRegistered);
                return parentActionSequenceBuilder.add(action);
            }
        }
        
        /**
         * Add a {@link OpenWindow} to the sequence of actions.
         * 
         * @return self reference
         */
        public ActionSequenceBuilder openWindow(){
            return new OpenWindowBuilder(this).done();
        }

        /**
         * Start building a {@link OpenWindow}.
         * 
         * @return a new {@link OpenWindowBuilder} with this {@link ActionSequenceBuilder} as parent builder
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
             * @return self reference
             */
            public OpenWindowBuilder withComposerFocus(){
                this.asComposerFocusWindow = true;
                return this;
            }

            /**
             * Register opened window with name.
             * 
             * @param registerName name to register
             * @return self reference
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
                Composable action = new OpenWindow(asComposerFocusWindow, registerName);
                return parentActionSequenceBuilder.add(action);
            }
        }
        
        /**
         * Add a {@link WaitUntil} to the sequence of actions.
         * 
         * @param <V> the expected return type of condition function
         * @param conditionFunc the condition function for evaluation by phases
         * @param totalTimeout the maximum amount of time to wait totally
         * @return self reference
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
         * @return a new {@link WaitUntilBuilder} with this {@link ActionSequenceBuilder} as parent builder
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
            private Set<Class<? extends Throwable>> ignoreExceptions;
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
             * @return self reference
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
             * @return self reference
             */
            public WaitUntilBuilder<V> withPollInterval(int pollInterval){
                if (pollInterval<=0) throw new IllegalArgumentException("Illegal poll interval to build");
                this.pollInterval = pollInterval;
                return this;
            }

            /**
             * Set the types of exceptions to ignore when evaluating condition function.
             * 
             * @param ignoreExceptions exception list
             * @return self reference
             */
            public WaitUntilBuilder<V> withIgnoredException(Set<Class<? extends Throwable>> ignoreExceptions){
                if (ignoreExceptions==null) throw new IllegalArgumentException("Illegal ignore exception list to build");
                this.ignoreExceptions = ignoreExceptions;
                return this;
            }

            /**
             * Set the callback function to be called when total timeout expires.
             * 
             * @param timeoutCallback timeout callback function
             * @return self reference
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
                Composable action = new WaitUntil<>(conditionFunc, totalTimeout, phaseTimeout, pollInterval, ignoreExceptions, timeoutCallback);
                return parentActionSequenceBuilder.add(action);
            }
        }
        
        /**
         * Add a {@link JustWait} to the sequence of actions.
         * 
         * @param totalTimeout the maximum amount of time to wait totally
         * @return self reference
         */
        public ActionSequenceBuilder justWait(int totalTimeout){
            return new JustWaitBuilder(this, totalTimeout).done();
        }

        /**
         * Start building a {@link JustWait}.
         * 
         * @param totalTimeout the maximum amount of time to wait totally
         * @return a new {@link JustWaitBuilder} with this {@link ActionSequenceBuilder} as parent builder
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
             * @return self reference
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
                Composable action = new JustWait(totalTimeout, phaseTimeout);
                return parentActionSequenceBuilder.add(action);
            }
        }
        
        /**
         * Add a {@link PostForm} to the sequence of actions.
         * 
         * @param url the address where to submit the form
         * @param formData the form data to submit
         * @return self reference
         */
        public ActionSequenceBuilder postForm(String url, List<SimpleImmutableEntry<String,String>> formData){
            return new PostFormBuilder(this, url, formData).done();
        }

        /**
         * Start building a {@link PostForm}.
         * 
         * @param url the address where to submit the form
         * @param formData the form data to submit
         * @return a new {@link PostFormBuilder} with this {@link ActionSequenceBuilder} as parent builder
         */
        public PostFormBuilder preparePostForm(String url, List<SimpleImmutableEntry<String,String>> formData){
            return new PostFormBuilder(this, url, formData);
        }

        /**
         * A builder to build {@link PostForm} in a fluent way.
         */
        public class PostFormBuilder extends InnerBuilderBase{
            private final String url;
            private final List<SimpleImmutableEntry<String,String>> formData;
            private String acceptCharset;

            /**
             * Create a new {@link PostFormBuilder} with specified {@link ActionSequenceBuilder} as parent builder.
             * 
             * @param parentActionSequenceBuilder parent builder({@link ActionSequenceBuilder})
             * @param url the address where to submit the form
             * @param formData the form data to submit
             */
            public PostFormBuilder(ActionSequenceBuilder parentActionSequenceBuilder, String url, List<SimpleImmutableEntry<String,String>> formData){
                super(parentActionSequenceBuilder);
                if (url==null || url.isEmpty()) throw new IllegalArgumentException("No url specified to build");
                this.url = url;
                this.formData = formData;
            }

            /**
             *
             * @param acceptCharset the charset used in the submitted form
             * @return self reference
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
                Composable action = new PostForm(url, formData, acceptCharset);
                return parentActionSequenceBuilder.add(action);
            }
        }
        
        /**
         * Add a {@link GetUrl} to the sequence of actions.
         * 
         * @param url the url of web page
         * @return self reference
         */
        public ActionSequenceBuilder getUrl(String url){
            return new GetUrlBuilder(this, url).done();
        }

        /**
         * Start building a {@link GetUrl}.
         * 
         * @param url the url of web page
         * @return a new {@link GetUrlBuilder} with this {@link ActionSequenceBuilder} as parent builder
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
             * @param url the url of web page
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
                Composable action = new GetUrl(url);
                return parentActionSequenceBuilder.add(action);
            }
        }
        
        /**
         * Add a {@link Select} to the sequence of actions, which select by index.
         * 
         * @param by the element locating mechanism
         * @param options the option to select; all options are deselected when no option is supplied and the SELECT element supports selecting multiple options
         * @return self reference
         */
        public ActionSequenceBuilder selectByIndex(By by, Integer... options){
            return new SelectBuilder(this, by).selectByIndex(options).done();
        }

        /**
         * Add a {@link Select} to the sequence of actions, which select by text.
         * 
         * @param by the element locating mechanism
         * @param options the option to select; all options are deselected when no option is supplied and the SELECT element supports selecting multiple options
         * @return self reference
         */
        public ActionSequenceBuilder selectByText(By by, String... options){
            return new SelectBuilder(this, by).selectByText(options).done();
        }

        /**
         * Add a {@link Select} to the sequence of actions, which select by value.
         * 
         * @param by the element locating mechanism
         * @param options the option to select; all options are deselected when no option is supplied and the SELECT element supports selecting multiple options
         * @return self reference
         */
        public ActionSequenceBuilder selectByValue(By by, String... options){
            return new SelectBuilder(this, by).selectByValue(options).done();
        }

        /**
         * Start building a {@link Select}.
         * 
         * @param by the element locating mechanism
         * @return a new {@link SelectBuilder} with this {@link ActionSequenceBuilder} as parent builder
         */
        public SelectBuilder prepareSelect(By by){
            return new SelectBuilder(this, by);
        }

        /**
         * A builder to build {@link Select} in a fluent way.
         */
        public class SelectBuilder extends InnerBuilderBase{
            private final By by;
            private List<By> frameBySequence;
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
             * @param frameBySequence the sequence of the frame locating mechanism
             * @return self reference
             */
            public SelectBuilder withInFrame(List<By> frameBySequence){
                if (frameBySequence==null)  throw new IllegalArgumentException("Illegal frame locator to build");
                this.frameBySequence = frameBySequence;
                return this;
            }
            private SelectBuilder selectBy(SelectBy selectBy, Object... options){
                if (options==null || options.length==0) throw new IllegalArgumentException("No options to build");
                this.selectBy = selectBy;
                this.options = options;
                return this;
            }

            /**
             * Select by index.
             * 
             * @param options the option to select; all options are deselected when no option is supplied and the SELECT element supports selecting multiple options
             * @return self reference
             */
            public SelectBuilder selectByIndex(Integer... options){
                return selectBy(SelectBy.Index, (Object[]) options);
            }

            /**
             * Select By text.
             * 
             * @param options the option to select; all options are deselected when no option is supplied and the SELECT element supports selecting multiple options
             * @return self reference
             */
            public SelectBuilder selectByText(String... options){
                return selectBy(SelectBy.Text, (Object[]) options);
            }

            /**
             * Select by value.
             * 
             * @param options the option to select; all options are deselected when no option is supplied and the SELECT element supports selecting multiple options
             * @return self reference
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
                Composable action = new Select(by, frameBySequence, selectBy, options);
                return parentActionSequenceBuilder.add(action);
            }
        }
        
        /**
         * Add a {@link SendKey} to the sequence of actions.
         * 
         * @param by the element locating mechanism
         * @param keysToSend character sequence to send to the element
         * @return self reference
         */
        public ActionSequenceBuilder sendKey(By by, CharSequence... keysToSend){
            return new SendKeyBuilder(this, by, keysToSend).done();
        }

        /**
         * Start building a {@link SendKey}.
         * 
         * @param by the element locating mechanism
         * @param keysToSend character sequence to send to the element
         * @return a new {@link SendKeyBuilder} with this {@link ActionSequenceBuilder} as parent builder
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
            private List<By> frameBySequence;
            private boolean clearBeforeSend=false;

            /**
             * Create a new {@link SendKeyBuilder} with specified {@link ActionSequenceBuilder} as parent builder.
             * 
             * @param parentActionSequenceBuilder parent builder({@link ActionSequenceBuilder})
             * @param by the element locating mechanism
             * @param keysToSend character sequence to send to the element
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
             * @param frameBySequence the sequence of the frame locating mechanism
             * @return self reference
             */
            public SendKeyBuilder withInFrame(List<By> frameBySequence){
                if (frameBySequence==null)  throw new IllegalArgumentException("Illegal frame locator to build");
                this.frameBySequence = frameBySequence;
                return this;
            }

            /**
             * Clear before sending keys.
             * 
             * @return self reference
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
                Composable action = new SendKey(by, frameBySequence, clearBeforeSend, keysToSend);
                return parentActionSequenceBuilder.add(action);
            }
        }
        
        /**
         * Start building a {@link Extract}.
         * 
         * @param by the element locating mechanism
         * @return a new {@link ExtractBuilder} with this {@link ActionSequenceBuilder} as parent builder
         */
        public ExtractBuilder prepareExtract(By by){
            return new ExtractBuilder(this, by);
        }

        /**
         * A builder to build {@link Extract} in a fluent way.
         */
        public class ExtractBuilder extends InnerBuilderBase{
            private final By by;
            private List<By> frameBySequence;
            private String textVariableName;
            private Map<String, String> attrVariableNames;

            /**
             * Create a new {@link ExtractBuilder} with specified {@link ActionSequenceBuilder} as parent builder.
             * 
             * @param parentActionSequenceBuilder parent builder({@link ActionSequenceBuilder})
             * @param by the element locating mechanism
             */
            public ExtractBuilder(ActionSequenceBuilder parentActionSequenceBuilder, By by){
                super(parentActionSequenceBuilder);
                if (by==null) throw new IllegalArgumentException("No locator specified to build");
                this.by = by;
            }

            /**
             * Set the frame locating mechanism for the element resides in a frame.
             * 
             * @param frameBySequence the sequence of the frame locating mechanism
             * @return self reference
             */
            public ExtractBuilder withInFrame(List<By> frameBySequence){
                if (frameBySequence==null)  throw new IllegalArgumentException("Illegal frame locator to build");
                this.frameBySequence = frameBySequence;
                return this;
            }
            
            /**
             * Set the text of the element as a variable of building {@link ActionComposer}.
             * 
             * @param textVariableName text variable name
             * @return self reference
             */
            public ExtractBuilder withTextAsVariable(String textVariableName){
                if (textVariableName==null)  throw new IllegalArgumentException("Illegal text variable name to build");
                this.textVariableName = textVariableName;
                return this;
            }
            
            /**
             * Set the value of properties/attributes of the element as variables of building {@link ActionComposer}.
             * 
             * @param attrVariableNames (attribute name, variable name) pairs to set as variables
             * @return self reference
             * @see Extract#Extract(org.openqa.selenium.By, java.util.List, java.lang.String, java.util.Map) 
             */
            public ExtractBuilder withAttributeAsVariable(Map<String, String> attrVariableNames){
                if (attrVariableNames==null)  throw new IllegalArgumentException("Illegal attribute variable names to build");
                this.attrVariableNames = attrVariableNames;
                return this;
            }

            /**
             * Finish building {@link Extract} and add it to parent builder.
             * 
             * @return parent builder({@link ActionSequenceBuilder})
             */
            public ActionSequenceBuilder done(){
                Composable action = new Extract(by, frameBySequence, textVariableName, attrVariableNames);
                return parentActionSequenceBuilder.add(action);
            }
        }
        
        /**
         * Add a {@link Click} to the sequence of actions.
         * 
         * @param by the element locating mechanism
         * @return self reference
         */
        public ActionSequenceBuilder click(By by){
            return new ClickBuilder(this, by).done();
        }

        /**
         * Start building a {@link Click}.
         * 
         * @param by the element locating mechanism
         * @return a new {@link ClickBuilder} with this {@link ActionSequenceBuilder} as parent builder
         */
        public ClickBuilder prepareClick(By by){
            return new ClickBuilder(this, by);
        }

        /**
         * A builder to build {@link Click} in a fluent way.
         */
        public class ClickBuilder extends InnerBuilderBase{
            private final By by;
            private List<By> frameBySequence;

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
             * @param frameBySequence the sequence of the frame locating mechanism
             * @return self reference
             */
            public ClickBuilder withInFrame(List<By> frameBySequence){
                if (frameBySequence==null)  throw new IllegalArgumentException("Illegal frame locator to build");
                this.frameBySequence = frameBySequence;
                return this;
            }

            /**
             * Finish building {@link Click} and add it to parent builder.
             * 
             * @return parent builder({@link ActionSequenceBuilder})
             */
            public ActionSequenceBuilder done(){
                Composable action = new Click(by, frameBySequence);
                return parentActionSequenceBuilder.add(action);
            }
        }
        
        /**
         * Add a {@link MouseOver} to the sequence of actions.
         * 
         * @param by the element locating mechanism
         * @return self reference
         */
        public ActionSequenceBuilder mouseOver(By by){
            return new MouseOverBuilder(this, by).done();
        }

        /**
         * Start building a {@link MouseOver}.
         * 
         * @param by the element locating mechanism
         * @return a new {@link MouseOverBuilder} with this {@link ActionSequenceBuilder} as parent builder
         */
        public MouseOverBuilder prepareMouseOver(By by){
            return new MouseOverBuilder(this, by);
        }

        /**
         * A builder to build {@link MouseOver} in a fluent way.
         */
        public class MouseOverBuilder extends InnerBuilderBase{
            private final By by;
            private List<By> frameBySequence;

            /**
             * Create a new {@link MouseOverBuilder} with specified {@link ActionSequenceBuilder} as parent builder.
             * 
             * @param parentActionSequenceBuilder parent builder({@link ActionSequenceBuilder})
             * @param by the element locating mechanism
             */
            public MouseOverBuilder(ActionSequenceBuilder parentActionSequenceBuilder, By by){
                super(parentActionSequenceBuilder);
                if (by==null) throw new IllegalArgumentException("No locator specified to build");
                this.by = by;
            }

            /**
             * Set the frame locating mechanism for the element resides in a frame.
             * 
             * @param frameBySequence the sequence of the frame locating mechanism
             * @return self reference
             */
            public MouseOverBuilder withInFrame(List<By> frameBySequence){
                if (frameBySequence==null)  throw new IllegalArgumentException("Illegal frame locator to build");
                this.frameBySequence = frameBySequence;
                return this;
            }

            /**
             * Finish building {@link MouseOver} and add it to parent builder.
             * 
             * @return parent builder({@link ActionSequenceBuilder})
             */
            public ActionSequenceBuilder done(){
                Composable action = new MouseOver(by, frameBySequence);
                return parentActionSequenceBuilder.add(action);
            }
        }
        
        /**
         * Add a single-phased {@link Custom} to the sequence of actions.
         * 
         * @param customAction custom action
         * @return self reference
         */
        public ActionSequenceBuilder custom(Consumer<ActionComposer> customAction){
            return new CustomBuilder(this, customAction).done();
        }
        
        /**
         * Add a multiple-phased {@link Custom} to the sequence of actions.
         * 
         * @param multiPhasedCustomAction multiple-phased custom action
         * @return self reference
         */
        public ActionSequenceBuilder customMultiPhase(Function<MultiPhased,Consumer<ActionComposer>> multiPhasedCustomAction){
            return new CustomBuilder(this, multiPhasedCustomAction).done();
        }

        /**
         * Start building a single-phased {@link Custom}.
         * 
         * @param customAction custom action
         * @return a new {@link CustomBuilder} with this {@link ActionSequenceBuilder} as parent builder
         */
        public CustomBuilder prepareCustom(Consumer<ActionComposer> customAction){
            return new CustomBuilder(this, customAction);
        }
        
        /**
         * Start building a multiple-phased {@link Custom}.
         * 
         * @param multiPhasedCustomAction multiple-phased custom action
         * @return a new {@link CustomBuilder} with this {@link ActionSequenceBuilder} as parent builder
         */
        public CustomBuilder prepareCustomMultiPhase(Function<MultiPhased,Consumer<ActionComposer>> multiPhasedCustomAction){
            return new CustomBuilder(this, multiPhasedCustomAction);
        }

        /**
         * A builder to build {@link Custom} in a fluent way.
         */
        public class CustomBuilder extends InnerBuilderBase{
            private Consumer<ActionComposer> customAction;
            private Function<MultiPhased,Consumer<ActionComposer>> multiPhasedCustomAction;
            private List<By> frameBySequence;

            /**
             * Create a new {@link CustomBuilder} with specified {@link ActionSequenceBuilder} as parent builder.
             * This is used to construct a single-phased {@link Custom}.
             * 
             * @param parentActionSequenceBuilder parent builder({@link ActionSequenceBuilder})
             * @param customAction custom action
             */
            public CustomBuilder(ActionSequenceBuilder parentActionSequenceBuilder, Consumer<ActionComposer> customAction){
                super(parentActionSequenceBuilder);
                if (customAction==null) throw new IllegalArgumentException("No composer consumer specified to build");
                this.customAction = customAction;
            }
            
            /**
             * Create a new {@link CustomBuilder} with specified {@link ActionSequenceBuilder} as parent builder.
             * This is used to construct a multiple-phased {@link Custom}.
             * 
             * @param parentActionSequenceBuilder parent builder({@link ActionSequenceBuilder})
             * @param multiPhasedCustomAction multiple-phased custom action
             */
            public CustomBuilder(ActionSequenceBuilder parentActionSequenceBuilder, Function<MultiPhased,Consumer<ActionComposer>> multiPhasedCustomAction){
                super(parentActionSequenceBuilder);
                if (multiPhasedCustomAction==null) throw new IllegalArgumentException("No multiple-phase custom action specified to build");
                this.multiPhasedCustomAction = multiPhasedCustomAction;
            }

            /**
             * Set the frame locating mechanism for the frame where the custom action to be performed against.
             * 
             * @param frameBySequence the sequence of the frame locating mechanism
             * @return self reference
             */
            public CustomBuilder withInFrame(List<By> frameBySequence){
                if (frameBySequence==null)  throw new IllegalArgumentException("Illegal frame locator to build");
                this.frameBySequence = frameBySequence;
                return this;
            }
            
            /**
             * Finish building {@link Custom} and add it to parent builder.
             * 
             * @return parent builder({@link ActionSequenceBuilder})
             */
            public ActionSequenceBuilder done(){
                Composable action;
                if (customAction!=null) action = new Custom(customAction, frameBySequence);
                else action = new Custom(multiPhasedCustomAction, frameBySequence);
                return parentActionSequenceBuilder.add(action);
            }
        }
        
        /**
         * Add a {@link ScrollToView} to the sequence of actions.
         * 
         * @param by the element locating mechanism
         * @param toTop {@code true}: scroll to top;{@code false}: scroll to bottom
         * @return self reference
         */
        public ActionSequenceBuilder scrollToView(By by, boolean toTop){
            return new ScrollToViewBuilder(this, by, toTop).done();
        }

        /**
         * Start building a {@link ScrollToView}.
         * 
         * @param by the element locating mechanism
         * @param toTop {@code true}: scroll to top;{@code false}: scroll to bottom
         * @return a new {@link ScrollToViewBuilder} with this {@link ActionSequenceBuilder} as parent builder
         */
        public ScrollToViewBuilder prepareScrollToView(By by, boolean toTop){
            return new ScrollToViewBuilder(this, by, toTop);
        }

        /**
         * A builder to build {@link ScrollToView} in a fluent way.
         */
        public class ScrollToViewBuilder extends InnerBuilderBase{
            private final By by;
            private final boolean toTop;
            private List<By> frameBySequence;
            

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
             * @param frameBySequence the sequence of the frame locating mechanism
             * @return self reference
             */
            public ScrollToViewBuilder withInFrame(List<By> frameBySequence){
                if (frameBySequence==null)  throw new IllegalArgumentException("Illegal frame locator to build");
                this.frameBySequence = frameBySequence;
                return this;
            }

            /**
             * Finish building {@link ScrollToView} and add it to parent builder.
             * 
             * @return parent builder({@link ActionSequenceBuilder})
             */
            public ActionSequenceBuilder done(){
                Composable action = new ScrollToView(by, frameBySequence, toTop);
                return parentActionSequenceBuilder.add(action);
            }
        }
        
        /**
         * Add a {@link Upload} to the sequence of actions.
         * 
         * @param by the element locating mechanism
         * @param pathOfFiles the paths of files to upload
         * @return self reference
         */
        public ActionSequenceBuilder upload(By by, String... pathOfFiles){
            return new UploadBuilder(this, by, pathOfFiles).done();
        }

        /**
         * Start building a {@link Upload}.
         * 
         * @param by the element locating mechanism
         * @param pathOfFiles the paths of files to upload
         * @return a new {@link UploadBuilder} with this {@link ActionSequenceBuilder} as parent builder
         */
        public UploadBuilder prepareUpload(By by, String... pathOfFiles){
            return new UploadBuilder(this, by, pathOfFiles);
        }

        /**
         * A builder to build {@link Upload} in a fluent way.
         */
        public class UploadBuilder extends InnerBuilderBase{
            private final By by;
            private final String[] pathOfFiles;
            private List<By> frameBySequence;
            

            /**
             * Create a new {@link UploadBuilder} with specified {@link ActionSequenceBuilder} as parent builder.
             * 
             * @param parentActionSequenceBuilder parent builder({@link ActionSequenceBuilder})
             * @param by the element locating mechanism
             * @param pathOfFiles the paths of files to upload
             */
            public UploadBuilder(ActionSequenceBuilder parentActionSequenceBuilder, By by, String... pathOfFiles){
                super(parentActionSequenceBuilder);
                if (by==null) throw new IllegalArgumentException("No locator specified to build");
                this.by = by;
                this.pathOfFiles = pathOfFiles;
            }

            /**
             * Set the frame locating mechanism for the element resides in a frame.
             * 
             * @param frameBySequence the sequence of the frame locating mechanism
             * @return self reference
             */
            public UploadBuilder withInFrame(List<By> frameBySequence){
                if (frameBySequence==null)  throw new IllegalArgumentException("Illegal frame locator to build");
                this.frameBySequence = frameBySequence;
                return this;
            }

            /**
             * Finish building {@link Upload} and add it to parent builder.
             * 
             * @return parent builder({@link ActionSequenceBuilder})
             */
            public ActionSequenceBuilder done(){
                Composable action = new Upload(by, frameBySequence, pathOfFiles);
                return parentActionSequenceBuilder.add(action);
            }
        }        
        
        /**
         * Add a {@link ReplyAlert} to the sequence of actions.
         * 
         * @param decision the way to deal with alert box
         * @return self reference
         */
        public ActionSequenceBuilder replyAlert(Decision decision){
            return new ReplyAlertBuilder(this, decision).done();
        }

        /**
         * Start building a {@link ReplyAlert}.
         * 
         * @param decision the way to deal with alert box
         * @return a new {@link ReplyAlertBuilder} with this {@link ActionSequenceBuilder} as parent builder
         */
        public ReplyAlertBuilder prepareReplyAlert(Decision decision){
            return new ReplyAlertBuilder(this, decision);
        }

        /**
         * A builder to build {@link ReplyAlert} in a fluent way.
         */
        public class ReplyAlertBuilder extends InnerBuilderBase{
            private final Decision decision;
            private String textVariableName;
            private String keysToSend;

            /**
             * Create a new {@link ReplyAlertBuilder} with specified {@link ActionSequenceBuilder} as parent builder.
             * 
             * @param parentActionSequenceBuilder parent builder({@link ActionSequenceBuilder})
             * @param decision the way to deal with alert box
             */
            public ReplyAlertBuilder(ActionSequenceBuilder parentActionSequenceBuilder, Decision decision){
                super(parentActionSequenceBuilder);
                if (decision==null) throw new IllegalArgumentException("No decision specified to build");
                this.decision = decision;
            }
            
            /**
             * Set the text of alert box as a variable of building {@link ActionComposer}.
             * 
             * @param textVariableName text variable name
             * @return self reference
             */
            public ReplyAlertBuilder withTextAsVariable(String textVariableName){
                if (textVariableName==null)  throw new IllegalArgumentException("Illegal text variable name to build");
                this.textVariableName = textVariableName;
                return this;
            }
            
            /**
             * Send characters to alert box.
             * 
             * @param keysToSend characters to send to alert box
             * @return self reference
             */
            public ReplyAlertBuilder withKeysToSend(String keysToSend){
                if (keysToSend==null)  throw new IllegalArgumentException("Illegal keys-to-send to build");
                this.keysToSend = keysToSend;
                return this;
            }

            /**
             * Finish building {@link ReplyAlert} and add it to parent builder.
             * 
             * @return parent builder({@link ActionSequenceBuilder})
             */
            public ActionSequenceBuilder done(){
                Composable action = new ReplyAlert(decision, textVariableName, keysToSend);
                return parentActionSequenceBuilder.add(action);
            }
        }
                
        /**
         *
         * Start building an {@link IfThenElse}.
         * 
         * @param evalFunction the function to evaluate
         * @return a new {@link IfThenElseBuilder} with this {@link ActionSequenceBuilder} as parent builder
         */
        public IfThenElseBuilder prepareIfThenElse(Function<ActionComposer, ?> evalFunction){
            return new IfThenElseBuilder(this, evalFunction);
        }

        /**
         * A builder to build {@link IfThenElse} in a fluent way.
         */
        public class IfThenElseBuilder extends InnerBuilderBase{
            private final Function<ActionComposer, ?> evalFunction;
            private boolean isPrepareThenAction = true;
            private final List<Composable> positiveActionList = new ArrayList<>();
            private final List<Composable> negativeActionList = new ArrayList<>();

            /**
             * Create a new {@link IfThenElseBuilder} with specified {@link ActionSequenceBuilder} as parent builder.
             * 
             * @param parentActionSequenceBuilder parent builder({@link ActionSequenceBuilder})
             * @param evalFunction the function to evaluate
             */
            public IfThenElseBuilder(ActionSequenceBuilder parentActionSequenceBuilder, Function<ActionComposer, ?> evalFunction){
                super(parentActionSequenceBuilder);
                if (evalFunction==null) throw new IllegalArgumentException("No evaluation function specified to build");
                this.evalFunction = evalFunction;
            }
            
            /**
             * Add action to the building {@link IfThenElseBuilder}.It depends on the building progress to add to positive or negative action list.
             * 
             * @param action the action to add
             * @return self reference
             */
            private IfThenElseBuilder add(Composable action){
                if (action!=null){
                    if (isPrepareThenAction) positiveActionList.add(action);
                    else negativeActionList.add(action);
                }
                return this;
            }
            
            /**
             * Finish building the sequence of actions so far and return control to root builder({@link ActionComposerBuilder}).
             * 
             * @return root builder({@link ActionComposerBuilder})
             */
            public ActionComposerBuilder returnToComposerBuilder(){
                parentActionSequenceBuilder.add(new IfThenElse(evalFunction, positiveActionList, negativeActionList));
                return parentActionSequenceBuilder.returnToComposerBuilder();
            }
            
            /**
             * Start building the action list for the positive result of predicate.
             * 
             * @return a new {@link ActionSequenceBuilder} with this {@link IfThenElseBuilder} as parent builder
             */
            public ActionSequenceBuilder then(){
                isPrepareThenAction = true;
                return new ActionSequenceBuilder(this);
            }
            
            /**
             * Start building the action list for the negative result of predicate.
             * 
             * @return a new {@link ActionSequenceBuilder} with this {@link IfThenElseBuilder} as parent builder
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
                parentActionSequenceBuilder.add(new IfThenElse(evalFunction, positiveActionList, negativeActionList));
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
