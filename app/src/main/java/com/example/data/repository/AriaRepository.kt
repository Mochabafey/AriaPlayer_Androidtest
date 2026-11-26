package com.example.data.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.local.AppPreferences
import com.example.data.local.AriaDatabase
import com.example.data.local.FavoriteTrackEntity
import com.example.data.local.LocalMediaEntity
import com.example.data.local.PlayHistoryEntity
import com.example.data.local.SearchHistoryEntity
import com.example.data.local.UserSettings
import com.example.data.remote.DynamicBaseUrlInterceptor
import com.example.data.remote.MvDto
import com.example.data.remote.NeteaseApiService
import com.example.data.remote.NeteaseCookieInterceptor
import com.example.data.remote.PlaylistDto
import com.example.data.remote.PublicNeteaseApiService
import com.example.data.remote.SongDto
import com.example.model.AudioQuality
import com.example.model.LrcParser
import com.example.model.LyricLine
import com.example.model.MediaKind
import com.example.model.MediaSourceType
import com.example.model.MediaTrack
import com.example.model.MvItem
import com.example.model.NeteaseUser
import com.example.model.PlaylistSummary
import com.example.ui.theme.ThemeMode
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

data class QrSessionData(
    val unikey: String,
    val qrUrl: String
)

sealed interface QrPollStatus {
    data object WaitingScan : QrPollStatus
    data object WaitingConfirm : QrPollStatus
    data object Expired : QrPollStatus
    data class Success(val user: NeteaseUser) : QrPollStatus
    data class Error(val message: String) : QrPollStatus
}

data class PlaylistDetailData(
    val summary: PlaylistSummary,
    val tracks: List<MediaTrack>
)

data class SearchBundle(
    val songs: List<MediaTrack> = emptyList(),
    val playlists: List<PlaylistSummary> = emptyList(),
    val mvs: List<MvItem> = emptyList()
)

