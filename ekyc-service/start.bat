@echo off
echo Starting eKYC Service...
cd /d "%~dp0"
<<<<<<< HEAD
call myenv\Scripts\activate
=======
rem Tránh xung đột với biến DEBUG toàn cục; cấu hình sẽ được đọc từ file .env.
set "DEBUG="
call .venv\Scripts\activate
>>>>>>> 72d1cd4 (feat: add payment status badge (100%/30%) to invoice modal and booking detail page)
python main.py
pause
