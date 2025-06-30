package com.iceiony.visualcalendar.testutil

import android.view.View
import android.view.ViewGroup

object TestHelper {
    fun getAllViews(root: View): List<View> {
        val result = mutableListOf<View>()
        val queue = ArrayDeque<View>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val view = queue.removeFirst()
            result.add(view)
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    queue.add(view.getChildAt(i))
                }
            }
        }
        return result
    }

}