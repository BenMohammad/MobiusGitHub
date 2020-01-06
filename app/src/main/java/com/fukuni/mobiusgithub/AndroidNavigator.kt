package com.fukuni.mobiusgithub

import android.content.Intent
import androidx.fragment.app.FragmentActivity
import com.fukuni.mobiusgithub.main.MainActivity

class AndroidNavigator(private val activity: FragmentActivity): Navigator {

    override fun GoToMainScreen() {
        val i = Intent(activity, MainActivity::class.java)
        activity.startActivity(i)
        activity.finish()
    }
}