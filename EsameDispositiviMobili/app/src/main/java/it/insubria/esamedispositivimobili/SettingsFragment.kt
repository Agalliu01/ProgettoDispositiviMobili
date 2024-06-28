package it.insubria.esamedispositivimobili

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
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

        profileImageView = view.findViewById(R.id.profileImageView)
        changeImageIcon = view.findViewById(R.id.changeImageIcon)
        usernameTextView = view.findViewById(R.id.usernameTextView)
        emailTextView = view.findViewById(R.id.emailTextView)
        followingTextView = view.findViewById(R.id.followingTextView)
        nomeTextView = view.findViewById(R.id.nomeTextView)
        cognomeTextView = view.findViewById(R.id.cognomeTextView)

        recyclerView = view.findViewById(R.id.recyclerView)
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        storageRef = storage.reference

        followedUserAdapter = ViewFollowedUser(requireContext(), userList, this)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = followedUserAdapter
        val logoutBtn = view.findViewById<Button>(R.id.logoutBtn)
        logoutBtn.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            Toast.makeText(context, "Logout effettuato con successo", Toast.LENGTH_SHORT).show()

            val intent = Intent(requireActivity(), LoginMainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }




        followingTextView.setOnClickListener {
            loadUsers()
        }

        profileImageView.setOnClickListener {
            changeImageIcon.visibility = View.VISIBLE
        }


        changeImageIcon.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        val confermaButton = view.findViewById<Button>(R.id.confermaButton)
        confermaButton.setOnClickListener {
            salvaModifiche()
        }

        observeUserData()

        return view
    }

    private fun loadUsers() {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid

        db.collection("users")
            .get()
            .addOnSuccessListener { documents ->
                val tempUserList = mutableListOf<UserDetails>()

                for (document in documents) {
                    try {
                        val user = document.toObject(UserDetails::class.java)
                        if (user.username != currentUserUid) {
                            tempUserList.add(user)
                        }
                    } catch (e: Exception) {
                        Log.e("Firestore", "Error converting document", e)
                    }
                }

                filteredUserList = tempUserList
                followedUserAdapter.updateUsers(filteredUserList)
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error getting documents", exception)
            }
    }

    private fun observeUserData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val followedUsers = document.get("followedUsers") as? List<*>
                    val followingCount = followedUsers?.size ?: 0

                    usernameTextView.text = "Username attuale: ${document.getString("username") ?: ""}"
                    emailTextView.text = "Email attuale: ${document.getString("email") ?: ""}"
                    followingTextView.text = "Visualizza Account seguiti: $followingCount "
                    nomeTextView.text = "Nome attuale: ${document.getString("nome") ?: ""}"
                    cognomeTextView.text = "Cognome attuale: ${document.getString("cognome") ?: ""}"
                    Picasso.get().load(document.getString("imageUrl")).into(profileImageView)
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(context, "Errore nel caricamento dei dati dell'utente: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun salvaModifiche() {
        val user = FirebaseAuth.getInstance().currentUser
        val nuovoNome = view?.findViewById<EditText>(R.id.nomeEditText)?.text.toString().trim()
        val nuovoCognome = view?.findViewById<EditText>(R.id.cognomeEditText)?.text.toString().trim()
        val nuovoUsername = view?.findViewById<EditText>(R.id.usernameEditText)?.text.toString().trim()
        val nuovaEmail = view?.findViewById<EditText>(R.id.emailEditText)?.text.toString().trim()
        val nuovaPassword = view?.findViewById<EditText>(R.id.passwordEditText)?.text.toString().trim()

        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(nuovoUsername) // Aggiorna il nome utente se necessario
            .build()

        user?.updateProfile(profileUpdates)?.addOnCompleteListener { profileUpdateTask ->
            if (profileUpdateTask.isSuccessful) {
                // Aggiornamento del profilo completato con successo
                user.updateEmail(nuovaEmail).addOnCompleteListener { emailUpdateTask ->
                    if (emailUpdateTask.isSuccessful) {
                        // Aggiornamento dell'email completato con successo
                        Toast.makeText(context, "Email aggiornata con successo", Toast.LENGTH_SHORT).show()
                    } else {
                        // Gestione dell'errore di aggiornamento dell'email
                        Toast.makeText(context, ": ${emailUpdateTask.exception?.message}", Toast.LENGTH_SHORT).show()
                    }

                    // Aggiornamento della password, se è stata fornita una nuova password
                    if (nuovaPassword.isNotEmpty()) {
                        user.updatePassword(nuovaPassword).addOnCompleteListener { passwordUpdateTask ->
                            if (passwordUpdateTask.isSuccessful) {
                                // Aggiornamento della password completato con successo
                                Toast.makeText(context, "Password aggiornata con successo", Toast.LENGTH_SHORT).show()
                            } else {
                                // Gestione dell'errore di aggiornamento della password
                                Toast.makeText(context, "Errore durante l'aggiornamento della password: ${passwordUpdateTask.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            } else {
                // Gestione dell'errore di aggiornamento del profilo
                Toast.makeText(context, "Errore durante l'aggiornamento del profilo: ${profileUpdateTask.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun verificaUnicitaUsername(userId: String, nuovoUsername: String, dataToUpdate: MutableMap<String, Any>) {
        db.collection("users")
            .whereEqualTo("username", nuovoUsername)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty || (documents.documents.size == 1 && documents.documents[0].id == userId)) {
                    eseguiUpdateFirestore(userId, dataToUpdate)
                } else {
                    Toast.makeText(context, "Username già in uso", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error getting documents", exception)
            }
    }

    private fun eseguiUpdateFirestore(userId: String, dataToUpdate: MutableMap<String, Any>) {
        db.collection("users").document(userId)
            .update(dataToUpdate)
            .addOnSuccessListener {
                // Dopo aver aggiornato i dati, aggiorna l'UI
                observeUserData() // Questo metodo aggiorna l'interfaccia utente con i nuovi dati
                Toast.makeText(context, "Modifiche salvate con successo", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Errore durante il salvataggio delle modifiche: ${e.message}", Toast.LENGTH_SHORT).show()
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