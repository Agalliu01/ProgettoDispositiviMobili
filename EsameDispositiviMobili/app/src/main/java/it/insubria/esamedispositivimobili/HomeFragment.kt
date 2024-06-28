package it.insubria.esamedispositivimobili

import Post
import PostAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

import android.widget.Button
import android.widget.Toast

import androidx.recyclerview.widget.LinearLayoutManager

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private lateinit var btnAllPosts: Button
    private lateinit var btnFollowedPosts: Button
    private lateinit var btnMyPosts: Button

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firebaseFirestore = FirebaseFirestore.getInstance()
    private val currentUser = firebaseAuth.currentUser

    private var currentSection = Section.ALL_POSTS

    enum class Section {
        ALL_POSTS,
        FOLLOWED_POSTS,
        MY_POSTS
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Inizializza RecyclerView
        recyclerView = view.findViewById(R.id.recyclerViewHome)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Inizializza bottoni
        btnAllPosts = view.findViewById(R.id.btn_tutti)
        btnFollowedPosts = view.findViewById(R.id.btn_seguiti)
        btnMyPosts = view.findViewById(R.id.btn_personali)

        // Inizializza adapter per RecyclerView
        postAdapter = PostAdapter(emptyList(), false) // false perché non è nella sezione "Personali"
        recyclerView.adapter = postAdapter

        // Gestione dei click sui bottoni
        btnAllPosts.setOnClickListener {
            currentSection = Section.ALL_POSTS
            loadPosts()
        }

        btnFollowedPosts.setOnClickListener {
            currentSection = Section.FOLLOWED_POSTS
            loadPosts()
        }

        btnMyPosts.setOnClickListener {
            currentSection = Section.MY_POSTS
            loadPosts()
        }

        // Inizialmente carica tutti i post degli utenti
        loadPosts()

        return view
    }

    private fun loadPosts() {
        when (currentSection) {
            Section.ALL_POSTS -> loadAllPosts()
            Section.FOLLOWED_POSTS -> loadFollowedPosts()
            Section.MY_POSTS -> loadMyPosts()
        }
    }

    private fun loadAllPosts() {
        firebaseFirestore.collection("posts")
            .get()
            .addOnSuccessListener { documents ->
                val posts = mutableListOf<Post>()
                for (document in documents) {
                    val post = document.toObject(Post::class.java)
                    posts.add(post)
                }
                postAdapter.setData(posts)
            }
            .addOnFailureListener { exception ->
                // Gestire l'errore
            }
    }

    private fun loadFollowedPosts() {
        // Implementa la logica per caricare i post degli utenti seguiti
        // Supponiamo che ci sia una lista di utenti seguiti
        val followedUsers = listOf("user1", "user2") // Esempio

        firebaseFirestore.collection("posts")
            .whereIn("username", followedUsers)
            .get()
            .addOnSuccessListener { documents ->
                val posts = mutableListOf<Post>()
                for (document in documents) {
                    val post = document.toObject(Post::class.java)
                    posts.add(post)
                }
                postAdapter.setData(posts)
            }
            .addOnFailureListener { exception ->
                // Gestire l'errore
            }
    }

    private fun loadMyPosts() {
        val currentUserId = currentUser?.uid ?: return

        firebaseFirestore.collection("posts")
            .whereEqualTo("uid", currentUserId)
            .get()
            .addOnSuccessListener { documents ->
                val posts = mutableListOf<Post>()
                for (document in documents) {
                    val post = document.toObject(Post::class.java)
                    posts.add(post)
                }
                postAdapter = PostAdapter(posts, true) // true perché è nella sezione "Personali"
                recyclerView.adapter = postAdapter
            }
            .addOnFailureListener { exception ->
                // Gestire l'errore
            }
    }
}