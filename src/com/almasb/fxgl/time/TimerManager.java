/*
 * The MIT License (MIT)
 *
 * FXGL - JavaFX Game Library
 *
 * Copyright (c) 2015 AlmasB (almaslvl@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.almasb.fxgl.time;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.almasb.fxgl.FXGLManager;
import com.almasb.fxgl.GameApplication;
import com.almasb.fxgl.time.TimerAction.TimerType;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;

public final class TimerManager extends FXGLManager {

    /**
     * List for all timer based actions
     */
    private List<TimerAction> timerActions = new CopyOnWriteArrayList<>();

    /**
     * These are used to approximate FPS value
     */
    private FPSCounter fpsCounter = new FPSCounter();
    private FPSCounter fpsPerformanceCounter = new FPSCounter();

    /**
     * Used as delta from internal JavaFX timestamp to calculate render FPS
     */
    private long fpsTime = 0;

    /**
     * Average render FPS
     */
    private IntegerProperty fps = new SimpleIntegerProperty();

    /**
     *
     * @return average render FPS
     */
    public int getFPS() {
        return fps.get();
    }

    /**
     *
     * @return average render FPS property
     */
    public IntegerProperty fpsProperty() {
        return fps;
    }

    /**
     * Average performance FPS
     */
    private IntegerProperty performanceFPS = new SimpleIntegerProperty();

    /**
     *
     * @return Average performance FPS
     */
    public int getPerformanceFPS() {
        return performanceFPS.get();
    }

    /**
     *
     * @return Average performance FPS property
     */
    public IntegerProperty performanceFPSProperty() {
        return performanceFPS;
    }

    private long startNanos = -1;
    private long realFPS = -1;

    /**
     * Called at the start of a game update tick.
     *
     * @param internalTime
     */
    public void tickStart(long internalTime) {
        startNanos = System.nanoTime();
        realFPS = internalTime - fpsTime;
        fpsTime = internalTime;
    }

    /**
     * Called at the end of a game update tick.
     */
    public void tickEnd() {
        performanceFPS.set(Math.round(fpsPerformanceCounter.count(GameApplication.SECOND / (System.nanoTime() - startNanos))));
        fps.set(Math.round(fpsCounter.count(GameApplication.SECOND / realFPS)));
    }

    @Override
    protected void onUpdate(long now) {
        timerActions.forEach(action -> action.update(now));
        timerActions.removeIf(TimerAction::isExpired);
    }

    /**
     * The Runnable action will be scheduled to run at given interval.
     * The action will run for the first time after given interval.
     *
     * Note: the scheduled action will not run while the game is paused.
     *
     * @param action the action
     * @param interval time in nanoseconds
     */
    public void runAtInterval(Runnable action, double interval) {
        timerActions.add(new TimerAction(app.getNow(), interval, action, TimerType.INDEFINITE));
    }

    /**
     * The Runnable action will be scheduled for execution iff
     * whileCondition is initially true. If that's the case
     * then the Runnable action will be scheduled to run at given interval.
     * The action will run for the first time after given interval
     *
     * The action will be removed from schedule when whileCondition becomes {@code false}.
     *
     * Note: the scheduled action will not run while the game is paused
     *
     * @param action
     * @param interval
     * @param whileCondition
     */
    public void runAtIntervalWhile(Runnable action, double interval, ReadOnlyBooleanProperty whileCondition) {
        if (!whileCondition.get()) {
            return;
        }
        TimerAction act = new TimerAction(app.getNow(), interval, action, TimerType.INDEFINITE);
        timerActions.add(act);

        whileCondition.addListener((obs, old, newValue) -> {
            if (!newValue.booleanValue())
                act.expire();
        });
    }

    /**
     * The Runnable action will be executed once after given delay
     *
     * Note: the scheduled action will not run while the game is paused
     *
     * @param action
     * @param delay
     */
    public void runOnceAfter(Runnable action, double delay) {
        timerActions.add(new TimerAction(app.getNow(), delay, action, TimerType.ONCE));
    }

    /**
     * Clears all registered timer based actions.
     */
    public void clearActions() {
        timerActions.clear();
    }
}
