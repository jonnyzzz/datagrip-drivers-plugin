package com.github.jonnyzzz.datagripdriversplugin

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.util.PathUtil
import java.nio.file.Files
import java.nio.file.Path

@Service(Service.Level.APP)
class PluginResourcesLocator {
    private val log = logger<PluginResourcesLocator>()

    fun locatePluginRoot() : Path? {
        class M

        val jar = try {
            PathUtil.getJarPathForClass(M::class.java).let(Path::of)
        } catch (e: Exception) {
            log.error("DataDrip drivers plugin cannot resolve it's root. ${e.message}", e)
            return null
        }

        if (!Files.isDirectory(jar)) {
            log.error("DataDrip drivers plugin cannot find it's root under $jar")
            return null
        }

        return jar
    }
}