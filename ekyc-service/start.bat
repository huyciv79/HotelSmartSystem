@echo off
echo Starting eKYC Service...
cd /d "%~dp0"
call .venv\Scripts\activate
python main.py
pause
