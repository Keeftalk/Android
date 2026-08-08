package com.keeftalk.chat.macrobenchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() = baselineProfileRule.collect(
        packageName = "com.keeftalk.chat",
        includeInStartupProfile = true
    ) {
        pressHome()
        startActivityAndWait()
        
        // Add flows to generate baseline profile for
        // e.g., scroll the chat list
    }
}
