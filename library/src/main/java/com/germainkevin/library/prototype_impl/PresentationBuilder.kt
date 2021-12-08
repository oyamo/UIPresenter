package com.germainkevin.library.prototype_impl

import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.annotation.AnimRes
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import com.germainkevin.library.Presenter
import com.germainkevin.library.R
import com.germainkevin.library.circularReveal
import com.germainkevin.library.mainThread
import com.germainkevin.library.prototype_impl.presentation_shapes.SquircleShape
import com.germainkevin.library.prototypes.PresenterShape
import com.germainkevin.library.prototypes.ResourceFinder
import kotlinx.coroutines.*
import timber.log.Timber


/**
 * Contains all the methods for presenting a UI element.
 * Provides data for [mPresenter] and any [presenter shapes][PresenterShape]
 * Those data are marked as internal variables
 * @param resourceFinder is an interface that gives access to an Activity or
 * a fragment's environment
 * @param T whatever class that implements this class
 */
abstract class PresentationBuilder<T : PresentationBuilder<T>>(val resourceFinder: ResourceFinder) {

    /**
     * [DecorView][ViewGroup] of [mViewToPresent]
     * It is responsible for adding [mPresenter] to your UI
     */
    private var mDecorView: ViewGroup? = null

    /**
     * The [Presenter] that will be created and added by the [mDecorView]
     */
    private var mPresenter: Presenter? = null

    /**
     * Exposes the current state of this [mPresenter] to the function [isRemoving] and [isRemoved]
     */
    @Presenter.PresenterState
    private var mState = Presenter.STATE_NOT_SHOWN

    /**
     * Exposes state changes from the [mPresenter] to the user of this library
     */
    private var mPresenterStateChangeListener: (Int) -> Unit = {}

    /**
     * Should the back button press dismiss the [Presenter].
     */
    private var mBackButtonDismissEnabled = true

    /**
     * Should the [mPresenter] be removed when clicked on the screen while it's displayed
     * */
    private var mAutoRemoveApproval = true

    /**
     * The [PresenterShape] by default or set by the user for this [mPresenter]
     * */
    private var mPresenterShape: PresenterShape = SquircleShape()

    /**
     * The view that the [presenter][Presenter] will present.
     * Will be accessed from a [PresenterShape] for example [SquircleShape]
     */
    internal var mViewToPresent: View? = null

    /**
     * Has the [View] to present been set successfully?
     * true, if the [PresentationBuilder.mViewToPresent] is not null or false otherwise
     */
    internal var mIsViewToPresentSet = false

    /** Represents what animation to use to animate [mPresenter]
     */
    private var mPresenterAnimation: Int = Presenter.ANIM_CIRCULAR_REVEAL

    /**
     * The duration of the animation on the [mPresenter]
     * */
    private var mPresenterAnimDuration = 600L

