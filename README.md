# Auto-browser [![Continuous Integration](https://github.com/kquiet/auto-browser/actions/workflows/continuous-integration.yml/badge.svg?branch=dev)](https://github.com/kquiet/auto-browser/actions/workflows/continuous-integration.yml) [![Continuous Delievery - staging](https://github.com/kquiet/auto-browser/actions/workflows/continuous-delivery-staging.yml/badge.svg)](https://github.com/kquiet/auto-browser/actions/workflows/continuous-delivery-staging.yml)
Auto-browser is a java library which wraps [selenium][] to help focus on
constructing and executing scripts of browser actions in applications.

Using [selenium][], you can manipulate the browser to perform almost any action
as you usually do with a mouse and a keyboard, e.g., log in to an e-commerce site
and place orders.

However, webdriver
[is not thread-safe](https://www.selenium.dev/documentation/legacy/selenium_2/faq/#q-is-webdriver-thread-safe)
, so there is still a lot of cumbersome work to do when you want to:

- execute multiple scripts of browser actions concurrently against a webdriver
instance without breaking each other(serializing access)
- avoid blocking the other script of browser actions while performing wait alike
actions on a webdriver instance

Auto-browser handles these internally and provides a fluent way to construct a
script of browser actions as an object, enabling cooperation of scripts.

## Supported browser
Currently only Chrome/Chromium/Firefox are supported because it should be enough
to automate most of web pages. The other browsers(except IE) may be supported in
the future.

## Getting Started
Add below to [maven][]'s `pom.xml`:
```xml
<dependency>
  <groupId>org.kquiet</groupId>
  <artifactId>auto-browser</artifactId>
  <version>X.X.X</version>
</dependency>
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
  .onFail(ac -> System.out.println("called when an exception is thrown or the script is marked as failed"))
  .onDone(ac -> System.out.println("always called after all browser actions and callbacks"));
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
'webdriver.chrome.driver' or 'webdriver.gecko.driver' in java command to run, e.g.,
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
    try (ActionRunner actionRunner = new BasicActionRunner()) {
      ActionComposer actionComposer = new ActionComposerBuilder()
        .prepareActionSequence()
          .getUrl("https://github.com/kquiet/auto-browser/find/master")
          .waitUntil(elementToBeClickable(By.id("tree-finder-field")), 3000)
          .sendKey(By.id("tree-finder-field"), "ActionComposerBuilder")
          .waitUntil(elementToBeClickable(By.xpath("//mark[text()='ActionComposerBuilder']")), 3000)
          .click(By.xpath("//mark[text()='ActionComposerBuilder']"))
          .returnToComposerBuilder()
        .buildBasic().setCloseWindow(false)
        .onFail(ac -> System.out.println("called when an exception is thrown or is marked as failed"))
        .onDone(ac -> System.out.println("always called after all browser actions and callbacks"));
      actionRunner.executeComposer(actionComposer).get();
    }
  }
}
```

Please refer to the [api doc](https://kquiet.github.io/auto-browser/) for
details.

## Q&A
1. How to use firefox as the browser instead of chrome?  
=> You can use other [constructors](https://kquiet.github.io/auto-browser/org/kquiet/browser/BasicActionRunner.html)
of `BasicActionRunner` to specify the browser type.

2. How to perform conditional browser actions in a script?  
=> Method [prepareIfThenElse()](https://kquiet.github.io/auto-browser/org/kquiet/browser/ActionComposerBuilder.ActionSequenceBuilder.html#prepareIfThenElse-java.util.function.Function-)
in `ActionComposerBuilder.ActionSequenceBuilder` is designed to perform such actions, please use it accordingly.
Or use another method [custom()](https://kquiet.github.io/auto-browser/org/kquiet/browser/ActionComposerBuilder.ActionSequenceBuilder.html#custom-java.util.function.Consumer-)
which takes a *Consumer* as its parameter allowing you to script all the customized logic inside.

[maven]: https://maven.apache.org/ "maven official website"
[selenium]: https://github.com/SeleniumHQ/selenium "selenium in github"
[chromedriver]: http://chromedriver.chromium.org/downloads "download chrome/chromium driver executable"
[geckodriver]: https://github.com/mozilla/geckodriver/releases "download firefox driver executable"
