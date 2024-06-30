package it.insubria.esameconsegnadispositivimobili

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import java.io.Serializable

class SignUpActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme) // Applica il tema personalizzato
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_sign_up)

        auth = FirebaseAuth.getInstance()

        val nomeEditText = findViewById<EditText>(R.id.nomeEditText)
        val cognomeEditText = findViewById<EditText>(R.id.cognomeEditText)
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val usernameEditText = findViewById<EditText>(R.id.usernameEditText)
        val continuaButton = findViewById<Button>(R.id.continuaButton)
        val loginMover = findViewById<TextView>(R.id.loginMover)

        loginMover.setOnClickListener {
            startActivity(Intent(this, LoginMainActivityLauncher::class.java))
        }

        continuaButton.setOnClickListener {
            val nome = nomeEditText.text.toString().trim()
            val cognome = cognomeEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val username = usernameEditText.text.toString().trim()

            if (nome.isEmpty() || cognome.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Tutti i campi sono obbligatori", Toast.LENGTH_SHORT).show()
            } else {
                createUserWithEmailPassword(nome, cognome, email, password, username)
            }
        }
    }

    private fun createUserWithEmailPassword(nome: String, cognome: String, email: String, password: String, username: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Se la registrazione è riuscita, ottieni l'UID dell'utente
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        // Verifica l'username
                        checkUsernameAndSaveUser(userId, nome, cognome, email, password, username)
                    } else {
                        Toast.makeText(this, "Errore durante il recupero dell'UID dell'utente", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorMessage = task.exception?.message ?: "Errore durante la registrazione"
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()

                    if (task.exception is FirebaseAuthUserCollisionException) {
                        Toast.makeText(this, "Questa email è già associata a un altro account", Toast.LENGTH_SHORT).show()
                        Log.w("SignUpActivity", "createUserWithEmailAndPassword:failure", task.exception)
                    }
                }
            }
    }

    private fun checkUsernameAndSaveUser(userId: String, nome: String, cognome: String, email: String, password: String, username: String) {
        // Verifica se l'username è già stato utilizzato
        val usersRef = FirebaseDatabase.getInstance().getReference("users")
        usersRef.orderByChild("username").equalTo(username).addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val finalUsername = if (snapshot.exists()) {
                    // L'username è già in uso, usa "admin" come default
                    Toast.makeText(this@SignUpActivity, "Username già usato, inserito default automatico 'admin'", Toast.LENGTH_SHORT).show()
                    "admin"
                } else {
                    // L'username non è stato trovato, procedi con quello fornito dall'utente
                    username
                }
                val userDetails = User(userId, nome, cognome, finalUsername, email, password, mutableListOf())
                saveUserDetails(userDetails)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("SignUpActivity", "Errore durante la verifica dell'username", error.toException())
                Toast.makeText(this@SignUpActivity, "Errore durante la registrazione: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveUserDetails(userDetails: User) {
        // Ottieni l'istanza del database Realtime
        val database = FirebaseDatabase.getInstance()

        // Ottieni un riferimento al nodo "users" nel tuo database
        val usersRef = database.getReference("users")

        // Usa l'UID dell'utente appena creato come chiave nel database
        val uid = auth.currentUser?.uid ?: return  // Assicurati che l'utente sia autenticato

        // Salva i dettagli dell'utente utilizzando l'UID come chiave
        usersRef.child(uid).setValue(userDetails)
            .addOnSuccessListener {
                Toast.makeText(this@SignUpActivity, "Registrazione completata con successo", Toast.LENGTH_SHORT).show()
                navigateToSignUpUsernameActivity(userDetails)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this@SignUpActivity, "Errore durante il salvataggio dei dettagli dell'utente: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("SignUpActivity", "Errore durante il salvataggio dei dettagli dell'utente", e)
            }
    }

    private fun navigateToSignUpUsernameActivity(userDetails: User) {
        val intent = Intent(this, FirstPageUserActivity::class.java)
        startActivity(intent)
        finish()
    }
}
