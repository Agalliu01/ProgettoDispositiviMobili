package it.insubria.esameconsegnadispositivimobili
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
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
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

//***Activity usata per gestire registrazioen utenti
class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog

    private lateinit var passwordEditText: EditText
    private lateinit var confermaPasswordEditText: EditText

    private lateinit var passwordToggle: ImageView
    private lateinit var confermaPasswordToggle: ImageView

    private var isPasswordVisible = false
    private var isConfermaPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme) // Applica il tema personalizzato
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        auth = FirebaseAuth.getInstance()

        val nomeEditText = findViewById<EditText>(R.id.nomeEditText)
        val cognomeEditText = findViewById<EditText>(R.id.cognomeEditText)
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val usernameEditText = findViewById<EditText>(R.id.usernameEditText)
        val continuaButton = findViewById<Button>(R.id.continuaButton)
        val loginMover = findViewById<TextView>(R.id.loginMover)

        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Attendere...")
        progressDialog.setCancelable(false)

        passwordEditText = findViewById(R.id.passwordEditText)
        confermaPasswordEditText = findViewById(R.id.confermaPasswordEditText)

        passwordToggle = findViewById(R.id.passwordToggle)
        confermaPasswordToggle = findViewById(R.id.confermaPasswordToggle)

        passwordToggle.setOnClickListener {
            togglePasswordVisibility(passwordEditText)
        }

        confermaPasswordToggle.setOnClickListener {
            togglePasswordVisibility(confermaPasswordEditText)
        }

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
                showProgressDialog()
                createUserWithEmailPassword(nome, cognome, email, password, username)
            }
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

    private fun createUserWithEmailPassword(
        nome: String,
        cognome: String,
        email: String,
        password: String,
        username: String
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                hideProgressDialog()
                if (task.isSuccessful) {
                    // Se la registrazione è riuscita, ottieni l'UID dell'utente
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        // Verifica l'username
                        checkUsernameAndSaveUser(userId, nome, cognome, email, password, username)
                    } else {
                        Toast.makeText(
                            this,
                            "Errore durante il recupero dell'UID dell'utente",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    val errorMessage = task.exception?.message ?: "Errore durante la registrazione"
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()

                    if (task.exception is FirebaseAuthUserCollisionException) {
                        Toast.makeText(
                            this,
                            "Questa email è già associata a un altro account",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.w("SignUpActivity", "createUserWithEmailAndPassword:failure", task.exception)
                    }
                }
            }
    }

    private fun checkUsernameAndSaveUser(
        userId: String,
        nome: String,
        cognome: String,
        email: String,
        password: String,
        username: String
    ) {
        // Verifica se l'username è già stato utilizzato
        val usersRef = FirebaseDatabase.getInstance().getReference("users")
        usersRef.orderByChild("username").equalTo(username)
            .addListenerForSingleValueEvent(object :
                ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val finalUsername = if (snapshot.exists()) {
                        // L'username è già in uso, usa "admin" come default
                        Toast.makeText(
                            this@SignUpActivity,
                            "Username già usato, inserito default automatico 'admin'",
                            Toast.LENGTH_SHORT
                        ).show()
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
                    Toast.makeText(
                        this@SignUpActivity,
                        "Errore durante la registrazione: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
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
                navigateToFirstPageUserActivity()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this@SignUpActivity,
                    "Errore durante il salvataggio dei dettagli dell'utente: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("SignUpActivity", "Errore durante il salvataggio dei dettagli dell'utente", e)
            }
    }

    private fun navigateToFirstPageUserActivity() {
        // Delay di 1.5 secondi prima di passare all'activity successiva
        Handler().postDelayed({
            val intent = Intent(this, FirstPageUserActivity::class.java)
            startActivity(intent)
            finish()
        }, 1000)
    }

    private fun togglePasswordVisibility(editText: EditText) {
        if (editText.transformationMethod == PasswordTransformationMethod.getInstance()) {
            // Password nascosta, mostra la password
            editText.transformationMethod = HideReturnsTransformationMethod.getInstance()
            editText.setSelection(editText.text.length) // Posiziona il cursore alla fine del testo
        } else {
            // Password visibile, nascondi la password
            editText.transformationMethod = PasswordTransformationMethod.getInstance()
            editText.setSelection(editText.text.length) // Posiziona il cursore alla fine del testo

        }
    }
}