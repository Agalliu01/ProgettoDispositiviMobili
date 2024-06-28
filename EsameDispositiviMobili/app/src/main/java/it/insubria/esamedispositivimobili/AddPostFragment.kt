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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
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
    private lateinit var firestore: FirebaseFirestore
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
        firestore = FirebaseFirestore.getInstance()
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
                Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadPost() {
        val description = descriptionEditText.text.toString().trim()
        val link = linkEditText.text.toString().trim()
        val user = firebaseAuth.currentUser

        // Non è necessario controllare se l'utente ha un username valido

        if (user != null) {
            if (imageUri != null) {
                // Upload dell'immagine con ulteriore elaborazione se necessario
                CoroutineScope(Dispatchers.IO).launch {
                    uploadImageAndPost(description, link, user)
                }
            } else {
                // Se non viene selezionata alcuna immagine, procedi con il caricamento del post senza immagine
                CoroutineScope(Dispatchers.IO).launch {
                    uploadPostWithoutImage(description, link, user)
                }
            }
        } else {
            // L'utente non è autenticato (sarebbe gestito da altre logiche, ad esempio navigazione al login)
            Toast.makeText(requireContext(), "Utente non autenticato", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun uploadImageAndPost(description: String, link: String, user: FirebaseUser) {
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
                likesCount = 0
            )

            // Aggiungi il post a Firestore
            addPostToFirestore(post)

        } catch (e: Exception) {
            // Gestione del fallimento
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun uploadPostWithoutImage(description: String, link: String, user: FirebaseUser) {
        try {
            // Creazione di un nuovo oggetto post senza URL dell'immagine ma con link
            val username = user.displayName ?: user.email ?: "Anonymous"
            val uid = user.uid

            val post = Post(
                uid = uid,
                username = username,
                description = description,
                imageUrl = "",
                link = link,
                likesCount = 0
            )

            // Aggiungi il post a Firestore
            addPostToFirestore(post)

        } catch (e: Exception) {
            // Gestione del fallimento
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Impossibile aggiungere il post a Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun addPostToFirestore(post: Post) {
        try {
            // Aggiungi il post a Firestore
            firestore.collection("posts").add(post.toMap()).await()

            // Gestione del successo
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Post caricato con successo", Toast.LENGTH_SHORT).show()

                // Naviga a HomeFragment dopo il caricamento riuscito
                navigateBack()
            }

        } catch (e: Exception) {
            // Gestione del fallimento
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Impossibile aggiungere il post a Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
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
}

data class Post(
    val uid: String,
    val username: String,
    val description: String,
    val imageUrl: String,
    val link: String,
    var likesCount: Int,
    var likedBy: MutableList<String> = mutableListOf() // Lista degli utenti che hanno messo "mi piace"
)
 {
    constructor() : this("", "", "", "", "", 0)

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "uid" to uid,
            "username" to username,
            "description" to description,
            "imageUrl" to imageUrl,
            "link" to link,
            "likesCount" to likesCount,
            "likedBy " to likedBy
        )
    }
}
