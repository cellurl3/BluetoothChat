package com.glodanif.bluetoothchat.ui.presenter

import android.support.annotation.ColorInt
import com.glodanif.bluetoothchat.data.model.SettingsManager
import com.glodanif.bluetoothchat.di.ComponentsManager
import com.glodanif.bluetoothchat.ui.view.ProfileView
import javax.inject.Inject

class ProfilePresenter(private val view: ProfileView) {

    @Inject
    lateinit var settings: SettingsManager

    init {
        ComponentsManager.getDataSourceComponent().inject(this)
    }

    @ColorInt
    private var currentColor = settings.getUserColor()
    private var currentName = settings.getUserName()

    fun saveUser() {
        if (!currentName.isEmpty() && currentName.length <= 25 && !currentName.contains("#")) {
            settings.saveUserName(currentName.trim())
            settings.saveUserColor(currentColor)
            view.redirectToConversations()
        } else {
            view.showNotValidNameError()
        }
    }

    fun prepareColorPicker() {
        view.showColorPicker(currentColor)
    }

    fun onColorPicked(@ColorInt color: Int) {
        currentColor = color
        view.showUserData(currentName, color)
    }

    fun onNameChanged(name: String) {
        currentName = name.replace("\\s{2,}".toRegex(), " ")
        view.showUserData(currentName, currentColor)
    }

    fun loadSavedUser() {
        view.prefillUsername(currentName)
        view.showUserData(currentName, currentColor)
    }
}
