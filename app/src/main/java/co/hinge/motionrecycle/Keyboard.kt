package co.hinge.motionrecycle

import android.app.Activity
import android.os.Build
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.DecelerateInterpolator
import arrow.core.Failure
import arrow.core.Success
import arrow.core.Try
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.BehaviorSubject

object Keyboard {

    private val viewStateChanges = BehaviorSubject.create<KeyboardViewState>()

    fun prepareToResize(activity: Activity): Flowable<Try<KeyboardViewState>> {
        return resize(KeyboardWindow.createFrom(activity))
    }

    private fun resize(window: KeyboardWindow): Flowable<Try<KeyboardViewState>> {

        val initialHeight = window.calculateContentHeight()
        viewStateChanges.onNext(KeyboardViewState(
            false,
            initialHeight,
            initialHeight))

        listen(window, viewStateChanges)

        window.onDetach {
            window.contentFrame.clearAnimation()
        }

        window.windowDecor.viewTreeObserver.addOnPreDrawListener(object: ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                window.contentFrame.setHeight(window.calculateContentHeight())
                window.windowDecor.viewTreeObserver.removeOnPreDrawListener(this)
                return true
            }
        })

        return viewStateChanges.toFlowable(BackpressureStrategy.LATEST)
            .map { Try { it } }
            .doOnError { Failure(it) }
            .doOnNext { result ->
                when (result) {
                    is Success -> animateHeight(window, result.value)
                    else -> {}
                }
            }
    }

    private fun animateHeight(window: KeyboardWindow, viewState: KeyboardViewState) {

        val contentFrame = window.contentFrame
        contentFrame.setHeight(viewState.contentHeightBeforeResize)

        val transition = ChangeBounds().apply {
            interpolator = DecelerateInterpolator(4f)
            duration = 270
        }
        val sceneRoot = window.resizeFrame

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            TransitionManager.endTransitions(sceneRoot)
        }

        TransitionManager.beginDelayedTransition(sceneRoot, transition)

        contentFrame.setHeight(viewState.contentHeight)
    }

    private fun View.setHeight(height: Int) {
        val params = layoutParams
        params.height = height
        layoutParams = params
    }

    fun listen(keyboardWindow: KeyboardWindow, viewStateChanges: BehaviorSubject<KeyboardViewState>) {
        val detector = Detector(keyboardWindow, viewStateChanges)
        keyboardWindow.windowDecor.viewTreeObserver.addOnPreDrawListener(detector)
        keyboardWindow.onDetach {
            keyboardWindow.windowDecor.viewTreeObserver.removeOnPreDrawListener(detector)
        }
    }

    private class Detector(
        val window: KeyboardWindow,
        val viewStateChanges: BehaviorSubject<KeyboardViewState>
    ) : ViewTreeObserver.OnPreDrawListener {

        private var previousHeight: Int = -1

        override fun onPreDraw(): Boolean {
            val detected = detect()

            // The layout flickers for a moment, usually on the first
            // animation. Intercepting this pre-draw seems to solve the problem.
            return detected.not()
        }

        private fun detect(): Boolean {
            val resizeHeight = window.resizeFrame.height
            if (resizeHeight == previousHeight) {
                return false
            }

            if (previousHeight != -1) {
                val isKeyboardVisible = resizeHeight < window.calculateContentHeight()
                viewStateChanges.onNext(KeyboardViewState(
                    visible = isKeyboardVisible,
                    contentHeight = resizeHeight,
                    contentHeightBeforeResize = previousHeight))
            }

            previousHeight = resizeHeight
            return true
        }
    }
}
