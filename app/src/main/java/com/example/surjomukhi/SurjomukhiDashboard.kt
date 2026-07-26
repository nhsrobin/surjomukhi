package com.example.surjomukhi

import android.content.Context
import android.util.Log
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SurjomukhiDashboard(viewModel: MainViewModel) {
    val isBn by viewModel.isBangla.collectAsState()
    
    // States from VM
    val myPhone by viewModel.myPhone.collectAsState()
    val myNicknameByVm by viewModel.myNickname.collectAsState()
    val myBindingCode by viewModel.myBindingCode.collectAsState()
    val partnerPhone by viewModel.partnerPhone.collectAsState()
    val partnerNicknameByVm by viewModel.partnerNickname.collectAsState()

    val myStatusKey by viewModel.myStatus.collectAsState()
    val myCustomText by viewModel.myCustomText.collectAsState()

    val partnerStatusKey by viewModel.partnerStatus.collectAsState()
    val partnerCustomText by viewModel.partnerCustomText.collectAsState()
    val partnerUpdatedAt by viewModel.partnerUpdatedAt.collectAsState()

    val pinReceivedActive by viewModel.vibrationFeedbackActive.collectAsState()
    val lastPingStatus by viewModel.lastPingStatus.collectAsState()

    val myMuteUntil by viewModel.myMuteUntil.collectAsState()
    val myMuteMessage by viewModel.myMuteMessage.collectAsState()
    val partnerMuteUntil by viewModel.partnerMuteUntil.collectAsState()
    val partnerMuteMessage by viewModel.partnerMuteMessage.collectAsState()
    val autoMuteUntil by viewModel.autoMuteUntil.collectAsState()

    val isVibeEnabled by viewModel.isVibrationEnabled.collectAsState()
    val isNotifEnabled by viewModel.isNotificationEnabled.collectAsState()
    val isBgSyncEnabled by viewModel.isBackgroundSyncEnabled.collectAsState()
    val vibeDurMs by viewModel.vibrationDurationMs.collectAsState()

    val myChirkut by viewModel.myChirkut.collectAsState()
    val partnerChirkut by viewModel.partnerChirkut.collectAsState()
    val isChirkutEnabledSetting by viewModel.isChirkutEnabled.collectAsState()

    // GitHub OTA update states
    val gitHubRepo by viewModel.gitHubRepo.collectAsState()
    val isCheckingGitHubUpdate by viewModel.isCheckingGitHubUpdate.collectAsState()
    val isGitHubUpdateAvailable by viewModel.isGitHubUpdateAvailable.collectAsState()
    val latestGitHubRelease by viewModel.latestGitHubRelease.collectAsState()
    val gitHubStatusMessage by viewModel.gitHubStatusMessage.collectAsState()
    var showGitHubRepoDialog by remember { mutableStateOf(false) }
    var inputGitHubRepoSlug by remember { mutableStateOf(gitHubRepo) }
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    var showMuteSettings by remember { mutableStateOf(false) }
    var muteDurationMinutes by remember { mutableStateOf(60) } // Default 60 mins
    var muteCustomMessageInput by remember { mutableStateOf("") }

    // Map keys to StatusType
    val myStatus = StatusType.fromKey(myStatusKey)
    val partnerStatus = StatusType.fromKey(partnerStatusKey)

    // Form states for LOGIN & SIGNUP
    var loginTabSelected by remember { mutableStateOf("phone") } // "phone" or "email"
    var isSignUpMode by remember { mutableStateOf(false) } // false = Login, true = Signup
    var inputPhone by remember { mutableStateOf("") }
    var inputPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var notificationMessage by remember { mutableStateOf<String?>(null) }

    // Form states for BINDING
    var inputBindCode by remember { mutableStateOf("") }

    // Form states for SETTINGS
    var showSettingsDialog by remember { mutableStateOf(false) }
    var dialogMyNick by remember { mutableStateOf(myNicknameByVm) }
    var dialogPartnerNick by remember { mutableStateOf(partnerNicknameByVm) }

    // Dialog trigger for custom-text status
    var showCustomTextDialog by remember { mutableStateOf(false) }
    var customTextInput by remember { mutableStateOf(myCustomText) }

    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Animations core
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_glow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val pulseOpacity by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "opacity"
    )
    val bobbingOffset by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bob"
    )

    // Root background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF09090D), // Soft cozy night atmosphere
                        Color(0xFF101018),
                        Color(0xFF0C0C12)
                    )
                )
            )
    ) {
        // High impact full-screen ripple/flash during physical vibration event success
        if (pinReceivedActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF00FF88).copy(alpha = 0.12f * pulseOpacity))
            ) {
                Text(
                    text = if (isBn) "⚡ হৃদস্পন্দন স্পর্শ অনুভব হচ্ছে... ⚡" else "⚡ Feeling the heartbeat pulse... ⚡",
                    color = Color(0xFF00FF88),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .scale(pulseScale),
                    textAlign = TextAlign.Center
                )
            }
        }

        // NAVIGATION CONTROLLER BY STATE
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.statusBarsPadding())

            // GITHUB OTA UPDATE BANNER (No USB required)
            if (isGitHubUpdateAvailable && latestGitHubRelease != null) {
                GitHubUpdateBanner(
                    isBn = isBn,
                    releaseInfo = latestGitHubRelease!!,
                    onDownloadClick = { downloadUrl ->
                        try {
                            uriHandler.openUri(downloadUrl)
                        } catch (e: Exception) {
                            notificationMessage = if (isBn) "ব্রাউজার ওপেন করা সম্ভব হয়নি" else "Could not open browser link"
                        }
                    },
                    onViewRepoClick = { repoUrl ->
                        try {
                            uriHandler.openUri(repoUrl)
                        } catch (e: Exception) {
                            notificationMessage = if (isBn) "ব্রাউজার ওপেন করা সম্ভব হয়নি" else "Could not open browser link"
                        }
                    },
                    onDismiss = {
                        viewModel.dismissGitHubUpdate()
                    }
                )
            }

            // SCENARIO 1: USER IS NOT LOGGED IN yet (Phone login)
            if (myPhone.isEmpty()) {
                // Integrated Beautiful Top Bar (scrolls with page)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Flower logo",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier
                                .size(22.dp)
                                .scale(pulseScale)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBn) "সূর্যমুখী" else "Surjomukhi",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E1E2C))
                                .border(1.dp, Color(0xFF28283B), RoundedCornerShape(12.dp))
                                .clickable { viewModel.toggleLanguage() }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isBn) "English" else "বাংলা",
                                color = Color(0xFFFFD700),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                              )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                
                // Poetic Welcome Header (Live rotating sunflower logo & customizable quotes cycler)
                PoeticWelcomeHeader(isBn = isBn, pulseScale = pulseScale, bobbingOffset = bobbingOffset)

                Spacer(modifier = Modifier.height(10.dp))

                // Credentials and Auth login box
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("login_card"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111119)),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0xFF20202F))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isSignUpMode) {
                                if (isBn) "নতুন অ্যাকাউন্ট তৈরি করুন" else "Create New Account"
                            } else {
                                if (isBn) "ব্যক্তিগত সেশনে প্রবেশ করুন" else "Sign In Securely"
                            },
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = if (isSignUpMode) {
                                if (isBn) "রিয়েল-টাইম উপস্থিতি এবং মৃদু ভাইব্রেশন স্পর্শের অভিজ্ঞতা নিতে নতুন অ্যাকাউন্ট খুলুন।" 
                                else "Create a secure account to stream presence & gentle vibe touches."
                            } else {
                                if (isBn) "রিয়েল-টাইম উপস্থিতি এবং মৃদু ভাইব্রেশন স্পর্শের অভিজ্ঞতা নিতে লগইন করুন।" 
                                else "Sign in securely via password to stream presence & gentle vibe touches."
                            },
                            color = Color.Gray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Tab selector: Phone vs Email
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp)
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF070B12))
                                .border(1.dp, Color(0xFF1E2F45), RoundedCornerShape(10.dp)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(if (loginTabSelected == "phone") Color(0xFFFFD700).copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable { loginTabSelected = "phone" }
                                    .testTag("phone_tab"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isBn) "মোবাইল নম্বর" else "Phone Number",
                                    color = if (loginTabSelected == "phone") Color(0xFFFFD700) else Color.Gray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(if (loginTabSelected == "email") Color(0xFFFFD700).copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable { loginTabSelected = "email" }
                                    .testTag("email_tab"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isBn) "ইমেইল অ্যাড্রেস" else "Email Address",
                                    color = if (loginTabSelected == "email") Color(0xFFFFD700) else Color.Gray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Form Inputs
                        OutlinedTextField(
                            value = inputPhone,
                            onValueChange = { inputPhone = it },
                            label = { 
                                Text(
                                    if (loginTabSelected == "phone") {
                                        if (isBn) "আপনার মোবাইল নম্বর" else "Your Phone Number"
                                    } else {
                                        if (isBn) "আপনার ইমেইল অ্যাড্রেস" else "Your Email Address"
                                    }
                                ) 
                            },
                            placeholder = { 
                                Text(
                                    if (loginTabSelected == "phone") "e.g. 017xxxxxxxx" else "e.g. mind@surjomukhi.com"
                                ) 
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = if (loginTabSelected == "phone") KeyboardType.Phone else KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            leadingIcon = { 
                                Icon(
                                    imageVector = if (loginTabSelected == "phone") Icons.Default.Phone else Icons.Default.Email, 
                                    contentDescription = "AuthIcon"
                                ) 
                            },
                            modifier = Modifier.fillMaxWidth().testTag("auth_target_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFFFD700),
                                unfocusedBorderColor = Color(0xFF28283B)
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Password Field
                        OutlinedTextField(
                            value = inputPassword,
                            onValueChange = { inputPassword = it },
                            label = { Text(if (isBn) "পাসওয়ার্ড" else "Password") },
                            placeholder = { Text(if (isBn) "অন্তত ৪ অক্ষরের পাসওয়ার্ড" else "At least 4 characters") },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "PasswordIcon") },
                            trailingIcon = {
                                Text(
                                    text = if (passwordVisible) (if (isBn) "লুকান" else "Hide") else (if (isBn) "দেখুন" else "Show"),
                                    color = Color(0xFFFFD700),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable { passwordVisible = !passwordVisible }
                                        .padding(8.dp)
                                )
                            },
                            modifier = Modifier.fillMaxWidth().testTag("password_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFFFD700),
                                unfocusedBorderColor = Color(0xFF28283B)
                            )
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Action Button
                        Button(
                            onClick = {
                                if (isSignUpMode) {
                                    viewModel.signUpWithPassword(
                                        target = inputPhone,
                                        password = inputPassword,
                                        isEmail = (loginTabSelected == "email")
                                    ) { success, msg ->
                                        if (success) {
                                            notificationMessage = if (isBn) "নিবন্ধন সফল হয়েছে!" else "Registered Successfully!"
                                        } else {
                                            notificationMessage = msg
                                        }
                                    }
                                } else {
                                    viewModel.loginWithPassword(
                                        target = inputPhone,
                                        password = inputPassword,
                                        isEmail = (loginTabSelected == "email")
                                    ) { success, msg ->
                                        if (success) {
                                            notificationMessage = if (isBn) "লগইন সফল!" else "Logged In Successfully!"
                                        } else {
                                            notificationMessage = msg
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("auth_submit_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSignUpMode) Color(0xFF00FF88) else Color(0xFFFFD700),
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (isSignUpMode) {
                                    if (isBn) "নিবন্ধন সম্পূর্ণ করুন ও প্রবেশ করুন ✨" else "Sign Up & Continue ✨"
                                } else {
                                    if (isBn) "প্রবেশ করুন 🔓" else "Sign In Securely 🔓"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Toggle Mode Button
                        TextButton(
                            onClick = {
                                isSignUpMode = !isSignUpMode
                                notificationMessage = null
                            },
                            modifier = Modifier.testTag("toggle_auth_mode_button")
                        ) {
                            Text(
                                text = if (isSignUpMode) {
                                    if (isBn) "ইতিমধ্যে অ্যাকাউন্ট আছে? লগইন করুন" else "Already have an account? Sign In"
                                } else {
                                    if (isBn) "নতুন অ্যাকাউন্ট তৈরি করতে চান? সাইন আপ করুন" else "Don't have an account? Sign Up"
                                },
                                color = Color(0xFFFFD700),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // SCENARIO 2: LOGGED IN, BUT NO PARTNER BOUND YET (Show pairing screen)
            else if (partnerPhone.isEmpty()) {
                // Integrated Beautiful Top Bar (scrolls with page)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Flower logo",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier
                                .size(22.dp)
                                .scale(pulseScale)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBn) "সূর্যমুখী" else "Surjomukhi",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E1E2C))
                                .border(1.dp, Color(0xFF28283B), RoundedCornerShape(12.dp))
                                .clickable { viewModel.toggleLanguage() }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isBn) "English" else "বাংলা",
                                color = Color(0xFFFFD700),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Poetic Welcome Header (Live rotating sunflower logo & customizable quotes cycler)
                PoeticWelcomeHeader(isBn = isBn, pulseScale = pulseScale, bobbingOffset = bobbingOffset)

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111119)),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0xFF28283B))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isBn) "আত্মিক বন্ধন স্থাপন করুন" else "Establish Soul Bond",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = if (isBn) "লগইন সফল! এখন দুই হৃদস্পন্দন মেলাতে আপনার সঙ্গীর সাথে কোডটি আদান-প্রদান করুন।"
                            else "Safe entry verified! Exchange 6-digit connection codes with your partner to merge live fields real-time.",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Display My unique connection Code
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF1B1B26))
                                .border(1.dp, Color(0xFF3A3A4F), RoundedCornerShape(16.dp))
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (isBn) "আপনার নিজের বন্ধন আইডি কোড" else "Your Connection ID Profile",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Bold
                               )
                                Text(
                                    text = myBindingCode,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFFFD700),
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        shadow = Shadow(
                                            color = Color(0xFFFFD700).copy(alpha = 0.4f),
                                            offset = Offset(0f, 0f),
                                            blurRadius = 14f
                                        )
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Match Partner's Code
                        OutlinedTextField(
                            value = inputBindCode,
                            onValueChange = { inputBindCode = it },
                            label = { Text(if (isBn) "প্রিয় মানুষের বন্ধন কোড লিখুন" else "Enter Partner's Connection Code") },
                            placeholder = { Text("e.g. 123456") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = "FavIcon", tint = Color.Red) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF00FF88),
                                unfocusedBorderColor = Color(0xFF28283B)
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                if (inputBindCode.length == 6) {
                                    viewModel.bindPartner(inputBindCode) { success, errorMsg ->
                                        if (!success) {
                                            notificationMessage = errorMsg
                                        }
                                    }
                                } else {
                                    notificationMessage = if (isBn) "বন্ধন কোডটি অ অবশ্যই ৬ সংখ্যার হতে হবে!" else "Binding code must be 6 digits!"
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF88), contentColor = Color.Black),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (isBn) "সংযুক্ত করুন 🔗" else "Connect Partners 🔗",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        TextButton(
                            onClick = { viewModel.logout() }
                        ) {
                            Text(
                                text = if (isBn) "← লগআউট করুন (স্থানান্তর)" else "← Logout / Change User",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // SCENARIO 3: BOTH LOGGED IN AND SUCCESSFULLY MUTUALLY BOUND (The main exquisite screen!)
            else {
                Spacer(modifier = Modifier.height(4.dp))

                // TOP BAR (with Title, Star, and Language Switcher)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Star Logo",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier
                                .size(24.dp)
                                .scale(pulseScale)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBn) "সূর্যমুখী" else "Surjomukhi",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Stylish Language Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E1E2C))
                                .border(1.dp, Color(0xFF28283B), RoundedCornerShape(12.dp))
                                .clickable { viewModel.toggleLanguage() }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isBn) "English" else "বাংলা",
                                color = Color(0xFFFFD700),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Settings Icon Button
                        IconButton(
                            onClick = {
                                dialogMyNick = myNicknameByVm
                                dialogPartnerNick = partnerNicknameByVm
                                showSettingsDialog = true
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // SUB-HEADER (Heart Connection)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Text(
                        text = if (isBn) "হৃদয়ের মেলবন্ধন" else "Heart-to-Heart Bond",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = myNicknameByVm,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Heart indicator",
                                tint = Color(0xFFEC4899),
                                modifier = Modifier
                                    .size(16.dp)
                                    .scale(pulseScale)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = partnerNicknameByVm,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEC4899)
                            )
                        }

                        // Edit and Logout action buttons
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    dialogMyNick = myNicknameByVm
                                    dialogPartnerNick = partnerNicknameByVm
                                    showSettingsDialog = true
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit profiles",
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = { viewModel.logout() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ExitToApp,
                                    contentDescription = "Logout",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // MAIN HERO STATUS DISPLAY CARD (Reflecting the active status canvas and character vibe)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .testTag("status_display_section"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F14)),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(
                        width = 2.dp,
                        color = partnerStatus.color
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Header indication showing partner's name
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(partnerStatus.color)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBn) "\"$partnerNicknameByVm\" এর বর্তমান উপস্থিতি"
                                else "\"$partnerNicknameByVm\"'s Current Presence",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                        }

                        // STATUS REPRESENTED 3D BOBBING FLOATING AVATAR WITH CANVAS RIPPLES
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .padding(bottom = 2.dp)
                                .offset(y = bobbingOffset.dp)
                                .scale(pulseScale),
                            contentAlignment = Alignment.Center
                        ) {
                            // Ambient radial layout background glow
                            Box(
                                modifier = Modifier
                                    .size(75.dp)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                partnerStatus.color.copy(alpha = 0.2f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )

                            // Load specialized high-impact canvas effects dynamically
                            StatusAvatarAnimation(
                                status = partnerStatus,
                                pulseScale = 0.8f,
                                opacityMultiplier = pulseOpacity
                            )

                            // Big floating character face expression emoji matching status
                            Text(
                                text = when (partnerStatus) {
                                    StatusType.FREE -> "😎"
                                    StatusType.STUDYING -> "🧠"
                                    StatusType.SLEEPING -> "😴"
                                    StatusType.SCROLLING -> "📱"
                                    StatusType.TALKING -> "📞"
                                    StatusType.HOME -> "🏠"
                                    StatusType.OFFLINE -> "📡"
                                    StatusType.BUSY -> "🔥"
                                    StatusType.CUSTOM -> "🔮"
                                },
                                fontSize = 48.sp,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Dynamic status badge name
                        val cleanStatusName = when (partnerStatus) {
                            StatusType.FREE -> if (isBn) "ফ্রি" else "Free"
                            StatusType.STUDYING -> if (isBn) "পড়ছি" else "Studying"
                            StatusType.SLEEPING -> if (isBn) "ঘুমাচ্ছি" else "Sleeping"
                            StatusType.SCROLLING -> if (isBn) "স্ক্রলিং" else "Scrolling"
                            StatusType.TALKING -> if (isBn) "কথা বলছি" else "On Call"
                            StatusType.HOME -> if (isBn) "বাসায়" else "At Home"
                            StatusType.OFFLINE -> if (isBn) "নেটের বাইরে" else "Offline"
                            StatusType.BUSY -> if (isBn) "ব্যস্ত" else "Busy"
                            StatusType.CUSTOM -> if (isBn) "কাস্টম" else "Custom"
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = cleanStatusName,
                                color = partnerStatus.color,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    shadow = Shadow(
                                        color = partnerStatus.color.copy(alpha = 0.4f),
                                        offset = Offset(0f, 0f),
                                        blurRadius = 12f
                                    )
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // Large glossy ball matching status color
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                Color.White,
                                                partnerStatus.color,
                                                partnerStatus.color.copy(alpha = 0.8f)
                                            ),
                                            center = Offset(6f, 6f)
                                        )
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                            )
                        }

                        // Custom subtext or description
                        if (partnerStatus == StatusType.CUSTOM && partnerCustomText.isNotEmpty()) {
                            Text(
                                text = "\"$partnerCustomText\"",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 4.dp),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            val statusDesc = if (isBn) partnerStatus.subtextBn else partnerStatus.subtextEn
                            Text(
                                text = statusDesc,
                                color = Color.Gray,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 3.dp),
                                textAlign = TextAlign.Center
                            )
                        }

                        // Last updated time diff ticker
                        val diffSeconds = (System.currentTimeMillis() - partnerUpdatedAt) / 1000
                        val lastUpdateText = when {
                            diffSeconds < 6 -> if (isBn) "এইমাত্র" else "Just now"
                            diffSeconds < 60 -> if (isBn) "$diffSeconds সেকেন্ড আগে" else "$diffSeconds seconds ago"
                            else -> if (isBn) "${diffSeconds / 60} মিনিট আগে" else "${diffSeconds / 60} minutes ago"
                        }
                        Text(
                            text = if (isBn) "সর্বশেষ আপডেট: $lastUpdateText" else "Updated: $lastUpdateText",
                            fontSize = 10.sp,
                            color = Color.DarkGray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // USER'S SELF-STATUS CONTROLLER LAYOUT
                Text(
                    text = if (isBn) "আপনার বর্তমান অনুভূতি নির্ধারণ করুন:" else "Select your active feeling:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // 2-LINE STANDARD STATUS SELECTION GRID BOX (4 columns, 2 rows)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF111119))
                        .border(1.dp, Color(0xFF20202F), RoundedCornerShape(20.dp))
                        .padding(8.dp)
                        .testTag("status_grid_box"),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val standardStatuses = listOf(
                        StatusType.FREE, StatusType.STUDYING, StatusType.SLEEPING, StatusType.SCROLLING,
                        StatusType.TALKING, StatusType.HOME, StatusType.OFFLINE, StatusType.BUSY
                    )
                    val cols = 4
                    val rowsCount = 2

                    for (r in 0 until rowsCount) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (c in 0 until cols) {
                                val idx = r * cols + c
                                if (idx < standardStatuses.size) {
                                    val status = standardStatuses[idx]
                                    val isSelected = (myStatusKey == status.key)
                                    val emojiIcon = when (status) {
                                        StatusType.FREE -> "😎"
                                        StatusType.STUDYING -> "📚"
                                        StatusType.SLEEPING -> "💤"
                                        StatusType.SCROLLING -> "📱"
                                        StatusType.TALKING -> "📞"
                                        StatusType.HOME -> "🏠"
                                        StatusType.OFFLINE -> "📡"
                                        StatusType.BUSY -> "🔴"
                                        StatusType.CUSTOM -> "🔮"
                                    }
                                    val displayName = when (status) {
                                        StatusType.FREE -> if (isBn) "ফ্রি" else "Free"
                                        StatusType.STUDYING -> if (isBn) "পড়ছি" else "Study"
                                        StatusType.SLEEPING -> if (isBn) "ঘুমাচ্ছি" else "Sleep"
                                        StatusType.SCROLLING -> if (isBn) "স্ক্রলিং" else "Scroll"
                                        StatusType.TALKING -> if (isBn) "কথা" else "Talk"
                                        StatusType.HOME -> if (isBn) "বাসায়" else "Home"
                                        StatusType.OFFLINE -> if (isBn) "অফলাইন" else "Offline"
                                        StatusType.BUSY -> if (isBn) "ব্যস্ত" else "Busy"
                                        StatusType.CUSTOM -> if (isBn) "কাস্টম" else "Custom"
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(50.dp))
                                            .background(
                                                if (isSelected) status.color.copy(alpha = 0.15f)
                                                else Color(0xFF161622)
                                            )
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) status.color else Color(0xFF28283B),
                                                shape = RoundedCornerShape(50.dp)
                                            )
                                            .clickable {
                                                viewModel.setUserStatus(status)
                                            }
                                            .padding(horizontal = 4.dp, vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            // Small status indicator dot
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(status.color)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "$displayName $emojiIcon",
                                                color = if (isSelected) Color.White else Color.Gray,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // LINE 3: STYLISH INLINE CUSTOM STATUS INPUT BAR
                var customTextInputField by remember { mutableStateOf(myCustomText) }
                
                LaunchedEffect(myCustomText) {
                    if (myCustomText != customTextInputField) {
                        customTextInputField = myCustomText
                    }
                }
                
                val isCustomSelected = (myStatusKey == StatusType.CUSTOM.key)
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Text(
                        text = if (isBn) "কাস্টম অনুভূতি স্ট্যাটাস:" else "Custom Status Message:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF161622))
                            .border(
                                width = if (isCustomSelected) 2.dp else 1.dp,
                                color = if (isCustomSelected) StatusType.CUSTOM.color else Color(0xFF28283B),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔮",
                            fontSize = 18.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        
                        androidx.compose.foundation.text.BasicTextField(
                            value = customTextInputField,
                            onValueChange = { customTextInputField = it },
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 8.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    keyboardController?.hide()
                                    viewModel.setUserStatus(StatusType.CUSTOM, customTextInputField)
                                }
                            ),
                            decorationBox = { innerTextField ->
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    if (customTextInputField.isEmpty()) {
                                        Text(
                                            text = if (isBn) "নিজের মনের অনুভূতি লিখে সেট করুন..." else "Write custom feeling details...",
                                            color = Color.Gray,
                                            fontSize = 14.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (customTextInputField.trim().isNotEmpty()) StatusType.CUSTOM.color
                                    else Color(0xFF2D1F3D)
                                )
                                .clickable(enabled = customTextInputField.trim().isNotEmpty()) {
                                    keyboardController?.hide()
                                    viewModel.setUserStatus(StatusType.CUSTOM, customTextInputField)
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isBn) "সেট" else "Set",
                                color = if (customTextInputField.trim().isNotEmpty()) Color.White else Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // ONE-CLICK COZY VIBE PULSE CARD (TAPPING TRIGGERS THE RECEIVER'S DEVICE PHYSICAL VIBRATOR)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1322)),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFF3B234F))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isBn) "অনুভূতির স্পর্শ (Heart Vibe)" else "Warm Connection Touch",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isBn) "\"$partnerNicknameByVm\" যদি এখন ফ্রি থাকে, তবে তার ফোন কেঁপে উঠবে"
                                    else "Trigger a sweet heartbeat sync pulse on \"$partnerNicknameByVm\"'s phone instantly",
                                    fontSize = 11.sp,
                                    color = Color.LightGray,
                                    lineHeight = 15.sp
                                )
                            }

                            // Vibe Heart Pulse trigger Button
                            val isPartnerMuted = partnerMuteUntil > System.currentTimeMillis()
                            val isAutoMuted = autoMuteUntil > System.currentTimeMillis()
                            val isVibeBlocked = isPartnerMuted || isAutoMuted
                            
                            IconButton(
                                onClick = { viewModel.sendPingNotification() },
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isVibeBlocked) Color(0xFFEF4444).copy(alpha = 0.15f)
                                        else if (partnerStatus == StatusType.FREE) Color(0xFF00FF88) 
                                        else Color(0xFFEF4444).copy(alpha = 0.2f)
                                    )
                                    .border(
                                        1.dp, 
                                        if (isVibeBlocked) Color(0xFFEF4444).copy(alpha = 0.4f)
                                        else if (partnerStatus == StatusType.FREE) Color(0xFF00FF88) 
                                        else Color(0xFFEF4444), 
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Pulse Ping notification",
                                    tint = if (isVibeBlocked) Color(0xFFEF4444)
                                           else if (partnerStatus == StatusType.FREE) Color.Black 
                                           else Color(0xFFEF4444),
                                    modifier = Modifier
                                        .size(20.dp)
                                        .scale(if (partnerStatus == StatusType.FREE && !isVibeBlocked) pulseScale else 1.0f)
                                )
                            }
                        }

                        // Status banners and indicators next to/below the button
                        val now = System.currentTimeMillis()
                        
                        // 1. If Partner has muted us
                        if (partnerMuteUntil > now) {
                            val timeLeftMins = ((partnerMuteUntil - now) / 60000).coerceAtLeast(1)
                            val partnerMsg = partnerMuteMessage.ifBlank { if (isBn) "কোনো বার্তা নেই" else "No message" }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFEF4444).copy(alpha = 0.1f))
                                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🚫 ",
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = if (isBn) "\"$partnerNicknameByVm\" ভাইব্রেশন মিউট করেছেন আরও $timeLeftMins মিনিটের জন্য।\nবার্তা: \"$partnerMsg\""
                                    else "\"$partnerNicknameByVm\" has muted vibrations for $timeLeftMins more mins.\nNote: \"$partnerMsg\"",
                                    fontSize = 11.sp,
                                    color = Color(0xFFFF8888),
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        // 2. If we are auto-muted (tana 3 times check)
                        if (autoMuteUntil > now) {
                            val timeLeftMins = ((autoMuteUntil - now) / 60000).coerceAtLeast(1)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFF9800).copy(alpha = 0.1f))
                                    .border(1.dp, Color(0xFFFF9800).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⚠️ ",
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = if (isBn) "অটো-মিউট সচল: ৩ বার ভাইব্রেশনের সাড়া না পাওয়ায় সিঙ্ক লক করা হয়েছে আরও $timeLeftMins মিনিটের জন্য।"
                                    else "Auto-Mute Active: Locked for $timeLeftMins more mins due to consecutive no-response.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFFFB74D),
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        // 3. If we muted ourselves
                        if (myMuteUntil > now) {
                            val timeLeftMins = ((myMuteUntil - now) / 60000).coerceAtLeast(1)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF9C27B0).copy(alpha = 0.1f))
                                    .border(1.dp, Color(0xFF9C27B0).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🔇 ",
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = if (isBn) "আপনি ভাইব্রেশন মিউট করেছেন আরও $timeLeftMins মিনিটের জন্য।"
                                        else "You have muted vibrations for $timeLeftMins more mins.",
                                        fontSize = 11.sp,
                                        color = Color(0xFFE040FB),
                                        lineHeight = 15.sp
                                    )
                                }
                                Text(
                                    text = if (isBn) "আনমিউট" else "Unmute",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF9C27B0))
                                        .clickable { viewModel.updateMuteStatus(0, "") }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Collapsible Mute Setup Pane
                        if (showMuteSettings) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = Color(0xFF3B234F).copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = if (isBn) "ভাইব্রেশন মিউটের সময়কাল নির্ধারণ করুন:" else "Select Mute Duration:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Duration row selection
                            val durations = listOf(
                                15 to (if (isBn) "১৫ মি." else "15m"),
                                30 to (if (isBn) "৩০ মি." else "30m"),
                                60 to (if (isBn) "১ ঘণ্টা" else "1h"),
                                480 to (if (isBn) "৮ ঘণ্টা" else "8h"),
                                1440 to (if (isBn) "২৪ ঘণ্টা" else "24h")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                durations.forEach { (mins, label) ->
                                    val isDurSelected = (muteDurationMinutes == mins)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isDurSelected) Color(0xFF9C27B0) else Color(0xFF251A30))
                                            .border(1.dp, if (isDurSelected) Color.White else Color(0xFF3B234F), RoundedCornerShape(8.dp))
                                            .clickable { muteDurationMinutes = mins }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            color = Color.White,
                                            fontWeight = if (isDurSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = if (isBn) "পার্টনারের জন্য একটি ছোট বার্তা (ঐচ্ছিক):" else "Message for Partner (optional):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Custom message text field
                            OutlinedTextField(
                                value = muteCustomMessageInput,
                                onValueChange = { muteCustomMessageInput = it },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp),
                                placeholder = {
                                    Text(
                                        text = if (isBn) "উদা. ক্লাসে আছি, ঘুমাচ্ছি..." else "e.g., Sleeping, In a class...",
                                        fontSize = 13.sp,
                                        color = Color.Gray
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF9C27B0),
                                    unfocusedBorderColor = Color(0xFF3B234F),
                                    focusedContainerColor = Color(0xFF160F1E),
                                    unfocusedContainerColor = Color(0xFF160F1E)
                                ),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { showMuteSettings = false }) {
                                    Text(text = if (isBn) "বাতিল" else "Cancel", color = Color.Gray, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        viewModel.updateMuteStatus(muteDurationMinutes, muteCustomMessageInput)
                                        showMuteSettings = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = if (isBn) "মিউট নিশ্চিত করুন" else "Confirm Mute",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Open Settings Toggle Button
                        if (!showMuteSettings) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showMuteSettings = true }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Mute config icon",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isBn) "মিউট ও কাস্টম বার্তা নির্ধারণ করুন" else "Configure Mute & Status Message",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // CHIRKUT (চিরকুট) SEND PANEL
                var chirkutInputText by remember { mutableStateOf("") }
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .testTag("chirkut_send_section"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0C141F)), // Dark slate blue-gray
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E2F45))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Text(
                                text = "✉️",
                                fontSize = 20.sp,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            Text(
                                text = if (isBn) "প্রিয়জনকে চিরকুট পাঠান" else "Send a Chirkut Note",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        
                        Text(
                            text = if (isBn) "এখানে কোনো বার্তা লিখে সেন্ড করলে তা আপনার পার্টনারের ডিভাইসের সম্পূর্ণ হোমস্ক্রিন উইজেট জুড়ে প্রদর্শিত হবে।" 
                            else "Sending a note here will temporarily replace your partner's widget with your message.",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        // If there is an active sent chirkut by me
                        if (myChirkut.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1E293B))
                                    .border(BorderStroke(0.5.dp, Color(0xFF334155)), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Column {
                                    Text(
                                        text = if (isBn) "আপনার পাঠানো সক্রিয় চিরকুট:" else "Your currently active Chirkut:",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF38BDF8)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "\"$myChirkut\"",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Text input field
                        OutlinedTextField(
                            value = chirkutInputText,
                            onValueChange = { if (it.length <= 45) chirkutInputText = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { 
                                Text(
                                    text = if (isBn) "যেমন: তোমাকে খুব মিস করছি..." else "e.g. Missing you so much...",
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                ) 
                            },
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF0EA5E9),
                                unfocusedBorderColor = Color(0xFF1E293B),
                                focusedContainerColor = Color(0xFF0B131E),
                                unfocusedContainerColor = Color(0xFF0B131E)
                            ),
                            singleLine = true,
                            supportingText = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (isBn) "সর্বোচ্চ ৪৫ অক্ষর" else "Max 45 chars",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = "${chirkutInputText.length}/45",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left side: setting toggle inside card
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { viewModel.toggleChirkut(!isChirkutEnabledSetting) }
                            ) {
                                Switch(
                                    checked = isChirkutEnabledSetting,
                                    onCheckedChange = { viewModel.toggleChirkut(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF0EA5E9),
                                        checkedTrackColor = Color(0xFF0284C7).copy(alpha = 0.4f),
                                        uncheckedThumbColor = Color.Gray,
                                        uncheckedTrackColor = Color(0xFF1E293B)
                                    ),
                                    modifier = Modifier.scale(0.7f)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = if (isBn) "উইজেটে চিরকুট দেখান" else "Show on Widget",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isChirkutEnabledSetting) Color.White else Color.Gray
                                )
                            }

                            // Right side buttons
                            Row {
                                if (myChirkut.isNotEmpty()) {
                                    TextButton(
                                        onClick = {
                                            viewModel.sendChirkut("")
                                            chirkutInputText = ""
                                        },
                                        modifier = Modifier.padding(end = 4.dp)
                                    ) {
                                        Text(
                                            text = if (isBn) "মুছুন" else "Clear",
                                            color = Color(0xFFEF4444),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (chirkutInputText.trim().isNotEmpty()) {
                                            viewModel.sendChirkut(chirkutInputText.trim())
                                            chirkutInputText = ""
                                            keyboardController?.hide()
                                        }
                                    },
                                    enabled = chirkutInputText.trim().isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF0EA5E9),
                                        disabledContainerColor = Color(0xFF1E293B)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = if (isBn) "পাঠান 🚀" else "Send 🚀",
                                        color = if (chirkutInputText.trim().isNotEmpty()) Color.White else Color.Gray,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // ACTIVITY REAL-TIME LOG TRACE
                var isLogVisible by remember { mutableStateOf(false) }
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isLogVisible = !isLogVisible }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isLogVisible) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = "log drop",
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isBn) "অনুভূতির জীবন্ত নথি (${viewModel.activityLogs.size})" else "Aura Synchronizations (${viewModel.activityLogs.size})",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    AnimatedVisibility(visible = isLogVisible) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .padding(vertical = 4.dp),
                                color = Color(0xFF0A0A0F),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF14141F))
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    if (viewModel.activityLogs.isEmpty()) {
                                        Text(
                                            text = if (isBn) "এখনও কোনো নতুন স্পন্দন নেই" else "No synced logs recorded yet.",
                                            fontSize = 11.sp,
                                            color = Color.DarkGray,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                                        )
                                    } else {
                                        LazyColumnForLogs(logs = viewModel.activityLogs)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            val exportContext = androidx.compose.ui.platform.LocalContext.current
                            Button(
                                onClick = {
                                    val logsText = viewModel.activityLogs.joinToString("\n")
                                    if (logsText.trim().isEmpty()) {
                                        notificationMessage = if (isBn) "কোনো লগ হিস্ট্রি পাওয়া যায়নি!" else "No logs available to export!"
                                    } else {
                                        // Copy to clipboard
                                        val clipboard = exportContext.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("Surjomukhi Logs", logsText)
                                        clipboard.setPrimaryClip(clip)

                                        // Open Share Intent
                                        try {
                                            val shareIntent = android.content.Intent().apply {
                                                action = android.content.Intent.ACTION_SEND
                                                putExtra(android.content.Intent.EXTRA_TEXT, logsText)
                                                type = "text/plain"
                                            }
                                            exportContext.startActivity(android.content.Intent.createChooser(shareIntent, "Export Logs"))
                                        } catch (e: Exception) {
                                            Log.e("SurjomukhiLogs", "Share failed", e)
                                        }

                                        notificationMessage = if (isBn) "লগ ক্লিপবোর্ডে কপি হয়েছে এবং শেয়ার অপশন ওপেন হয়েছে!" else "Logs copied successfully & share chooser opened!"
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF181824),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF28283B))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Export icon",
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isBn) "নথিপত্র এক্সপোর্ট করুন" else "Export Log History",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // FLOATING ACTION NOTIFICATION BAR (FOR TOAST MESSAGES)
        AnimatedVisibility(
            visible = notificationMessage != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF28283D)),
                border = BorderStroke(1.dp, Color(0xFFFFD700)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = notificationMessage ?: "",
                        fontSize = 13.sp,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (isBn) "ঠিক আছে" else "OK",
                        color = Color(0xFFFFD700),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { notificationMessage = null }
                            .padding(start = 12.dp)
                    )
                }
            }
        }
    }

    // FIRST-TIME ENTRY NICKNAME POPUP DIALOG
    if (myPhone.isNotEmpty() && myNicknameByVm == "NICKNAME_NOT_SET") {
        var nicknameInput by remember { mutableStateOf("") }
        var nicknameError by remember { mutableStateOf<String?>(null) }
        
        Dialog(
            onDismissRequest = { /* Do nothing to prevent dismissal of mandatory nickname */ },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("nickname_setup_dialog"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF151522)),
                border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "User Name Icon",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(48.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = if (isBn) "আপনার মিষ্টি নাম লিখুন" else "Enter Your Nickname",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = if (isBn) "সূর্যমুখীতে স্বাগতম! অনুগ্রহ করে আপনার অ্যাকাউন্টের জন্য একটি মিষ্টি নাম বা ডাকনাম সেট করুন।"
                               else "Welcome to Surjomukhi! Please set a sweet name or nickname for your profile.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    OutlinedTextField(
                        value = nicknameInput,
                        onValueChange = { 
                            nicknameInput = it
                            if (it.trim().isNotEmpty()) nicknameError = null
                        },
                        label = { Text(if (isBn) "আপনার নিকনেম" else "Nickname") },
                        placeholder = { Text(if (isBn) "যেমন: আমার হৃদয়" else "e.g. Dreamer") },
                        singleLine = true,
                        isError = nicknameError != null,
                        modifier = Modifier.fillMaxWidth().testTag("nickname_setup_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFFD700),
                            unfocusedBorderColor = Color(0xFF28283B)
                        )
                    )
                    
                    if (nicknameError != null) {
                        Text(
                            text = nicknameError!!,
                            color = Color.Red,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .align(Alignment.Start)
                                .padding(top = 4.dp, start = 4.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            val trimmed = nicknameInput.trim()
                            if (trimmed.isEmpty()) {
                                nicknameError = if (isBn) "নিকনেম খালি হতে পারবে না!" else "Nickname cannot be empty!"
                            } else {
                                viewModel.updateMyNickname(trimmed)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("nickname_setup_save_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isBn) "সংরক্ষণ করুন ও প্রবেশ করুন 🌟" else "Save & Continue 🌟",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }

    // DIALOG 1: COMPREHENSIVE SETTINGS & PRIVACY DIALOG
    if (showSettingsDialog) {
        val settingsContext = androidx.compose.ui.platform.LocalContext.current
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⚙️",
                        fontSize = 24.sp
                    )
                    Column {
                        Text(
                            text = if (isBn) "সূর্যমুখী সেটিংস" else "Surjomukhi Settings",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = if (isBn) "কাস্টমাইজেশন ও ব্যাকগ্রাউন্ড সিঙ্ক সেটিংস" else "Customization & background engine status",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // SECTION 1: PROFILE NICKNAMES (আমার ও প্রিয়জনের নাম)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C141F)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF1E2F45))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("👤", fontSize = 16.sp)
                                Text(
                                    text = if (isBn) "নাম ও পরিচয় কাস্টমাইজেশন" else "Nickname Customization",
                                    color = Color(0xFFFFD700),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedTextField(
                                value = dialogMyNick,
                                onValueChange = { dialogMyNick = it },
                                label = { Text(if (isBn) "আমার ডাকনাম" else "My Nickname") },
                                singleLine = true,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "User",
                                        tint = Color(0xFFFFD700).copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFFFFD700),
                                    unfocusedBorderColor = Color(0xFF1E2F45),
                                    focusedContainerColor = Color(0xFF070B12),
                                    unfocusedContainerColor = Color(0xFF070B12)
                                )
                            )

                            OutlinedTextField(
                                value = dialogPartnerNick,
                                onValueChange = { dialogPartnerNick = it },
                                label = { Text(if (isBn) "প্রিয় মানুষের ডাকনাম" else "Partner's Nickname") },
                                singleLine = true,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = "Partner",
                                        tint = Color(0xFFEC4899).copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFFFFD700),
                                    unfocusedBorderColor = Color(0xFF1E2F45),
                                    focusedContainerColor = Color(0xFF070B12),
                                    unfocusedContainerColor = Color(0xFF070B12)
                                )
                            )
                        }
                    }

                    // SECTION 2: LANGUAGE & TRANSLATION (ভাষা নির্বাচন)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C141F)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF1E2F45))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Text("🌐", fontSize = 16.sp)
                                Text(
                                    text = if (isBn) "ভাষা ও মাধ্যম / Language" else "Language Preference",
                                    color = Color(0xFFFFD700),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF070B12))
                                    .border(1.dp, Color(0xFF1E2F45), RoundedCornerShape(10.dp)),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .background(if (isBn) Color(0xFFFFD700).copy(alpha = 0.15f) else Color.Transparent)
                                        .clickable { if (!isBn) viewModel.toggleLanguage() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "বাংলা",
                                        color = if (isBn) Color(0xFFFFD700) else Color.Gray,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .background(if (!isBn) Color(0xFFFFD700).copy(alpha = 0.15f) else Color.Transparent)
                                        .clickable { if (isBn) viewModel.toggleLanguage() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "English",
                                        color = if (!isBn) Color(0xFFFFD700) else Color.Gray,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // SECTION 3: VIBRATION, NOTIFICATIONS & WIDGET (ফিডব্যাক ও নোটিফিকেশন)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C141F)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF1E2F45))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                Text("📳", fontSize = 16.sp)
                                Text(
                                    text = if (isBn) "স্পর্শ ফিডব্যাক ও নোটিফিকেশন" else "Tactile Feedback & Alerts",
                                    color = Color(0xFFFFD700),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Vibration Switch Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isBn) "মন ছুঁয়ে যাওয়া ভাইব্রেশন" else "Enable Touch Vibration",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (isBn) "প্রিয়জন স্পর্শ পাঠালে সাথে সাথে ফোন কেঁপে উঠবে" else "Vibrate phone instantly when partner pings",
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }
                                Switch(
                                    checked = isVibeEnabled,
                                    onCheckedChange = { viewModel.setVibrationEnabled(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFFFFD700),
                                        checkedTrackColor = Color(0xFFFFD700).copy(alpha = 0.3f),
                                        uncheckedThumbColor = Color.Gray,
                                        uncheckedTrackColor = Color(0xFF070B12)
                                    ),
                                    modifier = Modifier.scale(0.85f)
                                )
                            }

                            // Vibration Duration Selector
                            if (isVibeEnabled) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isBn) "কম্পন স্পর্শের সময়কাল:" else "Vibration Duration:",
                                    fontSize = 10.sp,
                                    color = Color.LightGray
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        200L to (if (isBn) "সংক্ষিপ্ত" else "Short"),
                                        450L to (if (isBn) "মধ্যম" else "Medium"),
                                        800L to (if (isBn) "দীর্ঘ" else "Long")
                                    ).forEach { (ms, label) ->
                                        val isSelected = vibeDurMs == ms
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) Color(0xFFFFD700).copy(alpha = 0.2f) else Color(0xFF070B12))
                                                .border(1.dp, if (isSelected) Color(0xFFFFD700) else Color(0xFF1E2F45), RoundedCornerShape(8.dp))
                                                .clickable { viewModel.setVibrationDuration(ms) }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                color = if (isSelected) Color(0xFFFFD700) else Color.Gray,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                            }

                            Divider(color = Color(0xFF1E2F45), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))

                            // Notification Switch Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isBn) "অনুভূতি পরিবর্তনের নোটিফিকেশন" else "Allow Notifications",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (isBn) "সঙ্গী স্ট্যাটাস পরিবর্তন করলে নোটিফিকেশন পাবেন" else "Get system notification on partner changes",
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }
                                Switch(
                                    checked = isNotifEnabled,
                                    onCheckedChange = { viewModel.setNotificationEnabled(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFFFFD700),
                                        checkedTrackColor = Color(0xFFFFD700).copy(alpha = 0.3f),
                                        uncheckedThumbColor = Color.Gray,
                                        uncheckedTrackColor = Color(0xFF070B12)
                                    ),
                                    modifier = Modifier.scale(0.85f)
                                )
                            }

                            Divider(color = Color(0xFF1E2F45), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))

                            // Chirkut Widget Switch Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isBn) "উইজেটে চিরকুট প্রদর্শন" else "Show Chirkut on Widget",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (isBn) "পার্টনারের পাঠানো চিরকুট উইজেট স্ক্রিনে সচল রাখা" else "If enabled, partner's custom notes cover widget",
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }
                                Switch(
                                    checked = isChirkutEnabledSetting,
                                    onCheckedChange = { viewModel.toggleChirkut(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFFFFD700),
                                        checkedTrackColor = Color(0xFFFFD700).copy(alpha = 0.3f),
                                        uncheckedThumbColor = Color.Gray,
                                        uncheckedTrackColor = Color(0xFF070B12)
                                    ),
                                    modifier = Modifier.scale(0.85f)
                                )
                            }
                        }
                    }

                    // SECTION 4: BACKGROUND SYNC & BATTERY (ব্যাকগ্রাউন্ড সিঙ্ক ও ব্যাটারি)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C141F)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF1E2F45))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("🔄", fontSize = 16.sp)
                                Text(
                                    text = if (isBn) "ব্যাকগ্রাউন্ড ইঞ্জিন ও সিঙ্ক সেটিংস" else "Background Engine & Sync",
                                    color = Color(0xFFFFD700),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isBn) "অলটাইম ব্যাকগ্রাউন্ড সিঙ্ক" else "Continuous Background Sync",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (isBgSyncEnabled) {
                                            if (isBn) "সক্রিয়: ব্যাকগ্রাউন্ডে সবসময় লাইভ কানেকশন থাকবে।" else "Active: Live updates running continuously in background."
                                        } else {
                                            if (isBn) "নিষ্ক্রিয়: শুধু অ্যাপ খোলা থাকলেই চলবে (ব্যাটারি সাশ্রয়ী)।" else "Inactive: Syncs only when app is open (Power Saver)."
                                        },
                                        color = if (isBgSyncEnabled) Color(0xFF00FF88) else Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }
                                Switch(
                                    checked = isBgSyncEnabled,
                                    onCheckedChange = { viewModel.setBackgroundSyncEnabled(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFFFFD700),
                                        checkedTrackColor = Color(0xFFFFD700).copy(alpha = 0.3f),
                                        uncheckedThumbColor = Color.Gray,
                                        uncheckedTrackColor = Color(0xFF070B12)
                                    ),
                                    modifier = Modifier.scale(0.85f)
                                )
                            }
                        }
                    }

                    // SECTION 5: DEVICE PERMISSIONS & SYSTEM DIAGNOSTICS (ডিভাইস পারমিশন)
                    val isBatteryOptimized = remember(showSettingsDialog) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val powerManager = settingsContext.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                            powerManager.isIgnoringBatteryOptimizations(settingsContext.packageName)
                        } else {
                            true
                        }
                    }

                    val isNotifPermitted = remember(showSettingsDialog) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            androidx.core.content.ContextCompat.checkSelfPermission(
                                settingsContext,
                                android.Manifest.permission.POST_NOTIFICATIONS
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        } else {
                            true
                        }
                    }

                    val isExactAlarmPermitted = remember(showSettingsDialog) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val alarmManager = settingsContext.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                            alarmManager.canScheduleExactAlarms()
                        } else {
                            true
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C141F)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF1E2F45))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("🛡️", fontSize = 16.sp)
                                Text(
                                    text = if (isBn) "সিস্টেম ডায়াগনস্টিক ও অনুমতি" else "System Permissions & Diagnostics",
                                    color = Color(0xFFFFD700),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF070B12))
                                    .border(1.dp, Color(0xFF1E2F45), RoundedCornerShape(12.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = if (isBn) "নোটিফিকেশন অনুমতি:" else "Notification Status:", color = Color.Gray, fontSize = 11.sp)
                                    Text(
                                        text = if (isNotifPermitted) (if (isBn) "✅ সচল" else "✅ Active") else (if (isBn) "❌ নিষ্ক্রিয়" else "❌ Inactive"),
                                        color = if (isNotifPermitted) Color(0xFF00FF88) else Color(0xFFEF4444),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = if (isBn) "ব্যাটারি অপ্টিমাইজেশন নিষ্ক্রিয়:" else "Battery Optimizer Exclusion:", color = Color.Gray, fontSize = 11.sp)
                                    Text(
                                        text = if (isBatteryOptimized) (if (isBn) "✅ অনুমোদিত (অলওয়েজ অন)" else "✅ Ignored (Always On)") else (if (isBn) "⚠️ অপ্টিমাইজড (সীমিত)" else "⚠️ Optimized (Limited)"),
                                        color = if (isBatteryOptimized) Color(0xFF00FF88) else Color(0xFFFFD700),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = if (isBn) "রিয়েল-টাইম সিঙ্ক ক্ষমতা:" else "Real-time Alarm Status:", color = Color.Gray, fontSize = 11.sp)
                                    Text(
                                        text = if (isExactAlarmPermitted) (if (isBn) "✅ অনুমোদিত" else "✅ Granted") else (if (isBn) "⚠️ সীমাবদ্ধ" else "⚠️ Limited"),
                                        color = if (isExactAlarmPermitted) Color(0xFF00FF88) else Color(0xFFFFD700),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = {
                                        try {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                val intent = android.content.Intent().apply {
                                                    action = android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                                                    data = android.net.Uri.parse("package:${settingsContext.packageName}")
                                                }
                                                settingsContext.startActivity(intent)
                                            } else {
                                                notificationMessage = if (isBn) "আপনার ডিভাইসে এই সেটিংস প্রয়োজন নেই!" else "Feature not needed on this device version!"
                                            }
                                        } catch (e: Exception) {
                                            try {
                                                val intent = android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
                                                settingsContext.startActivity(intent)
                                            } catch (err: Exception) {
                                                notificationMessage = if (isBn) "সিস্টেম সেটিংস ওপেন করা সম্ভব হয়নি!" else "Could not open system settings!"
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF14221A), contentColor = Color(0xFF88FF88)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(text = if (isBn) "ব্যাটারি অপ্টিমাইজ" else "Ignore Battery", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        try {
                                            val intent = android.content.Intent().apply {
                                                action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                                data = android.net.Uri.fromParts("package", settingsContext.packageName, null)
                                            }
                                            settingsContext.startActivity(intent)
                                        } catch (e: Exception) {
                                            notificationMessage = if (isBn) "সিস্টেম সেটিংস ওপেন করা সম্ভব হয়নি!" else "Could not open system settings!"
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E2C), contentColor = Color(0xFFFFD700)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(text = if (isBn) "অ্যাপ ইনফো সেটিংস" else "App System Info", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !isExactAlarmPermitted) {
                                Button(
                                    onClick = {
                                        try {
                                            val intent = android.content.Intent().apply {
                                                action = android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                                                data = android.net.Uri.parse("package:${settingsContext.packageName}")
                                            }
                                            settingsContext.startActivity(intent)
                                        } catch (e: Exception) {
                                            notificationMessage = if (isBn) "সিঙ্ক পারমিশন স্ক্রিন ওপেন করা সম্ভব হয়নি!" else "Could not open sync permission screen!"
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(38.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C1E1E), contentColor = Color(0xFFFF8888)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(text = if (isBn) "রিয়েল-টাইম সিঙ্ক পারমিশন দিন" else "Enable Exact Alarms", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // SECTION 6: CONNECTION CODE & PAIRING (বন্ধন তথ্য)
                    if (partnerPhone.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C141F)),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFF1E2F45))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                ) {
                                    Text("🔗", fontSize = 16.sp)
                                    Text(
                                        text = if (isBn) "ডিভাইস সংযোগ ও বন্ধন তথ্য" else "Connection & Soul Binding",
                                        color = Color(0xFFFFD700),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    text = if (isBn) "আমার সংযোগ কোড" else "My Binding Code",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )

                                val localContext = androidx.compose.ui.platform.LocalContext.current
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = myBindingCode,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFFFD700)
                                    )
                                    TextButton(
                                        onClick = {
                                            val clipboard = localContext.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                            val clip = android.content.ClipData.newPlainText("Binding Code", myBindingCode)
                                            clipboard.setPrimaryClip(clip)
                                            notificationMessage = if (isBn) "কোড কপি করা হয়েছে!" else "Binding code copied!"
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Copy code",
                                            tint = Color(0xFFFFD700),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isBn) "কোড কপি করুন" else "Copy Code",
                                            fontSize = 11.sp,
                                            color = Color(0xFFFFD700),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Divider(color = Color(0xFF1E2F45), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 10.dp))

                                // SECTION 7: GITHUB OTA UPDATES (NO USB REQUIRED)
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A121D)),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Color(0xFF1E3A5F))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("🐙", fontSize = 16.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (isBn) "গিটহাব অটো-আপডেট (No USB)" else "GitHub OTA Auto-Update",
                                                    color = Color(0xFF60A5FA),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Text(
                                                text = "v${com.example.BuildConfig.VERSION_NAME}",
                                                color = Color.Gray,
                                                fontSize = 11.sp
                                            )
                                        }

                                        Text(
                                            text = if (isBn) "গিটহাব রেপো নাম:" else "GitHub Repository Slug:",
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = gitHubRepo,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )

                                        if (!gitHubStatusMessage.isNullOrBlank()) {
                                            Text(
                                                text = gitHubStatusMessage!!,
                                                fontSize = 10.sp,
                                                color = if (isGitHubUpdateAvailable) Color(0xFF00FF88) else Color(0xFF94A3B8),
                                                modifier = Modifier.padding(bottom = 6.dp)
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { viewModel.checkForGitHubUpdates() },
                                                enabled = !isCheckingGitHubUpdate,
                                                modifier = Modifier.weight(1.2f).height(36.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A5F), contentColor = Color(0xFF60A5FA)),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                if (isCheckingGitHubUpdate) {
                                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color(0xFF60A5FA), strokeWidth = 2.dp)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                } else {
                                                    Text("🔄 ", fontSize = 12.sp)
                                                }
                                                Text(
                                                    text = if (isBn) "আপডেট চেক" else "Check Now",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            Button(
                                                onClick = {
                                                    inputGitHubRepoSlug = gitHubRepo
                                                    showGitHubRepoDialog = true
                                                },
                                                modifier = Modifier.weight(1f).height(36.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = Color.White),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Text(
                                                    text = if (isBn) "✏️ রেপো চেঞ্জ" else "✏️ Change Repo",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                Divider(color = Color(0xFF1E2F45), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 10.dp))

                                Button(
                                    onClick = {
                                        viewModel.logout()
                                        showSettingsDialog = false
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF3F1F1F),
                                        contentColor = Color(0xFFFFAAAA)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Disconnect icon",
                                        tint = Color(0xFFFFAAAA),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isBn) "লগআউট ও সংযোগ বিচ্ছিন্ন করুন" else "Logout & Disconnect",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateProfileNicknames(dialogMyNick, dialogPartnerNick)
                        showSettingsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(text = if (isBn) "পরিবর্তন সংরক্ষণ করুন" else "Save Changes", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text(text = if (isBn) "বাতিল" else "Cancel", color = Color.Gray, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            },
            containerColor = Color(0xFF070B12),
            shape = RoundedCornerShape(24.dp)
        )
    }

    // DIALOG 2: CUSTOM MESSAGE CONTROL
    if (showCustomTextDialog) {
        AlertDialog(
            onDismissRequest = { showCustomTextDialog = false },
            title = {
                Text(
                    text = if (isBn) "নিজের বার্তা লিখুন" else "Personal Mood Message",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                OutlinedTextField(
                    value = customTextInput,
                    onValueChange = { customTextInput = it },
                    label = { Text(if (isBn) "কাস্টম অনুভূতি স্ট্যাটাস" else "Custom feeling details") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFA855F7),
                        unfocusedBorderColor = Color(0xFF2C2C3F)
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setUserStatus(StatusType.CUSTOM, customTextInput)
                        showCustomTextDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7), contentColor = Color.White)
                ) {
                    Text(text = if (isBn) "নির্ধারণ করুন" else "Set Status")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomTextDialog = false }) {
                    Text(text = if (isBn) "বাতিল" else "Dismiss", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1B1B26),
            shape = RoundedCornerShape(20.dp)
        )
    }

    // DIALOG 3: GITHUB REPO CHANGE CONTROL
    if (showGitHubRepoDialog) {
        AlertDialog(
            onDismissRequest = { showGitHubRepoDialog = false },
            title = {
                Text(
                    text = if (isBn) "গিটহাব রেপজিটরি পরিবর্তন" else "Change GitHub Repository",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = if (isBn) "আপনার গিটহাবের username/repository লিখুন:" else "Enter your GitHub username/repository slug:",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = inputGitHubRepoSlug,
                        onValueChange = { inputGitHubRepoSlug = it },
                        label = { Text(if (isBn) "গিটহাব রেপো স্ল্যাগ" else "GitHub Repo Slug") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF60A5FA),
                            unfocusedBorderColor = Color(0xFF2C2C3F)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputGitHubRepoSlug.isNotBlank()) {
                            viewModel.updateGitHubRepoSlug(inputGitHubRepoSlug)
                        }
                        showGitHubRepoDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF60A5FA), contentColor = Color.Black)
                ) {
                    Text(text = if (isBn) "সংরক্ষণ ও চেক" else "Save & Check", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGitHubRepoDialog = false }) {
                    Text(text = if (isBn) "বাতিল" else "Cancel", color = Color.Gray, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            },
            containerColor = Color(0xFF070B12),
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun GitHubUpdateBanner(
    isBn: Boolean,
    releaseInfo: GitHubReleaseInfo,
    onDownloadClick: (String) -> Unit,
    onViewRepoClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101C2B)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, Color(0xFF00FF88))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🚀", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isBn) "গিটহাবে নতুন কোড আপডেট পাওয়া গেছে!" else "New GitHub OTA Update Found!",
                        color = Color(0xFF00FF88),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Text(
                    text = releaseInfo.tagName,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .background(Color(0xFF00FF88).copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = releaseInfo.releaseTitle.ifEmpty { if (isBn) "গিটহাবে নতুন কোড পরিবর্তন সফলভাবে আপডেট করা হয়েছে" else "Code update pushed to GitHub repository" },
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            if (releaseInfo.body.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = releaseInfo.body,
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    maxLines = 3
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val apkUrl = releaseInfo.apkDownloadUrl ?: releaseInfo.htmlUrl
                Button(
                    onClick = { onDownloadClick(apkUrl) },
                    modifier = Modifier.weight(1.2f).height(38.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF88), contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = if (isBn) "📥 সরাসরি APK ডাউনলোড" else "📥 Download APK",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = { onViewRepoClick(releaseInfo.htmlUrl) },
                    modifier = Modifier.weight(1f).height(38.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = if (isBn) "🔗 গিটহাব দেখুন" else "🔗 View Repo",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = Color.Gray
                    )
                }
            }
        }
    }
}

// SCROLLABLE SUB-LIST FOR LOG RECORDS
@Composable
fun LazyColumnForLogs(logs: List<String>) {
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(logs) { log ->
            Text(
                text = log,
                fontSize = 11.sp,
                color = Color.LightGray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            )
        }
    }
}

