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


//***Fragment collegato a navbar che permette l'agigunta di un post con Descrizione, immagine, link fonte e username account
class AddPostFragment : Fragment() {

  private lateinit var descriptionEditText: EditText//variabili inizializzate dopo
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
  //istanziamo variabili per post
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

  private fun selectImage() {//metodo per permette di prelevare immagine
    val intent = Intent(Intent.ACTION_GET_CONTENT)//ACTION_GET_CONTENT, permette da prelevarlo da dispositivo completo e NON solo da foto!
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
//Le condizioni per postare possono essere ovviamente cambiate,scelte queste semplicemente per questione "visiva
  //potre permettere anche di mettere post senza immagine o senza nulla, per questione "visiva", ho preferito metterle cosi...
  private fun validateAndUploadPost() {//ci sono condizioni per upload post
    val currentUser = auth.currentUser
    val uid = currentUser?.uid ?: return

    val description = descriptionEditText.text.toString().trim()
    val link = linkEditText.text.toString().trim()

    if (description.length > 500) {//se descrizione e piu di 500 caratteri, NON viene permesso inserimento
      Toast.makeText(
        requireContext(),
        "La descrizione non può superare i 500 caratteri",
        Toast.LENGTH_SHORT
      ).show()
      return
    }

    if (selectedImageUri == null) {//NON viene permesso pubblicare senza immagine,MA viene permesso pubblicare senza descrizione
      Toast.makeText(
        requireContext(),
        "Seleziona un'immagine prima di pubblicare",
        Toast.LENGTH_SHORT
      ).show()
      return
    }

    uploadPost(description, link)
  }

  private fun uploadPost(description: String, link: String) {//carichiamo il post
    val currentUser = auth.currentUser ?: return
    var username = "admin"//Inutile inizilaizzazione in quanto nel mostrare post, se poi NOn esiste username, associamo admin su postadapter!
    val database = FirebaseDatabase.getInstance()
    val postsRef = database.getReference("posts")//sezione post per aricarlo
    val usernameRef = database.getReference("users").child(currentUser.uid).child("username")//prelevo username associatyo a utente
    val newPostKey = postsRef.push().key ?: ""
    val commentsUidLista = postsRef.child(newPostKey).child("comments").push().key ?: ""

    usernameRef.addListenerForSingleValueEvent(object : ValueEventListener {
      override fun onDataChange(dataSnapshot: DataSnapshot) {
        username = dataSnapshot.value as String
      }

      override fun onCancelled(databaseError: DatabaseError) {
        println("Errore nel recupero dell'username: ${databaseError.message}")
      }
    })

    val storageRef = FirebaseStorage.getInstance().reference//prelevo immagine
    val imageRef = storageRef.child("images/$newPostKey.jpg")
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
//creo oggetto post con info prelevate in precedenda
        val post = Post(
          uid = newPostKey,
          uidAccount = currentUser.uid,//riferimento univoco a chi ha pubblicato il post!!!
          username = username,
          description = description,
          imageUrl = imageUrl,
          link = link,
          likedBy = mutableListOf(),
          comments = mutableListOf(),
          commentsUidLista = commentsUidLista,
        )

        postsRef.child(newPostKey).setValue(post)
          .addOnSuccessListener {
            Toast.makeText(requireContext(), "Post pubblicato con successo", Toast.LENGTH_SHORT).show()
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

  private fun navigateToFragment(fragment: Fragment) {//quando viene pubblicato post, visualizzazione immediata in home!
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