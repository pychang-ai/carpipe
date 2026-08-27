# carpipe 工作進度

> NewPipe 公開 fork，客製為 PI 車上無廣告聽歌用（有線＋藍牙連音響，Android Auto 暫不處理）。
> 總管：time-master（`state/core-repos.md` 已登錄）。
> **盤點規則**：每次工作停下來，必須更新本表——逐項更新狀態、問題轉新任務、commit + push、並在 time-master `log/claude-ops.md` 追加一行。

## 任務清單

| # | 任務 | 內容 | 狀態 | 備註 |
|---|---|---|---|---|
| 0a | GitHub Token | PI 提供 | ✅ 2026/08/27 | Token 不存 repo |
| 0b | 開源方式＋命名 | 公開 Fork、GPLv3 合規、命名 carpipe | ✅ 2026/08/27 | PI 拍板 |
| 1 | 建 repo | Fork → carpipe → clone → upstream → .gitignore 金鑰防護 | ✅ 2026/08/27 | 預設分支 dev |
| 2 | TM 登錄 | PROGRESS.md＋core-repos.md 加列＋ops log | ✅ 2026/08/27 | |
| 3 | 建置驗證 | 不改 code，編譯原版 debug APK 成功 | ✅ 2026/08/27 | `app/build/outputs/apk/debug/app-debug.apk` 32.3 MB。JAVA_HOME 需指向 Android Studio JBR；`local.properties` 需自建（見下方問題記錄） |
| 4 | 循環鍵改造 | 主播放器大型三態循環鍵（關／單曲／全部）＋設定跨次記憶 | ⬜ | 對應上游 issue #6914、#11012 |
| 5 | 字級設定 | 設定頁加介面字級選項，文字與觸控目標同步放大 | ⬜ | 車上好按優先於好看 |
| 6 | 車上驗收 | 有線＋藍牙實測一週：循環、背景播放、大字操作 | ⬜ | 問題回填本表建新任務 |
| 7 | 測速警示整合討論 | PI 2026/08/27 提出：現用神盾測速需開兩個 app，評估與 carpipe 整合方案（A 內建警示模組／B 一鍵並行／C 先B後A 等） | 🔄 待 PI 拍板 | 台灣無現成開源測速 app；固定式測速點有警政署開放資料（data.gov.tw 7320、13940）。社群回報延伸評估 A1 本地標記／A2′ 本地標記＋雲端備份（建議）／A3 開放平台（自用不划算）。⛔ 不抓取神盾社群資料（他人營運資產，涉服務條款與法律風險） |
| 8 | 雲端備份同步討論 | PI 2026/08/27 提出：用 Google 帳戶備份、登入即同步。評估 B1 現成匯出／B2 匯出＋Dropbox 自動同步／B3 app 內建 Drive appDataFolder 同步（須用不含 Google Play 服務的 OAuth，以免破壞 NewPipe 去 Google 化架構）／B4 Android 系統自動備份 | 🔄 待 PI 拍板 | 建議先 B1＋B4 零成本擋著，實測後再決定 B3。測速標記併入同一資料庫即共用此備份，A2 可省掉自架後端。**B5 LINE 登入已評估否決**：LINE Login 只提供身分驗證（OpenID Connect，回傳 user ID／暱稱／頭像），無任何第三方 app 可用的檔案儲存 API；LINE Keep 雲端儲存已於 2024/08/28 終止，Keep 筆記的檔案約兩週失效，不能當備份。用 LINE 登入仍須自架後端存資料，等於白繞一圈。LINE Login 只在未來做 A3 開放社群平台時才有價值（台灣使用者登入門檻低）。**B6 Discord 已評估否決**：OAuth2 同樣只給身分不給儲存；且 Discord 自 2023 起 CDN 連結加簽章與到期時間（約 24 小時失效），官方明言檔案長期寄存請另尋服務，拿它當備份等於違反其定位且檔案會失聯。**其他已評估**：OneDrive（Graph API 有 app 專屬資料夾 `/me/special/approot`，若學校提供 Microsoft 365 教育版空間最大，但學校帳號有離職回收風險）／GitHub 私有 repo（契合 PI 既有 token 工作流、天然版本化）／Syncthing（零帳號零程式的點對點同步，最符合 NewPipe 去雲端精神，但兩端需同時上線）。**UX 原則（PI 2026/08/27 提問「登入後是否還要再設定 Dropbox」）**：不做「Google 登入＋Dropbox 儲存」的拼裝，那會變成兩次授權。改採單一儲存來源設計——設定頁選一個備份位置（Google Drive／Dropbox／本機資料夾），只授權一次，之後自動續期不再登入；且**絕不在開 app 時強制登入**，無帳號仍須完整可用（NewPipe 本來就零帳號，加強制登入是退步）。差異提醒：Drive 應用程式專屬資料夾**使用者看不到也拿不到檔案**，Dropbox 的 Apps 資料夾**可見可手動取用**，PI 若想自己抓備份檔應選 Dropbox 或改用可見資料夾權限 |

## 問題記錄（發現即建任務）

| 日期 | 問題 | 解法 | 狀態 |
|---|---|---|---|
| 2026/08/27 | 首次編譯失敗：`SDK location not found` | 新 clone 的專案沒有 `local.properties`（該檔在 .gitignore 內不進版控）。手動建立並寫入 `sdk.dir` 指向本機 Android SDK 路徑，Gradle 即找得到 SDK。換機時需重建此檔 | ✅ 已解 |
| 2026/08/27 | 系統預設 java 為 Oracle JDK，非 Android 專案適用 | 編譯前設 `JAVA_HOME` 指向 Android Studio 內建的 JBR（`C:\Program Files\Android\Android Studio\jbr`，OpenJDK 21） | ✅ 已解 |

## 決策記錄

| 日期 | 決策 | 理由 |
|---|---|---|
| 2026/08/27 | 方案 B：直接 fork NewPipe（不自建、不用官方 API） | 車用聽歌需求 NewPipe 大多內建，fork 改缺口最快 |
| 2026/08/27 | 公開 fork（不走私有 repo） | 一鍵同步上游、GPLv3 零疑慮、可回饋 PR；無需要藏的內容 |
| 2026/08/27 | 命名 carpipe | 跟隨 NewPipe 社群 pipe 系命名慣例，短好記 |
| 2026/08/27 | Android Auto 暫不處理 | PI 指示：待後續上車測試再議 |
