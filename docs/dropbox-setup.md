# Dropbox 註冊步驟（PI 執行，約十分鐘）

> 這是任務 8 唯一需要 PI 本人做的一步。程式已經寫好，只等這組識別碼。

## 一、在 Dropbox 建立應用程式

1. 用你的 Dropbox 帳號登入 <https://www.dropbox.com/developers/apps>
2. 按 **Create app**
3. 三個選項這樣選：
   - Choose an API → **Scoped access**
   - Choose the type of access → **App folder**（只能存取自己的專屬資料夾，**不要選 Full Dropbox**）
   - Name your app → 填一個唯一的名字，例如 `CAI-PP-backup-py`（名稱被佔用就換一個，這個名字只是識別用）
4. 按 **Create app**

## 二、設定權限

進到該應用程式的頁面，切到 **Permissions** 分頁，勾選這四項後按 **Submit**：

- `files.content.write`（上傳備份）
- `files.content.read`（下載還原）
- `files.metadata.read`（列出備份清單）
- `account_info.read`（顯示目前連結的帳號，可略）

> 權限一定要在產生代碼之前先勾，否則之後登入拿到的權杖不足以上傳。

## 三、取得識別碼

回到 **Settings** 分頁，找到 **App key**，那是一串英數字。把它給我，或自己貼進下一步。

## 四、填進本機設定檔

在 `carpipe/local.properties` 檔案末尾加一行（這個檔案不會進版控，識別碼不會外流）：

```
dropbox.appKey=你的App key
```

存檔後重新建置 APK，備份功能就會啟用。

## 五、在手機上連結

1. 設定 → 備份與還原 → **連結 Dropbox**
2. 手機會開瀏覽器到 Dropbox 的授權頁，按同意
3. Dropbox 會顯示一段代碼，複製它
4. 回到 app，把代碼貼進跳出來的輸入框，按確定

完成後設定頁會顯示「已連結：你的名字」，「每天自動備份」與「立即備份」就能用了。

## 常見狀況

| 現象 | 原因與處理 |
|---|---|
| 設定頁顯示「這個版本未設定 Dropbox 識別碼」 | 第四步沒做或沒重新建置 |
| 貼代碼後顯示失敗 401 | 第二步的權限沒勾齊，補勾後重新走第五步 |
| 備份顯示空間不足 | Dropbox 免費帳號 2 GB，備份檔本身很小，通常是帳號本來就滿了 |
