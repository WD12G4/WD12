package main

import (
	"encoding/json"
	"log"
	"os"
	"os/signal"
	"runtime"
	"syscall"
	"time"

	"github.com/gorilla/websocket"
)

// InboundCommand représente un ordre envoyé par le serveur Spring Boot.
type InboundCommand struct {
	Action    string `json:"action"` // "GET_PROCESSES", "KILL_PROCESS"
	RequestId string `json:"requestId"`
	PID       int32  `json:"pid,omitempty"`
}

// OutboundResponse représente les messages envoyés au serveur.
type OutboundResponse struct {
	Type         string        `json:"type"` // "REGISTER", "PROCESS_LIST", "KILL_RESULT", "PONG"
	RequestId    string        `json:"requestId,omitempty"`
	SerialNumber string        `json:"serialNumber"`
	Hostname     string        `json:"hostname,omitempty"`
	MacAddress   string        `json:"macAddress,omitempty"`
	OS           string        `json:"os,omitempty"`
	Processes    []ProcessInfo `json:"processes,omitempty"`
	Result       *KillResult   `json:"result,omitempty"`
}

func main() {
	cfg, err := LoadConfig()
	if err != nil {
		log.Fatalf("Erreur de configuration : %v", err)
	}

	hostname, _ := os.Hostname()
	macAddress := GetMacAddress()

	log.Println("==========================================================")
	log.Println("  Agent de Supervision WD12 (Windows & Linux)")
	log.Println("==========================================================")
	log.Printf("Système : %s/%s", runtime.GOOS, runtime.GOARCH)
	log.Printf("Machine : %s", hostname)
	log.Printf("Adresse MAC : %s", macAddress)
	log.Printf("UC Serial Number : %s", cfg.SerialNumber)
	log.Printf("Serveur cible : %s", cfg.ServerURL)
	log.Println("==========================================================")

	// Gestion des signaux d'arrêt propres (SIGINT, SIGTERM)
	interrupt := make(chan os.Signal, 1)
	signal.Notify(interrupt, os.Interrupt, syscall.SIGTERM)

	for {
		log.Printf("Tentative de connexion à %s...", cfg.ServerURL)
		conn, _, dialErr := websocket.DefaultDialer.Dial(cfg.ServerURL, nil)
		if dialErr != nil {
			log.Printf("Échec de connexion au serveur (%v). Reconnexion dans %v...", dialErr, cfg.ReconnectDelay)
			select {
			case <-time.After(cfg.ReconnectDelay):
				continue
			case <-interrupt:
				log.Println("Arrêt de l'agent demandé.")
				return
			}
		}

		log.Println("Connecté au serveur de supervision WD12 avec succès.")

		// 1. Enregistrement initial
		registerMsg := OutboundResponse{
			Type:         "REGISTER",
			SerialNumber: cfg.SerialNumber,
			Hostname:     hostname,
			MacAddress:   macAddress,
			OS:           runtime.GOOS,
		}
		if writeErr := conn.WriteJSON(registerMsg); writeErr != nil {
			log.Printf("Erreur lors de l'enregistrement : %v", writeErr)
			_ = conn.Close()
			continue
		}

		// Canal pour notifier la fermeture de la connexion
		connClosed := make(chan struct{})

		// Goroutine de lecture des commandes serveur
		go func() {
			defer close(connClosed)
			for {
				_, messageBytes, readErr := conn.ReadMessage()
				if readErr != nil {
					log.Printf("Connexion fermée par le serveur : %v", readErr)
					return
				}

				var cmd InboundCommand
				if jsonErr := json.Unmarshal(messageBytes, &cmd); jsonErr != nil {
					log.Printf("Message JSON non reconnu : %v", jsonErr)
					continue
				}

				switch cmd.Action {
				case "GET_PROCESSES":
					log.Printf("Demande de la liste des processus reçue (requestId=%s)", cmd.RequestId)
					procs := GetUserProcesses()
					resp := OutboundResponse{
						Type:         "PROCESS_LIST",
						RequestId:    cmd.RequestId,
						SerialNumber: cfg.SerialNumber,
						Processes:    procs,
					}
					_ = conn.WriteJSON(resp)

				case "KILL_PROCESS":
					log.Printf("Ordre d'arrêt reçu pour PID=%d (requestId=%s)", cmd.PID, cmd.RequestId)
					res := KillUserProcess(cmd.PID)
					resp := OutboundResponse{
						Type:         "KILL_RESULT",
						RequestId:    cmd.RequestId,
						SerialNumber: cfg.SerialNumber,
						Result:       &res,
					}
					_ = conn.WriteJSON(resp)

				default:
					log.Printf("Action non supportée : %s", cmd.Action)
				}
			}
		}()

		// Goroutine de heartbeat périodique (toutes les 30 secondes)
		ticker := time.NewTicker(30 * time.Second)

		// Boucle d'attente d'événement
		terminated := false
		for !terminated {
			select {
			case <-connClosed:
				ticker.Stop()
				terminated = true

			case <-ticker.C:
				_ = conn.WriteJSON(OutboundResponse{
					Type:         "PONG",
					SerialNumber: cfg.SerialNumber,
				})

			case <-interrupt:
				ticker.Stop()
				log.Println("Fermeture propre de la connexion agent...")
				_ = conn.WriteMessage(websocket.CloseMessage, websocket.FormatCloseMessage(websocket.CloseNormalClosure, "Agent stopped"))
				_ = conn.Close()
				return
			}
		}

		log.Printf("Nouvelle tentative de connexion dans %v...", cfg.ReconnectDelay)
		time.Sleep(cfg.ReconnectDelay)
	}
}
