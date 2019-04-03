package co.hinge.motionrecycle

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer

abstract class BaseViewHolder(view: View) : RecyclerView.ViewHolder(view), LayoutContainer {

    override val containerView: View?
        get() = itemView
}
