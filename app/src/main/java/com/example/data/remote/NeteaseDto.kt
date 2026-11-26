package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BasicStatusResponse(
    val code: Int = 0,
    val message: String? = null,
    val msg: String? = null,
    val data: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class CellphoneLoginResponse(
    val code: Int = 0,
    val message: String? = null,
    val msg: String? = null,
    val cookie: String? = null,
    val profile: UserProfileDto? = null,
    val account: UserAccountDto? = null
)

@JsonClass(generateAdapter = true)
data class LoginStatusWrapper(
    val data: LoginStatusData? = null
)

@JsonClass(generateAdapter = true)
data class LoginStatusData(
    val code: Int = 0,
    val account: UserAccountDto? = null,
    val profile: UserProfileDto? = null
)

@JsonClass(generateAdapter = true)
data class UserAccountResponse(
    val code: Int = 0,
    val account: UserAccountDto? = null,
    val profile: UserProfileDto? = null
)

@JsonClass(generateAdapter = true)
data class UserAccountDto(
    val id: Long = 0L,
    val vipType: Int = 0
)

@JsonClass(generateAdapter = true)
data class UserProfileDto(
    val userId: Long = 0L,
    val nickname: String = "",
    val avatarUrl: String = "",
    val signature: String? = null,
    val vipType: Int = 0
)

@JsonClass(generateAdapter = true)
data class QrKeyResponse(
    val code: Int = 0,
    val data: QrKeyData? = null
)

@JsonClass(generateAdapter = true)
data class QrKeyData(
    val unikey: String = ""
)

@JsonClass(generateAdapter = true)
data class QrCreateResponse(
    val code: Int = 0,
    val data: QrCreateData? = null
)

@JsonClass(generateAdapter = true)
data class QrCreateData(
    val qrurl: String = "",
    val qrimg: String = ""
)

@JsonClass(generateAdapter = true)
data class QrCheckResponse(
    val code: Int = 0,
    val message: String = "",
    val cookie: String = ""
)

@JsonClass(generateAdapter = true)
data class PersonalizedResponse(
    val code: Int = 0,
    val result: List<PersonalizedPlaylistDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class PersonalizedPlaylistDto(
    val id: Long = 0L,
    val name: String = "",
    val picUrl: String = "",
    val copywriter: String? = null,
    val playCount: Long = 0L,
    val trackCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class ToplistResponse(
    val code: Int = 0,
    val list: List<PlaylistDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class UserPlaylistResponse(
    val code: Int = 0,
    val playlist: List<PlaylistDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class PlaylistDto(
    val id: Long = 0L,
    val name: String = "",
    val coverImgUrl: String? = null,
    val picUrl: String? = null,
    val description: String? = null,
    val trackCount: Int = 0,
    val playCount: Long = 0L,
    val creator: PlaylistCreatorDto? = null,
    val tracks: List<SongDto>? = null
)

@JsonClass(generateAdapter = true)
data class PlaylistCreatorDto(
    val userId: Long = 0L,
    val nickname: String = ""
)

@JsonClass(generateAdapter = true)
data class PlaylistDetailResponse(
    val code: Int = 0,
    val playlist: PlaylistDto? = null
)

@JsonClass(generateAdapter = true)
data class PlaylistTracksResponse(
    val code: Int = 0,
    val songs: List<SongDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class DailyRecommendResponse(
    val code: Int = 0,
    val data: DailyRecommendData? = null
)

@JsonClass(generateAdapter = true)
data class DailyRecommendData(
    val dailySongs: List<SongDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SongDetailResponse(
    val code: Int = 0,
    val songs: List<SongDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SongDto(
    val id: Long = 0L,
    val name: String = "",
    val ar: List<ArtistDto> = emptyList(),
    val artists: List<ArtistDto> = emptyList(),
    val al: AlbumDto? = null,
    val album: AlbumDto? = null,
    val dt: Long = 0L,
    val duration: Long = 0L,
    val mv: Long = 0L,
    val mvid: Long = 0L
)

@JsonClass(generateAdapter = true)
data class ArtistDto(
    val id: Long = 0L,
    val name: String = ""
)

@JsonClass(generateAdapter = true)
data class AlbumDto(
    val id: Long = 0L,
    val name: String = "",
    val picUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class SongUrlResponse(
    val code: Int = 0,
    val data: List<SongUrlItemDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SongUrlItemDto(
    val id: Long = 0L,
    val url: String? = null,
    val br: Long = 0L,
    val level: String? = null,
    val type: String? = null
)

@JsonClass(generateAdapter = true)
data class LyricResponse(
    val code: Int = 0,
    val lrc: LyricSectionDto? = null,
    val tlyric: LyricSectionDto? = null,
    val romalrc: LyricSectionDto? = null
)

@JsonClass(generateAdapter = true)
data class LyricSectionDto(
    val lyric: String? = null
)

@JsonClass(generateAdapter = true)
data class CloudSearchResponse(
    val code: Int = 0,
    val result: CloudSearchResultDto? = null
)

@JsonClass(generateAdapter = true)
data class CloudSearchResultDto(
    val songs: List<SongDto>? = null,
    val playlists: List<PlaylistDto>? = null,
    val mvs: List<MvDto>? = null
)

@JsonClass(generateAdapter = true)
data class PersonalizedMvResponse(
    val code: Int = 0,
    val result: List<MvDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AllMvResponse(
    val code: Int = 0,
    val data: List<MvDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class MvDto(
    val id: Long = 0L,
    val name: String = "",
    val artistName: String? = null,
    val artists: List<ArtistDto>? = null,
    val picUrl: String? = null,
    val cover: String? = null,
    val duration: Long = 0L,
    val playCount: Long = 0L
)

@JsonClass(generateAdapter = true)
data class MvDetailResponse(
    val code: Int = 0,
    val data: MvDetailDto? = null
)

@JsonClass(generateAdapter = true)
data class MvDetailDto(
    val id: Long = 0L,
    val name: String = "",
    val artistName: String = "",
    val cover: String? = null,
    val duration: Long = 0L,
    val playCount: Long = 0L,
    val brs: List<MvBitrateDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class MvBitrateDto(
    val br: Int = 0
)

@JsonClass(generateAdapter = true)
data class MvUrlResponse(
    val code: Int = 0,
    val data: MvUrlDataDto? = null
)

@JsonClass(generateAdapter = true)
data class MvUrlDataDto(
    val id: Long = 0L,
    val url: String? = null,
    val r: Int = 0
)

@JsonClass(generateAdapter = true)
data class LikeListResponse(
    val code: Int = 0,
    val ids: List<Long> = emptyList()
)
