package it.insubria.esamedispositivimobili


import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import java.util.*


class SignUpUsernameActivity : AppCompatActivity() {

    private lateinit var userDetails: UserDetails
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var storageRef: StorageReference
    private var imageUri: Uri? = null

    // Dichiarazione dell'ImageView
    private lateinit var profileImageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up_username)

        userDetails = intent.getSerializableExtra("userDetails") as UserDetails
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        storageRef = storage.reference

        val usernameEditText = findViewById<EditText>(R.id.usernameEditText)
        val confermaButton = findViewById<Button>(R.id.confermaButton)
        val selectImageButton = findViewById<Button>(R.id.selectImageButton)
        val skipButton = findViewById<Button>(R.id.skipButton)

        // Inizializzazione dell'ImageView
        profileImageView = findViewById(R.id.profileImageView)

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
                        userDetails.username = username
                        saveUsernameAndImageToFirestore()
                    }
                }
            } else {
                Toast.makeText(this, "Inserisci un username valido", Toast.LENGTH_SHORT).show()
            }
        }

        selectImageButton.setOnClickListener {
            // Intent per selezionare un'immagine da qualsiasi fonte
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
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

    private fun saveUsernameAndImageToFirestore() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val userRef = db.collection("users").document(userId)

            // Salva l'immagine del profilo se è stata selezionata
            if (imageUri != null) {
                val imageFileName = "profile_images/${UUID.randomUUID()}"
                val imageRef = storageRef.child(imageFileName)

                val uploadTask = imageRef.putFile(imageUri!!)
                uploadTask.continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let {
                            throw it
                        }
                    }
                    imageRef.downloadUrl
                }.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val downloadUri = task.result

                        // Aggiorna userDetails con l'URL dell'immagine del profilo
                        userDetails.imageUrl = downloadUri.toString()

                        // Aggiungi userDetails al Firestore
                        userRef.set(userDetails)
                            .addOnSuccessListener {
                                moveToNextActivity()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Errore durante il salvataggio dei dettagli utente", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, "Errore durante il caricamento dell'immagine", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // Non è stata selezionata un'immagine, salva solo userDetails
                userRef.set(userDetails)
                    .addOnSuccessListener {
                        moveToNextActivity()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Errore durante il salvataggio dei dettagli utente", Toast.LENGTH_SHORT).show()
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

    // Metodo per gestire la selezione dell'immagine dalla galleria
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.data != null) {
            imageUri = data.data

            // Visualizza l'immagine selezionata nell'ImageView
            profileImageView.visibility = View.VISIBLE
            profileImageView.setImageURI(imageUri)

            Toast.makeText(this, "Immagine selezionata", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val PICK_IMAGE_REQUEST = 71
    }
}