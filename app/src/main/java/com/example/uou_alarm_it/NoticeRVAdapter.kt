package com.example.uou_alarm_it

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.uou_alarm_it.databinding.ItemNoticeBinding

class NoticeRVAdapter() : RecyclerView.Adapter<NoticeRVAdapter.ViewHolder>() {

    var noticeList: ArrayList<Notice> = NoticeActivity.noticeList
    var bookmarkList: HashSet<Notice> = NoticeActivity.bookmarkList

    interface MyClickListener {
        fun onItemClick(notice: Notice)
        fun onBookmarkClick(notice: Notice)
    }

    private lateinit var myClickListener: MyClickListener

    fun setMyClickListener(itemClickListener: MyClickListener) {
        myClickListener = itemClickListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNoticeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = noticeList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notice = noticeList[position]
        holder.bind(notice)
    }

    inner class ViewHolder(private val binding: ItemNoticeBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(notice: Notice) {
            binding.itemNoticeTitle.text = notice.title
            binding.itemNoticeDate.text = notice.date

            // type이 "NOTICE"이면 표시, 아니면 숨김
            if (notice.type == "NOTICE") {
                binding.itemNoticeImportMarkIv.visibility = View.VISIBLE
                binding.itemNoticeImportMarkIv.setImageResource(R.drawable.import_mark)
            } else if (notice.type == "NECESSARY") {
                binding.itemNoticeImportMarkIv.visibility = View.VISIBLE
                binding.itemNoticeImportMarkIv.setImageResource(R.drawable.necessary_mark)
            } else {
                binding.itemNoticeImportMarkIv.visibility = View.GONE
            }

            if (notice in bookmarkList) {
                binding.itemNoticeBookmark.setImageResource(R.drawable.bookmark_on)
            } else {
                binding.itemNoticeBookmark.setImageResource(R.drawable.bookmark_off)
            }

            binding.itemNoticeBookmarkCl.setOnClickListener {
                myClickListener.onBookmarkClick(notice)
                notifyItemChanged(adapterPosition)
            }

            binding.itemNotice.setOnClickListener {
                myClickListener.onItemClick(notice)
                notifyItemChanged(adapterPosition)
            }
        }
    }
}