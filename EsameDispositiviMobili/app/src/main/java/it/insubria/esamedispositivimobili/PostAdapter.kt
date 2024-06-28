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
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import it.insubria.esamedispositivimobili.Post
import it.insubria.esamedispositivimobili.R
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope

import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


class PostAdapter(private var postList: MutableList<Post>, private val isPersonalSection: Boolean) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    private val TAG = "PostAdapter"

    private val firestore = FirebaseFirestore.getInstance()
    private val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid

    init {
        listenForPostChanges()
    }

    private fun listenForPostChanges() {
        firestore.collection("posts")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }

                for (dc in snapshots!!.documentChanges) {
                    when (dc.type) {
                        DocumentChange.Type.ADDED -> {
                            val post = dc.document.toObject(Post::class.java)
                            postList.add(post)
                            notifyItemInserted(postList.size - 1)
                        }
                        DocumentChange.Type.MODIFIED -> {
                            val post = dc.document.toObject(Post::class.java)
                            val index = postList.indexOfFirst { it.uid == post.uid }
                            if (index != -1) {
                                postList[index] = post
                                notifyItemChanged(index)
                            }
                        }
                        DocumentChange.Type.REMOVED -> {
                            val post = dc.document.toObject(Post::class.java)
                            val index = postList.indexOfFirst { it.uid == post.uid }
                            if (index != -1) {
                                postList.removeAt(index)
                                notifyItemRemoved(index)
                            }
                        }
                    }
                }
            }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_post_adapter, parent, false)
        return PostViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val currentPost = postList[position]
        val likesCount = currentPost.likedBy.size

        holder.usernameTextView.text = currentPost.username
        holder.descriptionTextView.text = currentPost.description
        holder.likesCountTextView.text = "Mi piace: $likesCount"

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
            toggleLike(currentPost, holder.likesCountTextView, holder.itemView.context)
        }
    }

    override fun getItemCount(): Int {
        return postList.size
    }

    // Metodo per aggiornare i dati del RecyclerView
    fun setData(newPostList: List<Post>) {
        postList = newPostList.toMutableList()
        notifyDataSetChanged()
    }

    // Elimina il post da Firebase e localmente
    private fun deletePost(context: Context, post: Post) {
        val postRef = firestore.collection("posts").document(post.uid)

        firestore.runTransaction { transaction ->
            transaction.delete(postRef)

            // Rimuovi il post dalla lista locale
            val newList = postList.toMutableList()
            newList.remove(post)
            postList = newList
        }.addOnSuccessListener {
            notifyDataSetChanged() // Aggiorna la RecyclerView
            Toast.makeText(context, "Post eliminato con successo", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Errore durante l'eliminazione del post", exception)
            Toast.makeText(context, "Errore durante l'eliminazione del post", Toast.LENGTH_SHORT).show()
        }
    }

    // Aggiunge o rimuove un "mi piace" dal post
    private fun toggleLike(post: Post, likesCountTextView: TextView, context: Context) {
        if (currentUserUid != null) {
            val postRef = firestore.collection("posts").document(post.uid)

            GlobalScope.launch(Dispatchers.IO) {
                try {
                    firestore.runTransaction { transaction ->
                        val snapshot = transaction.get(postRef)

                        if (snapshot.exists()) {
                            val currentLikesCount = snapshot.getLong("likesCount") ?: 0
                            val likedBy = snapshot.get("likedBy") as? MutableList<String> ?: mutableListOf()

                            if (currentUserUid in likedBy) {
                                likedBy.remove(currentUserUid)
                                transaction.update(postRef, "likesCount", currentLikesCount - 1)
                            } else {
                                likedBy.add(currentUserUid)
                                transaction.update(postRef, "likesCount", currentLikesCount + 1)
                            }

                            transaction.update(postRef, "likedBy", likedBy)
                            currentLikesCount + if (currentUserUid in likedBy) 1 else -1
                        } else {
                            throw IllegalStateException("Il documento non esiste più")
                        }
                    }.addOnSuccessListener { updatedLikesCount ->
                        launch(Dispatchers.Main) {
                            likesCountTextView.text = "Mi piace: $updatedLikesCount"
                        }
                    }.addOnFailureListener { exception ->
                        Log.e(TAG, "Errore durante l'aggiornamento dei mi piace", exception)
                        launch(Dispatchers.Main) {
                            Toast.makeText(context, "Errore durante l'aggiornamento dei mi piace", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "Errore durante l'aggiornamento dei mi piace: ${e.message}")
                    launch(Dispatchers.Main) {
                        Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Eccezione durante l'aggiornamento dei mi piace", e)
                    launch(Dispatchers.Main) {
                        Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
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