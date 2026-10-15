package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.EduRepository
import com.example.ui.theme.BrandDanger
import com.example.ui.theme.BrandLightBlue
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandSuccess
import kotlinx.coroutines.delay

@Composable
fun AuthScreen(
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    var isSignUp by remember { mutableStateOf(false) }

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // OTP Verification State
    var showOtpDialog by remember { mutableStateOf(false) }
    var generatedOtp by remember { mutableStateOf("") }
    var enteredOtp by remember { mutableStateOf("") }
    var otpSecondsRemaining by remember { mutableIntStateOf(60) }

    // Automatic login when admin credentials are typed: adminhamo@gmail.com & 246810
    LaunchedEffect(email, password) {
        if (EduRepository.isAdminCredentials(email, password)) {
            EduRepository.login(email, password)
            onLoginSuccess()
        }
    }

    LaunchedEffect(showOtpDialog) {
        if (showOtpDialog) {
            otpSecondsRemaining = 60
            while (otpSecondsRemaining > 0) {
                delay(1000)
                otpSecondsRemaining--
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F172A),
                            BrandPrimary
                        )
                    )
                )
                .padding(top = 40.dp, bottom = 32.dp, start = 20.dp, end = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .border(2.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.School,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = EduRepository.PLATFORM_NAME_DEFAULT,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "إعداد: حمادة ابن ضيف",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Auth Form Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 24.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Tab Switcher (تسجيل دخول / إنشاء حساب)
                TabRow(
                    selectedTabIndex = if (isSignUp) 1 else 0,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = !isSignUp,
                        onClick = {
                            isSignUp = false
                            errorMessage = ""
                        },
                        text = {
                            Text(
                                "تسجيل الدخول",
                                fontWeight = FontWeight.Bold,
                                color = if (!isSignUp) BrandPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    Tab(
                        selected = isSignUp,
                        onClick = {
                            isSignUp = true
                            errorMessage = ""
                        },
                        text = {
                            Text(
                                "إنشاء حساب جديد",
                                fontWeight = FontWeight.Bold,
                                color = if (isSignUp) BrandPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }

                if (isSignUp) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("اسم الطالب ثلاثي") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Filled.Person, contentDescription = null, tint = BrandPrimary)
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_fullname_input")
                    )
                }

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف (واتساب للتواصل)") },
                    placeholder = { Text("01xxxxxxxxx") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.Phone, contentDescription = null, tint = BrandPrimary)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_phone_input")
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        errorMessage = ""
                    },
                    label = { Text("البريد الإلكتروني") },
                    placeholder = { Text("example@gmail.com") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.Email, contentDescription = null, tint = BrandPrimary)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_email_input")
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = ""
                    },
                    label = { Text("كلمة السر") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.Lock, contentDescription = null, tint = BrandPrimary)
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_password_input")
                )

                if (errorMessage.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BrandDanger.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage,
                            color = BrandDanger,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            errorMessage = "يرجى كتابة البريد الإلكتروني وكلمة السر!"
                            return@Button
                        }

                        // Check special admin login
                        if (EduRepository.isAdminCredentials(email, password)) {
                            EduRepository.login(email, password)
                            onLoginSuccess()
                            return@Button
                        }

                        if (isSignUp) {
                            if (fullName.isBlank() || phone.isBlank()) {
                                errorMessage = "يرجى ملء الاسم ورقم الهاتف لإرسال رمز OTP!"
                                return@Button
                            }
                            // Generate OTP and open verification modal
                            generatedOtp = EduRepository.generateOtpForEmail(email, phone, fullName)
                            showOtpDialog = true
                        } else {
                            // Student login
                            val ok = EduRepository.login(email, password)
                            if (ok) {
                                onLoginSuccess()
                            } else {
                                errorMessage = "بيانات الدخول غير صحيحة"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("auth_submit_btn")
                ) {
                    Text(
                        text = if (isSignUp) "إرسال رمز التحقق OTP والإنشاء" else "تسجيل الدخول للمنصة",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Technical Support & Footer Info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${EduRepository.TECH_SUPPORT_PHONE}"))
                        context.startActivity(intent)
                    }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(imageVector = Icons.Filled.SupportAgent, contentDescription = null, tint = BrandPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "الدعم الفني المباشر: ${EduRepository.TECH_SUPPORT_PHONE}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = BrandPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = EduRepository.FOOTER_CREDIT,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // OTP Verification Dialog
    if (showOtpDialog) {
        var otpError by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showOtpDialog = false },
            title = {
                Text(
                    text = "التحقق من البريد الإلكتروني (رمز OTP)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "تم إرسال رمز التحقق إلى بريدك: $email",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Simulated push notification toast showing the code
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = BrandSuccess.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BrandSuccess.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null, tint = BrandSuccess)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "📩 إشعار المنصة برمز التحقق:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrandSuccess,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "رمز OTP الخاص بك هو: $generatedOtp",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BrandSuccess
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = enteredOtp,
                        onValueChange = {
                            enteredOtp = it
                            otpError = ""
                        },
                        label = { Text("أدخل رمز التحقق (4 أرقام)") },
                        placeholder = { Text(generatedOtp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("otp_code_input")
                    )

                    if (otpError.isNotBlank()) {
                        Text(
                            text = otpError,
                            color = BrandDanger,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (otpSecondsRemaining > 0) "إعادة الإرسال خلال: $otpSecondsRemaining ثانية" else "يمكنك طلب رمز جديد الآن",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (otpSecondsRemaining == 0) {
                            TextButton(onClick = {
                                generatedOtp = EduRepository.generateOtpForEmail(email, phone, fullName)
                                otpSecondsRemaining = 60
                            }) {
                                Text("إعادة إرسال")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val verified = EduRepository.verifyOtpAndRegister(fullName, email, phone, enteredOtp)
                        if (verified) {
                            showOtpDialog = false
                            onLoginSuccess()
                        } else {
                            otpError = "رمز OTP غير صحيح! جرب الرمز: $generatedOtp"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                    modifier = Modifier.testTag("confirm_otp_btn")
                ) {
                    Text("تأكيد وإنشاء الحساب")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOtpDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
