package com.keyri.exampleauth0

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationAPIClient
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.callback.Callback
import com.auth0.android.provider.WebAuthProvider
import com.auth0.android.result.Credentials
import com.auth0.android.result.UserProfile
import com.keyri.exampleauth0.databinding.ActivityMainBinding
import com.keyrico.keyrisdk.Keyri
import com.keyrico.scanner.easyKeyriAuth
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val easyKeyriAuthLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val text = if (it.resultCode == RESULT_OK) "Authenticated" else "Failed to authenticate"

            showText(text)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        binding.bAuth0.setOnClickListener {
            authWithAuth0()
        }
    }

    private fun authWithAuth0() {
        val account =
            Auth0(getString(R.string.com_auth0_client_id), getString(R.string.com_auth0_domain))

        val callback = object : Callback<Credentials, AuthenticationException> {
            override fun onFailure(error: AuthenticationException) {
                onAuthenticationFailure(error)
            }

            override fun onSuccess(result: Credentials) {
                getAuth0Profile(account, result)
            }
        }

        WebAuthProvider.login(account)
            .withScheme(getString(R.string.com_auth0_scheme))
            .withScope("openid profile email")
            .start(this, callback)
    }

    private fun getAuth0Profile(account: Auth0, credentials: Credentials) {
        val profileCallback = object : Callback<UserProfile, AuthenticationException> {
            override fun onFailure(error: AuthenticationException) {
                onAuthenticationFailure(error)
            }

            override fun onSuccess(result: UserProfile) {
                val email = result.email
                val keyri = Keyri()

                val tokenData = JSONObject().apply {
                    put("accessToken", credentials.accessToken)
                    put("idToken", credentials.idToken)
                    put("refreshToken", credentials.refreshToken)
                    put("expiresAt", credentials.expiresAt)
                    put("recoveryCode", credentials.recoveryCode)
                    put("scope", credentials.scope)
                    put("type", credentials.type)
                }

                val userProfileData = JSONObject().apply {
                    put("email", result.email)
                    put("createdAt", result.createdAt?.time)
                    put("name", result.name)
                    put("familyName", result.familyName)
                    put("givenName", result.givenName)
                    put("isEmailVerified", result.isEmailVerified)
                    put("nickname", result.nickname)
                    put("pictureURL", result.pictureURL)
                    put("id", result.getId())
                }

                val data = JSONObject().apply {
                    put("token", tokenData)
                    put("userProfile", userProfileData)
                }

                val signingData = JSONObject().apply {
                    put("timestamp", System.currentTimeMillis())
                    put("email", email)
                    put("uid", result.getId())
                }.toString()

                val signature = email?.let {
                    keyri.generateUserSignature(it, signingData)
                } ?: keyri.generateUserSignature(data = signingData)

                val associationKey = email?.let {
                    keyri.getAssociationKey(it)
                } ?: keyri.getAssociationKey()

                val payload = JSONObject().apply {
                    put("data", data)
                    put("signingData", signingData)
                    put("userSignature", signature) // Optional
                    put("associationKey", associationKey) // Optional
                }.toString()

                // Public user ID (email) is optional
                keyriAuth(email, payload)
            }
        }

        AuthenticationAPIClient(account)
            .userInfo(credentials.accessToken)
            .start(profileCallback)
    }

    private fun keyriAuth(publicUserId: String?, payload: String) {
        easyKeyriAuth(
            this,
            easyKeyriAuthLauncher,
            "SQzJ5JLT4sEE1zWk1EJE1ZGNfwpvnaMP",
            payload,
            publicUserId
        )
    }

    private fun onAuthenticationFailure(error: Throwable) {
        copyMessageToClipboard(error.message.toString())
        showText("Failed to authenticate, ${error.message}")
    }

    private fun showText(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun copyMessageToClipboard(message: String) {
        val clipboard: ClipboardManager =
            getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        val clip = ClipData.newPlainText("Keyri Auth0 example", message)

        clipboard.setPrimaryClip(clip)
    }
}
