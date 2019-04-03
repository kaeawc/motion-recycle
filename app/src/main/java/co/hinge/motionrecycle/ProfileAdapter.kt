package co.hinge.motionrecycle

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject

open class ProfileAdapter : RecyclerView.Adapter<BaseViewHolder>() {

    val clicks = PublishSubject.create<Int>()

    val items: List<Photo> = (0 until 6).map { Photo("asdf") }

    open fun getClickFlow(): Flowable<Int> {
        return clicks.toFlowable(BackpressureStrategy.LATEST)
    }

    override fun getItemViewType(position: Int): Int {
        return when (position % 2) {
            0 -> R.layout.photo_item
            else -> R.layout.prompt_item
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(viewType, parent, false)
        return when (viewType) {
            R.layout.photo_item -> PhotoViewHolder(clicks, view)
            R.layout.prompt_item -> PromptViewHolder(clicks, view)
            else -> throw IllegalArgumentException("Impossible")
        }
    }

    override fun getItemCount(): Int {
        return 12
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val photo = items.getOrNull(position) ?: return
        when (holder) {
            is PhotoViewHolder -> holder.onBind(photo)
            is PromptViewHolder -> holder.onBind(Prompt("What is this?", "Just the best thing ever!"))
        }
    }

    open fun hide(holder: BaseViewHolder) {

        when (holder) {
            is PhotoViewHolder -> holder.hide()
            is PromptViewHolder -> holder.hide()
        }
    }
}
