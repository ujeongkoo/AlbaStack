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
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.auth.oAuthCredential
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
        auth = Firebase.auth
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
        val email = "aoyujeong@naver.com"
        val password = "7290yjeo^^"

        val credential = EmailAuthProvider.getCredential(email, password)

        // Firebase Authentication에서 사용자 삭제 전에 다시 로그인
        currentUser?.reauthenticate(credential)
            ?.addOnCompleteListener { reauthTask ->
                if (reauthTask.isSuccessful) {
                    // 사용자가 성공적으로 재인증된 경우
                    currentUser?.delete()
                    // 삭제 작업 수행
                    deleteFirestoreData(currentUser.uid)
                    deleteStorageData(currentUser.uid)

                    // 로그인 페이지로 이동
                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    startActivity(intent)
                    requireActivity().finish()
                } else {
                    // 재인증 실패
                    Log.e("MypageFragment", "재인증 실패", reauthTask.exception)
                }
            }
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
                Log.e("DeleteImage", "Failed to delete images: $exception")
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
                        Log.e("LoadImage", "Image loading failed: ${e.message}", e)
                        imageView.setImageResource(R.drawable.basic_image)
                    }
                }
            }
    }

}