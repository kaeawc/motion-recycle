package co.hinge.motionrecycle

import android.content.Context
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_PARENT
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.TOP
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.START
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.END
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.BOTTOM
import androidx.lifecycle.Lifecycle
import arrow.core.Success
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
        Timber.i("onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        recycler_view?.adapter = ProfileAdapter()
    }

    override fun onResume() {
        Timber.i("onResume")
        super.onResume()

        val adapter = (recycler_view?.adapter as? ProfileAdapter) ?: return

        adapter.getClickFlow()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(::onContentClicked)
                .disposeOn(this, Lifecycle.Event.ON_PAUSE)

        Keyboard.prepareToResize(this)
            .doOnNext {
                    result ->

                if (result is Success) {
                    onKeyboardViewState(result.value)
                }
            }
            .subscribe()
            .disposeOn(this, Lifecycle.Event.ON_DESTROY)

        motion_scene?.setTransition(R.id.profileExpanded, R.id.profileCollapsed)
    }

    override fun onBackPressed() {
        Timber.i("onBackPressed")

        when (motion_scene?.currentState) {
            R.id.likedContent -> returnToProfile(currentLikedContent)
            R.id.writingCommentForLike -> motion_scene?.apply {
                stopListening()
                setTransition(R.id.writingCommentForLike, R.id.likedContent)
                transitionToEnd()
                likedContentState(currentLikedContent)
            }
            else -> super.onBackPressed()
        }
    }

    private fun onContentClicked(position: Int) {
        Timber.i("onContentClicked")
        setupLikedContentTransition(position)
        likedContentState(position)
    }

    private fun setupLikedContentTransition(position: Int) {
        Timber.i("setupLikedContentTransition")

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
    }

    private fun onKeyboardViewState(viewState: KeyboardViewState) {
        Timber.i("onKeyboardViewState")

        if (motion_scene?.currentState !in setOf(R.id.likedContent, R.id.writingCommentForLike)) return

        motion_scene?.apply {
            stopListening()
            if (viewState.visible) {
                setTransition(R.id.likedContent, R.id.writingCommentForLike)
                transitionToEnd()
            } else {
                setTransition(R.id.writingCommentForLike, R.id.likedContent)
                transitionToEnd()
                likedContentState(currentLikedContent)
            }

            val likedContentBackground = when (viewState.visible) {
                true -> 0
                else -> R.drawable.liked_content_bubble
            }

            motion_liked_content?.setBackgroundResource(likedContentBackground)
        }
    }

    private fun likedContentState(position: Int) {
        Timber.i("likedContentState")

        motion_liked_content?.setBackgroundResource(R.drawable.liked_content_bubble)

        cancel_button?.setOnClickListener {
            cancel_button?.setOnClickListener(null)
            returnToProfile(position)
        }

        like_blur?.setOnTouchListener { v, event ->
            true
        }

        comment_bubble?.setOnClickListener {
            comment_composition_view?.requestFocus()
            showKeyboardNow()
        }
    }

    private fun getLikedContentViewAt(viewHolder: BaseViewHolder): View? {
        Timber.i("getLikedContentViewAt")
        return when (viewHolder) {
            is PhotoViewHolder -> viewHolder.photo_view
            else -> viewHolder.prompt_bubble
        }
    }

    private fun applyViewToLikedContentPlaceholder(view: View) {
        Timber.i("applyViewToLikedContentPlaceholder")
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
        Timber.i("returnToProfile")

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
        Timber.i("getViewHolderAt")
        val itemCount = recycler_view?.adapter?.itemCount ?: return null
        if (itemCount <= 0) return null
        return getProfileViewHolder(position)
    }

    private fun getProfileViewHolder(position: Int): BaseViewHolder? {
        Timber.i("getProfileViewHolder")


        val view = (recycler_view?.layoutManager)?.findViewByPosition(position) ?: return null

        val viewHolder = try {
            recycler_view?.getChildViewHolder(view)
        } catch (ex: IllegalArgumentException) {
            // The given view is not a child of the RecyclerView. This is possible during onPause
            null
        }

        return viewHolder as? BaseViewHolder
    }

    private fun showKeyboard(delay: Long = 0L) {
        Timber.i("showKeyboard")
        Handler().postDelayed({
            showKeyboardNow()
        }, delay)
    }

    private fun showKeyboardNow() {
        Timber.i("showKeyboardNow")
        val inputManager = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager? ?: return
        val windowToken = currentFocus?.windowToken ?: return
        inputManager.toggleSoftInputFromWindow(windowToken, InputMethodManager.SHOW_FORCED, 0)
    }
}
