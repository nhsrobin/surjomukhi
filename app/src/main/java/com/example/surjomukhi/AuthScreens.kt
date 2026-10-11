package com.example.surjomukhi

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

// =========================================================================
// 1. REUSABLE GLASS BACKGROUND CONTAINER (Seamless blend with auth_bg.jpg)
// =========================================================================
@Composable
fun AuthBackgroundContainer(
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090B10))
    ) {
        // User's custom auth background image (auth_bg.jpg)
        Image(
            painter = painterResource(id = R.drawable.auth_bg),
            contentDescription = "Auth Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Soft, transparent glass gradient overlay so the background image shines through vividly
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF000000).copy(alpha = 0.16f),
                            Color(0xFF000000).copy(alpha = 0.08f),
                            Color(0xFF000000).copy(alpha = 0.32f)
                        )
                    )
                )
        )

        content()
    }
}

// =========================================================================
// 2. FROSTED GLASS INPUT COMPONENT (Matches screen seamlessly without clutter)
// =========================================================================
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {}
) {
    var passwordVisible by remember { mutableStateOf(!isPassword) }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(
            color = Color.White,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Medium
        ),
        cursorBrush = SolidColor(Color(0xFFFF3366)),
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onDone = { onImeAction() },
            onNext = { onImeAction() }
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF0C0B10).copy(alpha = 0.42f),
                                Color(0xFF140F16).copy(alpha = 0.38f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.80f),
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 14.sp
                        )
                    }
                    innerTextField()
                }

                if (isPassword) {
                    IconButton(
                        onClick = { passwordVisible = !passwordVisible },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (passwordVisible) "লুকান" else "দেখুন",
                            tint = Color.White.copy(alpha = 0.80f),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    )
}

// =========================================================================
// 3. GRADIENT PILL BUTTON WITH ARROW BADGE (Clean 52.dp matching height)
// =========================================================================
@Composable
fun GradientArrowButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFFFF3366),
            Color(0xFFFF2A6D),
            Color(0xFFE11D48)
        )
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color(0xFF331B24).copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(26.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            // Circular Right Arrow Badge on the far right
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(30.dp)
                    .background(Color.White.copy(alpha = 0.24f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Forward",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// =========================================================================
// 4. GOOGLE AUTH BUTTON (Matching 52.dp glass style with input boxes)
// =========================================================================
@Composable
fun GoogleAuthButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF0C0B10).copy(alpha = 0.42f),
                        Color(0xFF140F16).copy(alpha = 0.38f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.22f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        GoogleLogoIcon(modifier = Modifier.size(20.dp))

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = text,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun GoogleLogoIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f
        val strokeWidth = radius * 0.32f

        // Google Multi-Color stylized G
        drawCircle(
            color = Color(0xFF4285F4),
            radius = radius - strokeWidth / 2,
            center = center,
            style = Stroke(width = strokeWidth)
        )
        // Red top arc
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = 180f,
            sweepAngle = 100f,
            useCenter = false,
            style = Stroke(width = strokeWidth)
        )
        // Yellow left arc
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 120f,
            sweepAngle = 60f,
            useCenter = false,
            style = Stroke(width = strokeWidth)
        )
        // Green bottom arc
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 20f,
            sweepAngle = 100f,
            useCenter = false,
            style = Stroke(width = strokeWidth)
        )
        // Blue horizontal crossbar
        drawLine(
            color = Color(0xFF4285F4),
            start = Offset(center.x, center.y),
            end = Offset(center.x + radius, center.y),
            strokeWidth = strokeWidth
        )
    }
}

