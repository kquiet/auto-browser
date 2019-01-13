/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.kquiet.utility;

import java.time.Duration;

/**
 * Stopwatch.
 * 
 * @author Kimberly
 */
public class Stopwatch {
    private volatile long startTime = 0;
    private volatile long stopTime = 0;
    private volatile boolean running = false;

    /**
     * Start this {@link Stopwatch} without reset.
     */
    public void start() {
        this.start(false);
    }
    
    /**
     * Start this {@link Stopwatch} with the option to reset.
     * 
     * @param resetFlag {@code true}: reset; {@code false}: not reset
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
     * Stop this {@link Stopwatch}
     */
    public void stop() {
        if (this.running){
            this.stopTime = System.nanoTime();
            this.running = false;
        }
    }

    /**
     *
     * @return elapsed time in millisecond
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

    /**
     *
     * @return elapsed time in nanosecond
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
     * @return elapsed time
     */
    public Duration getDuration(){
        return Duration.ofNanos(getElapsedNanoSecond());
    }
}
