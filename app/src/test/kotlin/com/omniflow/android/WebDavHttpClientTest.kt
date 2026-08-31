package com.omniflow.android

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

class WebDavHttpClientTest {
    private lateinit var server: MockWebServer

    @BeforeTest
    fun setUp() {
        server = MockWebServer().apply { start() }
    }

    @AfterTest
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun sendsCustomWebDavMethods() {
        val client = WebDavHttpClient(OkHttpClient(), credentials = { "" to "" })
        server.enqueue(MockResponse().setResponseCode(201))
        server.enqueue(MockResponse().setResponseCode(207))
        val url = server.url("/backups/").toString()

        client.execute(url, "MKCOL").use { assertEquals(201, it.code) }
        client.execute(
            url = url,
            method = "PROPFIND",
            headers = mapOf("Depth" to "1"),
            body = "<propfind xmlns=\"DAV:\" />",
        ).use { assertEquals(207, it.code) }

        assertEquals("MKCOL", server.takeRequest().method)
        val propfind = server.takeRequest()
        assertEquals("PROPFIND", propfind.method)
        assertEquals("1", propfind.getHeader("Depth"))
        assertEquals("<propfind xmlns=\"DAV:\" />", propfind.body.readUtf8())
    }
}
