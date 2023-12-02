package org.techtown.albastack

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.DatePicker
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast

class InfoPlusFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_info_plus, container, false)

        val buttonDatePicker: Button = view.findViewById(R.id.btn_date)
        buttonDatePicker.setOnClickListener {
            val InfoDate: TextView = view.findViewById(R.id.date_time)
            showDatePicker(InfoDate)
        }

        val buttonAttendance: Button = view.findViewById(R.id.btn_attendance)
        buttonAttendance.setOnClickListener {
            val InfoAttendace: TextView = view.findViewById(R.id.attendance_time)
            showAttendancePicker(InfoAttendace)
        }

        val buttonLeave: Button = view.findViewById(R.id.btn_leave)
        buttonLeave.setOnClickListener {
            val InfoLeave: TextView = view.findViewById(R.id.leave_time)
            showLeavePicker(InfoLeave)
        }


        return view
    }

    private fun showDatePicker(textView: TextView) {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.popup_datepicker, null)

        val datePicker: DatePicker = dialogView.findViewById(R.id.popup_datePicker)

        builder.setView(dialogView)
            .setPositiveButton("설정") { dialog, _ ->
                val year = datePicker.year
                val month = datePicker.month
                val dayOfMonth = datePicker.dayOfMonth

                textView.text = "날짜: ${year}년 ${month+1}월 ${dayOfMonth}일"
                Toast.makeText(requireContext(), "날짜 설정 완료", Toast.LENGTH_SHORT).show()
                Log.d("InfoPlusFragement", "${year}/${month+1}/${dayOfMonth}")

            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.cancel()
            }

        builder.create().show()
    }

    private fun showAttendancePicker(textView: TextView) {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.popup_timepicker, null)

        val timePicker: TimePicker = dialogView.findViewById(R.id.popup_timePicker)

        builder.setView(dialogView)
            .setPositiveButton("설정") { dialog, _ ->
                val hour =  timePicker.hour
                val minute = timePicker.minute

                if( hour > 12 ) {
                    textView.text = "출근 시간: 오후 ${hour - 12}시 ${minute}분"
                } else {
                    textView.text = "출근 시간: 오전 ${hour}시 ${minute}분"
                }
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.cancel()
            }

        builder.create().show()
    }

    private fun showLeavePicker(textView: TextView) {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.popup_timepicker, null)

        val timePicker: TimePicker = dialogView.findViewById(R.id.popup_timePicker)

        builder.setView(dialogView)
            .setPositiveButton("설정") { dialog, _ ->
                val hour =  timePicker.hour
                val minute = timePicker.minute

                if( hour > 12 ) {
                    textView.text = "퇴근 시간: 오후 ${hour - 12}시 ${minute}분"
                } else {
                    textView.text = "퇴근 시간: 오전 ${hour}시 ${minute}분"
                }
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.cancel()
            }

        builder.create().show()
    }

}