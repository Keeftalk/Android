package com.keeftalk.chat.util

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager

/**
 * Automatically profiles Activity and Fragment lifecycle events.
 */
class PerformanceLifecycleCallbacks : Application.ActivityLifecycleCallbacks {
    
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        PerformanceProfiler.startStage("${activity.javaClass.simpleName}.onCreate")
        
        if (activity is FragmentActivity) {
            activity.supportFragmentManager.registerFragmentLifecycleCallbacks(object : FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentCreated(fm: FragmentManager, f: Fragment, savedInstanceState: Bundle?) {
                    PerformanceProfiler.startStage("${f.javaClass.simpleName}.onCreate")
                }
                
                override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
                    PerformanceProfiler.endStage("${f.javaClass.simpleName}.onCreate", category = PerformanceProfiler.Category.ANDROID)
                    PerformanceProfiler.logEvent("${f.javaClass.simpleName}.onResume", category = PerformanceProfiler.Category.ANDROID)
                }

                override fun onFragmentPaused(fm: FragmentManager, f: Fragment) {
                    PerformanceProfiler.logEvent("${f.javaClass.simpleName}.onPause", category = PerformanceProfiler.Category.ANDROID)
                }
            }, true)
        }
    }

    override fun onActivityStarted(activity: Activity) {
        PerformanceProfiler.logEvent("${activity.javaClass.simpleName}.onStart", category = PerformanceProfiler.Category.ANDROID)
    }

    override fun onActivityResumed(activity: Activity) {
        PerformanceProfiler.endStage("${activity.javaClass.simpleName}.onCreate", category = PerformanceProfiler.Category.ANDROID)
        PerformanceProfiler.logEvent("${activity.javaClass.simpleName}.onResume", category = PerformanceProfiler.Category.ANDROID)
    }

    override fun onActivityPaused(activity: Activity) {
        PerformanceProfiler.logEvent("${activity.javaClass.simpleName}.onPause", category = PerformanceProfiler.Category.ANDROID)
    }

    override fun onActivityStopped(activity: Activity) {
        PerformanceProfiler.logEvent("${activity.javaClass.simpleName}.onStop", category = PerformanceProfiler.Category.ANDROID)
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {
        PerformanceProfiler.logEvent("${activity.javaClass.simpleName}.onDestroy", category = PerformanceProfiler.Category.ANDROID)
    }
}
