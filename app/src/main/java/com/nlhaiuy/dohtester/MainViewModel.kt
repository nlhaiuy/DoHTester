package com.nlhaiuy.dohtester

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Status { IDLE, RUNNING, OK, FAIL }

data class ServerItem(
    val server: DohServer,
    val status: Status = Status.IDLE,
    val millis: Long = 0,
    val detail: String = "",
)

class MainViewModel : ViewModel() {

    var domain by mutableStateOf("example.com")
    var recordType by mutableStateOf(DnsMessage.TYPE_A)
    var usePost by mutableStateOf(false)
    var customUrl by mutableStateOf("")
    var customError by mutableStateOf("")

    val items = mutableStateListOf<ServerItem>().apply {
        addAll(defaultServers.map { ServerItem(it) })
    }

    fun addCustom() {
        val url = customUrl.trim()
        if (!url.startsWith("https://")) {
            customError = "地址要以 https:// 开头"
            return
        }
        if (items.any { it.server.url == url }) {
            customError = "已经在列表里了"
            return
        }
        items.add(ServerItem(DohServer("自定义", url)))
        customUrl = ""
        customError = ""
    }

    fun remove(index: Int) {
        if (index in items.indices) items.removeAt(index)
    }

    fun testAll() {
        viewModelScope.launch {
            items.indices.map { i -> async { runOne(i) } }.awaitAll()
        }
    }

    fun testOne(index: Int) {
        viewModelScope.launch { runOne(index) }
    }

    private suspend fun runOne(index: Int) {
        val url = items.getOrNull(index)?.server?.url ?: return
        items[index] = items[index].copy(status = Status.RUNNING, detail = "")

        val result = withContext(Dispatchers.IO) {
            DohTester.test(url, domain.trim(), recordType, usePost)
        }

        // 列表可能在测试期间被改动，按地址重新定位
        val now = items.indexOfFirst { it.server.url == url }
        if (now >= 0) {
            items[now] = items[now].copy(
                status = if (result.ok) Status.OK else Status.FAIL,
                millis = result.millis,
                detail = result.detail,
            )
        }
    }
}
