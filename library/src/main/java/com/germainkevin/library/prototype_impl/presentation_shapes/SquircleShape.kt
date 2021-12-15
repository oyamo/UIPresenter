package com.germainkevin.library.prototype_impl.presentation_shapes

import android.graphics.*
import android.os.Build
import android.text.StaticLayout
import com.germainkevin.library.buildStaticLayout
import com.germainkevin.library.prototype_impl.PresentationBuilder
import com.germainkevin.library.prototypes.PresenterShape

/**
 * This is a [PresenterShape] that has the shape of a Squircle
 * @author Kevin Germain
 * */
class SquircleShape : PresenterShape() {
    /**
     * The first layer of the [SquircleShape], a rectangle that we will later on be the Squircle
     *
     * It's the background of the [SquircleShape]
     */
    private lateinit var mSquircleShapeRectF: RectF

    /**
     * The radius applied to the [mSquircleShapeRectF]
     */
    private var mSquircleRadius = 15f

    /**
     * Will hold the coordinates of the [PresentationBuilder.mViewToPresent] on the decorView
     * through the [android.view.View.getGlobalVisibleRect] method, which gives the accurate
     * positioning of a View on a screen
     * */
    private lateinit var vTPCoordinates: Rect

    /**
     * The left,top,right and bottom position, of the
     * [view to present][PresentationBuilder.mViewToPresent] in [Float]
     * This variable will hold the [vTPCoordinates] in [RectF]
     */
    private lateinit var mViewToPresentBounds: RectF

    /**
     * The [Paint] to use to draw the [mSquircleShapeRectF]
     */
    private lateinit var mSquircleShapePaint: Paint

    /**
     * Layout to wrap the [PresentationBuilder.mDescriptionText]
     * so that the text can be laid out in multiline instead of the
     * default singleLine that the [Canvas.drawText] method puts the text in, by default
     */
    private lateinit var staticLayout: StaticLayout

    /**
     * Position of the [staticLayout] inside this [mSquircleShapeRectF]
     */
    private lateinit var mStaticLayoutPosition: PointF

    private fun initPaint() {
        mSquircleShapePaint = Paint()
        mSquircleShapePaint.isAntiAlias = true
        mSquircleShapePaint.style = Paint.Style.FILL
    }

    private fun initFloats() {
        mViewToPresentBounds = RectF()
        mSquircleShapeRectF = RectF()
        mStaticLayoutPosition = PointF()
        vTPCoordinates = Rect()
    }

    init {
        initPaint()
        initFloats()
    }

    override fun setShapeBackgroundColor(color: Int) {
        mSquircleShapePaint.color = color
    }

    override fun shapeContains(x: Float, y: Float): Boolean {
        return mSquircleShapeRectF.contains(x, y)
    }

    override fun viewToPresentContains(x: Float, y: Float): Boolean {
        return mViewToPresentBounds.contains(x, y)
    }

