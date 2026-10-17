# 📚 Digital Magazine Portal - Java Mini Project

A modern, full-stack Java web application built using **Java 21**, **Spring Boot**, **Thymeleaf**, **Spring Data JPA**, and **H2 Database**.

---

## 🌟 Key Features

1. **Modern Magazine Home Page (`/`)**:
   - Modern responsive **card grid layout** showcasing all published magazines.
   - Dynamic covers, category badges, upload date, and formatted file size indicators.
   - **Inline PDF Reader**: Read magazines directly in a modal without navigating away.
   - **Download PDF**: Direct download button for offline reading.
   - **Search Filter**: Search magazines instantly by title or description.

2. **Admin Authentication (`/admin/login`)**:
   - Secure login restricted to administrators.
   - **Username**: `admin`
   - **Password**: `password123`
   - Session-based authentication with clear error alerts.

3. **Admin Dashboard & Magazine Upload (`/admin/dashboard`)**:
   - Upload new issues with **Title**, **Category**, **Description**, and **PDF Document (.pdf)**.
   - Optional **Cover Image** upload.
   - File validation (ensures valid PDF files up to 50MB).
   - Real-time magazine catalog management with **Delete** functionality (which automatically cleans up physical files).
   - One-click logout.

4. **Zero-Configuration Embedded Database**:
   - Powered by **H2 file-based database** (`./data/magazinedb`).
   - Requires **no external database server** (no MySQL/PostgreSQL installation needed).
   - Data persists across application restarts.
   - Built-in H2 Web Console accessible at `http://localhost:8080/h2-console`.

---

## 🚀 Quick Start Guide

### Option 1: Double-Click Runner (Windows)

Simply double-click the **`run.bat`** file in the project folder.

### Option 2: Using Maven Command Line

Open PowerShell or Command Prompt in this folder and run:

```bash
mvn spring-boot:run
```

Once started, open your browser:

- 🌐 **Home Page**: [http://localhost:8080](http://localhost:8080)
- 🔐 **Admin Login**: [http://localhost:8080/admin/login](http://localhost:8080/admin/login)
  - **Username**: `admin`
  - **Password**: `password123`
- 🗄️ **H2 Database Console**: [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
  - JDBC URL: `jdbc:h2:file:./data/magazinedb`
  - Username: `sa`
  - Password: _(leave blank)_

---

## 📁 Project Architecture & Structure

```
Magazine/
│── pom.xml                               # Maven project dependencies & build configuration
│── run.bat                               # One-click Windows runner script
│── README.md                             # Documentation & Viva guide
│── uploads/                              # Persistent storage for uploaded PDFs and covers
│── data/                                 # H2 database persistence directory
│── src/main/java/com/magazine/
│   │── MagazineApplication.java          # Spring Boot main application entry point
│   │── model/
│   │   └── Magazine.java                 # JPA Entity representing Magazine schema
│   │── repository/
│   │   └── MagazineRepository.java       # Spring Data JPA database operations
│   │── service/
│   │   └── MagazineService.java          # Business logic, file I/O & PDF storage
│   └── controller/
│       │── HomeController.java           # Public routes (Home page, PDF streaming, Download)
│       └── AdminController.java          # Admin login, upload, dashboard & delete routes
└── src/main/resources/
    │── application.properties            # Application configuration & file upload limits
    │── static/
    │   ├── css/styles.css                # Modern responsive UI styling
    │   └── js/app.js                     # Interactive PDF modal reader script
    └── templates/
        ├── index.html                    # Modern card view home page
        ├── admin-login.html              # Clean admin login page
        └── admin-dashboard.html          # Admin upload form & management table
```

---

## 🎓 Viva / Presentation Guide

If your teacher or examiner asks questions during the presentation, here are quick answers:

1. **Q: Why did you choose Spring Boot for this Java project?**
   - _A: Spring Boot provides an enterprise-standard, pure Java framework with an embedded Tomcat server and automatic dependency management, eliminating complicated external server setups._

2. **Q: How does the PDF file upload work?**
   - _A: We use Spring's `MultipartFile` interface in `AdminController.java`. The `MagazineService` validates that the uploaded file has a `.pdf` extension, saves it with a unique UUID to prevent file name collisions in the `uploads/` directory, and stores the metadata in the database._

3. **Q: Where is the magazine data stored?**
   - _A: Magazine metadata (title, description, category, filename, size, date) is saved in a file-based H2 database using Spring Data JPA. The physical PDF files are stored in the server's `./uploads/` folder._

4. **Q: How is the admin login secured?**
   - _A: In `AdminController.java`, user credentials are submitted via HTTP POST. Upon validating `admin` and `password123`, an authenticated session attribute is created (`session.setAttribute("adminUser", "admin")`), which protects the upload and dashboard endpoints._
