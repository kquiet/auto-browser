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
import java.util.Optional;
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
import org.kquiet.browser.action.ActionUtility;
import org.kquiet.browser.action.control.IfThenElse;

/**
 *
 * @author Kimberly
 */
public class ActionComposerBuilder{
    private ActionComposer actionComposer = null;
    
    /**
     *
     */
    public ActionComposerBuilder(){
        prepareActionComposer();
    }
    
    private void prepareActionComposer(){
        if (actionComposer==null){
            actionComposer = new ActionComposer();
        }
    }
    
    /**
     *
     * @return
     */
    public ActionComposer getBrowserActionComposer(){
        return actionComposer;
    }
    
    /**
     *
     * @param action
     * @return
     */
    public ActionComposerBuilder addToHead(MultiPhaseAction action){
        prepareActionComposer();
        actionComposer.addActionToHead(action);
        return this;
    }
    
    /**
     *
     * @param action
     * @return
     */
    public ActionComposerBuilder addToTail(MultiPhaseAction action){
        prepareActionComposer();
        actionComposer.addActionToTail(action);
        return this;
    }
    
    /**
     *
     * @param action
     * @param index
     * @return
     */
    public ActionComposerBuilder addToIndex(MultiPhaseAction action, int index){
        prepareActionComposer();
        actionComposer.addActionToIndex(action, index);
        return this;
    }
    
    /**
     *
     * @param func
     * @return
     */
    public ActionComposerBuilder onFail(Consumer<ActionComposer> func){
        actionComposer.setOnFailFunction(func);
        return this;
    }
    
    /**
     *
     * @param func
     * @return
     */
    public ActionComposerBuilder onSuccess(Consumer<ActionComposer> func){
        actionComposer.setOnSuccessFunction(func);
        return this;
    }
    
    /**
     *
     * @param func
     * @return
     */
    public ActionComposerBuilder onDone(Consumer<ActionComposer> func){
        actionComposer.setOnDoneFunction(func);
        return this;
    }
    
    /**
     *
     * @return
     */
    public ActionComposer build(){
        return build(UUID.randomUUID().toString(), true, true);
    }
    
    /**
     *
     * @param openWindowFlag
     * @param closeWindowFlag
     * @return
     */
    public ActionComposer build(boolean openWindowFlag, boolean closeWindowFlag){
        return build(UUID.randomUUID().toString(), openWindowFlag, closeWindowFlag);
    }
    
    /**
     *
     * @param name
     * @return
     */
    public ActionComposer build(String name){
        return build(name, true, true);
    }
    
    /**
     *
     * @param name
     * @param openWindowFlag
     * @param closeWindowFlag
     * @return
     */
    public ActionComposer build(String name, boolean openWindowFlag, boolean closeWindowFlag){
        final ActionComposer currentActionComposer = actionComposer;
        currentActionComposer.setName(name);
        currentActionComposer.setOpenWindow(openWindowFlag);
        currentActionComposer.setCloseWindow(closeWindowFlag);
        
        MultiPhaseAction initAction = new IfThenElse(ac->currentActionComposer.needOpenWindow(), Arrays.asList(new OpenWindow(true, UUID.randomUUID().toString())), null);
        currentActionComposer.setInitAction(initAction);
        
        MultiPhaseAction finalAction = new IfThenElse(ac->currentActionComposer.needCloseWindow(), Arrays.asList(new CloseWindow()), null);
        currentActionComposer.setFinalAction(finalAction);
        
        //clear for next build
        clearActionComposer();
        return currentActionComposer;
    }
    
    private void clearActionComposer(){
        this.actionComposer = null;
    }
    
    /**
     *
     * @param action
     * @return
     */
    public ActionComposerBuilder accept(MultiPhaseAction action){
            if (action!=null) addToTail(action);
            return this;
        }
    
    /**
     *
     * @return
     */
    public ActionSequenceBuilder prepareActionSequence(){
        return new ActionSequenceBuilder(this);
    }
    
    
    //inner builder

    /**
     *
     */
    public class ActionSequenceBuilder{
        private final ActionComposerBuilder parentComposerBuilder;
        private final IfThenElseBuilder parentIfThenElseBuilder;
        private final List<MultiPhaseAction> actionList = new ArrayList<>();
        
