package com.github.jonnyzzz.datagripdriversplugin

import com.intellij.database.dataSource.DatabaseDriver
import com.intellij.database.dataSource.artifacts.DatabaseArtifactLoader
import com.intellij.database.dataSource.artifacts.DatabaseArtifactManager
import com.intellij.database.dataSource.validation.DatabaseDriverValidator
import com.intellij.database.dataSource.validation.NamedProgressive
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task

object ArtifactDownloader {
    fun ensureArtifactsPreset(drivers: List<DatabaseDriver>) {
        DatabaseArtifactManager.getInstance().forceUpdate(null)
        val downloads = collectDownloadTasks(drivers)
        if (downloads.isNotEmpty()) {
            object : Task.Backgroundable(null, "Downloading drivers", true) {
                override fun run(indicator: ProgressIndicator) {
                    for (download in downloads) {
                        download.run(indicator)
                    }
                }
            }.queue()
        }
    }

    private fun collectDownloadTasks(drivers: List<DatabaseDriver>): List<NamedProgressive> {
        val downloads: MutableList<NamedProgressive> = ArrayList()
        for (driver in drivers) {
            collectDownloadTasks(driver, downloads)
        }
        return downloads
    }

    private fun collectDownloadTasks(driver: DatabaseDriver, downloads: MutableList<NamedProgressive>) {
        for (artifact in driver.artifacts) {
            collectDownloadTasks(driver, artifact, downloads)
        }
    }

    private fun collectDownloadTasks(driver: DatabaseDriver, artifact: DatabaseDriver.ArtifactRef, downloads: MutableList<NamedProgressive>) {
        val artifactVersion = DatabaseArtifactManager.resolveVersion(driver, artifact)
        if (artifactVersion != null) {
            if (!DatabaseArtifactLoader.getInstance().isValid(artifactVersion)) {
                downloads += DatabaseDriverValidator.createDownloadTask(artifactVersion, driver, null)
            }
        }
    }
}