package it.insubria.esamedispositivimobili


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
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

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

        // Initialize RecyclerView
        recyclerView = view.findViewById(R.id.recyclerViewHome)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Initialize buttons
        btnAllPosts = view.findViewById(R.id.btn_tutti)
        btnFollowedPosts = view.findViewById(R.id.btn_seguiti)
        btnMyPosts = view.findViewById(R.id.btn_personali)
        val listVuota = mutableListOf<Post>()

        // Initialize RecyclerView Adapter
        postAdapter = PostAdapter(listVuota, false) // false because not in "My Posts" section
        recyclerView.adapter = postAdapter

        // Button click handlers
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

        // Initially load all posts
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
                // Handle error
                Toast.makeText(requireContext(), "Errore nel caricamento dei post", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadFollowedPosts() {
        val currentUserId = currentUser?.uid ?: return

        firebaseFirestore.collection("users")
            .document(currentUserId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val followedUsers = document.get("followedUsers") as? List<String>

                    if (followedUsers != null && followedUsers.isNotEmpty()) {
                        val postsPromises = mutableListOf<Task<QuerySnapshot>>()
                        followedUsers.forEach { username ->
                            val promise = firebaseFirestore.collection("posts")
                                .whereEqualTo("username", username)
                                .get()
                            postsPromises.add(promise)
                        }

                        Tasks.whenAllSuccess<QuerySnapshot>(postsPromises)
                            .addOnSuccessListener { snapshots ->
                                val posts = mutableListOf<Post>()
                                snapshots.forEach { snapshot ->
                                    snapshot.documents.forEach { document ->
                                        val post = document.toObject(Post::class.java)
                                        if (post != null) {
                                            posts.add(post)
                                        }
                                    }
                                }
                                requireActivity().runOnUiThread {
                                    postAdapter.setData(posts)
                                }
                            }
                            .addOnFailureListener { exception ->
                                // Handle error
                                Toast.makeText(requireContext(), "Errore nel caricamento dei post seguiti", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        // No followed users, handle accordingly
                        requireActivity().runOnUiThread {
                            postAdapter.setData(emptyList())
                        }
                    }
                } else {
                    // User document not found, handle accordingly
                    requireActivity().runOnUiThread {
                        postAdapter.setData(emptyList())
                    }
                }
            }
            .addOnFailureListener { exception ->
                // Handle error
                Toast.makeText(requireContext(), "Errore nel caricamento dei dati utente", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadMyPosts() {
        val currentUserId = currentUser?.uid ?: ""
        firebaseFirestore.collection("posts")
            .whereEqualTo("uid", currentUserId)
            .get()
            .addOnSuccessListener { documents ->
                val posts = mutableListOf<Post>()
                for (document in documents) {
                    val post = document.toObject(Post::class.java)
                    posts.add(post)
                }
                postAdapter = PostAdapter(posts, true) // true because in "My Posts" section
                recyclerView.adapter = postAdapter
            }
            .addOnFailureListener { exception ->
                // Handle error
                Toast.makeText(requireContext(), "Errore nel caricamento dei tuoi post", Toast.LENGTH_SHORT).show()
            }
    }
}