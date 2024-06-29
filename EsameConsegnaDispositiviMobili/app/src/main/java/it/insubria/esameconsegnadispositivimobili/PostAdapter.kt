package it.insubria.esameconsegnadispositivimobili

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class PostAdapter(
    private var postList: MutableList<Post>,
    private val isPersonalSection: Boolean
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    private val TAG = "PostAdapter"

    private val database = FirebaseDatabase.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid

    init {
        listenForPostChanges()
    }

    private fun listenForPostChanges() {
        val postsRef = database.getReference("posts")

        postsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                postList.clear()
                for (postSnapshot in snapshot.children) {
                    val post = postSnapshot.getValue(Post::class.java)
                    post?.let {
                        postList.add(it)
                    }
                }
                notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Listen failed.", error.toException())
            }
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_post_adapter, parent, false)
        return PostViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val currentPost = postList[position]

        holder.usernameTextView.text = currentPost.username
        holder.descriptionTextView.text = currentPost.description
        holder.likesCountTextView.text = "Mi piace: ${currentPost.likedBy.size}"

        // Caricamento dell'immagine del post
        Glide.with(holder.postImageView.context)
            .load(currentPost.imageUrl)
            .into(holder.postImageView)

        // Mostra/nascondi l'icona di eliminazione del post se è nella sezione "Personali"
        holder.deleteIcon.visibility = if (isPersonalSection) View.VISIBLE else View.GONE

        // Gestione del click sull'icona per eliminare il post
        holder.deleteIcon.setOnClickListener {
            deletePost(holder.itemView.context, currentPost)
        }

        // Gestione del click sull'icona dei "mi piace"
        holder.likeIcon.setOnClickListener {
            toggleLike(currentPost)
        }

        // Verifica se l'utente corrente ha già messo "mi piace" al post corrente
        if (currentUserUid != null && currentPost.likedBy.contains(currentUserUid)) {
            holder.likeIcon.setImageResource(R.drawable.ic_launcher_background)
        } else {
            holder.likeIcon.setImageResource(R.drawable.ic_launcher_background)
        }
    }

    override fun getItemCount(): Int {
        return postList.size
    }

    // Metodo per eliminare il post dal Realtime Database, da "users/listaPost" e dallo Storage Firebase
    private fun deletePost(context: Context, post: Post) {
        val postsRef = database.getReference("posts").child(post.uid)
        val userRef = database.getReference("users").child(currentUserUid ?: "").child("listaPost").child(post.uid)

        postsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val imageUrl = snapshot.child("imageUrl").getValue(String::class.java)

                // Elimina l'immagine dallo Storage Firebase (se necessario)
                imageUrl?.let {
                    val storageRef = storage.getReferenceFromUrl(it)
                    storageRef.delete()
                        .addOnSuccessListener {
                            Log.d(TAG, "Immagine eliminata dallo Storage Firebase")
                        }
                        .addOnFailureListener { exception ->
                            Log.e(TAG, "Errore durante l'eliminazione dell'immagine dallo Storage Firebase", exception)
                        }
                }

                // Elimina il post da "posts"
                postsRef.removeValue()

                // Elimina il post da "users/listaPost"
                userRef.removeValue()
                    .addOnSuccessListener {
                        Log.d(TAG, "Post rimosso da users/listaPost con successo")
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "Errore durante la rimozione del post da users/listaPost", exception)
                    }

                // Rimuovi il post dalla lista locale
                postList.remove(post)
                notifyDataSetChanged()

                Toast.makeText(context, "Post eliminato con successo", Toast.LENGTH_SHORT).show()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Delete operation cancelled.", error.toException())
                Toast.makeText(context, "Errore durante l'eliminazione del post", Toast.LENGTH_SHORT).show()
            }
        })
    }


    // Aggiunge o rimuove un "mi piace" dal post nel Realtime Database
    private fun toggleLike(post: Post) {
        val postsRef = database.getReference("posts").child(post.uid)
        val likedByRef = postsRef.child("likedBy")

        likedByRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val likedBy = snapshot.getValue(MutableList::class.java)?.filterIsInstance<String>()?.toMutableList()
                    ?: mutableListOf()

                if (currentUserUid != null) {
                    if (likedBy.contains(currentUserUid)) {
                        likedBy.remove(currentUserUid)
                    } else {
                        likedBy.add(currentUserUid)
                    }

                    likedByRef.setValue(likedBy)
                        .addOnSuccessListener {
                            Log.d(TAG, "Mi piace aggiornato con successo")
                        }
                        .addOnFailureListener { exception ->
                            Log.e(TAG, "Errore durante l'aggiornamento dei mi piace", exception)
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Listen failed.", error.toException())
            }
        })
    }

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val usernameTextView: TextView = itemView.findViewById(R.id.text_username)
        val descriptionTextView: TextView = itemView.findViewById(R.id.text_description)
        val postImageView: ImageView = itemView.findViewById(R.id.image_post)
        val likesCountTextView: TextView = itemView.findViewById(R.id.text_likes_count)
        val deleteIcon: ImageView = itemView.findViewById(R.id.image_delete)
        val likeIcon: ImageView = itemView.findViewById(R.id.image_like_icon)
    }
}