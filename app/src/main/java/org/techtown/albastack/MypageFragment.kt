package org.techtown.albastack

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import java.text.SimpleDateFormat
import java.util.Date


class MypageFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private lateinit var imageViewUser: ImageView
    private lateinit var spinnerStore: Spinner
    private lateinit var spinnerMonth: Spinner
    private lateinit var storeEditText: EditText
    private lateinit var hourPay: EditText
    private lateinit var monthPay: TextView
    private lateinit var btnAddStore: Button
    private lateinit var btnMonthPay: Button
    private lateinit var btnMemberCheck: Button


    private lateinit var storeList: ArrayList<String>
    private lateinit var storeAdapter: ArrayAdapter<String>

    private lateinit var monthList: ArrayList<String>

    private var pickImageFromAlbum = 0
    private var uriPhoto : Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_mypage, container, false)

        // user_image ImageView 초기화
        imageViewUser = view.findViewById(R.id.user_image)
        // FirebaseAuth 및 FirebaseFirestore 인스턴스 초기화
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // 사용자 이름을 가져와서 TextView에 설정
        val mypageNameTextView: TextView = view.findViewById(R.id.mypage_name)
        setUserNameToTextView(mypageNameTextView)

        val mypageProfileImageView: ImageView = view.findViewById(R.id.user_image)
        setUserImageToImageView(mypageProfileImageView)

        val buttonChooseImage: Button = view.findViewById(R.id.btn_image_change)
        buttonChooseImage.setOnClickListener {
            var photoPickerIntent = Intent(Intent.ACTION_PICK)
            photoPickerIntent.type = "image/*"
            startActivityForResult(photoPickerIntent, pickImageFromAlbum)
        }

        val buttonMonthPay: Button = view.findViewById(R.id.btn_paycheck)
        buttonMonthPay.setOnClickListener {
            try {
                showpayCheck()
            } catch(e: Exception) {
                Log.e("MyPageFragment", "${e.message}")
            }
        }

        val buttonSetStore: Button = view.findViewById(R.id.btn_setstore)
        buttonSetStore.setOnClickListener {
            try {
            showSetStore()
            } catch(e: Exception) {
                Log.e("MyPageFragment", "${e.message}")
            }
        }

        btnMemberCheck = view.findViewById(R.id.btn_worktime)
        val userId = auth.currentUser?.uid.toString()
        val userRef = firestore.collection("users").document(userId)

        userRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val document: DocumentSnapshot? = task.result
                if (document != null && document.exists()) {
                    val userJob = document.getString("selected_job")
                    Log.d("MypageFragment", "${userJob}")

                    if (userJob == "자영업자") {
                        btnMemberCheck.visibility = View.VISIBLE
                        btnMemberCheck.setOnClickListener {
                            try {
                                userRef.get()
                                    .addOnSuccessListener { document ->
                                        if (document.exists()) {
                                            val storeName = document.getString("storeName").toString()
                                            popupMember(storeName)
                                        }
                                    }
                                    .addOnFailureListener { exception ->
                                        Log.e("MypageFragment", "에러 발생", exception)
                                    }
                            } catch(e: Exception) {
                                Log.e("MyPageFragment", "${e.message}")
                            }
                        }
                    }
                }
            }
        }

        // mypage_logout TextView 초기화
        val logoutTextView: TextView = view.findViewById(R.id.btn_logout)

        logoutTextView.setOnClickListener {
            logout()
        }

        val withdrawTextView: TextView = view.findViewById(R.id.btn_withdrawal)

        withdrawTextView.setOnClickListener {
            showWithdrawPopup()
        }

        return view
    }

    // 로그아웃 함수
    private fun logout() {
        auth.signOut()
        val intent = Intent(requireContext(), LoginActivity::class.java)
        startActivity(intent)
    }

    private fun showpayCheck() {

        val builder = AlertDialog.Builder(requireContext())
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.popup_paycheck, null)

        spinnerMonth = dialogView.findViewById(R.id.spinner_paycheck_month)
        hourPay = dialogView.findViewById(R.id.hourpay_edittext)
        monthPay = dialogView.findViewById(R.id.paycheck_textView)
        btnMonthPay = dialogView.findViewById(R.id.btn_month_calculate)

        val monthArray = resources.getStringArray(R.array.monthList)
        monthList = ArrayList(monthArray.toList())

        val monthAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, monthList)
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMonth.adapter = monthAdapter
        spinnerMonth.setSelection(0)

        spinnerMonth.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                btnMonthPay.setOnClickListener {
                    val selectedMonth = spinnerMonth.selectedItem.toString()
                    val userId = auth.currentUser?.uid.toString()
                    val dataRef = firestore.collection("users").document(userId)
                        .collection("total_workDuration").document("2023년 ${selectedMonth}월 근무시간")

                    dataRef.get().addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val workDuration =
                                Regex("""(\d+)시간 (\d+)분""").find(task.result.toString())
                            val inputPay = hourPay.text.toString()
                            val final_inputPay = inputPay.toIntOrNull() ?: 0

                            if (workDuration != null) {
                                val minute = workDuration.groupValues[2].toInt()
                                if (minute >= 30) {
                                    val hour = workDuration.groupValues[1].toInt() + 1

                                    calculatePay(final_inputPay, hour, monthPay)
                                } else {
                                    val hour = workDuration.groupValues[1].toInt()

                                    calculatePay(final_inputPay, hour, monthPay)
                                }
                            } else {
                                Log.d("PayCalculation", "${selectedMonth}")
                            }
                        }
                    }
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
            }
        }

        builder.setView(dialogView)
            .setNegativeButton("취소") { dialog, _ ->
                dialog.cancel()
            }

        builder.create().show()
    }

    private fun calculatePay(final_inputPay: Int, hour: Int, monthPay: TextView) {
        val finalPay = final_inputPay * hour
        Log.d("PayCalculation", "Final Pay: $finalPay")
        monthPay.text = "이번달 월급은 ${finalPay}원 입니다."
    }

    private fun showSetStore() {

        val builder = AlertDialog.Builder(requireContext())
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.popup_setstore, null)

        spinnerStore = dialogView.findViewById(R.id.spinner_store)
        storeEditText = dialogView.findViewById(R.id.spinner_store_name_editext)
        btnAddStore = dialogView.findViewById(R.id.btn_store_name_add)

        initStoreSpinner()

        spinnerStore.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val selectedStore = spinnerStore.selectedItem.toString()
                if (selectedStore == "직접 입력") {
                    storeEditText.visibility = View.VISIBLE
                    btnAddStore.visibility = View.VISIBLE
                } else {
                    storeEditText.visibility = View.GONE
                    btnAddStore.visibility = View.GONE
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
            }

        }

        btnAddStore.setOnClickListener {
            val newStoreName = storeEditText.text.toString().trim()
            if(newStoreName.isNotEmpty()) {
                addStoreToFirebase(newStoreName)
                loadStore()
                storeEditText.text.clear()
            }
        }

        builder.setView(dialogView)
            .setPositiveButton("설정하기") { dialog, _ ->
                val storeName = spinnerStore.selectedItem.toString()
                val userId = auth.currentUser?.uid.toString()

                // Spinner에서 선택한 가게이름 users에 추가하기
                addStoreName(storeName, userId)
                addMemberName(storeName)

            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.cancel()
            }

        builder.create().show()
    }

    private fun addStoreName(storeName: String, userId: String) {
        val userDocument = firestore.collection("users").document(userId)

        userDocument.update("storeName", storeName)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "가게 멤버가 되셨습니다!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "멤버 되기에 실패하셨습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addMemberName (storeName: String) {

        val userId = auth.currentUser?.uid.toString()
        val nameRef = firestore.collection("users").document(userId)
        val storeRef = firestore.collection("stores").document(storeName)
        nameRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val document: DocumentSnapshot = task.result
                val userName = document.getString("name")
                if (userName != null) {
                    storeRef.update("storeMember", userName)
                        .addOnSuccessListener {
                            Log.d("MypageFragment", "멤버 업데이트 성공")
                        }
                }
            }
        }
    }


    private fun initStoreSpinner() {
        storeList = ArrayList()
        storeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, storeList)
        storeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStore.adapter = storeAdapter

        loadStore()

    }

    private fun loadStore() {

        val storesRef = firestore.collection("stores")

        storesRef.get().addOnSuccessListener { result ->
            for (document in result) {
                val storeName = document.getString("name")
                storeName?.let { storeList.add(it) }
            }

            storeList.add("직접 입력")
            storeAdapter.notifyDataSetChanged()
        } .addOnFailureListener { exception ->
            Toast.makeText(requireContext(), "가게 목록을 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
        }

    }

    private fun addStoreToFirebase(newStoreName: String) {

        val store = hashMapOf("name" to newStoreName)

        firestore.collection("stores")
            .add(store)
            .addOnCompleteListener { task ->
                if(task.isSuccessful) {
                    Toast.makeText(requireContext(), "가게가 추가되었습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "가게 추가가 실패하였습니다.", Toast.LENGTH_SHORT).show()
                }
            }
    }


    private fun showWithdrawPopup() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("회원 탈퇴")
        builder.setMessage("정말로 떠나시겠습니까?")

        builder.setPositiveButton("확인") { dialogInterface: DialogInterface, i: Int ->
            withdraw()
        }

        builder.setNegativeButton("취소") { dialogInterface: DialogInterface, i: Int ->
            dialogInterface.dismiss()
        }

        builder.show()
    }

    private fun withdraw() {
        val currentUser = auth.currentUser

        val builder = AlertDialog.Builder(requireContext())
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.popup_withdraw, null)

        val emailEditText = dialogView.findViewById<EditText>(R.id.popup_email)
        val passwordEditText = dialogView.findViewById<EditText>(R.id.popup_password)

        builder.setView(dialogView)
            .setPositiveButton("회원탈퇴") { dialog, _ ->
                val userEmail = auth.currentUser?.email
                val inputEmail = emailEditText.text.toString()
                val inputPassword = passwordEditText.text.toString()

                if (userEmail == inputEmail) {
                    val credential = EmailAuthProvider.getCredential(inputEmail, inputPassword)

                    currentUser?.reauthenticate(credential)
                        ?.addOnCompleteListener { reauthTask ->
                            if (reauthTask.isSuccessful) {
                                currentUser?.delete()

                                deleteFirestoreData(currentUser.uid)
                                deleteStorageData(currentUser.uid)

                                val intent = Intent(requireContext(), LoginActivity::class.java)
                                startActivity(intent)
                                requireActivity().finish()
                                Toast.makeText(requireContext(), "안녕히가세요!", Toast.LENGTH_SHORT).show()
                            } else {
                                Log.e("MypageFragment", "재인증 실패", reauthTask.exception)
                            }
                            dialog.dismiss()

                        }
                }
            }
            .setNegativeButton("취소") {dialog, _ ->
                dialog.cancel()
            }

        builder.create().show()
    }

    private fun deleteFirestoreData(userId: String) {
        // Firestore에서 사용자 데이터 삭제
        firestore.collection("users").document(userId)
            .delete()
            .addOnSuccessListener {
                Log.d("MypageFragment", "사용자 데이터 삭제 성공")
            }
            .addOnFailureListener {
                Log.d("MypageFragment", "사용자 데이터 삭제 실패")
            }
    }

    private fun deleteStorageData(userId: String) {
        val storageRef = storage.reference.child("images/$userId")

        storageRef.listAll()
            .addOnSuccessListener { result ->
                for (item in result.items) {
                    item.delete()
                        .addOnSuccessListener {
                            Log.d("MypageFragment", "사용자 이미지 삭제 성공")
                        }
                        .addOnFailureListener {
                            Log.d("MypageFragment", "사용자 이미지 삭제 실패")
                        }
                }
            }
            .addOnFailureListener {
                Log.d("MypageFragment", "파일 목록 가져오기 실패")
            }
    }

    private fun setUserNameToTextView(textView: TextView) {
        // 현재 로그인한 사용자의 UID 가져오기
        val currentUser = auth.currentUser
        val userId = currentUser?.uid


        // 사용자의 이름을 가져와서 TextView에 설정
        userId.let {
            val userRef = it?.let { it1 -> firestore.collection("users").document(it1) }

            userRef?.get()?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val document: DocumentSnapshot? = task.result
                    if (document != null && document.exists()) {
                        // Cloud Firesotre에서 사용자 이름 가져오기
                        val userName = document.getString("name")

                        // TextView에서 사용자 이름 설정
                        userName?.let {
                            textView.text = "${it}님 안녕하세요!"
                        }
                    }
                }
            }
        }
    }

    private fun setUserImageToImageView(imageView: ImageView) {
        loadProfileImage(imageView)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == pickImageFromAlbum && resultCode == Activity.RESULT_OK) {
            uriPhoto = data?.data
            imageViewUser.setImageURI(uriPhoto)

            uploadProfileImage()
        }
    }

    private fun uploadProfileImage() {
        val userId = auth.currentUser?.uid ?: return

        deletePreviousImage(userId)

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imgFileName = "IMAGE_$timestamp.jpg"
        val storageRef = storage.reference.child("/images/$userId/$imgFileName")

        val inputStream = requireContext().contentResolver.openInputStream(uriPhoto!!)
        val imageBytes = inputStream?.readBytes()

        if (imageBytes != null) {
            storageRef.putBytes(imageBytes)
                .addOnCompleteListener { task: Task<UploadTask.TaskSnapshot> ->
                    if (task.isSuccessful) {
                        storageRef.downloadUrl.addOnSuccessListener { uri ->
                            updateProfileImageURL(uri.toString())
                        }
                    } else {
                        Toast.makeText(requireContext(), "이미지 업로드 실패", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun deletePreviousImage(userId: String) {
        val storageRef = storage.reference.child("/images/$userId/")

        storageRef.listAll()
            .addOnSuccessListener { result ->
                for (item in result.items) {
                    item.delete()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("DeleteImage", "이미지 삭제 실패: $exception")
            }
    }


    private fun updateProfileImageURL(imageURL: String) {
        val userId = auth.currentUser?.uid ?: return

        val userRef = firestore.collection("users").document(userId)
        userRef.update("profileImageUrl", imageURL)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "프로필 이미지 업로드 완료", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "프로필 이미지 URL 업데이트 실패", Toast.LENGTH_SHORT).show()
            }
    }

    private fun popupMember(storeName: String) {
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

        val showMemberList = TextView(requireContext())
        val textViewlayoutParams = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        textViewlayoutParams.setMargins(10, 10, 10, 10)
        showMemberList.layoutParams = textViewlayoutParams
        showMemberList.text="${storeName}의 근무시간"
        showMemberList.textSize= 15f
        showMemberList.setTypeface(null, Typeface.BOLD)

        linearLayout.addView(showMemberList)

        val spinner = Spinner(requireContext())
        val spinnerLayoutParams = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        spinnerLayoutParams.setMargins(10, 10, 10, 10)
        spinner.layoutParams = spinnerLayoutParams

        val monthList = resources.getStringArray(R.array.monthList)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, monthList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        linearLayout.addView(spinner)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val selectedMonth = spinner.selectedItem.toString()
                showMemberWorkDuration(storeName, selectedMonth, linearLayout)
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
            }

        }

        scrollView.addView(linearLayout)

        builder.setView(scrollView)

        builder.setNegativeButton("확인") { dialog, _ ->
            dialog.cancel()
        }

        builder.create().show()
    }

    private fun showMemberWorkDuration(storeName: String, selectedMonth: String, linearLayout: LinearLayout) {
        val userRef = firestore.collection("users")
        userRef.whereEqualTo("storeName", storeName)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.d("MypageFragment", "해당 storeName을 가진 사용자를 찾을 수 없습니다.")
                    return@addOnSuccessListener
                }
                for (document in documents) {
                    val userName = document.getString("name").toString()
                    val userId = document.id

                    val formattedDate = "2023년 ${selectedMonth}월 근무시간"
                    val dataRef = firestore.collection("users").document(userId)
                        .collection("total_workDuration").document(formattedDate)

                    dataRef.get()
                        .addOnSuccessListener { dataDocument ->
                            if(dataDocument.exists()) {
                                val month_workDuration = dataDocument.getString("2023년 ${selectedMonth}월 근무시간")
                                if (month_workDuration != null) {
                                    val cardView = showMemberCardView(month_workDuration, userName)
                                    linearLayout.addView(cardView)
                                }
                        }
                    }
                }
            }
            .addOnFailureListener { e: Exception ->
                Log.e("MypageFragment", "오류 발생: ${e}")
            }
    }


    private fun showMemberCardView(month_workDuration: String, userName: String): CardView {
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
        storeTime.text="${userName}의 총 근무시간: ${month_workDuration}"
        storeTime.textSize = 18f

        linearLayout.addView(storeTime)

        cardView.addView(linearLayout)

        return cardView
    }

    private fun loadProfileImage(imageView: ImageView) {
        val userId = auth.currentUser?.uid ?: return
        val ImageRef = storage.reference.child("images/$userId/")

        ImageRef.listAll()
            .addOnSuccessListener { result ->
                if (result.items.isNotEmpty()) {
                    val firstItem = result.items[0]

                    val fileName = firstItem.name

                    val storageRef = storage.reference.child("images/$userId/$fileName")
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        Glide.with(this).load(uri).into(imageView)
                    }.addOnFailureListener {e ->
                        Log.e("LoadImage", "이미지 로딩 실패: ${e.message}", e)
                        imageView.setImageResource(R.drawable.basic_image)
                    }
                }
            }
    }

}