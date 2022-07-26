# Overview

This module contains example of implementation [Keyri](https://keyri.com) with Auth0.

## Contents

* [Requirements](#Requirements)
* [Permissions](#Permissions)
* [Keyri Integration](#Keyri-Integration)
* [Auth0 Integration](#Auth0-Integration)
* [Authentication](#Authentication)

## Requirements

* Android API level 23 or higher
* AndroidX compatibility
* Kotlin coroutines compatibility

Note: Your app does not have to be written in kotlin to integrate this SDK, but must be able to
depend on kotlin functionality.

## Permissions

Open your app's `AndroidManifest.xml` file and add the following permission:

```xml

<uses-permission android:name="android.permission.INTERNET" />
```

## Keyri Integration

* Add the JitPack repository to your root build.gradle file:

```groovy
allprojects {
    repositories {
        // ...
        maven { url "https://jitpack.io" }
    }
}
```

* Add SDK dependency to your build.gradle file and sync project:

```kotlin
dependencies {
    // ...
  implementation("com.github.Keyri-Co.keyri-android-whitelabel-sdk:keyrisdk:$latestKeyriVersion")
  implementation("com.github.Keyri-Co.keyri-android-whitelabel-sdk:scanner:$latestKeyriVersion")
}
```

## Auth0 Integration

* Check Auth0.Android [Installation](https://github.com/auth0/Auth0.Android#installation) section to
  integrate Auth0 SDK into your app.

* Add client ID, domain and scheme to the `strings.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!--  ...-->

    <string name="com_auth0_client_id">YOUR_CLIENT_ID</string>
    <string name="com_auth0_domain">YOUR_DOMAIN</string>
    <string name="com_auth0_scheme">YOUR_SCHEME</string>
</resources>
```

## Authentication

Declare Auth0 callback and login with WebAuthProvider:

```kotlin
val callback = object : Callback<Credentials, AuthenticationException> {
    override fun onFailure(error: AuthenticationException) {
        // Process error
    }

    override fun onSuccess(result: Credentials) {
        val accessToken = result.accessToken

        // See Keyri authorization section
    }
}

WebAuthProvider.login(account)
    .withScheme(getString(R.string.com_auth0_scheme))
    .withScope("openid profile email")
    .start(this, callback)
```

Optional. If you want to provide payload with signature of public user ID, add next block:

```kotlin
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

        val signature = keyri.getUserSignature(email, signingData)

        val payload = JSONObject().apply {
            put("data", data)
            put("signingData", signingData)
            put("userSignature", signature) // Optional
            put("associationKey", keyri.getAssociationKey(email)) // Optional
        }.toString()

        // Public user ID (email) is optional
        keyriAuth(email, payload)
    }
}

AuthenticationAPIClient(account)
    .userInfo(credentials.accessToken)
    .start(profileCallback)
```

Authenticate with Keyri. In the next showing `AuthWithScannerActivity` with providing
`publicUserId` and `payload`.

```kotlin
private val easyKeyriAuthLauncher =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // Process authentication result
    }

private fun keyriAuth(publicUserId: String?, payload: String) {
    val intent = Intent(this, AuthWithScannerActivity::class.java).apply {
        putExtra(AuthWithScannerActivity.APP_KEY, BuildConfig.APP_KEY)
        putExtra(AuthWithScannerActivity.PUBLIC_USER_ID, publicUserId)
        putExtra(AuthWithScannerActivity.PAYLOAD, payload)
    }

    easyKeyriAuthLauncher.launch(intent)
}
```
