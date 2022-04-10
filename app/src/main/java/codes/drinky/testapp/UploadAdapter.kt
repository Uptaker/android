package codes.drinky.testapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import codes.drinky.testapp.model.Upload
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class UploadAdapter(private val context: Context, private val items: ArrayList<Upload>) :
    RecyclerView.Adapter<UploadAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.item_upload_layout, parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.date.text = epochToDate(item.uploadDate)
        holder.url.text = item.url
    }

    override fun getItemCount(): Int {
        return items.size
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val date: TextView = view.findViewById(R.id.tv_upload_date)
        val url: TextView = view.findViewById(R.id.tv_url)
    }

    fun epochToDate(epoch: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy 'at' HH:mm")
        return sdf.format(Date(epoch))
    }
}