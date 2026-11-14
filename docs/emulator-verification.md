# Android 运行时自验证环境实测与操作手册

本文档记录在无显示器、无硬件虚拟化加速（无 `/dev/kvm`）的 Linux 环境下，搭建 Android Emulator 运行时自验证环境的全过程、排坑指南、实测性能数据、可行性结论及替代方案调研。

---

## 1. 环境现状与事实基准

- **宿主系统**：Linux 7.0.0-44-generic x86_64（AMD EPYC 9D64 88-Core Processor，分配 4 vCPU）。
- **显示环境**：无头环境（Headless，无 X11/Wayland 物理显示器）。
- **硬件虚拟化**：`/dev/kvm` 不存在，宿主机未透传 CPU 硬件虚拟化标志（SVM/VMX），仅能使用 QEMU TCG（Tiny Code Generator）纯软件动态二进制翻译。
- **内存与磁盘**：RAM 约 7.7 GiB（可用常态约 1.5 ~ 2.0 GiB），磁盘可用约 148 GiB。
- **目标工程**：OmniFlow（`compileSdk = 36`, `targetSdk = 35`, `minSdk = 26`）。

---

## 2. SDK 组件安装与环境配置步骤

### 2.1 环境变量配置 (`~/.bashrc`)

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
export ANDROID_SDK_ROOT="$HOME/Android/Sdk"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
export JAVA_TOOL_OPTIONS="-Djava.net.preferIPv4Stack=true"
```

### 2.2 核心踩坑与系统级修复

1. **Java CA 证书缺失导致 HTTPS 下载中断**：
   - **现象**：JVM 发起 HTTPS 请求时抛出 `InvalidAlgorithmParameterException: the trustAnchors parameter must be non-empty`，`cacerts` 为断开的软链接。
   - **修复**：
     ```bash
     mkdir -p /etc/ssl/certs/java && update-ca-certificates -f
     ```
2. **Egress Proxy 与 Java IPv6 握手失败**：
   - **现象**：网络代理环境对无认证/IPv6 隧道返回拦截报文，导致 `HttpURLConnection.doTunneling0` 抛出 `NoSuchElementException`。
   - **修复**：配置 `JAVA_TOOL_OPTIONS="-Djava.net.preferIPv4Stack=true"` 强制 JVM 走 IPv4 通道。
3. **root 与 home 目录差异**：
   - **现象**：`avdmanager` 默认向 `/root/.android/avd` 创建 AVD，而普通会话可能指向 `/home/hatch/.android`。
   - **修复**：软链接保持一致：
     ```bash
     ln -sf /home/hatch/.android /root/.android
     ```

### 2.3 SDK 组件补齐命令

```bash
# 1. 接受所有许可
yes | sdkmanager --licenses

# 2. 安装必要组件（已安装项自动跳过）
sdkmanager "platform-tools" \
           "emulator" \
           "platforms;android-35" \
           "system-images;android-35;default;x86_64"
```

---

## 3. AVD 创建与配置调优

### 3.1 创建 AVD

```bash
echo "no" | avdmanager create avd \
    -n omniflow_avd \
    -k "system-images;android-35;default;x86_64" \
    --force
```

### 3.2 针对无加速与低内存环境的 `config.ini` 调优

文件路径：`~/.android/avd/omniflow_avd.avd/config.ini`

```ini
# 限制 AVD 内存，防止 7GB 宿主 VM 发生 OOM Crash
hw.ramSize=1536M
vm.heapSize=256M
disk.dataPartition.size=2048M

# CPU 与图形渲染配置
hw.cpu.ncore=2
hw.gpu.enabled=yes
hw.gpu.mode=swiftshader_indirect

# 屏幕分辨率调优为轻量级（降低显存与渲染负载）
hw.lcd.width=720
hw.lcd.height=1280
hw.lcd.density=320
```

---

## 4. 模拟器无头启动、关闭与操作命令

### 4.1 无头启动命令

```bash
emulator -avd omniflow_avd \
    -no-window \
    -no-audio \
    -no-boot-anim \
    -accel off \
    -gpu swiftshader_indirect
```

### 4.2 引导完成检测

```bash
adb wait-for-device shell 'while [[ "$(getprop sys.boot_completed | tr -d "\r")" != "1" ]]; do sleep 5; done && echo "Boot completed"'
```

### 4.3 构建、装包与启动 App

```bash
# 构建 Debug APK
./gradlew :app:assembleDebug

# 安装 APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 启动 OmniFlow 主界面
adb shell am start -n com.omniflow.android/.MainActivity
```

### 4.4 截屏获取

```bash
# 直接截屏并保存到指定目录
adb exec-out screencap -p > ~/workspace/goals/omni-flow/hidden_files/emulator-check/main_screen.png
```

### 4.5 优雅关闭与强制终止

```bash
# 优雅关闭
adb emu kill

