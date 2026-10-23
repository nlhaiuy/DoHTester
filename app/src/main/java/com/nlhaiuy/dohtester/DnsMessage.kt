package com.nlhaiuy.dohtester

import java.io.ByteArrayOutputStream
import java.net.InetAddress

/** DNS 报文的简单构造与解析（只支持 A、AAAA、CNAME 的展示）。 */
object DnsMessage {

    const val TYPE_A = 1
    const val TYPE_CNAME = 5
    const val TYPE_AAAA = 28

    data class Answer(val name: String, val type: Int, val ttl: Long, val data: String)

    data class Response(val rcode: Int, val answers: List<Answer>)

    /** 构造一个递归查询。按 RFC 8484 的建议，ID 填 0。 */
    fun buildQuery(domain: String, type: Int): ByteArray {
        val out = ByteArrayOutputStream()
        writeU16(out, 0)       // ID
        writeU16(out, 0x0100)  // 标志：RD
        writeU16(out, 1)       // 问题数
        writeU16(out, 0)       // 回答数
        writeU16(out, 0)       // 权威数
        writeU16(out, 0)       // 附加数

        val labels = domain.trim().trimEnd('.').split('.')
        for (label in labels) {
            val bytes = label.toByteArray(Charsets.US_ASCII)
            require(bytes.isNotEmpty() && bytes.size <= 63) { "域名格式不对" }
            out.write(bytes.size)
            out.write(bytes)
        }
        out.write(0)
        writeU16(out, type)
        writeU16(out, 1)       // 类：IN
        return out.toByteArray()
    }

    fun parse(buf: ByteArray): Response {
        require(buf.size >= 12) { "响应太短" }
        val rcode = u16(buf, 2) and 0x0F
        val questions = u16(buf, 4)
        val answers = u16(buf, 6)

        var pos = 12
        repeat(questions) {
            pos = readName(buf, pos).second + 4
        }

        val list = mutableListOf<Answer>()
        repeat(answers) {
            val (name, afterName) = readName(buf, pos)
            pos = afterName
            require(pos + 10 <= buf.size) { "响应被截断" }
            val type = u16(buf, pos)
            val ttl = u32(buf, pos + 4)
            val length = u16(buf, pos + 8)
            pos += 10
            require(pos + length <= buf.size) { "响应被截断" }

            val data = when (type) {
                TYPE_A, TYPE_AAAA -> InetAddress.getByAddress(buf.copyOfRange(pos, pos + length)).hostAddress ?: ""
                TYPE_CNAME -> readName(buf, pos).first
                else -> "类型 $type"
            }
            list += Answer(name, type, ttl, data)
            pos += length
        }
        return Response(rcode, list)
    }

    fun rcodeText(rcode: Int): String = when (rcode) {
        0 -> "NOERROR"
        1 -> "FORMERR"
        2 -> "SERVFAIL"
        3 -> "NXDOMAIN"
        4 -> "NOTIMP"
        5 -> "REFUSED"
        else -> "RCODE $rcode"
    }

    /** 读取域名，处理压缩指针。返回（域名，名字之后的位置）。 */
    private fun readName(buf: ByteArray, start: Int): Pair<String, Int> {
        val sb = StringBuilder()
        var pos = start
        var end = -1
        var jumps = 0
        while (true) {
            require(pos < buf.size) { "域名越界" }
            val len = buf[pos].toInt() and 0xFF
            if (len == 0) {
                pos += 1
                break
            }
            if (len and 0xC0 == 0xC0) {
                require(pos + 1 < buf.size) { "域名越界" }
                val target = ((len and 0x3F) shl 8) or (buf[pos + 1].toInt() and 0xFF)
                if (end < 0) end = pos + 2
                pos = target
                jumps++
                require(jumps < 20) { "域名指针循环" }
                continue
            }
            require(pos + 1 + len <= buf.size) { "域名越界" }
            if (sb.isNotEmpty()) sb.append('.')
            sb.append(String(buf, pos + 1, len, Charsets.US_ASCII))
            pos += 1 + len
        }
        return sb.toString() to (if (end >= 0) end else pos)
    }

    private fun writeU16(out: ByteArrayOutputStream, v: Int) {
        out.write((v shr 8) and 0xFF)
        out.write(v and 0xFF)
    }

    private fun u16(b: ByteArray, i: Int): Int =
        ((b[i].toInt() and 0xFF) shl 8) or (b[i + 1].toInt() and 0xFF)

    private fun u32(b: ByteArray, i: Int): Long =
        (u16(b, i).toLong() shl 16) or u16(b, i + 2).toLong()
}
