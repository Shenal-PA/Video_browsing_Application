# Web-Based-Video-Browsing-System.

![Hero Demo](docs/hero-demo.gif)  
✨ Browse, search, and stream videos with a fun, fast, Java-powered experience!

> Note: Tailored for a Java (Spring Boot) stack, following your similar project. Add/adjust details to match your exact implementation.

---

## 🧭 Overview
- 🎥 Video library with categories, tags, and metadata  
- 🔎 Search by title, tags, and categories  
- 📺 Smooth playback 
- ❤️ Favorites, 📃 Watchlist, 🕒 History  
- 🔐 Secure access (Spring Security/JWT)  
- 📊 Analytics (views, trends)

![Browse Demo](docs/browse.gif)
![Watch Demo](docs/watch.gif)
![Search Demo](docs/search.gif)
![Playlist Demo](docs/playlist.gif)

---

## 🧩 System Architecture
![Architecture](docs/architecture.gif)

- 🧑‍🎨 UI: Web client (HTML,CSS)  
- 🧰 API: Spring Boot (REST controllers)  
- 📚 Catalog: Video metadata, categories, tags  
- 🔎 Search: DB queries + index (optional)  
- 🎞️ Transcoding
- 🗄️ Database: MySQL/PostgreSQL  
- ☁️ Storage: Local/Cloud (S3/MinIO)  
- 🔐 Auth: Spring Security + JWT  
- 🧪 CDN: Static + segment delivery

---

## 🛠️ Tech Stack
- ☕ Java 17+  
- 🍃 Spring Boot (Web, Security, Data JPA)  
- 🗄️ MySQL/PostgreSQL  
- 🎞️ FFmpeg 
- 🧰 Build: Maven 

---

## 🏁 Getting Started

### 🔧 Prerequisites
- ☕ JDK 17+  
- 🧰 Maven
- 🗄️ Database (MySQL/PostgreSQL)  
- 🎞️ FFmpeg (for streaming/transcoding)  
- 🔐 JWT secret (for auth)

### ⚙️ Local Setup (Maven)

🗂️ clone
```bash
git clone https://github.com/Shenal-PA/Video_browsing_Application.git
cd Video_browsing_Application
```

## 🔧 environment
```bash
cp src/main/resources/application.example.properties src/main/resources/application.properties
# ✍️ update DB, storage, and auth settings inside application.properties
```

## ▶️ run
```bash
mvn spring-boot:run
```

## 🔧 Configuration (application.properties)
```properties
# 🌐 App
server.port=8080

# 🔐 Auth
security.jwt.secret=your-secret
security.jwt.expiry=3600

# 🗄️ Database
spring.datasource.url=jdbc:postgresql://localhost:5432/videos
spring.datasource.username=user
spring.datasource.password=pass
spring.jpa.hibernate.ddl-auto=update

# ☁️ Storage
storage.type=local        # local | s3 | minio
storage.local.path=./data/videos
storage.s3.bucket=videos
storage.s3.endpoint=https://s3.example.com
storage.s3.accessKey=key
storage.s3.secretKey=secret

# 🎞️ Streaming/Transcoding
ffmpeg.path=/usr/bin/ffmpeg
streaming.hls.enabled=true
streaming.hls.preset=HLS_720p
```

---


![Streaming Demo](docs/streaming.gif)

---

## 🔐 Security
- 🔒 Spring Security + JWT  
- 🛡️ Role-based access (user/admin)  
- 🔑 Token refresh and expiry

---

## 📊 Analytics (optional)
- 📈 Track views, likes, watch time  
- 🔥 Trending & recommendations

---


## 📜 License
- ⚖️ MIT (adjust as needed)

---

## ✨ Tips
- 🎞️ Use HLS/DASH for adaptive streaming  
- 🧊 Serve static + segments via CDN  
- 🔎 Index metadata for fast search  @Shenal-PA/Video_browsing_Application  this repository u can made readme file guid line usefull and u include this system detaild only  use animation or imogy
- 🧪 Event logging → trends & insights  
- 🛡️ Secure upload + signed URLs

---

