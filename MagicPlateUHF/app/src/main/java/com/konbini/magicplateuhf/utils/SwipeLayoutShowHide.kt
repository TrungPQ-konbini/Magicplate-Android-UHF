package com.konbini.magicplateuhf.utils

import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.TranslateAnimation

class SwipeLayoutShowHide : OnTouchListener {
    enum class SwipeDirection {
        topToBottom, bottomToTop, leftToRight, rightToLeft
    }

    private var rootLayout: ViewGroup? = null
    private var layoutToShowHide: ViewGroup? = null
    private var gestureDetector: GestureDetector? = null
    private var swipeDirections: MutableList<SwipeDirection>? = null

    fun initialize(
        rootLayout: ViewGroup,
        layoutToShowHide: ViewGroup?,
        swipeDirections: MutableList<SwipeDirection>,
        maxSwipeDistance: Int = 1
    ) {
        val gestureListener = GestureListener()
        gestureDetector = GestureDetector(rootLayout.context, gestureListener)
        this.rootLayout = rootLayout
        this.layoutToShowHide = layoutToShowHide
        this.swipeDirections = swipeDirections
        gestureListener.MAX_SWIPE_DISTANCE = maxSwipeDistance
        this.rootLayout!!.setOnTouchListener(this)
    }

    fun cancel() {
        rootLayout!!.setOnTouchListener(null)
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        return gestureDetector!!.onTouchEvent(event)
    }

    inner class GestureListener : SimpleOnGestureListener() {
        var MAX_SWIPE_DISTANCE = 1
        private val SWIPE_VELOCITY_THRESHOLD = 1

        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onFling(
            e1: MotionEvent,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            var result = false
            try {
                val diffY = e2.y - e1.y
                val diffX = e2.x - e1.x
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > MAX_SWIPE_DISTANCE && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            onSwipeLeftToRight()
                        } else {
                            onSwipeRightToLeft()
                        }
                    }
                    result = true
                } else if (Math.abs(diffY) > MAX_SWIPE_DISTANCE && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {
                        onSwipeTopToBottom()
                    } else {
                        onSwipeBottomToTop()
                    }
                }
                result = true
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
            return result
        }
    }

    fun onSwipeLeftToRight() {
        val isVisible = layoutToShowHide!!.visibility == View.VISIBLE
        if (swipeDirections!!.contains(SwipeDirection.leftToRight) && isVisible || swipeDirections!!.contains(
                SwipeDirection.rightToLeft
            ) && !isVisible
        )
            toggleViewVisibilityWithAnimation(SwipeDirection.leftToRight)
    }

    fun onSwipeRightToLeft() {
        val isVisible =
            layoutToShowHide!!.visibility == View.VISIBLE
        if (swipeDirections!!.contains(SwipeDirection.rightToLeft) && isVisible || swipeDirections!!.contains(
                SwipeDirection.leftToRight
            ) && !isVisible
        )
            toggleViewVisibilityWithAnimation(SwipeDirection.rightToLeft)
    }

    fun onSwipeBottomToTop() {
        val isVisible =
            layoutToShowHide!!.visibility == View.VISIBLE
        if (swipeDirections!!.contains(SwipeDirection.bottomToTop) && isVisible || swipeDirections!!.contains(
                SwipeDirection.topToBottom
            ) && !isVisible
        )
            toggleViewVisibilityWithAnimation(SwipeDirection.bottomToTop)
    }

    fun onSwipeTopToBottom() {
        val isVisible =
            layoutToShowHide!!.visibility == View.VISIBLE
        if (swipeDirections!!.contains(SwipeDirection.topToBottom) && isVisible || swipeDirections!!.contains(
                SwipeDirection.bottomToTop
            ) && !isVisible
        )
            toggleViewVisibilityWithAnimation(SwipeDirection.topToBottom)
    }

    private fun toggleViewVisibilityWithAnimation(swipeDirection: SwipeDirection) {
        val currentVisibility = layoutToShowHide!!.visibility
        var deltaVal =
            if (swipeDirection == SwipeDirection.leftToRight || swipeDirection == SwipeDirection.topToBottom) 1000 else -1000
        if (currentVisibility == View.GONE) {
            deltaVal = -deltaVal
        }
        val fromXDelta =
            if (currentVisibility == View.VISIBLE || swipeDirection == SwipeDirection.topToBottom || swipeDirection == SwipeDirection.bottomToTop) 0 else deltaVal
        val toXDelta =
            if (currentVisibility == View.GONE || swipeDirection == SwipeDirection.topToBottom || swipeDirection == SwipeDirection.bottomToTop) 0 else deltaVal
        val fromYDelta =
            if (currentVisibility == View.VISIBLE || swipeDirection == SwipeDirection.leftToRight || swipeDirection == SwipeDirection.rightToLeft) 0 else deltaVal
        val toYDelta =
            if (currentVisibility == View.GONE || swipeDirection == SwipeDirection.leftToRight || swipeDirection == SwipeDirection.rightToLeft) 0 else deltaVal
        val animation: Animation = TranslateAnimation(
            fromXDelta.toFloat(),
            toXDelta.toFloat(),
            fromYDelta.toFloat(),
            toYDelta.toFloat()
        )

        animation.duration = 500
        layoutToShowHide!!.startAnimation(animation)
        layoutToShowHide!!.visibility =
            if (toXDelta == 0 && toYDelta == 0) View.VISIBLE else View.GONE
    }
}