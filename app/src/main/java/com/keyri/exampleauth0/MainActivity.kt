package com.keyri.exampleauth0

import android.content.Intent
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
import com.keyrico.keyrisdk.ui.auth.AuthWithScannerActivity
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
                getAuth0Profile(account, result.accessToken)
            }
        }

        WebAuthProvider.login(account)
            .withScheme(getString(R.string.com_auth0_scheme))
            .withScope("openid profile email")
            .start(this, callback)
    }

    private fun getAuth0Profile(account: Auth0, accessToken: String) {
        val profileCallback = object : Callback<UserProfile, AuthenticationException> {
            override fun onFailure(error: AuthenticationException) {
                onAuthenticationFailure(error)
            }

            override fun onSuccess(profile: UserProfile) {
                val email = profile.email
                val keyri = Keyri()

                val payload = JSONObject().apply {
                    put("token", accessToken)
                    put("provider", "auth0:email_password") // Optional
                    put("timestamp", System.currentTimeMillis()) // Optional
                    put("associationKey", keyri.getAssociationKey(email)) // Optional
                    put("userSignature", keyri.getUserSignature(email, email)) // Optional
                }.toString()

                // Public user ID (email) is optional
                keyriAuth(email, payload)
            }
        }

        AuthenticationAPIClient(account)
            .userInfo(accessToken)
            .start(profileCallback)
    }

    private fun keyriAuth(publicUserId: String?, payload: String) {
        val intent = Intent(this, AuthWithScannerActivity::class.java).apply {
            putExtra(AuthWithScannerActivity.APP_KEY, "IT7VrTQ0r4InzsvCNJpRCRpi1qzfgpaj")
            putExtra(AuthWithScannerActivity.PUBLIC_USER_ID, publicUserId)
            putExtra(AuthWithScannerActivity.PAYLOAD, payload)
        }

        easyKeyriAuthLauncher.launch(intent)
    }

    private fun onAuthenticationFailure(error: Throwable) {
        showText("Failed to authenticate, ${error.message}")
    }

    private fun showText(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
