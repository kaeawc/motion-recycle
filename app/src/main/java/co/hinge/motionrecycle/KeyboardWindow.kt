package co.hinge.motionrecycle

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.view.Window

data class KeyboardWindow(
    val heightPixels: Int,
    val windowDecor: ViewGroup,
    val resizeFrame: ViewGroup,
    val contentFrame: ViewGroup
) {

    fun calculateContentHeight(): Int {
        return heightPixels - resizeFrame.top
    }

    companion object {

        /**
         * The Activity View tree usually looks like this:
         *
         * DecorView <- does not get resized, contains space for system Ui bars.
         * - LinearLayout
         * -- FrameLayout <- gets resized
         * --- LinearLayout
         * ---- Activity content
         */
        fun createFrom(activity: Activity): KeyboardWindow {
            val decorView = activity.window.decorView as ViewGroup
            val contentFrame = decorView.findViewById<ViewGroup>(Window.ID_ANDROID_CONTENT)
            val resizeFrame = (contentFrame.parent as ViewGroup).parent as ViewGroup
            return KeyboardWindow(
                heightPixels = activity.resources.displayMetrics.heightPixels,
                windowDecor = decorView,
                resizeFrame = resizeFrame,
                contentFrame = contentFrame)
        }
    }

    fun onDetach(onDetach: () -> Unit) {
        windowDecor.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewDetachedFromWindow(v: View?) {
                onDetach()
            }

            override fun onViewAttachedToWindow(v: View?) {}
        })
    }
}