// CORE FEATURE 3 COMPANION: RENDER CUSTOM CANVAS DRAWING BASED ON THE SEVEN STATUS TYPES PASS-IN WITH ENDLESS ROTATION
@Composable
fun StatusAvatarAnimation(status: StatusType, pulseScale: Float, opacityMultiplier: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "avatar_motion")
    val angleRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val breathingRippleRadius by infiniteTransition.animateFloat(
        initialValue = 30f,
        targetValue = 85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ripple"
    )

    when (status) {
        StatusType.FREE -> {
            // Blooming Sunflower Canvas
            Canvas(modifier = Modifier.fillMaxSize().scale(pulseScale)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val petalRadius = 45f
                val petalCount = 8
                for (i in 0 until petalCount) {
                    val angleRad = Math.toRadians((i * (360f / petalCount) + angleRotation).toDouble())
                    val petalCenter = Offset(
                        x = center.x + (60f * Math.cos(angleRad)).toFloat(),
                        y = center.y + (60f * Math.sin(angleRad)).toFloat()
                    )
                    drawCircle(
                        color = Color(0xFFFFD700),
                        radius = petalRadius,
                        center = petalCenter,
                        alpha = 0.85f
                    )
                    drawCircle(
                        color = Color(0xFFFFA500),
                        radius = petalRadius,
                        center = petalCenter,
                        style = Stroke(width = 3f),
                        alpha = 0.9f
                    )
                }

                drawCircle(
                    color = Color(0xFF4B2306),
                    radius = 52f,
                    center = center
                )
                drawCircle(
                    color = Color(0xFF00FF88),
                    radius = 52f,
                    center = center,
                    style = Stroke(width = 4f),
                    alpha = opacityMultiplier
                )

                // Eyes
                drawCircle(
                    color = Color.White,
                    radius = 5f,
                    center = Offset(center.x - 16f, center.y - 8f)
                )
                drawCircle(
                    color = Color.White,
                    radius = 5f,
                    center = Offset(center.x + 16f, center.y - 8f)
                )
                // Smiley curve mouth
                drawArc(
                    color = Color.White,
                    startAngle = 10f,
                    sweepAngle = 160f,
                    useCenter = false,
                    topLeft = Offset(center.x - 18f, center.y - 4f),
                    size = androidx.compose.ui.geometry.Size(36f, 24f),
                    style = Stroke(width = 4f)
                )
            }
        }

        StatusType.STUDYING -> {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                drawCircle(
                    color = Color(0xFFFFD700).copy(alpha = 0.3f),
                    radius = 65f,
                    center = center,
                    style = Stroke(width = 3f)
                )
                val bubbleCount = 4
                for (i in 0 until bubbleCount) {
                    val angleRad = Math.toRadians((i * (360f / bubbleCount) + angleRotation).toDouble())
                    val bubbleCenter = Offset(
                        x = center.x + (65f * Math.cos(angleRad)).toFloat(),
                        y = center.y + (65f * Math.sin(angleRad)).toFloat()
                    )
                    drawCircle(
                        color = Color(0xFFFFD700),
                        radius = 12f,
                        center = bubbleCenter
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 4f,
                        center = bubbleCenter
                    )
                }

                drawCircle(
                    color = Color(0xFFFFD700).copy(alpha = 0.15f * pulseScale),
                    radius = 40f,
                    center = center
                )
                drawCircle(
                    color = Color(0xFFFFD700),
                    radius = 18f,
                    center = center
                )
                drawCircle(
                    color = Color.Black,
                    radius = 8f,
                    center = center
                )
                drawCircle(
                    color = Color.White,
                    radius = 3f,
                    center = Offset(center.x + 3f, center.y - 3f)
                )
            }
        }

        StatusType.SLEEPING -> {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                drawCircle(
                    color = Color(0xFF3B82F6).copy(alpha = 0.15f),
                    radius = breathingRippleRadius,
                    center = center
                )
                drawCircle(
                    color = Color(0xFF1E3A8A),
                    radius = 42f,
                    center = center
                )
                drawCircle(
                    color = Color(0xFF3B82F6),
                    radius = 42f,
                    center = center,
                    style = Stroke(width = 3f)
                )
                drawArc(
                    color = Color(0xFF93C5FD),
                    startAngle = 10f,
                    sweepAngle = 160f,
                    useCenter = false,
                    topLeft = Offset(center.x - 22f, center.y - 12f),
                    size = androidx.compose.ui.geometry.Size(16f, 12f),
                    style = Stroke(width = 3f)
                )
                drawArc(
                    color = Color(0xFF93C5FD),
                    startAngle = 10f,
                    sweepAngle = 160f,
                    useCenter = false,
                    topLeft = Offset(center.x + 6f, center.y - 12f),
                    size = androidx.compose.ui.geometry.Size(16f, 12f),
                    style = Stroke(width = 3f)
                )
                drawCircle(
                    color = Color.LightGray.copy(alpha = 0.7f),
                    radius = 6f,
                    center = Offset(center.x + 22f, center.y + 12f)
                )
            }
        }

        StatusType.BUSY -> {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                drawCircle(
                    color = Color(0xFFEF4444).copy(alpha = 0.12f * pulseScale),
                    radius = 80f,
                    center = center
                )
                drawCircle(
                    color = Color(0xFFEF4444).copy(alpha = 0.4f),
                    radius = 70f,
                    center = center,
                    style = Stroke(width = 2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f), angleRotation))
                )
                drawCircle(
                    color = Color(0xFF7F1D1D),
                    radius = 38f,
                    center = center
                )
                drawCircle(
                    color = Color(0xFFEF4444),
                    radius = 38f,
                    center = center,
                    style = Stroke(width = 4f)
                )
                drawLine(
                    color = Color(0xFFEF4444),
                    start = Offset(center.x - 20f, center.y),
                    end = Offset(center.x + 20f, center.y),
                    strokeWidth = 8f
                )
            }
        }

        StatusType.SCROLLING -> {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = 40f
                val flowOffset = (angleRotation * 0.8f) % 40f
                drawCircle(
                    color = Color(0xFFEC4899).copy(alpha = 0.15f),
                    radius = baseRadius + flowOffset,
                    center = center
                )
                drawCircle(
                    color = Color(0xFFEC4899).copy(alpha = 0.3f),
                    radius = baseRadius + flowOffset / 2f,
                    center = center,
                    style = Stroke(width = 3f)
                )
                drawCircle(
                    color = Color(0xFFEC4899),
                    radius = 20f,
                    center = center
                )
            }
        }

        StatusType.TALKING -> {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val waveHeight = 25f * opacityMultiplier
                val strokeWidth = 5f
                for (i in -2..2) {
                    if (i == 0) continue
                    val xPos = center.x + (i * 20f)
                    val height = waveHeight * (3 - Math.abs(i)) * 0.4f
                    drawLine(
                        color = Color(0xFF14B8A6),
                        start = Offset(xPos, center.y - height),
                        end = Offset(xPos, center.y + height),
                        strokeWidth = strokeWidth
                    )
                }
                drawCircle(
                    color = Color(0xFF14B8A6).copy(alpha = 0.2f),
                    radius = 50f,
                    center = center,
                    style = Stroke(width = 2f)
                )
            }
        }

        StatusType.HOME -> {
            Canvas(modifier = Modifier.fillMaxSize().rotate(angleRotation)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                drawCircle(
                    color = Color(0xFFF59E0B).copy(alpha = 0.4f),
                    radius = 60f,
                    center = center,
                    style = Stroke(width = 3f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 15f)))
                )
                drawCircle(
                    color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                    radius = 40f,
                    center = center
                )
                drawCircle(
                    color = Color(0xFFF59E0B),
                    radius = 12f,
                    center = center
                )
            }
        }

        StatusType.OFFLINE -> {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                drawCircle(
                    color = Color(0xFF9CA3AF).copy(alpha = 0.2f),
                    radius = breathingRippleRadius,
                    center = center,
                    style = Stroke(width = 2f)
                )
                drawCircle(
                    color = Color(0xFF9CA3AF),
                    radius = 15f,
                    center = center
                )
                drawLine(
                    color = Color(0xFF9CA3AF),
                    start = Offset(center.x - 30f, center.y + 30f),
                    end = Offset(center.x + 30f, center.y - 30f),
                    strokeWidth = 3f
                )
            }
        }

        StatusType.CUSTOM -> {
            Canvas(modifier = Modifier.fillMaxSize().rotate(angleRotation)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val loopCount = 6
                for (i in 0 until loopCount) {
                    val loopAngle = i * (360f / loopCount)
                    val loopAngleRad = Math.toRadians(loopAngle.toDouble())
                    val loopPos = Offset(
                        x = center.x + (15f * Math.cos(loopAngleRad)).toFloat(),
                        y = center.y + (15f * Math.sin(loopAngleRad)).toFloat()
                    )
                    drawCircle(
                        color = Color(0xFFA855F7).copy(alpha = 0.4f),
                        radius = 35f,
                        center = loopPos,
                        style = Stroke(width = 3f)
                    )
                }

                drawCircle(
                    color = Color(0xFFA855F7),
                    radius = 16f,
                    center = center
                )
                drawCircle(
                    color = Color.White,
                    radius = 6f,
                    center = center
                )
            }
        }
    }
}

