@echo off
echo Starting eKYC Service...
cd /d "%~dp0"
call myenv\Scripts\activate
python main.py
pause
