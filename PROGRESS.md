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
| 3 | 建置驗證 | 不改 code，編譯原版 debug APK 成功 | 🔄 進行中 | 本機首次編譯 |
| 4 | 循環鍵改造 | 主播放器大型三態循環鍵（關／單曲／全部）＋設定跨次記憶 | ⬜ | 對應上游 issue #6914、#11012 |
| 5 | 字級設定 | 設定頁加介面字級選項，文字與觸控目標同步放大 | ⬜ | 車上好按優先於好看 |
| 6 | 車上驗收 | 有線＋藍牙實測一週：循環、背景播放、大字操作 | ⬜ | 問題回填本表建新任務 |

## 問題記錄（發現即建任務）

（目前無）

## 決策記錄

| 日期 | 決策 | 理由 |
|---|---|---|
| 2026/08/27 | 方案 B：直接 fork NewPipe（不自建、不用官方 API） | 車用聽歌需求 NewPipe 大多內建，fork 改缺口最快 |
| 2026/08/27 | 公開 fork（不走私有 repo） | 一鍵同步上游、GPLv3 零疑慮、可回饋 PR；無需要藏的內容 |
| 2026/08/27 | 命名 carpipe | 跟隨 NewPipe 社群 pipe 系命名慣例，短好記 |
| 2026/08/27 | Android Auto 暫不處理 | PI 指示：待後續上車測試再議 |
