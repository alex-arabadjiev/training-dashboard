package com.example.trainingdashboard

import android.app.Application
import com.example.trainingdashboard.data.db.AppDatabase

class TrainingApp : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }
}
