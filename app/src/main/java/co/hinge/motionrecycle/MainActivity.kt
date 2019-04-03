package co.hinge.motionrecycle

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.header.*
import kotlinx.android.synthetic.main.photo_item.*
import kotlinx.android.synthetic.main.prompt_item.*
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        recycler_view?.adapter = ProfileAdapter()
    }

    override fun onResume() {
        super.onResume()

        val adapter = (recycler_view?.adapter as? ProfileAdapter) ?: return

        adapter.getClickFlow()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(::onContentClicked)
                .disposeOn(this, Lifecycle.Event.ON_PAUSE)

        motion_scene?.setTransition(R.id.profileExpanded, R.id.profileCollapsed)
    }

    override fun onBackPressed() {
        if (motion_scene?.currentState == R.id.likedContent) {
            returnToProfile()
        } else {
            super.onBackPressed()
        }
    }

    private fun onContentClicked(position: Int) {
        Timber.i("onContentClicked $position")

        val viewHolder = getViewHolderAt(position) ?: return
        recycler_view?.smoothScrollBy(0, viewHolder.itemView.top, DecelerateInterpolator())

        val view = when (viewHolder) {
            is PhotoViewHolder -> viewHolder.photo_view
            else -> viewHolder.prompt_bubble
        }

        applyViewToLikedContentPlaceholder(view)

        motion_header?.setTransition(R.id.expanded, R.id.hidden)

        motion_scene?.apply {
            stopListening()
            setTransition(R.id.profileExpanded, R.id.likedContent)
            transitionToEnd()
        }

        cancel_button?.setOnClickListener {
            cancel_button?.setOnClickListener(null)
            returnToProfile()
        }

        like_blur?.setOnTouchListener { v, event ->
            true
        }
    }

    private fun applyViewToLikedContentPlaceholder(view: View) {
        view.apply {
            isDrawingCacheEnabled = true

            val cachedBitmap = try {
                drawingCache ?: return
            } catch (ex: Exception) {
                return
            }
            val bitmap = try {
                Bitmap.createBitmap(cachedBitmap)
            } catch (ex: OutOfMemoryError) {
                Runtime.getRuntime().gc()
                null
            }

            motion_liked_content?.setImageBitmap(bitmap)

            isDrawingCacheEnabled = false
        }
    }

    private fun returnToProfile() {

        like_blur?.setOnTouchListener { v, event ->
            false
        }

        motion_scene?.stopListening()
        motion_scene?.setTransition(R.id.likedContent, R.id.profileExpanded)
        motion_scene?.transitionToEnd()
    }

    private fun hideAdapterItem(position: Int) {
        val viewHolder = getViewHolderAt(position) ?: return
        (recycler_view?.adapter as? ProfileAdapter)?.hide(viewHolder)
    }

    private fun notifyItemChanged(position: Int) {
        val viewHolder = getViewHolderAt(position) ?: return
        (recycler_view?.adapter as? ProfileAdapter)?.bindViewHolder(viewHolder, position)
    }

    private fun hidePlaceholder() {
        motion_scene?.postDelayed({
            motion_header?.setTransition(R.id.expanded, R.id.collapsed)
            motion_scene?.setTransition(R.id.profileExpanded, R.id.profileCollapsed)
            motion_scene?.setTransitionListener(null)
            motion_scene?.progress = 0.01f
            motion_scene?.progress = 0f
            motion_liked_content?.isVisible = false
        }, 1)
    }

    private fun getViewHolderAt(position: Int): BaseViewHolder? {
        val itemCount = recycler_view?.adapter?.itemCount ?: return null
        if (itemCount <= 0) return null
        return getProfileViewHolder(position)
    }

    private fun getProfileViewHolder(position: Int): BaseViewHolder? {


        val view = (recycler_view?.layoutManager)?.findViewByPosition(position) ?: return null

        val viewHolder = try {
            recycler_view?.getChildViewHolder(view)
        } catch (ex: IllegalArgumentException) {
            // The given view is not a child of the RecyclerView. This is possible during onPause
            null
        }

        return viewHolder as? BaseViewHolder
    }


}
