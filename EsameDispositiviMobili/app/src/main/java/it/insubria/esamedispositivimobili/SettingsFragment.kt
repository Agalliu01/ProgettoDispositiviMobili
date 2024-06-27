package it.insubria.esamedispositivimobili

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso

class SettingsFragment : Fragment() {
    // Dichiarazioni delle proprietà
    private lateinit var profileImageView: ImageView
    private lateinit var changeImageIcon: ImageView
    private lateinit var usernameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var followingTextView: TextView
    private lateinit var nomeTextView: TextView
    private lateinit var cognomeTextView: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var followedUserAdapter: ViewFollowedUser
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var storageRef: StorageReference
    private var userList: List<UserDetails> = emptyList()
    private var filteredUserList: List<UserDetails> = emptyList()

    private var imageUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        // Inizializzazione delle viste
        profileImageView = view.findViewById(R.id.profileImageView)
        changeImageIcon = view.findViewById(R.id.changeImageIcon)
        usernameTextView = view.findViewById(R.id.usernameTextView)
        emailTextView = view.findViewById(R.id.emailTextView)
        followingTextView = view.findViewById(R.id.followingTextView)
        nomeTextView = view.findViewById(R.id.nomeTextView)
        cognomeTextView = view.findViewById(R.id.cognomeTextView)

        // Inizializzazione della RecyclerView
        recyclerView = view.findViewById(R.id.recyclerView) // Sostituisci con l'ID corretto della tua RecyclerView

        // Inizializzazione di Firebase
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        storageRef = storage.reference

        // Inizializzazione dell'adapter per la RecyclerView
        followedUserAdapter = ViewFollowedUser(requireContext(), userList, this)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = followedUserAdapter




        // Gestione del click su FollowingTextView
        followingTextView.setOnClickListener {
            loadUsers()
        }

        //da inizializzare adapter

        nomeTextView.setOnClickListener {
            changeInfoAccount()
        }




        // Gestione del click su profileImageView per cambiare l'immagine del profilo
        profileImageView.setOnClickListener {
            changeImageIcon.visibility = View.VISIBLE
        }

        // Gestione del click su changeImageIcon per selezionare un'immagine da caricare
        changeImageIcon.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        // Osserva i dati dell'utente e aggiorna l'UI di conseguenza
        observeUserData()

        return view
    }

    private fun changeInfoAccount() {
        TODO("Not yet implemented")













    }

    // Metodo per caricare gli utenti seguiti dall'utente corrente
    // Metodo per caricare gli utenti seguiti dall'utente corrente
    private fun loadUsers() {
        val db = FirebaseFirestore.getInstance()
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid

        db.collection("users")
            .get()
            .addOnSuccessListener { documents ->
                val tempUserList = mutableListOf<UserDetails>()

                for (document in documents) {
                    try {
                        val user = document.toObject(UserDetails::class.java)
                        // Escludi l'utente corrente
                        if (user.username != currentUserUid) {
                            tempUserList.add(user)
                        }
                    } catch (e: Exception) {
                        // Gestisci l'eccezione nel caso in cui la deserializzazione fallisca
                        Log.e("Firestore", "Error converting document", e)
                    }
                }

                // Aggiorna la lista filtrata e l'adattatore
                filteredUserList = tempUserList
                followedUserAdapter.updateUsers(filteredUserList)
            }
            .addOnFailureListener { exception ->
                // Gestisci l'errore
                Log.e("Firestore", "Error getting documents", exception)
            }
    }


    // Metodo per osservare i dati dell'utente e aggiornare l'UI di conseguenza
    private fun observeUserData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val followedUsers = document.get("followedUsers") as? List<*>
                    val followingCount = followedUsers?.size ?: 0

                    // Aggiorna l'UI con i dati dell'utente
                    usernameTextView.text = document.getString("username") ?: ""
                    emailTextView.text = document.getString("email") ?: ""
                    followingTextView.text = "Seguiti : $followingCount"
                    nomeTextView.text = document.getString("nome") ?: ""
                    cognomeTextView.text = document.getString("cognome") ?: ""

                    val imageUrl = document.getString("imageUrl")
                    if (!imageUrl.isNullOrEmpty()) {
                        Picasso.get().load(imageUrl).into(profileImageView)
                    } else {
                        // Gestione del caso in cui imageUrl sia vuoto o null, ad esempio, impostando un'immagine di default
                        Picasso.get().load(R.drawable.ic_launcher_background).into(profileImageView)
                    }


                //    Picasso.get().load(document.getString("imageUrl")).into(profileImageView)
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(context, "Errore nel caricamento dei dati dell'utente: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Gestione del risultato dell'attività di selezione immagine
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            imageUri = data.data
            uploadImageToFirebase()
        }
    }

    // Metodo per caricare un'immagine su Firebase Storage e aggiornare l'URL nell'utente corrente
    private fun uploadImageToFirebase() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null && imageUri != null) {
            val imageFileName = "profile_images/${userId}.jpg"
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
                    db.collection("users").document(userId).update("imageUrl", downloadUri.toString())
                        .addOnSuccessListener {
                            Picasso.get().load(downloadUri).into(profileImageView)
                            changeImageIcon.visibility = View.GONE
                            Toast.makeText(context, "Immagine del profilo aggiornata con successo", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Errore durante l'aggiornamento dell'immagine del profilo: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(context, "Errore durante il caricamento dell'immagine", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        private const val PICK_IMAGE_REQUEST = 71
    }
}
