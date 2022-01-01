package com.syntext_error.demoKeyBoard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.syntext_error.demoKeyBoard.models.Link

class GifListAdapter(private val interaction: Interaction? = null) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Link>() {

        override fun areItemsTheSame(oldItem: Link, newItem: Link): Boolean {
            return oldItem.link == newItem.link
        }

        override fun areContentsTheSame(oldItem: Link, newItem: Link): Boolean {
            return oldItem == newItem
        }

    }
    private val differ = AsyncListDiffer(this, DIFF_CALLBACK)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return videholder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_gif,
                parent,
                false
            ),
            interaction
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is videholder -> {
                holder.bind(differ.currentList[position])
            }
        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    fun submitList(list: List<Link>) {
        val lsit = ArrayList(list)
        differ.submitList(lsit)

    }

    class videholder
    constructor(
        itemView: View,
        private val interaction: Interaction?
    ) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.gifView)
        fun bind(item: Link) = with(itemView) {
            itemView.setOnClickListener {
                interaction?.onItemSelected(absoluteAdapterPosition, item)
            }

            Glide
                .with(itemView.context)
                .asGif()
                .load(item.link)
                .placeholder(R.mipmap.ic_launcher)
                .into(image)


        }
    }

    interface Interaction {
        fun onItemSelected(position: Int, item: Link)
    }
}
