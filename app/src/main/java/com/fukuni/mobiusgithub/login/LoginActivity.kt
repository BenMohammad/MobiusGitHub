package com.fukuni.mobiusgithub.login

import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import com.fukuni.mobiusgithub.R

class LoginActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        setContentView(R.layout.login_main)

        if(supportFragmentManager.findFragmentByTag("TAG") == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.login_fragment, LoginFragment(), "TAG")
                .commit()
        }
    }
}