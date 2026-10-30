package com.example.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkBg
import com.example.ui.theme.HindSiliguriFontFamily
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.SecondaryViolet
import com.example.ui.viewmodel.AppViewModel
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    viewModel: AppViewModel,
    onAuthSuccess: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Login, 1: Sign Up
    val scope = rememberCoroutineScope()

    // Form states
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var signUpFullName by remember { mutableStateOf("") }
    var signUpUsername by remember { mutableStateOf("") }
    var signUpEmail by remember { mutableStateOf("") }
    var signUpPassword by remember { mutableStateOf("") }
    var signUpConfirmPassword by remember { mutableStateOf("") }
    var signUpPasswordVisible by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showForgotDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var resetMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        PrimaryPurple.copy(alpha = 0.2f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(30.dp))

            // App Brand Logo & Title
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(PrimaryPurple),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoStories,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "গল্পকথা",
                fontFamily = HindSiliguriFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "বাংলা সাহিত্যের আধুনিক ডিজিটাল ভুবন",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Tab Selector: লগইন / রেজিস্ট্রেশন
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = PrimaryPurple,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = PrimaryPurple
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            errorMessage = null
                        },
                        text = {
                            Text(
                                text = "লগইন",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 16.sp
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            errorMessage = null
                        },
                        text = {
                            Text(
                                text = "রেজিস্ট্রেশন",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 16.sp
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Error Message Banner
            if (errorMessage != null) {
                Surface(
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFC62828),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (selectedTab == 0) {
                // LOGIN TAB
                OutlinedTextField(
                    value = identifier,
                    onValueChange = { identifier = it },
                    label = { Text("ইউজারনেম অথবা ইমেইল") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryPurple)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_identifier_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("পাসওয়ার্ড") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryPurple)
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "পাসওয়ার্ড লুকান" else "পাসওয়ার্ড দেখুন"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_password_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showForgotDialog = true }) {
                        Text("পাসওয়ার্ড ভুলে গেছেন?", fontSize = 13.sp, color = SecondaryViolet)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (identifier.isBlank() || password.isBlank()) {
                            errorMessage = "অনুগ্রহ করে সকল তথ্য পূরণ করুন।"
                            return@Button
                        }
                        isLoading = true
                        errorMessage = null
                        scope.launch {
                            val result = viewModel.authRepository.login(identifier, password)
                            isLoading = false
                            result.fold(
                                onSuccess = { onAuthSuccess() },
                                onFailure = { errorMessage = it.message }
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("login_submit_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("লগইন করুন", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Fast One-Click Reader Login for immediate testing
                OutlinedButton(
                    onClick = {
                        isLoading = true
                        scope.launch {
                            val result = viewModel.authRepository.login("ariful_reader", "123456")
                            isLoading = false
                            result.fold(
                                onSuccess = { onAuthSuccess() },
                                onFailure = { errorMessage = it.message }
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("fast_reader_login_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("পাঠক হিসেবে সরাসরি প্রবেশ করুন (এক-ক্লিক)")
                }

            } else {
                // SIGN UP TAB
                OutlinedTextField(
                    value = signUpFullName,
                    onValueChange = { signUpFullName = it },
                    label = { Text("পূর্ণ নাম (Full Name)") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryPurple)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("signup_name_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = signUpUsername,
                    onValueChange = { signUpUsername = it },
                    label = { Text("ইউজারনেম (ইংরেজি ছোট অক্ষর)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("signup_username_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = signUpEmail,
                    onValueChange = { signUpEmail = it },
                    label = { Text("ইমেইল ঠিকানা") },
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null, tint = PrimaryPurple)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("signup_email_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = signUpPassword,
                    onValueChange = { signUpPassword = it },
                    label = { Text("পাসওয়ার্ড (কমপক্ষে ৬ অক্ষর)") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryPurple)
                    },
                    visualTransformation = if (signUpPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { signUpPasswordVisible = !signUpPasswordVisible }) {
                            Icon(
                                imageVector = if (signUpPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("signup_password_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = signUpConfirmPassword,
                    onValueChange = { signUpConfirmPassword = it },
                    label = { Text("পাসওয়ার্ড নিশ্চিত করুন") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryPurple)
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("signup_confirm_password_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (signUpFullName.isBlank() || signUpUsername.isBlank() || signUpEmail.isBlank() || signUpPassword.isBlank()) {
                            errorMessage = "অনুগ্রহ করে সকল তথ্য পূরণ করুন।"
                            return@Button
                        }
                        if (signUpPassword != signUpConfirmPassword) {
                            errorMessage = "উভয় পাসওয়ার্ড একই হতে হবে।"
                            return@Button
                        }
                        isLoading = true
                        errorMessage = null
                        scope.launch {
                            val result = viewModel.authRepository.register(
                                fullName = signUpFullName,
                                username = signUpUsername,
                                email = signUpEmail,
                                password = signUpPassword
                            )
                            isLoading = false
                            result.fold(
                                onSuccess = { onAuthSuccess() },
                                onFailure = { errorMessage = it.message }
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("signup_submit_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("অ্যাকাউন্ট তৈরি করুন", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer branding
            Text(
                text = "Made by Mahin Parvez",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Forgot Password Dialog
    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = {
                showForgotDialog = false
                resetMessage = null
            },
            title = { Text("পাসওয়ার্ড উদ্ধার") },
            text = {
                Column {
                    Text("আপনার অ্যাকাউন্টের ইমেইল ঠিকানা প্রদান করুন:")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("ইমেইল") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (resetMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(resetMessage!!, color = PrimaryPurple, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        val res = viewModel.authRepository.requestPasswordReset(resetEmail)
                        resetMessage = res.getOrElse { it.message }
                    }
                }) {
                    Text("রিসেট করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showForgotDialog = false
                    resetMessage = null
                }) {
                    Text("বন্ধ করুন")
                }
            }
        )
    }
}
