package com.fukuni.mobiusgithub.data

import io.reactivex.Single

interface IAppPrefs {

    fun getUserSavedCredentials(): Single<Pair<String, String>>

    fun saveUserSavedCredentails(login: String, pass: String): Single<Boolean>
}