package it.insubria.esameconsegnadispositivimobili

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

//****Classe adapter utilizzata per visualizzare gli utenti con immagine profilo, username e pulsante follow/unfllow
class AccountAdapter(
    private val context: Context,
    private var userList: List<User>,
    private val listener: OnItemClickListener// non implementato, sarebbe per un listener su click
) : RecyclerView.Adapter<AccountAdapter.UserViewHolder>() {

    private val db = Firebase.database.reference//prendiamo referenze database istanza attuale
    private val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid//utente loggato

    interface OnItemClickListener {
        fun onItemClick(user: User, followButton: Button)
    }

    fun updateUsers(updatedList: List<User>) {
        userList = updatedList//metodo per "aggiornare" dinamicamente la lista utenti con notifica successiva
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.fragment_account_adapter, parent, false)//creiamo la vista dandogli anche adapter e riferimenti
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {//
        val user = userList[position]
        holder.bind(user)
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {//assegnamo valori per username,immagine profilo e followbutton per utenti
        private val usernameTextView: TextView = itemView.findViewById(R.id.usernameTextView)
        private val profileImageView: ImageView = itemView.findViewById(R.id.profileImageView)
        private val followButton: Button = itemView.findViewById(R.id.followButton)

        init {
            followButton.setOnClickListener {//istanziamo un listener per gli utenti in modo tale da permettere il "seguire"
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val user = userList[position]
                    toggleFollow(user, followButton)
                }
            }
        }

        fun bind(user: User) {
            if(user.username.length<=1)//se utente NON possiede username viene visualizzato admin per convenzione
                usernameTextView.text="admin"
            else
                usernameTextView.text = user.username//altrimenti viene visualizzato normale username utente

            profileImageView.setImageResource(R.drawable.icons8_busto_in_sagoma_48)
            // Carica l'immagine profilo con Glide
            Glide.with(context)
                .load(user.profileImageUrl) //
                .placeholder(R.drawable.icons8_busto_in_sagoma_48) // Immagine placeholder mentre carica
                .error(R.drawable.icons8_busto_in_sagoma_48) // Immagine di fallback in caso di errore
                .centerCrop()
                .into(profileImageView)//carichiamo immagine utente
           // toggleFollow(user, followButton)

            // Verifica se l'utente è seguito o meno
            if (currentUserUid != null) {//aggiorniamo dinamicamente il pulsante con sostituzione testo dopo verifica su firebase realtime
                db.child("users").child(currentUserUid).child("utentiSeguiti")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            val followedUsers = dataSnapshot.getValue(object : GenericTypeIndicator<ArrayList<String>>() {})
                            if (followedUsers != null && followedUsers.contains(user.username)) {
                                followButton.text = "Non seguire più"
                            } else {
                                followButton.text = "Segui"
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            // Gestione dell'errore nel caricamento dei dati utente
                            followButton.isEnabled = false // Disabilita il pulsante in caso di errore
                        }
                    })
            }
        }
    }
//FUNZIONE simile al bind, ma viene chiamata poi quando pulsante viene schiacciato, il bind al momento della creazione
    private fun toggleFollow(user: User, followButton: Button) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            db.child("users").child(userId).child("utentiSeguiti").get()
                .addOnSuccessListener { dataSnapshot ->
                    val followedUsers = dataSnapshot.getValue(object : GenericTypeIndicator<ArrayList<String>>() {}) ?: arrayListOf()

                    if (followedUsers.contains(user.username)) {
                        // Utente già seguito, quindi rimuovilo
                        followedUsers.remove(user.username)
                        followButton.text = "Segui"
                     //   followButton.setBackgroundColor(Color.GREEN)
                    } else {
                        // Utente non seguito, quindi aggiungilo
                        followedUsers.add(user.username)
                        followButton.text = "Non seguire più"
                       // followButton.setBackgroundColor(Color.RED)

                    }

                    // Aggiorna nel database
                    db.child("users").child(userId).child("utentiSeguiti").setValue(followedUsers)
                        .addOnSuccessListener {
                       //     Toast.makeText(context, "Aggiornamento avvenuto con successo", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Errore durante l'aggiornamento dei dati utente: ${e.message}", Toast.LENGTH_SHORT).show()
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