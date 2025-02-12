package com.example.mobileattester.data.model

import com.google.gson.JsonObject
import org.json.JSONObject

data class Policy(
    val itemid: String,
    val name: String,
    val intent: String,
    val parameters: JsonObject,
    //val type: String,
    val description: String?,
)