# Mouse&mouse

Mouse&mouse 是一个面向 Minecraft 1.21.1 NeoForge 的客户端输入分流模组。它通过 Windows Raw Input 捕获真实键鼠设备数据，再把选中的设备输入注入到当前 Minecraft 实例中，用于多窗口、多套键鼠的本地协作或测试场景。

## 功能特性

- 支持为不同 Minecraft 窗口分别选择独立的键盘和鼠标设备。
- 设备选择只保存在当前实例内存中，关闭游戏后不会写入配置文件。
- 菜单界面使用虚拟鼠标光标，真实鼠标移动不会直接影响虚拟光标。
- 世界内输入会按选中设备进行路由，减少后开窗口抢占第一套设备的问题。
- 使用 `Alt + F8` 作为安全入口，可打开设备选择界面并临时释放原生鼠标捕获。
- 设备列表通过本机 IPC 从 splitter 进程实时读取，不依赖磁盘缓存文件。
- splitter 会尽量读取设备厂商名、产品名、VID 和 PID，便于区分同类设备。

## 运行环境

| 项目 | 要求 |
| --- | --- |
| Minecraft | `1.21.1` |
| Mod Loader | NeoForge `21.1.233` 或兼容版本 |
| Java | `21` |
| 系统 | Windows |
| 模组版本 | `1.0.0` |

该模组依赖 Windows Raw Input 和随包携带的 `splitter.exe`。Linux、macOS 以及没有 Raw Input 支持的环境目前不在适配范围内。

## 安装

1. 将构建产物放入对应整合包或客户端的 `mods` 目录。
2. 启动 Minecraft 1.21.1 NeoForge 客户端。
3. 进入游戏或菜单后按 `Alt + F8` 打开设备选择界面。
4. 勾选当前实例要接管的鼠标或键盘设备，点击 `Save` 生效。

当前构建产物名称：

```text
Mouse&mouse-NeoForge-1.0.0+1.21.1.jar
```

## 使用说明

如果要让两个窗口分别使用两套设备，建议先启动两个 Minecraft 实例，然后在每个窗口里分别按 `Alt + F8` 选择对应设备。选择结果存放在该实例的内存中，因此两个窗口之间不会共享已选设备。

设备选择保存后，模组会启用输入隔离。菜单界面由虚拟光标处理点击、拖动和滚轮；世界内则把选中设备的键鼠输入注入到当前窗口。未被选中的设备不会作为该实例的虚拟输入来源。

重启游戏后需要重新选择设备。这是刻意设计的行为，原因是 Raw Input 的设备句柄和枚举顺序可能随系统、插拔、驱动变化而变化，持久化旧设备 ID 容易导致选错设备或后开窗口输入异常。

## 安全键

`Alt + F8` 是当前的安全键组合。它用于打开设备选择界面，并在按住组合键时让 Minecraft 释放原生鼠标捕获，避免真实鼠标被长期锁在窗口内。

如果选择错误导致当前窗口无法正常操作，可以按 `Alt + F8` 重新进入设备选择界面，取消错误设备或重新选择。

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

## 技术原理

模组启动时会解压并启动随包携带的 `splitter.exe`。splitter 使用 Windows Raw Input 枚举键鼠设备，并在本机 `127.0.0.1:19091` 上提供 IPC 服务。

Minecraft 客户端通过 IPC 请求设备列表，并向 splitter 声明当前实例要接管的设备。收到这些设备的输入包后，Java 侧会把鼠标移动、按键、滚轮和键盘事件转换为 Minecraft 可处理的输入事件。

菜单界面不会直接使用真实鼠标坐标，而是维护一个虚拟光标位置。这样真实鼠标被系统或窗口捕获时，菜单仍能通过选中设备的数据进行稳定点击和拖动。

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
- 某些虚拟 HID、触控板或系统设备可能会出现在设备列表中，具体名称取决于驱动暴露的信息。
- splitter 使用固定本机端口 `19091`，如果端口被其他程序占用，设备列表和输入分流会失败。

## 许可证

本项目采用 GPL-3.0-only 许可证，完整条款见 [LICENSE](LICENSE)。
