@echo off
title MagzPortal - Java Magazine Application
echo ========================================================
echo   Starting MagzPortal (Java Spring Boot)
echo ========================================================
echo.
echo Home Page:        http://localhost:8080
echo Admin Login:      http://localhost:8080/admin/login
echo Credentials:      Username: admin ^| Password: password123
echo H2 Database:      http://localhost:8080/h2-console
echo.
mvn spring-boot:run
pause
