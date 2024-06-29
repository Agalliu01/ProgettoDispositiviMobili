package it.insubria.esameconsegnadispositivimobili

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*

class SearchFragment : Fragment(), AccountAdapter.OnItemClickListener {
    private lateinit var searchEditText: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var userAdapter: AccountAdapter
    private val userList = mutableListOf<User>()
    private var filteredUserList = listOf<User>()

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
        userAdapter = AccountAdapter(requireContext(), filteredUserList, this)
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
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        val   database = FirebaseDatabase.getInstance().reference
        database.child("users").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userList.clear()
                for (postSnapshot in snapshot.children) {
                    val user = postSnapshot.getValue(User::class.java)
                    if (user != null && user.uid != currentUserUid) {
                        userList.add(user)
                    }
                }
                userAdapter.notifyDataSetChanged() // Update RecyclerView
            filteredUserList = userList
            userAdapter.updateUsers(filteredUserList)
        }

            override fun onCancelled(error: DatabaseError) {
                // Gestisci l'errore
                Log.e("RealtimeDatabase", "Error getting data", error.toException())
            }
        })
    }

    private fun filterUsers(query: String) {
        filteredUserList = if (query.isEmpty()) {
            userList
        } else {
            userList.filter { it.Username.contains(query, ignoreCase = true) }
        }
        userAdapter.updateUsers(filteredUserList)
    }

    // Implementazione del listener per il click sul pulsante di follow nell'adapter
    override fun onItemClick(user: User, followButton: Button) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val db = Firebase.database.reference

            db.child("users").child(userId).get().addOnSuccessListener { dataSnapshot ->
                val followedUsers = dataSnapshot.child("followedUsers").getValue(object : GenericTypeIndicator<ArrayList<String>>() {})

                if (followedUsers != null) {
                    val usersList = followedUsers.toMutableList()

                    if (usersList.contains(user.Username)) {
                        // Utente già seguito, quindi rimuovilo
                        usersList.remove(user.Username)
                        followButton.text = "Segui"
                    } else {
                        // Utente non seguito, quindi aggiungilo
                        usersList.add(user.Username)
                        followButton.text = "Non seguire più"
                    }

                    // Aggiorna nel database
                    db.child("users").child(userId).child("followedUsers").setValue(usersList)
                        .addOnSuccessListener {
                            // Aggiornamento riuscito
                        }
                        .addOnFailureListener { e ->
                            // Gestione dell'errore nell'aggiornamento
                            Log.e("RealtimeDatabaseUpdate", "Error updating followedUsers", e)
                        }
                } else {
                    // Gestisci il caso in cui followedUsers sia null
                    Log.e("RealtimeDatabaseUpdate", "followedUsers is null")
                }
            }.addOnFailureListener { exception ->
                // Gestione dell'errore nel caricamento dei dati dell'utente corrente
                Log.e("RealtimeDatabaseUpdate", "Error fetching data", exception)
            }
        }
    }
}