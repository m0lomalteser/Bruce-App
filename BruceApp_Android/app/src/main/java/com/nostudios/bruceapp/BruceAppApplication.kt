package com.nostudios.bruceapp

import android.app.Application
import com.nostudios.bruceapp.data.local.AppDatabase

class BruceAppApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
}
