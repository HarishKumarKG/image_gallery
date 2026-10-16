package com.harish.gallery

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Starts Hilt (dependency injection) for the whole app. */
@HiltAndroidApp
class GalleryApp : Application()
