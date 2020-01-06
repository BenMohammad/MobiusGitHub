package com.fukuni.mobiusgithub.login

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.TextView

import androidx.fragment.app.Fragment
import com.fukuni.mobiusgithub.AndroidNavigator
import com.fukuni.mobiusgithub.Navigator
import com.fukuni.mobiusgithub.R
import com.fukuni.mobiusgithub.SampleApp
import com.fukuni.mobiusgithub.data.AppPrefs
import com.fukuni.mobiusgithub.data.GitHubService
import com.fukuni.mobiusgithub.domain.login.*
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxCompoundButton
import com.jakewharton.rxbinding2.widget.RxTextView
import com.spotify.mobius.EventSource
import com.spotify.mobius.First
import com.spotify.mobius.MobiusLoop
import com.spotify.mobius.android.AndroidLogger
import com.spotify.mobius.android.MobiusAndroid
import com.spotify.mobius.rx2.RxConnectables
import com.spotify.mobius.rx2.RxEventSources
import com.spotify.mobius.rx2.RxMobius
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers

class LoginFragment: Fragment(), ILoginView {

    lateinit var prefs: AppPrefs
    lateinit var api: GitHubService
    lateinit var navigator: Navigator

    lateinit var loginText: TextInputEditText
    lateinit var loginInput: TextInputLayout
    lateinit var passwordInput: TextInputLayout
    lateinit var passwordText: TextInputEditText
    lateinit var loginBtn: Button
    lateinit var errorTxt: TextView
    lateinit var loginProgress: ProgressBar
    lateinit var saveCredentialsCB: CheckBox


    var rxEffectHandler =
        RxMobius.subtypeEffectHandler<LoginEffect, LoginEvent>()
            .add(GetSavedUserEffect::class.java, this::handleGetUserSavedCredentials)
            .add(SavedUserCredentialsEffect::class.java, this::handleSaveUserSavedCredentials)
            .add(LoginRequestEffect::class.java, this::handleLoginRequest)
            .add(GoToMainEffect::class.java, this::handleNavigateToMainScreen, AndroidSchedulers.mainThread())
            .build()


    val networkObservable: Observable<LoginEvent> = Observable.just(NetworkStateEvent(true))
    val eventSource: EventSource<LoginEvent> = RxEventSources.fromObservables(networkObservable)

    var loopFactory: MobiusLoop.Factory<LoginModel, LoginEvent, LoginEffect> =
        RxMobius
            .loop(LoginUpdate(), rxEffectHandler)
            .init {
                First.first(LoginModel(), setOf(GetSavedUserEffect))

            }
            .eventSource(eventSource)
            .logger(AndroidLogger.tag<LoginModel, LoginEvent, LoginEffect>("my-app"))

    private val controller: MobiusLoop.Controller<LoginModel, LoginEvent> =
        MobiusAndroid.controller(loopFactory, LoginModel())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        prefs = AppPrefs(activity?.getPreferences(Context.MODE_PRIVATE)!!)
        api = (activity!!.application as SampleApp).service
        navigator = AndroidNavigator(activity!!)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)
        loginText = view.findViewById(R.id.login)
        loginInput = view.findViewById(R.id.login_til)
        passwordInput = view.findViewById(R.id.password_til)
        passwordText = view.findViewById(R.id.password)
        loginBtn = view.findViewById(R.id.login_btn)
        errorTxt = view.findViewById(R.id.error_text)
        loginProgress = view.findViewById(R.id.login_progress)
        saveCredentialsCB = view.findViewById(R.id.save_credentials_cb)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        controller.connect(RxConnectables.fromTransformer(this::connectViews))
    }

    override fun onDestroy() {
        super.onDestroy()
        controller.disconnect()
    }

    override fun onResume() {
        super.onResume()
        controller.start()
    }

    override fun onPause() {
        super.onPause()
        controller.stop()
    }

    fun connectViews(models: Observable<LoginModel>): Observable<LoginEvent> {
        val disposable = models
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe{
                render(it)
            }

        val loginBtnclick = RxView.clicks(loginBtn)
            .map { LoginClickEvent as LoginEvent }

        val loginText = RxTextView.textChanges(loginText)
            .map { LoginInputEvent(it.toString()) as LoginEvent }

        val passText = RxTextView.textChanges(passwordText)
            .map{PassInputEvent(it.toString()) as LoginEvent}
        val saveCreds = RxCompoundButton.checkedChanges(saveCredentialsCB)
            .map { IsSaveCredentialsEvent(it) }

        return Observable
            .merge(listOf(loginBtnclick, loginText, passText, saveCreds))
            .doOnDispose(disposable::dispose)
    }

    fun render(state: LoginModel) {
        state.apply {
            setProgress(isLoading)
            setEnableLoginBtn(btnEnabled)
            setError(error)
            showLoginError(loginError)
            showPasswordError(passError)
        }
    }



    fun handleNavigateToMainScreen() {
        navigator.GoToMainScreen()
    }

    fun handleLoginRequest(request: Observable<LoginRequestEffect>): Observable<LoginEvent> {
        return request
            .flatMap {
                effect -> api.login(effect.login, effect.pass)
                .map { logged -> LoginResponseEvent(logged) as LoginEvent }
                .onErrorReturn{
                    LoginResponseErrorEvent(it)
                }.toObservable()
            }
    }


    fun handleSaveUserSavedCredentials(request: Observable<SavedUserCredentialsEffect>): Observable<LoginEvent> {
        return request.flatMap {
            effect -> prefs.saveUserSavedCredentails(effect.login, effect.pass)
            .map {
                saved -> UserCredentialsSavedEvent
            }.toObservable()
        }
    }



    fun handleGetUserSavedCredentials(request: Observable<GetSavedUserEffect>): Observable<LoginEvent> {
        return prefs.getUserSavedCredentials()
            .map { (login, pass) ->
                UserCredentialsLoadedEvent(
                    login, pass
                ) as LoginEvent
            }
            .toObservable()
            .onErrorReturn { return@onErrorReturn UserCredentialsErrorEvent(err = it)}
    }



    override fun setProgress(show: Boolean) {
        loginProgress.visibility = if(show) View.VISIBLE else View.GONE
    }

    override fun showPasswordError(errorText: String?) {
        errorText?.let { passwordInput.error = errorText } ?: run { passwordInput.error = "" } }


    override fun showLoginError(errorText: String?) {
        errorText?.let { loginInput.error = errorText } ?: run { loginInput.error = "" }
    }

    override fun setError(error: String?) {
        error?.let {
            errorTxt.visibility = View.VISIBLE
            errorTxt.text = error
            }?: run { errorTxt.visibility = View.GONE }
        }

    override fun hideKeyboard() {
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.hideSoftInputFromWindow(loginText.windowToken, 0)
    }

    override fun setEnableLoginBtn(enabled: Boolean) {
        loginBtn.isEnabled = enabled
    }
}

