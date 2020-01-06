package com.fukuni.mobiusgithub.domain.main

import org.eclipse.egit.github.core.Repository

data class MainModel(val isLoading: Boolean = true,
                     val userName: String,
                     val repoList: List<Repository> = listOf())

sealed class MainEffect
data class LoadReposEffect(val userName: String): MainEffect()

sealed class MainEvent
object MainInit: MainEvent()
data class ReposLoadedEvent(val repoList: List<Repository>): MainEvent()
object idleEvent: MainEvent()