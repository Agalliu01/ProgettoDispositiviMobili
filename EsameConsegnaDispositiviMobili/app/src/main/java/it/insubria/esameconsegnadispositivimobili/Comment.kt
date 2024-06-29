package it.insubria.esameconsegnadispositivimobili

import android.widget.ImageView

data class Comment(
    val imageProfile:String,
    val uid: String,
    val username: String,
    val text: String
) {
    constructor() : this("","", "", "")

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "imageProfile" to imageProfile,
            "uid" to uid,
            "username" to username,
            "text" to text
        )
    }
}