class AriaRepository(
    private val context: Context,
    private val preferences: AppPreferences,
    private val database: AriaDatabase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dao = database.ariaDao()

    private val currentBaseUrl = AtomicReference("")
    private val currentCookie = AtomicReference("")

    init {
        scope.launch {
            preferences.settingsFlow.collect { settings ->
                currentBaseUrl.set(settings.apiBaseUrl)
                currentCookie.set(settings.cookie)
            }
        }
    }

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val neteaseHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(DynamicBaseUrlInterceptor { currentBaseUrl.get() })
        .addInterceptor(
            NeteaseCookieInterceptor(
                cookieProvider = { currentCookie.get() },
                onCookiesCaptured = { captured ->
                    currentCookie.set(captured)
                }
            )
        )
        .addInterceptor(logging)
        .build()

    private val publicHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("Referer", "https://music.163.com/")
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/126.0 Safari/537.36"
                )
                .build()
            chain.proceed(req)
        }
        .build()

    private val neteaseApi: NeteaseApiService = Retrofit.Builder()
        .baseUrl("http://localhost:3000/")
        .client(neteaseHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(NeteaseApiService::class.java)

    private val publicApi: PublicNeteaseApiService = Retrofit.Builder()
        .baseUrl("https://music.163.com/")
        .client(publicHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(PublicNeteaseApiService::class.java)

    val settingsFlow: Flow<UserSettings> = preferences.settingsFlow
    val playHistoryFlow: Flow<List<MediaTrack>> = dao.observePlayHistory().map { list -> list.map { it.toDomain() } }
    val localMediaFlow: Flow<List<MediaTrack>> = dao.observeLocalMedia().map { list -> list.map { it.toDomain() } }
    val favoritesFlow: Flow<List<MediaTrack>> = dao.observeFavorites().map { list -> list.map { it.toDomain() } }
    val searchHistoryFlow: Flow<List<String>> = dao.observeSearchHistory().map { list -> list.map { it.keyword } }

    fun observeIsFavorite(trackId: String): Flow<Boolean> = dao.observeIsFavorite(trackId)

    private fun hasCustomServer(): Boolean = currentBaseUrl.get().isNotBlank()

    suspend fun saveServerUrl(url: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val normalized = AppPreferences.normalizeUrl(url)
            preferences.setApiBaseUrl(normalized)
            currentBaseUrl.set(normalized)
            if (normalized.isNotBlank()) {
                val status = neteaseApi.getToplists()
                if (status.code != 200 && status.list.isEmpty()) {
                    throw IllegalStateException("服务器响应异常 (code=${status.code})")
                }
            }
            normalized
        }
    }

    suspend fun sendSmsCaptcha(phone: String, countryCode: String = "86"): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(hasCustomServer()) { "请先在下方或设置中填写您的 api-NetEasyCloud 服务地址" }
            val cleanPhone = phone.trim()
            require(cleanPhone.length >= 6) { "请输入有效的手机号码" }
            val resp = neteaseApi.sendCaptcha(phone = cleanPhone, countryCode = countryCode.trim())
            if (resp.code != 200) {
                throw IllegalStateException(resp.message ?: resp.msg ?: "验证码发送失败 (code=${resp.code})")
            }
        }
    }

    suspend fun loginWithPhoneCaptcha(
        phone: String,
        captcha: String,
        countryCode: String = "86"
    ): Result<NeteaseUser> = withContext(Dispatchers.IO) {
        runCatching {
            require(hasCustomServer()) { "请先配置 api-NetEasyCloud 服务地址" }
            val resp = neteaseApi.loginWithCellphoneCaptcha(
                phone = phone.trim(),
                captcha = captcha.trim(),
                countryCode = countryCode.trim()
            )
            if (resp.code != 200) {
                throw IllegalStateException(resp.message ?: resp.msg ?: "登录失败，请检查手机号或验证码 (code=${resp.code})")
            }
            val cookie = resp.cookie?.takeIf { it.isNotBlank() } ?: currentCookie.get()
            require(cookie.isNotBlank()) { "登录成功但未获取到会话 Cookie" }
            currentCookie.set(cookie)
            val profile = resp.profile
            val user = if (profile != null && profile.userId > 0L) {
                NeteaseUser(
                    userId = profile.userId,
                    nickname = profile.nickname.ifBlank { "网易云用户" },
                    avatarUrl = ensureHttps(profile.avatarUrl),
                    signature = profile.signature.orEmpty(),
                    vipType = profile.vipType
                )
            } else {
                fetchCurrentUserAccount()
            }
            preferences.saveSession(cookie, user)
            user
        }
    }

    suspend fun createQrLogin(): Result<QrSessionData> = withContext(Dispatchers.IO) {
        runCatching {
            require(hasCustomServer()) { "请先配置 api-NetEasyCloud 服务地址" }
            val keyResp = neteaseApi.createQrKey()
            val unikey = keyResp.data?.unikey.orEmpty()
            require(unikey.isNotBlank()) { "获取二维码 key 失败" }
            val createResp = neteaseApi.createQrCode(key = unikey, qrImg = false)
            val qrUrl = createResp.data?.qrurl.orEmpty()
            require(qrUrl.isNotBlank()) { "生成二维码链接失败" }
            QrSessionData(unikey = unikey, qrUrl = qrUrl)
        }
    }

    suspend fun checkQrLogin(unikey: String): QrPollStatus = withContext(Dispatchers.IO) {
        runCatching {
            val resp = neteaseApi.checkQrStatus(key = unikey)
            when (resp.code) {
                800 -> QrPollStatus.Expired
                801 -> QrPollStatus.WaitingScan
                802 -> QrPollStatus.WaitingConfirm
                803 -> {
                    val cookie = resp.cookie.ifBlank { currentCookie.get() }
                    currentCookie.set(cookie)
                    val user = fetchCurrentUserAccount()
                    preferences.saveSession(cookie, user)
                    QrPollStatus.Success(user)
                }
                else -> QrPollStatus.Error(resp.message.ifBlank { "扫码状态异常 (${resp.code})" })
            }
        }.getOrElse { QrPollStatus.Error(it.message ?: "网络轮询失败") }
    }

    suspend fun loginWithCookie(rawCookie: String): Result<NeteaseUser> = withContext(Dispatchers.IO) {
        runCatching {
            require(hasCustomServer()) { "请先配置 api-NetEasyCloud 服务地址" }
            val cleaned = rawCookie.trim()
            require(cleaned.isNotBlank()) { "Cookie 不能为空" }
            currentCookie.set(cleaned)
            val user = fetchCurrentUserAccount()
            preferences.saveSession(cleaned, user)
            user
        }
    }

    private suspend fun fetchCurrentUserAccount(): NeteaseUser {
        val status = neteaseApi.getLoginStatus()
        val profile = status.data?.profile ?: neteaseApi.getUserAccount().profile
        val uid = profile?.userId ?: status.data?.account?.id ?: 0L
        if (uid <= 0L) {
            throw IllegalStateException("未检测到有效登录账号，请检查 Cookie 或重新登录")
        }
        return NeteaseUser(
            userId = uid,
            nickname = profile?.nickname?.ifBlank { "网易云音乐用户" } ?: "网易云音乐用户",
            avatarUrl = ensureHttps(profile?.avatarUrl.orEmpty()),
            signature = profile?.signature.orEmpty(),
            vipType = profile?.vipType ?: 0
        )
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        runCatching {
            if (hasCustomServer()) neteaseApi.logout()
        }
        currentCookie.set("")
        preferences.clearSession()
    }

    suspend fun getPersonalizedPlaylists(): Result<List<PlaylistSummary>> = withContext(Dispatchers.IO) {
        runCatching {
            if (hasCustomServer()) {
                val resp = neteaseApi.getPersonalizedPlaylists(12)
                resp.result.map {
                    PlaylistSummary(
                        id = it.id,
                        name = it.name,
                        coverUrl = ensureHttps(it.picUrl),
                        description = it.copywriter.orEmpty(),
                        trackCount = it.trackCount,
                        playCount = it.playCount
                    )
                }
            } else {
                // Fallback to public Netease toplists when custom backend is not yet configured
                publicApi.getToplists().list.take(12).map { it.toSummary() }
            }
        }
    }

    suspend fun getToplists(): Result<List<PlaylistSummary>> = withContext(Dispatchers.IO) {
        runCatching {
            val list = if (hasCustomServer()) {
                neteaseApi.getToplists().list
            } else {
                publicApi.getToplists().list
            }
            list.take(16).map { it.toSummary() }
        }
    }

    suspend fun getDailyRecommendSongs(): Result<List<MediaTrack>> = withContext(Dispatchers.IO) {
        runCatching {
            if (!hasCustomServer() || currentCookie.get().isBlank()) {
                return@runCatching emptyList()
            }
            neteaseApi.getDailyRecommendSongs().data?.dailySongs.orEmpty().map { it.toTrack() }
        }
    }

    suspend fun getUserPlaylists(uid: Long): Result<List<PlaylistSummary>> = withContext(Dispatchers.IO) {
        runCatching {
            if (!hasCustomServer() || uid <= 0L) return@runCatching emptyList()
            neteaseApi.getUserPlaylists(uid).playlist.map { it.toSummary() }
        }
    }

    suspend fun getPlaylistDetail(playlistId: Long): Result<PlaylistDetailData> = withContext(Dispatchers.IO) {
        runCatching {
            if (hasCustomServer()) {
                val detail = neteaseApi.getPlaylistDetail(playlistId).playlist
                    ?: throw IllegalStateException("未找到歌单信息")
                val tracks = runCatching {
                    neteaseApi.getPlaylistTracks(playlistId, limit = 80).songs
                }.getOrNull()?.takeIf { it.isNotEmpty() } ?: detail.tracks.orEmpty()
                PlaylistDetailData(
                    summary = detail.toSummary(),
                    tracks = tracks.map { it.toTrack() }
                )
            } else {
                val detail = publicApi.getPlaylistDetail(playlistId).playlist
                    ?: throw IllegalStateException("获取歌单详情失败")
                PlaylistDetailData(
                    summary = detail.toSummary(),
                    tracks = detail.tracks.orEmpty().map { it.toTrack() }
                )
            }
        }
    }

    suspend fun search(keywords: String, type: Int): Result<SearchBundle> = withContext(Dispatchers.IO) {
        runCatching {
            val trimmed = keywords.trim()
            if (trimmed.isEmpty()) return@runCatching SearchBundle()
            dao.upsertSearchKeyword(SearchHistoryEntity(trimmed))
            val result = if (hasCustomServer()) {
                neteaseApi.cloudSearch(keywords = trimmed, type = type).result
            } else {
                publicApi.search(keywords = trimmed, type = type).result
            }
            SearchBundle(
                songs = result?.songs.orEmpty().map { it.toTrack() },
                playlists = result?.playlists.orEmpty().map { it.toSummary() },
                mvs = result?.mvs.orEmpty().map { it.toMvItem() }
            )
        }
    }

    suspend fun getFeaturedMvs(): Result<List<MvItem>> = withContext(Dispatchers.IO) {
        runCatching {
            if (hasCustomServer()) {
                val personalized = runCatching { neteaseApi.getPersonalizedMvs().result }.getOrDefault(emptyList())
                val all = runCatching { neteaseApi.getAllMvs(limit = 20).data }.getOrDefault(emptyList())
                (personalized + all).distinctBy { it.id }.map { it.toMvItem() }
            } else {
                // Public search for popular official MVs so the video tab works out-of-the-box
                publicApi.search(keywords = "MV", type = 1004, limit = 20).result?.mvs.orEmpty().map { it.toMvItem() }
            }
        }
    }

    suspend fun resolveAudioStreamUrl(track: MediaTrack): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (track.sourceType != MediaSourceType.NETEASE) {
                return@runCatching track.streamUrl ?: throw IllegalStateException("无效的播放地址")
            }
            val numericId = track.id.removePrefix("ncm_").toLongOrNull()
                ?: throw IllegalStateException("无效的曲目 ID")
            val quality = preferences.settingsFlow.first().audioQuality.level
            if (hasCustomServer()) {
                val urlResp = neteaseApi.getSongUrls(ids = numericId.toString(), level = quality)
                val url = urlResp.data.firstOrNull()?.url
                if (!url.isNullOrBlank()) {
                    return@runCatching ensureHttps(url)
                }
            }
            // Fallback public outer playback endpoint for non-VIP tracks
            "https://music.163.com/song/media/outer/url?id=$numericId.mp3"
        }
    }

    suspend fun resolveMvPlaybackTrack(mvId: Long, fallbackTitle: String = "", fallbackArtist: String = "", fallbackCover: String? = null): Result<MediaTrack> = withContext(Dispatchers.IO) {
        runCatching {
            val detail = runCatching {
                if (hasCustomServer()) neteaseApi.getMvDetail(mvId).data
                else publicApi.getMvDetail(mvId).data
            }.getOrNull()

            val videoUrl = if (hasCustomServer()) {
                val preferredRes = detail?.brs?.maxOfOrNull { it.br } ?: 1080
                neteaseApi.getMvUrl(mvId, preferredRes).data?.url
            } else {
                null
            }

            val resolvedUrl = videoUrl?.takeIf { it.isNotBlank() }?.let { ensureHttps(it) }
                ?: throw IllegalStateException("该 MV 需要连接您的 api-NetEasyCloud 服务以解析视频流地址")

            MediaTrack(
                id = "mv_$mvId",
                title = detail?.name?.ifBlank { fallbackTitle } ?: fallbackTitle.ifBlank { "MV #$mvId" },
                artist = detail?.artistName?.ifBlank { fallbackArtist } ?: fallbackArtist,
                album = "MV 视频",
                coverUrl = ensureHttps(detail?.cover ?: fallbackCover.orEmpty()).ifBlank { null },
                streamUrl = resolvedUrl,
                mvId = mvId,
                durationMs = detail?.duration ?: 0L,
                sourceType = MediaSourceType.NETEASE,
                mediaKind = MediaKind.VIDEO,
                qualityLabel = "HD MV"
            )
        }
    }

    suspend fun getLyrics(track: MediaTrack): List<LyricLine> = withContext(Dispatchers.IO) {
        if (track.sourceType != MediaSourceType.NETEASE || track.isVideo) {
            return@withContext emptyList()
        }
        val numericId = track.id.removePrefix("ncm_").toLongOrNull() ?: return@withContext emptyList()
        runCatching {
            val resp = if (hasCustomServer()) {
                neteaseApi.getLyric(numericId)
            } else {
                publicApi.getLyric(numericId)
            }
            LrcParser.parse(
                rawLrc = resp.lrc?.lyric,
                rawTranslatedLrc = resp.tlyric?.lyric
            )
        }.getOrDefault(emptyList())
    }

    suspend fun recordPlayHistory(track: MediaTrack) = withContext(Dispatchers.IO) {
        dao.upsertHistory(PlayHistoryEntity.fromDomain(track))
    }

    suspend fun clearPlayHistory() = withContext(Dispatchers.IO) {
        dao.clearHistory()
    }

    suspend fun toggleFavorite(track: MediaTrack): Boolean = withContext(Dispatchers.IO) {
        val currentlyFav = dao.observeIsFavorite(track.id).first()
        if (currentlyFav) {
            dao.removeFavorite(track.id)
        } else {
            dao.upsertFavorite(FavoriteTrackEntity.fromDomain(track))
        }
        if (track.sourceType == MediaSourceType.NETEASE && hasCustomServer() && currentCookie.get().isNotBlank()) {
            val numericId = track.id.removePrefix("ncm_").toLongOrNull()
            if (numericId != null) {
                runCatching { neteaseApi.toggleLike(numericId, !currentlyFav) }
            }
        }
        !currentlyFav
    }

    suspend fun importLocalUri(uri: Uri, forceVideo: Boolean = false): Result<MediaTrack> = withContext(Dispatchers.IO) {
        runCatching {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val mimeType = context.contentResolver.getType(uri).orEmpty()
            val isVideo = forceVideo || mimeType.startsWith("video/")
            val displayName = queryDisplayName(uri) ?: "本地媒体"
            var title = displayName.substringBeforeLast('.')
            var artist = if (isVideo) "本地视频" else "未知艺术家"
            var durationMs = 0L

            val retriever = MediaMetadataRetriever()
            runCatching {
                retriever.setDataSource(context, uri)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    ?.takeIf { it.isNotBlank() }?.let { title = it }
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    ?.takeIf { it.isNotBlank() }?.let { artist = it }
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull()?.let { durationMs = it }
            }
            runCatching { retriever.release() }

            val id = "local_${uri.toString().hashCode()}"
            val entity = LocalMediaEntity(
                id = id,
                uriString = uri.toString(),
                title = title,
                artist = artist,
                durationMs = durationMs,
                mediaKind = if (isVideo) MediaKind.VIDEO.name else MediaKind.AUDIO.name
            )
            dao.upsertLocalMedia(entity)
            entity.toDomain()
        }
    }

    suspend fun clearSearchHistory() = withContext(Dispatchers.IO) {
        dao.clearSearchHistory()
    }

    suspend fun setAudioQuality(quality: AudioQuality) = preferences.setAudioQuality(quality)
    suspend fun setLyricFontScale(scale: Float) = preferences.setLyricFontScale(scale)
    suspend fun setShowTranslatedLyrics(show: Boolean) = preferences.setShowTranslatedLyrics(show)
    suspend fun setThemeMode(mode: ThemeMode) = preferences.setThemeMode(mode)

    private fun queryDisplayName(uri: Uri): String? {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return cursor.getString(idx)
            }
        }
        return uri.lastPathSegment
    }

    private fun PlaylistDto.toSummary(): PlaylistSummary = PlaylistSummary(
        id = id,
        name = name,
        coverUrl = ensureHttps(coverImgUrl ?: picUrl.orEmpty()),
        description = description.orEmpty(),
        trackCount = trackCount,
        playCount = playCount,
        creatorName = creator?.nickname.orEmpty()
    )

    private fun SongDto.toTrack(): MediaTrack {
        val artistsList = if (ar.isNotEmpty()) ar else artists
        val artistStr = artistsList.joinToString(" / ") { it.name }.ifBlank { "未知歌手" }
        val albumObj = al ?: album
        val cover = ensureHttps(albumObj?.picUrl.orEmpty()).ifBlank { null }
        val duration = if (dt > 0L) dt else duration
        val mv = if (mv > 0L) mv else mvid
        return MediaTrack(
            id = "ncm_$id",
            title = name,
            artist = artistStr,
            album = albumObj?.name.orEmpty(),
            coverUrl = cover,
            streamUrl = null,
            mvId = mv,
            durationMs = duration,
            sourceType = MediaSourceType.NETEASE,
            mediaKind = MediaKind.AUDIO
        )
    }

    private fun MvDto.toMvItem(): MvItem {
        val artist = artistName?.takeIf { it.isNotBlank() }
            ?: artists.orEmpty().joinToString(" / ") { it.name }.ifBlank { "未知歌手" }
        return MvItem(
            id = id,
            title = name,
            artistName = artist,
            coverUrl = ensureHttps(cover ?: picUrl.orEmpty()),
            durationMs = duration,
            playCount = playCount
        )
    }

    private fun ensureHttps(url: String): String {
        val trimmed = url.trim()
        return if (trimmed.startsWith("http://")) {
            trimmed.replaceFirst("http://", "https://")
        } else trimmed
    }
}
