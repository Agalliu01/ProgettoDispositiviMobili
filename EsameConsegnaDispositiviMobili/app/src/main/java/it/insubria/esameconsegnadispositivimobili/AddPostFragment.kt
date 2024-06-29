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
    // Inflate the layout for this fragment
    val view = inflater.inflate(R.layout.fragment_add_post, container, false)

    // Initialize Firebase Auth
    auth = FirebaseAuth.getInstance()

    // Initialize views
    descriptionEditText = view.findViewById(R.id.descriptionEditText)
    linkEditText = view.findViewById(R.id.linkEditText)
    uploadButton = view.findViewById(R.id.uploadButton)
    selectImageButton = view.findViewById(R.id.selectImageButton)
    imageView = view.findViewById(R.id.imageView)

    // Setup click listener for upload button
    uploadButton.setOnClickListener {
      uploadPost()
    }

    // Setup click listener for select image button
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

  private fun uploadPost() {
    val currentUser = auth.currentUser
    val uid = currentUser?.uid ?: return

    val description = descriptionEditText.text.toString().trim()
    val link = linkEditText.text.toString().trim()

    if (selectedImageUri == null) {
      Toast.makeText(requireContext(), "Seleziona un'immagine prima di pubblicare", Toast.LENGTH_SHORT).show()
      return
    }

    val database = FirebaseDatabase.getInstance()
    val postsRef = database.getReference("posts")
    val userRef = database.getReference("users").child(uid)

    // Recupera lo username dall'Realtime Database
    userRef.addListenerForSingleValueEvent(object : ValueEventListener {
      override fun onDataChange(snapshot: DataSnapshot) {
        val username = snapshot.child("username").getValue(String::class.java) ?: "Anonymous"

        // Procedi con l'upload del post
        val newPostRef = postsRef.push()

        // Upload dell'immagine
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

            // Creazione dell'oggetto Post completo
            val post = Post(
              uid = newPostRef.key ?: "",
              username = username,
              description = description,
              imageUrl = imageUrl,
              link = link,
              likedBy = mutableListOf(),
              comments = mutableListOf()
            )

            // Salvataggio del post nel nodo "posts"
            newPostRef.setValue(post)
              .addOnSuccessListener {
                Toast.makeText(requireContext(), "Post pubblicato con successo", Toast.LENGTH_SHORT).show()

                // Aggiunta del riferimento del post alla listaPost dell'utente
                userRef.child("listaPost").child(newPostRef.key ?: "").setValue(post)

                // Navigazione alla HomeFragment o altro fragment desiderato
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