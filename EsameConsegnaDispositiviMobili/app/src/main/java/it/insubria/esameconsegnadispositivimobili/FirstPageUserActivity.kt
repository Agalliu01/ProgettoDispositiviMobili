package it.insubria.esameconsegnadispositivimobili

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import it.insubria.esameconsegnadispositivimobili.databinding.ActivityFirstPageUserBinding

class FirstPageUserActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFirstPageUserBinding
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var db: FirebaseFirestore  // Istanza di Firebase Firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFirstPageUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inizializzazione dell'istanza di Firebase Firestore
        db = FirebaseFirestore.getInstance()

        bottomNavigationView = binding.bottomNavigationView

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    db = FirebaseFirestore.getInstance()
                    replaceFragment(HomeFragment())
                    return@setOnNavigationItemSelectedListener true
                }

                R.id.navigation_search -> {
                    db = FirebaseFirestore.getInstance()
                    replaceFragment(SearchFragment())
                    return@setOnNavigationItemSelectedListener true
                }

                R.id.navigation_add_post -> {
                    db = FirebaseFirestore.getInstance()
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
            .replace(binding.fragmentContainer.id, fragment)
            .commit()
    }
}
