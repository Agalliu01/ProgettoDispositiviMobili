package it.insubria.esamedispositivimobili

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private lateinit var nomeEditText: EditText
    private lateinit var cognomeEditText: EditText
    private lateinit var usernameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confermaPasswordEditText: EditText
    private lateinit var showPassword1ImageView: ImageView
    private lateinit var showPassword2ImageView: ImageView
    private lateinit var registratiButton: Button
    private lateinit var loginMover: TextView
//
    private var isPasswordVisible1 = false
    private var isPasswordVisible2 = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference.child("Usernames")

        // Inizializzazione delle view
        nomeEditText = findViewById(R.id.nomeEditText)
        cognomeEditText = findViewById(R.id.cognomeEditText)
        usernameEditText = findViewById(R.id.usernameEditText)
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        confermaPasswordEditText = findViewById(R.id.confermaPasswordEditText)
        showPassword1ImageView = findViewById(R.id.showPassword1ImageView)
        showPassword2ImageView = findViewById(R.id.showPassword2ImageView)
        registratiButton = findViewById(R.id.registratiButton)
        loginMover = findViewById(R.id.loginMover)

        // Gestione della visibilità delle password
        showPassword1ImageView.setOnClickListener {
            isPasswordVisible1 = !isPasswordVisible1
            updatePasswordVisibility(passwordEditText, isPasswordVisible1)
        }

        showPassword2ImageView.setOnClickListener {
            isPasswordVisible2 = !isPasswordVisible2
            updatePasswordVisibility(confermaPasswordEditText, isPasswordVisible2)
        }

        // Listener per il pulsante di registrazione
        registratiButton.setOnClickListener {
            val nome = nomeEditText.text.toString().trim()
            val cognome = cognomeEditText.text.toString().trim()
            val username = usernameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString()
            val confermaPassword = confermaPasswordEditText.text.toString()

            if (nome.isEmpty() || cognome.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty() || confermaPassword.isEmpty()) {
                Toast.makeText(this, "Tutti i campi sono obbligatori", Toast.LENGTH_SHORT).show()
            } else if (password != confermaPassword) {
                Toast.makeText(this, "Le password non corrispondono", Toast.LENGTH_SHORT).show()
            } else {
                // Verifica se lo username è già presente nel database
                checkUsernameAndRegister(nome, cognome, username, email, password)
            }
        }

        // Listener per cliccare sul testo "Hai già un account?"
        loginMover.setOnClickListener {
            val intent = Intent(this, LoginMainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun checkUsernameAndRegister(nome: String, cognome: String, username: String, email: String, password: String) {
        // Verifica se lo username è già presente nel database
        database.child(username).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Lo username esiste già
                    Toast.makeText(this@SignUpActivity, "Username già presente", Toast.LENGTH_SHORT).show()
                } else {
                    // Registra l'utente su Firebase
                    registerUser(email, password)
                    Toast.makeText(this@SignUpActivity, "Utente registrato correttamente", Toast.LENGTH_SHORT).show()

                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@SignUpActivity, "Errore durante la verifica dello username", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun registerUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Registrazione avvenuta con successo
                    Toast.makeText(this, "Registrazione avvenuta con successo", Toast.LENGTH_SHORT).show()
                    // Puoi gestire il login automatico o altre azioni qui se necessario
                    // Esempio: loginUser(email, password)
                    finish()
                } else {
                    // Gestione delle eccezioni
                    if (task.exception is FirebaseAuthUserCollisionException) {
                        Toast.makeText(this, "Esiste già un account con questa email", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Registrazione fallita: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun updatePasswordVisibility(editText: EditText, isVisible: Boolean) {
        if (isVisible) {
            // Mostra la password
            editText.transformationMethod = null
        } else {
            // Nascondi la password
            editText.transformationMethod = android.text.method.PasswordTransformationMethod()
        }
        // Imposta il cursore alla fine del testo
        editText.setSelection(editText.text.length)
    }
}