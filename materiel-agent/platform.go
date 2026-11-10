package main

import (
	"path/filepath"
	"runtime"
	"strings"

	"github.com/shirou/gopsutil/v3/process"
)

// minUserUID renvoie le premier UID d'un compte utilisateur (en dessous : comptes système / démons).
// Linux : 1000. macOS : 501 (les comptes créés dans Réglages commencent à 501).
func minUserUID() uint32 {
	if runtime.GOOS == "darwin" {
		return 501
	}
	return 1000
}

// macAppBundle renvoie le nom de l'application macOS dont le processus est l'exécutable principal,
// ou "" s'il ne s'agit pas d'une application visible (voir appBundleFromExe).
func macAppBundle(p *process.Process) string {
	exe, err := p.Exe()
	if err != nil {
		return ""
	}
	return appBundleFromExe(exe)
}

// appBundleFromExe renvoie le nom de l'application (« Visual Studio Code ») si exe est l'exécutable
// principal d'une application (.app/Contents/MacOS/…), sinon "" : composants internes du système
// (/System/Library, /usr…) et processus d'aide (« … Helper », application imbriquée dans une autre).
func appBundleFromExe(exe string) string {
	if exe == "" || strings.HasPrefix(exe, "/System/Library/") || strings.HasPrefix(exe, "/Library/Apple/") || strings.HasPrefix(exe, "/usr/") {
		return ""
	}
	marker := ".app/Contents/MacOS/"
	idx := strings.Index(exe, marker)
	if idx < 0 {
		return ""
	}
	if strings.Count(exe[:idx+len(marker)], ".app/") > 1 || strings.Contains(strings.ToLower(filepath.Base(exe)), "helper") {
		return ""
	}
	return filepath.Base(exe[:idx])
}

// isHiddenOnThisOS écarte, sous macOS, tout ce qui n'est pas une application visible (voir macAppBundle) :
// un compte Mac fait tourner des centaines de processus de fond (Dock, services, aides des navigateurs).
func isHiddenOnThisOS(p *process.Process) bool {
	return runtime.GOOS == "darwin" && macAppBundle(p) == ""
}

// displayName renvoie le nom à afficher : le nom de l'application sous macOS, sinon le nom du processus.
func displayName(p *process.Process, fallback string) string {
	if runtime.GOOS == "darwin" {
		if app := macAppBundle(p); app != "" {
			return app
		}
	}
	return fallback
}