    init {
        mDecorView = resourceFinder.getDecorView()
        mPresenter = resourceFinder.getContext()?.let { Presenter(it) }?.also {
            it.mPresentationBuilder = this
            it.presenterShape = mPresenterShape
            it.mPresenterStateChangeNotifier = object : Presenter.StateChangeNotifier {
                override fun onPresenterStateChange(state: Int) {
                    onPresenterStateChanged(state)
                    when (state) {
                        Presenter.STATE_CANVAS_DRAWN -> {
                            if (mPresenterAnimation == Presenter.ANIM_CIRCULAR_REVEAL) {
                                mPresenter?.circularReveal(mPresenterAnimDuration)
                                onPresenterStateChange(Presenter.STATE_REVEALED)
                            }
                        }

                        Presenter.STATE_BACK_BUTTON_PRESSED -> {
                            if (mAutoRemoveApproval && mBackButtonDismissEnabled) {
                                onPresenterStateChanged(Presenter.STATE_REMOVING)
                                removePresenterIfPresent()
                            }
                        }
                        Presenter.STATE_VTP_PRESSED -> {
                            if (mAutoRemoveApproval) {
                                onPresenterStateChanged(Presenter.STATE_REMOVING)
                                removePresenterIfPresent()
                            }
                        }
                        Presenter.STATE_FOCAL_PRESSED -> {
                            if (mAutoRemoveApproval) {
                                onPresenterStateChanged(Presenter.STATE_REMOVING)
                                removePresenterIfPresent()
                            }
                        }
                        Presenter.STATE_NON_FOCAL_PRESSED -> {
                            if (mAutoRemoveApproval) {
                                onPresenterStateChanged(Presenter.STATE_REMOVING)
                                removePresenterIfPresent()
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * This method is made to only be called after you've finished
     * propagating data to a [PresentationBuilder]
     * It displays a [Presenter] inside a [DecorView][ViewGroup]
     *
     * @return the current [PresentationBuilder]
     */
    open fun present(): T {
        mainThread {
            // By this time, every configuration necessary would have already
            // been done, we can now pass this builder to the PresenterShape
            // so it builds a Shape to output to the UI.
            mViewToPresent?.let {
                val job = async {
                    mPresenterShape.buildSelfWith(this@PresentationBuilder)
                    val job1 = async { removePresenterIfPresent() }
                    job1.await()
                    job1.join()
                }
                job.await()
                job.join()
                if (job.isCompleted) {
                    mPresenter?.let { _v ->
                        mDecorView?.addView(_v)
                    }
                }
            }
        }

        return this as T
    }

    /**
     * Is the [Presenter]'s current state: [Presenter.STATE_REMOVING].
     *
     * @return True if removing.
     */
    private fun isRemoving(): Boolean = mState == Presenter.STATE_REMOVING

    /**
     * Is the [Presenter]'s current state: [Presenter.STATE_REMOVED].
     *
     * @return True if removed.
     */
    private fun isRemoved(): Boolean = mState == Presenter.STATE_REMOVED

    /**
     * Removes the [mPresenter] if present, from the [mDecorView]
     * */
    private fun removePresenterIfPresent() = mainThread {
        var mViewToRemove: View? = null
        val job = async {
            // Never reference mPresenter directly, always reference the mPresenter this way
            mViewToRemove = mDecorView?.findViewById(R.id.android_ui_presenter)
        }
        job.await()
        job.join()
        if (job.isCompleted) {
            mViewToRemove?.let {
                if (isRemoving() && !isRemoved()) {
                    onPresenterStateChanged(Presenter.STATE_REMOVED)
                    mDecorView?.removeView(it)
                }
            }
        }
    }

    /**
     * This method is only called from the [mPresenter] to notify this builder
     * of its state change
     */
    private fun onPresenterStateChanged(@Presenter.PresenterState state: Int) {
        mState = state
        mPresenterStateChangeListener(state)
    }

    /**
     * Sets a listener that listens to the [presenter][Presenter] state changes.
     * @param listener The listener to use
     */
    open fun setPresenterStateChangeListener(listener: (Int) -> Unit): T {
        mPresenterStateChangeListener = listener
        return this as T
    }

    /**
     * Gives the user the possibility to choose one of one of the animations available
     * to display [mPresenter]
     * */
    open fun setPresenterAnimation(@Presenter.PresenterAnimation presenterAnimation: Int): T {
        mPresenterAnimation = presenterAnimation
        return this as T
    }

    /**
     * Gives the user the possibility to define how long the [mPresenterAnimation] should run
     * */
    open fun setAnimationDuration(duration: Long): T {
        mPresenterAnimDuration = duration
        return this as T
    }

    /**
     * Sets the view to place the [presenter][Presenter] around.
     * @param view The view to present
     */
    open fun setViewToPresent(view: View?): T {
        mViewToPresent = view
        mIsViewToPresentSet = mViewToPresent != null
        return this as T
    }

    /**
     * Sets the view to place the [presenter][Presenter] around.
     * @param viewId The id of the view to present
     */
    open fun setViewToPresent(@IdRes viewId: Int): T {
        mViewToPresent = resourceFinder.findViewById(viewId)
        mIsViewToPresentSet = mViewToPresent != null
        return this as T
    }

    /**
     * Sets the shape of the [Presenter] to be added to the UI
     */
    open fun setPresenterShape(mPresenterShape: PresenterShape): T {
        this.mPresenterShape = mPresenterShape
        return this as T
    }

    open fun setBackgroundColor(bgColor: Int): T {
        mPresenterShape.setBackgroundColor(bgColor)
        return this as T
    }

    /**
     * @param mBoolean defines whether the shape should have
     * a shadow layer drawn in its background or not
     * */
    open fun setHasShadowLayer(mBoolean: Boolean): T {
        mPresenterShape.setHasShadowLayer(mBoolean)
        return this as T
    }

    /**
     * @param shadowColor The color of the shadow layer
     * */
    open fun setShadowLayerColor(@ColorInt shadowColor: Int): T {
        mPresenterShape.setShadowLayerColor(shadowColor)
        return this as T
    }

    /**
     * The text that will be explaining the ui element you
     * want to present to the user
     * @param descriptionText The text you want to explain the ui element
     * */
    open fun setDescriptionText(descriptionText: String): T {
        mPresenterShape.setDescriptionText(descriptionText)
        return this as T
    }

    open fun setDescriptionTextColor(textColor: Int): T {
        mPresenterShape.setDescriptionTextColor(textColor)
        return this as T
    }

    open fun setDescriptionTextSize(typedValue: Int, textSize: Float): T {
        mPresenterShape.setDescriptionTextSize(typedValue, textSize)
        return this as T
    }

    open fun setDescriptionTypeface(typeface: Typeface?): T {
        mPresenterShape.setDescriptionTypeface(typeface)
        return this as T
    }

    /**
     * Sets whether or not, a click on a
     * [Presenter] should result in the removal ot this [Presenter] from the DecorView
     */
    open fun setAutoRemoveApproval(autoRemoveApproval: Boolean): T {
        mAutoRemoveApproval = autoRemoveApproval
        return this as T
    }

    /**
     * Back button can be used to dismiss the prompt. True by default.
     * @param enabled True for back button dismiss enabled
     * @return This Builder object to allow for chaining of calls to set methods
     */
    open fun setBackButtonDismissEnabled(enabled: Boolean): T {
        mBackButtonDismissEnabled = enabled
        return this as T
    }
}