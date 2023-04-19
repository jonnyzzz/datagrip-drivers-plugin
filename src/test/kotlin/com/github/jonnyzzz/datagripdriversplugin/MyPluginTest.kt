package com.github.jonnyzzz.datagripdriversplugin

import com.intellij.openapi.components.service
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import junit.framework.TestCase

@TestDataPath("\$CONTENT_ROOT/src/test/testData")
class MyPluginTest : BasePlatformTestCase() {
    fun testProjectService() {
        val locator = project.service<PluginResourcesLocator>()

        val root = locator.locatePluginRoot()
        TestCase.assertNotNull("plugin should compute it's root", root)
    }

    override fun getTestDataPath() = "src/test/testData/"
}
