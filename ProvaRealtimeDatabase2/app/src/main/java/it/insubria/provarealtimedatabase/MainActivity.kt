package it.insubria.provarealtimedatabase

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {

    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        database = FirebaseDatabase.getInstance()

        val uploadButton = findViewById<Button>(R.id.uploadButton)
        uploadButton.setOnClickListener {
            uploadPost()
        }
    }

    private fun uploadPost() {
        val uid = "post123" // Esempio di UID del post, potrebbe essere generato dinamicamente
        val username = "JohnDoe"
        val description = "Questa è una descrizione del post"
        val imageUrl = "https://example.com/image.jpg"
        val link = "https://example.com"
        val likedBy = mutableListOf<String>() // Lista degli utenti che hanno messo "mi piace"
        val counteLike=0


        // Creazione dell'oggetto Post
        val post = Post(uid, username, description, imageUrl, link, likedBy)

        // Riferimento al nodo "posts" nel database
        val postsRef = database.getReference("posts")

        // Genera una chiave unica per il nuovo post
        val newPostRef = postsRef.push()

        // Salva il post utilizzando la chiave generata
        newPostRef.setValue(post)
            .addOnSuccessListener {
                Toast.makeText(this, "Post salvato con successo", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Errore durante il salvataggio del post", e)
                Toast.makeText(this, "Errore durante il salvataggio del post", Toast.LENGTH_SHORT).show()
            }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}


data class Posdt(
    val uid: String,
    val username: String,
    val description: String,
    val imageUrl: String,
    val link: String,
    var likedBy: MutableList<String> = mutableListOf() // Lista degli utenti che hanno messo "mi piace"
) {
    constructor() : this("", "", "", "", "")

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "uid" to uid,
            "username" to username,
            "description" to description,
            "imageUrl" to imageUrl,
            "link" to link,
            "likedBy" to likedBy
        )
    }
}
