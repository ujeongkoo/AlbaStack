package org.techtown.albastack

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        supportActionBar!!.setDisplayShowTitleEnabled(false)

        // BottomNavigationView 초기화
        bottomNavigation = findViewById(R.id.bottom_navigation)

        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES), 1)

        // 첫 번째 Fragment 표시
        val homeFragment = HomeFragment()
        replaceFragment(homeFragment)
        bottomNavigation.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_home -> {
                    val homeFragment = HomeFragment()
                    replaceFragment(homeFragment)
                    true
                }
                R.id.menu_plus -> {
                    val infoPlusFragment = InfoPlusFragment()
                    replaceFragment(infoPlusFragment)
                    true
                }
                R.id.menu_mypage -> {
                    val mypageFragment = MypageFragment()
                    replaceFragment(mypageFragment)
                    true
                } else -> false
            }
        }

    }

    // Fragment 교체 함수
    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager: FragmentManager = supportFragmentManager
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.containers, fragment)
        fragmentTransaction.commit()
    }

}