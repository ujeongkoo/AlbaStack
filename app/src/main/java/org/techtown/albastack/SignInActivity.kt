package org.techtown.albastack

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class SignInActivity : AppCompatActivity() {

    // FirebaseAuth의 인스턴스를 선언
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseReference: FirebaseDatabase
    private lateinit var firestore: FirebaseFirestore

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        // onCreate() 메서드에서 FirebaseAuth 및 FirebaseAuth 인스턴스 초기화
        auth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance()
        firestore = FirebaseFirestore.getInstance() // Firestore 초기화

        val spinner = findViewById<Spinner>(R.id.spinner_job)
        val nameEditText = findViewById<EditText>(R.id.signin_name)
        val emailEditText = findViewById<EditText>(R.id.signin_id)
        val passwordEditText = findViewById<EditText>(R.id.signin_pw)
        val passwordCheckEditText = findViewById<EditText>(R.id.signin_pw_again)
        val passwordCheckText = findViewById<TextView>(R.id.signin_pw_check)
        val btnSave = findViewById<Button>(R.id.btn_account_save)

        // Cloud Firestore 초기화
        //val db = Firebase.firestore

        spinner.adapter = ArrayAdapter.createFromResource(this, R.array.jobList, android.R.layout.simple_spinner_item)

        passwordCheckEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if(passwordEditText.text.toString() == p0.toString()) {
                    passwordCheckText.setText("비밀번호가 일치합니다.")
                } else {
                    passwordCheckText.setText("비밀번호가 일치하지 않습니다.")
                }
            }

            override fun afterTextChanged(p0: Editable?) {
                if(passwordEditText.text.toString() == p0.toString()) {
                    btnSave.isEnabled = true
                } else {
                    btnSave.isEnabled = false
                }
            }

        })

        // SIGNIN 버튼 클릭 시 회원가입 및 데이터 Firebase로 저장
        btnSave.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            val name = nameEditText.text.toString()
            val selectedJob = spinner.selectedItem.toString()
            val selectedSex = getSelectedSex()

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) {task ->
                    if (task.isSuccessful) {
                        // 회원가입 성공 시 데이터 Firebase에 저장
                        saveDataToFirebase(selectedJob, selectedSex, name, email)
                        Toast.makeText(this@SignInActivity, "회원가입 성공", Toast.LENGTH_SHORT).show()
                        moveToLoginActivity()
                    } else {
                        Toast.makeText(this@SignInActivity, "회원가입 실패", Toast.LENGTH_SHORT).show()
                    }
                }
        }

    }

    public override fun onStart() {
        super.onStart()
        // 활동을 초기화할 때 사용자가 현재 로그인되어 있는지 확인
        val currentUser = auth.currentUser
        if (currentUser != null) {
            reload()
        }
    }

    //선택된 라디오버튼을 문자열로 변환
    private fun getSelectedSex(): String {
        val radioBtnId = findViewById<RadioGroup>(R.id.radio_group_sex)
        val selectedRadioBtnId = radioBtnId.checkedRadioButtonId

        return when (selectedRadioBtnId) {
            R.id.radio_btn_man -> "남자"
            R.id.radio_btn_woman -> "여자"
            else -> ""
        }
    }

    private fun saveDataToFirebase(selectedJob: String, selectedSex: String, name: String, email: String) {
        val currentUser = auth.currentUser

        currentUser?.let {
            val userId = it.uid

            // Firestore에 데이터 추가
            val userDocument = firestore.collection("users").document(userId)
            userDocument.set(
                mapOf(
                    "selected_job" to selectedJob,
                    "selected_sex" to selectedSex,
                    "name" to name,
                    "email" to email
                )
            )
        }
    }


    private fun reload() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    private fun moveToLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish() // 현재 화면 종료
    }

}