// POETIC WELCOME HEADER - Renders the golden logo, poetic branding, and a beautiful cycling carousel of literary quote pairs.
@Composable
fun PoeticWelcomeHeader(isBn: Boolean, pulseScale: Float, bobbingOffset: Float) {
    val quotesList = listOf(
        Pair(
            "সূর্যমুখী যেমন সূর্যের দিকে মুখ করে থাকে, তেমনি আমিও তোমার দিকে মুখিয়ে থাকি।",
            "Just as a sunflower keeps its face turned towards the sun, so do I keep yearning for your gaze."
        ),
        Pair(
            "দূরত্ব কেবল কিছু মাইলফলকের খেলা, আমাদের স্পর্শ তো এক স্পন্দনেই মিশে থাকে।",
            "Distance is but a game of miles; our touch lies synchronized within a single heartbeat."
        ),
        Pair(
            "শত ব্যস্ততার মাঝেও আমার মন তোমাতেই ফিরে আসে, সূর্যমুখী যেমন ফেরে তার সূর্যের টানে।",
            "Amidst the world's busy tides, my mind floats back to you—as surely as a sunflower follows her sun."
        ),
        Pair(
            "পৃথিবীর কোলাহলে যখনই এ মন ক্লান্ত হয়, তোমার শান্ত উপস্থিতির ছোঁয়া আমাকে সজীব করে।",
            "Whenever the clamor of life tires my spirit, the gentle touch of your presence breathes life back into me."
        ),
        Pair(
            "আমাদের নীরব কথোপকথন আত্মার গভীরে এক অনির্বাণ সুর তোলে, প্রিয় সূর্যমুখী।",
            "Our voiceless conversations script an eternal song inside the depths of our souls, my precious sunflower."
        )
    )

    var currentQuoteIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            currentQuoteIndex = (currentQuoteIndex + 1) % quotesList.size
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Aesthetic sunflower logo container
        Box(
            modifier = Modifier
                .size(100.dp)
                .offset(y = bobbingOffset.dp)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            // Ambient glowing shadow effect
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFD700).copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Circular logo image
            Image(
                painter = painterResource(id = R.drawable.surjomukhi_logo),
                contentDescription = "Surjomukhi Poetic Logo",
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .border(
                        width = 1.5.dp,
                        color = Color(0xFFFFD700).copy(alpha = 0.75f),
                        shape = CircleShape
                    )
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Poetic brand title
        Text(
            text = if (isBn) "সূর্যমুখী" else "Surjomukhi",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.headlineLarge.copy(
                shadow = Shadow(
                    color = Color(0xFFFFD700).copy(alpha = 0.35f),
                    offset = Offset(0f, 0f),
                    blurRadius = 12f
                )
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Romantic tagline
        Text(
            text = if (isBn) "অনির্বাণ হৃদস্পন্দন ও আত্মিক মেলবন্ধন" else "Eternal Heartbeat & Soulful Presence",
            color = Color(0xFFFFD700).copy(alpha = 0.85f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Elegant ornamental divider
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.size(24.dp).height(1.dp).background(Color(0xFF28283B)))
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Ornamental Heart",
                tint = Color(0xFFFFD700).copy(alpha = 0.62f),
                modifier = Modifier.size(12.dp).scale(pulseScale)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.size(24.dp).height(1.dp).background(Color(0xFF28283B)))
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Active Quote Box (with automatic transition and interactive Tap-to-Skip)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0F0F16))
                .border(1.dp, Color(0xFF1E1E2B), RoundedCornerShape(16.dp))
                .clickable {
                    currentQuoteIndex = (currentQuoteIndex + 1) % quotesList.size
                }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            val (bnText, enText) = quotesList[currentQuoteIndex]
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Poetic Quotation marks
                Text(
                    text = "“",
                    color = Color(0xFFFFD700).copy(alpha = 0.4f),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 16.sp
                )
                
                Box(modifier = Modifier.animateContentSize()) {
                    Text(
                        text = if (isBn) bnText else enText,
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                
                // Indicators inside the card
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    quotesList.forEachIndexed { idx, _ ->
                        val isSelected = idx == currentQuoteIndex
                        Box(
                            modifier = Modifier
                                .size(width = if (isSelected) 12.dp else 6.dp, height = 6.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color(0xFFFFD700) else Color(0xFF2A2A3B))
                        )
                    }
                }
            }
        }
    }
}

