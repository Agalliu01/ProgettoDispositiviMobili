package it.insubria.esamedispositivimobili

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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
    private lateinit var profileImageView: ImageView
    private lateinit var changeImageIcon: ImageView
    private lateinit var usernameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var followingTextView: TextView
    private lateinit var nomeTextView: TextView
    private lateinit var cognomeTextView: TextView

    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var storageRef: StorageReference

    private var imageUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        profileImageView = view.findViewById(R.id.profileImageView)
        changeImageIcon = view.findViewById(R.id.changeImageIcon)
        usernameTextView = view.findViewById(R.id.usernameTextView)
        emailTextView = view.findViewById(R.id.emailTextView)
        followingTextView = view.findViewById(R.id.followingTextView)
        nomeTextView = view.findViewById(R.id.nomeTextView)
        cognomeTextView = view.findViewById(R.id.cognomeTextView)

        db = FirebaseFirestore.getInstance()

        storage = FirebaseStorage.getInstance()
        storageRef = storage.reference

        observeUserData()

        profileImageView.setOnClickListener {
            changeImageIcon.visibility = View.VISIBLE
        }

        changeImageIcon.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        followingTextView.setOnClickListener {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null) {
                db.collection("users").document(userId).get()
                    .addOnSuccessListener { document ->
                        if (document != null && document.exists()) {
                            val followedUsers = document.get("followedUsers") as List<*>
                            val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid

                            if (currentUserUid != null) {
                                if (followedUsers.contains(currentUserUid)) {
                                    // Utente già seguito, quindi mostra l'elenco degli utenti seguiti
                                    showFollowedUsersDialog(followedUsers as ArrayList<String>)
                                } else {
                                    Toast.makeText(context, "Non stai seguendo nessun utente al momento", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(context, "Errore nel caricamento dei dati dell'utente: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(context, "Utente non autenticato", Toast.LENGTH_SHORT).show()
            }
        }
        return view
    }

        // Funzione per mostrare un dialog con l'elenco degli utenti seguiti
        private fun showFollowedUsersDialog(followedUsers: ArrayList<String>) {
            // Inflate the dialog view with the appropriate layout
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_followed_users, null)

            // Find the RecyclerView in the dialog view
            val recyclerView = dialogView.findViewById<RecyclerView>(R.id.followedUsersRecyclerView)

            // Set up RecyclerView with LinearLayoutManager
            recyclerView.layoutManager = LinearLayoutManager(context)

            // Create and set adapter for the RecyclerView
            val adapter = UserAdapter(requireContext(), getUserDetailsList(followedUsers), object : UserAdapter.OnItemClickListener {
                override fun onItemClick(user: UserDetails, followButton: Button) {
                    // Handle item click if needed
                }
            })
            recyclerView.adapter = adapter

            // Build and show the dialog
            val dialogBuilder = AlertDialog.Builder(context)
                .setView(dialogView)
                .setTitle("Utenti Seguiti")
                .setPositiveButton("Chiudi") { dialog, _ ->
                    dialog.dismiss()
                }

            val dialog = dialogBuilder.create()
            dialog.show()
        }

    // Helper function to convert followedUsers IDs to UserDetails list
    private fun getUserDetailsList(followedUsers: ArrayList<String>): List<UserDetails> {
        // Assuming you have a function to fetch UserDetails from IDs or you already have them
        // This is just a placeholder, implement as per your logic
        return followedUsers.map { userId ->
            UserDetails(userId, "Username $userId", "https://example.com/$userId.jpg")
        }
    }



    private fun observeUserData() {
        // Simula il caricamento dei dati dell'utente
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    usernameTextView.text = document.getString("username") ?: ""
                    emailTextView.text = document.getString("email") ?: ""
                    followingTextView.text = "Following: ${document.getLong("following") ?: 0}"
                    nomeTextView.text = document.getString("nome") ?: ""
                    cognomeTextView.text = document.getString("cognome") ?: ""
                    Picasso.get().load(document.getString("imageUrl")).into(profileImageView)
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(context, "Errore nel caricamento dei dati dell'utente: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            imageUri = data.data
            uploadImageToFirebase()
        }
    }

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

