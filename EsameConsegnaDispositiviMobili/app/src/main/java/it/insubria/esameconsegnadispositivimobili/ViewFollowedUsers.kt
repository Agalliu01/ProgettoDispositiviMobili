package it.insubria.esameconsegnadispositivimobili

import android.content.Context
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
import com.google.firebase.database.*

class ViewFollowedUser(
    private val context: Context,
    private var userList: List<User>,
    private val listener: SettingsFragment
) : RecyclerView.Adapter<ViewFollowedUser.UserViewHolder>() {

    private var followedUsers: List<String> = listOf()
    private val database = FirebaseDatabase.getInstance()
    private val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid

    interface OnItemClickListener {
        fun onItemClick(user: User, followButton: Button)
    }

    fun updateUsers(updatedList: List<User>) {
        userList = updatedList
        filterFollowedUsers() // Filtra gli utenti seguiti quando la lista viene aggiornata
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.fragment_account_adapter, parent, false)
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

        fun bind(user: User) {
            usernameTextView.text = user.username

            // Carica l'immagine profilo con Glide
            Glide.with(context)
                .load(user.profileImageUrl) // Assumi che user.profileImageUrl contenga l'URL dell'immagine profilo
                .placeholder(R.drawable.icons8_busto_in_sagoma_48) // Immagine placeholder mentre carica
                .error(R.drawable.icons8_busto_in_sagoma_48) // Immagine di fallback in caso di errore
                .centerCrop()
                .into(profileImageView)

            // Verifica se l'utente è seguito o meno
            if (currentUserUid != null) {
                val followedUsersRef = database.reference.child("users").child(currentUserUid).child("utentiSeguiti")
                followedUsersRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val followedUsers = dataSnapshot.value as? List<String> ?: listOf()

                        if (followedUsers.contains(user.username)) {
                            followButton.text = "Non seguire più"
                        } else {
                            followButton.text = "Segui"
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Toast.makeText(context, "Errore nel caricamento dei dati dell'utente: ${databaseError.message}", Toast.LENGTH_SHORT).show()
                        followButton.isEnabled = false // Disabilita il pulsante in caso di errore
                    }
                })
            }
        }
    }

    private fun filterFollowedUsers() {
        if (currentUserUid != null) {
            val followedUsersRef = database.reference.child("users").child(currentUserUid).child("utentiSeguiti")
            followedUsersRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    followedUsers = dataSnapshot.value as? List<String> ?: listOf()
                    userList = userList.filter { followedUsers.contains(it.username) }
                    notifyDataSetChanged() // Aggiorna la lista visualizzata
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(context, "Errore nel caricamento dei dati dell'utente: ${databaseError.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun toggleFollow(user: User, followButton: Button) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val followedUsersRef = database.reference.child("users").child(userId).child("utentiSeguiti")
            followedUsersRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val followedUsers = dataSnapshot.value as? MutableList<String> ?: mutableListOf()

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
                    followedUsersRef.setValue(followedUsers)
                        .addOnSuccessListener {
                            // Toast.makeText(context, "Aggiornamento avvenuto con successo", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Errore durante l'aggiornamento dei dati utente: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(context, "Errore nel caricamento dei dati dell'utente: ${databaseError.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Toast.makeText(context, "Utente non autenticato", Toast.LENGTH_SHORT).show()
        }
    }
}
