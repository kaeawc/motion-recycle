package co.hinge.motionrecycle

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class ProfileAdapter : RecyclerView.Adapter<PhotoViewHolder>() {

    val items: List<Photo> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(viewType, parent, false)
        return PhotoViewHolder(view)
    }

    override fun getItemCount(): Int {
        return 6
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photo = items.getOrNull(position) ?: return
        holder.onBind(photo)
    }

}
