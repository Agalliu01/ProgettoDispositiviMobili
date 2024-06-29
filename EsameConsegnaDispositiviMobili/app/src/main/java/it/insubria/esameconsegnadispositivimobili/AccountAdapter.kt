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
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*

class AccountAdapter(
    private val context: Context,
    private var userList: List<User>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<AccountAdapter.UserViewHolder>() {

    private val db = FirebaseDatabase.getInstance().reference
    private val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid

    interface OnItemClickListener {
        fun onItemClick(user: User, followButton: Button)
    }

    fun updateUsers(updatedList: List<User>) {
        userList = updatedList
        notifyDataSetChanged()
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
                    listener.onItemClick(user, followButton)
                }
            }
        }

        fun bind(user: User) {
            usernameTextView.text = user.Username

            // Carica l'immagine profilo con Glide
            Glide.with(context)
                .load(user.profileImageUrl)
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .centerCrop()
                .into(profileImageView)

            // Verifica se l'utente è seguito o meno
            currentUserUid?.let { uid ->
                db.child("users").child(uid).child("utentiSeguiti").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val followedUsers = snapshot.getValue(object : GenericTypeIndicator<List<String>>() {}) ?: listOf()

                        if (followedUsers.contains(user.uid)) {
                            followButton.text = "Non seguire più"
                        } else {
                            followButton.text = "Segui"
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(context, "Errore nel caricamento dei dati utente: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }
    }
}
