# Pocket Screen Guard

**English** | [简体中文](#简体中文)

An Xposed module that automatically turns off the screen when the proximity sensor detects the phone is in a pocket or bag.

## Background

When **"Don't use lock screen"** is enabled in Android Developer Options, pressing the power button wakes the screen **directly to the last app** with no lock screen protection.

**The problem:**
1. Use WeChat → lock screen → put phone in pocket
2. Something in the pocket wakes the screen (accidental power button press, notification, etc.)
3. Screen wakes directly to WeChat with no lock screen — pocket touches trigger real actions (sending messages, etc.)

## How It Works

Hooks into SystemUI and monitors the proximity sensor. When the screen wakes up and the sensor detects the phone is covered (in a pocket or face-down), it immediately calls `PowerManager.goToSleep()` to turn the screen back off.

A configurable delay (0–3000ms, default 1000ms) prevents accidentally re-locking when you intentionally unlock your phone.

## Requirements

- Android 8.0+
- LSPosed / EdXposed
- Scope: **SystemUI** (`com.android.systemui`)
- Developer Options → **"Don't use lock screen"** enabled

## Installation

1. Download the APK from [Releases](../../releases)
2. Install the APK
3. Enable the module in LSPosed with scope set to **SystemUI**
4. Reboot

## Settings

| Setting | Description | Default |
|---------|-------------|---------|
| Enable Module | Master toggle | On |
| Detection Delay | Wait time after screen on before proximity monitoring starts | 1000ms |

---

## 简体中文

**[English](#pocket-screen-guard)** | 简体中文

一个 Xposed 模块，当距离传感器检测到手机在口袋或包里时，自动关闭屏幕。

## 背景

启用了 Android 开发者选项中的**「不使用锁屏」**后，按电源键亮屏会**直接回到上次使用的应用**，没有任何锁屏保护。

**问题场景：**
1. 用完微信 → 锁屏 → 放进口袋
2. 口袋里某些原因唤醒了屏幕（误触电源键、通知等）
3. 屏幕直接亮到微信界面，没有锁屏保护 → 口袋里的误触变成真实操作（发消息等）

## 工作原理

hook 进 SystemUI，监听距离传感器。屏幕亮起后如果传感器检测到手机被遮挡（在口袋里或面朝下），立即调用 `PowerManager.goToSleep()` 关闭屏幕。

可设定检测延迟（0–3000ms，默认 1000ms），防止主动解锁时误触发。

## 要求

- Android 8.0+
- LSPosed / EdXposed
- 作用域：**SystemUI** (`com.android.systemui`)
- 开发者选项 → **「不使用锁屏」** 已开启

## 安装

1. 从 [Releases](../../releases) 下载 APK
2. 安装 APK
3. 在 LSPosed 中启用模块，作用域选择 **SystemUI**
4. 重启手机

## 设置说明

| 设置 | 说明 | 默认值 |
|------|------|--------|
| 启用模块 | 总开关 | 开启 |
| 检测延迟 | 亮屏后等待多久再开始检测（防止主动解锁时误触发） | 1000ms |

## License

[MIT](LICENSE)
