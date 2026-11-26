package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.ExperimentalMaterial3Api
import com.example.model.AudioQuality
import com.example.ui.components.CoverArtwork
import com.example.ui.theme.ThemeMode
import com.example.ui.viewmodel.AuthUiState
import com.example.ui.viewmodel.LoginMethodTab

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    uiState: AuthUiState,
    onServerUrlChange: (String) -> Unit,
    onSaveAndTestServer: () -> Unit,
    onSelectLoginTab: (LoginMethodTab) -> Unit,
    onCountryCodeChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onCaptchaChange: (String) -> Unit,
    onSendCaptcha: () -> Unit,
    onLoginWithSms: () -> Unit,
    onStartQrLogin: () -> Unit,
    onCookieChange: (String) -> Unit,
    onLoginWithCookie: () -> Unit,
    onLogout: () -> Unit,
    onSelectQuality: (AudioQuality) -> Unit,
    onLyricScaleChange: (Float) -> Unit,
    onShowTranslatedChange: (Boolean) -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    bottomContentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        contentPadding = PaddingValues(
            top = 12.dp,
            start = 20.dp,
            end = 20.dp,
            bottom = bottomContentPadding.calculateBottomPadding() + 28.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "账号与设置",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "连接 Mochabafey/api-NetEasyCloud 并个性化音视频偏好",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (uiState.statusBanner != null) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = uiState.statusBanner,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        if (uiState.errorMessage != null) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = uiState.errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

        // 1. Server URL configuration
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Dns, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "api-NetEasyCloud 服务端地址",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    OutlinedTextField(
                        value = uiState.serverUrlInput,
                        onValueChange = onServerUrlChange,
                        label = { Text("服务端 URL（例如 http://192.168.1.10:3000）") },
                        placeholder = { Text("https://your-netease-api.example.com") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("server_url_input")
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (uiState.settings.isServerConfigured) "当前已配置服务端" else "留空则使用公开兜底搜索与榜单",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = onSaveAndTestServer,
                            enabled = !uiState.isTestingServer,
                            modifier = Modifier.testTag("server_save_button")
                        ) {
                            if (uiState.isTestingServer) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Text("保存并测试")
                            }
                        }
                    }
                }
            }
        }

        // 2. Account Profile OR Three-Method Login Card (Phone SMS default!)
        item {
            val user = uiState.settings.user
            if (uiState.settings.isLoggedIn && user != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(18.dp)
                    ) {
                        CoverArtwork(
                            coverUrl = user.avatarUrl,
                            contentDescription = user.nickname,
                            shape = CircleShape,
                            modifier = Modifier.size(60.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = user.nickname,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = user.signature.ifBlank { "网易云音乐已连接 · UID ${user.userId}" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = onLogout,
                            modifier = Modifier.testTag("logout_button")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("退出")
                        }
                    }
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Text(
                            text = "登录网易云音乐",
                            style = MaterialTheme.typography.titleLarge
                        )

                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            LoginMethodTab.entries.forEachIndexed { index, tab ->
                                SegmentedButton(
                                    selected = uiState.selectedLoginTab == tab,
                                    onClick = { onSelectLoginTab(tab) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = LoginMethodTab.entries.size
                                    ),
                                    modifier = Modifier.testTag("login_tab_${tab.name.lowercase()}")
                                ) {
                                    Text(tab.label, maxLines = 1)
                                }
                            }
                        }

                        when (uiState.selectedLoginTab) {
                            LoginMethodTab.SMS -> {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = uiState.countryCode,
                                        onValueChange = onCountryCodeChange,
                                        label = { Text("区号") },
                                        prefix = { Text("+") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .width(96.dp)
                                            .testTag("sms_country_code_input")
                                    )
                                    OutlinedTextField(
                                        value = uiState.phoneInput,
                                        onValueChange = onPhoneChange,
                                        label = { Text("手机号码") },
                                        leadingIcon = { Icon(Icons.Rounded.PhoneAndroid, contentDescription = null) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("sms_phone_input")
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = uiState.captchaInput,
                                        onValueChange = onCaptchaChange,
                                        label = { Text("短信验证码") },
                                        leadingIcon = { Icon(Icons.Rounded.Sms, contentDescription = null) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("sms_captcha_input")
                                    )
                                    OutlinedButton(
                                        onClick = onSendCaptcha,
                                        enabled = uiState.captchaCountdown == 0 && !uiState.isSendingCaptcha,
                                        modifier = Modifier
                                            .height(54.dp)
                                            .testTag("sms_send_captcha_button")
                                    ) {
                                        when {
                                            uiState.isSendingCaptcha -> CircularProgressIndicator(
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            uiState.captchaCountdown > 0 -> Text("${uiState.captchaCountdown}s")
                                            else -> Text("获取验证码")
                                        }
                                    }
                                }

                                Button(
                                    onClick = onLoginWithSms,
                                    enabled = !uiState.isLoggingInSms,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("sms_login_submit_button")
                                ) {
                                    if (uiState.isLoggingInSms) {
                                        CircularProgressIndicator(
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else {
                                        Text("验证码登录")
                                    }
                                }
                            }

                            LoginMethodTab.QR -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (uiState.qrBitmap != null) {
                                        Box(
                                            modifier = Modifier
                                                .size(196.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(Color.White)
                                                .padding(10.dp)
                                        ) {
                                            Image(
                                                bitmap = uiState.qrBitmap,
                                                contentDescription = "网易云登录二维码",
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    } else {
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier.size(160.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Rounded.QrCode2,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(64.dp)
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = uiState.qrStatusText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Button(
                                        onClick = onStartQrLogin,
                                        enabled = !uiState.isGeneratingQr,
                                        modifier = Modifier.testTag("qr_generate_button")
                                    ) {
                                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            if (uiState.qrBitmap == null) "生成登录二维码"
                                            else if (uiState.isQrExpired) "刷新过期二维码"
                                            else "重新生成"
                                        )
                                    }
                                }
                            }

                            LoginMethodTab.COOKIE -> {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = uiState.cookieInput,
                                        onValueChange = onCookieChange,
                                        label = { Text("粘贴网易云 Cookie（含 MUSIC_U=...）") },
                                        leadingIcon = { Icon(Icons.Rounded.Key, contentDescription = null) },
                                        minLines = 3,
                                        maxLines = 5,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("cookie_input")
                                    )
                                    Button(
                                        onClick = onLoginWithCookie,
                                        enabled = !uiState.isLoggingInCookie,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("cookie_login_submit_button")
                                    ) {
                                        if (uiState.isLoggingInCookie) {
                                            CircularProgressIndicator(
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        } else {
                                            Text("验证并保存 Cookie")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Playback, Lyrics & Theme Preferences
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(18.dp)
                ) {
                    Text(
                        text = "音质与播放偏好",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "在线音频默认音质",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        androidx.compose.foundation.layout.FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AudioQuality.entries.forEach { q ->
                                FilterChip(
                                    selected = uiState.settings.audioQuality == q,
                                    onClick = { onSelectQuality(q) },
                                    label = { Text(q.label) },
                                    modifier = Modifier.testTag("quality_${q.level}")
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("显示双语翻译歌词", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "自动对齐并显示网易云外文歌曲中文翻译",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.settings.showTranslatedLyrics,
                            onCheckedChange = onShowTranslatedChange,
                            modifier = Modifier.testTag("setting_translated_lyrics_switch")
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("歌词字号缩放", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "${(uiState.settings.lyricFontScale * 100).toInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = uiState.settings.lyricFontScale,
                            onValueChange = onLyricScaleChange,
                            valueRange = 0.8f..1.4f,
                            modifier = Modifier.testTag("setting_lyric_scale_slider")
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("界面主题外观", style = MaterialTheme.typography.bodyMedium)
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            val options = listOf(
                                ThemeMode.SYSTEM to "跟随系统",
                                ThemeMode.LIGHT to "浅色",
                                ThemeMode.DARK to "深色"
                            )
                            options.forEachIndexed { idx, (mode, title) ->
                                SegmentedButton(
                                    selected = uiState.settings.themeMode == mode,
                                    onClick = { onSelectThemeMode(mode) },
                                    shape = SegmentedButtonDefaults.itemShape(idx, options.size),
                                    modifier = Modifier.testTag("theme_mode_${mode.name.lowercase()}")
                                ) {
                                    Text(title)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
