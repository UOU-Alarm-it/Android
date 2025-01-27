package com.example.uou_alarm_it

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.uou_alarm_it.databinding.ActivityNoticeBinding
import com.google.gson.Gson

class NoticeActivity : AppCompatActivity() {
    lateinit var binding : ActivityNoticeBinding
    lateinit var noticeRVAdapter : NoticeRVAdapter


    companion object {
        var position : Int = 0
        var noticeList : ArrayList<Notice> = arrayListOf()
        var bookmarkList : HashSet<Notice> = hashSetOf()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoticeBinding.inflate(layoutInflater)
        bookmarkList = loadBookmarkList()

        setposition(position)

        binding.noticeTabAll.setOnClickListener{
            setposition(0)
        }
        binding.noticeTabImportant.setOnClickListener {
            setposition(1)
        }
        binding.noticeTabBookmark.setOnClickListener {
            setposition(2)
        }
        binding.noticeSearchIv.setOnClickListener{
            if(binding.noticeSearchEt.visibility == View.GONE) {
                binding.noticeSearchEt.visibility = View.VISIBLE
                binding.noticeTabLayout.visibility = View.GONE
            }
            else {
                noticeSearch()
            }
        }

        setContentView(binding.root)
    }

    private fun setposition(position:Int){

        binding.noticeTabAll.setTextColor(ContextCompat.getColor(this, R.color.gray40))
        binding.noticeTabImportant.setTextColor(ContextCompat.getColor(this, R.color.gray40))
        binding.noticeTabBookmark.setTextColor(ContextCompat.getColor(this, R.color.gray40))



        when (position) {
            1 -> {
                NoticeActivity.position = 1
                binding.noticeTabImportant.setTextColor(ContextCompat.getColor(this, R.color.black))
                noticeList = arrayListOf() // 주요 공지
            }
            2 -> {
                NoticeActivity.position = 2
                binding.noticeTabBookmark.setTextColor(ContextCompat.getColor(this, R.color.black))
                noticeList = noticeList.filter { it in bookmarkList }.toCollection(ArrayList())
            }
            else -> {
                NoticeActivity.position = 0
                binding.noticeTabAll.setTextColor(ContextCompat.getColor(this, R.color.black))
            }
        }
        noticeRVAdapter = NoticeRVAdapter()
        binding.noticeRv.adapter = noticeRVAdapter
        noticeRVAdapter.setMyClickListener(object : NoticeRVAdapter.MyClickListener{
            override fun onItemClick(notice: Notice) {
                Log.d("test", "Item")
            }

            override fun onBookmarkClick(notice: Notice) {
                Log.d("test", "Bookmark")
                if (notice in bookmarkList) {
                    bookmarkList.remove(notice)
                }
                else {
                    bookmarkList.add(notice)
                }
                noticeRVAdapter.bookmarkList = bookmarkList
                saveBookmarkList(bookmarkList)
                Log.d("Save Bookmark", bookmarkList.toString())
            }

        })
        binding.noticeRv.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
    }

    fun saveBookmarkList(BookmarkList : HashSet<Notice>) {
        val sharedPreferences = this.getSharedPreferences("Bookmarks", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val json = gson.toJson(BookmarkList) // List를 JSON 문자열로 변환
        editor.putString("Bookmark", json)
        editor.apply()
    }

    fun loadBookmarkList(): HashSet<Notice> {
        val sharedPreferences = this.getSharedPreferences("Bookmarks", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("Bookmark", null) // 기본값은 null로 설정

        return if (json != null) {
            val type = object : com.google.gson.reflect.TypeToken<HashSet<Notice>>() {}.type
            gson.fromJson(json, type) // JSON 문자열을 ArrayList로 변환
        } else {
            hashSetOf() // 데이터가 없으면 빈 리스트 반환
        }
    }

    private fun noticeSearch() {
        Log.d("Notice Search", binding.noticeSearchEt.text.toString())
    }

    override fun onBackPressed() {
        if (binding.noticeSearchEt.visibility == View.VISIBLE) {
            binding.noticeSearchEt.visibility = View.GONE
            binding.noticeTabLayout.visibility = View.VISIBLE
        }
        else {
            super.onBackPressed()
        }
    }
}