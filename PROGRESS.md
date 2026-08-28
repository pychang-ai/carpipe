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
| 2026/08/27 | **不加入 YouTube／Google 帳號登入功能** | PI 提問「登入 Google 會不會把 YT 偏好帶進來」。答：不會，Drive 備份用的權限與 YouTube 資料完全分離。若要帶入須另接 YouTube Data API 並登入，等於把實名 Google 帳號綁到一個繞過廣告的用戶端，有帳號風險，也違背 NewPipe 零帳號設計（上游 issue #12500 已明確不做）。**改用 Google Takeout 一次性匯入訂閱**（Takeout → subscriptions.csv → 訂閱頁 Import from YouTube），零登入零風險 |
| 2026/08/27 | 私人播放清單改設「不公開」而非登入取得 | 私人清單無登入拿不到；改為不公開後憑網址即可在 app 內開啟並加書籤，不必登入 |
| 2026/08/27 | **完全不帶入任何 YouTube 帳號資料**（PI 拍板） | 不只不做帳號登入，連 Google Takeout 一次性匯入訂閱也不做。訂閱與播放清單一律在 app 內從零建立。理由：與 YouTube 帳號徹底切斷，無任何關聯風險 |
| 2026/08/27 | 備份同步儲存位置＝Dropbox（PI 拍板） | 單一儲存來源、選配不強制。備份檔在 Apps 資料夾可見可自取，符合 PI 自己掌握檔案的習慣 |
