# BlueMoon – Hệ thống quản lý hộ gia đình và cư dân - sprint 1

## 1. Công nghệ sử dụng

| Thành phần | Công nghệ |
|---|---|
| Backend | Java, Spring Boot, Spring Data JPA, Spring Security |
| Frontend | ReactJS, Vite, JavaScript, CSS |
| Cơ sở dữ liệu | PostgreSQL |
| Công cụ xây dựng Backend | Maven |
| Quản lý thư viện Frontend | npm |

## 2. Cấu trúc dự án

```text
IT3180-SE_07/
├── backend/
│   ├── src/
│   ├── pom.xml
│   └── ...
│
├── frontend2/
│   ├── src/
│   ├── package.json
│   ├── vite.config.js
│   └── ...
│
└── README.md
```

## 3. Yêu cầu môi trường

Trước khi chạy dự án, cần cài đặt các công cụ sau:

- **JDK 17 trở lên:** Môi trường chạy ứng dụng Java (sử dụng phiên bản phù hợp với cấu hình Backend).
- **Node.js:** Khuyến nghị sử dụng phiên bản LTS.
- **npm:** Được cài đặt kèm theo Node.js (Cài nodejs rồi thì nó sẽ cài luôn cho npm).
- **PostgreSQL 14 trở lên:** Hệ quản trị cơ sở dữ liệu.
- **Git:** Quản lý mã nguồn.

Kiểm tra phiên bản các công cụ:

```bash
java -version
node -v
npm -v
psql --version
```

## 4. Hướng dẫn chạy Backend

Backend được xây dựng bằng Java và Spring Boot.

### Bước 1: Tạo cơ sở dữ liệu

Khởi động PostgreSQL và tạo database cho dự án:

```sql
CREATE DATABASE bluemoon;
```

### Bước 2: Cấu hình kết nối cơ sở dữ liệu

Cập nhật thông tin kết nối PostgreSQL trong file cấu hình ứng dụng hoặc biến môi trường tương ứng (file backend/src/main/resources/application.properties).

Ví dụ:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/bluemoon
spring.datasource.username=your_username
spring.datasource.password=your_password
```

Thay `your_username` và `your_password` bằng thông tin đăng nhập PostgreSQL trên máy cá nhân.

**Lưu ý:** Không đưa mật khẩu hoặc thông tin nhạy cảm vào repository công khai.

### Bước 3: Khởi chạy Backend

Mở Terminal tại thư mục gốc dự án và di chuyển vào thư mục Backend:

```bash
cd backend
```

Nếu dự án có Maven Wrapper, chạy lệnh:

**Windows:**

```powershell
.\mvnw.cmd spring-boot:run
```

**Linux / macOS:**

```bash
./mvnw spring-boot:run
```

Hoặc sử dụng Maven đã cài đặt trên máy:

```bash
mvn spring-boot:run
```

Sau khi khởi chạy thành công, Backend mặc định hoạt động tại:

```text
http://localhost:8080
```

## 5. Hướng dẫn chạy Frontend

Frontend được xây dựng bằng ReactJS và Vite.

### Bước 1: Cài đặt thư viện

Mở một Terminal mới và di chuyển vào thư mục Frontend:

```bash
cd frontend2
```

Cài đặt các thư viện cần thiết:

```bash
npm install
```

### Bước 2: Cấu hình biến môi trường

Tạo file `.env` trong thư mục `frontend2`. copy đúng từ .env.example

Đảm bảo tên biến môi trường khớp với cấu hình được sử dụng trong mã nguồn Frontend.

### Bước 3: Khởi chạy Frontend

```bash
npm run dev
```

Sau khi khởi chạy thành công, Vite sẽ cung cấp địa chỉ truy cập ứng dụng, mặc định là:

```text
http://localhost:5173
```

Mở địa chỉ trên bằng trình duyệt để sử dụng hệ thống.

## 6. Hướng dẫn chạy toàn bộ hệ thống

Để hệ thống hoạt động đầy đủ, cần khởi chạy PostgreSQL, Backend và Frontend.

Sử dụng hai Terminal riêng biệt:

**Terminal 1 – Backend**

```bash
cd backend
.\mvnw.cmd spring-boot:run
```

**Terminal 2 – Frontend**

```bash
cd frontend2
npm install
npm run dev
```

Đảm bảo PostgreSQL đã được khởi động và Backend kết nối thành công với cơ sở dữ liệu trước khi sử dụng Frontend.

## 7. Một số lệnh thường dùng

### Backend

```bash
# Khởi chạy ứng dụng
mvn spring-boot:run

# Biên dịch và đóng gói dự án
mvn clean package

# Chạy kiểm thử
mvn test (chưa làm test đâu, đừng chạy cnay)
```

### Frontend

```bash
# Cài đặt thư viện
npm install

# Khởi chạy môi trường phát triển
npm run dev

# Biên dịch cho môi trường production
npm run build

# Chạy kiểm thử
npm test
```

## 8. Một số lưu ý

- Đảm bảo PostgreSQL đang hoạt động trước khi khởi chạy Backend.
- Kiểm tra thông tin kết nối cơ sở dữ liệu nếu Backend không thể khởi động.
- Đảm bảo URL API của Frontend khớp với địa chỉ Backend.
- Kiểm tra cấu hình CORS nếu Frontend không thể gửi request đến Backend.
- Không commit các file chứa thông tin đăng nhập hoặc biến môi trường cá nhân lên GitHub.

---