package com.termux.hz;

import android.content.Context;
import android.content.SharedPreferences;

import com.termux.shared.termux.TermuxConstants;

import java.io.File;

/** Paths, constants and preferences for HZ. */
public final class Hz {
    private Hz() {}

    public static final String PREFIX = TermuxConstants.TERMUX_PREFIX_DIR_PATH;
    public static final String HOME = TermuxConstants.TERMUX_HOME_DIR_PATH;
    public static final File PROJECTS_DIR = new File(HOME, "hz-projects");
    public static final File JVM_DIR = new File(PREFIX, "lib/jvm");
    public static final int[] JAVA_VERSIONS = {8, 11, 17, 21};

    public static final String GROQ_BASE = "https://api.groq.com/openai/v1";
    public static final String OPENROUTER_BASE = "https://openrouter.ai/api/v1";

    // Preference keys
    public static final String K_SETUP_DONE = "setup_done";
    public static final String K_JDKS_WANTED = "jdks_wanted";
    public static final String K_GROQ_KEY = "groq_key";
    public static final String K_OR_KEY = "openrouter_key";
    public static final String K_PROVIDER = "provider";
    public static final String K_GROQ_MODEL = "groq_model";
    public static final String K_OR_MODEL = "openrouter_model";
    public static final String K_APPROVAL = "approval"; // 0 ask all, 1 auto edits, 2 full auto
    public static final String K_BUILD_TASK = "build_task";
    public static final String K_BUILD_ARGS = "build_args";
    public static final String K_XMX = "xmx_mb";
    public static final String K_LAST_PROJECT = "last_project";
    public static final String K_FONT_SIZE = "font_size";
    public static final String K_WRAP = "word_wrap";
    public static final String K_JDK_URL_PREFIX = "jdk_url_"; // + version
    public static final String K_JAVA_PREFIX = "java_override_"; // + project name

    public static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("hz", Context.MODE_PRIVATE);
    }

    public static boolean isBootstrapped() {
        return new File(PREFIX, "bin/bash").exists() || new File(PREFIX, "bin/sh").exists();
    }

    public static File projectsDir() {
        if (!PROJECTS_DIR.exists()) PROJECTS_DIR.mkdirs();
        return PROJECTS_DIR;
    }
}
