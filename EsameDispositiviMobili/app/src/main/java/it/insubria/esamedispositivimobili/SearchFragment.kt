package it.insubria.esamedispositivimobili

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SearchFragment : Fragment(), UserAdapter.OnItemClickListener {
    private lateinit var searchEditText: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var userAdapter: UserAdapter
    private val userList = mutableListOf<UserDetails>()
    private var filteredUserList = listOf<UserDetails>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)
        searchEditText = view.findViewById(R.id.searchEditText)
        recyclerView = view.findViewById(R.id.recyclerView)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inizializza l'adapter con una lista vuota e l'interfaccia per la gestione delle selezioni
        userAdapter = UserAdapter(requireContext(), filteredUserList, this)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = userAdapter

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterUsers(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        loadUsersFromFirebase()
    }

    private fun loadUsersFromFirebase() {
        val db = FirebaseFirestore.getInstance()
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid

        db.collection("users")
            .get()
            .addOnSuccessListener { documents ->
                userList.clear()
                for (document in documents) {
                    val user = document.toObject(UserDetails::class.java)
                    // Escludi l'utente corrente
                    if (user.username != currentUserUid) {
                        userList.add(user)
                    }
                }
                filteredUserList = userList
                userAdapter.updateUsers(filteredUserList)
            }
            .addOnFailureListener { exception ->
                // Gestisci l'errore
            }
    }

    private fun filterUsers(query: String) {
        filteredUserList = if (query.isEmpty()) {
            userList
        } else {
            userList.filter { it.username.contains(query, ignoreCase = true) }
        }
        userAdapter.updateUsers(filteredUserList)
    }

    // Implementazione del listener per il click sul pulsante di follow nell'adapter
    override fun onItemClick(user: UserDetails, followButton: Button) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val db = FirebaseFirestore.getInstance()

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
                                // Aggiornamento riuscito
                            }
                            .addOnFailureListener { e ->
                                // Gestione dell'errore nell'aggiornamento
                            }
                    }
                }
                .addOnFailureListener { exception ->
                    // Gestione dell'errore nel caricamento dei dati dell'utente corrente
                }
        }
    }
}
