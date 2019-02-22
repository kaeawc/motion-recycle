package co.hinge.motionrecycle

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
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

        val layoutManager = recycler_view?.layoutManager as? FastLayoutManager ?: return
        val viewHolder = getViewHolderAt(position) ?: return
        layoutManager.smoothScrollToPosition(recycler_view, RecyclerView.State(), position)
        recycler_view?.smoothScrollBy(0, viewHolder.itemView.top)
        motion_photo_view.setBackgroundColor(ContextCompat.getColor(baseContext, color))
    }

    fun getViewHolderAt(position: Int): PhotoViewHolder? {
        val itemCount = recycler_view?.adapter?.itemCount ?: return null
        if (itemCount <= 0) return null
        return getProfileViewHolder(position)
    }

    private fun getProfileViewHolder(position: Int): PhotoViewHolder? {

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
