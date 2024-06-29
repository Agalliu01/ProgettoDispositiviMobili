package it.insubria.esameconsegnadispositivimobili


data class Post (
val uid: String,
val username: String,
val description: String,
val imageUrl: String,
val link: String,
var likedBy: MutableList<String> = mutableListOf(), // Lista degli utenti che hanno messo "mi piace"
val comments: MutableList<Comment> = mutableListOf() // Lista dei commenti relativi al post
) {
    constructor() : this("", "", "", "", "", mutableListOf(), mutableListOf())

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "uid" to uid,
            "username" to username,
            "description" to description,
            "imageUrl" to imageUrl,
            "link" to link,
            "likedBy" to likedBy,
            "comments" to comments.map { it.toMap() } // Converti ogni commento in mappa
        )
    }
}
