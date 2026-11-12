@echo off
:: ========================================================
:: Arret et desinstallation du service Agent WD12
:: Doit etre execute en tant qu'Administrateur
:: ========================================================

net session >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERREUR] Ce script doit etre execute en tant qu'Administrateur.
    echo Clic droit sur le fichier -^> "Executer en tant qu'administrateur".
    pause
    exit /b 1
)

set "TASK_NAME=MaterielAgent"

echo [1/2] Arret de l'agent...
taskkill /f /im MaterielAgent.exe >nul 2>&1
schtasks /end /tn "%TASK_NAME%" >nul 2>&1

echo [2/2] Suppression du demarrage automatique...
schtasks /delete /tn "%TASK_NAME%" /f >nul 2>&1

echo.
echo ========================================================
echo   Agent WD12 arrete et desinstalle avec succes !
echo ========================================================
echo.
pause
