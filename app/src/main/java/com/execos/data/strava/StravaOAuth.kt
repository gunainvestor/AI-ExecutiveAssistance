package com.execos.data.strava

import android.content.Context
import android.net.Uri
import com.execos.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Strava OAuth2 uses authorization/token endpoints (not OIDC discovery).
 */
@Singleton
class StravaOAuth @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val serviceConfig = AuthorizationServiceConfiguration(
        Uri.parse("https://www.strava.com/oauth/mobile/authorize"),
        Uri.parse("https://www.strava.com/oauth/token"),
    )

    fun buildAuthRequest(): AuthorizationRequest {
        return AuthorizationRequest.Builder(
            serviceConfig,
            BuildConfig.STRAVA_CLIENT_ID,
            ResponseTypeValues.CODE,
            Uri.parse(BuildConfig.STRAVA_REDIRECT_URI),
        )
            .setScope("read,activity:read_all")
            .build()
    }

    fun authorizationService(): AuthorizationService = AuthorizationService(context)
}

