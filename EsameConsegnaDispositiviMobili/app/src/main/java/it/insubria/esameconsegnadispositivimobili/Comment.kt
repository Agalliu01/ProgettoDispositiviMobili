package it.insubria.esameconsegnadispositivimobili

import android.widget.ImageView
import java.io.Serializable

data class Comment(
    val imageProfile:String,
    val uid: String,
    val username: String,
    val text: String
) :Serializable{
    constructor() : this("","", "", "")


}
