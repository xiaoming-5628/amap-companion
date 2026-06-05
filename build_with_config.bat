@echo off
chcp 65001 >nul
echo ========================================
echo   高德地图伴侣 - 一键APK构建脚本
echo ========================================
echo.

REM 尝试加载配置文件
if exist "%~dp0build_config.bat" (
    echo [信息] 加载配置文件 build_config.bat
    call "%~dp0build_config.bat"
    echo.
) else (
    echo [提示] 未找到 build_config.bat，使用默认配置
    echo [提示] 可以复制 build_config.bat.example 为 build_config.bat 并修改
    echo.
)

REM 设置默认的Android SDK路径（如果没有配置）
if "%ANDROID_HOME%"=="" (
    if exist "D:\Android\SDK" (
        echo [信息] 使用默认SDK路径: D:\Android\SDK
        set ANDROID_HOME=D:\Android\SDK
    ) else if exist "%LOCALAPPDATA%\Android\Sdk" (
        echo [信息] 使用默认SDK路径: %%LOCALAPPDATA%%\Android\Sdk
        set ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk
    ) else (
        echo [错误] 未找到 Android SDK，请配置 ANDROID_HOME 环境变量
        echo        或创建 build_config.bat 文件
        pause
        exit /b 1
    )
)

REM 检查并设置Java路径
if "%JAVA_HOME%"=="" (
    if exist "D:\Android\Java\jdk-17" (
        echo [信息] 使用默认JDK路径: D:\Android\Java\jdk-17
        set JAVA_HOME=D:\Android\Java\jdk-17
        set PATH=%JAVA_HOME%\bin;%PATH%
    )
)

echo SDK路径: %ANDROID_HOME%
if not "%JAVA_HOME%"=="" echo JDK路径: %JAVA_HOME%
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