// =========================================================================
// 5. SCREEN 1: LOGIN SCREEN (স্বাগত! 💗)
// =========================================================================
@Composable
fun LoginScreen(
    onLoginClick: (String, String) -> Unit,
    onNavigateToSignUp: () -> Unit,
    onGoogleSignInClick: (email: String, displayName: String) -> Unit
) {
    var emailOrPhone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var showGoogleAccountPicker by remember { mutableStateOf(false) }

    AuthBackgroundContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Top Branding: Sunflower + by TasfiwnLabs with white science labs icon
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🌻", fontSize = 28.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "সূর্যমুখী",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Science,
                            contentDescription = "Labs Icon",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "by TasfiwnLabs",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Welcome Title
            Text(
                text = "স্বাগত! 💗",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.3.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "তোমার প্রিয় মানুষের সাথে\nসংযুক্ত থাকো, সবসময় 🌻",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Field 1: Email or Mobile Number
            GlassTextField(
                value = emailOrPhone,
                onValueChange = { emailOrPhone = it },
                placeholder = "ইমেইল বা মোবাইল নাম্বার",
                leadingIcon = Icons.Outlined.Email,
                keyboardType = KeyboardType.Email,
                modifier = Modifier.testTag("login_identity_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Field 2: Password
            GlassTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = "পাসওয়ার্ড",
                leadingIcon = Icons.Outlined.Lock,
                isPassword = true,
                imeAction = ImeAction.Done,
                onImeAction = {
                    if (emailOrPhone.isNotBlank() && password.isNotBlank()) {
                        onLoginClick(emailOrPhone, password)
                    }
                },
                modifier = Modifier.testTag("login_password_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Forgot Password Link (Right aligned)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "পাসওয়ার্ড ভুলে গেছেন?",
                    color = Color(0xFFFF4D6D),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clickable { showForgotPasswordDialog = true }
                        .padding(vertical = 4.dp, horizontal = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Main Gradient Pill Button: লগইন করুন →
            GradientArrowButton(
                text = "লগইন করুন",
                onClick = { onLoginClick(emailOrPhone, password) },
                modifier = Modifier.testTag("login_submit_button")
            )

            Spacer(modifier = Modifier.height(22.dp))

            // Divider: अथवा
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color.White.copy(alpha = 0.20f),
                    thickness = 0.8.dp
                )
                Text(
                    text = "অথবা",
                    color = Color.White.copy(alpha = 0.70f),
                    fontSize = 12.5.sp,
                    modifier = Modifier.padding(horizontal = 14.dp)
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color.White.copy(alpha = 0.20f),
                    thickness = 0.8.dp
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // ONLY Google Sign-In with Firebase Auth
            GoogleAuthButton(
                text = "Google দিয়ে লগইন",
                onClick = { showGoogleAccountPicker = true },
                modifier = Modifier.testTag("google_login_button")
            )

            Spacer(modifier = Modifier.height(34.dp))

            // Footer Link
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 30.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "একাউন্ট নেই? ",
                    color = Color.White.copy(alpha = 0.80f),
                    fontSize = 13.5.sp
                )
                Text(
                    text = "সাইন আপ করুন",
                    color = Color(0xFFFF4D6D),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onNavigateToSignUp() }
                        .testTag("to_signup_button")
                )
            }
        }

        // Forgot password dialog
        if (showForgotPasswordDialog) {
            AlertDialog(
                onDismissRequest = { showForgotPasswordDialog = false },
                title = { Text("পাসওয়ার্ড পুনরুদ্ধার", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "আপনার একাউন্টের ইমেইল অথবা সিক্রেট কোড দিয়ে পাসওয়ার্ড রিসেট করতে সহায়তার জন্য যোগাযোগ করুন অথবা গুগল দিয়ে সরাসরি লগইন করুন।",
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showForgotPasswordDialog = false }) {
                        Text("ঠিক আছে", color = Color(0xFFFF3B69), fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = Color(0xFF131118),
                shape = RoundedCornerShape(18.dp)
            )
        }

        // Functional Firebase Google Account Picker Sheet
        if (showGoogleAccountPicker) {
            GoogleAccountPickerDialog(
                onDismiss = { showGoogleAccountPicker = false },
                onSelectAccount = { email, name ->
                    showGoogleAccountPicker = false
                    onGoogleSignInClick(email, name)
                }
            )
        }
    }
}

// =========================================================================
// 6. SCREEN 2: SIGN UP SCREEN (নতুন একাউন্ট তৈরি করুন 💗)
// =========================================================================
@Composable
fun SignUpScreen(
    onSignUpClick: (name: String, email: String, pass: String, confirmPass: String, termsAgreed: Boolean) -> Unit,
    onBackToLogin: () -> Unit,
    onGoogleSignUpClick: (email: String, displayName: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var termsAgreed by remember { mutableStateOf(true) }
    var showGoogleAccountPicker by remember { mutableStateOf(false) }

    AuthBackgroundContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Back Arrow Button & Top Branding
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBackToLogin,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .testTag("signup_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Login",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Science,
                        contentDescription = "Labs",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "by TasfiwnLabs",
                        color = Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Heading
            Text(
                text = "নতুন একাউন্ট তৈরি করুন 💗",
                color = Color.White,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "সূর্যমুখী-তে তোমাকে স্বাগতম 🌻",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 1. Name
            GlassTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = "নাম লিখুন",
                leadingIcon = Icons.Outlined.Person,
                modifier = Modifier.testTag("signup_name_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Email
            GlassTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = "ইমেইল",
                leadingIcon = Icons.Outlined.Email,
                keyboardType = KeyboardType.Email,
                modifier = Modifier.testTag("signup_email_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Password
            GlassTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = "পাসওয়ার্ড",
                leadingIcon = Icons.Outlined.Lock,
                isPassword = true,
                modifier = Modifier.testTag("signup_password_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Confirm Password
            GlassTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                placeholder = "পাসওয়ার্ড নিশ্চিত করুন",
                leadingIcon = Icons.Outlined.Lock,
                isPassword = true,
                imeAction = ImeAction.Done,
                onImeAction = {
                    onSignUpClick(name, email, password, confirmPassword, termsAgreed)
                },
                modifier = Modifier.testTag("signup_confirm_password_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Checkbox: Terms & Privacy Agreement
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { termsAgreed = !termsAgreed },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (termsAgreed) Color(0xFFFF2A6D) else Color.White.copy(alpha = 0.12f))
                        .border(
                            width = 1.dp,
                            color = if (termsAgreed) Color(0xFFFF2A6D) else Color.White.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(6.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (termsAgreed) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Checked",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "আমি শর্তাবলী এবং গোপনীয়তা নীতিমালা\nপড়েছি এবং সম্মত",
                    color = Color(0xFFFF4D6D),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Main Gradient Pill Button: সাইন আপ করুন →
            GradientArrowButton(
                text = "সাইন আপ করুন",
                onClick = { onSignUpClick(name, email, password, confirmPassword, termsAgreed) },
                modifier = Modifier.testTag("signup_submit_button")
            )

            Spacer(modifier = Modifier.height(22.dp))

            // Divider: अथवा
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.20f), thickness = 0.8.dp)
                Text(text = "অথবা", color = Color.White.copy(alpha = 0.70f), fontSize = 12.5.sp, modifier = Modifier.padding(horizontal = 14.dp))
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.20f), thickness = 0.8.dp)
            }

            Spacer(modifier = Modifier.height(22.dp))

            // ONLY Google Sign-Up with Firebase Auth
            GoogleAuthButton(
                text = "Google দিয়ে সাইন আপ",
                onClick = { showGoogleAccountPicker = true }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Footer Link: ইতিমধ্যে একাউন্ট আছে? লগইন করুন
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 30.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ইতিমধ্যে একাউন্ট আছে? ",
                    color = Color.White.copy(alpha = 0.80f),
                    fontSize = 13.5.sp
                )
                Text(
                    text = "লগইন করুন",
                    color = Color(0xFFFF4D6D),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onBackToLogin() }
                        .testTag("to_login_button")
                )
            }
        }

        // Functional Firebase Google Account Picker Sheet
        if (showGoogleAccountPicker) {
            GoogleAccountPickerDialog(
                onDismiss = { showGoogleAccountPicker = false },
                onSelectAccount = { googleEmail, googleName ->
                    showGoogleAccountPicker = false
                    onGoogleSignUpClick(googleEmail, googleName)
                }
            )
        }
    }
}

// =========================================================================
// 7. SCREEN 3: BINDING SCREEN (বাইন্ডিং করুন 💗)
// =========================================================================
@Composable
fun BindingScreen(
    myBindingCode: String,
    onBindClick: (String) -> Unit,
    onLogoutClick: () -> Unit
) {
    val context = LocalContext.current
    var inputCode by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf("code") } // "code" or "qr"
    var copyNotice by remember { mutableStateOf<String?>(null) }

    // Pulsing heart animation between avatars
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_heart")
    val heartScale by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heart_pulse"
    )

    AuthBackgroundContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Top Bar: Back Arrow + Center Title + Lab Branding
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
            ) {
                IconButton(
                    onClick = onLogoutClick,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .testTag("binding_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back / Logout",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = "বাইন্ডিং করুন",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )

                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Science,
                        contentDescription = "Labs",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "TasfiwnLabs",
                        color = Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Heading
            Text(
                text = "তোমার প্রিয় মানুষকে যুক্ত করো 💗",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "একটি সিক্রেট কোড বা QR স্ক্যান করে\nসংযোগ স্থাপন করুন 🌻",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.5.sp,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Two Avatars with Glowing Line & Pulsing Neon Heart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // User Avatar (Left, Coral/Orange glowing ring)
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .border(
                            width = 2.5.dp,
                            brush = Brush.sweepGradient(
                                listOf(Color(0xFFFF7A00), Color(0xFFFF3366), Color(0xFFFF7A00))
                            ),
                            shape = CircleShape
                        )
                        .background(Color(0xFF140F16).copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "User",
                        tint = Color(0xFFFFAB70),
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Center Glowing Connecting Line with Pulsing Heart
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Glowing horizontal connector line
                    HorizontalDivider(
                        color = Color(0xFFFF2A6D).copy(alpha = 0.75f),
                        thickness = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Glowing pulsating Heart in the middle
                    Box(
                        modifier = Modifier
                            .scale(heartScale)
                            .size(32.dp)
                            .background(Color(0xFF131118), CircleShape)
                            .border(1.dp, Color(0xFFFF2A6D).copy(alpha = 0.7f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Connection Heart",
                            tint = Color(0xFFFF2A6D),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Partner Avatar (Right, Magenta/Purple glowing ring)
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .border(
                            width = 2.5.dp,
                            brush = Brush.sweepGradient(
                                listOf(Color(0xFF8B5CF6), Color(0xFFD946EF), Color(0xFF8B5CF6))
                            ),
                            shape = CircleShape
                        )
                        .background(Color(0xFF140F16).copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Partner",
                        tint = Color(0xFFC084FC),
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tab Selector Pill: [ সিক্রেট কোড ] [ QR স্ক্যান ]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0C0B10).copy(alpha = 0.45f))
                    .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(14.dp))
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tab 1: Secret Code
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (selectedTab == "code") Color.White.copy(alpha = 0.16f) else Color.Transparent)
                        .border(
                            width = if (selectedTab == "code") 1.dp else 0.dp,
                            color = if (selectedTab == "code") Color.White.copy(alpha = 0.25f) else Color.Transparent,
                            shape = RoundedCornerShape(11.dp)
                        )
                        .clickable { selectedTab = "code" }
                        .testTag("tab_secret_code"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "সিক্রেট কোড",
                        color = if (selectedTab == "code") Color.White else Color.White.copy(alpha = 0.65f),
                        fontSize = 13.5.sp,
                        fontWeight = if (selectedTab == "code") FontWeight.Bold else FontWeight.Medium
                    )
                }

                // Tab 2: QR Scan
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (selectedTab == "qr") Color.White.copy(alpha = 0.16f) else Color.Transparent)
                        .border(
                            width = if (selectedTab == "qr") 1.dp else 0.dp,
                            color = if (selectedTab == "qr") Color.White.copy(alpha = 0.25f) else Color.Transparent,
                            shape = RoundedCornerShape(11.dp)
                        )
                        .clickable { selectedTab = "qr" }
                        .testTag("tab_qr_scan"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "QR স্ক্যান",
                        color = if (selectedTab == "qr") Color.White else Color.White.copy(alpha = 0.65f),
                        fontSize = 13.5.sp,
                        fontWeight = if (selectedTab == "qr") FontWeight.Bold else FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (selectedTab == "code") {
                // Label: — সিক্রেট কোড দিন
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = "— সিক্রেট কোড দিন",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Input Field: Key icon + placeholder
                GlassTextField(
                    value = inputCode,
                    onValueChange = { inputCode = it },
                    placeholder = "সিক্রেট কোড লিখুন",
                    leadingIcon = Icons.Default.VpnKey,
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                    onImeAction = {
                        if (inputCode.isNotBlank()) onBindClick(inputCode)
                    },
                    modifier = Modifier.testTag("bind_code_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Helper Note
                Text(
                    text = "তোমার প্রিয় মানুষকে অ্যাপের সেটিংস থেকে\nসিক্রেট কোড শেয়ার করতে বলো।",
                    color = Color.White.copy(alpha = 0.70f),
                    fontSize = 12.5.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Convenient Self Secret Code Pill with Copy Action
                if (myBindingCode.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0C0B10).copy(alpha = 0.45f))
                            .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "তোমার নিজের সিক্রেট কোড:",
                                color = Color.White.copy(alpha = 0.70f),
                                fontSize = 11.5.sp
                            )
                            Text(
                                text = myBindingCode,
                                color = Color(0xFFFFD700),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                        }

                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Surjomukhi Secret Code", myBindingCode)
                                clipboard.setPrimaryClip(clip)
                                copyNotice = "কোড কপি করা হয়েছে!"
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Code",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (copyNotice != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = copyNotice!!,
                            color = Color(0xFF00FF88),
                            fontSize = 11.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Main Gradient Pill Button: সংযোগ করুন →
                GradientArrowButton(
                    text = "সংযোগ করুন",
                    onClick = { onBindClick(inputCode) },
                    modifier = Modifier.testTag("bind_submit_button")
                )
            } else {
                // QR Scan Tab Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF0C0B10).copy(alpha = 0.45f))
                        .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(18.dp))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = "QR Code",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(90.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "সিক্রেট কোড: $myBindingCode",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "তোমার সঙ্গীকে এই কিউআর স্ক্যান করতে বলো অথবা সিক্রেট কোডটি শেয়ার করো।",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.5.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 17.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                GradientArrowButton(
                    text = "কোড ইনপুট ট্যাবে যান",
                    onClick = { selectedTab = "code" }
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Bottom Privacy Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = "Privacy Shield",
                    tint = Color.White.copy(alpha = 0.80f),
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "তোমার প্রাইভেসি আমাদের অগ্রাধিকার 💗",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.5.sp
                )
            }
        }
    }
}

// =========================================================================
// 8. GOOGLE ACCOUNT SELECTION DIALOG (Firebase Auth)
// =========================================================================
@Composable
fun GoogleAccountPickerDialog(
    onDismiss: () -> Unit,
    onSelectAccount: (email: String, displayName: String) -> Unit
) {
    var customEmailInput by remember { mutableStateOf("") }
    var isEnteringCustom by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GoogleLogoIcon(modifier = Modifier.size(22.dp))
                Text(
                    text = "গুগল দিয়ে সাইন ইন",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "ফায়ারবেস অথেন্টিকেশনের জন্য আপনার গুগল অ্যাকাউন্টটি বেছে নিন:",
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Default 1-Tap account option: nhsrobin595@gmail.com
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1E2436))
                        .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(14.dp))
                        .clickable { onSelectAccount("nhsrobin595@gmail.com", "Robin") }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF4285F4), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "R",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Robin",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                        Text(
                            text = "nhsrobin595@gmail.com",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (!isEnteringCustom) {
                    TextButton(
                        onClick = { isEnteringCustom = true },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = "+ অন্য গুগল অ্যাকাউন্ট লিখুন",
                            color = Color(0xFF60A5FA),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    GlassTextField(
                        value = customEmailInput,
                        onValueChange = { customEmailInput = it },
                        placeholder = "yourname@gmail.com",
                        leadingIcon = Icons.Outlined.Email,
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done,
                        onImeAction = {
                            if (customEmailInput.isNotBlank()) {
                                onSelectAccount(customEmailInput, customEmailInput.substringBefore("@"))
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (customEmailInput.isNotBlank()) {
                                onSelectAccount(customEmailInput, customEmailInput.substringBefore("@"))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text("এই অ্যাকাউন্ট দিয়ে প্রবেশ করুন", color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "বাতিল", color = Color.Gray, fontSize = 12.5.sp)
            }
        },
        containerColor = Color(0xFF131118),
        shape = RoundedCornerShape(20.dp)
    )
}
