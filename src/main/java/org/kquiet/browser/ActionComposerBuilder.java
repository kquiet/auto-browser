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
 * {@link ActionComposerBuilder} is a builder to build {@link ActionComposer} in a fluent way.
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
         * @return this {@link ActionSequenceBuilder}
         */
        public ActionSequenceBuilder add(MultiPhaseAction action){
            if (action!=null) actionList.add(action);
            return this;
        }
        
        /**
         * Finish building the sequence of actions so far and return control to root builder({@link ActionComposerBuilder}).
         * 
         * @return {@link ActionComposerBuilder}
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
         * @return {@link IfThenElseBuilder}
         * @throws IllegalArgumentException if parent builder is not {@link IfThenElseBuilder}.
         */
        public IfThenElseBuilder endActionSequence(){
            if (parentIfThenElseBuilder==null) throw new IllegalArgumentException("Parent builder is not IfThenElseBuilder");
            actionList.forEach(action->{
                parentIfThenElseBuilder.add(action);
            });
            return parentIfThenElseBuilder;
        }
        
        /**
         *
         * @param closeAllRegistered
         * @return
         */
        public ActionSequenceBuilder closeWindow(boolean closeAllRegistered){
            return new CloseWindowBuilder(this, closeAllRegistered).done();
        }

        /**
         *
         * @param closeAllRegistered
         * @return
         */
        public CloseWindowBuilder prepareCloseWindow(boolean closeAllRegistered){
            return new CloseWindowBuilder(this, closeAllRegistered);
        }

        /**
         *
         */
        public class CloseWindowBuilder extends InnerBuilderBase{
            private final boolean closeAllRegistered;

            /**
             *
             * @param parentActionSequenceBuilder
             * @param closeAllRegistered
             */
            public CloseWindowBuilder(ActionSequenceBuilder parentActionSequenceBuilder, boolean closeAllRegistered){
                super(parentActionSequenceBuilder);
                this.closeAllRegistered = closeAllRegistered;
            }

            /**
             *
             * @return
             */
            public ActionSequenceBuilder done(){
                MultiPhaseAction action = new CloseWindow(closeAllRegistered);
                return parentActionSequenceBuilder.add(action);
            }
        }
        
        /**
         *
         * @return
         */
        public ActionSequenceBuilder openWindow(){
            return new OpenWindowBuilder(this).done();
        }

        /**
         *
         * @return
         */
        public OpenWindowBuilder prepareOpenWindow(){
            return new OpenWindowBuilder(this);
        }

        /**
         *
         */
        public class OpenWindowBuilder extends InnerBuilderBase{
            private boolean asComposerFocusWindow = false;
            private String registerName = UUID.randomUUID().toString();

            /**
             *
             * @param parentActionSequenceBuilder
             */
            public OpenWindowBuilder(ActionSequenceBuilder parentActionSequenceBuilder){
                super(parentActionSequenceBuilder);
            }

            /**
             *
             * @return
             */
            public OpenWindowBuilder withComposerFocus(){
                this.asComposerFocusWindow = true;
                return this;
            }

            /**
             *
             * @param registerName
             * @return
             */
            public OpenWindowBuilder registerAs(String registerName){
                this.registerName = registerName;
                return this;
            }

            /**
             *
             * @return
             */
            public ActionSequenceBuilder done(){
                MultiPhaseAction action = new OpenWindow(asComposerFocusWindow, registerName);
                return parentActionSequenceBuilder.add(action);
            }
        }
        
        /**
         *
         * @param <V>
         * @param evaluateFunc
         * @param totalTimeout
         * @return
         */
        public <V> ActionSequenceBuilder waitUntil(Function<WebDriver,V> evaluateFunc, int totalTimeout){
            return new WaitUntilBuilder<>(this, evaluateFunc, totalTimeout).done();
        }

        /**
         *
         * @param <V>
         * @param evaluateFunc
         * @param totalTimeout
         * @return
         */
        public <V> WaitUntilBuilder<V> prepareWaitUntil(Function<WebDriver,V> evaluateFunc, int totalTimeout){
            return new WaitUntilBuilder<>(this, evaluateFunc, totalTimeout);
        }

        /**
         *
         * @param <V>
         */
        public class WaitUntilBuilder<V> extends InnerBuilderBase{
            private final int totalTimeout;
            private final Function<WebDriver,V> evaluateFunc;
            private int phaseTimeout = 10;
            private int pollInterval = 5;
            private List<Class<? extends Throwable>> ignoreExceptionList = Arrays.asList(NoSuchElementException.class, StaleElementReferenceException.class);
            private Consumer<ActionComposer> timeoutCallback;

            /**
             *
             * @param parentActionSequenceBuilder
             * @param evaluateFunc
             * @param totalTimeout
             */
            public WaitUntilBuilder(ActionSequenceBuilder parentActionSequenceBuilder, Function<WebDriver,V> evaluateFunc, int totalTimeout){
                super(parentActionSequenceBuilder);
                if (evaluateFunc==null) throw new IllegalArgumentException("No evaluate function to build");
                if (totalTimeout<=0) throw new IllegalArgumentException("Illegal timeout to build");
                this.evaluateFunc = evaluateFunc;
                this.totalTimeout = totalTimeout;
                
            }

            /**
             *
             * @param phaseTimeout
             * @return
             */
            public WaitUntilBuilder<V> withPhaseTimeout(int phaseTimeout){
                if (phaseTimeout<=0) throw new IllegalArgumentException("Illegal phase timeout to build");
                this.phaseTimeout = phaseTimeout;
                return this;
            }

            /**
             *
             * @param pollInterval
             * @return
             */
            public WaitUntilBuilder<V> withPollInterval(int pollInterval){
                if (pollInterval<=0) throw new IllegalArgumentException("Illegal poll interval to build");
                this.pollInterval = pollInterval;
                return this;
            }

            /**
             *
             * @param ignoreExceptionList
             * @return
             */
            public WaitUntilBuilder<V> withIgnoredException(List<Class<? extends Throwable>> ignoreExceptionList){
                if (ignoreExceptionList==null || ignoreExceptionList.isEmpty()) throw new IllegalArgumentException("Illegal ignore exception list to build");
                this.ignoreExceptionList = ignoreExceptionList;
                return this;
            }

            /**
             *
             * @param timeoutCallback
             * @return
             */
            public WaitUntilBuilder<V> withTimeoutCallback(Consumer<ActionComposer> timeoutCallback){
                if (timeoutCallback==null) throw new IllegalArgumentException("Illegal timeout callback to build");
                this.timeoutCallback = timeoutCallback;
                return this;
            }

            /**
             *
             * @return
             */
            public ActionSequenceBuilder done(){
                MultiPhaseAction action = new WaitUntil<>(evaluateFunc, totalTimeout, phaseTimeout, pollInterval, ignoreExceptionList, timeoutCallback);
                return parentActionSequenceBuilder.add(action);
            }
        }
        
        /**
         *
         * @param totalTimeout
         * @return
         */
        public ActionSequenceBuilder justWait(int totalTimeout){
            return new JustWaitBuilder(this, totalTimeout).done();
        }

        /**
         *
         * @param totalTimeout
         * @return
         */
        public JustWaitBuilder prepareJustWait(int totalTimeout){
            return new JustWaitBuilder(this, totalTimeout);
        }

        /**
         *
         */
        public class JustWaitBuilder extends InnerBuilderBase{
            private final int totalTimeout;
            private int phaseTimeout = 10;

            /**
             *
             * @param parentActionSequenceBuilder
             * @param totalTimeout
             */
            public JustWaitBuilder(ActionSequenceBuilder parentActionSequenceBuilder, int totalTimeout){
                super(parentActionSequenceBuilder);
                if (totalTimeout<=0) throw new IllegalArgumentException("Illegal timeout to build");
                this.totalTimeout = totalTimeout;
            }

            /**
             *
             * @param phaseTimeout
             * @return
             */
            public JustWaitBuilder withPhaseTimeout(int phaseTimeout){
                if (phaseTimeout<=0) throw new IllegalArgumentException("Illegal phase timeout to build");
                this.phaseTimeout = phaseTimeout;
                return this;
            }

            /**
             *
             * @return
             */
            public ActionSequenceBuilder done(){
                MultiPhaseAction action = new JustWait(totalTimeout, phaseTimeout);
                return parentActionSequenceBuilder.add(action);
            }
        }
        
        /**
         *
         * @param url
         * @param simpleFormData
         * @return
         */
        public ActionSequenceBuilder postForm(String url, List<SimpleImmutableEntry<String,String>> simpleFormData){
            return new PostFormBuilder(this, url).withSimpleFormData(simpleFormData).done();
        }

        /**
         *
         * @param url
         * @return
         */
        public PostFormBuilder preparePostForm(String url){
            return new PostFormBuilder(this, url);
        }

        /**
         *
         */
        public class PostFormBuilder extends InnerBuilderBase{
            private final String url;
            private List<SimpleImmutableEntry<String,String>> simpleFormData;
            private String acceptCharset;

            /**
             *
             * @param parentActionSequenceBuilder
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
             * @return
             */
            public PostFormBuilder withSimpleFormData(List<SimpleImmutableEntry<String,String>> simpleFormData){
                if (simpleFormData==null) throw new IllegalArgumentException("Illegal form data to build");
                this.simpleFormData = simpleFormData;
                return this;
            }

            /**
             *
             * @param acceptCharset
             * @return
             */
            public PostFormBuilder withAcceptCharset(String acceptCharset){
                if (acceptCharset==null || acceptCharset.isEmpty()) throw new IllegalArgumentException("Illegal accept charset to build");
                this.acceptCharset = acceptCharset;
                return this;
            }

            /**
             *
             * @return
             */
            public ActionSequenceBuilder done(){
                MultiPhaseAction action = new PostForm(url, simpleFormData, acceptCharset);
                return parentActionSequenceBuilder.add(action);
            }
        }
        
        /**
         *
         * @param url
         * @return
         */
        public ActionSequenceBuilder getUrl(String url){
            return new GetUrlBuilder(this, url).done();
        }

        /**
         *
         * @param url
         * @return
         */
        public GetUrlBuilder prepareGetUrl(String url){
            return new GetUrlBuilder(this, url);
        }

        /**
         *
         */
        public class GetUrlBuilder extends InnerBuilderBase{
            private final String url;

            /**
             *
             * @param parentActionSequenceBuilder
             * @param url
             */
            public GetUrlBuilder(ActionSequenceBuilder parentActionSequenceBuilder, String url){
                super(parentActionSequenceBuilder);
                if (url==null || url.isEmpty()) throw new IllegalArgumentException("No url specified to build");
                this.url = url;
            }

            /**
             *
             * @return
             */
            public ActionSequenceBuilder done(){
                MultiPhaseAction action = new GetUrl(url);
                return parentActionSequenceBuilder.add(action);
            }
        }
        
        /**
         *
         * @param by
         * @param options
         * @return
         */
        public ActionSequenceBuilder selectByIndex(By by, Integer... options){
            return new SelectBuilder(this, by).selectByIndex(options).done();
        }

        /**
         *
         * @param by
         * @param options
         * @return
         */
        public ActionSequenceBuilder selectByText(By by, String... options){
            return new SelectBuilder(this, by).selectByText(options).done();
        }

        /**
         *
         * @param by
         * @param options
         * @return
         */
        public ActionSequenceBuilder selectByValue(By by, String... options){
            return new SelectBuilder(this, by).selectByValue(options).done();
        }

        /**
         *
         * @param by
         * @return
         */
        public SelectBuilder prepareSelect(By by){
            return new SelectBuilder(this, by);
        }

        /**
         *
         */
        public class SelectBuilder extends InnerBuilderBase{
            private final By by;
            private By frameBy;
            private SelectBy selectBy;
            private Object[] options;

            /**
             *
             * @param parentActionSequenceBuilder
             * @param by
             */
            public SelectBuilder(ActionSequenceBuilder parentActionSequenceBuilder, By by){
                super(parentActionSequenceBuilder);
                if (by==null) throw new IllegalArgumentException("No locator specified to build");
                this.by = by;
            }

            /**
             *
             * @param frameBy
             * @return
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
             * @return
             */
            public SelectBuilder selectByIndex(Integer... options){
                return selectBy(SelectBy.Index, (Object[]) options);
            }

            /**
             *
             * @param options
             * @return
             */
            public SelectBuilder selectByText(String... options){
                return selectBy(SelectBy.Text, (Object[]) options);
            }

            /**
             *
             * @param options
             * @return
             */
            public SelectBuilder selectByValue(String... options){
                return selectBy(SelectBy.Value, (Object[]) options);
            }

            /**
             *
             * @return
             */
            public ActionSequenceBuilder done(){
                MultiPhaseAction action = new Select(by, frameBy, selectBy, options);
                return parentActionSequenceBuilder.add(action);
            }
        }
        
        /**
         *
         * @param by
         * @param keysToSend
         * @return
         */
        public ActionSequenceBuilder sendKey(By by, CharSequence... keysToSend){
            return new SendKeyBuilder(this, by, keysToSend).done();
        }

        /**
         *
         * @param by
         * @param keysToSend
         * @return
         */
        public SendKeyBuilder prepareSendKey(By by, CharSequence... keysToSend){
            return new SendKeyBuilder(this, by, keysToSend);
        }

        /**
         *
         */
        public class SendKeyBuilder extends InnerBuilderBase{
            private final By by;
            private final CharSequence[] keysToSend;
            private volatile By frameBy;
            private volatile boolean clearBeforeSend=false;

            /**
             *
             * @param parentActionSequenceBuilder
             * @param by
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
             *
             * @param frameBy
             * @return
             */
            public SendKeyBuilder withInFrame(By frameBy){
                if (frameBy==null)  throw new IllegalArgumentException("Illegal frame locator to build");
                this.frameBy = frameBy;
                return this;
            }

            /**
             *
             * @return
             */
            public SendKeyBuilder withClearBeforeSend(){
                this.clearBeforeSend = true;
                return this;
            }

            /**
             *
             * @return
             */
            public ActionSequenceBuilder done(){
                MultiPhaseAction action = new SendKey(by, frameBy, clearBeforeSend, keysToSend);
                return parentActionSequenceBuilder.add(action);
            }
        }
        
        /**
         *
         * @param by
         * @return
         */
        public ActionSequenceBuilder click(By by){
            return new ClickBuilder(this, by).done();
        }

        /**
         *
         * @param by
         * @return
         */
        public ClickBuilder prepareClick(By by){
            return new ClickBuilder(this, by);
        }

        /**
         *
         */
        public class ClickBuilder extends InnerBuilderBase{
            private final By by;
            private volatile By frameBy;

            /**
             *
             * @param parentActionSequenceBuilder
             * @param by
             */
            public ClickBuilder(ActionSequenceBuilder parentActionSequenceBuilder, By by){
                super(parentActionSequenceBuilder);
                if (by==null) throw new IllegalArgumentException("No locator specified to build");
                this.by = by;
            }

            /**
             *
             * @param frameBy
             * @return
             */
            public ClickBuilder withInFrame(By frameBy){
                if (frameBy==null)  throw new IllegalArgumentException("Illegal frame locator to build");
                this.frameBy = frameBy;
                return this;
            }

            /**
             *
             * @return
             */
            public ActionSequenceBuilder done(){
                MultiPhaseAction action = new Click(by, frameBy);
                return parentActionSequenceBuilder.add(action);
            }
        }
        
        /**
         *
         * @param composerConsumer
         * @return
         */
        public ActionSequenceBuilder custom(Consumer<ActionComposer> composerConsumer){
            return new CustomBuilder(this, composerConsumer).done();
        }

        /**
         *
         * @param composerConsumer
         * @return
         */
        public CustomBuilder prepareCustom(Consumer<ActionComposer> composerConsumer){
            return new CustomBuilder(this, composerConsumer);
        }

        /**
         *
         */
        public class CustomBuilder extends InnerBuilderBase{
            private final Consumer<ActionComposer> composerConsumer;

            /**
             *
             * @param parentActionSequenceBuilder
             * @param composerConsumer
             */
            public CustomBuilder(ActionSequenceBuilder parentActionSequenceBuilder, Consumer<ActionComposer> composerConsumer){
                super(parentActionSequenceBuilder);
                if (composerConsumer==null) throw new IllegalArgumentException("No composer consumer specified to build");
                this.composerConsumer = composerConsumer;
            }

            /**
             *
             * @return
             */
            public ActionSequenceBuilder done(){
                MultiPhaseAction action = new Custom(composerConsumer);
                return parentActionSequenceBuilder.add(action);
            }
        }
        
        /**
         *
         * @param by
         * @param toTop
         * @return
         */
        public ActionSequenceBuilder scrollToView(By by, boolean toTop){
            return new ScrollToViewBuilder(this, by, toTop).done();
        }

        /**
         *
         * @param by
         * @param toTop
         * @return
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
             *
             * @param parentActionSequenceBuilder
             * @param by
             * @param toTop
             */
            public ScrollToViewBuilder(ActionSequenceBuilder parentActionSequenceBuilder, By by, boolean toTop){
                super(parentActionSequenceBuilder);
                if (by==null) throw new IllegalArgumentException("No locator specified to build");
                this.by = by;
                this.toTop = toTop;
            }

            /**
             *
             * @param frameBy
             * @return
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
         *
         * @param by
         * @param pathOfFile
         * @return
         */
        public ActionSequenceBuilder upload(By by, String pathOfFile){
            return new UploadBuilder(this, by, pathOfFile).done();
        }

        /**
         *
         * @param by
         * @param pathOfFile
         * @return
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
             *
             * @param parentActionSequenceBuilder
             * @param by
             * @param pathOfFile
             */
            public UploadBuilder(ActionSequenceBuilder parentActionSequenceBuilder, By by, String pathOfFile){
                super(parentActionSequenceBuilder);
                if (by==null) throw new IllegalArgumentException("No locator specified to build");
                this.by = by;
                this.pathOfFile = pathOfFile;
            }

            /**
             *
             * @param frameBy
             * @return
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
         * @param predicate
         * @return
         */
        public IfThenElseBuilder prepareIfThenElse(Predicate<ActionComposer> predicate){
            return new IfThenElseBuilder(this, predicate);
        }

        /**
         *
         */
        public class IfThenElseBuilder extends InnerBuilderBase{
            private final Predicate<ActionComposer> predicate;
            private volatile boolean isPrepareThenAction = true;
            private final List<MultiPhaseAction> thenActionList = new ArrayList<>();
            private final List<MultiPhaseAction> elseActionList = new ArrayList<>();

            /**
             *
             * @param parentActionSequenceBuilder
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
             * @return
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
             *
             * @return
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
