<div align="center">
  <img src="src/main/resources/mouse_logo.png" width="144" alt="Mouse-mouse Logo">

  <h1>Mouse-mouse</h1>

  <p><strong>Minecraft 1.21.1 NeoForge 客户端输入分流模组</strong></p>
  <p>为本地多窗口、多套键鼠协作和测试场景提供独立设备选择、虚拟光标与 Raw Input 输入路由。</p>

  <p>
    <img alt="Minecraft" src="https://img.shields.io/badge/Minecraft-1.21.1-62B47A?style=for-the-badge">
    <img alt="NeoForge" src="https://img.shields.io/badge/NeoForge-21.1.233-EF6F3E?style=for-the-badge">
    <img alt="Java" src="https://img.shields.io/badge/Java-21-3A75B0?style=for-the-badge">
    <img alt="License" src="https://img.shields.io/badge/License-GPL--3.0--only-2E3440?style=for-the-badge">
  </p>
</div>

---

## 概览

Mouse-mouse 通过 Windows Raw Input 捕获真实键盘和鼠标设备数据，再将用户选中的设备输入注入到当前 Minecraft 实例中。它的目标不是替代 Minecraft 原生输入，而是在多窗口场景下把不同物理设备分配给不同实例，降低后开窗口抢占输入的问题。

| 能力 | 说明 |
| --- | --- |
| 独立设备选择 | 每个 Minecraft 实例可以选择自己的键盘和鼠标设备。 |
| 内存态配置 | 设备选择只保存在当前实例内存中，重启后重新选择。 |
| 虚拟光标 | 菜单界面由虚拟光标处理移动、点击、拖动和滚轮。 |
| 输入隔离 | 选中设备通过 IPC 注入，真实鼠标不会直接影响虚拟光标。 |
| 安全入口 | `Alt + F8` 打开设备选择界面，并临时释放原生鼠标捕获。 |
| 设备识别 | splitter 尽量显示厂商名、产品名、VID 和 PID。 |

## 快速开始

1. 将构建产物放入对应客户端或整合包的 `mods` 目录。
2. 启动 Minecraft `1.21.1` NeoForge 客户端。
3. 进入游戏或菜单后按 `Alt + F8` 打开设备选择界面。
4. 勾选当前窗口要接管的键盘或鼠标设备。
5. 点击 `Save`，当前实例立即启用输入隔离。

当前构建产物：

```text
Mouse-mouse-NeoForge-1.0.1+1.21.1.jar
```

## 运行环境

| 项目 | 要求 |
| --- | --- |
| Minecraft | `1.21.1` |
| Mod Loader | NeoForge `21.1.233` 或兼容版本 |
| Java | `21` |
| 操作系统 | Windows |
| 模组版本 | `1.0.1` |
| 本机端口 | `127.0.0.1:19091` |

> 该模组依赖 Windows Raw Input 和随包携带的 `splitter.exe`。Linux、macOS 以及没有 Raw Input 支持的环境暂不适配。

## 双窗口使用

如果要让两个 Minecraft 窗口分别使用两套设备，建议按下面的顺序操作：

1. 启动第一个 Minecraft 实例。
2. 启动第二个 Minecraft 实例。
3. 在第一个窗口按 `Alt + F8`，选择第一套键鼠。
4. 在第二个窗口按 `Alt + F8`，选择第二套键鼠。
5. 分别点击 `Save`，让两个实例各自建立 IPC 输入路由。

设备选择不会写入磁盘。这样设计是为了避免 Raw Input 设备句柄、枚举顺序或驱动信息变化后误选旧设备，也能让两个窗口保持实例级隔离。

## 安全键

`Alt + F8` 是当前安全键组合。

| 场景 | 作用 |
| --- | --- |
| 输入被隔离后 | 打开设备选择界面。 |
| 鼠标被窗口捕获时 | 临时释放原生鼠标捕获。 |
| 选错设备时 | 重新进入选择界面，取消或更换设备。 |

## 构建

普通构建：

```powershell
.\gradlew.bat jar
```

完整构建：

```powershell
.\gradlew.bat build
```

如果修改了 `splitter/splitter.cpp`，需要先重新编译 splitter：

```powershell
.\splitter\build.bat
.\gradlew.bat jar
```

`splitter/build.bat` 会把新的 `splitter.exe` 复制到 `src/main/resources/assets/mouse/splitter.exe`，随后 Gradle 会把它打入最终 JAR。

## 工作原理

```text
物理键鼠
   |
   v
Windows Raw Input
   |
   v
splitter.exe  -- IPC: 127.0.0.1:19091
   |
   v
Minecraft Client
   |
   +-- 世界内输入注入
   +-- 菜单虚拟光标路由
```

模组启动时会解压并启动随包携带的 `splitter.exe`。splitter 使用 Raw Input 枚举键鼠设备，并通过本机 IPC 向 Minecraft 客户端提供设备列表和输入数据。

Minecraft 客户端声明当前实例要接管的设备后，Java 侧会把这些设备的鼠标移动、按键、滚轮和键盘事件转换为 Minecraft 可处理的输入事件。菜单界面不直接使用真实鼠标坐标，而是维护一个虚拟光标位置，减少真实鼠标移动对菜单操作的干扰。

## 项目结构

```text
src/main/java/cn/kafei/mouse/       客户端输入分流、虚拟光标和 IPC 逻辑
src/main/java/cn/kafei/mouse/mixin/ Minecraft 输入处理相关 Mixin
src/main/resources/assets/mouse/    语言文件和 splitter.exe
src/main/templates/                 NeoForge 模组元数据模板
splitter/                           Windows Raw Input splitter 源码和构建脚本
```

## 已知限制

- 当前只面向 Windows 客户端。
- 设备选择不持久化，重启后需要重新选择。
- 某些虚拟 HID、触控板或系统设备可能出现在设备列表中，具体名称取决于驱动暴露的信息。
- splitter 使用固定本机端口 `19091`，端口被占用时设备列表和输入分流会失败。

## 许可证

本项目采用 GPL-3.0-only 许可证，完整条款见 [LICENSE](LICENSE)。
