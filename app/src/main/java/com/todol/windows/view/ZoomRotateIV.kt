package com.todol.windows.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.atan2
import kotlin.math.sqrt

//用于控制ImageView的缩放、旋转
class ZoomRotateIV @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : AppCompatImageView(context, attrs, defStyle) {

    private val matrix: Matrix = Matrix()
    private var mode = NONE
    private val startPointF = PointF()
    private val midPointF = PointF()
    private var oldDistance = 1f
    private var oldRotation = 0f

    private var gestureDetector: GestureDetector
    private var scaleGestureDetector: ScaleGestureDetector

    companion object {
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2
    }

    init {
        scaleType = ScaleType.MATRIX
        gestureDetector = GestureDetector(context, GestureListener())
        scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        scaleGestureDetector.onTouchEvent(event)

        val currentPointF = PointF(event.x, event.y)

        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                mode = DRAG
                startPointF.set(currentPointF)
            }

            MotionEvent.ACTION_MOVE -> {
                if (mode == DRAG) {
                    val deltaX = currentPointF.x - startPointF.x
                    val deltaY = currentPointF.y - startPointF.y
                    matrix.postTranslate(deltaX, deltaY)
                    startPointF.set(currentPointF.x, currentPointF.y)
                    imageMatrix = matrix
                }
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                mode = ZOOM
                oldDistance = calculateDistance(event)
                oldRotation = calculateRotation(event)
                midPointF.set(calculateMidPoint(event))
            }

            MotionEvent.ACTION_POINTER_UP -> {
                mode = NONE
            }
        }

        return true
    }

    private fun calculateDistance(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return sqrt(x * x + y * y.toDouble()).toFloat()
    }

    private fun calculateMidPoint(event: MotionEvent): PointF {
        val x = (event.getX(0) + event.getX(1)) / 2
        val y = (event.getY(0) + event.getY(1)) / 2
        return PointF(x, y)
    }

    private fun calculateRotation(event: MotionEvent): Float {
        val deltaX = event.getX(0) - event.getX(1)
        val deltaY = event.getY(0) - event.getY(1)
        val radians = atan2(deltaY.toDouble(), deltaX.toDouble())
        return Math.toDegrees(radians).toFloat()
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            matrix.postScale(scaleFactor, scaleFactor, midPointF.x, midPointF.y)
            imageMatrix = matrix
            return true
        }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            matrix.reset()
            imageMatrix = matrix
            return true
        }
    }
}