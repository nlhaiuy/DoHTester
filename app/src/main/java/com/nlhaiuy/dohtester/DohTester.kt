package com.nlhaiuy.dohtester

import android.util.Base64
import okhttp3.ConnectionPool
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class DohServer(val name: String, val url: String)

data class TestResult(
    val ok: Boolean,
    val millis: Long,
    val detail: String,
)

val defaultServers = listOf(
    DohServer("Cloudflare", "https://cloudflare-dns.com/dns-query"),
    DohServer("Google", "https://dns.google/dns-query"),
    DohServer("Quad9", "https://dns.quad9.net/dns-query"),
    DohServer("AdGuard", "https://dns.adguard-dns.com/dns-query"),
    DohServer("OpenDNS", "https://doh.opendns.com/dns-query"),
    DohServer("Mullvad", "https://dns.mullvad.net/dns-query"),
    DohServer("阿里 DNS", "https://dns.alidns.com/dns-query"),
    DohServer("DNSPod", "https://doh.pub/dns-query"),
    DohServer("360 DNS", "https://doh.360.cn/dns-query"),
)

object DohTester {

    private const val DNS_MESSAGE = "application/dns-message"

    private val baseClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .build()

    /** 发一次 DoH 查询。每次都新建连接，所以耗时包含 TCP 和 TLS 握手。 */
    fun test(url: String, domain: String, type: Int, usePost: Boolean): TestResult {
        val start = System.nanoTime()
        fun elapsed() = (System.nanoTime() - start) / 1_000_000

        return try {
            val query = DnsMessage.buildQuery(domain, type)
            val request = if (usePost) {
                Request.Builder()
                    .url(url.toHttpUrl())
                    .header("Accept", DNS_MESSAGE)
                    .post(query.toRequestBody(DNS_MESSAGE.toMediaType()))
                    .build()
            } else {
                val encoded = Base64.encodeToString(
                    query,
                    Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP,
                )
                Request.Builder()
                    .url(url.toHttpUrl().newBuilder().setQueryParameter("dns", encoded).build())
                    .header("Accept", DNS_MESSAGE)
                    .get()
                    .build()
            }

            val client = baseClient.newBuilder()
                .connectionPool(ConnectionPool(0, 1, TimeUnit.SECONDS))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.bytes() ?: ByteArray(0)
                val ms = elapsed()
                if (!response.isSuccessful) {
                    return TestResult(false, ms, "HTTP ${response.code}")
                }
                val parsed = DnsMessage.parse(body)
                val rcode = DnsMessage.rcodeText(parsed.rcode)
                val answers = parsed.answers.joinToString("\n") { "${it.data}（TTL ${it.ttl}）" }
                val protocol = response.protocol.toString().uppercase()
                val text = buildString {
                    append("$protocol，$rcode")
                    if (answers.isNotEmpty()) append("\n").append(answers)
                }
                TestResult(parsed.rcode == 0, ms, text)
            }
        } catch (e: Exception) {
            TestResult(false, elapsed(), e.message ?: e.javaClass.simpleName)
        }
    }
}
