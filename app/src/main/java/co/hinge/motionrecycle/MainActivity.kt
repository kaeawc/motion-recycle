package co.hinge.motionrecycle

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.header.*
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
                .subscribe(::onPhotoClicked)
                .disposeOn(this, Lifecycle.Event.ON_PAUSE)

        motion_scene?.setTransition(R.id.profileExpanded, R.id.profileCollapsed)
    }

    private fun onPhotoClicked(position: Int) {

        Timber.i("photo at $position was clicked")

        val color = when (position) {
            0 -> R.color.green
            1 -> R.color.blue
            2 -> R.color.red
            3 -> R.color.orange
            4 -> R.color.yellow
            else -> R.color.teal
        }

        val adapter = recycler_view?.adapter as? ProfileAdapter ?: return
        val layoutManager = recycler_view?.layoutManager as? FastLayoutManager ?: return
        val viewHolder = getViewHolderAt(position) ?: return
//        layoutManager.smoothScrollToPosition(recycler_view, RecyclerView.State(), position)
        recycler_view?.smoothScrollBy(0, viewHolder.itemView.top)
        motion_photo_view.setBackgroundColor(ContextCompat.getColor(baseContext, color))

//        motion_photo_view?.isVisible = true
//        motion_scene?.rebuildMotion()

        motion_header?.setTransition(R.id.expanded, R.id.hidden)
        motion_scene?.setTransitionListener(object: MotionLayout.TransitionListener {

            var hidden = false

            override fun onTransitionTrigger(p0: MotionLayout?, p1: Int, p2: Boolean, p3: Float) {}
            override fun onTransitionStarted(p0: MotionLayout?, p1: Int, p2: Int) {}
            override fun onTransitionChange(p0: MotionLayout?, p1: Int, p2: Int, progress: Float) {
                if (progress > 0.1f && !hidden) {
                    hidden = true
                    hideAdapterItem(position)
                }
            }
            override fun onTransitionCompleted(p0: MotionLayout?, p1: Int) {}
        })
        motion_scene?.setTransition(R.id.profileExpanded, R.id.likedPhoto)
        motion_scene?.transitionToEnd()

        Timber.i("setting motion photo view click listener")
        cancel_button?.setOnClickListener {
            Timber.i("motion_photo_view was clicked")
            cancel_button?.setOnClickListener(null)
            returnToProfile(position)
        }

    }

    private fun returnToProfile(position: Int) {
        Timber.i("returnToProfile $position")

        motion_scene?.setTransitionListener(object: MotionLayout.TransitionListener {

            var reset = false

            override fun onTransitionTrigger(p0: MotionLayout?, p1: Int, p2: Boolean, p3: Float) {}
            override fun onTransitionStarted(p0: MotionLayout?, p1: Int, p2: Int) {}
            override fun onTransitionChange(p0: MotionLayout?, p1: Int, p2: Int, progress: Float) {
                if (progress > 0.9f && !reset) {
                    reset = true
                    notifyItemChanged(position)
                }
            }
            override fun onTransitionCompleted(p0: MotionLayout?, p1: Int) {
                hidePlaceholder()
            }
        })
        motion_scene?.setTransition(R.id.likedPhoto, R.id.profileExpanded)
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
            motion_photo_view?.isVisible = false
        }, 1)
    }

    private fun getViewHolderAt(position: Int): PhotoViewHolder? {
        Timber.i("getViewHolderAt $position")
        val itemCount = recycler_view?.adapter?.itemCount ?: return null
        if (itemCount <= 0) return null
        return getProfileViewHolder(position)
    }

    private fun getProfileViewHolder(position: Int): PhotoViewHolder? {
        Timber.i("getProfileViewHolder $position")


        val view = (recycler_view?.layoutManager)?.findViewByPosition(position) ?: return null

        val viewHolder = try {
            recycler_view?.getChildViewHolder(view)
        } catch (ex: IllegalArgumentException) {
            // The given view is not a child of the RecyclerView. This is possible during onPause
            null
        }

        return viewHolder as? PhotoViewHolder
    }


}
