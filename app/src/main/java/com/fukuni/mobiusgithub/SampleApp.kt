package com.fukuni.mobiusgithub

import android.app.Application
import com.fukuni.mobiusgithub.data.GitHubService
import io.reactivex.schedulers.Schedulers

class SampleApp: Application() {

    lateinit var service: GitHubService

    override fun onCreate() {
        super.onCreate()
        service = GitHubService(Schedulers.io())
    }

}