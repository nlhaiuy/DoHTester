# DoHTester

测试安卓设备能不能连上各个 DoH（DNS over HTTPS）服务器的小应用。

## 功能

- 内置常用 DoH 服务器：Cloudflare、Google、Quad9、AdGuard、OpenDNS、Mullvad、阿里 DNS、DNSPod、360 DNS
- 可以添加自定义的 DoH 地址
- 选择查询类型（A / AAAA）和请求方式（GET / POST）
- 显示是否连通、耗时、使用的 HTTP 协议、解析结果和错误原因
- 可以单个测试，也可以一次全部测试

耗时包含 TCP 和 TLS 握手，因为每次测试都会新建连接。
DoH 服务器自己的域名由系统 DNS 解析，所以系统 DNS 完全不可用时，测试会失败。

## 构建

需要 JDK 17 和 Android SDK 34。

用 Android Studio 打开项目，直接运行即可。

命令行构建（需要装好 Gradle 8.7 以上）：

```
gradle assembleDebug
```

APK 在 `app/build/outputs/apk/debug/`。

仓库里的 GitHub Actions（`.github/workflows/build.yml`）会在每次推送后自动构建并上传 APK。

## 项目结构

- `DnsMessage.kt`：构造和解析 DNS 报文
- `DohTester.kt`：发送 DoH 请求（RFC 8484），内置服务器列表
- `MainViewModel.kt`：界面状态和测试流程
- `MainActivity.kt`：界面
