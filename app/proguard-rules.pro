# R8 aktif pada build release. Jangan menonaktifkan minify untuk rilis.
-repackageclasses ''
-allowaccessmodification
-keepattributes SourceFile,LineNumberTable,Signature,*Annotation*
-renamesourcefileattribute SourceFile

# OkHttp: simpan metadata anotasi yang dipakai refleksi.
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations

# Jangan mencetak log debug/verbose/info pada release.
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Jangan menambahkan aturan keep luas untuk seluruh package aplikasi;
# aturan keep berlebihan akan mengurangi manfaat obfuscation R8.
