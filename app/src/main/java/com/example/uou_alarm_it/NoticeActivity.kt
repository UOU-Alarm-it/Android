package com.example.uou_alarm_it

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_ENTER
import android.view.View
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import com.example.uou_alarm_it.databinding.ActivityNoticeBinding
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import com.launchdarkly.eventsource.ConnectStrategy
import com.launchdarkly.eventsource.EventSource
import com.launchdarkly.eventsource.background.BackgroundEventSource
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.URL
import java.util.concurrent.TimeUnit

class NoticeActivity : AppCompatActivity() {
    lateinit var binding: ActivityNoticeBinding
    lateinit var noticeRVAdapter: NoticeRVAdapter

    var eventSource: BackgroundEventSource? = null

    var bookmarkImportant: HashSet<Notice> = hashSetOf()
    var bookmarkCommon: HashSet<Notice> = hashSetOf()

    var keyWord: String = ""
    var major: String = "ICT융합학부" // 기본값

    lateinit var setting: Setting

    companion object {
        var category: Int = 1
        var noticeList: ArrayList<Notice> = arrayListOf()
        var bookmarkList: HashSet<Notice> = hashSetOf()
        const val REQUEST_CODE_MAJOR = 100
    }

    var isLast = false
    var page = 0
    var isLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoticeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 알림 링크 처리 (생략)
        var link = ""
        intent.extras?.let {
            link = it.getString("link") ?: ""
            if (link.isNotEmpty()) {
                Log.d("NoticeActivity", "알림 링크: $link")
                val webIntent = Intent(this, WebActivity::class.java).apply {
                    putExtra("url", link)
                }
                startActivity(webIntent)
            }
        }

        // 기존 저장된 Setting을 불러옵니다.
        setting = loadSetting()

        // SharedPreferences에서 "selected_major"와 최초 실행 플래그("isFirstNoticeRun")를 읽어옵니다.
        val sharedPref = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val spMajor = sharedPref.getString("selected_major", null)
        val isFirstRun = sharedPref.getBoolean("isFirstNoticeRun", true)
        val defaultMajor = "ICT융합학부"

        // Intent extra "major" 값
        val intentMajor = intent.getStringExtra("major")

        // 로그로 값 확인
        Log.d("NoticeActivity", "=== 초기 로그 ===")
        Log.d("NoticeActivity", "isFirstRun: $isFirstRun")
        Log.d("NoticeActivity", "SharedPreferences selected_major: $spMajor")
        Log.d("NoticeActivity", "Setting.notificationMajor: ${setting.notificationMajor}")
        Log.d("NoticeActivity", "Intent extra major: $intentMajor")

        // 최초 실행 시에만 Intent extra "major"를 적용하고 플래그를 false로 업데이트
        major = if (isFirstRun && !intentMajor.isNullOrEmpty() && setting.notificationMajor == defaultMajor) {
            sharedPref.edit().putString("selected_major", intentMajor)
                .putBoolean("isFirstNoticeRun", false)
                .apply()
            intentMajor
        } else {
            spMajor ?: setting.notificationMajor
        }

        // 업데이트된 major 값을 Setting에 반영하고 저장
        setting.notificationMajor = major
        saveSetting(setting)

        Log.d("NoticeActivity", "최종 major 값: $major")
        binding.noticeSelectedMajorTv.text = major

        // 즐겨찾기 리스트 불러오기 등 나머지 로직은 그대로 진행합니다.
        bookmarkList = loadBookmarkList()
        bookmarkList.filter { it.type == "NOTICE" }.toCollection(bookmarkImportant)
        bookmarkList.filter { it.type == "COMMON" }.toCollection(bookmarkCommon)
        bookmarkList = (bookmarkImportant + bookmarkCommon) as HashSet<Notice>

        initAllTab()

        binding.noticeTabAllIv.setOnClickListener { setCategory(1) }
        binding.noticeTabImportIv.setOnClickListener { setCategory(0) }
        binding.noticeTabBookmarkIv.setOnClickListener { setCategory(3) }

