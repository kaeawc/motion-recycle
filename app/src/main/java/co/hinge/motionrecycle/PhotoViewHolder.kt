package co.hinge.motionrecycle

import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.subjects.PublishSubject
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.photo_item.*

class PhotoViewHolder(val clicks: PublishSubject<Int>, view: View) : RecyclerView.ViewHolder(view), LayoutContainer {

    override val containerView: View?
        get() = itemView

    fun onBind(photo: Photo) {
        val color = when (adapterPosition) {
            0 -> R.color.green
            1 -> R.color.blue
            2 -> R.color.red
            3 -> R.color.orange
            4 -> R.color.yellow
            else -> R.color.teal
        }
        photo_view.setBackgroundColor(ContextCompat.getColor(itemView.context, color))
        photo_view.setOnClickListener {
            clicks.onNext(adapterPosition)
        }
    }
}
