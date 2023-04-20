package com.github.jonnyzzz.datagripdriversplugin

import com.intellij.database.dataSource.*
import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.util.xml.dom.createXmlStreamReader
import com.thoughtworks.xstream.io.HierarchicalStreamReader
import com.thoughtworks.xstream.io.xml.QNameMap
import com.thoughtworks.xstream.io.xml.StaxReader
import java.io.InputStream

object DataSourceRegistrar {
    fun performRegistration() {
        ensureArtifactsPresent()
        loadDataSources(DataSourceStorage.getStorage())
    }

    private fun loadDataSources(storage: DataSourceStorage) {
        val dataSources = LinkedHashMap<String, LocalDataSource>()
        loadDataSources(storage, dataSources)
        for (ds in dataSources.values) {
            if (storage.getDataSourceById(ds.uniqueId) != null) {
                storage.updateDataSource(ds)
            } else {
                storage.addDataSource(ds)
            }
        }
    }

    private fun loadDataSources(storage: DataSourceStorage, res: MutableMap<String, LocalDataSource>
    ) {
        for (extension in CONFIG_EP.extensions) {
            for (url in ConfigUrlBean.enumerateUrls(extension)) {
                url.openStream().use { stream ->
                    loadDataSources(stream, storage, res)
                }
            }
        }
    }

    private fun loadDataSources(stream: InputStream, storage: DataSourceStorage, res: MutableMap<String, LocalDataSource>) {
        val reader = StaxReader(QNameMap(), createXmlStreamReader(stream))
        try {
            while (reader.hasMoreChildren()) {
                reader.moveDown()
                loadDataSource(reader, storage, res)
                reader.moveUp()
            }
        }
        finally {
            reader.close()
        }
    }

    private fun loadDataSource(reader: HierarchicalStreamReader, storage: DataSourceStorage, res: MutableMap<String, LocalDataSource>) {
        if (reader.nodeName != "data-source") return
        val uuid = reader.getAttribute("uuid")
        if (uuid != null) {
            val ds = res[uuid] ?: storage.getDataSourceById(uuid) ?: LocalDataSource()
            LocalDataSourceSerialization.deserialize(null, ds, reader, LocalDataSourceSerialization.SaveMode.ALL)
            ds.resolveDriver()
            res[ds.uniqueId] = ds
        }
    }

    val CONFIG_EP = ExtensionPointName.create<ConfigUrlBean>("com.intellij.database.extraDataSources")

    private fun ensureArtifactsPresent() {
        val managedDrivers = DatabaseDriverManager.getInstance().drivers.filter { isAutoManaged(it) }
        ArtifactDownloader.ensureArtifactsPreset(managedDrivers)
    }

    private fun isAutoManaged(driver: DatabaseDriver) =
        driver.isPredefined && driver.getAdditionalProperty("auto-managed") == "true"

    class Listener: AppLifecycleListener {
        override fun appFrameCreated(commandLineArgs: MutableList<String>) {
            ApplicationManager.getApplication().executeOnPooledThread {
                performRegistration()
            }
        }
    }

}
