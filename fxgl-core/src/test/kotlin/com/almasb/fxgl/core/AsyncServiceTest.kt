/*
 * FXGL - JavaFX Game Library. The MIT License (MIT).
 * Copyright (c) AlmasB (almaslvl@gmail.com).
 * See LICENSE for details.
 */
@file:Suppress("JAVA_MODULE_DOES_NOT_DEPEND_ON_MODULE")
package com.almasb.fxgl.core

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.greaterThan
import org.hamcrest.Matchers.lessThan
import org.hamcrest.Matchers.both
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.system.measureTimeMillis

/**
 *
 * @author Jean-Rene Lavoie (jeanrlavoie@gmail.com)
 */
class AsyncServiceTest {

    @Test
    fun `Async Service with Unit (Kotlin Void)`() {
        val service = object : AsyncService<Unit>() {
            override fun onGameUpdateAsync(tpf: Double) {
                Thread.sleep(100)  // Processing takes more time than a normal tick
            }
        }

        // On first call, it'll launch the async process and continue the game loop without affecting the tick
        // If it takes less than 5 millis, it's running async
        assertThat(measureTimeMillis { service.onGameUpdate(1.0) }.toDouble(), lessThan(7.0))

        // On the second call, it must wait until the first call is resolved before calling it again (to prevent major desync)
        // We expect it to take more than 80 millis
        assertThat(measureTimeMillis { service.onGameUpdate(1.0) }.toDouble(), greaterThan(80.0))
    }

    @Test
    fun `Async Service with T (String)`() {
        var postUpdateValue = ""
        val service = object : AsyncService<String>() {
            override fun onGameUpdateAsync(tpf: Double): String {
                return "Done"
            }

            override fun onPostGameUpdateAsync(result: String) {
                postUpdateValue = result
            }
        }

        // On first call, we don't have the postUpdateValue yet
        service.onGameUpdate(1.0)
        assertEquals(postUpdateValue, "")

        // On second update, we updated the postUpdateValue
        service.onGameUpdate(1.0)
        assertEquals(postUpdateValue, "Done")
    }

    @Test
    fun `Async Service parallel`() {
        val services = listOf(
            object : AsyncService<Unit>() {
                override fun onGameUpdateAsync(tpf: Double) {
                    Thread.sleep(100)  // Processing takes more time than a normal tick
                }
            },
            object : AsyncService<Unit>() {
                override fun onGameUpdateAsync(tpf: Double) {
                    Thread.sleep(100)  // Processing takes more time than a normal tick
                }
            },
            object : AsyncService<Unit>() {
                override fun onGameUpdateAsync(tpf: Double) {
                    Thread.sleep(100)  // Processing takes more time than a normal tick
                }
            }
        )

        // 3 services started in parallel without additional latency
        assertThat(measureTimeMillis {
            services.forEach { service ->
                service.onGameUpdate(1.0)
            }
        }.toDouble(), lessThan(7.0))

        // 3 services resolved in approximately 1/3 of what it would take if they were sequentially resolved
        assertThat(measureTimeMillis {
            services.forEach { service ->
                service.onGameUpdate(1.0)
            }
        }.toDouble(), `is`(both(greaterThan(80.0)).and(lessThan(120.0))))
    }

}