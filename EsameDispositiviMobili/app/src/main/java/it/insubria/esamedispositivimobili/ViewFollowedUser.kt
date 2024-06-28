package it.insubria.esamedispositivimobili

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ViewFollowedUser(
    private val context: Context,
    private var userList: List<UserDetails>,
    private val listener: SettingsFragment
) : RecyclerView.Adapter<ViewFollowedUser.UserViewHolder>() {

    private var followedUsers: List<String> = listOf()
    private val db = FirebaseFirestore.getInstance()
    private val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid

    interface OnItemClickListener {
        fun onItemClick(user: UserDetails, followButton: Button)
    }

    fun updateUsers(updatedList: List<UserDetails>) {
        userList = updatedList
        filterFollowedUsers() // Filtra gli utenti seguiti quando la lista viene aggiornata
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.fragment_user_adapter, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        holder.bind(user)
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val usernameTextView: TextView = itemView.findViewById(R.id.usernameTextView)
        private val profileImageView: ImageView = itemView.findViewById(R.id.profileImageView)
        private val followButton: Button = itemView.findViewById(R.id.followButton)

        init {
            followButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val user = userList[position]
                    toggleFollow(user, followButton)
                }
            }
        }

        fun bind(user: UserDetails) {
            usernameTextView.text = user.username

            // Carica l'immagine profilo con Glide
            Glide.with(context)
                .load(user.imageUrl) // Assumi che user.imageUrl contenga l'URL dell'immagine profilo
                .placeholder(R.drawable.bottoncustom) // Immagine placeholder mentre carica
                .error(R.drawable.bottoncustom) // Immagine di fallback in caso di errore
                .centerCrop()
                .into(profileImageView)

            // Verifica se l'utente è seguito o meno
            if (currentUserUid != null) {
                db.collection("users").document(currentUserUid).get()
                    .addOnSuccessListener { document ->
                        if (document != null && document.exists()) {
                            val followedUsers = document.get("followedUsers") as? ArrayList<String> ?: arrayListOf()

                            if (followedUsers.contains(user.username)) {
                                followButton.text = "Non seguire più"
                            } else {
                                followButton.text = "Segui"
                            }
                        }
                        db.collection("users").document(currentUserUid)
                            .update("followedUsers", followedUsers)
                            .addOnSuccessListener {
                                // Toast.makeText(context, "Aggiornamento avvenuto con successo", Toast.LENGTH_SHORT).show()
                                filterFollowedUsers() // Filtra nuovamente la lista degli utenti seguiti dopo l'aggiornamento
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Errore durante l'aggiornamento dei dati utente: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener { exception ->
                        // Gestione dell'errore nel caricamento dei dati utente
                        followButton.isEnabled = false // Disabilita il pulsante in caso di errore
                    }
            }
        }
    }

    private fun filterFollowedUsers() {
        if (currentUserUid != null) {
            db.collection("users").document(currentUserUid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        followedUsers = document.get("followedUsers") as? List<String> ?: listOf()
                        userList = userList.filter { followedUsers.contains(it.username) }
                        notifyDataSetChanged() // Aggiorna la lista visualizzata
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(context, "Errore nel caricamento dei dati dell'utente: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun toggleFollow(user: UserDetails, followButton: Button) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val followedUsers = document.get("followedUsers") as? ArrayList<String> ?: arrayListOf()

                        if (followedUsers.contains(user.username)) {
                            // Utente già seguito, quindi rimuovilo
                            followedUsers.remove(user.username)
                            followButton.text = "Segui"
                        } else {
                            // Utente non seguito, quindi aggiungilo
                            followedUsers.add(user.username)
                            followButton.text = "Non seguire più"
                        }

                        // Aggiorna nel database
                        db.collection("users").document(userId)
                            .update("followedUsers", followedUsers)
                            .addOnSuccessListener {
                                // Toast.makeText(context, "Aggiornamento avvenuto con successo", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Errore durante l'aggiornamento dei dati utente: ${e.message}", Toast.LENGTH_SHORT).show()
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
}
