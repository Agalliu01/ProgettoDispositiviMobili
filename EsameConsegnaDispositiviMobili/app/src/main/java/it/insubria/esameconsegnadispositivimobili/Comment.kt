package it.insubria.esameconsegnadispositivimobili

data class Comment(
    val uid: String,
    val username: String,
    val text: String
) {
    constructor() : this("", "", "")

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "uid" to uid,
            "username" to username,
            "text" to text
        )
    }
}
