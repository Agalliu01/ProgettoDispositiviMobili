package it.insubria.esameconsegnadispositivimobili

import com.google.firebase.database.DatabaseReference
import java.io.Serializable


data class Post (
    val uid: String,
    val uidAccount:String,
    val username: String,
    val description: String,
    val imageUrl: String,
    val link: String,
    var likedBy: MutableList<String> = mutableListOf(), // Lista degli utenti che hanno messo "mi piace"
    val comments: MutableList<Comment> = mutableListOf(), // Lista dei commenti relativi al post
    val commentsUidLista:String
):Serializable {
    constructor() : this("","", "", "", "", "", mutableListOf(), mutableListOf(),"")
}
