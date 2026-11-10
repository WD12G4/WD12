package main

import (
	"fmt"
	"log"
	"os"
	"runtime"
	"strings"
	"time"

	"github.com/shirou/gopsutil/v3/process"
)

// ProcessInfo représente les métadonnées d'un processus utilisateur.
type ProcessInfo struct {
	PID       int32  `json:"pid"`
	Name      string `json:"name"`
	StartedAt int64  `json:"startedAt"` // Millisecondes
}

// KillResult contient le résultat de l'opération d'arrêt d'un processus.
type KillResult struct {
	Success     bool   `json:"success"`
	PID         int32  `json:"pid"`
	ProcessName string `json:"processName,omitempty"`
	Error       string `json:"error,omitempty"`
}

// protectedNames regroupe les processus critiques indispensables sous Windows et Linux.
var protectedNames = map[string]bool{
	// Windows
	"system":            true,
	"registry":          true,
	"smss.exe":          true,
	"csrss.exe":         true,
	"wininit.exe":       true,
	"services.exe":      true,
	"lsass.exe":         true,
	"svchost.exe":       true,
	"fontdrvhost.exe":   true,
	"winlogon.exe":      true,
	"dwm.exe":           true,
	"explorer.exe":      true,
	"spoolsv.exe":       true,
	"taskhostw.exe":     true,
	"sihost.exe":        true,
	"runtimebroker.exe": true,

	// Linux
	"systemd":        true,
	"init":           true,
	"kthreadd":       true,
	"sshd":           true,
	"dbus-daemon":    true,
	"networkmanager": true,
	"xorg":           true,
	"wayland":        true,
	"gnome-shell":    true,
	"polkitd":        true,
	"rsyslogd":       true,
	"cron":           true,
	"crond":          true,
}

// IsSystemOrProtected détermine si un processus est un composant système ou critique.
func IsSystemOrProtected(p *process.Process) bool {
	// 1. PIDs système universels et PID de l'agent
	if p.Pid <= 4 || p.Pid == int32(os.Getpid()) {
		return true
	}

	// 2. Vérification par UID sous Linux / Unix
	// Tout UID < 1000 correspond aux comptes démons / système / root
	if runtime.GOOS != "windows" {
		uids, err := p.Uids()
		if err == nil && len(uids) > 0 {
			// Premier UID utilisateur : 1000 sous Linux, 501 sous macOS (voir platform.go)
			if uids[0] < int32(minUserUID()) {
				return true
			}
		}
	}

	// 3. Vérification du nom dans la liste noire
	name, err := p.Name()
	if err == nil {
		cleanName := strings.ToLower(strings.TrimSpace(name))
		if protectedNames[cleanName] {
			return true
		}
	}

	// 4. Vérification par nom d'utilisateur (spécifiquement sous Windows : SYSTEM, etc.)
	username, err := p.Username()
	if err == nil {
		u := strings.ToLower(username)
		systemAccounts := []string{"nt authority", "system", "local service", "network service", "root"}
		for _, acc := range systemAccounts {
			if strings.Contains(u, acc) {
				return true
			}
		}
	}

	return false
}

// GetUserProcesses inspecte la machine et renvoie uniquement les processus utilisateurs (RG-25).
func GetUserProcesses() []ProcessInfo {
	procs, err := process.Processes()
	if err != nil {
		log.Printf("Erreur lors de la récupération des processus : %v", err)
		return nil
	}

	result := make([]ProcessInfo, 0, len(procs))
	for _, p := range procs {
		if IsSystemOrProtected(p) || isHiddenOnThisOS(p) {
			continue
		}

		name, err := p.Name()
		if err != nil || name == "" {
			continue
		}
		name = displayName(p, name)

		createTime, err := p.CreateTime()
		if err != nil {
			createTime = time.Now().UnixMilli()
		}

		result = append(result, ProcessInfo{
			PID:       p.Pid,
			Name:      name,
			StartedAt: createTime,
		})
	}

	return result
}

// waitForProcessExit vérifie activement si le processus a effectivement quitté les tables du système d'exploitation.
func waitForProcessExit(pid int32, timeout time.Duration) bool {
	deadline := time.Now().Add(timeout)
	for time.Now().Before(deadline) {
		exists, err := process.PidExists(pid)
		if err != nil || !exists {
			return true
		}
		time.Sleep(100 * time.Millisecond)
	}
	exists, err := process.PidExists(pid)
	return err == nil && !exists
}

// KillUserProcess tente d'arrêter un processus après validation de sécurité (RG-26)
// et confirme formellement la fin du processus avant de renvoyer le résultat pour l'enregistrement du KillEvent.
func KillUserProcess(pid int32) KillResult {
	p, err := process.NewProcess(pid)
	if err != nil {
		return KillResult{
			Success: false,
			PID:     pid,
			Error:   "Processus introuvable (déjà arrêté ou inexistant)",
		}
	}

	name, _ := p.Name()
	name = displayName(p, name)

	// Sécurité absolue : refus catégorique d'arrêter un processus système ou protégé
	if IsSystemOrProtected(p) {
		log.Printf("Tentative d'arrêt bloquée pour PID=%d (%s) : processus système ou protégé", pid, name)
		return KillResult{
			Success:     false,
			PID:         pid,
			ProcessName: name,
			Error:       "Action refusée : ce processus est un composant système ou protégé",
		}
	}

	// Tentative 1 : Demande d'arrêt gracieux (SIGTERM sous Linux / WM_CLOSE sous Windows)
	_ = p.Terminate()

	// Vérification active de la terminaison effective du processus (jusqu'à 1.5 seconde)
	if !waitForProcessExit(pid, 1500*time.Millisecond) {
		log.Printf("Arrêt gracieux inefficace pour PID=%d (%s), déclenchement d'un arrêt forcé...", pid, name)
		// Tentative 2 : Arrêt forcé immédiat si le processus tourne toujours
		if killErr := p.Kill(); killErr != nil {
			log.Printf("Échec de l'arrêt forcé pour PID=%d (%s) : %v", pid, name, killErr)
			return KillResult{
				Success:     false,
				PID:         pid,
				ProcessName: name,
				Error:       fmt.Sprintf("Échec de l'arrêt forcé : %v", killErr),
			}
		}

		// Confirmation après arrêt forcé (jusqu'à 1 seconde)
		if !waitForProcessExit(pid, 1000*time.Millisecond) {
			log.Printf("Processus PID=%d (%s) toujours en cours après tentative de kill", pid, name)
			return KillResult{
				Success:     false,
				PID:         pid,
				ProcessName: name,
				Error:       "Le processus n'a pas pu être arrêté",
			}
		}
	}

	log.Printf("Processus arrêté et confirmé avec succès [PID=%d, Nom=%s]", pid, name)
	return KillResult{
		Success:     true,
		PID:         pid,
		ProcessName: name,
	}
}
