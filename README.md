# E-Bank

E-Bank 是一套以 Spring Boot 開發的網路銀行後端系統，提供會員驗證、帳戶管理、存提款、轉帳、交易查詢、角色權限與稽核功能。系統採用 JWT 無狀態驗證，並整合 HTML Email 通知，模擬完整的網路銀行後端流程。

## 專案特色

- RESTful API 與分層式架構（Controller、Service、Repository）
- JWT 無狀態身分驗證與角色權限控管
- 存款、提款及帳戶間轉帳
- 使用悲觀鎖維持帳務一致性
- HTML Email 與非同步通知
- DTO、資料驗證及全域例外處理
- 管理員與稽核人員專用查詢功能

## 技術棧

| 類別 | 技術 |
| --- | --- |
| 語言 | Java 21 |
| 後端框架 | Spring Boot 4、Spring Web MVC |
| 驗證與授權 | Spring Security、JWT、BCrypt |
| 資料存取 | Spring Data JPA、Hibernate |
| 資料庫 | MySQL |
| 郵件通知 | Spring Mail、Thymeleaf、Gmail SMTP |
| 資料處理 | DTO、ModelMapper、Jakarta Validation |
| 建置工具 | Maven、Maven Wrapper |
| 開發輔助 | Lombok |

## 系統功能

### 使用者與驗證

- 使用者註冊與登入
- 註冊時預設指派 `CUSTOMER` 角色
- 註冊成功後自動建立 TWD 儲蓄帳戶
- JWT Bearer Token 驗證
- BCrypt 密碼雜湊
- 查詢目前登入者資料
- 更新密碼及上傳個人頭像
- 忘記密碼、重設碼驗證與密碼重設
- 管理員分頁查詢所有使用者

### 帳戶與交易

- 查詢目前使用者擁有的帳戶
- 關閉本人帳戶
- 存款（`DEPOSIT`）
- 提款（`WITHDRAWAL`）
- 帳戶轉帳（`TRANSFER`）
- 提款與轉帳餘額檢查
- 轉帳建立借方與貸方兩筆帳務紀錄
- 使用共同的 transfer reference 關聯轉帳紀錄
- 分頁查詢指定帳戶的交易紀錄

### 權限與稽核

系統目前包含以下角色：

- `CUSTOMER`：一般銀行使用者
- `AUDITOR`：查詢系統、帳戶與交易稽核資料
- `ADMIN`：管理角色、查詢使用者及使用稽核功能

稽核功能包含：

- 系統使用者、帳戶及交易總數
- 依 Email 查詢使用者
- 依帳號查詢帳戶及交易
- 依交易 ID 查詢交易明細

### Email 通知

系統使用 Thymeleaf HTML 樣板寄送非同步 Email，包含：

- 歡迎信
- 開戶通知
- 忘記密碼與密碼變更通知
- 入帳與出帳通知
- 轉帳雙方通知

成功寄送的通知也會保存於資料庫。

## 專案結構

```text
src/main/java/com/eric/eBank
├── account             # 帳戶模組
├── audit_dashboard     # 稽核查詢模組
├── auth_users          # 使用者、登入與密碼管理
├── config              # 共用 Bean 設定
├── exceptions          # 自訂例外與全域例外處理
├── notification        # Email 與通知紀錄
├── role                # 角色管理
├── security            # JWT、Security Filter 與 CORS
└── transaction         # 存款、提款及轉帳
```

## 主要 API

除 `/api/auth/**` 外，其餘 API 皆須在 Header 帶入 JWT：

```http
Authorization: Bearer <token>
```

### 驗證

| Method | Endpoint | 說明 | 權限 |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | 使用者註冊 | Public |
| POST | `/api/auth/login` | 使用者登入 | Public |
| POST | `/api/auth/forget-password` | 寄送密碼重設信 | Public |
| POST | `/api/auth/reset-password` | 使用重設碼更新密碼 | Public |

### 使用者與帳戶

| Method | Endpoint | 說明 | 權限 |
| --- | --- | --- | --- |
| GET | `/api/users/me` | 取得個人資料 | Authenticated |
| GET | `/api/users` | 分頁取得所有使用者 | ADMIN |
| PUT | `/api/users/update-password` | 更新密碼 | Authenticated |
| PUT | `/api/users/profile-picture` | 上傳個人頭像 | Authenticated |
| GET | `/api/accounts/me` | 取得本人帳戶 | Authenticated |
| GET | `/api/accounts/close/{accountNumber}` | 關閉本人帳戶 | Authenticated |

### 交易

| Method | Endpoint | 說明 | 權限 |
| --- | --- | --- | --- |
| POST | `/api/transactions` | 建立存款、提款或轉帳 | Authenticated |
| GET | `/api/transactions/{accountNumber}` | 分頁查詢本人帳戶交易 | Authenticated |

交易請求範例：

```json
{
  "transactionType": "TRANSFER",
  "amount": 1000,
  "accountNumber": "003011234567",
  "destinationAccountNumber": "003017654321",
  "description": "Transfer payment"
}
```

### 角色與稽核

| Method | Endpoint | 說明 | 權限 |
| --- | --- | --- | --- |
| GET | `/api/roles` | 查詢所有角色 | ADMIN |
| POST | `/api/roles` | 建立角色 | ADMIN |
| PUT | `/api/roles` | 更新角色 | ADMIN |
| DELETE | `/api/roles/{id}` | 刪除角色 | ADMIN |
| GET | `/api/audit/totals` | 查詢系統統計 | AUDITOR / ADMIN |
| GET | `/api/audit/users?email={email}` | 依 Email 查詢使用者 | AUDITOR / ADMIN |
| GET | `/api/audit/accounts?accountNumber={number}` | 查詢帳戶明細 | Authenticated |
| GET | `/api/audit/transactions/by-account?accountNumber={number}` | 查詢帳戶交易 | AUDITOR / ADMIN |
| GET | `/api/audit/transactions/by-id?transactionId={id}` | 查詢交易明細 | AUDITOR / ADMIN |

## 執行專案

### 環境需求

- JDK 21
- MySQL
- 可使用 SMTP 的 Gmail 帳號或應用程式密碼

### 環境變數

在專案根目錄建立 `.env`：

```properties
PORT=8080
LOCAL_DB_URL=jdbc:mysql://localhost:3306/ebank
LOCAL_DB_USERNAME=your_database_username
LOCAL_DB_PASSWORD=your_database_password
JWT_SECRET=replace_with_a_secure_secret_key
JWT_EXPIRATION_TIME=86400000
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_gmail_app_password
```

> 請勿將包含真實密碼或金鑰的 `.env` 提交到版本控制。

### 啟動方式

> 專案目錄下

Windows：

```powershell
.\mvnw.cmd spring-boot:run
```

macOS / Linux：

```bash
./mvnw spring-boot:run
```

服務預設會依 `PORT` 環境變數啟動，例如：

```text
http://localhost:8090
```

## 設計重點

- 使用 `@Transactional` 確保交易流程成功完成或完整回滾。
- 轉帳時依固定順序鎖定帳戶，降低並行轉帳產生死鎖的風險。
- 使用 JWT 與 `SessionCreationPolicy.STATELESS`，不在伺服器保存登入 Session。
- 使用自訂 Authentication Entry Point、Access Denied Handler 與全域例外處理，統一 API 錯誤回應。
- 使用非同步 Email，避免寄信流程阻塞主要 API 請求。

