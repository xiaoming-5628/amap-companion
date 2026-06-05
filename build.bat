@echo off
chcp 65001 >nul
echo ========================================
echo   高德地图伴侣 - 一键APK构建脚本
echo ========================================
echo.

REM 检查是否有 PowerShell
where powershell >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到 PowerShell，请先安装 PowerShell
    pause
    exit /b 1
)

echo 正在使用 PowerShell 构建...
echo.
powershell -ExecutionPolicy Bypass -File "%~dp0build.ps1"

if %errorlevel% equ 0 (
    echo.
    echo ========================================
    echo   构建成功！
    echo   APK文件: amap_companion_signed.apk
    echo ========================================
) else (
    echo.
    echo ========================================
    echo   构建失败，请检查错误信息
    echo ========================================
)
echo.
pause
