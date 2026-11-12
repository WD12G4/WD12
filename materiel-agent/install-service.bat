@echo off
:: ========================================================
:: Installation et demarrage automatique de l'Agent WD12
:: Doit etre execute en tant qu'Administrateur
:: ========================================================

net session >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERREUR] Ce script doit etre execute en tant qu'Administrateur.
    echo Clic droit sur le fichier -^> "Executer en tant qu'administrateur".
    pause
    exit /b 1
)

set "AGENT_DIR=%~dp0"
if "%AGENT_DIR:~-1%"=="\" set "AGENT_DIR=%AGENT_DIR:~0,-1%"
set "EXE_PATH=%AGENT_DIR%\MaterielAgent.exe"
set "TASK_NAME=MaterielAgent"

if not exist "%EXE_PATH%" (
    echo [ERREUR] Fichier introuvable : %EXE_PATH%
    pause
    exit /b 1
)

echo [1/3] Arret des instances precedentes...
taskkill /f /im MaterielAgent.exe >nul 2>&1
schtasks /end /tn "%TASK_NAME%" >nul 2>&1

echo [2/3] Configuration du demarrage automatique...
schtasks /create /tn "%TASK_NAME%" /tr "\"%EXE_PATH%\"" /sc onstart /ru "SYSTEM" /rl HIGHEST /f

if %errorlevel% neq 0 (
    echo [ERREUR] Echec de creation de la tache planifiee.
    pause
    exit /b 1
)

echo [3/3] Lancement de l'agent...
schtasks /run /tn "%TASK_NAME%"

echo.
echo ========================================================
echo   Agent WD12 installe et lance avec succes !
echo   Tache planifiee : %TASK_NAME%
echo   Dossier         : %AGENT_DIR%
echo ========================================================
echo.
pause
