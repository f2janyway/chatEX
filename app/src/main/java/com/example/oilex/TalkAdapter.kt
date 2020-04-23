package com.example.oilex

import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import kotlinx.android.synthetic.main.msg_item.view.*

class TalkAdapter(val list: List<ChatDto>, val glide: RequestManager) :
    RecyclerView.Adapter<TalkAdapter.ViewHolder>() {
    private var mList = list as ArrayList<ChatDto>

    lateinit var param: LinearLayout.LayoutParams
    private val myId = list[0].id


    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TalkAdapter.ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.msg_item, parent, false))

    override fun getItemCount(): Int = mList.size

    override fun onBindViewHolder(holder: TalkAdapter.ViewHolder, position: Int) {

        holder.itemView.apply {
//            param = chat_image.layoutParams as LinearLayout.LayoutParams

//            Log.e("param width", param.width.toString())
//            Log.e("param width", param.gravity.toString())
            if (mList[position].id == myId) {
                msg_area.apply {
                    if (position != 0)
                        text = mList[position].msg
//                    gravity = Gravity.END
                }
                user_id.text = ""
//                param.gravity = Gravity.END
//                Log.e("tag >> ", chat_image.layoutParams.toString())
//                chat_image.layoutParams = param
//                Log.e("tag", chat_image.layoutParams.toString())
//                item_parent.gravity = Gravity.END
                item_parent.gravity = Gravity.END
            } else {
                msg_area.apply {
                    text = mList[position].msg
//                    gravity = Gravity.START
                }
                user_id.text = mList[position].id
//                param.gravity = Gravity.START
//                chat_image.layoutParams = param
//                Log.e("tag", chat_image.layoutParams.toString())
                item_parent.gravity = Gravity.START
            }

            chat_image.apply {
                if (mList[position].uri != null) {
                    visibility = View.VISIBLE
                    glide.load(mList[position].uri)
                        .override(900,1200)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(this)
                } else {
                    visibility = View.GONE
                }
            }
        }


    }

    fun addItem(item: ChatDto) {
        mList.add(item)
        Log.e("mList-size", mList.size.toString())
        notifyItemInserted(mList.size - 1)
    }

}