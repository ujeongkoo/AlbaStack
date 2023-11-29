package org.techtown.albastack

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView

class MypageActivity : AppCompatActivity() {

    private lateinit var imageViewUser: ImageView
    private val PICK_IMAGE_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mypage)

        imageViewUser = findViewById(R.id.user_image)
        val btn_image_chage = findViewById<Button>(R.id.btn_image_change)

        btn_image_chage.setOnClickListener {
            // 갤러리에서 이미지를 선택하기 위한 Intent 호출
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }
    }

    // 이미지를 선택한 결과를 받기 위한 메서드
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val selectedImageUri = data.data
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedImageUri)

            // 선택한 이미지를 ImageView에 설정
            imageViewUser.setImageBitmap(bitmap)
        }
    }
}