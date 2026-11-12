@echo off
chcp 65001 >nul
:: ========================================================
:: Arrêt et désinstallation du service Agent WD12
:: Doit être exécuté en tant qu'Administrateur
:: ========================================================

net session >nul 2>&1
if %errorLevel% neq 0 (
    echo [ERREUR] Ce script doit être exécuté en tant qu'Administrateur.
    echo Veuillez faire un clic droit -> "Exécuter en tant qu'administrateur".
    pause
    exit /b 1
)

set "TASK_NAME=MaterielAgent"

echo [1/2] Arrêt de l'agent...
taskkill /f /im MaterielAgent.exe >nul 2>&1
schtasks /end /tn "%TASK_NAME%" >nul 2>&1

echo [2/2] Suppression du démarrage automatique...
schtasks /delete /tn "%TASK_NAME%" /f >nul 2>&1

echo.
echo ========================================================
echo   Agent WD12 arrêté et désinstallé avec succès !
echo ========================================================
echo.
pause
