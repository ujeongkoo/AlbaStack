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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.lang.Math.abs
import java.text.SimpleDateFormat

class InfoPlusFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private var selectedYear: Int = 0
    private var selectedMonth: Int = 0

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

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

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

        val buttonSubmit: Button = view.findViewById(R.id.btn_work_submit)
        buttonSubmit.setOnClickListener {
            submitWorkDuration()
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

                selectedYear = datePicker.year
                selectedMonth = datePicker.month

                textView.text = "${year}년 ${month+1}월 ${dayOfMonth}일"
                Toast.makeText(requireContext(), "날짜 설정 완료", Toast.LENGTH_SHORT).show()

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

                textView.text = "${hour}시 ${minute}분"
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

                textView.text = "${hour}시 ${minute}분"
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.cancel()
            }

        builder.create().show()
    }

    private fun submitWorkDuration() {
        val attendanceTextView: TextView = view?.findViewById(R.id.attendance_time)!!
        val attendanceTime = attendanceTextView.text.toString()

        val leaveTextView: TextView = view?.findViewById(R.id.leave_time)!!
        val leaveTime = leaveTextView.text.toString()

        val workDuration = calculateWorkDuration(attendanceTime, leaveTime)

        val dateTextView: TextView = view?.findViewById(R.id.date_time)!!
        val date = dateTextView.text.toString()

        saveWorkDuration(date, workDuration)
    }

    private fun calculateWorkDuration(attendanceTime: String, leaveTime: String): String {
        val dateFormat = SimpleDateFormat("hh시 mm분")
        val attendanceDate = dateFormat.parse(attendanceTime)
        val leaveDate = dateFormat.parse(leaveTime)

        val durationInMills = abs(leaveDate.time - attendanceDate.time)
        val hours = durationInMills / (60 * 60 * 1000) % 24
        val minutes = durationInMills / (60 * 1000) % 60


        return "${hours}시간 ${minutes}분"
    }

    private fun saveWorkDuration(date: String, workDuration: String) {
        val userId = auth.currentUser?.uid.toString()
        val dateRef = firestore.collection("users").document(userId)
            .collection("dates").document(date)

        val data = hashMapOf("workDuration" to workDuration)

        dateRef.set(data)
            .addOnSuccessListener { documentReference ->
                Log.d("InfoPlusFragment", "근무 시간 저장 성공")

                val dateTextView: TextView = view?.findViewById(R.id.date_time)!!
                dateTextView.text = "날짜를 입력해주세요"

                val attendaceTextView: TextView = view?.findViewById(R.id.attendance_time)!!
                attendaceTextView.text = "시간을 입력해주세요"

                val leaveTextView: TextView = view?.findViewById(R.id.leave_time)!!
                leaveTextView.text="시간을 입력해주세요"

                Toast.makeText(requireContext(), "${workDuration} 일하셨군요!", Toast.LENGTH_SHORT).show()

                IndividualSaveWorkDuration(workDuration)
            }
            .addOnFailureListener { e ->
                Log.e("InfoPlusFragment", "근무 시간 저장 실패", e)
            }

    }

    private fun IndividualSaveWorkDuration(workDuration: String) {
        val userId = auth.currentUser?.uid.toString()
        val dataRef = firestore.collection("users").document(userId)
            .collection("total_workDuration").document("${selectedYear}년 ${selectedMonth + 1}월 근무시간")

        dataRef.get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val currentworkDuration = Regex("""(\d+)시간 (\d+)분""").find(task.result.toString())
                    val newworkDuration = Regex("""(\d+)시간 (\d+)분""").find(workDuration)

                    Log.d("InfoPlusFragment", "${currentworkDuration}")

                    if (currentworkDuration == null && newworkDuration != null) {
                        val first_currentHours = 0
                        val first_currentMinutes = 0

                        val first_newHours = newworkDuration.groupValues[1].toInt()
                        val first_newMinutes = newworkDuration.groupValues[2].toInt()

                        val first_totalHours = first_currentHours + first_newHours
                        val first_totalMinutes = first_currentMinutes + first_newMinutes

                        val first_storeHours = first_totalHours + first_totalMinutes / 60
                        val first_storeMinutes = first_totalMinutes % 60

                        val storeWorkDuration = "${first_storeHours}시간 ${first_storeMinutes}분"
                        val first_finalWorkDuration =
                            hashMapOf("${selectedYear}년 ${selectedMonth + 1}월 근무시간" to storeWorkDuration)

                        dataRef.set(first_finalWorkDuration)
                            .addOnSuccessListener { documentReference ->
                                Log.d("InfoPlusFragment", "총 근무시간 저장 성공")
                            }
                            .addOnFailureListener { e ->
                                Log.e("InfoPlusFragment", "총 근무 시간 저장 실패: ${e.message}")
                            }

                    } else if (currentworkDuration != null && newworkDuration != null) {
                        val currentHours = currentworkDuration.groupValues[1].toInt()
                        val currentMinutes = currentworkDuration.groupValues[2].toInt()

                        val newHours = newworkDuration.groupValues[1].toInt()
                        val newMinutes = newworkDuration.groupValues[2].toInt()

                        val totalHours = currentHours + newHours
                        val totalMinutes = currentMinutes + newMinutes

                        val storeHours = totalHours + totalMinutes / 60
                        val storeMinutes = totalMinutes % 60

                        val storeWorkDuration = "${storeHours}시간 ${storeMinutes}분"
                        val finalWorkDuration =
                            hashMapOf("${selectedYear}년 ${selectedMonth + 1}월 근무시간" to storeWorkDuration)

                        dataRef.set(finalWorkDuration)
                            .addOnSuccessListener { documentReference ->
                                Log.d("InfoPlusFragment", "총 근무시간 저장 성공")
                            }
                            .addOnFailureListener { e ->
                                Log.e("InfoPlusFragment", "총 근무 시간 저장 실패: ${e.message}")
                            }
                    }
                } else {
                    Log.d("InfoPlusFragment", "실패")
                }
            }

    }

}