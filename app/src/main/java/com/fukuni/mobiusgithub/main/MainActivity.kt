package com.fukuni.mobiusgithub.main

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatViewInflater
import androidx.core.view.LayoutInflaterCompat
import androidx.core.view.isEmpty
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fukuni.mobiusgithub.R
import com.fukuni.mobiusgithub.SampleApp
import com.fukuni.mobiusgithub.data.GitHubService
import com.fukuni.mobiusgithub.domain.main.*
import com.spotify.mobius.First
import com.spotify.mobius.MobiusLoop
import com.spotify.mobius.android.AndroidLogger
import com.spotify.mobius.android.MobiusAndroid
import com.spotify.mobius.rx2.RxConnectables
import com.spotify.mobius.rx2.RxMobius
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_main.*
import org.eclipse.egit.github.core.Repository

class MainActivity : AppCompatActivity(), IMainView {

    lateinit var reposList : RecyclerView
    lateinit var progressbar : ProgressBar
    lateinit var errorText : TextView

    lateinit var api : GitHubService

    var rxEffectHandler =
        RxMobius.subtypeEffectHandler<MainEffect, MainEvent>()
            .add(LoadReposEffect::class.java, this::handleLoadRepos)
            .build()

    var loopFactory: MobiusLoop.Factory<MainModel, MainEvent, MainEffect> =
        RxMobius
            .loop(MainUpdate(), rxEffectHandler)
            .init{
                First.first(MainModel(userName = api.getUserName()), setOf(
                    LoadReposEffect(
                        api.getUserName()
                    )
                ))
            }.logger(AndroidLogger.tag<MainModel, MainEvent, MainEffect>("my-app"))


    lateinit var controller: MobiusLoop.Controller<MainModel, MainEvent>



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        reposList = findViewById(R.id.repos_list)
        progressbar = findViewById(R.id.repos_progress)
        errorText = findViewById(R.id.error_text)

        reposList.layoutManager = LinearLayoutManager(applicationContext)

        api = (application as SampleApp).service
        controller = MobiusAndroid.controller(loopFactory,
            MainModel(userName = api.getUserName())
        )
        controller.connect(RxConnectables.fromTransformer(this::connectViews))
    }


    override fun onResume() {
        super.onResume()
        controller.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        controller.stop()
    }

    fun handleLoadRepos(request: Observable<LoadReposEffect>): Observable<MainEvent> {
        return request.flatMap {
            effect -> api.getStarredRepos(effect.userName).map {
            repos -> ReposLoadedEvent(
            repos)
        }.toObservable()
        }
    }

    fun connectViews(models: Observable<MainModel>): Observable<MainEvent> {
        val disposable = models
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe{render(it)}

        return Observable
            .just(IdleEvent as MainEvent)
            .doOnDispose(disposable::dispose)
    }

    fun render(state: MainModel) {
        state.apply {
            setTitle(state.userName + "'s starred repos")

            if(isLoading) {
                if(reposList.isEmpty()) {
                    showProgress()
                }
            } else {
                hideProgres()
                if(reposList.isEmpty()) {
                    setErrorText("User has no starred repos")
                    showErrorText()
                }
            }

            setRepos(repoList)
        }
    }



    override fun setTitle(s: String) {
        supportActionBar!!.setTitle(title)
    }

    override fun showProgress() {
        progressbar.visibility = View.VISIBLE
    }

    override fun hideProgres() {
        progressbar.visibility = View.GONE
    }

    override fun setErrorText(s: String) {
        errorText.text = s
    }

    override fun showErrorText() {
        errorText.visibility = View.VISIBLE
    }

    override fun setRepos(repos: List<Repository>) {
        reposList.adapter = ReposAdapter(repos, layoutInflater)
    }


    private inner class ReposAdapter(private val repos: List<Repository>, private val inflater: LayoutInflater) :
            RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return ReposViewHolder(inflater.inflate(R.layout.repos_list_item_layout, parent, false))


        }

        override fun getItemCount(): Int {
            return repos.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            (holder as ReposViewHolder).bind(repos[position])
        }

        internal inner class ReposViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
            var repoName: TextView
            var repoStarsCount : TextView

            init {
                repoName = itemView.findViewById(R.id.repo_name) as TextView
                repoStarsCount = itemView.findViewById(R.id.repo_stars_count) as TextView
            }

            fun bind(repository: Repository) {
                repoName.text = repository.name
                repoStarsCount.text = "Watchers: " + repository.watchers
            }

        }

    }




}
