# TUANANH BROWSER - quy tac giu code khi minify (R8)
# App khong dung reflection / addJavascriptInterface; androidx.webkit tu kem quy tac rieng.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
# Giu Activity/Service/Receiver khai bao trong Manifest (R8 da tu giu, them cho chac)
-keep public class * extends android.app.Activity
