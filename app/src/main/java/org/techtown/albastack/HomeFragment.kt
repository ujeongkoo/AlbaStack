package org.techtown.albastack

import android.app.AlertDialog
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class HomeFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var datePicker: DatePicker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_home, container, false)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val storeNameTextView: TextView = view.findViewById(R.id.calendar_store_name)
        setStoreNameToTextView(storeNameTextView)

        datePicker = view.findViewById(R.id.store_datePicker)
        val currentDate = Calendar.getInstance()
        datePicker.init(
            currentDate.get(Calendar.YEAR),
            currentDate.get(Calendar.MONTH),
            currentDate.get(Calendar.DAY_OF_MONTH)
        ) { _, year, month, day ->
            CoroutineScope(Dispatchers.Main).launch {
                val storeName = showStoreName().toString()
                showStoreTime(storeName)
            }
        }

        return view
    }

    private fun setStoreNameToTextView(textView: TextView) {
        val currentUser = auth.currentUser
        val userId = currentUser?.uid

        userId.let {
            val userRef = it?.let{ it1 -> firestore.collection("users").document(it1) }

            userRef?.get()?.addOnCompleteListener { task ->
                if(task.isSuccessful) {
                    val document: DocumentSnapshot? = task.result
                    if (document != null && document.exists()) {
                        val storeName = document.getString("storeName")

                        storeName?.let {
                            textView.text = "$it 달력"
                        }
                    }
                }
            }
        }
    }

    private suspend fun showStoreName(): String? {
        val currentUser = auth.currentUser

        return if (currentUser != null) {
            val userId = currentUser.uid

            try {
                val userDocumentSnapshot = firestore.collection("users")
                    .document(userId)
                    .get()
                    .await()

                userDocumentSnapshot.getString("storeName")
            } catch (exception: Exception) {
                println("사용자 데이터 가져오기 실패: $exception")
                null
            }
        } else {
            println("사용자가 인증되지 않았습니다.")
            null
        }
    }

    private fun showStoreTime(storeName: String) {
        val day = datePicker.dayOfMonth
        val month = datePicker.month
        val year = datePicker.year

        val builder = AlertDialog.Builder(requireContext())

        val scrollView = ScrollView(requireContext())
        scrollView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )

        val linearLayout = LinearLayout(requireContext())
        linearLayout.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        linearLayout.orientation = LinearLayout.VERTICAL

        val showSelectedDay = TextView(requireContext())

        val layoutParams = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(20, 20, 20, 20)
        showSelectedDay.layoutParams = layoutParams
        showSelectedDay.text="${year}년 ${month + 1}월 ${day}일"
        showSelectedDay.textSize = 20f
        showSelectedDay.setTypeface(null, Typeface.BOLD)

        linearLayout.addView(showSelectedDay)

        printUserworkDuration(storeName, year, month, day, linearLayout)

        scrollView.addView(linearLayout)

        builder.setView(scrollView)

        builder.setNegativeButton("확인") { dialog, _ ->
            dialog.cancel()
        }

        builder.create().show()


    }

    private fun printUserworkDuration(storeName: String, year: Int, month: Int, dayOfMonth: Int, linearLayout: LinearLayout) {

        val userRef = firestore.collection("users")
        userRef.whereEqualTo("storeName", storeName)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.d("HomeFragment", "해당 storeName을 가진 사용자를 찾을 수 없습니다.")
                    return@addOnSuccessListener
                }
                for (document in documents) {
                    val userName = document.getString("name")
                    val userId = document.id

                    val formattedDate = "${year}년 ${month + 1}월 ${dayOfMonth}일"
                    val datesRef = firestore.collection("users").document(userId)
                        .collection("dates").document(formattedDate)

                    datesRef.get()
                        .addOnSuccessListener { dateDocument ->
                            if(dateDocument.exists()) {
                                val workDuration = dateDocument.getString("workDuration")
                                if (workDuration != null) {
                                    val cardView = showUserworkDuration(workDuration, userName)
                                    linearLayout.addView(cardView)
                                }
                            }
                        }
                }
            }
            .addOnFailureListener { e: Exception ->
                Log.e("HomeFragment", "오류 발생: ${e}")
            }

        }

    private fun showUserworkDuration(workDuration: String, userName: String?): CardView {
        val cardView = CardView(requireContext())

        val layoutParams = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(20, 20, 20, 20)
        cardView.layoutParams = layoutParams
        cardView.cardElevation = 20f
        cardView.radius = 50f
        cardView.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.gray))

        val linearLayout = LinearLayout(requireContext())
        linearLayout.orientation = LinearLayout.VERTICAL
        linearLayout.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        linearLayout.setPadding(16, 16, 16, 16)

        val cardView_storeName = TextView(requireContext())
        cardView_storeName.text="${userName}님"
        cardView_storeName.textSize = 20f
        cardView_storeName.setTypeface(null, Typeface.BOLD)

        linearLayout.addView(cardView_storeName)

        val storeTime = TextView(requireContext())
        storeTime.text="${userName}의 근무시간: ${workDuration}"
        storeTime.textSize = 18f

        linearLayout.addView(storeTime)

        cardView.addView(linearLayout)

        return cardView
    }

}

