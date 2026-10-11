# Bookmarks — 隐私目录衍生版

Android 应用：将 Intent 保存为有组织的书签集合。

本项目基于 [warren-bank/Android-Bookmarks](https://github.com/warren-bank/Android-Bookmarks)（分支 `v02`）修改，新增**单个密码保护的隐私目录**功能。

## 出处与许可

- **原项目**：[warren-bank/Android-Bookmarks](https://github.com/warren-bank/Android-Bookmarks)
- **原作者**：Warren Bank
- **原始许可**：[GPL-2.0](https://www.gnu.org/licenses/old-licenses/gpl-2.0.txt)（见 [LICENSE.txt](./LICENSE.txt)）
- **本衍生版许可**：GPL-2.0（与原版一致，传染性要求）
- **依赖库许可**：
  - `libs/PublicFileProvider` — Apache-2.0
  - `libs/HiddenApiBypass` — Apache-2.0
  - Gson — Apache-2.0

Apache-2.0 与 GPL-2.0 单向兼容，可在 GPL 项目中使用，各库许可证文本保留在其各自目录中。

## 衍生版改动

### 新增：密码保护的隐私目录

- 根目录下固定存在一个名为 `Private` 的文件夹（锁形图标），不可改名、移动、删除、隐藏；其下可建子文件夹。
- 首次进入时设置密码（输入两次确认），之后进入需输入密码；支持修改密码（需验证旧密码）。
- 密码**不明文存储**：随机 16 字节 salt + `SHA-256(salt + 密码)` 摘要 Base64 存入 SharedPreferences；口令比较采用常量时间比较。
- 解锁为**会话级**：离开隐私子树（返回上级）、主界面 `onPause`、ActionBar「锁定」按钮均会重新上锁。
- 数据库迁移版本 v3 → v4：`folders` 表新增 `is_private` 列，已有同名 `Private` 文件夹自动标记。

### 隐私目录的访问收口

- **全局搜索**排除隐私子树。
- **文件夹选择器**：未解锁时不显示私目录；在 SaveBookmark 页且已解锁时可选。
- **桌面快捷方式**：私目录内书签禁止添加桌面快捷方式。
- **文件夹移动**：禁止移入自身/子孙目录（防循环）；移入私目录需先解锁。
- **导入**：私目录内拦截导入并提示。
- **启动目录**：不允许指向私目录。

### 导出行为（与原版一致，不加密）

- HTML / JSON 导出格式与原版完全一致，**不加密**，包含私目录及其书签。
- 数据库备份（DB 格式）同样包含私目录，隐私书签为明文。

### 其他修复

- 修复中文翻译错误：`加添`→`添加`、`出口`→`导出/退出`、`英/温秀`→`隐藏/显示` 等。
- 文件夹/书签「移动」改为长按后直接弹出目标文件夹选择器，选定即移动。
- 包名改为 `wxh.github.bookmarks`（与原版 `com.github.warren_bank.bookmarks` 不冲突，可共存安装）。

### 已知限制

- 数据库备份/导出文件中隐私书签为明文（未加密）。
- 桌面快捷方式若已存在可能绕过口令（已禁止新建）。
- 忘记密码不可恢复（本版本不提供重置）。

## 构建

要求：JDK 17、Android SDK（platform 35、build-tools 35.0.0）、Gradle 8.9。

```bash
# 中文版（含全部语言翻译）
./gradlew :Bookmarks:assembleWithUserRuntimePermissionsWithAllLanguageTranslationsDebug
```

APK 输出路径：
`Bookmarks/build/outputs/apk/withUserRuntimePermissionsWithAllLanguageTranslations/debug/`

预构建的 APK 见 [APK/](./APK/) 目录。

## 最低支持版本

Android 2.3 Gingerbread (API 9)
