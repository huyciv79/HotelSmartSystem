@echo off
echo Starting eKYC Service...
cd /d "%~dp0"
rem Tránh xung đột với biến DEBUG toàn cục; cấu hình sẽ được đọc từ file .env.
set "DEBUG="
call myenv\Scripts\activate
python main.py
pause
