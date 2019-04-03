package co.hinge.motionrecycle

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.lifecycle.Lifecycle
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.header.*
import kotlinx.android.synthetic.main.photo_item.*
import kotlinx.android.synthetic.main.prompt_item.*
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    var currentLikedContent = -1

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
            returnToProfile(currentLikedContent)
        } else {
            super.onBackPressed()
        }
    }

    private fun onContentClicked(position: Int) {
        Timber.i("onContentClicked $position")

        val view = getLikedContentViewAt(position) ?: return
        currentLikedContent = position

        applyViewToLikedContentPlaceholder(view)

        view.alpha = 0f

        motion_header?.setTransition(R.id.expanded, R.id.hidden)

        motion_scene?.apply {
            stopListening()
            setTransition(R.id.profileExpanded, R.id.likedContent)
            transitionToEnd()
        }

        cancel_button?.setOnClickListener {
            cancel_button?.setOnClickListener(null)
            returnToProfile(position)
        }

        like_blur?.setOnTouchListener { v, event ->
            true
        }
    }

    private fun getLikedContentViewAt(position: Int): View? {
        val viewHolder = getViewHolderAt(position) ?: return null
        recycler_view?.smoothScrollBy(0, viewHolder.itemView.top, DecelerateInterpolator(4f))

        return when (viewHolder) {
            is PhotoViewHolder -> viewHolder.photo_view
            else -> viewHolder.prompt_bubble
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

    private fun returnToProfile(position: Int) {

        currentLikedContent = -1

        like_blur?.setOnTouchListener { v, event ->
            false
        }

        motion_scene?.after {
            getLikedContentViewAt(position)?.alpha = 1f
        }
        motion_scene?.setTransition(R.id.likedContent, R.id.profileExpanded)
        motion_scene?.transitionToEnd()
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
