package it.insubria.esameconsegnadispositivimobili

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_post_adapter, parent, false)
        return PostViewHolder(itemView)
    }

    private fun loadCurrentUserProfileImage() {
        val uid = currentUser.uid
        val userRef = database.getReference("users").child(uid).child("profileImageUrl")

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                currentUserProfileImageUrl = snapshot.value as? String
                isProfileImageLoaded = true
                notifyDataSetChanged()
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
                    try {
                        val post = postSnapshot.getValue(Post::class.java)
                        post?.let {
                            postList.add(it)
                        }
                    } catch (e: DatabaseException) {
                        Log.e(TAG, "Error deserializing post: ${e.message}", e)
                    }
                }
                notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Listen failed.", error.toException())
            }
        })
    }



    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val currentPost = postList[position]
        holder.usernameTextView.text ="Post di: "+currentPost.username

        val des=currentPost.description
        if(des.length>1)
            holder.descriptionTextView.text = "Descrizione: \n"+currentPost.description
        else
            holder.descriptionTextView.text="Nessuna descrizione"
        holder.likesCountTextView.text = "Mi piace: ${currentPost.likedBy?.size ?: 0}"

        // Caricamento dell'immagine del post
        Glide.with(holder.postImageView.context)
            .load(currentPost.imageUrl)
            .into(holder.postImageView)

        // Mostra/nascondi l'icona di eliminazione del post se è nella sezione "Personali"
        holder.deleteIcon.visibility = if (isPersonalSection) View.VISIBLE else View.GONE

        // Gestione del click sull'icona per eliminare il post
        holder.deleteIcon.setOnClickListener {
            showDeleteConfirmationDialog( holder.itemView.context, currentPost)
        // deletePost(holder.itemView.context, currentPost)
        }

        // Gestione del click sull'icona dei "mi piace"
        holder.likeIcon.setOnClickListener {
            toggleLike(currentPost)
        }
        holder.link.setOnClickListener {
            openWebsite(holder.itemView.context, currentPost)
        }


        holder.imageProfileImageView.setImageResource(R.drawable.icons8_busto_in_sagoma_48);


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
        }

        // Configurazione del RecyclerView dei commenti
        setupCommentsRecyclerView(holder.recyclerViewComments, currentPost)
    }

    private fun showDeleteConfirmationDialog(context: Context, post: Post) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Delete post")
        builder.setMessage("Are you sure you want to delete this post?")

        builder.setPositiveButton("Yes") { dialog, which ->
            deletePost(context, post)
            Toast.makeText(context, "Post deleted", Toast.LENGTH_SHORT).show()
        }

        builder.setNegativeButton("No") { dialog, which ->
            dialog.dismiss()
        }

        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    override fun getItemCount(): Int {
        return postList.size
    }

    fun deletePost(context: Context, post: Post) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            if (post.uidAccount == user.uid) {
                val postsRef = FirebaseDatabase.getInstance().getReference("posts").child(post.uid)
                postsRef.removeValue().addOnSuccessListener {
                    // Rimozione dell'immagine dal Firebase Storage
                    val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(post.imageUrl)
                    storageRef.delete().addOnSuccessListener {
                        Toast.makeText(context, "Post eliminato con successo", Toast.LENGTH_SHORT).show()
                    }.addOnFailureListener {
                        Toast.makeText(context, "Errore nell'eliminazione dell'immagine", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener {
                    Toast.makeText(context, "Errore nell'eliminazione del post", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Non hai i permessi per eliminare questo post", Toast.LENGTH_SHORT).show()
            }
        }
    }


    fun toggleLike(post: Post) {
        val postsRef = database.getReference("posts").child(post.uid).child("likedBy")

        if (post.likedBy == null) {
            post.likedBy = mutableListOf()
        }

        val currentUserUid = currentUser.uid
        if (post.likedBy?.contains(currentUserUid) == true) {
            post.likedBy?.remove(currentUserUid)
        } else {
            post.likedBy?.add(currentUserUid)
        }

        postsRef.setValue(post.likedBy).addOnSuccessListener {
            notifyDataSetChanged()
        }.addOnFailureListener {
            Log.e(TAG, "Error updating likes for post")
        }
    }

    fun publishComment(context: Context, post: Post, commentText: String) {
        val uid = currentUser.uid
        var username = currentUser.displayName ?: ""

        // Ottieni il username dall'utente nel database
        val userRef = database.getReference("users").child(uid).child("username")
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                username = snapshot.value as? String ?: ""

                val imageProfile = currentUserProfileImageUrl ?: ""

                val newComment = Comment(imageProfile, uid, username, commentText)

                // Aggiungi il commento alla lista dei commenti del post
                val updatedComments = post.comments?.toMutableList() ?: mutableListOf()
                updatedComments.add(newComment)

                // Aggiorna i commenti nel database
                val postsRef = database.getReference("posts").child(post.uid).child("comments")
                postsRef.setValue(updatedComments).addOnSuccessListener {
                    // Aggiornamento riuscito
                    notifyDataSetChanged()
                }.addOnFailureListener {
                    // Gestione dell'errore nell'aggiornamento
                    Log.e(TAG, "Error updating comments for post")
                    Toast.makeText(context, "Errore nell'aggiunta del commento", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Failed to load username.", error.toException())
                Toast.makeText(context, "Errore nel caricamento del nome utente", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupCommentsRecyclerView(recyclerView: RecyclerView, post: Post) {
        val commentsAdapter = post.comments?.let { CommentAdapter(recyclerView.context, it) }
        recyclerView.layoutManager = LinearLayoutManager(recyclerView.context)
        recyclerView.adapter = commentsAdapter
    }
    private fun openWebsite(context: Context, post: Post) {
        val url = post.link

        if (url.isNotEmpty() && URLUtil.isValidUrl(url)) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            // Usa Intent.createChooser per permettere all'utente di scegliere l'applicazione
            val chooser = Intent.createChooser(intent, "Apri con")

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(chooser)
            } else {
                Toast.makeText(context, "Nessuna app disponibile per aprire il link", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "URL non valido", Toast.LENGTH_SHORT).show()
        }
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
        val recyclerViewComments: RecyclerView = itemView.findViewById(R.id.recycle_comment)
        val link:TextView=itemView.findViewById(R.id.text_link)
    }

}
