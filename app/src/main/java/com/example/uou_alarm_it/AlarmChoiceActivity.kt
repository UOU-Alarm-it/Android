package com.example.uou_alarm_it

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.uou_alarm_it.databinding.ActivityAlarmChoiceBinding
import com.example.uou_alarm_it.databinding.ItemAlarmChoiceBinding
import com.example.uou_alarm_it.databinding.ItemAlarmChoiceCollegeBinding

class AlarmChoiceActivity: AppCompatActivity() {
    lateinit var binding: ActivityAlarmChoiceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlarmChoiceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val collegeRVAdapter = CollegeRVAdapter()
        binding.alarmChoiceRv.adapter = collegeRVAdapter
        binding.alarmChoiceRv.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
    }

    inner class CollegeRVAdapter(): RecyclerView.Adapter<CollegeRVAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollegeRVAdapter.ViewHolder {
            val collegeBinding = ItemAlarmChoiceCollegeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(collegeBinding)
        }

        override fun onBindViewHolder(holder: CollegeRVAdapter.ViewHolder, collegePos: Int) {
            holder.bind(CollegesList.collegesList[collegePos])
        }

        override fun getItemCount(): Int = CollegesList.collegesList.size

        inner class ViewHolder(private val collegeBinding: ItemAlarmChoiceCollegeBinding): RecyclerView.ViewHolder(collegeBinding.root) {
            fun bind(college: CollegesList.College) {
                collegeBinding.itemAlarmChoiceCollegeTitle.text = college.name


                val majorRVAdapter = MajorRVAdapter(college.majors)
                collegeBinding.itemAlarmChoiceCollegeRv.adapter = majorRVAdapter
                collegeBinding.itemAlarmChoiceCollegeRv.layoutManager = LinearLayoutManager(this@AlarmChoiceActivity, LinearLayoutManager.VERTICAL, false)
            }

            inner class MajorRVAdapter(majors: Array<String>): RecyclerView.Adapter<MajorRVAdapter.ViewHolder>() {
                val majors = majors
                override fun onCreateViewHolder(
                    parent: ViewGroup,
                    viewType: Int
                ): MajorRVAdapter.ViewHolder {
                    val majorBinding = ItemAlarmChoiceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                    return ViewHolder(majorBinding)
                }

                override fun onBindViewHolder(holder: MajorRVAdapter.ViewHolder, position: Int) {
                    holder.bind(majors[position])
                }

                override fun getItemCount(): Int = majors.size

                inner class ViewHolder(private val majorBinding: ItemAlarmChoiceBinding): RecyclerView.ViewHolder(majorBinding.root) {
                    fun bind(major: String) {
                        majorBinding.itemAlarmChoiceTitle.text = major
                    }
                }
            }
        }
    }
}