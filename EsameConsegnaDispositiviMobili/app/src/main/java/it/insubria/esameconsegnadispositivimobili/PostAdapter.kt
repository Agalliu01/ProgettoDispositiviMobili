package it.insubria.esameconsegnadispositivimobili

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
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
    private val auth = FirebaseAuth.getInstance()
    private lateinit var currentUser: FirebaseUser
    private var currentUserProfileImageUrl: String? = null
    private var isProfileImageLoaded = false

    init {
        currentUser = auth.currentUser!!
        loadCurrentUserProfileImage()
        listenForPostChanges()
    }

    private fun loadCurrentUserProfileImage() {
        val uid = currentUser.uid
        val userRef = database.getReference("users").child(uid)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    currentUserProfileImageUrl = snapshot.child("profileImageUrl").getValue(String::class.java)
                    isProfileImageLoaded = true
                    notifyDataSetChanged()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Failed to load current user's profile image.", error.toException())
            }
        })
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
        val currentUserUid = currentUser.uid
        if (currentUserUid != null && currentPost.likedBy.contains(currentUserUid)) {
            holder.likeIcon.setImageResource(R.drawable.ic_launcher_background)
        } else {
            holder.likeIcon.setImageResource(R.drawable.ic_launcher_background)
        }

        // Gestione del click sull'icona per pubblicare il commento
        holder.publishCommentIcon.setOnClickListener {
            val commentText = holder.commentEditText.text.toString().trim()

            if (commentText.isNotEmpty()) {
                publishComment(holder.itemView.context, currentPost, commentText)
                holder.commentEditText.setText("") // Pulizia della EditText dopo l'inserimento
            } else {
                Toast.makeText(holder.itemView.context, "Inserisci un commento", Toast.LENGTH_SHORT).show()
            }
        }

        // Caricamento dell'immagine del profilo dell'utente corrente se disponibile
        if (isProfileImageLoaded) {
            currentUserProfileImageUrl?.let {
                Glide.with(holder.imageProfileImageView.context)
                    .load(it)
                    .into(holder.imageProfileImageView)
            }
        } else {
            // Immagine di placeholder o gestione alternativa
            holder.imageProfileImageView.setImageResource(R.drawable.ic_launcher_background)
        }
    }

    override fun getItemCount(): Int {
        return postList.size
    }

    // Metodo per eliminare il post dal Realtime Database, da "users/listaPost" e dallo Storage Firebase
    private fun deletePost(context: Context, post: Post) {
        val postId = post.uid
        val postsRef = database.getReference("posts").child(postId)
        val userRef = database.getReference("users").child(currentUser.uid).child("listaPost").child(postId)

        postsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val imageUrl = snapshot.child("imageUrl").getValue(String::class.java)

                // Elimina l'immagine dallo Storage Firebase (se necessario)
                imageUrl?.let {
                    val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(it)
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

    // Aggiunge un commento al post nel Realtime Database
    private fun publishComment(context: Context, post: Post, commentText: String) {
        val postId = post.uid
        val commentsRef = database.getReference("comments").child(postId).push()

        // Creazione dell'oggetto commento con i dati necessari
        val comment = Comment(
            imageProfile = currentUserProfileImageUrl ?: "",
            uid = currentUser.uid,
            username = currentUser.displayName ?: "",
            text = commentText
        )

        // Salva il commento nella sezione "comments"
        commentsRef.setValue(comment.toMap())
            .addOnSuccessListener {
                Log.d(TAG, "Commento pubblicato con successo")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Errore durante la pubblicazione del commento", exception)
                Toast.makeText(context, "Errore durante la pubblicazione del commento", Toast.LENGTH_SHORT).show()
            }

        // Aggiorna la lista dei commenti associati al post nella sezione "posts"
        val postRef = database.getReference("posts").child(postId)
        val commentsListRef = postRef.child("comments")

        // Ottieni un nuovo ID per il commento all'interno del post
        val newCommentId = commentsListRef.push().key ?: ""

        // Aggiungi l'ID del commento alla lista dei commenti nel post
        commentsListRef.child(newCommentId).setValue(comment)
            .addOnSuccessListener {
                Log.d(TAG, "Commento aggiunto alla lista dei commenti del post")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Errore durante l'aggiunta del commento alla lista dei commenti del post", exception)
            }
    }


    // Metodo per aggiornare i "mi piace" al post nel Realtime Database
    private fun toggleLike(post: Post) {
        val postsRef = database.getReference("posts").child(post.uid)
        val likedByRef = postsRef.child("likedBy")

        likedByRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val likedBy = snapshot.getValue(MutableList::class.java)?.filterIsInstance<String>()?.toMutableList()
                    ?: mutableListOf()

                if (currentUser.uid != null) {
                    if (likedBy.contains(currentUser.uid)) {
                        likedBy.remove(currentUser.uid)
                    } else {
                        likedBy.add(currentUser.uid)
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
        val descriptionTextView: TextView = itemView.findViewById(R.id.text_post_description)
        val postImageView: ImageView = itemView.findViewById(R.id.image_post)
        val likesCountTextView: TextView = itemView.findViewById(R.id.text_likes_count)
        val deleteIcon: ImageView = itemView.findViewById(R.id.image_delete)
        val likeIcon: ImageView = itemView.findViewById(R.id.image_like_icon)
        val commentEditText: EditText = itemView.findViewById(R.id.edit_comment)
        val publishCommentIcon: ImageView = itemView.findViewById(R.id.image)
        val imageProfileImageView: ImageView = itemView.findViewById(R.id.image_profile)
    }
}