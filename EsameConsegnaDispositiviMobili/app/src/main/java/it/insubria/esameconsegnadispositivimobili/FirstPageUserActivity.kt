package it.insubria.esameconsegnadispositivimobili

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import it.insubria.esameconsegnadispositivimobili.databinding.ActivityFirstPageUserBinding


//*** Activity contenitore di navbar e di fragment: HomeFragment,SearchFragment,AddPostFragment e SettingsFragment
class FirstPageUserActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFirstPageUserBinding
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        binding = ActivityFirstPageUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bottomNavigationView = binding.bottomNavigationView

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    replaceFragment(HomeFragment())
                    true
                }
                R.id.navigation_search -> {
                    replaceFragment(SearchFragment())
                    true
                }
                R.id.navigation_add_post -> {
                    replaceFragment(AddPostFragment())
                    true
                }
                R.id.navigation_settings -> {
                    replaceFragment(SettingsFragment())
                    true
                }
                else -> false
            }
        }

        // Impostare il fragment Home come predefinito all'avvio dell'activity
        bottomNavigationView.selectedItemId = R.id.navigation_home
    }

    private fun replaceFragment(fragment: Fragment) {//metodo di sostituzione vista dei fragment al click di navbar!
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment)
            .commit()
    }
}
