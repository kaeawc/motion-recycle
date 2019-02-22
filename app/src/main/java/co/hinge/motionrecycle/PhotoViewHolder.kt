package co.hinge.motionrecycle

import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.subjects.PublishSubject
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.photo_item.*
import timber.log.Timber

class PhotoViewHolder(val clicks: PublishSubject<Int>, view: View) : RecyclerView.ViewHolder(view), LayoutContainer {

    override val containerView: View?
        get() = itemView

    fun onBind(photo: Photo) {
        Timber.i("onBind at $adapterPosition")
        val color = when (adapterPosition) {
            0 -> R.color.green
            1 -> R.color.blue
            2 -> R.color.red
            3 -> R.color.orange
            4 -> R.color.yellow
            else -> R.color.teal
        }
        photo_view.setBackgroundColor(ContextCompat.getColor(itemView.context, color))
        Timber.i("photo_view.setOnClickListener at $adapterPosition")
        photo_view.setOnClickListener { view ->
            Timber.i("photo at $adapterPosition clicked")
            view.setOnClickListener(null)
            clicks.onNext(adapterPosition)
        }
    }

    fun hide() {
        photo_view.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.transparent))
    }
}
