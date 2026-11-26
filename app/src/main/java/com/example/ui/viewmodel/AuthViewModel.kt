package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.UserSettings
import com.example.data.repository.AriaRepository
import com.example.data.repository.QrPollStatus
import com.example.model.AudioQuality
import com.example.model.PlaylistSummary
import com.example.ui.theme.ThemeMode
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class LoginMethodTab(val label: String) {
    SMS("手机验证码"),
    QR("二维码扫码"),
    COOKIE("Cookie 导入")
}

data class AuthUiState(
    val settings: UserSettings = UserSettings(),
    val serverUrlInput: String = "",
    val isTestingServer: Boolean = false,
    val selectedLoginTab: LoginMethodTab = LoginMethodTab.SMS,
    // SMS Login
    val countryCode: String = "86",
    val phoneInput: String = "",
    val captchaInput: String = "",
    val isSendingCaptcha: Boolean = false,
    val captchaCountdown: Int = 0,
    val isLoggingInSms: Boolean = false,
    // QR Login
    val qrBitmap: ImageBitmap? = null,
    val qrStatusText: String = "点击下方按钮生成网易云登录二维码",
    val isQrExpired: Boolean = false,
    val isGeneratingQr: Boolean = false,
    // Cookie Login
    val cookieInput: String = "",
    val isLoggingInCookie: Boolean = false,
    // User Library
    val userPlaylists: List<PlaylistSummary> = emptyList(),
    val statusBanner: String? = null,
    val errorMessage: String? = null
)

