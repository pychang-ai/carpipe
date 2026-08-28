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
| 4 | 循環鍵改造 | 主播放器大型三態循環鍵（關／單曲／全部）＋設定跨次記憶 | ✅ 2026/08/27 編譯通過，待實機驗收 | 對應上游 issue #6914、#11012。改動：`player.xml` 播放列加 `repeatButtonMain`；`VideoPlayerUi` 綁定點擊與圖示同步（關閉時半透明）；`Player.cycleNextRepeatMode()` 寫入偏好、`initPlayback()` 讀回；`settings_keys.xml` 新增 `last_repeat_mode`；popup 播放器隱藏該鍵 |
| 5 | 字級設定 | 設定頁加介面字級選項，文字與觸控目標同步放大 | ✅ 2026/08/27 編譯通過，待實機驗收 | 車上好按優先於好看。做法：新增 `util/UiScaleHelper.java`，以覆寫 Configuration 的 densityDpi 放大整個介面（文字與按鈕等比例一起變大，等同系統「顯示大小」），並同步縮減 screenWidthDp 避免誤選平板版面。外觀設定頁新增「Interface scale」四段（100／115／130／150%），改選後立即 recreate。掛載點：MainActivity／SettingsActivity／PlayQueueActivity／PlayerService 的 attachBaseContext |
| 6 | 車上驗收 | 有線＋藍牙實測一週：循環、背景播放、大字操作、測速警示 | ⬜ **改排在任務 7、8 之後**（PI 2026/08/27 指示：兩項完成後才上車測試） | 問題回填本表建新任務 |
| 9 | **測試規格** | PI 2026/08/28 提問：專案是否含軟體測試規格。答：先前**沒有**，已補。功能對測試案例對照共 40 案（A 已實作 19／B 承襲回歸 9／C 待開發 12）→ `docs/test-plan.html`。基準線：上游 155 個單元測試案例首次執行，全數通過零失敗。**兩項自訂功能仍零自動測試覆蓋**，補齊約需 2 天 | ✅ 2026/08/28 自動化部分完成 | PI 2026/08/28 核准。已完成：7 個自動測試（`UiScaleHelperTest`、`PlayerHelperRepeatModeTest`）全數通過，測試總數 155→162；手動檢查表 `docs/manual-test-checklist.md`。評估後補 4 案（A2-4 通知列切換同步、A3-5 播放器類型切換保狀態、A5-7 通知列破版、B10 設定納入備份），規格 40→44 案。第二批（PI 2026/08/28 核准加裝 Robolectric 4.16 測試工具）：再加 9 案涵蓋設定儲存往返與縮放換算，**測試總數 171 案零失敗**。設定儲存邏輯已抽成 `PlayerHelper.retrieveRepeatModeFromPrefs`／`saveRepeatModeToPrefs`，比照既有畫面比例設定寫法。**殘餘缺口**：外觀類（按鍵是否畫出、150% 是否破版）電腦驗不到，靠 `docs/manual-test-checklist.md` 28 項手動＋6 項實車 |
| — | **施工計畫** | 任務 7、8 的動工前工作清單、八個待拍板決定、風險與施工順序 → `docs/plan-speedcam-and-sync.html` | ✅ 2026/08/27 已備妥 | 拍板後拆成子任務寫回本表 |
| 7.1 | **測速資料取得與驗證** | PI 2026/08/28 拍板乙案（app 內建測速警示、神盾退場、完整九項）。已下載警政署測速執法設置點並寫驗證工具 `tools/verify_speedcam_data.py`。結果：1898 筆公布、1897 筆可用、剔除 1 筆來源錯誤（苗栗緯度 26.6 應為 24.5）。**三項確認全數通過：①公開 ✅ 政府開放授權 ②可下載 ✅ 官方 API ③已測試可執行 ✅ 實跑驗證**。關鍵發現：座標為 WGS84 經緯度**不需轉換**、**國道 168 筆已含在內不必另抓資料集 13940**、`direct` 欄位有 150+ 種寫法約 15% 為地名無法判讀 → **設計決策：判讀不出方向者一律警示，不做方向過濾**（漏報收罰單 vs 誤報多聽一句，代價不對等）。決定 2/3/5 依建議做成設定選項：語音報讀、依速限調整距離、藍牙連上車機自動啟動 | ✅ 2026/08/28 | 詳見 `data/speedcam/README.md` |
| 7.2–7.9 | **測速警示實作（乙案九項）** | 7.2 資料打包：1895 點成 62.9KB 資源檔＋`SpeedCameraStore` 載入器。7.3 定位：`SpeedCameraService` 前景服務，用平台 LocationManager（不引入 Google Play 服務，維持去 Google 化）。7.4 前方判斷：`Geo`＋`SpeedCameraAlerts`，只報前方 ±50°、同向 ±60°，`CameraDirections` 解析 150+ 種方向寫法且**地名中的方位字不當方向用**（避免漏報）。7.5 語音＋音量閃避：TTS 以導航音訊屬性播報並請求 transient-duck 焦點壓低音樂；同一點一趟只報一次、離開 2km 後才重置。7.6 設定頁：開關／提前距離三段／試聽鍵。7.7 自我標記：行車通知列加大按鈕，盲按即記錄目前位置並語音確認，存 `marked_cameras.csv`。7.8 模擬驗證：29 個測速相關單元測試。7.9 省電引導：設定頁一鍵開啟電池最佳化排除。藍牙連上車機自動啟動、斷線自動關閉 | ✅ 2026/08/28 編譯與 199 案測試全過，待實機與實車驗收 | 提交 `7b571eea8`、`ff408c8e0`、`9921d4399` |
| 7 | 測速警示整合討論 | PI 2026/08/27 提出：現用神盾測速需開兩個 app，評估與 carpipe 整合方案（A 內建警示模組／B 一鍵並行／C 先B後A 等） | 🔄 待 PI 拍板 | 台灣無現成開源測速 app；固定式測速點有警政署開放資料（data.gov.tw 7320、13940）。社群回報延伸評估 A1 本地標記／A2′ 本地標記＋雲端備份（建議）／A3 開放平台（自用不划算）。⛔ 不抓取神盾社群資料（他人營運資產，涉服務條款與法律風險） |
| 8.2–8.7 | **Dropbox 備份實作** | 2026/08/28 完成程式：`BackupNames`／`BackupRetention`（保留 7 份、永不刪最新，7 個單元測試）、`Pkce`＋`DropboxAccount`（PKCE 授權、refresh token 自動續期）、`DropboxApi`（直接呼叫 HTTP，不引入官方程式庫）、`BackupManager`（沿用既有匯出＋把 `marked_cameras.csv` 併入同一個 zip，還原時一併復原）、`DailyBackupWorker`（充電＋Wi-Fi 時每日一次）、設定頁「備份到 Dropbox」四項。識別碼由 `local.properties` 的 `dropbox.appKey` 讀入 BuildConfig，**不進版控**。編譯與 206 案測試全過 | ✅ 程式完成，**待 PI 註冊後才能實測** | 註冊步驟見 `docs/dropbox-setup.md` |
| 8 | 雲端備份同步討論 | PI 2026/08/27 提出：用 Google 帳戶備份、登入即同步。評估 B1 現成匯出／B2 匯出＋Dropbox 自動同步／B3 app 內建 Drive appDataFolder 同步（須用不含 Google Play 服務的 OAuth，以免破壞 NewPipe 去 Google 化架構）／B4 Android 系統自動備份 | 🔄 待 PI 拍板 | 建議先 B1＋B4 零成本擋著，實測後再決定 B3。測速標記併入同一資料庫即共用此備份，A2 可省掉自架後端。**B5 LINE 登入已評估否決**：LINE Login 只提供身分驗證（OpenID Connect，回傳 user ID／暱稱／頭像），無任何第三方 app 可用的檔案儲存 API；LINE Keep 雲端儲存已於 2024/08/28 終止，Keep 筆記的檔案約兩週失效，不能當備份。用 LINE 登入仍須自架後端存資料，等於白繞一圈。LINE Login 只在未來做 A3 開放社群平台時才有價值（台灣使用者登入門檻低）。**B6 Discord 已評估否決**：OAuth2 同樣只給身分不給儲存；且 Discord 自 2023 起 CDN 連結加簽章與到期時間（約 24 小時失效），官方明言檔案長期寄存請另尋服務，拿它當備份等於違反其定位且檔案會失聯。**其他已評估**：OneDrive（Graph API 有 app 專屬資料夾 `/me/special/approot`，若學校提供 Microsoft 365 教育版空間最大，但學校帳號有離職回收風險）／GitHub 私有 repo（契合 PI 既有 token 工作流、天然版本化）／Syncthing（零帳號零程式的點對點同步，最符合 NewPipe 去雲端精神，但兩端需同時上線）。**UX 原則（PI 2026/08/27 提問「登入後是否還要再設定 Dropbox」）**：不做「Google 登入＋Dropbox 儲存」的拼裝，那會變成兩次授權。改採單一儲存來源設計——設定頁選一個備份位置（Google Drive／Dropbox／本機資料夾），只授權一次，之後自動續期不再登入；且**絕不在開 app 時強制登入**，無帳號仍須完整可用（NewPipe 本來就零帳號，加強制登入是退步）。差異提醒：Drive 應用程式專屬資料夾**使用者看不到也拿不到檔案**，Dropbox 的 Apps 資料夾**可見可手動取用**，PI 若想自己抓備份檔應選 Dropbox 或改用可見資料夾權限 |

