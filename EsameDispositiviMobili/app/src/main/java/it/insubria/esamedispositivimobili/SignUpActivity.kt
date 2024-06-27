package it.insubria.esamedispositivimobili

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.Serializable

class SignUpActivity : AppCompatActivity() {


    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val nomeEditText = findViewById<EditText>(R.id.nomeEditText)
        val cognomeEditText = findViewById<EditText>(R.id.cognomeEditText)
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val continuaButton = findViewById<Button>(R.id.continuaButton)
        val loginMover = findViewById<TextView>(R.id.loginMover)

        loginMover.setOnClickListener {
            val intent = Intent(this, LoginMainActivity::class.java)
            startActivity(intent)
        }

        continuaButton.setOnClickListener {
            val nome = nomeEditText.text.toString().trim()
            val cognome = cognomeEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (nome.isEmpty() || cognome.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Tutti i campi sono obbligatori", Toast.LENGTH_SHORT).show()
            } else {
                // Registrazione utente con Firebase Authentication
                registerUser(nome, cognome, email, password)
            }
        }
    }

    private fun registerUser(nome: String, cognome: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Registrazione utente con successo, passa alla seconda activity
                    val userDetails = UserDetails(nome, cognome, email, password)
                    saveUserDetails(userDetails)
                } else {
                    // Gestione errori durante la registrazione
                    val errorMessage = task.exception?.message ?: "Errore durante la registrazione"
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveUserDetails(userDetails: UserDetails) {
        // Salva userDetails in Firestore
        val userRef = db.collection("users").document(auth.currentUser!!.uid)

        userRef.set(userDetails)
            .addOnSuccessListener {
                // Dopo aver salvato userDetails con successo, aggiungi questo utente alla lista dei seguiti dell'utente corrente
                addCurrentUserToFollowList(userDetails)
            }
            .addOnFailureListener { e ->
                // Gestione errori durante il salvataggio
                Toast.makeText(this, "Errore durante il salvataggio dei dettagli utente", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Error saving user details", e)
            }
    }

    private fun addCurrentUserToFollowList(userDetails: UserDetails) {
        val currentUserUid = auth.currentUser?.uid
        if (currentUserUid != null) {
            db.collection("users").document(currentUserUid)
                .update("seguiti", userDetails.username)
                .addOnSuccessListener {
                    navigateToSignUpUsernameActivity(userDetails)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Errore durante l'aggiunta agli utenti seguiti", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Error adding user to follow list", e)
                }
        } else {
            Toast.makeText(this, "Utente corrente non trovato", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToSignUpUsernameActivity(userDetails: UserDetails) {
        val intent = Intent(this, SignUpUsernameActivity::class.java)
        intent.putExtra("userDetails", userDetails)
        startActivity(intent)
        finish()
    }
}


data class UserDetails(
    val nome: String,
    val cognome: String,
    val email: String,
    val password: String,
    var username: String = "", // Username opzionale
    var seguiti: List<String> = emptyList() ,
    var imageUrl: String = "" // URL dell'immagine associata su Firebase Storage
) : Serializable {
    // Costruttore senza argomenti richiesto da Firebase Firestore
    constructor() : this("", "", "", "", "", listOf(),"")
    constructor(userId: String, s: String, s1: String) : this()
    @Suppress("UNCHECKED_CAST")
    @JvmOverloads
    fun fromString(data: String?, delimiter: String = ","): UserDetails {
        if (data.isNullOrEmpty()) {
            seguiti = emptyList()
        } else {
            seguiti = data.split(delimiter).map { it.trim() }
        }
        return this
    }
}