        binding.noticeSearchCl.setOnClickListener {
            if (binding.noticeSearchBarCl.visibility == View.GONE) {
                animSearch()
            } else {
                noticeSearch(binding.noticeSearchEt.text.toString())
                binding.noticeTabAllIv.setImageResource(R.drawable.btn_tab_all_on)
                binding.noticeTabImportIv.setImageResource(R.drawable.btn_tab_import_off)
                binding.noticeTabBookmarkIv.setImageResource(R.drawable.btn_tab_bookmark_off)
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                    .hideSoftInputFromWindow(binding.noticeSearchEt.windowToken, 0)
            }
        }

        binding.noticeSearchEt.setOnKeyListener { view, i, keyEvent ->
            if (keyEvent.action == KeyEvent.ACTION_DOWN && i == KEYCODE_ENTER) {
                noticeSearch(binding.noticeSearchEt.text.toString())
                binding.noticeTabAllIv.setImageResource(R.drawable.btn_tab_all_on)
                binding.noticeTabImportIv.setImageResource(R.drawable.btn_tab_import_off)
                binding.noticeTabBookmarkIv.setImageResource(R.drawable.btn_tab_bookmark_off)
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                    .hideSoftInputFromWindow(view.windowToken, 0)
                return@setOnKeyListener true
            }
            false
        }

        binding.noticeSearchEt.setTextCursorDrawable(R.drawable.edittext_cusor)

