@echo off
title TRINH KHOI DONG CHROME CHO CRAWLER TOOL
color 0A

echo ========================================================
echo      CONG CU HO TRO CRAWLER FACEBOOK (JAVA)
echo ========================================================

echo [BUOC 1] Dang tat cac tien trinh Chrome cu...
taskkill /F /IM chrome.exe /T >nul 2>&1
echo ... Da don dep xong!

echo [BUOC 2] Dang tao Profile rieng biet...
if not exist "ChromeBotProfile" mkdir "ChromeBotProfile"
echo ... Profile nam tai: %~dp0ChromeBotProfile

echo [BUOC 3] Dang tim kiem Google Chrome tren may tinh...
set "CHROME_PATH="

if exist "C:\Program Files\Google\Chrome\Application\chrome.exe" (
    set "CHROME_PATH=C:\Program Files\Google\Chrome\Application\chrome.exe"
) else if exist "C:\Program Files (x86)\Google\Chrome\Application\chrome.exe" (
    set "CHROME_PATH=C:\Program Files (x86)\Google\Chrome\Application\chrome.exe"
) else if exist "%LOCALAPPDATA%\Google\Chrome\Application\chrome.exe" (
    set "CHROME_PATH=%LOCALAPPDATA%\Google\Chrome\Application\chrome.exe"
)

if "%CHROME_PATH%"=="" (
    color 0C
    echo [LOI] Khong tim thay Google Chrome tren may tinh nay!
    echo Vui long cai Chrome hoac sua lai duong dan trong file .bat
    pause
    exit
)

echo ... Da tim thay Chrome tai: "%CHROME_PATH%"

echo [BUOC 4] Dang khoi dong Chrome...
echo ========================================================
echo HUONG DAN QUAN TRONG:
echo 1. Mot cua so Chrome trang tinh se hien ra.
echo 2. Hay DANG NHAP FACEBOOK vao cua so do (Login tay).
echo 3. Sau khi Login xong, GIU NGUYEN CUA SO DO (Dung tat).
echo 4. Quay lai IntelliJ/Eclipse va chay file Java.
echo ========================================================

start "" "%CHROME_PATH%" --remote-debugging-port=9222 --user-data-dir="%~dp0ChromeBotProfile"

echo ... Chrome da mo xong! Hay lam theo huong dan tren.
pause