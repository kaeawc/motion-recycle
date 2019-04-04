package co.hinge.motionrecycle

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_PARENT
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.TOP
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.START
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.END
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.BOTTOM
import androidx.lifecycle.Lifecycle
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.header.*
import kotlinx.android.synthetic.main.photo_item.*
import kotlinx.android.synthetic.main.prompt_item.*

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
        val viewHolder = getViewHolderAt(position) ?: return
        val view = getLikedContentViewAt(viewHolder) ?: return

        currentLikedContent = position

        applyViewToLikedContentPlaceholder(view)

        view.alpha = 0f

        like_blur?.setImageDrawable(Blur.blurScreen(this, motion_scene))

        val startMargin = resources.getDimensionPixelSize(R.dimen.profile_horizontal_margin)
        val topOffset = viewHolder.itemView.top
        val height = viewHolder.itemView.height
        val placeholderTop = motion_header.height + topOffset + startMargin
        val placeholderBottom = recycler_view.height - (height + topOffset)

        val placeholderId = R.id.motion_liked_content
        val parentId = R.id.motion_scene

        motion_header?.setTransition(R.id.expanded, R.id.hidden)

        motion_scene?.apply {

            getConstraintSet(R.id.profileExpanded)?.apply {
                constrainWidth(placeholderId, MATCH_PARENT)
                constrainHeight(placeholderId, WRAP_CONTENT)
                if (topOffset > 0) {
                    connect(placeholderId, TOP, parentId, TOP, placeholderTop)
                    clear(placeholderId, BOTTOM)
                } else {
                    connect(placeholderId, BOTTOM, parentId, BOTTOM, placeholderBottom)
                    clear(placeholderId, TOP)
                }
                connect(placeholderId, START, parentId, START, startMargin)
                connect(placeholderId, END, parentId, END, startMargin)
            }

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

    private fun getLikedContentViewAt(viewHolder: BaseViewHolder): View? {
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
            val viewHolder = getViewHolderAt(position) ?: return@after
            getLikedContentViewAt(viewHolder)?.alpha = 1f
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
