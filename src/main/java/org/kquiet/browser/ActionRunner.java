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

import java.io.Closeable;
import java.io.File;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;
import java.util.Optional;
import java.util.concurrent.Future;

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

import org.kquiet.concurrent.PriorityCallable;
import org.kquiet.concurrent.PausablePriorityThreadPoolExecutor;

/**
 *
 * @author Kimberly
 */
public class ActionRunner implements Closeable,AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActionRunner.class);
    
    private final String name;
    private final WebDriver brsDriver;
    private final String rootWindowIdentity;
    
    private volatile boolean isPaused = false;
    private volatile CountDownLatch pauseLatch = new CountDownLatch(1);
    
    private final PausablePriorityThreadPoolExecutor browserActionExecutor;
    private final PausablePriorityThreadPoolExecutor composerExecutor;
    
    /**
     *
     * @param name
     * @param maxConcurrentComposer
     */
    public ActionRunner(String name, int maxConcurrentComposer){
        this(PageLoadStrategy.NONE, BrowserType.CHROME, name, maxConcurrentComposer);
    }
    
    /**
     *
     * @param pageLoadStrategy
     * @param browserType
     * @param name
     * @param maxConcurrentComposer
     */
    public ActionRunner(PageLoadStrategy pageLoadStrategy, BrowserType browserType, String name, int maxConcurrentComposer){
        this.name = name;
        
        browserActionExecutor = new PausablePriorityThreadPoolExecutor("BrowserActionExecutorPool", 1, 1);
        composerExecutor = new PausablePriorityThreadPoolExecutor("ActionComposerExecutorPool", maxConcurrentComposer, maxConcurrentComposer);

        //create browser
        this.brsDriver = createBrowserDriver(browserType, pageLoadStrategy);
        
        //this.brsDriver.manage().window().maximize();
        this.brsDriver.manage().timeouts().implicitlyWait(1, TimeUnit.MILLISECONDS);
        this.rootWindowIdentity = this.brsDriver.getWindowHandle();
    }
    
    /**
     *
     * @return
     */
    public String getRootWindowIdentity() {
        return rootWindowIdentity;
    }
    
    /**
     *
     * @param browserType
     * @param pageLoadStrategy
     * @return
     */
    public static WebDriver createBrowserDriver(BrowserType browserType, PageLoadStrategy pageLoadStrategy){
        Capabilities extraCapabilities = new ImmutableCapabilities(CapabilityType.PAGE_LOAD_STRATEGY, pageLoadStrategy.toString().toLowerCase());
        switch(browserType){
            case CHROME:
                ChromeOptions chromeOption = new ChromeOptions();
                if ("no".equalsIgnoreCase(System.getenv("chrome_sandbox"))){
                    chromeOption.addArguments("--no-sandbox");
                }
                if ("yes".equalsIgnoreCase(System.getenv("webdriver_headless"))){
                    chromeOption.setHeadless(true);
                    LOGGER.info("headless chrome used");
                }
                if ("yes".equalsIgnoreCase(System.getenv("webdriver_use_default_user_data_dir"))){
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
                if ("yes".equalsIgnoreCase(System.getenv("webdriver_headless"))){
                    firefoxOption.setHeadless(true);
                    LOGGER.info("headless firefox used");
                }
                if ("yes".equalsIgnoreCase(System.getenv("webdriver_use_default_user_data_dir"))){
                    String defaultUserDataDir2 = getDefaultUserDataDir(browserType);
                    if (defaultUserDataDir2!=null){
                        firefoxOption.setProfile(new FirefoxProfile(new File(defaultUserDataDir2)));
                        LOGGER.info("default user profile dir used:{}", defaultUserDataDir2);
                    }
                }
                return new FirefoxDriver(firefoxOption.merge(extraCapabilities));
        }
    }
    
    /**
     *
     * @param browserType
     * @return
     */
    public static String getDefaultUserDataDir(BrowserType browserType){
        boolean isWindows = Optional.ofNullable(System.getProperty("os.name")).orElse("").toLowerCase().startsWith("windows");
        String path = null;
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
            default:
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
        }
        return path;
    }
    
    /**
     *
     * @param runnable
     * @param priority
     * @return
     */
    public Future<Exception> executeAction(Runnable runnable, int priority){
        PriorityCallable<Exception> callable = new PriorityCallable<>(()->{
            try{
                runnable.run();
                return null;
            }catch(Exception ex){
                return ex;
            }
        }, priority);
        return browserActionExecutor.submit(callable);
    }
    
    /**
     *
     * @param actionComposer
     * @return
     */
    public Future<?> executeComposer(ActionComposer actionComposer){
        actionComposer.setActionRunner(this);
        return composerExecutor.submit(actionComposer);
    }
    
    /**
     *
     * @return
     */
    public WebDriver getWebDriver(){
        return brsDriver;
    }
    
    /**
     *
     * @return
     */
    public boolean isAlive(){
        if (brsDriver==null) return false;

        Set<String> windowSet = brsDriver.getWindowHandles();
        return (windowSet!=null && windowSet.size()>0 && windowSet.contains(getRootWindowIdentity()));
    }
    
    /**
     *
     */
    public synchronized void pause() {
        try{
            browserActionExecutor.pause();
            composerExecutor.pause();
        }finally{
            isPaused = true;
        }
    }
    
    /**
     *
     */
    public synchronized void resume() {
        try{
            browserActionExecutor.resume();
            composerExecutor.resume();
        }finally{
            pauseLatch.countDown();
            pauseLatch = new CountDownLatch(1);
            isPaused = false;
        }
    }
    
    /**
     *
     * @return
     */
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