        binding.noticeSearchEt.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                if (query.isEmpty()) {
                    when (category) {
                        1 -> initAllTab()
                        0 -> initImportantTab()
                        3 -> {
                            noticeList = bookmarkList.toCollection(ArrayList())
                            initRV()
                            updateEmptyState()
                        }
                    }
                } else {
                    noticeSearch(query)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.noticeCloseSearchIv.setOnClickListener { animSearch() }

        initNotification()

        binding.noticeNoticeCl.setOnClickListener {
            setting.notificationSetting = !setting.notificationSetting
            setting.notificationMajor = major
            saveSetting(setting)
            initNotification()
        }
        binding.noticeSelectBtnLl.setOnClickListener {
            val intent = Intent(this, MajorActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_MAJOR)
        }
        updateEmptyState()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_MAJOR && resultCode == RESULT_OK) {
            val selectedText = data?.getStringExtra("selectedItem")
            Log.d("NoticeActivity", "Selected item: $selectedText")
            binding.noticeSelectedMajorTv.text = selectedText
            if (!selectedText.isNullOrEmpty()) {
                major = selectedText
                setting.notificationMajor = major
                saveSetting(setting)
                // 추가: SharedPreferences "selected_major" 값도 업데이트
                val sharedPref = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
                sharedPref.edit().putString("selected_major", major).apply()

                unConnectNotification()
                connectNotification()
            }
            setCategory(category)
        }
    }

    private fun initNotification() {
        if (setting.notificationSetting) {
            binding.noticeNoticeIv.setImageResource(R.drawable.notice_on)
            connectNotification()
        } else {
            binding.noticeNoticeIv.setImageResource(R.drawable.notice_off)
            unConnectNotification()
        }
    }

    private fun updateEmptyState() {
        if (noticeList.isEmpty()) {
            binding.noticeEmptyLogoIv.visibility = View.VISIBLE
            binding.noticeRv.visibility = View.GONE
        } else {
            binding.noticeEmptyLogoIv.visibility = View.GONE
            binding.noticeRv.visibility = View.VISIBLE
        }
    }

    private fun initAllTab() {
        page = 0
        noticeList.clear()
        RetrofitClient.service.getNotice(0, page++, major).enqueue(object : Callback<GetNoticeResponse> {
            override fun onResponse(call: Call<GetNoticeResponse>, response: Response<GetNoticeResponse>) {
                if (response.body()?.code == "COMMON200") {
                    val res = response.body()!!.result
                    noticeList.addAll(res.content)
                    initRV()
                    updateEmptyState()
                }
            }
            override fun onFailure(call: Call<GetNoticeResponse>, t: Throwable) {
                Log.e("retrofit", t.toString())
            }
        })
    }

    private fun initImportantTab() {
        page = 0
        noticeList.clear()
        RetrofitClient.service.getNotice(0, page++, major).enqueue(object : Callback<GetNoticeResponse> {
            override fun onResponse(call: Call<GetNoticeResponse>, response: Response<GetNoticeResponse>) {
                if (response.body()?.code == "COMMON200") {
                    val res = response.body()!!.result
                    noticeList.addAll(res.content.filter { it.type == "NOTICE" })
                    initRV()
                    updateEmptyState()
                }
            }
            override fun onFailure(call: Call<GetNoticeResponse>, t: Throwable) {
                Log.e("retrofit", t.toString())
            }
        })
    }

    private fun setCategory(category: Int) {
        binding.noticeTabAllIv.setImageResource(R.drawable.btn_tab_all_off)
        binding.noticeTabImportIv.setImageResource(R.drawable.btn_tab_import_off)
        binding.noticeTabBookmarkIv.setImageResource(R.drawable.btn_tab_bookmark_off)
        if (category != NoticeActivity.category) {
            noticeList = arrayListOf()
            page = 0
        }
        when (category) {
            1 -> {
                NoticeActivity.category = 1
                binding.noticeTabAllIv.setImageResource(R.drawable.btn_tab_all_on)
                initAllTab()
                updateEmptyState()
            }
            0 -> {
                NoticeActivity.category = 0
                binding.noticeTabImportIv.setImageResource(R.drawable.btn_tab_import_on)
                initImportantTab()
                updateEmptyState()
            }
            3 -> {
                NoticeActivity.category = 3
                binding.noticeTabBookmarkIv.setImageResource(R.drawable.btn_tab_bookmark_on)
                noticeList = bookmarkList.toCollection(ArrayList())
                Log.d("Bookmark", noticeList.toString())
                initRV()
                updateEmptyState()
            }
        }
    }

    fun initRV() {
        noticeRVAdapter = NoticeRVAdapter()
        binding.noticeRv.adapter = noticeRVAdapter
        noticeRVAdapter.setMyClickListener(object : NoticeRVAdapter.MyClickListener {
            override fun onItemClick(notice: Notice) {
                Log.d("test", "Item")
                val intent = Intent(this@NoticeActivity, WebActivity::class.java)
                intent.putExtra("url", notice.link)
                this@NoticeActivity.startActivity(intent)
            }
            override fun onBookmarkClick(notice: Notice) {
                Log.d("test", "Bookmark")
                if (notice in bookmarkList) {
                    bookmarkList.remove(notice)
                    if (notice.type == "NOTICE") {
                        bookmarkCommon.remove(notice)
                    } else {
                        bookmarkImportant.remove(notice)
                    }
                } else {
                    if (notice.type == "NOTICE") {
                        bookmarkCommon.add(notice)
                    } else {
                        bookmarkImportant.add(notice)
                    }
                    bookmarkList = (bookmarkImportant + bookmarkCommon) as HashSet<Notice>
                }
                noticeRVAdapter.bookmarkList = bookmarkList
                saveBookmarkList(bookmarkList)
                Log.d("Save Bookmark", bookmarkList.toString())
            }
        })
        binding.noticeRv.addOnScrollListener(object : OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (category == 4) {
                        if (!binding.noticeRv.canScrollVertically(-1)) {
                            Log.d("Paging", "Top of list")
                        } else if (!binding.noticeRv.canScrollVertically(1)) {
                            if (!isLoading) {
                                isLoading = true
                                noticeSearch(keyWord)
                            }
                        }
                    } else if (category != 3) {
                        if (!binding.noticeRv.canScrollVertically(-1)) {
                            Log.d("Paging", "Top of list")
                        } else if (!binding.noticeRv.canScrollVertically(1)) {
                            if (!isLoading) {
                                isLoading = true
                                RetrofitClient.service.getNotice(category, page++, major)
                                    .enqueue(object : Callback<GetNoticeResponse> {
                                        override fun onResponse(call: Call<GetNoticeResponse>, response: Response<GetNoticeResponse>) {
                                            isLoading = false
                                            if (response.body()?.code == "COMMON200") {
                                                val res = response.body()!!.result
                                                val newItems = if (category == 0) {
                                                    res.content.filter { it.type == "NOTICE" }
                                                } else {
                                                    res.content
                                                }
                                                noticeList.addAll(newItems)
                                                binding.noticeRv.adapter?.notifyDataSetChanged()
                                                updateEmptyState()
                                                Log.d("Paging", newItems.toString())
                                            }
                                        }
                                        override fun onFailure(call: Call<GetNoticeResponse>, t: Throwable) {
                                            isLoading = false
                                            Log.e("retrofit", t.toString())
                                        }
                                    })
                            }
                        }
                    }
                }
            }
        })
        binding.noticeRv.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
    }

    fun saveBookmarkList(BookmarkList: HashSet<Notice>) {
        val sharedPreferences = this.getSharedPreferences("Bookmarks", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val json = gson.toJson(BookmarkList)
        editor.putString("Bookmark", json)
        editor.apply()
    }

    fun loadBookmarkList(): HashSet<Notice> {
        val sharedPreferences = this.getSharedPreferences("Bookmarks", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("Bookmark", null)
        return if (json != null) {
            val type = object : com.google.gson.reflect.TypeToken<HashSet<Notice>>() {}.type
            gson.fromJson(json, type)
        } else {
            hashSetOf()
        }
    }

    private fun noticeSearch(keyword: String) {
        Log.d("Notice Search", keyword)
        if (keyWord != keyword) {
            noticeList = arrayListOf()
            keyWord = keyword
            page = 0
            initRV()
        }
        if (keyword.isEmpty()) {
            when (NoticeActivity.category) {
                1 -> initAllTab()
                0 -> initImportantTab()
                3 -> {
                    noticeList = bookmarkList.toCollection(ArrayList())
                    initRV()
                    updateEmptyState()
                }
            }
            return
        }
        if (NoticeActivity.category == 3) {
            val filtered = bookmarkList.filter {
                it.title.contains(keyword, ignoreCase = true)
            }
            noticeList = filtered.toCollection(ArrayList())
            initRV()
            updateEmptyState()
            return
        }
        if (isLoading) return
        isLoading = true
        RetrofitClient.service.getSearch(keyword, major, page++).enqueue(object : Callback<GetNoticeResponse> {
            override fun onResponse(call: Call<GetNoticeResponse>, response: Response<GetNoticeResponse>) {
                isLoading = false
                if (response.body()?.code == "COMMON200") {
                    val res = response.body()!!.result
                    val newItems = if (NoticeActivity.category == 0) {
                        res.content.filter { it.type == "NOTICE" }
                    } else {
                        res.content
                    }
                    noticeList.addAll(newItems)
                    binding.noticeRv.adapter?.notifyDataSetChanged()
                    updateEmptyState()
                }
            }
            override fun onFailure(call: Call<GetNoticeResponse>, t: Throwable) {
                isLoading = false
                Log.e("retrofit", t.toString())
            }
        })
    }

    override fun onBackPressed() {
        if (binding.noticeSearchEt.visibility == View.VISIBLE) {
            animSearch()
        } else {
            super.onBackPressed()
        }
    }

    private fun animSearch() {
        if (binding.noticeSearchBarCl.visibility == View.VISIBLE) {
            val animation = AnimationUtils.loadAnimation(this, R.anim.anim_search_close)
            animation.setAnimationListener(object : AnimationListener {
                override fun onAnimationStart(p0: Animation?) {
                    binding.noticeCloseSearchIv.visibility = View.GONE
                    binding.noticeCloseSearchCl.visibility = View.GONE
                    binding.noticeSearchIv.visibility = View.VISIBLE
                    binding.noticeSearchCl.visibility = View.VISIBLE
                }
                override fun onAnimationEnd(p0: Animation?) {
                    binding.noticeSearchBarCl.visibility = View.GONE
                    binding.noticeTabLl.visibility = View.VISIBLE
                    binding.noticeLineView.visibility = View.VISIBLE
                    binding.noticeSearchEt.setText("")
                    if (category == 4) {
                        setCategory(1)
                    }
                }
                override fun onAnimationRepeat(p0: Animation?) {}
            })
            binding.noticeSearchEt.startAnimation(animation)
            Log.d("anim", "close")
        } else {
            val animation = AnimationUtils.loadAnimation(this, R.anim.anim_search_open)
            animation.setAnimationListener(object : AnimationListener {
                override fun onAnimationStart(p0: Animation?) {
                    binding.noticeSearchBarCl.visibility = View.VISIBLE
                    binding.noticeCloseSearchIv.visibility = View.VISIBLE
                    binding.noticeCloseSearchCl.visibility = View.VISIBLE
                    binding.noticeSearchIv.visibility = View.GONE
                    binding.noticeSearchCl.visibility = View.GONE
                    binding.noticeTabLl.visibility = View.GONE
                    binding.noticeLineView.visibility = View.GONE
                }
                override fun onAnimationEnd(p0: Animation?) {}
                override fun onAnimationRepeat(p0: Animation?) {}
            })
            binding.noticeSearchBarCl.visibility = View.VISIBLE
            binding.noticeSearchBarCl.startAnimation(animation)
            Log.d("anim", "open")
        }
    }

    private fun getFcmToken(){
        var token = ""
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "FCM 토큰 가져오기 실패", task.exception)
                return@addOnCompleteListener
            } else {
                token = task.result.toString()
                Log.d("FCM", "FCM 토큰: $token")
                RetrofitClient.service.postFCMRegister(token, major).enqueue(object: Callback<PostFCMRegisterResponse>{
                    override fun onResponse(
                        call: Call<PostFCMRegisterResponse>,
                        response: Response<PostFCMRegisterResponse>
                    ) {
                        Log.d("FCM", "FCM 연결 성공")
                    }
                    override fun onFailure(call: Call<PostFCMRegisterResponse>, t: Throwable) {
                        Log.e("FCM", "FCM 연결 실패" + t)
                    }
                })
            }
        }
    }
    private fun deleteFcmToken() {
        var token = ""
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "FCM 토큰 가져오기 실패", task.exception)
                return@addOnCompleteListener
            } else {
                token = task.result.toString()
                Log.d("FCM", "FCM 토큰: $token")
                RetrofitClient.service.deleteFCMUnregister(token, major).enqueue(object: Callback<PostFCMRegisterResponse>{
                    override fun onResponse(
                        call: Call<PostFCMRegisterResponse>,
                        response: Response<PostFCMRegisterResponse>
                    ) {
                        Log.d("FCM", "FCM 연결 해제 성공")
                    }
                    override fun onFailure(call: Call<PostFCMRegisterResponse>, t: Throwable) {
                        Log.e("FCM", "FCM 연결 해제 실패" + t)
                    }
                })
            }
        }
    }
    private fun connectNotification() {
        eventSource = BackgroundEventSource.Builder(
            SSEService(this),
            EventSource.Builder(
                ConnectStrategy.http(URL("https://alarm-it.ulsan.ac.kr:58080/notification/subscribe?major=$major"))
                    .connectTimeout(3, TimeUnit.SECONDS)
                    .readTimeout(600, TimeUnit.SECONDS)
            )
        )
            .threadPriority(Thread.MAX_PRIORITY)
            .build()
        eventSource?.start()
        getFcmToken()
    }
    private fun unConnectNotification() {
        try {
            eventSource?.close()
        } catch (e: Exception) {
            Log.e("SSEService", "오류 발생: ${e.message}")
        } finally {
            eventSource = null
        }
        deleteFcmToken()
    }
    fun saveSetting(setting: Setting) {
        val sharedPreferences = this.getSharedPreferences("Setting", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val json = gson.toJson(setting)
        editor.putString("Setting", json)
        editor.apply()
    }
    fun loadSetting(): Setting {
        val sharedPreferences = this.getSharedPreferences("Setting", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("Setting", null)
        return if (json != null) {
            gson.fromJson(json, Setting::class.java)
        } else {
            Setting(true, "ICT융합학부")
        }
    }
}