package com.captionai.studio

import android.app.Application
import com.captionai.studio.storage.PrefsManager
import com.captionai.studio.utils.AdManager

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PrefsManager.init(this)
        AdManager.initialize(this)
    }
}
