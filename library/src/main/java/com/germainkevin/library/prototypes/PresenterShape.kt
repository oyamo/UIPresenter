package com.germainkevin.library.prototypes

import android.graphics.*
import android.text.TextPaint
import android.util.DisplayMetrics
import android.util.TypedValue
import com.germainkevin.library.prototype_impl.presentation_shapes.SquircleShape

/**
 * A [PresenterShape] represents the shape in which a
 * [Presenter][com.germainkevin.library.Presenter] is drawn
 *
 * Extend this class when you want to create your own [PresenterShape]
 *
 * Then if you want the [PresenterShape] you created to be used instead, pass it
 * inside the [UIPresenter.set][com.germainkevin.library.UIPresenter.set] method like so:
 *
 * UIPresenter.set(presenterShape = YouPresenterShape())
 *
 * When creating your own [PresenterShape],
 *
 * Once you want any object to be drawn inside the [Presenter][com.germainkevin.library.Presenter]
 * do it inside the implemented [PresenterShape.onDrawInPresenterWith]
 * otherwise if you just want to do positioning, setting text size, color, typeface,
 * background, shadow layer etc... do it inside
 * [YouPresenterShape.buildSelfWith][PresenterShape.buildSelfWith]
 *
 * Take example of the [SquircleShape]
 *
 * @see SquircleShape for example
 * @author Kevin Germain
 */
abstract class PresenterShape : ShapeLifecycle {

    /** Paints the background of the implemented [PresenterShape] */
    protected var shapeBackgroundPaint = Paint()

    /**
     * Will hold the coordinates of the decorView through
     * the [View.getGlobalVisibleRect][android.view.View.getGlobalVisibleRect] method,
     * which gives the accurate positioning of a [android.view.View] on a screen's decorView
     *
     * Will be assigned this value through the implemented [PresenterShape.buildSelfWith]
     *
     * @see SquircleShape.buildSelfWith
     * */
    protected var decorViewCoordinates: Rect = Rect()

    /**
     * The shadowed window, is just a [Rect] that's the same size as the decorView and is given
     * a black shadowed color by default in [SquircleShape].
     *
     * It is drawn on top of a clipped out view to present on the canvas
     * in the [PresenterShape.onDrawInPresenterWith] method in [SquircleShape]
     *
     * To make it the same size as the decorView, you should do that in the implemented
     * [PresenterShape.buildSelfWith] like so:
     *
     * builder.resourceFinder.getDecorView().getGlobalVisibleRect(decorViewCoordinates)
     * shadowedWindow.set(rect) // takes the coordinates of the decorView
     * */
    protected var shadowedWindow = Rect()

    /** Paints the [shadowedWindow] */
    protected var shadowedWindowPaint = Paint()

    /**
     * Checks [com.germainkevin.library.UIPresenter.hasShadowedWindow] to know whether the
     * developer wants a [shadowedWindow] drawn on the canvas through the
     * [PresenterShape.onDrawInPresenterWith] method
     *
     * It is true by default in the [com.germainkevin.library.UIPresenter]
     * */
    protected var hasShadowedWindow = true

    /** Paints the description text of the [PresenterShape] */
    protected var descriptionTextPaint: TextPaint = TextPaint()

    /**
     * Will hold the coordinates of the [com.germainkevin.library.UIPresenter.viewToPresent]
     * on the decorView through the [View.getGlobalVisibleRect][android.view.View.getGlobalVisibleRect]
     * method, which gives the accurate positioning of a [android.view.View], on a screen's decorView
     *
     * Will be assigned this value through the implemented [PresenterShape.buildSelfWith]
     *
     * @see SquircleShape.buildSelfWith
     * */
    protected var vTPCoordinates: Rect = Rect()

    /**
     * This variable will hold the [vTPCoordinates] in [RectF]
     *
     *  Do the following to get the exact coordinates (position) of the [view to present],
     *  inside the implemented [PresenterShape.buildSelfWith]:
     *
     *  builder.viewToPresent!!.getGlobalVisibleRect(vTPCoordinates)
     *  viewToPresentBounds.set(vTPCoordinates)
     *
     *  We get the [view to present]'s coordinates so that we can draw our [PresenterShape]: on top
     *  of the [view to present], below the [view to present] or anywhere we want to draw it
     *  relatively to the [view to present]
     */
    protected var viewToPresentBounds: RectF = RectF()

    init {
        shapeBackgroundPaint.isAntiAlias = true
        shapeBackgroundPaint.style = Paint.Style.FILL
        descriptionTextPaint.isAntiAlias = true
        shadowedWindowPaint.isAntiAlias = true
        shadowedWindowPaint.style = Paint.Style.FILL
        // Let's set the default color of the shadowedWindow to a transparent black color
        shadowedWindowPaint.color = Color.parseColor("#80000000")
    }

    /**
     * Sets the text size of the description text
     *
     * @param textSize       The desired text size wanted for the description text
     * @param typedValueUnit the unit in which the description text should be displayed,
     * e.g. [android.util.TypedValue.COMPLEX_UNIT_SP]
     * @param displayMetrics Necessary information to calculate the accurate TextSize
     */
    protected fun setDescriptionTextSize(
        typedValueUnit: Int,
        textSize: Float,
        displayMetrics: DisplayMetrics
    ) {
        descriptionTextPaint.textSize =
            TypedValue.applyDimension(typedValueUnit, textSize, displayMetrics)
    }

    /**
     * Helps to know if a click occurred on this [shape][PresenterShape]
     * by capturing the coordinates where the click event fired, and comparing it to
     * coordinates present in this [shape][PresenterShape]'s drawn bounds.
     *
     * Does the [shape][PresenterShape] contain the point.
     *
     * @param x x coordinate.
     * @param y y coordinate.
     * @return True if the [shape][PresenterShape] contains the point, false otherwise.
     */
    open fun shapeContains(x: Float, y: Float): Boolean = false

    /**
     * Helps to know if a click occurred on the
     * [view to present][android.view.View] by capturing the
     * coordinates where the click event fired, and comparing it to
     * coordinates present in this [view to present][android.view.View]'s drawn bounds.
     *
     * Does the [view to present][android.view.View] contain the point.
     *
     * @param x x coordinate.
     * @param y y coordinate.
     * @return True if the [view to present][android.view.View] contains the point, false otherwise.
     */
    open fun viewToPresentContains(x: Float, y: Float): Boolean {
        return viewToPresentBounds.contains(x, y)
    }
}