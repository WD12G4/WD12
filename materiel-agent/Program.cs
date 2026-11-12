using System.Net.WebSockets;
using System.Runtime.InteropServices;
using System.Text;
using System.Text.Json;

namespace MaterielAgent;

public class Program
{
    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        PropertyNameCaseInsensitive = true
    };

    public static async Task Main(string[] args)
    {
        var cfg = Config.Load(args);
        string hostname = Environment.MachineName;
        string macAddress = Config.GetMacAddress();
        string sessionUser = Config.GetActiveSessionUsername();
        string os = RuntimeInformation.IsOSPlatform(OSPlatform.Windows) ? "windows" :
                    RuntimeInformation.IsOSPlatform(OSPlatform.Linux) ? "linux" : "osx";

        Console.WriteLine("==========================================================");
        Console.WriteLine("  Agent de Supervision WD12 (.NET 8 C#)");
        Console.WriteLine("==========================================================");
        Console.WriteLine($"Système         : {RuntimeInformation.OSDescription} ({RuntimeInformation.OSArchitecture})");
        Console.WriteLine($"Machine         : {hostname}");
        Console.WriteLine($"Adresse MAC     : {macAddress}");
        Console.WriteLine($"Session active  : {sessionUser}");
        Console.WriteLine($"UC Serial Number: {cfg.SerialNumber}");
        Console.WriteLine($"Serveur cible   : {cfg.ServerUrl}");
        Console.WriteLine("==========================================================");

        using var cts = new CancellationTokenSource();
        Console.CancelKeyPress += (sender, e) =>
        {
            e.Cancel = true;
            Console.WriteLine("\nArrêt de l'agent demandé (Ctrl+C)...");
            cts.Cancel();
        };

        while (!cts.IsCancellationRequested)
        {
            Console.WriteLine($"Tentative de connexion à {cfg.ServerUrl}...");

            using var ws = new ClientWebSocket();
            try
            {
                await ws.ConnectAsync(new Uri(cfg.ServerUrl), cts.Token);
            }
            catch (Exception ex) when (!cts.IsCancellationRequested)
            {
                Console.WriteLine($"Échec de connexion ({ex.Message}). Reconnexion dans {cfg.ReconnectDelaySec}s...");
                try
                {
                    await Task.Delay(cfg.ReconnectDelay, cts.Token);
                }
                catch (OperationCanceledException)
                {
                    break;
                }
                continue;
            }

            Console.WriteLine("Connecté au serveur de supervision WD12 avec succès.");

            try
            {
                // 1. Enregistrement initial (REGISTER)
                var registerMsg = new OutboundResponse
                {
                    Type = "REGISTER",
                    SerialNumber = cfg.SerialNumber,
                    Hostname = hostname,
                    MacAddress = macAddress,
                    Username = Config.GetActiveSessionUsername(),
                    Os = os
                };

                await SendJsonAsync(ws, registerMsg, cts.Token);

                // 2. Tâche de boucle Heartbeat (PONG / Ping toutes les 30s)
                using var linkCts = CancellationTokenSource.CreateLinkedTokenSource(cts.Token);
                var heartbeatTask = RunHeartbeatAsync(ws, cfg.SerialNumber, linkCts.Token);

                // 3. Boucle de réception des commandes serveur
                await ReceiveLoopAsync(ws, cfg.SerialNumber, linkCts.Token);

                linkCts.Cancel();
                await Task.WhenAny(heartbeatTask, Task.Delay(500));
            }
            catch (OperationCanceledException)
            {
                break;
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Connexion interrompue : {ex.Message}");
            }
            finally
            {
                if (ws.State == WebSocketState.Open)
                {
                    try
                    {
                        await ws.CloseAsync(WebSocketCloseStatus.NormalClosure, "Agent stopping", CancellationToken.None);
                    }
                    catch { }
                }
            }

            if (!cts.IsCancellationRequested)
            {
                Console.WriteLine($"Nouvelle tentative dans {cfg.ReconnectDelaySec}s...");
                try
                {
                    await Task.Delay(cfg.ReconnectDelay, cts.Token);
                }
                catch (OperationCanceledException)
                {
                    break;
                }
            }
        }

        Console.WriteLine("Agent arrêté proprement.");
    }

    private static async Task RunHeartbeatAsync(ClientWebSocket ws, string serialNumber, CancellationToken ct)
    {
        try
        {
            while (!ct.IsCancellationRequested && ws.State == WebSocketState.Open)
            {
                await Task.Delay(TimeSpan.FromSeconds(30), ct);

                var pong = new OutboundResponse
                {
                    Type = "PONG",
                    SerialNumber = serialNumber,
                    Username = Config.GetActiveSessionUsername()
                };

                await SendJsonAsync(ws, pong, ct);
            }
        }
        catch (OperationCanceledException) { }
        catch (Exception ex)
        {
            Console.WriteLine($"[Heartbeat] Erreur : {ex.Message}");
        }
    }

    private static async Task ReceiveLoopAsync(ClientWebSocket ws, string serialNumber, CancellationToken ct)
    {
        var buffer = new byte[8192];
        var ms = new MemoryStream();

        while (!ct.IsCancellationRequested && ws.State == WebSocketState.Open)
        {
            ms.SetLength(0);
            WebSocketReceiveResult result;
            do
            {
                result = await ws.ReceiveAsync(new ArraySegment<byte>(buffer), ct);
                if (result.MessageType == WebSocketMessageType.Close)
                {
                    Console.WriteLine("Fermeture reçue du serveur.");
                    return;
                }
                ms.Write(buffer, 0, result.Count);
            } while (!result.EndOfMessage);

            ms.Seek(0, SeekOrigin.Begin);
            string json = Encoding.UTF8.GetString(ms.ToArray());

            try
            {
                var cmd = JsonSerializer.Deserialize<InboundCommand>(json, JsonOptions);
                if (cmd == null || string.IsNullOrEmpty(cmd.Action))
                {
                    Console.WriteLine($"[AVERTISSEMENT] Commande non reconnue : {json}");
                    continue;
                }

                switch (cmd.Action)
                {
                    case "GET_PROCESSES":
                        Console.WriteLine($"Demande GET_PROCESSES reçue (requestId={cmd.RequestId})");
                        var procs = ProcessManager.GetUserProcesses();
                        var procResp = new OutboundResponse
                        {
                            Type = "PROCESS_LIST",
                            RequestId = cmd.RequestId,
                            SerialNumber = serialNumber,
                            Username = Config.GetActiveSessionUsername(),
                            Processes = procs
                        };
                        await SendJsonAsync(ws, procResp, ct);
                        break;

                    case "KILL_PROCESS":
                        int pid = cmd.Pid ?? 0;
                        Console.WriteLine($"Demande KILL_PROCESS reçue pour PID={pid} (requestId={cmd.RequestId})");
                        var killRes = ProcessManager.KillUserProcess(pid);
                        var killResp = new OutboundResponse
                        {
                            Type = "KILL_RESULT",
                            RequestId = cmd.RequestId,
                            SerialNumber = serialNumber,
                            Result = killRes
                        };
                        await SendJsonAsync(ws, killResp, ct);
                        break;

                    default:
                        Console.WriteLine($"[AVERTISSEMENT] Action inconnue reçue : {cmd.Action}");
                        break;
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine($"[ERREUR] Échec de traitement du message serveur : {ex.Message}");
            }
        }
    }

    private static async Task SendJsonAsync(ClientWebSocket ws, object payload, CancellationToken ct)
    {
        if (ws.State != WebSocketState.Open) return;

        string json = JsonSerializer.Serialize(payload, JsonOptions);
        byte[] bytes = Encoding.UTF8.GetBytes(json);
        await ws.SendAsync(new ArraySegment<byte>(bytes), WebSocketMessageType.Text, true, ct);
    }
}