        /**
         *
         * @param parentComposerBuilder
         */
        public ActionSequenceBuilder(ActionComposerBuilder parentComposerBuilder){
            if (parentComposerBuilder==null) throw new IllegalArgumentException("No parent builder");
            this.parentComposerBuilder = parentComposerBuilder;
            this.parentIfThenElseBuilder = null;
        }
        
        /**
         *
         * @param parentIfThenElseBuilder
         */
        public ActionSequenceBuilder(IfThenElseBuilder parentIfThenElseBuilder){
            if (parentIfThenElseBuilder==null) throw new IllegalArgumentException("No parent builder");
            this.parentComposerBuilder = null;
            this.parentIfThenElseBuilder = parentIfThenElseBuilder;
        }
        
        /**
         *
         * @param action
         * @return
         */
        public ActionSequenceBuilder accept(MultiPhaseAction action){
            if (action!=null) actionList.add(action);
            return this;
        }
        
        /**
         *
         * @return
         */
        public ActionComposerBuilder returnToComposerBuilder(){
            if (parentIfThenElseBuilder!=null){
                actionList.forEach(action->{
                    parentIfThenElseBuilder.accept(action);
                });
                return parentIfThenElseBuilder.returnToComposerBuilder();
            }
            else{
                actionList.forEach(action->{
                    parentComposerBuilder.accept(action);
                });
                return parentComposerBuilder;
            }
        }
        
        /**
         *
         * @return
         */
        public IfThenElseBuilder endActionSequence(){
            if (parentIfThenElseBuilder==null) throw new IllegalArgumentException("Parent builder is not IfThenElseBuilder");
            actionList.forEach(action->{
                parentIfThenElseBuilder.accept(action);
            });
            return parentIfThenElseBuilder;
        }
        
        /**
         *
         * @return
         */
        public ActionSequenceBuilder closeWindow(){
            return new CloseWindowBuilder(this).done();
        }

        /**
         *
         * @return
         */
        public CloseWindowBuilder prepareCloseWindow(){
            return new CloseWindowBuilder(this);
        }

        /**
         *
         */
        public class CloseWindowBuilder extends InnerBuilderBase{
            private String registeredName = null;
            private boolean closeAllRegistered = false;

            /**
             *
             * @param parentActionSequenceBuilder
             */
            public CloseWindowBuilder(ActionSequenceBuilder parentActionSequenceBuilder){
                super(parentActionSequenceBuilder);
            }

            /**
             *
             * @return
             */
            public CloseWindowBuilder forAllRegistered(){
                this.closeAllRegistered = true;
                return this;
            }

            /**
             *
             * @param registeredName
             * @return
             */
            public CloseWindowBuilder forRegisteredName(String registeredName){
                this.registeredName = registeredName;
                return this;
            }

            /**
             *
             * @return
             */
            public ActionSequenceBuilder done(){
                MultiPhaseAction action;
                if (closeAllRegistered) action = new CloseWindow(true);
                else if (!Optional.ofNullable(registeredName).orElse("").isEmpty()) action = new CloseWindow(registeredName);
                else action = new CloseWindow();
                return parentActionSequenceBuilder.accept(action);
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
                return parentActionSequenceBuilder.accept(action);
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
                return parentActionSequenceBuilder.accept(action);
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
                return parentActionSequenceBuilder.accept(action);
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
                return parentActionSequenceBuilder.accept(action);
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
                return parentActionSequenceBuilder.accept(action);
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
                return parentActionSequenceBuilder.accept(action);
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
                this.keysToSend = ActionUtility.purifyCharSequences(keysToSend);
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
                return parentActionSequenceBuilder.accept(action);
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
                return parentActionSequenceBuilder.accept(action);
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
                return parentActionSequenceBuilder.accept(action);
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
            public IfThenElseBuilder accept(MultiPhaseAction action){
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
                parentActionSequenceBuilder.accept(new IfThenElse(predicate, thenActionList, elseActionList));
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
                parentActionSequenceBuilder.accept(new IfThenElse(predicate, thenActionList, elseActionList));
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
