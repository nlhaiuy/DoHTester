package com.nlhaiuy.dohtester

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(vm: MainViewModel) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "DoH 连通性测试",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 40.dp),
                )

                OutlinedTextField(
                    value = vm.domain,
                    onValueChange = { vm.domain = it },
                    label = { Text("要查询的域名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = vm.recordType == DnsMessage.TYPE_A,
                        onClick = { vm.recordType = DnsMessage.TYPE_A },
                        label = { Text("A") },
                    )
                    FilterChip(
                        selected = vm.recordType == DnsMessage.TYPE_AAAA,
                        onClick = { vm.recordType = DnsMessage.TYPE_AAAA },
                        label = { Text("AAAA") },
                    )
                    FilterChip(
                        selected = !vm.usePost,
                        onClick = { vm.usePost = false },
                        label = { Text("GET") },
                    )
                    FilterChip(
                        selected = vm.usePost,
                        onClick = { vm.usePost = true },
                        label = { Text("POST") },
                    )
                }

                Button(onClick = { vm.testAll() }, modifier = Modifier.fillMaxWidth()) {
                    Text("全部测试")
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = vm.customUrl,
                        onValueChange = { vm.customUrl = it },
                        label = { Text("自定义 DoH 地址") },
                        placeholder = { Text("https://例子/dns-query") },
                        singleLine = true,
                        isError = vm.customError.isNotEmpty(),
                        supportingText = {
                            if (vm.customError.isNotEmpty()) Text(vm.customError)
                        },
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(onClick = { vm.addCustom() }) { Text("添加") }
                }
            }
        }

        itemsIndexed(vm.items, key = { _, it -> it.server.url }) { index, item ->
            ServerCard(
                item = item,
                onTest = { vm.testOne(index) },
                onRemove = { vm.remove(index) },
            )
        }

        item { Text("", modifier = Modifier.padding(bottom = 24.dp)) }
    }
}

@Composable
private fun ServerCard(item: ServerItem, onTest: () -> Unit, onRemove: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.server.name, fontWeight = FontWeight.Bold)
                    Text(
                        item.server.url,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val (label, color) = when (item.status) {
                    Status.IDLE -> "未测试" to MaterialTheme.colorScheme.onSurfaceVariant
                    Status.RUNNING -> "测试中…" to MaterialTheme.colorScheme.primary
                    Status.OK -> "正常 ${item.millis} ms" to Color(0xFF2E7D32)
                    Status.FAIL -> "失败" to MaterialTheme.colorScheme.error
                }
                Text(label, color = color, fontWeight = FontWeight.Medium)
            }

            if (item.detail.isNotEmpty()) {
                Text(item.detail, fontSize = 13.sp)
            }

            Row {
                TextButton(onClick = onTest, enabled = item.status != Status.RUNNING) {
                    Text("测试")
                }
                TextButton(onClick = onRemove) { Text("删除") }
            }
        }
    }
}
