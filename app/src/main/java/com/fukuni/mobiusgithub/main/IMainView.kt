package com.fukuni.mobiusgithub.main

import org.eclipse.egit.github.core.Repository

interface IMainView {

    fun setTitle(s: String)
    fun showProgress()
    fun hideProgres()
    fun setErrorText(s: String)
    fun showErrorText()
    fun setRepos(reposList: List<Repository>)

}