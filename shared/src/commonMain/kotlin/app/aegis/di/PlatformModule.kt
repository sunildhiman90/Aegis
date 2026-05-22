package app.aegis.di

import org.koin.core.module.Module

/**
 * Platform-specific module for platform dependencies (e.g., Database builder)
 */
expect val platformModule: Module
