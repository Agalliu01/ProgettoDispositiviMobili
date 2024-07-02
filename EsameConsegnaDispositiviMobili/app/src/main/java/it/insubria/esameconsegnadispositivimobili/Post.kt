package it.insubria.esameconsegnadispositivimobili

import java.io.Serializable


data class Post(
    val uid: String,
    val uidAccount: String,
    val username: String,
    val description: String,
    val imageUrl: String,
    val link: String,
    var likedBy: MutableList<String>? = null, // Lista di utenti che hanno messo "mi piace" inizializzata a null
    val comments: MutableList<Comment>? = null, // Lista dei commenti inizializzata a null
    val commentsUidLista: String,
) : Serializable {
    constructor() : this("", "", "", "", "", "", null, null, "")
}
