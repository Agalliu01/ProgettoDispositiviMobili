package it.insubria.esamedispositivimobili

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
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
            startActivity(Intent(this, LoginMainActivity::class.java))
        }

        continuaButton.setOnClickListener {
            val nome = nomeEditText.text.toString().trim()
            val cognome = cognomeEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (nome.isEmpty() || cognome.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Tutti i campi sono obbligatori", Toast.LENGTH_SHORT).show()
            } else {
                registerUser(nome, cognome, email, password)
            }
        }
    }

    private fun registerUser(nome: String, cognome: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userDetails = UserDetails(nome, cognome, email, password)
                    saveUserDetails(userDetails)
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

    private fun saveUserDetails(userDetails: UserDetails) {
        db.collection("users").document(auth.currentUser!!.uid)
            .set(userDetails)
            .addOnSuccessListener {
                navigateToSignUpUsernameActivity(userDetails)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Errore durante il salvataggio dei dettagli utente", Toast.LENGTH_SHORT).show()
                Log.e("SignUpActivity", "Error saving user details", e)
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
    val nome: String = "",
    val cognome: String = "",
    val email: String = "",
    val password: String = "",
    var username: String = "",
    var seguiti: MutableList<String> = mutableListOf(),
    var imageUrl: String = ""
) : Serializable {

    // Costruttore vuoto richiesto da Firebase Firestore
    constructor() : this("", "", "", "", "", mutableListOf(), "")

    // Metodo per aggiungere un utente seguito
    fun addSeguito(user: String) {
        seguiti.add(user)
    }

    // Metodo per rimuovere un utente seguito
    fun removeSeguito(user: String) {
        seguiti.remove(user)
    }

    // Metodo per ottenere la rappresentazione come stringa dei seguiti
    fun seguitiToString(delimiter: String = ","): String {
        return seguiti.joinToString(delimiter)
    }

    // Metodo per impostare i seguiti da una stringa
    fun seguitiFromString(data: String?, delimiter: String = ",") {
        if (data.isNullOrEmpty()) {
            seguiti = mutableListOf()
        } else {
            seguiti = data.split(delimiter).map { it.trim() }.toMutableList()
        }
    }
}
