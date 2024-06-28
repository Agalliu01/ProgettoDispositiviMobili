package it.insubria.esamedispositivimobili
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.database
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import it.insubria.esamedispositivimobili.HomeFragment
import it.insubria.esamedispositivimobili.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.*

class AddPostFragment : Fragment() {

    private lateinit var descriptionEditText: EditText
    private lateinit var linkEditText: EditText
    private lateinit var uploadButton: Button
    private lateinit var selectImageButton: Button
    private lateinit var imageView: ImageView

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var storage: FirebaseStorage

    private var imageUri: Uri? = null

    companion object {
        private const val PICK_IMAGE_REQUEST = 71
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_post, container, false)

        descriptionEditText = view.findViewById(R.id.descriptionEditText)
        linkEditText = view.findViewById(R.id.linkEditText)
        uploadButton = view.findViewById(R.id.uploadButton)
        selectImageButton = view.findViewById(R.id.selectImageButton)
        imageView = view.findViewById(R.id.imageView)

        firebaseAuth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()

        selectImageButton.setOnClickListener {
            chooseImage()
        }

        uploadButton.setOnClickListener {
            uploadPost()
        }

        return view
    }

    private fun chooseImage() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            imageUri = data.data
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, imageUri)
                imageView.setImageBitmap(bitmap)
            } catch (e: IOException) {
                e.printStackTrace()
                showToast("Failed to load image")
            }
        }
    }

    private fun uploadPost() {
        val description = descriptionEditText.text.toString().trim()
        val link = linkEditText.text.toString().trim()
        val user = firebaseAuth.currentUser

        if (user != null) {
            if (imageUri != null) {
                // Upload dell'immagine con ulteriore elaborazione se necessario
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // Genera un nome casuale per l'immagine
                        val imageName = UUID.randomUUID().toString()
                        val imageRef = storage.reference.child("images/$imageName")

                        // Upload dell'immagine su Firebase Storage
                        val uploadTask = imageRef.putFile(imageUri!!).await()
                        val downloadUrl = imageRef.downloadUrl.await()

                        // Creazione di un nuovo oggetto post con URL dell'immagine e link
                        val username = user.displayName ?: user.email ?: "Anonymous"
                        val uid = user.uid

                        val post = Post(
                            uid = uid,
                            username = username,
                            description = description,
                            imageUrl = downloadUrl.toString(),
                            link = link,
                            likedBy = mutableListOf(),
                            comments = mutableListOf()
                        )

                        // Salva il post su Firebase Realtime Database
                        savePostToDatabase(post)

                    } catch (e: Exception) {
                        // Gestione del fallimento
                        withContext(Dispatchers.Main) {
                            showToast("Failed: ${e.message}")
                        }
                    }
                }
            } else {
                // Avvisa l'utente di inserire un'immagine prima di procedere
                showToast("Seleziona un'immagine prima di caricare il post")
            }
        } else {
            // L'utente non è autenticato (gestito da logiche di autenticazione)
            showToast("Utente non autenticato")
        }
    }

    private suspend fun savePostToDatabase(post: Post) {
        try {
            // Ottieni un riferimento al nodo "posts" nel tuo database
            val database = FirebaseDatabase.getInstance()
            val postsRef = database.getReference("posts")

            // Genera una chiave univoca per il nuovo post
            val postKey = postsRef.push().key ?: ""

            // Salva il post utilizzando la chiave generata
            postsRef.child(postKey).setValue(post.toMap()).await()

            // Gestione del successo
            withContext(Dispatchers.Main) {
                showToast("Post caricato con successo")
                // Naviga a HomeFragment dopo il caricamento riuscito
                navigateBack()
            }

        } catch (e: Exception) {
            // Gestione del fallimento
            withContext(Dispatchers.Main) {
                showToast("Impossibile aggiungere il post a Realtime Database: ${e.message}")
            }
        }
    }

    private fun navigateBack() {
        val fragmentManager: FragmentManager = requireActivity().supportFragmentManager

        // Sostituisci il fragment corrente con HomeFragment senza aggiungerlo allo stack
        fragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, HomeFragment())
            .commit()
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}