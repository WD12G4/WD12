@echo off
chcp 65001 >nul
:: ========================================================
:: Installation et démarrage automatique de l'Agent WD12
:: Doit être exécuté en tant qu'Administrateur
:: ========================================================

net session >nul 2>&1
if %errorLevel% neq 0 (
    echo [ERREUR] Ce script doit être exécuté en tant qu'Administrateur.
    echo Veuillez faire un clic droit -> "Exécuter en tant qu'administrateur".
    pause
    exit /b 1
)

set "AGENT_DIR=%~dp0"
if "%AGENT_DIR:~-1%"=="\" set "AGENT_DIR=%AGENT_DIR:~0,-1%"
set "EXE_PATH=%AGENT_DIR%\MaterielAgent.exe"
set "CONFIG_PATH=%AGENT_DIR%\config.json"
set "TASK_NAME=MaterielAgent"

if not exist "%EXE_PATH%" (
    echo [ERREUR] Fichier introuvable : %EXE_PATH%
    pause
    exit /b 1
)

echo [1/3] Arrêt des instances précédentes...
taskkill /f /im MaterielAgent.exe >nul 2>&1
schtasks /end /tn "%TASK_NAME%" >nul 2>&1

echo [2/3] Configuration du démarrage automatique au boot...
schtasks /create /tn "%TASK_NAME%" /tr "\"%EXE_PATH%\"" /sc onstart /ru "SYSTEM" /rl HIGHEST /f

if %errorLevel% neq 0 (
    echo [ERREUR] Échec de création de la tâche planifiée.
    pause
    exit /b 1
)

echo [3/3] Lancement de l'agent...
schtasks /run /tn "%TASK_NAME%"

echo.
echo ========================================================
echo   Agent WD12 installé et lancé avec succès en service !
echo   Tâche planifiée : %TASK_NAME%
echo   Dossier         : %AGENT_DIR%
echo ========================================================
echo.
pause
