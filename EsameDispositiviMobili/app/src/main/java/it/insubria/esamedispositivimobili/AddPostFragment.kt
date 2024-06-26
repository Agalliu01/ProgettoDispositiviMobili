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
import it.insubria.esamedispositivimobili.LoginMainActivity
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

        if (user != null) {
            if (imageUri != null) {
                // Upload image with additional processing if needed
                CoroutineScope(Dispatchers.IO).launch {
                    uploadImageAndPost(description, link, user)
                }
            } else {
                // If no image is selected, proceed to upload post without image
                CoroutineScope(Dispatchers.IO).launch {
                    uploadPostWithoutImage(description, link, user)
                }
            }
        } else {
            // Handle case where user is not authenticated
            Toast.makeText(requireContext(), "Please login to create a post", Toast.LENGTH_SHORT).show()
            startActivity(Intent(requireContext(),LoginMainActivity::class.java))
        }
    }

    private suspend fun uploadImageAndPost(description: String, link: String, user: FirebaseUser) {
        // Generate a random image name
        val imageName = UUID.randomUUID().toString()
        val imageRef = storage.reference.child("images/$imageName")

        try {
            // Upload image to Firebase Storage
            val uploadTask = imageRef.putFile(imageUri!!).await()
            val downloadUrl = imageRef.downloadUrl.await()

            // Create a new post object with image URL and link
            val username = user.displayName ?: user.email ?: "Anonymous"
            val uid = user.uid

            val post = hashMapOf(
                "uid" to uid,
                "username" to username,
                "description" to description,
                "imageURL" to downloadUrl.toString(), // Store image URL in Firestore
                "link" to link, // Store link in Firestore
                "commentsEnabled" to false, // Default to false
                "likesCount" to 0 // Initial likes count
            )

            // Add the post to Firestore
            firestore.collection("posts").add(post).await()

            // Handle success
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Post uploaded successfully", Toast.LENGTH_SHORT).show()

                // Navigate to HomeFragment after successful upload
                navigateBack()
            }

        } catch (e: Exception) {
            // Handle failure
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun uploadPostWithoutImage(description: String, link: String, user: FirebaseUser) {
        try {
            // Create a new post object without image URL but with link
            val username = user.displayName ?: user.email ?: "Anonymous"
            val uid = user.uid

            val post = hashMapOf(
                "uid" to uid,
                "username" to username,
                "description" to description,
                "link" to link, // Store link in Firestore
                "commentsEnabled" to false, // Default to false
                "likesCount" to 0 // Initial likes count
            )

            // Add the post to Firestore
            firestore.collection("posts").add(post).await()

            // Handle success
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Post uploaded successfully", Toast.LENGTH_SHORT).show()

                // Navigate to HomeFragment after successful upload
                navigateBack()
            }

        } catch (e: Exception) {
            // Handle failure
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Failed to add post to Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateBack() {
        val fragmentManager: FragmentManager = requireActivity().supportFragmentManager

        // Replace current fragment with HomeFragment without adding to back stack
        fragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer,AddPostFragment())
            .commit()
    }
}
