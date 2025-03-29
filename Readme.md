## 音檔轉錄系統

### 介紹

提供音檔轉錄服務，支援上傳影片與音檔，並透過語音辨識模型轉換為文字，支援多種輸出格式，並提供標準化接口以便擴展。

---

### 功能特色

1. 支援多種音檔格式: 能夠處理多種音檔格式以及影片，包括 mp3, wav, ogg, mp4等，系統會自動轉換並標準化格式。
2. 語音辨識: 透過語音辨識模型將音檔轉換為文字，依照不同語言提供不同的語音辨識模型。
3. 文字處理: 提供音檔轉換為文字後的文字處理功能，如標點符號處理、斷詞等。
4. 多種輸出格式: 支援多種輸出格式，如 `TXY`, `DOCX`, `PDF`等。
5. 處理進度追蹤: 提供處理進度追蹤功能
    - 若任務需要較長時間處理，可透過 `任務 ID` 查詢當前轉換狀態。
    - 支援異步處理，確保系統穩定運行。
6. 簡易的擴展:
    - 未來如需處理不同語言，只需將支援的語音辨識模型放入`model`並配置訊息即可。
    - 如需支援不同的輸出格式，只需實現FileWriter介面即可。

---

### 安裝說明

這邊僅包含後端安裝，前端請參考 [前端庫](https://github.com/tommot20077/AudioToTextWeb)<br>
這邊提供2種安裝方式，分別為 Maven 與 Docker 安裝，請依照需求選擇安裝方式。

#### 1. Maven 安裝

- 前置條件
    - JDK 21 或以上
    - Maven 3.8.6 或以上
    - MySQL 8.0 或以上
    - Vosk 0.15.0 或以上
- 步驟:
    1. 先拉取本專案源代碼到本地並進入專案目錄
       ```bash
       git clone https://github.com/tommot20077/AudioToText.git
       cd AudioToText
       ```
    2. 使用Maven安裝並編譯專案(建議跳過測試)，並將打包後的jar檔案放入需要的目錄下
       ```bash
       mvn clean install -DskipTests
       mv target/AudioToText-V1.jar /path/to/your/directory
       ```
    3. 新建一個資料夾 `config`，並在裡面新建一個 `application.yml` 檔案，將以下內容複製到 `application.yml`
       檔案中，並根據需要修改資料庫連線資訊以及一些配置，其他配置可以參考 [範例設定](https://github.com/tommot20077/AudioToText/blob/master/src/main/resources/application-demo.yml)
       自行調整
         ```yaml
         spring:
           datasource:
             url: jdbc:mysql://your_mysql_ip/audio_to_text
             username: username
             password: password
             driver-class-name: com.mysql.cj.jdbc.Driver
           jpa:
             hibernate:
               ddl-auto: update
         ```
    4. 啟動專案即可使用，並在瀏覽器中輸入 `http://localhost:8080` 即可訪問
       ```bash
       java -jar AudioToText-V1.jar
       ```

#### 2. Docker 安裝

- 前置需求
    - Docker 20.10.0 或以上
    - Docker Compose 1.29.2 或以上
- 安裝步驟:
    1. 先下載位於 [本專案](https://github.com/tommot20077/AudioToText/tree/master/docker) 的 `docker/docker-compose.yml`
       以及 `docker/Dockerfile-end` 檔案
    2. 將 `docker-compose.yml` 檔案中的 `MYSQL_ROOT_PASSWORD`、`MYSQL_USER`、`MYSQL_PASSWORD` 修改為自己的資料庫資訊
       ```yaml
       environment:
         MYSQL_ROOT_PASSWORD: your_mysql_root_password
         MYSQL_USER: your_mysql_username
         MYSQL_PASSWORD: your_mysql_password
       ```
    3. 使用以下指令啟動Docker容器，並等待容器啟動完成即可
       ```bash
       docker-compose up -d
       ```
    4. 最後連接 `http://localhost:10880` 即可使用

---

### 使用說明

在啟動完成後可以使用 API 進行音檔轉錄，以下是一些常用的 API 以及使用說明

- **轉錄音檔**
    - `POST /api/transcribe`
        - 上傳音檔，支援多種格式，並會自動轉換為標準格式
        - 請求參數:
            - `file`: 上傳的音檔
            - `model`: 語音辨識模型名稱
            - `isNeedSegment`: 是否需要斷詞，預設為 `true`
            - `format_type`: 輸出格式，支援 `txt`, `docx`, `pdf`等格式
        - 回傳參數:
            - `taskId`: 任務 ID，可用於查詢處理進度
- **查詢處理進度**
    - `GET /api/getTaskStatus`
        - 查詢任務狀態
        - 請求參數:
            - `task_id`: 任務 ID
        - 回傳參數:
          分成3種情況
        - 若任務處理完成，則會回傳以下參數
            - `taskId`: 任務 ID
            - `status`: 任務狀態，`success` 或 `fail`
            - `downloadUrl`: 下載連結
            - `result`: 處理結果，如果處理錯誤則為錯誤訊息
        - 若任務處理中，則會回傳以下參數
            - `taskId`: 任務 ID
            - `progress`: 處理進度，範圍為 0~100
            - `status`: 任務狀態，`processing`
            - `result`: 處理結果
        - 若任務不存在，回傳錯誤訊息
- **當前可用類型以及模型**
    - `GET /API/getAvailableOutputTypes`
        - 查詢可用的輸出格式
        - 回傳參數:
            - `outputTypes`: 可用的輸出格式，包含 `txt`, `docx`, `pdf`等格式
    - `GET /api/getAvailableModels`
        - 查詢可用的語音辨識模型
        - 回傳參數:
            - `code` 模型辨識碼
            - `description` 模型描述
            - `language` 處理語言

---

### 架構設計

- 後端框架: Spring Boot 3.4.0
- 語音辨識: Vosk
- 資料庫: MySQL 8.0
- 服務器: Nginx 1.18.0



