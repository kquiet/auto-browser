# Auto-browser [![Travis CI build status](https://travis-ci.org/kquiet/auto-browser.svg?branch=master)](https://travis-ci.org/kquiet/auto-browser)
Auto-browser is a java library which wraps [selenium][] to help focus on
constructing and executing scripts of browser actions in applications.

Using [selenium][], you can manipulate the browser to perform almost any action
as you usually do with a mouse and a keyboard, e.g., log in to an ecommerce site
and place orders.

However, webdriver
[is not thread-safe](https://github.com/SeleniumHQ/selenium/wiki/Frequently-Asked-Questions#user-content-q-is-webdriver-thread-safe)
, so there is still a lot of cumbersome work to do when you want to:

- execute multiple scripts of browser actions synchronizedly on a webdriver
instance without breaking each other
- avoid blocking the other script of browser actions while performing wait-like
actions on a webdriver instance

Auto-browser handles these internally and provides a fluent way to construct a
script of browser actions as an object, enabling custom scheduling of scripts.

## Supported browser
Currently only Chrome/Chromium/Firefox are supported because it should be enough
to automate most of web pages. The other browsers(except IE) may be supported in
the future.

## Getting Started
If you are using [maven][], add below to `pom.xml`:
```xml
<dependency>
  <groupId>org.kquiet</groupId>
  <artifactId>auto-browser</artifactId>
  <version>X.X.X</version>
</dependency>
```

If you are using [gradle][], add below to `build.gradle`:
```groovy
dependencies {
    implementation 'org.kquiet:auto-browser:X.X.X'
}
```

Java version 1.8+ is required.

## Sample Usage
1. Construct a script of browser actions:
```java
// Search for the link to the source code of ActionComposerBuilder.java, and then click it
ActionComposer actionComposer = new ActionComposerBuilder()
  .prepareActionSequence()
    .getUrl("https://github.com/kquiet/auto-browser/find/master")
    .waitUntil(elementToBeClickable(By.id("tree-finder-field")), 3000)
    .sendKey(By.id("tree-finder-field"), "ActionComposerBuilder")
    .waitUntil(elementToBeClickable(By.xpath("//mark[text()='ActionComposerBuilder']")), 3000)
    .click(By.xpath("//mark[text()='ActionComposerBuilder']"))
    .returnToComposerBuilder()
  .buildBasic().setCloseWindow(false)
  .onFail(ac->System.out.println("called when an exception is thrown or the script is marked as failed"))
  .onDone(ac->System.out.println("always called after all browser actions and callbacks"));
```
2. Start a browser:
```java
//ActionRunner encapsulates a webdriver instance and synchronizes all browser actions on it
ActionRunner actionRunner = new BasicActionRunner();
```
3. Execute the script:
```java
actionRunner.executeComposer(actionComposer);
```
Remember to download the driver executable and set its path by system property
in java command to run, e.g.,
`java -Dwebdriver.chrome.driver=<path to chromedriver>`.
- [driver executable for chrome/chromium][chromedriver]
- [driver executable for firefox][geckodriver]

### Full sample code:
```java
import org.kquiet.browser.*;
import org.openqa.selenium.By;
import static org.openqa.selenium.support.ui.ExpectedConditions.*;

public class Sample {    
  public static void main(String args[]) throws Exception{
    try(ActionRunner actionRunner = new BasicActionRunner()){
      ActionComposer actionComposer = new ActionComposerBuilder()
        .prepareActionSequence()
          .getUrl("https://github.com/kquiet/auto-browser/find/master")
          .waitUntil(elementToBeClickable(By.id("tree-finder-field")), 3000)
          .sendKey(By.id("tree-finder-field"), "ActionComposerBuilder")
          .waitUntil(elementToBeClickable(By.xpath("//mark[text()='ActionComposerBuilder']")), 3000)
          .click(By.xpath("//mark[text()='ActionComposerBuilder']"))
          .returnToComposerBuilder()
        .buildBasic().setCloseWindow(false)
        .onFail(ac->System.out.println("called when an exception is thrown or is marked as failed"))
        .onDone(ac->System.out.println("always called after all browser actions and callbacks"));
      actionRunner.executeComposer(actionComposer).get();
    }
  }
}
```

Please refer to the [api doc](https://kquiet.github.io/auto-browser/) if you
want to know more api detail.

## Q&A
1. How to use firefox as the browser instead of chrome?  
=> You can use other [constructor](https://kquiet.github.io/auto-browser/org/kquiet/browser/BasicActionRunner.html)
of `BasicActionRuner` to specify browser type.

2. How to perform conditional browser actions in a script?  
=> You can use the [prepareIfThenElse()](https://kquiet.github.io/auto-browser/org/kquiet/browser/ActionComposerBuilder.ActionSequenceBuilder.html#prepareIfThenElse-java.util.function.Function-)
method in `ActionComposerBuilder.ActionSequenceBuilder` to perform conditional browser
actions.

## License (Apache License Version 2.0)
Copyright 2018-2019 kquiet.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

<http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

[gradle]: https://gradle.org/ "gradle official website"
[maven]: https://maven.apache.org/ "maven official website"
[selenium]: https://github.com/SeleniumHQ/selenium "selenium in github"
[chromedriver]: http://chromedriver.chromium.org/downloads "download chrome/chromium driver executable"
[geckodriver]: https://github.com/mozilla/geckodriver/releases "download firefox driver executable"