# 若无响应则强制清理进程
killall -9 qemu-system-x86_64-headless
```

---

## 5. 实测性能记录与时间盒测试（20分钟限时）

在完全清空的 cold boot 条件下，启动命令发出后监控各阶段耗时：

| 阶段节点 | 绝对时刻 | 累计耗时 | 状态观察 |
| :--- | :--- | :--- | :--- |
| **启动发出** | 23:21:20 | 0分00秒 | QEMU TCG 进程启动，加载 system.img |
| **ADB 连通** | 23:24:25 | 3分05秒 | 设备从 `offline` 转为 `device` |
| **开机动画停止** | 23:26:12 | 4分52秒 | `init.svc.bootanim=stopped`，进入核心系统服务装载 |
| **BootPhase 200** | 23:28:03 | 6分43秒 | 系统服务进入基础就绪阶段 |
| **ArtService 预检** | 23:33:15 | 11分55秒 | 预装应用 dexopt/verify 逐个校验 |
| **BootPhase 600** | 23:35:23 | 14分03秒 | 核心服务就绪，允许启动三方应用 |
| **Launcher3 绑定** | 23:37:12 | 15分52秒 | ActivityManager 绑定桌面进程 |
| **20分钟时间盒截止** | 23:41:20 | **20分00秒** | **`sys.boot_completed` 仍未置 1**，截屏全黑 |

### 关键资源消耗事实
1. **CPU 负载**：QEMU 进程全程持续占满 100% ~ 120% 单/双核算力。
2. **内存激增**：QEMU 运行至 18 分钟时物理常驻内存（RSS）达到 **3.4 GiB**。
3. **系统余量危机**：宿主总内存 7.7 GiB，扣除系统守护进程与 QEMU 后，可用物理内存跌破 **250 MiB**，Swap 消耗超 400 MiB。在模拟器运行期间，宿主机无法再并发执行 `./gradlew :app:assembleDebug`（Gradle JVM 至少申请 1~2 GiB），否则必触发 Linux 内核 OOM Killer 强杀进程。

---

## 6. 可行性结论

> **最终结论：在当前 Linux 无硬件虚拟化（无 `/dev/kvm`）虚拟机上，使用完整 Android 模拟器作为常规自验证手段【不可行】。**

### 核心不可行原因：
1. **启动耗时超标**：受制于 QEMU TCG 软件指令转译，单次冷启动耗时超过 20 分钟仍未完成桌面就绪，无法融入日常的快速迭代闭环。
2. **内存严重冲突**：模拟器单体耗费 3.4 GiB 内存，导致宿主机无法同时执行 Gradle 构建与 ADB 操作，极易引发 OOM 崩溃或系统卡死。
3. **不可预期渲染延迟**：软件渲染（SwiftShader）在转译模式下帧率极低，截屏获取耗时极长且容易出现未刷新完成的黑屏。

---

## 7. 推荐替代方案调研（JVM 侧自验证）

为了满足“改动后能够自己跑起来验证 UI 与交互效果，而不依赖实体模拟器”的目标，建议未来采用 **JVM 侧截图与组件验证方案**（调研结论，当前不修改工程依赖）：

### 方案 A：Paparazzi (Cash App) —— 推荐首选
- **原理**：直接利用 Android Studio 内部的 LayoutLib 在宿主 JVM 上渲染 Compose 界面为 PNG 图像，不依赖 Android 设备、模拟器或 ADB。
- **性能**：单张 UI 渲染耗时通常在 **100 ~ 300 毫秒** 之间，整套页面回归仅需几秒。
- **资源**：仅需标准 JVM 内存（~512MB），无额外后台常驻进程。
- **适用场景**：验证页面浅色/深色主题、多语言、不同屏幕尺寸下的布局裁切、空态/错误态等 UI 验收。

### 方案 B：Robolectric + Roborazzi
- **原理**：基于 Robolectric 模拟 Android 运行时环境，结合 Roborazzi 进行 Compose 截图与点击交互测试。
- **性能**：单测试用例执行耗时约 **1 ~ 3 秒**。
- **能力优势**：不仅可以截图，还支持模拟点击、输入、导航状态跳转等交互逻辑自验证。

### 方案对比与建议落地路线
对于 OmniFlow 这样业务与 UI 清晰解耦（`core` 纯逻辑 + `app` Jetpack Compose）的工程：
- 业务与状态逻辑：继续由 `./gradlew :core:testDebugUnitTest` 保障；
- 视觉与交互自验证：后续如需引入免模拟器的快速截图检验，推荐评估配置 **Paparazzi** 或 **Roborazzi**，由 Agent 在本地执行单一 Gradle 测试任务直接输出 PNG 图像进行比对与查看。
