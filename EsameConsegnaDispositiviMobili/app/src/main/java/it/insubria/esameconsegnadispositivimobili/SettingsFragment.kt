package it.insubria.esameconsegnadispositivimobili

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
import com.google.firebase.database.*
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
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage
    private lateinit var storageRef: StorageReference
    private var userList: List<User> = emptyList()
    private var filteredUserList: List<User> = emptyList()

    private var imageUri: Uri? = null

    companion object {
        private const val PICK_IMAGE_REQUEST = 71
        private const val STORAGE_PERMISSION_CODE = 101
    }

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
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()
        storageRef = storage.reference

        followedUserAdapter = ViewFollowedUser(requireContext(), userList, this)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = followedUserAdapter

        val logoutBtn = view.findViewById<Button>(R.id.logoutBtn)
        logoutBtn.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            Toast.makeText(context, "Logout effettuato con successo", Toast.LENGTH_SHORT).show()

            val intent = Intent(requireActivity(), LoginMainActivityLauncher::class.java)
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
        val usersRef = database.reference.child("users")

        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val tempUserList = mutableListOf<User>()

                for (userSnapshot in dataSnapshot.children) {
                    try {
                        val user = userSnapshot.getValue(User::class.java)
                        if (user != null && user.uid != currentUserUid) {
                            tempUserList.add(user)
                        }
                    } catch (e: Exception) {
                        Log.e("Firebase", "Error converting user snapshot", e)
                    }
                }

                filteredUserList = tempUserList
                followedUserAdapter.updateUsers(filteredUserList)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("Firebase", "Error fetching users", databaseError.toException())
            }
        })
    }

    private fun observeUserData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val userRef = database.reference.child("users").child(userId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val user = dataSnapshot.getValue(User::class.java)
                    user?.let {
                        usernameTextView.text = "Username attuale: ${it.username ?: ""}"
                        emailTextView.text = "Email attuale: ${it.email ?: ""}"
                        followingTextView.text = "Visualizza Account seguiti: ${it.utentiSeguiti?.size ?: 0}"
                        nomeTextView.text = "Nome attuale: ${it.nome ?: ""}"
                        cognomeTextView.text = "Cognome attuale: ${it.cognome ?: ""}"
                        if (!it.profileImageUrl.isNullOrEmpty()) {
                            Picasso.get().load(it.profileImageUrl).into(profileImageView)
                        } else {
                            // Usa un'immagine di default se l'URL è vuoto
                            profileImageView.setImageResource(R.drawable.icons8_busto_in_sagoma_48)
                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("Firebase", "Error fetching user data", databaseError.toException())
            }
        })
    }

    private fun salvaModifiche() {
        val user = FirebaseAuth.getInstance().currentUser
        val nuovoNome = view?.findViewById<EditText>(R.id.nomeEditText)?.text.toString().trim()
        val nuovoCognome = view?.findViewById<EditText>(R.id.cognomeEditText)?.text.toString().trim()
        val nuovoUsername = view?.findViewById<EditText>(R.id.usernameEditText)?.text.toString().trim()
        val nuovaEmail = view?.findViewById<EditText>(R.id.emailEditText)?.text.toString().trim()
        val nuovaPassword = view?.findViewById<EditText>(R.id.passwordEditText)?.text.toString().trim()

        val profileUpdatesBuilder = UserProfileChangeRequest.Builder()

        // Aggiornamento del nome utente se fornito
        if (nuovoUsername.isNotEmpty()) {
            profileUpdatesBuilder.setDisplayName(nuovoUsername)
        }

        // Costruisci le modifiche al profilo
        val profileUpdates = profileUpdatesBuilder.build()

        user?.updateProfile(profileUpdates)?.addOnCompleteListener { profileUpdateTask ->
            if (profileUpdateTask.isSuccessful) {
                // Aggiornamento del profilo completato con successo
                // Aggiorna l'email solo se fornita e diversa dall'attuale
                if (nuovaEmail.isNotEmpty() && nuovaEmail != user.email) {
                    user.updateEmail(nuovaEmail).addOnCompleteListener { emailUpdateTask ->
                        if (emailUpdateTask.isSuccessful) {
                            // Aggiornamento dell'email completato con successo
                            Toast.makeText(context, "Email aggiornata con successo", Toast.LENGTH_SHORT).show()
                        } else {
                            // Gestione dell'errore di aggiornamento dell'email
                            Toast.makeText(context, "Errore durante l'aggiornamento dell'email: ${emailUpdateTask.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                // Aggiornamento della password solo se fornita
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

                // Solo se tutto è andato bene, esegui l'aggiornamento nel Realtime Database
                val dataToUpdate = mutableMapOf<String, Any>()
                if (nuovoNome.isNotEmpty()) {
                    dataToUpdate["nome"] = nuovoNome
                }
                if (nuovoCognome.isNotEmpty()) {
                    dataToUpdate["cognome"] = nuovoCognome
                }
                if (nuovoUsername.isNotEmpty()) {
                    dataToUpdate["username"] = nuovoUsername
                }

                // Aggiorna nel Realtime Database solo se ci sono modifiche da applicare
                if (dataToUpdate.isNotEmpty()) {
                    val userId = user?.uid ?: ""
                    val userRef = database.reference.child("users").child(userId)
                    userRef.updateChildren(dataToUpdate)
                        .addOnSuccessListener {
                            // Dopo aver aggiornato i dati, aggiorna l'UI
                            observeUserData() // Questo metodo aggiorna l'interfaccia utente con i nuovi dati
                            Toast.makeText(context, "Modifiche salvate con successo", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Errore durante il salvataggio delle modifiche: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            } else {
                // Gestione dell'errore di aggiornamento del profilo
                Toast.makeText(context, "Errore durante l'aggiornamento del profilo: ${profileUpdateTask.exception?.message}", Toast.LENGTH_SHORT).show()
            }
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
                    val userRef = database.reference.child("users").child(userId)
                    userRef.child("profileImageUrl").setValue(downloadUri.toString())
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            imageUri = data.data
            profileImageView.setImageURI(imageUri)
            uploadImageToFirebase()
        }
    }
}
