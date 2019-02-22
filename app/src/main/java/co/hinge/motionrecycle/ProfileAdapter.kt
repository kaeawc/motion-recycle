package co.hinge.motionrecycle

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject

open class ProfileAdapter : RecyclerView.Adapter<PhotoViewHolder>() {

    val clicks = PublishSubject.create<Int>()

    val items: List<Photo> = (0 until 6).map { Photo("asdf") }

    open fun getClickFlow(): Flowable<Int> {
        return clicks.toFlowable(BackpressureStrategy.LATEST)
    }

    override fun getItemViewType(position: Int): Int {
        return R.layout.photo_item
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(viewType, parent, false)
        return PhotoViewHolder(clicks, view)
    }

    override fun getItemCount(): Int {
        return 6
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photo = items.getOrNull(position) ?: return
        holder.onBind(photo)
    }

    open fun hide(holder: PhotoViewHolder) {
        holder.hide()
    }
}
