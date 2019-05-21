package co.hinge.motionrecycle

import android.content.Context
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_PARENT
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.TOP
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.START
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.END
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.BOTTOM
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import arrow.core.Success
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.header.*
import kotlinx.android.synthetic.main.photo_item.*
import kotlinx.android.synthetic.main.prompt_item.*

class MainActivity : AppCompatActivity() {

    var currentLikedContent = -1
    var needToWarnOnBackPress = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        recycler_view?.adapter = ProfileAdapter()

        comment_composition_view?.isFocusable = false
        comment_composition_view?.isFocusableInTouchMode = false
    }

    override fun onResume() {
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

        profile_scene_view?.setTransition(R.id.profileExpanded, R.id.profileCollapsed)
        // TODO liking_scene_view?.transitionToState(R.id.showProfile)
    }

    override fun onBackPressed() {
        when (liking_scene_view?.currentState) {
            -1 -> liking_scene_view?.transitionToEnd()
            R.id.likedContent -> {
                val writtenComment = comment_bubble?.text?.toString() ?: ""
                if (writtenComment.isNotBlank() && writtenComment != getString(R.string.add_a_comment) && needToWarnOnBackPress) {
                    showCommentDeleteWarning()
                } else {
                    returnToProfile(currentLikedContent)
                }
            }
            R.id.writingCommentForTallContent,
            R.id.writingCommentForShortContent -> {
                hideKeyboardNow()
            }
            else -> super.onBackPressed()
        }
    }

    private fun onContentClicked(position: Int) {
        setupLikedContentTransition(position)
        likedContentState(position)
    }

    private fun setupLikedContentTransition(position: Int) {

        val viewHolder = getViewHolderAt(position) ?: return
        val view = getLikedContentViewAt(viewHolder) ?: return
        if (view.alpha != 1f) return returnToProfile(position)

        currentLikedContent = position

        applyViewToLikedContentPlaceholder(view)

        view.alpha = 0f

        like_blur?.setImageDrawable(Blur.blurScreen(this, profile_scene_view))

        val startMargin = resources.getDimensionPixelSize(R.dimen.profile_horizontal_margin)
        val topOffset = viewHolder.itemView.top
        val height = viewHolder.itemView.height
        val placeholderTop = motion_header.height + topOffset + startMargin
        val placeholderBottom = recycler_view.height - (height + topOffset)

        val placeholderId = R.id.motion_liked_content
        val parentId = R.id.liking_scene_view

        liking_scene_view?.apply {

            stopListening()

            val cs = getConstraintSet(R.id.showProfile)

            cs?.constrainWidth(placeholderId, MATCH_PARENT)
            cs?.constrainHeight(placeholderId, WRAP_CONTENT)
            if (topOffset > 0) {
                cs?.setMargin(placeholderId, TOP, placeholderTop)
                cs?.setMargin(placeholderId, BOTTOM, 0)
                cs?.setVerticalBias(placeholderId, 0f)
            } else {
                cs?.setMargin(placeholderId, BOTTOM, placeholderBottom)
                cs?.setMargin(placeholderId, TOP, 0)
                cs?.setVerticalBias(placeholderId, 1f)
            }
            cs?.connect(placeholderId, START, parentId, START, startMargin)
            cs?.connect(placeholderId, END, parentId, END, startMargin)

            updateState(R.id.showProfile, cs)

            setTransition(R.id.showProfile, R.id.likedContent)
            motion_liked_content?.isVisible = true
            transitionToEnd()
        }
    }

    private fun onKeyboardViewState(viewState: KeyboardViewState) {

        if (liking_scene_view?.currentState !in setOf(
                R.id.likedContent,
                R.id.writingCommentForTallContent,
                R.id.writingCommentForShortContent)) return

        liking_scene_view?.apply {

            val tall = motion_liked_content?.run { height > width } ?: false
            val commentState = when (tall) {
                true -> R.id.writingCommentForTallContent
                else -> R.id.writingCommentForShortContent
            }

            stopListening()
            when {
                viewState.visible -> {
                    setTransition(R.id.likedContentShort, commentState)
                    transitionToEnd()
                }
                tall -> {

                    setTransition(R.id.writingCommentForTallContent, R.id.likedContent)
                    transitionToEnd()
                    likedContentState(currentLikedContent)
                }
                else -> {
                    setTransition(R.id.finishingCommentForShortContent, R.id.likedContent)
                    transitionToEnd()
                    likedContentState(currentLikedContent)
                }
            }

            if (!viewState.visible) {
                val writtenComment = comment_composition_view?.text?.toString() ?: ""
                if (writtenComment.isBlank()) {
                    comment_bubble?.text = getString(R.string.add_a_comment)
                    comment_bubble?.setTextColor(ContextCompat.getColor(baseContext, R.color.gray))
                } else {
                    comment_bubble?.text = writtenComment
                    comment_bubble?.setTextColor(ContextCompat.getColor(baseContext, R.color.black))
                }
            }

            val likedContentBackground = when (viewState.visible) {
                true -> 0
                else -> R.drawable.liked_content_bubble
            }

            motion_liked_content?.setBackgroundResource(likedContentBackground)
        }
    }

    private fun likedContentState(position: Int) {

        motion_liked_content?.setBackgroundResource(R.drawable.liked_content_bubble)

        cancel_button?.setOnClickListener {
            returnToProfile(position)
        }

        like_blur?.setOnTouchListener { v, event ->
            true
        }

        comment_bubble?.setOnClickListener {
            setupCommentComposition()
        }

        comment_done_button?.setOnClickListener(null)
    }

    private fun setupCommentComposition() {

        comment_composition_view?.isFocusableInTouchMode = true
        comment_composition_view?.isFocusable = true
        comment_composition_view?.requestFocus()
        showKeyboardNow()

        comment_done_button?.setOnClickListener {
            onBackPressed()
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

        needToWarnOnBackPress = true
        comment_bubble?.text = getString(R.string.add_a_comment)
        comment_composition_view?.text?.clear()
        currentLikedContent = -1

        liking_scene_view?.after {
            liking_scene_view?.stopListening()
            like_blur?.setOnTouchListener { v, event ->
                false
            }
            val viewHolder = getViewHolderAt(position) ?: return@after
            getLikedContentViewAt(viewHolder)?.alpha = 1f
            motion_liked_content?.isVisible = false
        }

        liking_scene_view?.setTransition(R.id.likedContent, R.id.showProfile)
        liking_scene_view?.transitionToEnd()

        cancel_button?.setOnClickListener(null)
        comment_bubble?.setOnClickListener(null)
        comment_done_button?.setOnClickListener(null)
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

    private fun showCommentDeleteWarning() {
        needToWarnOnBackPress = false
        Toast.makeText(
            baseContext,
            "Exiting will delete the comment you've written",
            Toast.LENGTH_LONG).show()
    }

    private fun showKeyboard(delay: Long = 0L) {
        Handler().postDelayed({
            showKeyboardNow()
        }, delay)
    }

    private fun showKeyboardNow() {
        val inputManager = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager? ?: return
        val windowToken = currentFocus?.windowToken ?: return
        inputManager.toggleSoftInputFromWindow(windowToken, InputMethodManager.SHOW_FORCED, 0)
    }

    private fun hideKeyboardNow() {
        val inputManager = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager? ?: return
        val windowToken = currentFocus?.windowToken ?: return
        inputManager.hideSoftInputFromWindow(windowToken, 0)
    }
}
