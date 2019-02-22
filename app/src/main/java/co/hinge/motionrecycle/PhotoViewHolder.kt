package co.hinge.motionrecycle

import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.photo_item.*

class PhotoViewHolder(view: View) : RecyclerView.ViewHolder(view), LayoutContainer {

    override val containerView: View?
        get() = itemView

    fun onBind(photo: Photo) {
        photo_view.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.blue))
    }
}
