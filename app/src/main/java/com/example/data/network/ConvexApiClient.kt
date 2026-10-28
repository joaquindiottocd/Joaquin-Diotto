package com.example.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ConvexApiClient(context: Context) {
    private val appContext = context.applicationContext
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val sharedPrefs = appContext.getSharedPreferences("cobranzas_cd_prefs", Context.MODE_PRIVATE)

    companion object {
        const val BASE_URL = "https://optimistic-bird-499.convex.cloud"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_COLLECTOR_NAME = "collector_name"
        private const val TAG = "ConvexApiClient"
    }

    var authToken: String?
        get() = sharedPrefs.getString(KEY_AUTH_TOKEN, null)
        set(value) {
            sharedPrefs.edit().putString(KEY_AUTH_TOKEN, value).apply()
        }

    fun isOnline(): Boolean {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun query(path: String, args: JSONObject = JSONObject()): Result<Any?> = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/api/query"
            val bodyJson = JSONObject().apply {
                put("path", path)
                put("args", JSONArray().put(args))
                put("format", "convex_encoded_json")
            }

            val requestBuilder = Request.Builder()
                .url(url)
                .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .addHeader("Convex-Client", "android-cobranzas-cd")

            authToken?.let {
                requestBuilder.addHeader("Authorization", "Bearer $it")
            }

            val response = client.newCall(requestBuilder.build()).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                Log.e(TAG, "Query error for $path: ${response.code} $responseBody")
                return@withContext Result.failure(Exception("HTTP ${response.code}: $responseBody"))
            }

            val json = JSONObject(responseBody)
            val status = json.optString("status")
            if (status == "success") {
                val value = json.opt("value")
                Result.success(value)
            } else {
                val errMsg = json.optString("errorMessage", "Error desconocido en el servidor")
                Result.failure(Exception(errMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during query $path", e)
            Result.failure(e)
        }
    }

    suspend fun mutation(path: String, args: JSONObject = JSONObject()): Result<Any?> = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/api/mutation"
            val bodyJson = JSONObject().apply {
                put("path", path)
                put("args", JSONArray().put(args))
                put("format", "convex_encoded_json")
            }

            val requestBuilder = Request.Builder()
                .url(url)
                .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .addHeader("Convex-Client", "android-cobranzas-cd")

            authToken?.let {
                requestBuilder.addHeader("Authorization", "Bearer $it")
            }

            val response = client.newCall(requestBuilder.build()).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                Log.e(TAG, "Mutation error for $path: ${response.code} $responseBody")
                return@withContext Result.failure(Exception("HTTP ${response.code}: $responseBody"))
            }

            val json = JSONObject(responseBody)
            val status = json.optString("status")
            if (status == "success") {
                val value = json.opt("value")
                Result.success(value)
            } else {
                val errMsg = json.optString("errorMessage", "Error desconocido en el servidor")
                Result.failure(Exception(errMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during mutation $path", e)
            Result.failure(e)
        }
    }

    suspend fun action(path: String, args: JSONObject = JSONObject()): Result<Any?> = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/api/action"
            val bodyJson = JSONObject().apply {
                put("path", path)
                put("args", JSONArray().put(args))
                put("format", "convex_encoded_json")
            }

            val requestBuilder = Request.Builder()
                .url(url)
                .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .addHeader("Convex-Client", "android-cobranzas-cd")

            authToken?.let {
                requestBuilder.addHeader("Authorization", "Bearer $it")
            }

            val response = client.newCall(requestBuilder.build()).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                Log.e(TAG, "Action error for $path: ${response.code} $responseBody")
                return@withContext Result.failure(Exception("HTTP ${response.code}: $responseBody"))
            }

            val json = JSONObject(responseBody)
            val status = json.optString("status")
            if (status == "success") {
                val value = json.opt("value")
                Result.success(value)
            } else {
                val errMsg = json.optString("errorMessage", "Error en la acción")
                Result.failure(Exception(errMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during action $path", e)
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val args = JSONObject().apply {
                put("provider", "password")
                put("params", JSONObject().apply {
                    put("flow", "signIn")
                    put("email", email)
                    put("password", password)
                })
            }

            val actionResult = action("auth:signIn", args)
            actionResult.fold(
                onSuccess = { res ->
                    if (res is JSONObject) {
                        val tokens = res.optJSONObject("tokens")
                        val token = tokens?.optString("token")
                        if (!token.isNullOrEmpty()) {
                            authToken = token
                            sharedPrefs.edit()
                                .putString(KEY_USER_EMAIL, email)
                                .apply()
                            Result.success(token)
                        } else {
                            Result.failure(Exception("Credenciales incorrectas o sesión no autorizada."))
                        }
                    } else {
                        Result.failure(Exception("Respuesta inesperada del servidor de autenticación."))
                    }
                },
                onFailure = { err ->
                    val msg = when {
                        err.message?.contains("InvalidSecret") == true || err.message?.contains("Invalid credentials") == true ->
                            "El mail o la contraseña no coinciden."
                        err.message?.contains("Invalid password") == true ->
                            "La contraseña debe tener al menos 8 caracteres."
                        else -> "No se pudo iniciar sesión en cobranzascd.freebuff.app. Revisá los datos."
                    }
                    Result.failure(Exception(msg))
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun saveUserSession(email: String, name: String, role: String, collectorName: String?) {
        sharedPrefs.edit()
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_USER_NAME, name)
            .putString(KEY_USER_ROLE, role)
            .putString(KEY_COLLECTOR_NAME, collectorName)
            .apply()
    }

    fun getStoredUserSession(): Map<String, String?> {
        return mapOf(
            "email" to sharedPrefs.getString(KEY_USER_EMAIL, null),
            "name" to sharedPrefs.getString(KEY_USER_NAME, null),
            "role" to sharedPrefs.getString(KEY_USER_ROLE, "collector"),
            "collectorName" to sharedPrefs.getString(KEY_COLLECTOR_NAME, null)
        )
    }

    fun signOut() {
        authToken = null
        sharedPrefs.edit()
            .remove(KEY_AUTH_TOKEN)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_ROLE)
            .remove(KEY_COLLECTOR_NAME)
            .apply()
    }
}
