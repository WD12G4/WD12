package main

import (
	"encoding/json"
	"flag"
	"net"
	"os"
	"strings"
	"time"
)

// Config regroupe les paramètres de l'agent.
type Config struct {
	ServerURL      string        `json:"server_url"`
	SerialNumber   string        `json:"serial_number"`
	ReconnectDelay time.Duration `json:"reconnect_delay_sec"`
	ConfigFile     string        `json:"-"`
}

// LoadConfig charge la configuration avec priorité : flags > variables d'env > config.json > adresse MAC épurée.
func LoadConfig() (*Config, error) {
	cfg := &Config{
		ServerURL:      "ws://127.0.0.1:8080/ws/agent",
		ReconnectDelay: 5 * time.Second,
	}

	serverFlag := flag.String("server", "", "URL WebSocket du serveur WD12 (ex: ws://192.168.1.50:8080/ws/agent)")
	serialFlag := flag.String("serial", "", "Numéro de série de l'UC (identique à celui en base de données)")
	configFlag := flag.String("config", "config.json", "Chemin vers un fichier de configuration JSON")
	reconnectFlag := flag.Int("reconnect", 5, "Délai de reconnexion en secondes")
	flag.Parse()

	// 1. Lecture du fichier JSON s'il existe
	if *configFlag != "" {
		if data, err := os.ReadFile(*configFlag); err == nil {
			_ = json.Unmarshal(data, cfg)
		}
	}

	// 2. Variables d'environnement
	if envServer := os.Getenv("WD12_SERVER_URL"); envServer != "" {
		cfg.ServerURL = envServer
	}
	if envSerial := os.Getenv("WD12_SERIAL_NUMBER"); envSerial != "" {
		cfg.SerialNumber = envSerial
	}

	// 3. Flags ligne de commande
	if *serverFlag != "" {
		cfg.ServerURL = *serverFlag
	}
	if *serialFlag != "" {
		cfg.SerialNumber = *serialFlag
	}
	if *reconnectFlag > 0 {
		cfg.ReconnectDelay = time.Duration(*reconnectFlag) * time.Second
	}

	// 4. Fallback du numéro de série si non spécifié ou vide : adresse MAC sans séparateurs (ex: 000000001415)
	if cfg.SerialNumber == "" {
		mac := GetMacAddress()
		cleanMac := strings.ToUpper(strings.ReplaceAll(strings.ReplaceAll(mac, ":", ""), "-", ""))
		if cleanMac != "" && cleanMac != "000000000000" {
			cfg.SerialNumber = cleanMac
		} else {
			hostname, err := os.Hostname()
			if err == nil && hostname != "" {
				cfg.SerialNumber = hostname
			} else {
				cfg.SerialNumber = "UNKNOWN"
			}
		}
	}

	return cfg, nil
}

// GetMacAddress récupère l'adresse MAC de la première carte réseau active (hors boucle locale), en
// préférant une vraie carte à une interface virtuelle ou à une adresse factice.
func GetMacAddress() string {
	interfaces, err := net.Interfaces()
	if err != nil {
		return "00:00:00:00:00:00"
	}
	fallback := ""
	for _, iface := range interfaces {
		if iface.Flags&net.FlagUp == 0 || iface.Flags&net.FlagLoopback != 0 || len(iface.HardwareAddr) == 0 {
			continue
		}
		if fallback == "" {
			fallback = iface.HardwareAddr.String()
		}
		// On préfère une vraie carte réseau : ni interface virtuelle (VPN, Docker, OrbStack, AirDrop…),
		// ni adresse « administrée localement » (bit 0x02 du premier octet, ex. 02:00:00:00:00:00).
		if isVirtualInterface(iface.Name) || iface.HardwareAddr[0]&0x02 != 0 {
			continue
		}
		return iface.HardwareAddr.String()
	}
	if fallback != "" {
		return fallback
	}
	return "00:00:00:00:00:00"
}

// isVirtualInterface reconnaît les interfaces réseau virtuelles courantes (macOS, Linux, Docker).
func isVirtualInterface(name string) bool {
	prefixes := []string{"utun", "awdl", "llw", "bridge", "anpi", "ap", "gif", "stf", "vmnet", "vboxnet",
		"docker", "br-", "veth", "virbr", "tun", "tap", "zt", "tailscale"}
	lower := strings.ToLower(name)
	for _, prefix := range prefixes {
		if strings.HasPrefix(lower, prefix) {
			return true
		}
	}
	return false
}
