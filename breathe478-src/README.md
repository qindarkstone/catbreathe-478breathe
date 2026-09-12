# 小猫呼吸 · 4-7-8 呼吸法 Android App —— 源码

一个纯 Android 框架 API（无 AndroidX、无第三方依赖）的 4-7-8 呼吸放松 App 完整源码。
呼吸主体为「丝滑抠图」的小猫（透明背景），随呼吸节奏缩放，并按阶段切换神情；每个呼吸阶段结束播放提示音。

## 版本说明
- 应用名称：**小猫呼吸**
- versionCode = 6，versionName = 6.0（对应已交付的 v7 去水印安装包）
- 最低支持：Android 5.0 (API 21)
- 本源码与已交付 APK `breathe478-v7-nomark-*.apk` 完全对应（图标已去除“AI生成”水印）

## 目录结构
```
breathe478-src/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/tabbit/breathe478/
│   │   ├── MainActivity.java      # 界面与交互（标题、状态、按钮、呼吸小课堂卡片）
│   │   └── BreathingView.java     # 呼吸动画自定义 View（4-7-8 状态机 + 抠图小猫 + 提示音）
│   └── res/
│       ├── values/strings.xml     # app_name = 小猫呼吸
│       ├── values/styles.xml      # 深色主题
│       ├── drawable-nodpi/        # cat_inhale / cat_hold / cat_exhale（透明背景抠图）
│       └── mipmap-*/ic_launcher.png  # 启动图标（5 档密度，已去水印）
├── assets/cat/                    # 原始小猫素材（6 张源图 + 去水印图标源图）
├── tools/
│   ├── cutout.py                  # 抠图脚本（blueness 阈值 + 最大连通域 + 侵蚀 + 羽化）
│   ├── prep_assets.py             # 资源生成脚本（drawable 抠图 + mipmap 图标）
│   └── gen_icons.py               # 早期图标生成脚本
├── build.sh                       # 命令行构建脚本
└── README.md
```

## 用 Android Studio 打开
1. Android Studio → Open → 选择本目录（或其中的 `app`）。
2. 若提示缺少 Gradle 配置，可新建一个 Empty Views Activity 工程（包名 `com.tabbit.breathe478`），
   把本项目的 `app/src/main` 内容复制进去即可。
3. 本工程只使用 Android 框架 API，无需额外依赖。

## 命令行构建（无 Android Studio）
需要：JDK 8+（或更高）、Android SDK 的 build-tools 与 platform android-34。
```bash
# 1) 生成签名密钥（示例）
keytool -genkeypair -keystore debug.keystore -alias androiddebugkey \
  -storepass android -keypass android -keyalg RSA -keysize 2048 -validity 10000 \
  -dname "CN=Android Debug,O=Android,C=US"

# 2) 构建（脚本内的 SDK 路径请按本机调整）
bash build.sh   # 产物：build/breathe478.apk
```
`build.sh` 依次执行：aapt2 编译资源 → aapt2 链接 → javac 编译 → d8 生成 dex →
打包 classes.dex → zipalign 对齐 → apksigner 签名 → 校验。

## 自定义
- 呼吸节奏：修改 `BreathingView.java` 中 `DURATIONS = {4000L, 7000L, 8000L}`（毫秒）。
- 缩放幅度：`MIN_SCALE` / `MAX_SCALE`。
- 提示音：`BreathingView.playCue()` 中的 `ToneGenerator` 音型与时长。
- 配色：`MainActivity.java` 与 `BreathingView.java` 中的颜色常量。
- 重新生成抠图/图标：把 `tools/*.py` 中的源图与输出路径改为本机路径后运行。

## 说明
本应用仅用于呼吸放松练习辅助，不构成任何医疗建议。如有健康问题请咨询专业医生。
