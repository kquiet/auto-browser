/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.kquiet.utility;

import java.time.Duration;

/**
 *
 * @author Kimberly
 */
public class StopWatch {
    private volatile long startTime = 0;
    private volatile long stopTime = 0;
    private volatile boolean running = false;

    /**
     *
     */
    public void start() {
        this.start(false);
    }
    
    /**
     *
     * @param resetFlag
     */
    public void start(boolean resetFlag) {
        if (resetFlag){
            startTime = 0;
            stopTime = 0;
        }
        if (startTime==0) this.startTime = System.nanoTime();
        this.running = true;
    }

    /**
     *
     */
    public void stop() {
        if (this.running){
            this.stopTime = System.nanoTime();
            this.running = false;
        }
    }

    //elaspsed time in milliseconds

    /**
     *
     * @return
     */
    public long getElapsedMilliSecond() {
        long elapsed;
        if (running) {
            elapsed = (System.nanoTime() - startTime)/1000000;
        } else {
            elapsed = (stopTime - startTime)/1000000;
        }
        return elapsed;
    }
    
    //elaspsed time in nanoseconds

    /**
     *
     * @return
     */
    public long getElapsedNanoSecond() {
        long elapsed;
        if (running) {
            elapsed = (System.nanoTime() - startTime);
        } else {
            elapsed = (stopTime - startTime);
        }
        return elapsed;
    }
    
    /**
     *
     * @return
     */
    public Duration getDuration(){
        return Duration.ofNanos(getElapsedNanoSecond());
    }
}
