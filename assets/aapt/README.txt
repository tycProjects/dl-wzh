android.jar is no longer bundled. The app downloads it once on first use from kAndroidJarUrl
(lib/quick/aapt_build.dart). To bundle it again, put android.jar in this folder and it is used
instead of downloading.
