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
    implementation("com.github.Keyri-Co:keyri-android-whitelabel-sdk:$latestKeyriVersion")
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
        // Process error
    }

    override fun onSuccess(profile: UserProfile) {
        val email = profile.email

        val payload = JSONObject().apply {
            put("token", accessToken)
            put("provider", "auth0:email_password") // Optional
            put("timestamp", System.currentTimeMillis()) // Optional
            put("associationKey", keyri.getAssociationKey(email)) // Optional
            put("userSignature", keyri.getUserSignature(email, email)) // Optional
        }.toString()

        // See Keyri authorization section
        // Public user ID (email) is optional
        keyriAuth(email, payload)
    }
}

AuthenticationAPIClient(account)
    .userInfo(accessToken)
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
