package com.actionanand.carechime.app

import android.app.Application

class CareChimeApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
