package com.example.surjomukhi

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

    // Map keys to StatusType
    val myStatus = StatusType.fromKey(myStatusKey)
    val partnerStatus = StatusType.fromKey(partnerStatusKey)

    // Form states for LOGIN
    var inputPhone by remember { mutableStateOf("") }
    var inputNickname by remember { mutableStateOf("") }
    var inputOtpCode by remember { mutableStateOf("") }
    var isOtpSent by remember { mutableStateOf(false) }
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
            .windowInsetsPadding(WindowInsets.statusBars)
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

        // Header floating indicators (Always accessible on top right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Poetic app name logo
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Flower logo",
                    tint = Color(0xFFFFD700),
                    modifier = Modifier
                        .size(28.dp)
                        .rotate(15f * pulseScale)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isBn) "সূর্যমুখী" else "Surjomukhi",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge.copy(
                        shadow = Shadow(
                            color = Color(0xFFFFD700).copy(alpha = 0.3f),
                            offset = Offset(0f, 0f),
                            blurRadius = 8f
                        )
                    )
                )
            }

            // Language switcher button
            Button(
                onClick = { viewModel.toggleLanguage() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF181824),
                    contentColor = Color(0xFFFFD700)
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(34.dp)
                    .border(1.dp, Color(0xFF28283B), RoundedCornerShape(12.dp))
            ) {
                Text(
                    text = if (isBn) "English" else "বাংলা",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // NAVIGATION CONTROLLER BY STATE
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 64.dp, start = 20.dp, end = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {

            // SCENARIO 1: USER IS NOT LOGGED IN yet (Phone login)
            if (myPhone.isEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                
                // Greeting Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111119)),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0xFF20202F))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isBn) "মনের মেলবন্ধন" else "Emotional Connection",
                            color = Color(0xFFFFD700),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isBn) "দূরত্ব যাই হোক না কেন, স্পর্শ থাকুক সবসময়। ওটিপির মাধ্যমে আপনার মোবাইল নম্বর দিয়ে ব্যক্তিগত সেশনে প্রবেশ করুন।"
                            else "No matter the distance, stay connected instantly. Sign in securely using your phone to start your soulful status sync.",
                            color = Color.LightGray,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Form Inputs
                        OutlinedTextField(
                            value = inputPhone,
                            onValueChange = { inputPhone = it },
                            label = { Text(if (isBn) "আপনার মোবাইল নম্বর" else "Your Phone Number") },
                            placeholder = { Text("e.g. +88017xxxxxxxx") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            enabled = !isOtpSent,
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "PhoneIcon") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFFFD700),
                                unfocusedBorderColor = Color(0xFF28283B)
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = inputNickname,
                            onValueChange = { inputNickname = it },
                            label = { Text(if (isBn) "আপনার মিষ্টি নাম (নিকনেম)" else "Your Nickname") },
                            placeholder = { Text(if (isBn) "যেমন: আমার হৃদয়" else "e.g. Dreamer") },
                            singleLine = true,
                            enabled = !isOtpSent,
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "NickIcon") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFFFD700),
                                unfocusedBorderColor = Color(0xFF28283B)
                            )
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Toggle Buttons (Send SMS or Submit OTP)
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val activity = remember(context) {
                            var ctx = context
                            while (ctx is android.content.ContextWrapper) {
                                if (ctx is android.app.Activity) {
                                    break
                                }
                                ctx = ctx.baseContext
                            }
                            ctx as? android.app.Activity
                        }

                        if (!isOtpSent) {
                            Button(
                                onClick = {
                                    if (inputPhone.length >= 10) {
                                        if (activity != null) {
                                            viewModel.verifyPhoneAndSendOtp(
                                                activity = activity,
                                                phone = inputPhone,
                                                nickname = inputNickname,
                                                onCodeSent = {
                                                    isOtpSent = true
                                                    notificationMessage = if (isBn) "ওটিপি কোড পাঠানো হয়েছে! আপনার ইনবক্স চেক করুন।" else "OTP Code Sent! Check your mobile messages."
                                                },
                                                onFailure = { errorMsg ->
                                                    notificationMessage = errorMsg
                                                }
                                            )
                                        } else {
                                            notificationMessage = if (isBn) "রানিং অ্যাক্টিভিটি পাওয়া যায়নি!" else "Unable to resolve active Android window context!"
                                        }
                                    } else {
                                        notificationMessage = if (isBn) "সঠিক মোবাইল নম্বর টাইপ করুন!" else "Please enter a valid phone number!"
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color.Black),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = if (isBn) "ভেরিফিকেশন কোড পাঠান 💬" else "Send Verification Code 💬",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        } else {
                            // OTP input field is revealed
                            OutlinedTextField(
                                value = inputOtpCode,
                                onValueChange = { inputOtpCode = it },
                                label = { Text(if (isBn) "৬ ডিজিট ওটিপি কোড" else "6-Digit SMS Code") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "LockIcon") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF00FF88),
                                    unfocusedBorderColor = Color(0xFF28283B)
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    viewModel.confirmOtpAndLogin(inputPhone, inputNickname, inputOtpCode) { success, msg ->
                                        if (success) {
                                            notificationMessage = if (isBn) "সফলভাবে প্রবেশ করেছেন!" else "Successfully Logged In!"
                                        } else {
                                            notificationMessage = msg
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF88), contentColor = Color.Black),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = if (isBn) "যাচাই সম্পূর্ণ করুন ও প্রবেশ করুন 🔓" else "Verify & Sign In 🔓",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            TextButton(
                                onClick = {
                                    isOtpSent = false
                                    inputOtpCode = ""
                                }
                            ) {
                                Text(
                                    text = if (isBn) "← মোবাইল নম্বর পরিবর্তন করুন" else "← Change Phone Number",
                                    color = Color.LightGray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // SCENARIO 2: LOGGED IN, BUT NO PARTNER BOUND YET (Show pairing screen)
            else if (partnerPhone.isEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111119)),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0xFF28283B))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isBn) "আত্মিক বন্ধন স্থাপন" else "Establish Soul Bond",
                            color = Color(0xFFFFD700),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isBn) "লগইন সম্পন্ন হয়েছে! এখন অপর পাশের প্রিয় মানুষটির কোড যোগ করে হৃদয়ের মেলবন্ধন গড়ুন।"
                            else "Safe entry complete! Now exchange 6-digit connection codes with your partner to bridge your live statuses real-time.",
                            color = Color.LightGray,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Display My unique connection Code
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF1B1B26))
                                .border(1.dp, Color(0xFF3A3A4F), RoundedCornerShape(16.dp))
                                .padding(horizontal = 24.dp, vertical = 16.dp)
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

                        Spacer(modifier = Modifier.height(24.dp))

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

                        Spacer(modifier = Modifier.height(18.dp))

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

                        Spacer(modifier = Modifier.height(24.dp))

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
                Spacer(modifier = Modifier.height(16.dp))

                // TOP ROW: NICKNAMES & SETTINGS TRIGGERS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isBn) "হৃদয়ের মেলবন্ধন" else "Heart & Soul Synchronized",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = myNicknameByVm,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Matched favorite heart",
                                tint = Color.Red,
                                modifier = Modifier
                                    .padding(horizontal = 6.dp)
                                    .size(14.dp)
                                    .scale(pulseScale)
                            )
                            Text(
                                text = partnerNicknameByVm,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFEC4899)
                            )
                        }
                    }

                    // Settings & Logout Buttons inside small row
                    Row {
                        IconButton(
                            onClick = {
                                dialogMyNick = myNicknameByVm
                                dialogPartnerNick = partnerNicknameByVm
                                showSettingsDialog = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Profile Settings",
                                tint = Color.LightGray,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.logout() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Logout and disconnect",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // MAIN HERO STATUS DISPLAY CARD (Reflecting the active status canvas and character vibe)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 360.dp)
                        .padding(bottom = 16.dp)
                        .testTag("status_display_section"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF12121B)),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(
                        width = 2.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                partnerStatus.color,
                                Color(0xFF222230)
                            )
                        )
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Header indication showing partner's name
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(11.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(partnerStatus.color)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBn) "\"$partnerNicknameByVm\" এর বর্তমান উপস্থিতি"
                                else "\"$partnerNicknameByVm\"'s Current Presence",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.LightGray
                            )
                        }

                        // STATUS REPRESENTED 3D BOBBING FLOATING AVATAR WITH CANVAS RIPPLES
                        Box(
                            modifier = Modifier
                                .size(175.dp)
                                .padding(bottom = 8.dp)
                                .offset(y = bobbingOffset.dp)
                                .scale(pulseScale),
                            contentAlignment = Alignment.Center
                        ) {
                            // Ambient radial layout background glow
                            Box(
                                modifier = Modifier
                                    .size(115.dp)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                partnerStatus.color.copy(alpha = 0.22f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )

                            // Load specialized high-impact canvas effects dynamically
                            StatusAvatarAnimation(
                                status = partnerStatus,
                                pulseScale = 1.0f,
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
                                fontSize = 60.sp,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Dynamic status badge name
                        val statusLabel = if (isBn) partnerStatus.labelBn else partnerStatus.labelEn
                        Text(
                            text = statusLabel,
                            color = partnerStatus.color,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                shadow = Shadow(
                                    color = partnerStatus.color.copy(alpha = 0.35f),
                                    offset = Offset(0f, 0f),
                                    blurRadius = 14f
                                )
                            )
                        )

                        // Custon subtext or description
                        if (partnerStatus == StatusType.CUSTOM && partnerCustomText.isNotEmpty()) {
                            Text(
                                text = "\"$partnerCustomText\"",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            val statusDesc = if (isBn) partnerStatus.subtextBn else partnerStatus.subtextEn
                            Text(
                                text = statusDesc,
                                color = Color.Gray,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 4.dp),
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
                            fontSize = 11.sp,
                            color = Color.DarkGray,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }
                }

                // USER'S SELF-STATUS CONTROLLER LAYOUT
                Text(
                    text = if (isBn) "আপনার বর্তমান অনুভূতি নির্ধারণ করুন:" else "Select your active feeling:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .testTag("status_button_row"),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(StatusType.entries) { status ->
                        val isSelected = (myStatusKey == status.key)
                        val badgeLabel = if (isBn) status.labelBn else status.labelEn

                        Box(
                            modifier = Modifier
                                .offset(y = if (isSelected) 3.dp else 0.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) {
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                status.color.copy(alpha = 0.22f),
                                                status.color.copy(alpha = 0.08f)
                                            )
                                        )
                                    } else {
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0xFF20202F),
                                                Color(0xFF12121B)
                                            )
                                        )
                                    }
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) status.color else Color(0xFF2C2C3F),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    if (status == StatusType.CUSTOM) {
                                        customTextInput = myCustomText
                                        showCustomTextDialog = true
                                    } else {
                                        viewModel.setUserStatus(status)
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .testTag("status_btn_${status.key}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(status.color)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = badgeLabel,
                                    color = if (isSelected) Color.White else Color.LightGray,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else Modifier.let { FontWeight.Normal }
                                )
                            }
                        }
                    }
                }

                // Reveal Custom Text Dialogue if active
                AnimatedVisibility(
                    visible = myStatus == StatusType.CUSTOM,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B26)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF28283B))
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isBn) "কাস্টম মনের অনুভূতি" else "Custom Spirit Message",
                                    fontSize = 11.sp,
                                    color = Color.LightGray,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (myCustomText.isNotEmpty()) "\"$myCustomText\"" else if (isBn) "(কোনো কাস্টম বার্তা দেওয়া নেই)" else "(No custom status written)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFA855F7)
                                )
                            }
                            IconButton(onClick = { showCustomTextDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit unique text",
                                    tint = Color.LightGray
                                )
                            }
                        }
                    }
                }

                // ONE-CLICK COZY VIBE PULSE CARD (TAPPING TRIGGERS THE RECEIVER'S DEVICE PHYSICAL VIBRATOR)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1322)),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFF3B234F))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
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
                        IconButton(
                            onClick = { viewModel.sendPingNotification() },
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(if (partnerStatus == StatusType.FREE) Color(0xFF00FF88) else Color(0xFFEF4444).copy(alpha = 0.2f))
                                .border(1.dp, if (partnerStatus == StatusType.FREE) Color(0xFF00FF88) else Color(0xFFEF4444), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Pulse Ping notification",
                                tint = if (partnerStatus == StatusType.FREE) Color.Black else Color(0xFFEF4444),
                                modifier = Modifier
                                    .size(20.dp)
                                    .scale(if (partnerStatus == StatusType.FREE) pulseScale else 1.0f)
                            )
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
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .padding(vertical = 6.dp),
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
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // FLOATING ACTION NOTIFICATION BAR (FOR TOAST MESSAGES)
        AnimatedVisibility(
            visible = notificationMessage != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp, start = 20.dp, end = 20.dp)
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

    // DIALOG 1: NICKNAME & PROFILE EDITING DIALOG (Editable by personal or partner!)
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Text(
                    text = if (isBn) "নাম সংশোধন করুন" else "Customize Synergies",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isBn) "নিজের এবং সঙ্গীর নিকনেম ইচ্ছামতো পরিবর্তন করুন। যেকোনো পরিবর্তন সাথে সাথে সার্ভারে সিঙ্ক হবে।"
                        else "Update nicknames on the fly. Changing either values directly updates Firestore.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 14.dp)
                    )

                    OutlinedTextField(
                        value = dialogMyNick,
                        onValueChange = { dialogMyNick = it },
                        label = { Text(if (isBn) "আমার নিকনেম" else "My Nickname") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFFD700),
                            unfocusedBorderColor = Color(0xFF2C2C3F)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = dialogPartnerNick,
                        onValueChange = { dialogPartnerNick = it },
                        label = { Text(if (isBn) "সঙ্গীর নিকনেম" else "Partner's Nickname") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFFD700),
                            unfocusedBorderColor = Color(0xFF2C2C3F)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateProfileNicknames(dialogMyNick, dialogPartnerNick)
                        showSettingsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF88), contentColor = Color.Black)
                ) {
                    Text(text = if (isBn) "সংরক্ষণ" else "Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text(text = if (isBn) "বাতিল" else "Dismiss", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1B1B26),
            shape = RoundedCornerShape(20.dp)
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

