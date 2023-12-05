package org.techtown.albastack

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
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
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
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
    private lateinit var storeEditText: EditText
    private lateinit var btnAddStore: Button

    private lateinit var storeList: ArrayList<String>
    private lateinit var storeAdapter: ArrayAdapter<String>

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

        val buttonSetStore: Button = view.findViewById(R.id.btn_setstore)
        buttonSetStore.setOnClickListener {
            try {
            showSetStore()
            } catch(e: Exception) {
                Log.e("MyPageFragment", "${e.message}")
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
                            textView.text = "$it 님 안녕하세요!"
                        }
                    } else {
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