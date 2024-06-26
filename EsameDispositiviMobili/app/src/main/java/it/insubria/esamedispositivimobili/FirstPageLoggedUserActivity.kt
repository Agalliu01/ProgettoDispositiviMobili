package it.insubria.esamedispositivimobili

import AddPostFragment
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

import it.insubria.esamedispositivimobili.HomeFragment
import it.insubria.esamedispositivimobili.R
import it.insubria.esamedispositivimobili.SearchFragment
import it.insubria.esamedispositivimobili.SettingsFragment

class FirstPageLoggedUserActivity : AppCompatActivity() {
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first_page_logged_user)

        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    replaceFragment(HomeFragment())
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.navigation_search -> {
                    replaceFragment(SearchFragment())
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.navigation_add_post -> {
                    replaceFragment(AddPostFragment())
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.navigation_settings -> {
                    replaceFragment(SettingsFragment())
                    return@setOnNavigationItemSelectedListener true
                }
                else -> false
            }
        }

        // Impostare il fragment Home come predefinito all'avvio dell'activity
        bottomNavigationView.selectedItemId = R.id.navigation_home
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}