package it.insubria.esamedispositivimobili


import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginMainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_main)

        auth = FirebaseAuth.getInstance() // Prendo l'istanza del database

        val emailEditText = findViewById<EditText>(R.id.emailEditText) // Inizializzo variabili collegate con i bottoni e TextView
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val loginButton = findViewById<Button>(R.id.loginBtn)
        val registrazioneMover = findViewById<TextView>(R.id.registrazioneMover)

        loginButton.setOnClickListener { // Associo listener ai bottoni
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) { // Mostro messaggi ERRORE se sbaglia...
                Toast.makeText(this, "Email e password sono obbligatori", Toast.LENGTH_SHORT).show()
            } else {
                loginUser(email, password) // Altrimenti faccio login
            }
        }

        registrazioneMover.setOnClickListener {
            val intent = Intent(this,SignUpActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Login successful
                    Toast.makeText(this, "Login avvenuto con successo", Toast.LENGTH_SHORT).show()
                    // Proceed to the next activity
                    val intent = Intent(this, FirstPageLoggedUserActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // If login fails, display a message to the user.
                    Toast.makeText(this, "Autenticazione fallita: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun registerUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Registration successful
                    Toast.makeText(this, "Registrazione avvenuta con successo", Toast.LENGTH_SHORT).show()
                    // Automatically log the user in
                    loginUser(email, password)
                } else {
                    // If registration fails, display a message to the user.
                    Toast.makeText(this, "Registrazione fallita: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
