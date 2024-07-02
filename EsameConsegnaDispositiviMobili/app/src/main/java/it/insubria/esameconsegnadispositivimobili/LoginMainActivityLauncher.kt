package it.insubria.esameconsegnadispositivimobili

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth


//***Activity launcher principale la quale e l unica ad esser avviata all avvio!!
class LoginMainActivityLauncher : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_Dark)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_main_launcher)

        auth = FirebaseAuth.getInstance() // Prendo l'istanza del database

        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val loginButton = findViewById<Button>(R.id.loginBtn)
        val registrazioneMover = findViewById<TextView>(R.id.registrazioneMover)
        val passwordVisibilityToggle = findViewById<ImageView>(R.id.passwordVisibilityToggle)
//simulazione attesa...
        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Attendere...")
        progressDialog.setCancelable(false)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email e password sono obbligatori", Toast.LENGTH_SHORT).show()
            } else {
                showProgressDialog()
                loginUser(email, password)
            }
        }

        registrazioneMover.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        // Listener per il toggle della visibilità della password
        passwordVisibilityToggle.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            togglePasswordVisibility(passwordEditText, passwordVisibilityToggle)
        }
    }

    private fun showProgressDialog() {
        if (!progressDialog.isShowing) {
            progressDialog.show()
        }
    }

    private fun hideProgressDialog() {
        if (progressDialog.isShowing) {
            progressDialog.dismiss()
        }
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                hideProgressDialog()
                if (task.isSuccessful) {
                    Toast.makeText(this, "Login avvenuto con successo", Toast.LENGTH_SHORT).show()
                    // Delay di 1 secondo prima di passare all'activity successiva
                    Handler().postDelayed({
                        val intent = Intent(this, FirstPageUserActivity::class.java)
                        startActivity(intent)
                        finish()
                    }, 1000)
                } else {
                    Toast.makeText(this, "Autenticazione fallita: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }


//metodo per gestione visibilita password....
    private fun togglePasswordVisibility(passwordEditText: EditText, passwordVisibilityToggle: ImageView) {
        if (isPasswordVisible) {
            // Mostro la password
            passwordEditText.inputType = android.text.InputType.TYPE_CLASS_TEXT
            passwordVisibilityToggle.setImageResource(R.drawable.icons8_visibile_30)
        } else {
            // Nascondo la password
            passwordEditText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            passwordVisibilityToggle.setImageResource(R.drawable.icons8_visibile_30)
        }

        // Muovo il cursore alla fine del testo
        passwordEditText.setSelection(passwordEditText.text.length)
    }
}