    override fun buildSelfWith(builder: PresentationBuilder<*>) {
        builder.mDescriptionText?.let {
            setShapeBackgroundColor(builder.mBackgroundColor!!)
            hasShadowedWindow = builder.mPresenterHasShadowedWindow
            if (hasShadowedWindow) setShadowedWindowColor(Color.parseColor("#80000000"))
            if (builder.mHasShadowLayer) {
                mSquircleShapePaint.setShadowLayer(
                    builder.presenterShadowLayer.radius,
                    builder.presenterShadowLayer.dx,
                    builder.presenterShadowLayer.dy,
                    builder.presenterShadowLayer.shadowColor
                )
            }
            setDescriptionTextColor(builder.mDescriptionTextColor!!)
            setDescriptionTypeface(builder.mTypeface)

            val mDecorView = builder.resourceFinder.getDecorView()!!
            setDescriptionTextSize(
                builder.mDescriptionTextUnit,
                builder.mDescriptionTextSize,
                mDecorView.resources.displayMetrics
            )
            // Get the exact coordinates of the view to present inside the decorView
            // So we can relatively place the StaticLayout & mSquircleShapeRectF next to it
            builder.mViewToPresent!!.getGlobalVisibleRect(vTPCoordinates)
            mViewToPresentBounds.set(vTPCoordinates)

            val descTextWidth = mDescriptionTextPaint.measureText(it).toInt()

            // Remaining Space between View to present's left position and the end of the
            // decorView's width
            val a = mDecorView.width - mViewToPresentBounds.left
            // $a minus description text width
            val b = a - descTextWidth

            val horizontalMargin = 56
            var staticLayoutWidth: Int = when {
                // The description text's width is larger than the available space
                // for it to be laid out horizontally
                b <= 0 -> (a - horizontalMargin).toInt()
                // The description text's width is large enough to be laid out
                b >= horizontalMargin -> descTextWidth + 16
                // The description text's width is larger than the horizontalMargin
                // but not larger than the remaining space it can be laid out in, inside
                // the decorView's width
                else -> descTextWidth - horizontalMargin
            }

            // The percentage of the decor view's width occupied by the staticLayoutWidth
            val c = staticLayoutWidth * 100 / mDecorView.width

            // Registers the position the text inside the StaticLayout should be placed
            // inside the canvas
            val sLTextPosition = PointF()
            // Verifies if the static layout width is more than 45% of the decor view width
            val isWidthOver45Percent: Boolean
            if (c > 45) {
                // Build Static layout as we are satisfied with its current width
                staticLayout = buildStaticLayout(it, mDescriptionTextPaint, staticLayoutWidth)
                isWidthOver45Percent = true
            } else {
                // let's make staticLayoutWidth 65% of decor view's width
                staticLayoutWidth = (65 * mDecorView.width / 100)
                // then build it with new width
                staticLayout = buildStaticLayout(it, mDescriptionTextPaint, staticLayoutWidth)
                isWidthOver45Percent = false
            }

            // The amount of space in px that the StaticLayout needs to lay itself out
            // vertically
            val slHeightVertically = mViewToPresentBounds.bottom + staticLayout.height

            // Distance left between the end of the screen and the StaticLayout's final
            // bottom position or final Height position
            val e = mDecorView.height - slHeightVertically
            // How much percentage of the decor view height's still available if
            // we lay out the StaticLayout vertically from up to down
            val ePercentage = e * 100 / mDecorView.height

            if (isWidthOver45Percent) {
                // StaticLayout goes: up to down, start to end

                // If the DecorView's Height has a remaining of 15% space available
                // still at its bottom even after the StaticLayout gets laid out
                // vertically from up to down, then execute the condition inisde the
                // if statement
                if (ePercentage >= 15) {
                    sLTextPosition.x = mViewToPresentBounds.left + 16
                    sLTextPosition.y = mViewToPresentBounds.bottom + 32
                    mSquircleShapeRectF.set(
                        sLTextPosition.x - 16,
                        sLTextPosition.y - 16,
                        sLTextPosition.x + staticLayoutWidth.toFloat(),
                        sLTextPosition.y + (staticLayout.height + 16)
                    )
                    mStaticLayoutPosition = PointF(sLTextPosition.x, sLTextPosition.y)
                } else {
                    // StaticLayout goes: down to up, start to end
                    sLTextPosition.x = mViewToPresentBounds.left + 16
                    sLTextPosition.y = mViewToPresentBounds.top - (staticLayout.height + 32)

                    mSquircleShapeRectF.set(
                        sLTextPosition.x - 16,
                        sLTextPosition.y - 16,
                        sLTextPosition.x + (staticLayoutWidth + 16).toFloat(),
                        sLTextPosition.y + staticLayout.height + 16
                    )

                    mStaticLayoutPosition = PointF(mSquircleShapeRectF.left + 16, sLTextPosition.y)
                }

            } else {
                // StaticLayout goes: up to down, end to start
                if (ePercentage >= 15) {
                    // the position of the text based on those conditions
                    sLTextPosition.x = mViewToPresentBounds.right - 16
                    sLTextPosition.y = mViewToPresentBounds.bottom + 32

                    mSquircleShapeRectF.set(
                        sLTextPosition.x - staticLayoutWidth.toFloat(),
                        sLTextPosition.y - 16,
                        sLTextPosition.x + 16,
                        sLTextPosition.y + (staticLayout.height + 16)
                    )
                    mStaticLayoutPosition = PointF(mSquircleShapeRectF.left + 16, sLTextPosition.y)
                } else {
                    // StaticLayout goes: down to up, end to start
                    sLTextPosition.x = mViewToPresentBounds.right
                    sLTextPosition.y =
                        mViewToPresentBounds.top - (staticLayout.height + 32)

                    mSquircleShapeRectF.set(
                        sLTextPosition.x - (staticLayoutWidth + 16).toFloat(),
                        sLTextPosition.y - 16,
                        sLTextPosition.x,
                        sLTextPosition.y + staticLayout.height + 16
                    )

                    mStaticLayoutPosition = PointF(mSquircleShapeRectF.left + 16, sLTextPosition.y)
                }
            }
            val rect = Rect()
            mDecorView.getGlobalVisibleRect(rect)
            shadowedWindow.set(rect)
        }
    }

    override fun onDrawInPresenterWith(canvas: Canvas?) {
        canvas!!.save()
        // Draws the shadowed window first, then draws the presenter shape
        if (hasShadowedWindow) {
            mViewToPresentBounds.inset(-4f, -4f)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                canvas.clipOutRect(mViewToPresentBounds)
            } else {
                canvas.clipRect(mViewToPresentBounds, Region.Op.DIFFERENCE)
            }
            canvas.drawRect(shadowedWindow, shadowedWindowPaint)
        }
        canvas.drawRoundRect(
            mSquircleShapeRectF,
            mSquircleRadius,
            mSquircleRadius,
            mSquircleShapePaint
        )
        canvas.translate(mStaticLayoutPosition.x, mStaticLayoutPosition.y)
        staticLayout.draw(canvas)
        canvas.restore()
    }
}