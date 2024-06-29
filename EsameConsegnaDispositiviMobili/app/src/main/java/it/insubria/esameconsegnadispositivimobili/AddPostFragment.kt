package it.insubria.esameconsegnadispositivimobili

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.FragmentTransaction
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage


class AddPostFragment : Fragment() {




  private lateinit var descriptionEditText: EditText
  private lateinit var linkEditText: EditText
  private lateinit var uploadButton: Button
  private lateinit var selectImageButton: Button
  private lateinit var imageView: ImageView

  private lateinit var auth: FirebaseAuth
  private var selectedImageUri: Uri? = null

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val view = inflater.inflate(R.layout.fragment_add_post, container, false)
    auth = FirebaseAuth.getInstance()

    descriptionEditText = view.findViewById(R.id.descriptionEditText)
    linkEditText = view.findViewById(R.id.linkEditText)
    uploadButton = view.findViewById(R.id.uploadButton)
    selectImageButton = view.findViewById(R.id.selectImageButton)
    imageView = view.findViewById(R.id.imageView)

    uploadButton.setOnClickListener {
      validateAndUploadPost()
    }

    selectImageButton.setOnClickListener {
      selectImage()
    }

    return view
  }

  private fun selectImage() {
    val intent = Intent(Intent.ACTION_GET_CONTENT)
    intent.type = "image/*"
    startActivityForResult(intent, RC_IMAGE_PICKER)
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    if (requestCode == RC_IMAGE_PICKER && resultCode == Activity.RESULT_OK) {
      data?.data?.let { uri ->
        selectedImageUri = uri
        Glide.with(this)
          .load(uri)
          .centerCrop()
          .into(imageView)
      }
    }
  }

  private fun validateAndUploadPost() {
    val currentUser = auth.currentUser
    val uid = currentUser?.uid ?: return

    val description = descriptionEditText.text.toString().trim()
    val link = linkEditText.text.toString().trim()

    if (description.length > 500) {
      Toast.makeText(requireContext(), "La descrizione non può superare i 500 caratteri", Toast.LENGTH_SHORT).show()
      return
    }

    if (selectedImageUri == null) {
      Toast.makeText(requireContext(), "Seleziona un'immagine prima di pubblicare", Toast.LENGTH_SHORT).show()
      return
    }

    uploadPost(description, link)
  }

  private fun uploadPost(description: String, link: String) {
    val currentUser = auth.currentUser
    val uid = currentUser?.uid ?: return

    val database = FirebaseDatabase.getInstance()
    val postsRef = database.getReference("posts")
    val userRef = database.getReference("users").child(uid)

    userRef.addListenerForSingleValueEvent(object : ValueEventListener {
      override fun onDataChange(snapshot: DataSnapshot) {
        val username = snapshot.child("username").getValue(String::class.java) ?: "Anonymous"

        val newPostRef = postsRef.push()

        // Genera un identificatore univoco per commentsUidLista
        val commentsUidLista = newPostRef.child("comments").push().key ?: ""

        val storageRef = FirebaseStorage.getInstance().reference
        val imageRef = storageRef.child("images/${newPostRef.key}.jpg")
        val uploadTask = imageRef.putFile(selectedImageUri!!)

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
            val imageUrl = downloadUri.toString()

            val post = Post(
              uid = newPostRef.key ?: "",
              uidAccount = currentUser.uid,
              username = username,
              description = description,
              imageUrl = imageUrl,
              link = link,
              likedBy = mutableListOf(),
              comments = mutableListOf(), // Lista vuota, senza commenti iniziali
              commentsUidLista = commentsUidLista // Aggiungi l'identificatore univoco per i commenti
            )
            newPostRef.setValue(post)
              .addOnSuccessListener {
                Toast.makeText(requireContext(), "Post pubblicato con successo", Toast.LENGTH_SHORT).show()

                userRef.child("listaPost").child(newPostRef.key ?: "").setValue(post)

                navigateToFragment(HomeFragment())
              }
              .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Errore durante il salvataggio del post: ${e.message}", Toast.LENGTH_SHORT).show()
              }
          } else {
            Toast.makeText(requireContext(), "Errore durante il caricamento dell'immagine", Toast.LENGTH_SHORT).show()
          }
        }
      }

      override fun onCancelled(error: DatabaseError) {
        Toast.makeText(requireContext(), "Errore durante il recupero dei dati utente", Toast.LENGTH_SHORT).show()
      }
    })
  }




  private fun navigateToFragment(fragment: Fragment) {
    val fragmentManager = parentFragmentManager
    val fragmentTransaction = fragmentManager.beginTransaction()
    fragmentTransaction.replace(R.id.fragmentContainer, fragment)
    fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
    fragmentTransaction.addToBackStack(null)
    fragmentTransaction.commit()
  }

  companion object {
    private const val RC_IMAGE_PICKER = 123
  }
}