package com.fakesocial.app

import android.app.Application
import com.fakesocial.app.data.AppDatabase
import com.fakesocial.app.data.ProjectRepository

class FakeSocialApp : Application() {
    val repository: ProjectRepository by lazy {
        ProjectRepository(AppDatabase.create(this).projectDao())
    }
}