## 問題記錄（發現即建任務）

| 日期 | 問題 | 解法 | 狀態 |
|---|---|---|---|
| 2026/08/27 | 首次編譯失敗：`SDK location not found` | 新 clone 的專案沒有 `local.properties`（該檔在 .gitignore 內不進版控）。手動建立並寫入 `sdk.dir` 指向本機 Android SDK 路徑，Gradle 即找得到 SDK。換機時需重建此檔 | ✅ 已解 |
| 2026/08/28 | PI 回報「循環鍵未看到」 | 模擬器實測確認：按鍵**有畫出來**（截圖為證，位於播放列最右），但關閉狀態設 30% 透明度，白色圖示在全黑播放器上幾乎隱形。已改為關閉 63%／開啟 100%，重新截圖確認清楚可見（`dac1c8846`）。**點擊行為已於模擬器完整驗證**（先前失敗是因控制列 2 秒自動隱藏，改在暫停狀態下測即成功）：連按三次 `last_repeat_mode` 依序寫入 1→2→0，圖示切到單曲時顯示帶「1」的循環符號；force-stop 後重開影片，日誌顯示 `onRepeatModeChanged=[1]`，**跨次記憶確認有效** | ✅ 已解並驗證 |
| 2026/08/28 | PI 安裝後回報「跟原版看來一樣」 | APK 內容經查無誤（`id/repeatButtonMain`、`ui_scale` 資源皆在）。根因是**只改功能沒改身分**：app 名稱圖示沿用上游，且 debug 版帶 `.debug` 套件後綴會與原版並存，PI 可能開到原版。解法：app 名稱改為 `carpipe`，並補客製設定的中文翻譯 | ✅ 已解（`ff13a76d0`） |
| 2026/08/27 | 系統預設 java 為 Oracle JDK，非 Android 專案適用 | 編譯前設 `JAVA_HOME` 指向 Android Studio 內建的 JBR（`C:\Program Files\Android\Android Studio\jbr`，OpenJDK 21） | ✅ 已解 |

