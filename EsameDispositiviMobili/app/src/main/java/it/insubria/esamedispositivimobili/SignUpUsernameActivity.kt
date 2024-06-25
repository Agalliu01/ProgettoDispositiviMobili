package it.insubria.esamedispositivimobili

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignUpUsernameActivity : AppCompatActivity() {

    private lateinit var userDetails: UserDetails
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up_username)

        userDetails = intent.getSerializableExtra("userDetails") as UserDetails
        db = FirebaseFirestore.getInstance()

        val usernameEditText = findViewById<EditText>(R.id.usernameEditText)
        val confermaButton = findViewById<Button>(R.id.confermaButton)
        val skipButton = findViewById<Button>(R.id.skipButton)

        confermaButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()

            if (username.isNotEmpty()) {
                // Verifica se l'username è già presente nel database
                checkUsernameExists(username) { exists ->
                    if (exists) {
                        runOnUiThread {
                            Toast.makeText(this, "Username già esistente, scegline un altro", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // Procedi con la registrazione dell'username
                        saveUsernameToFirestore(username)
                    }
                }
            } else {
                Toast.makeText(this, "Inserisci un username valido", Toast.LENGTH_SHORT).show()
            }
        }

        skipButton.setOnClickListener {
            // Passa direttamente alla prossima activity senza username
            moveToNextActivity()
        }
    }

    private fun checkUsernameExists(username: String, callback: (Boolean) -> Unit) {
        db.collection("users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { documents ->
                callback(!documents.isEmpty)
            }
            .addOnFailureListener { e ->
                runOnUiThread {
                    Toast.makeText(this, "Errore durante la verifica dell'username: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                callback(true) // Considera l'username esistente in caso di errore
            }
    }

    private fun saveUsernameToFirestore(username: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val userRef = db.collection("users").document(userId)

            userRef.get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        // Il documento esiste, quindi aggiorna l'username
                        userRef.update("username", username)
                            .addOnSuccessListener {
                                runOnUiThread {
                                    Toast.makeText(this, "Username salvato con successo", Toast.LENGTH_SHORT).show()
                                }
                                moveToNextActivity()
                            }
                            .addOnFailureListener { e ->
                                runOnUiThread {
                                    Toast.makeText(this, "Errore durante l'aggiornamento dell'username: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                    } else {
                        // Il documento non esiste, crea un nuovo documento con l'username
                        val newUser = hashMapOf(
                            "username" to username
                            // Aggiungi altri campi utente se necessario
                        )

                        userRef.set(newUser)
                            .addOnSuccessListener {
                                runOnUiThread {
                                    Toast.makeText(this, "Username salvato con successo", Toast.LENGTH_SHORT).show()
                                }
                                moveToNextActivity()
                            }
                            .addOnFailureListener { e ->
                                runOnUiThread {
                                    Toast.makeText(this, "Errore durante il salvataggio dell'username: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                }
                .addOnFailureListener { e ->
                    runOnUiThread {
                        Toast.makeText(this, "Errore durante il recupero del documento utente: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            Toast.makeText(this, "Utente non autenticato", Toast.LENGTH_SHORT).show()
        }
    }

    private fun moveToNextActivity() {
        val intent = Intent(this, FirstPageLoggedUserActivity::class.java)
        intent.putExtra("userDetails", userDetails)
        startActivity(intent)
        finish()
    }
}
