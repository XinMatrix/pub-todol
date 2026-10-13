# TODOL

**一个全局悬浮窗记事 App。点一下就记，点一下就走。**

> A floating-window note app for Android. Tap to write, tap again to vanish — one motion, no app switching.

TODOL 是我（陈鑫 / XinMatrix）在大二（2023 年 5 月）用 Kotlin 手写的 Android 应用，从写下第一行代码起，我几乎每天用它记录待办与灵感，一直用到今天。它没赚过一分钱，但是我最成功的作品 —— 因为它解决的不是"记什么"，而是**记录的阻力**。

灵感来自福格行为模型（Fogg Behavior Model）：一个行为能否发生，不只取决于动机，还取决于阻力够不够小。所以 TODOL 把记录成本压到一个动作 —— 在任何界面（微信、相机、文章里）点一下悬浮球，输入框直接弹出、键盘唤起，写完再点一下，无感收起。

## 特性

- **全局悬浮窗**：不切 App、不打断手头的事，点一下唤起输入，再点一下收起
- **极速记录**：无标题、无目录、无分类，输入即保存
- **完全本地**：数据存在本机 SQLite，不联网、无服务器、无账号（Manifest 中没有 `INTERNET` 权限）
- **体积小巧**：仅几 MB
- 长短笔记切换、定时提醒、子集（子笔记）、多媒体附件、笔记统计、导出/导入

## 构建

```bash
# 环境：JDK 17 + Gradle 8.7（AGP 8.5）
./gradlew assembleRelease
```

| 项 | 值 |
|---|---|
| 语言 | Kotlin |
| minSdk | 28（Android 9+） |
| compileSdk / targetSdk | 34 |
| 版本 | 3.0 |
| 主要依赖 | AndroidX、Material Components、PermissionX、Glide、Gson |

> Android 14（targetSdk 34）要求前台服务声明类型，本项目已适配 `FOREGROUND_SERVICE_TYPE_DATA_SYNC`。若你改动悬浮球服务，请不要删掉 `startForeground(...)` 里的类型参数，否则在 Android 14+ 会崩溃。

## 下载

APK 见 [Releases](../../releases) 页面。

## 开源协议

[MIT](LICENSE)

---

作者：陈鑫 / XinMatrix · 官网 [xinmatrix.top](https://xinmatrix.top) · GitHub [@XinMatrix](https://github.com/XinMatrix)

这个项目是一个人从西安起步、用 AI 与手搓代码一点点试出来的真实记录的一部分。我的全部项目（含失败的）都公开在 [xinmatrix.top](https://xinmatrix.top)。

> `app/src/main/README.md` 保留了 2023 年参赛时的原始开发文档，`app/src/main/Bug` 是当时的开发笔记 —— 没有润色过，如果你想看一个 App 是怎么一点点长出来的，可以从那里读起。
