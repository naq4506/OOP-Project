package com.example.server.service.collector;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;

public abstract class BaseSeleniumCollector implements Collector {

    protected WebDriver driver;
    protected JavascriptExecutor js;
    protected WebDriverWait wait;

    protected void initDriver() {
        if (this.driver != null) {
            return; 
        }

        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("debuggerAddress", "127.0.0.1:9222"); 
        
        this.driver = new ChromeDriver(options);
        this.driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        this.js = (JavascriptExecutor) driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        System.out.println(">>> [BaseCollector] Driver initialized.");
    }
    
    
    protected void closeDriver() {
    }

    protected void sleep(long millis) {
        try { Thread.sleep(millis); } catch (InterruptedException e) {}
    }

    protected void scrollDown(int pixels) {
        if (js != null) {
            js.executeScript("window.scrollBy(0, " + pixels + ")");
            sleep(1500);
        }
    }
}