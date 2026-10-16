package com.harish.gallery.core.data.di

import javax.inject.Qualifier

/** A coroutine scope that lives as long as the app (for work that must outlive a screen). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
