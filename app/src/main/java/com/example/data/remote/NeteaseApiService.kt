package com.example.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface NeteaseApiService {
    @GET("login/status")
    suspend fun getLoginStatus(@Query("timestamp") timestamp: Long = System.currentTimeMillis()): LoginStatusWrapper

    @GET("user/account")
    suspend fun getUserAccount(@Query("timestamp") timestamp: Long = System.currentTimeMillis()): UserAccountResponse

    @GET("captcha/sent")
    suspend fun sendCaptcha(
        @Query("phone") phone: String,
        @Query("ctcode") countryCode: String = "86",
        @Query("timestamp") timestamp: Long = System.currentTimeMillis()
    ): BasicStatusResponse

    @GET("login/cellphone")
    suspend fun loginWithCellphoneCaptcha(
        @Query("phone") phone: String,
        @Query("captcha") captcha: String,
        @Query("countrycode") countryCode: String = "86",
        @Query("timestamp") timestamp: Long = System.currentTimeMillis()
    ): CellphoneLoginResponse

    @GET("login/qr/key")
    suspend fun createQrKey(@Query("timestamp") timestamp: Long = System.currentTimeMillis()): QrKeyResponse

    @GET("login/qr/create")
    suspend fun createQrCode(
        @Query("key") key: String,
        @Query("qrimg") qrImg: Boolean = true,
        @Query("timestamp") timestamp: Long = System.currentTimeMillis()
    ): QrCreateResponse

    @GET("login/qr/check")
    suspend fun checkQrStatus(
        @Query("key") key: String,
        @Query("timestamp") timestamp: Long = System.currentTimeMillis()
    ): QrCheckResponse

    @GET("logout")
    suspend fun logout(): BasicStatusResponse

    @GET("personalized")
    suspend fun getPersonalizedPlaylists(@Query("limit") limit: Int = 12): PersonalizedResponse

    @GET("toplist")
    suspend fun getToplists(): ToplistResponse

    @GET("recommend/songs")
    suspend fun getDailyRecommendSongs(): DailyRecommendResponse

    @GET("user/playlist")
    suspend fun getUserPlaylists(
        @Query("uid") uid: Long,
        @Query("limit") limit: Int = 50
    ): UserPlaylistResponse

    @GET("playlist/detail")
    suspend fun getPlaylistDetail(@Query("id") id: Long): PlaylistDetailResponse

    @GET("playlist/track/all")
    suspend fun getPlaylistTracks(
        @Query("id") id: Long,
        @Query("limit") limit: Int = 80,
        @Query("offset") offset: Int = 0
    ): PlaylistTracksResponse

    @GET("song/url/v1")
    suspend fun getSongUrls(
        @Query("id") ids: String,
        @Query("level") level: String = "exhigh"
    ): SongUrlResponse

    @GET("lyric")
    suspend fun getLyric(@Query("id") id: Long): LyricResponse

    @GET("cloudsearch")
    suspend fun cloudSearch(
        @Query("keywords") keywords: String,
        @Query("type") type: Int = 1,
        @Query("limit") limit: Int = 30,
        @Query("offset") offset: Int = 0
    ): CloudSearchResponse

    @GET("personalized/mv")
    suspend fun getPersonalizedMvs(): PersonalizedMvResponse

    @GET("mv/all")
    suspend fun getAllMvs(
        @Query("limit") limit: Int = 24,
        @Query("offset") offset: Int = 0
    ): AllMvResponse

    @GET("mv/detail")
    suspend fun getMvDetail(@Query("mvid") mvId: Long): MvDetailResponse

    @GET("mv/url")
    suspend fun getMvUrl(
        @Query("id") mvId: Long,
        @Query("r") resolution: Int = 1080
    ): MvUrlResponse

    @GET("like")
    suspend fun toggleLike(
        @Query("id") id: Long,
        @Query("like") like: Boolean
    ): BasicStatusResponse

    @GET("likelist")
    suspend fun getLikeList(@Query("uid") uid: Long): LikeListResponse
}

interface PublicNeteaseApiService {
    @GET("api/toplist")
    suspend fun getToplists(): ToplistResponse

    @GET("api/v6/playlist/detail")
    suspend fun getPlaylistDetail(
        @Query("id") id: Long,
        @Query("n") limit: Int = 60
    ): PlaylistDetailResponse

    @GET("api/cloudsearch/pc")
    suspend fun search(
        @Query("s") keywords: String,
        @Query("type") type: Int = 1,
        @Query("limit") limit: Int = 30,
        @Query("offset") offset: Int = 0
    ): CloudSearchResponse

    @GET("api/song/lyric")
    suspend fun getLyric(
        @Query("id") id: Long,
        @Query("lv") lv: Int = 1,
        @Query("tv") tv: Int = 1
    ): LyricResponse

    @GET("api/mv/detail")
    suspend fun getMvDetail(@Query("id") mvId: Long): MvDetailResponse
}

class DynamicBaseUrlInterceptor(
    private val baseUrlProvider: () -> String
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val configuredUrl = baseUrlProvider().trim()
        val parsedBase = configuredUrl.toHttpUrlOrNull()
        if (parsedBase == null) {
            return chain.proceed(original)
        }
        val newUrl = original.url.newBuilder()
            .scheme(parsedBase.scheme)
            .host(parsedBase.host)
            .port(parsedBase.port)
            .build()
        val newRequest = original.newBuilder()
            .url(newUrl)
            .build()
        return chain.proceed(newRequest)
    }
}

class NeteaseCookieInterceptor(
    private val cookieProvider: () -> String,
    private val onCookiesCaptured: (String) -> Unit
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val cookie = cookieProvider().trim()
        val requestBuilder = chain.request().newBuilder()
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/126.0 Mobile Safari/537.36")
        if (cookie.isNotEmpty()) {
            requestBuilder.header("Cookie", cookie)
        }
        val response = chain.proceed(requestBuilder.build())
        val setCookies = response.headers("Set-Cookie")
        if (setCookies.isNotEmpty()) {
            val merged = mergeCookies(cookie, setCookies)
            if (merged.isNotBlank() && merged != cookie) {
                onCookiesCaptured(merged)
            }
        }
        return response
    }

    private fun mergeCookies(existing: String, newSetCookies: List<String>): String {
        val map = LinkedHashMap<String, String>()
        existing.split(";").forEach { pair ->
            val idx = pair.indexOf('=')
            if (idx > 0) {
                map[pair.substring(0, idx).trim()] = pair.substring(idx + 1).trim()
            }
        }
        newSetCookies.forEach { header ->
            val firstPart = header.substringBefore(';')
            val idx = firstPart.indexOf('=')
            if (idx > 0) {
                val key = firstPart.substring(0, idx).trim()
                val value = firstPart.substring(idx + 1).trim()
                if (value.isNotEmpty()) {
                    map[key] = value
                }
            }
        }
        return map.entries.joinToString("; ") { "${it.key}=${it.value}" }
    }
}
