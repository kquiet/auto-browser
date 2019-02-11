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

import java.io.File;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.ImmutableCapabilities;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.PageLoadStrategy;

import org.kquiet.concurrent.PausablePriorityThreadPoolExecutor;
import org.kquiet.concurrent.PriorityRunnable;

/**
 * {@link BasicActionRunner} maintains two prioritized thread pool internally to execute {@link ActionComposer} and browser actions separatedly.
 * The thread pool for {@link ActionComposer} allows multiple ones to be run concurrently(depends on parameter values in constructors).
 * The thread pool for browser actions is single-threaded,
 * so only one browser action is executed at a time(due to <a href="https://github.com/SeleniumHQ/selenium/wiki/Frequently-Asked-Questions#q-is-webdriver-thread-safe" target="_blank"> the constraint of WebDriver</a>).
 * 
 * @author Kimberly
 */
public class BasicActionRunner implements ActionRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(BasicActionRunner.class);
    
    private String name = "";
    private final WebDriver brsDriver;
    private final String rootWindowIdentity;
    
    private volatile boolean isPaused = false;
    
    private final PausablePriorityThreadPoolExecutor browserActionExecutor;
    private final PausablePriorityThreadPoolExecutor composerExecutor;

    /**
     * Create an {@link BasicActionRunner} with {@link PageLoadStrategy#NONE} as page load strategy and {@link BrowserType#CHROME} as browser type.
     * At most one {@link ActionComposer} is allowed to be executed at a time.
     * 
     */
    public BasicActionRunner(){
        this(1);
    }
    
    /**
     * Create an {@link BasicActionRunner} with {@link PageLoadStrategy#NONE} as page load strategy and {@link BrowserType#CHROME} as browser type.
     * 
     * @param maxConcurrentComposer the max number of {@link ActionComposer} that could be executed by this {@link BasicActionRunner} concurrently.
     */
    public BasicActionRunner(int maxConcurrentComposer){
        this(PageLoadStrategy.NONE, BrowserType.CHROME, maxConcurrentComposer);
    }
    
    /**
     * Create an {@link BasicActionRunner} with specified page load strategy, browser type, name, and max number of concurrent composer.
     * 
     * @param pageLoadStrategy page load strategy
     * @param browserType the type of browser
     * @param maxConcurrentComposer max number of {@link ActionComposer} that could be executed concurrently.
     * @see <a href="https://github.com/SeleniumHQ/selenium/blob/master/java/client/src/org/openqa/selenium/PageLoadStrategy.java" target="_blank"> possible page load strategies </a>
     * and <a href="https://w3c.github.io/webdriver/#dfn-table-of-page-load-strategies" target="_blank">corresponding document readiness</a>
     */
    public BasicActionRunner(PageLoadStrategy pageLoadStrategy, BrowserType browserType, int maxConcurrentComposer){
        this.browserActionExecutor = new PausablePriorityThreadPoolExecutor("BrowserActionExecutorPool", 1, 1);
        this.composerExecutor = new PausablePriorityThreadPoolExecutor("ActionComposerExecutorPool", maxConcurrentComposer, maxConcurrentComposer);

        this.brsDriver = createBrowserDriver(browserType, pageLoadStrategy);
        
        this.brsDriver.manage().timeouts().implicitlyWait(1, TimeUnit.MILLISECONDS);
        this.rootWindowIdentity = this.brsDriver.getWindowHandle();
    }

    private static WebDriver createBrowserDriver(BrowserType browserType, PageLoadStrategy pageLoadStrategy){
        Capabilities extraCapabilities = new ImmutableCapabilities(CapabilityType.PAGE_LOAD_STRATEGY, pageLoadStrategy.toString().toLowerCase());
        switch(browserType){
            case CHROME:
                ChromeOptions chromeOption = new ChromeOptions();
                if ("no".equalsIgnoreCase(System.getProperty("chrome_sandbox"))){
                    chromeOption.addArguments("--no-sandbox");
                }
                if ("yes".equalsIgnoreCase(System.getProperty("webdriver_headless"))){
                    chromeOption.setHeadless(true);
                    LOGGER.info("headless chrome used");
                }
                if ("yes".equalsIgnoreCase(System.getProperty("webdriver_use_default_user_data_dir"))){
                    String defaultUserDataDir = getDefaultUserDataDir(browserType);
                    if (defaultUserDataDir!=null){
                        chromeOption.addArguments("user-data-dir="+defaultUserDataDir);
                        LOGGER.info("default user data dir used:{}", defaultUserDataDir);
                    }
                }
                return new ChromeDriver(chromeOption.merge(extraCapabilities));
            case FIREFOX:
            default:
                FirefoxOptions firefoxOption = new FirefoxOptions();
                if ("yes".equalsIgnoreCase(System.getProperty("webdriver_headless"))){
                    firefoxOption.setHeadless(true);
                    LOGGER.info("headless firefox used");
                }
                if ("yes".equalsIgnoreCase(System.getProperty("webdriver_use_default_user_data_dir"))){
                    String defaultProfileDir = getDefaultUserDataDir(browserType);
                    if (defaultProfileDir!=null){
                        firefoxOption.setProfile(new FirefoxProfile(new File(defaultProfileDir)));
                        LOGGER.info("default user profile dir used:{}", defaultProfileDir);
                    }
                }
                return new FirefoxDriver(firefoxOption.merge(extraCapabilities));
        }
    }
    
    private static String getDefaultUserDataDir(BrowserType browserType){
        boolean isWindows = Optional.ofNullable(System.getProperty("os.name")).orElse("").toLowerCase(Locale.ENGLISH).startsWith("windows");
        String path;
        switch(browserType){
            case CHROME:
                if (isWindows){
                    path = System.getenv("LOCALAPPDATA")+"\\Google\\Chrome\\User Data";
                    if (!new File(path).isDirectory()) path = System.getenv("LOCALAPPDATA")+"\\Chromium\\User Data";
                }
                else{
                    path = "~/.config/google-chrome";
                    if (!new File(path).isDirectory()) path = "~/.config/chromium";
                }
                if (!new File(path).isDirectory()) path = null;
                break;
            case FIREFOX:
                if (isWindows){
                    path = System.getenv("APPDATA")+"\\Mozilla\\Firefox\\Profiles";
                }
                else{
                    path = "~/.mozilla/firefox";
                }
                File pathFile = new File(path);
                if (!pathFile.isDirectory()) path = null;
                else{
                    String[] dirs = pathFile.list((current, name) -> new File(current, name).isDirectory() && name.toLowerCase().endsWith(".default"));
                    if (dirs!=null && dirs.length>0) path = new File(pathFile, dirs[0]).getAbsolutePath();
                    else path = null;
                }
                break;
            default:
                throw new UnsupportedOperationException("Unsupported browser type:"+browserType.toString());
        }
        return path;
    }
    
    @Override
    public String getName(){
        return name;
    }
    
    @Override
    public BasicActionRunner setName(String name){
        this.name = name;
        return this;
    }
    
    @Override
    public String getRootWindowIdentity() {
        return rootWindowIdentity;
    }
    
    @Override
    public CompletableFuture<Void> executeAction(Runnable browserAction, int priority){
        CompletableFuture<Void> cFuture = new CompletableFuture<>();
        PriorityRunnable runnable = new PriorityRunnable(()->{
            try{
                browserAction.run();
                cFuture.complete(null);
            }catch(Exception ex){
                cFuture.completeExceptionally(ex);
            }
        }, priority);
        browserActionExecutor.submit(runnable);
        return cFuture;
    }
    
    @Override
    public CompletableFuture<Void> executeComposer(ActionComposer actionComposer){
        actionComposer.setActionRunner(this);
        CompletableFuture<Void> cFuture = new CompletableFuture<>();
        PriorityRunnable runnable = new PriorityRunnable(()->{
            try{
                actionComposer.run();
                cFuture.complete(null);
            }catch(Exception ex){
                cFuture.completeExceptionally(ex);
            }
        }, actionComposer.getPriority());
        composerExecutor.submit(runnable);
        return cFuture;
    }
    
    @Override
    public WebDriver getWebDriver(){
        return brsDriver;
    }
    
    /**
     * This method checks the existence of root window and use the result as the result of browser's aliveness.
     * When a negative result is acquired, users can choose to {@link #close() close} {@link ActionRunner} and create a new one.
     * 
     * @return {@code true} if the root window exists; {@code false} otherwise
     */
    @Override
    public boolean isBrowserAlive(){
        if (brsDriver==null) return false;
        
        try{
            Set<String> windowSet = brsDriver.getWindowHandles();
            return (windowSet!=null && !windowSet.isEmpty() && windowSet.contains(getRootWindowIdentity()));
        }catch(Exception ex){
            LOGGER.warn("[{}] check browser alive error!", name, ex);
            return false;
        }
    }
    
    /**
     * Stop executing queued {@link ActionComposer} or browser action, but the running ones will keep running.
     */
    @Override
    public void pause() {
        if (isPaused) return;
        
        isPaused = true;
        browserActionExecutor.pause();
        composerExecutor.pause();
    }
    
    /**
     * Resume to executing queued {@link ActionComposer} or browser action (if any).
     */
    @Override
    public void resume() {
        if (!isPaused) return;
        
        isPaused = false;
        browserActionExecutor.resume();
        composerExecutor.resume();
    }
    
    @Override
    public boolean isPaused(){
        return isPaused;
    }
    
    @Override
    public void close(){
        try{
            if (browserActionExecutor!=null){
                browserActionExecutor.shutdownNow();
                LOGGER.info("[{}] Close BrowserActionExecutor done!", name);
            }
            
            if (composerExecutor!=null){
                composerExecutor.shutdown();
                LOGGER.info("[{}] Close ActionComposerExecutor done!", name);
            }

            if (brsDriver!=null){
                brsDriver.quit();
                LOGGER.info("[{}] Close BrsDriver done!", name);
            }
        }catch(Exception ex){
            LOGGER.error("[{}] Close {} fail!", name, getClass().getSimpleName(), ex);
        }
    }
}