## 決策記錄

| 日期 | 決策 | 理由 |
|---|---|---|
| 2026/08/27 | 方案 B：直接 fork NewPipe（不自建、不用官方 API） | 車用聽歌需求 NewPipe 大多內建，fork 改缺口最快 |
| 2026/08/27 | 公開 fork（不走私有 repo） | 一鍵同步上游、GPLv3 零疑慮、可回饋 PR；無需要藏的內容 |
| 2026/08/27 | 命名 carpipe | 跟隨 NewPipe 社群 pipe 系命名慣例，短好記 |
| 2026/08/27 | Android Auto 暫不處理 | PI 指示：待後續上車測試再議 |
| 2026/08/27 | **不加入 YouTube／Google 帳號登入功能** | PI 提問「登入 Google 會不會把 YT 偏好帶進來」。答：不會，Drive 備份用的權限與 YouTube 資料完全分離。若要帶入須另接 YouTube Data API 並登入，等於把實名 Google 帳號綁到一個繞過廣告的用戶端，有帳號風險，也違背 NewPipe 零帳號設計（上游 issue #12500 已明確不做）。**改用 Google Takeout 一次性匯入訂閱**（Takeout → subscriptions.csv → 訂閱頁 Import from YouTube），零登入零風險 |
| 2026/08/27 | 私人播放清單改設「不公開」而非登入取得 | 私人清單無登入拿不到；改為不公開後憑網址即可在 app 內開啟並加書籤，不必登入 |
| 2026/08/27 | **完全不帶入任何 YouTube 帳號資料**（PI 拍板） | 不只不做帳號登入，連 Google Takeout 一次性匯入訂閱也不做。訂閱與播放清單一律在 app 內從零建立。理由：與 YouTube 帳號徹底切斷，無任何關聯風險 |
| 2026/08/27 | 備份同步儲存位置＝Dropbox（PI 拍板） | 單一儲存來源、選配不強制。備份檔在 Apps 資料夾可見可自取，符合 PI 自己掌握檔案的習慣 |
