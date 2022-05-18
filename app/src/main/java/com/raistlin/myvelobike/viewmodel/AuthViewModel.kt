package com.raistlin.myvelobike.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.raistlin.myvelobike.api.ApiService
import com.raistlin.myvelobike.db.AppDb
import com.raistlin.myvelobike.dto.LoginData
import com.raistlin.myvelobike.store.saveAuthData
import com.raistlin.myvelobike.store.saveLoginData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val context = getApplication<Application>()
    private val dao = AppDb.getInstance(context).rideDao()
    private val service by lazy { ApiService(context, dao) }

    private val _status = MutableStateFlow<Status>(Status.None)
    val status: StateFlow<Status>
        get() = _status

    fun login(login: String, pin: String) = viewModelScope.launch {
        try {
            _status.value = Status.None
            val auth = service.login(login, pin)
            context.saveAuthData(auth)
            context.saveLoginData(LoginData(login, pin))
            _status.value = Status.Ok
        } catch (e: Throwable) {
            _status.value = Status.Error(e.message ?: "")
        }
    }

    override fun onCleared() {
        super.onCleared()
        service.close()
    }

    sealed class Status {
        object None : Status()
        object Ok : Status()
        class Error(val message: String) : Status()
    }
}
