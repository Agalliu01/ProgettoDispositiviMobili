package it.insubria.esameconsegnadispositivimobili

import com.google.firebase.database.DatabaseReference


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
) {
    constructor() : this("","", "", "", "", "", mutableListOf(), mutableListOf(),"")


    fun toMap(): Map<String, Any?> {
        return mapOf(
            "uid" to uid,
            "uidAcocunt" to uidAccount,
            "username" to username,
            "description" to description,
            "imageUrl" to imageUrl,
            "link" to link,
            "likedBy" to likedBy,
            "comments" to comments.map { it.toMap() }, // Converti ogni commento in mappa
            "commentsUidLista" to commentsUidLista
        )
    }
}