class AuthViewModel(
    private val repository: AriaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null
    private var qrPollJob: Job? = null

    init {
        viewModelScope.launch {
            repository.settingsFlow.collect { settings ->
                _uiState.update { state ->
                    state.copy(
                        settings = settings,
                        serverUrlInput = if (state.serverUrlInput.isBlank() && settings.apiBaseUrl.isNotBlank()) {
                            settings.apiBaseUrl
                        } else state.serverUrlInput
                    )
                }
            }
        }
        viewModelScope.launch {
            repository.settingsFlow
                .map { it.user?.userId }
                .distinctUntilChanged()
                .collect { uid ->
                    if (uid != null && uid > 0L) {
                        loadUserPlaylists(uid)
                    } else {
                        _uiState.update { it.copy(userPlaylists = emptyList()) }
                    }
                }
        }
    }

    fun updateServerUrlInput(url: String) {
        _uiState.update { it.copy(serverUrlInput = url) }
    }

    fun saveAndTestServerUrl() {
        val url = _uiState.value.serverUrlInput
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingServer = true, errorMessage = null, statusBanner = null) }
            repository.saveServerUrl(url)
                .onSuccess { saved ->
                    _uiState.update {
                        it.copy(
                            isTestingServer = false,
                            serverUrlInput = saved,
                            statusBanner = if (saved.isBlank()) "已切换为公开兜底模式" else "已连接 api-NetEasyCloud：$saved"
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isTestingServer = false,
                            errorMessage = err.message ?: "无法连接该服务地址，请检查端口与网络"
                        )
                    }
                }
        }
    }

    fun selectLoginTab(tab: LoginMethodTab) {
        _uiState.update { it.copy(selectedLoginTab = tab, errorMessage = null) }
        if (tab != LoginMethodTab.QR) {
            qrPollJob?.cancel()
        }
    }

    fun updateCountryCode(code: String) {
        _uiState.update { it.copy(countryCode = code.filter { ch -> ch.isDigit() }) }
    }

    fun updatePhoneInput(phone: String) {
        _uiState.update { it.copy(phoneInput = phone.filter { ch -> ch.isDigit() }) }
    }

    fun updateCaptchaInput(captcha: String) {
        _uiState.update { it.copy(captchaInput = captcha.filter { ch -> ch.isDigit() }.take(8)) }
    }

    fun sendSmsCaptcha() {
        val state = _uiState.value
        if (state.captchaCountdown > 0 || state.isSendingCaptcha) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSendingCaptcha = true, errorMessage = null, statusBanner = null) }
            repository.sendSmsCaptcha(state.phoneInput, state.countryCode.ifBlank { "86" })
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isSendingCaptcha = false,
                            statusBanner = "验证码已发送至 +${state.countryCode.ifBlank { "86" }} ${state.phoneInput}"
                        )
                    }
                    startCountdown()
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isSendingCaptcha = false,
                            errorMessage = err.message ?: "发送验证码失败"
                        )
                    }
                }
        }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (sec in 60 downTo 1) {
                _uiState.update { it.copy(captchaCountdown = sec) }
                delay(1000L)
            }
            _uiState.update { it.copy(captchaCountdown = 0) }
        }
    }

    fun loginWithSms() {
        val state = _uiState.value
        if (state.phoneInput.length < 6 || state.captchaInput.length < 4) {
            _uiState.update { it.copy(errorMessage = "请输入完整的手机号与短信验证码") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingInSms = true, errorMessage = null, statusBanner = null) }
            repository.loginWithPhoneCaptcha(
                phone = state.phoneInput,
                captcha = state.captchaInput,
                countryCode = state.countryCode.ifBlank { "86" }
            ).onSuccess { user ->
                _uiState.update {
                    it.copy(
                        isLoggingInSms = false,
                        captchaInput = "",
                        statusBanner = "登录成功，欢迎回来 ${user.nickname}"
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoggingInSms = false,
                        errorMessage = err.message ?: "验证码登录失败"
                    )
                }
            }
        }
    }

    fun startQrLogin() {
        qrPollJob?.cancel()
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isGeneratingQr = true,
                    isQrExpired = false,
                    qrStatusText = "正在生成登录二维码...",
                    errorMessage = null
                )
            }
            repository.createQrLogin()
                .onSuccess { session ->
                    val bitmap = renderQrBitmap(session.qrUrl)
                    _uiState.update {
                        it.copy(
                            isGeneratingQr = false,
                            qrBitmap = bitmap,
                            qrStatusText = "请使用网易云音乐 App 扫描二维码"
                        )
                    }
                    pollQrStatus(session.unikey)
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isGeneratingQr = false,
                            errorMessage = err.message ?: "生成二维码失败"
                        )
                    }
                }
        }
    }

    private fun pollQrStatus(unikey: String) {
        qrPollJob?.cancel()
        qrPollJob = viewModelScope.launch {
            while (isActive) {
                delay(2000L)
                when (val status = repository.checkQrLogin(unikey)) {
                    is QrPollStatus.WaitingScan -> {
                        _uiState.update { it.copy(qrStatusText = "等待扫码...") }
                    }
                    is QrPollStatus.WaitingConfirm -> {
                        _uiState.update { it.copy(qrStatusText = "已扫码，请在手机端确认登录") }
                    }
                    is QrPollStatus.Expired -> {
                        _uiState.update {
                            it.copy(isQrExpired = true, qrStatusText = "二维码已过期，请点击刷新")
                        }
                        break
                    }
                    is QrPollStatus.Success -> {
                        _uiState.update {
                            it.copy(
                                qrBitmap = null,
                                qrStatusText = "登录成功",
                                statusBanner = "扫码登录成功：${status.user.nickname}"
                            )
                        }
                        break
                    }
                    is QrPollStatus.Error -> {
                        _uiState.update { it.copy(qrStatusText = status.message) }
                    }
                }
            }
        }
    }

    fun updateCookieInput(cookie: String) {
        _uiState.update { it.copy(cookieInput = cookie) }
    }

    fun loginWithCookie() {
        val raw = _uiState.value.cookieInput
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingInCookie = true, errorMessage = null, statusBanner = null) }
            repository.loginWithCookie(raw)
                .onSuccess { user ->
                    _uiState.update {
                        it.copy(
                            isLoggingInCookie = false,
                            cookieInput = "",
                            statusBanner = "Cookie 验证成功：${user.nickname}"
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isLoggingInCookie = false,
                            errorMessage = err.message ?: "Cookie 无效或已过期"
                        )
                    }
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            qrPollJob?.cancel()
            repository.logout()
            _uiState.update { it.copy(statusBanner = "已退出网易云账号") }
        }
    }

    fun loadUserPlaylists(uid: Long) {
        viewModelScope.launch {
            repository.getUserPlaylists(uid).onSuccess { list ->
                _uiState.update { it.copy(userPlaylists = list) }
            }
        }
    }

    fun setAudioQuality(quality: AudioQuality) {
        viewModelScope.launch { repository.setAudioQuality(quality) }
    }

    fun setLyricFontScale(scale: Float) {
        viewModelScope.launch { repository.setLyricFontScale(scale) }
    }

    fun setShowTranslatedLyrics(show: Boolean) {
        viewModelScope.launch { repository.setShowTranslatedLyrics(show) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(statusBanner = null, errorMessage = null) }
    }

    private suspend fun renderQrBitmap(content: String, sizePx: Int = 512): ImageBitmap = withContext(Dispatchers.Default) {
        val hints = mapOf(EncodeHintType.MARGIN to 1)
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val pixels = IntArray(sizePx * sizePx)
        for (y in 0 until sizePx) {
            val offset = y * sizePx
            for (x in 0 until sizePx) {
                pixels[offset + x] = if (matrix.get(x, y)) Color.BLACK else Color.WHITE
            }
        }
        Bitmap.createBitmap(pixels, sizePx, sizePx, Bitmap.Config.ARGB_8888).asImageBitmap()
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
        qrPollJob?.cancel()
    }
}
