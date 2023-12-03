package org.techtown.albastack

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        val btn_login = findViewById<Button>(R.id.btn_login)
        val btn_sign_in = findViewById<Button>(R.id.btn_sign_in)
        val emailEditText = findViewById<EditText>(R.id.login_id)
        val passwordEditText = findViewById<EditText>(R.id.login_pw)

        btn_login.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            logIn(email, password)
        }

        btn_sign_in.setOnClickListener {
            try {
                val intent = Intent(this, SignInActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Log.e("LoginActivity", "오류: ${e.message}")
            }
        }
    }

    private fun logIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if(task.isSuccessful) {
                    // 로그인 성공
                    Toast.makeText(this@LoginActivity, "환영합니다!", Toast.LENGTH_SHORT).show()

                    // 메인 액티비티로 이동
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // 로그인 실패
                    Toast.makeText(this@LoginActivity, "로그인 실패", Toast.LENGTH_SHORT).show()
                }
            }
    }

}