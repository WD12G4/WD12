using System.Diagnostics;
using System.Net.NetworkInformation;
using System.Runtime.InteropServices;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace MaterielAgent;

public class Config
{
    [JsonPropertyName("server_url")]
    public string ServerUrl { get; set; } = "ws://127.0.0.1:8080/ws/agent";

    [JsonPropertyName("serial_number")]
    public string SerialNumber { get; set; } = string.Empty;

    [JsonPropertyName("reconnect_delay_sec")]
    public int ReconnectDelaySec { get; set; } = 5;

    public TimeSpan ReconnectDelay => TimeSpan.FromSeconds(ReconnectDelaySec > 0 ? ReconnectDelaySec : 5);

    public static Config Load(string[] args)
    {
        var config = new Config();
        string configPath = "config.json";

        // Analyse manuelle des arguments simples style --server / -server / --serial / -config
        for (int i = 0; i < args.Length; i++)
        {
            string arg = args[i];
            if ((arg == "--config" || arg == "-config" || arg == "-c") && i + 1 < args.Length)
            {
                configPath = args[++i];
            }
            else if ((arg == "--server" || arg == "-server" || arg == "-s") && i + 1 < args.Length)
            {
                config.ServerUrl = args[++i];
            }
            else if ((arg == "--serial" || arg == "-serial") && i + 1 < args.Length)
            {
                config.SerialNumber = args[++i];
            }
            else if ((arg == "--reconnect" || arg == "-reconnect" || arg == "-r") && i + 1 < args.Length)
            {
                if (int.TryParse(args[++i], out int sec)) config.ReconnectDelaySec = sec;
            }
        }

        // 1. Lecture du fichier config.json si présent
        if (File.Exists(configPath))
        {
            try
            {
                string json = File.ReadAllText(configPath);
                var loaded = JsonSerializer.Deserialize<Config>(json);
                if (loaded != null)
                {
                    if (!string.IsNullOrWhiteSpace(loaded.ServerUrl)) config.ServerUrl = loaded.ServerUrl;
                    if (!string.IsNullOrWhiteSpace(loaded.SerialNumber)) config.SerialNumber = loaded.SerialNumber;
                    if (loaded.ReconnectDelaySec > 0) config.ReconnectDelaySec = loaded.ReconnectDelaySec;
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine($"[AVERTISSEMENT] Impossible de lire {configPath} : {ex.Message}");
            }
        }

        // 2. Variables d'environnement (prioritaires sur config.json)
        string? envServer = Environment.GetEnvironmentVariable("WD12_SERVER_URL");
        if (!string.IsNullOrWhiteSpace(envServer))
        {
            config.ServerUrl = envServer;
        }

        string? envSerial = Environment.GetEnvironmentVariable("WD12_SERIAL_NUMBER");
        if (!string.IsNullOrWhiteSpace(envSerial))
        {
            config.SerialNumber = envSerial;
        }

        // 3. Ré-application des flags explicites (priorité maximale)
        for (int i = 0; i < args.Length; i++)
        {
            string arg = args[i];
            if ((arg == "--server" || arg == "-server" || arg == "-s") && i + 1 < args.Length)
            {
                config.ServerUrl = args[++i];
            }
            else if ((arg == "--serial" || arg == "-serial") && i + 1 < args.Length)
            {
                config.SerialNumber = args[++i];
            }
            else if ((arg == "--reconnect" || arg == "-reconnect" || arg == "-r") && i + 1 < args.Length)
            {
                if (int.TryParse(args[++i], out int sec)) config.ReconnectDelaySec = sec;
            }
        }

        // 4. Fallback pour SerialNumber si vide : Adresse MAC sans séparateurs ou Hostname
        if (string.IsNullOrWhiteSpace(config.SerialNumber))
        {
            string mac = GetMacAddress();
            string cleanMac = mac.Replace(":", "").Replace("-", "").ToUpperInvariant();
            if (!string.IsNullOrWhiteSpace(cleanMac) && cleanMac != "000000000000")
            {
                config.SerialNumber = cleanMac;
            }
            else
            {
                config.SerialNumber = Environment.MachineName;
            }
        }

        return config;
    }

    public static string GetMacAddress()
    {
        try
        {
            var interfaces = NetworkInterface.GetAllNetworkInterfaces();
            string fallback = "";

            foreach (var iface in interfaces)
            {
                if (iface.OperationalStatus != OperationalStatus.Up ||
                    iface.NetworkInterfaceType == NetworkInterfaceType.Loopback)
                {
                    continue;
                }

                byte[] bytes = iface.GetPhysicalAddress().GetAddressBytes();
                if (bytes.Length == 0) continue;

                string formatted = string.Join(":", bytes.Select(b => b.ToString("X2")));

                if (string.IsNullOrEmpty(fallback))
                {
                    fallback = formatted;
                }

                // Filtrer les interfaces virtuelles courantes (VPN, Docker, Hyper-V, veth, etc.)
                // ou adresses administrées localement (bit 0x02 du premier octet)
                if (IsVirtualInterface(iface.Name, iface.Description) || (bytes[0] & 0x02) != 0)
                {
                    continue;
                }

                return formatted;
            }

            return !string.IsNullOrEmpty(fallback) ? fallback : "00:00:00:00:00:00";
        }
        catch
        {
            return "00:00:00:00:00:00";
        }
    }

    private static bool IsVirtualInterface(string name, string description)
    {
        string[] virtualKeywords =
        {
            "virtual", "vbox", "vmware", "hyper-v", "docker", "wsl", "vpn",
            "tap", "tun", "tailscale", "loopback", "teredo", "pseudo", "wireguard",
            "utun", "awdl", "bridge", "veth", "virbr"
        };

        string n = name.ToLowerInvariant();
        string d = description.ToLowerInvariant();

        return virtualKeywords.Any(k => n.Contains(k) || d.Contains(k));
    }

    /// <summary>
    /// Détecte le nom d'utilisateur de la session active sur le poste de travail (cross-platform).
    /// Si l'agent s'exécute en tant que service système (root sous Linux, SYSTEM sous Windows),
    /// cette méthode recherche l'utilisateur connecté à la console / session graphique.
    /// </summary>
    public static string GetActiveSessionUsername()
    {
        try
        {
            // 1. Si le processus tourne directement sous une session utilisateur (non-root, non-service système)
            string currentUser = Environment.UserName;
            if (!string.IsNullOrWhiteSpace(currentUser) &&
                !currentUser.Equals("root", StringComparison.OrdinalIgnoreCase) &&
                !currentUser.Equals("SYSTEM", StringComparison.OrdinalIgnoreCase) &&
                !currentUser.Equals("LOCAL SERVICE", StringComparison.OrdinalIgnoreCase) &&
                !currentUser.Equals("NETWORK SERVICE", StringComparison.OrdinalIgnoreCase))
            {
                return currentUser;
            }

            // 2. Linux : détection de la session utilisateur connectée
            if (RuntimeInformation.IsOSPlatform(OSPlatform.Linux))
            {
                string? linuxUser = DetectLinuxSessionUser();
                if (!string.IsNullOrWhiteSpace(linuxUser))
                {
                    return linuxUser;
                }
            }

            // 3. macOS : détection via /dev/console
            if (RuntimeInformation.IsOSPlatform(OSPlatform.OSX))
            {
                string? macUser = DetectMacConsoleUser();
                if (!string.IsNullOrWhiteSpace(macUser))
                {
                    return macUser;
                }
            }

            // 4. Windows : détection via session de console si service SYSTEM
            if (RuntimeInformation.IsOSPlatform(OSPlatform.Windows))
            {
                string? winUser = DetectWindowsConsoleUser();
                if (!string.IsNullOrWhiteSpace(winUser))
                {
                    return winUser;
                }
            }

            return !string.IsNullOrWhiteSpace(currentUser) ? currentUser : "inconnu";
        }
        catch
        {
            return Environment.UserName ?? "inconnu";
        }
    }

    private static string? DetectLinuxSessionUser()
    {
        // Méthode A : loginctl (standard systemd)
        try
        {
            using var proc = new Process
            {
                StartInfo = new ProcessStartInfo
                {
                    FileName = "loginctl",
                    Arguments = "list-users --no-legend",
                    RedirectStandardOutput = true,
                    UseShellExecute = false,
                    CreateNoWindow = true
                }
            };
            if (proc.Start())
            {
                string output = proc.StandardOutput.ReadToEnd();
                proc.WaitForExit(1000);
                foreach (var line in output.Split('\n', StringSplitOptions.RemoveEmptyEntries))
                {
                    var parts = line.Trim().Split(' ', StringSplitOptions.RemoveEmptyEntries);
                    if (parts.Length >= 2 && int.TryParse(parts[0], out int uid) && uid >= 1000)
                    {
                        return parts[1];
                    }
                }
            }
        }
        catch { }

        // Méthode B : commande who (utilisateurs loggés)
        try
        {
            using var proc = new Process
            {
                StartInfo = new ProcessStartInfo
                {
                    FileName = "who",
                    RedirectStandardOutput = true,
                    UseShellExecute = false,
                    CreateNoWindow = true
                }
            };
            if (proc.Start())
            {
                string output = proc.StandardOutput.ReadToEnd();
                proc.WaitForExit(1000);
                foreach (var line in output.Split('\n', StringSplitOptions.RemoveEmptyEntries))
                {
                    var parts = line.Trim().Split(' ', StringSplitOptions.RemoveEmptyEntries);
                    if (parts.Length > 0 && !parts[0].Equals("root", StringComparison.OrdinalIgnoreCase))
                    {
                        return parts[0];
                    }
                }
            }
        }
        catch { }

        // Méthode C : corrélation entre /etc/passwd (UID >= 1000) et les processus actifs (/proc)
        try
        {
            if (File.Exists("/etc/passwd"))
            {
                var uidToUser = new Dictionary<int, string>();
                foreach (var line in File.ReadLines("/etc/passwd"))
                {
                    var parts = line.Split(':');
                    if (parts.Length >= 3 && int.TryParse(parts[2], out int uid) && uid >= 1000 && uid < 65534)
                    {
                        uidToUser[uid] = parts[0];
                    }
                }

                foreach (var p in Process.GetProcesses())
                {
                    try
                    {
                        string statusPath = $"/proc/{p.Id}/status";
                        if (File.Exists(statusPath))
                        {
                            foreach (var sLine in File.ReadLines(statusPath))
                            {
                                if (sLine.StartsWith("Uid:", StringComparison.OrdinalIgnoreCase))
                                {
                                    var uParts = sLine.Split('\t', StringSplitOptions.RemoveEmptyEntries);
                                    if (uParts.Length > 1 && int.TryParse(uParts[1], out int rUid) && uidToUser.TryGetValue(rUid, out string? uName))
                                    {
                                        return uName;
                                    }
                                    break;
                                }
                            }
                        }
                    }
                    catch { }
                    finally { p.Dispose(); }
                }
            }
        }
        catch { }

        return null;
    }

    private static string? DetectMacConsoleUser()
    {
        try
        {
            using var proc = new Process
            {
                StartInfo = new ProcessStartInfo
                {
                    FileName = "stat",
                    Arguments = "-f%Su /dev/console",
                    RedirectStandardOutput = true,
                    UseShellExecute = false,
                    CreateNoWindow = true
                }
            };
            if (proc.Start())
            {
                string output = proc.StandardOutput.ReadToEnd().Trim();
                proc.WaitForExit(1000);
                if (!string.IsNullOrWhiteSpace(output) && !output.Equals("root", StringComparison.OrdinalIgnoreCase))
                {
                    return output;
                }
            }
        }
        catch { }
        return null;
    }

    private static string? DetectWindowsConsoleUser()
    {
        try
        {
            using var proc = new Process
            {
                StartInfo = new ProcessStartInfo
                {
                    FileName = "cmd.exe",
                    Arguments = "/c query user",
                    RedirectStandardOutput = true,
                    UseShellExecute = false,
                    CreateNoWindow = true
                }
            };
            if (proc.Start())
            {
                string output = proc.StandardOutput.ReadToEnd();
                proc.WaitForExit(1500);
                foreach (var line in output.Split('\n'))
                {
                    if (line.Contains("Active", StringComparison.OrdinalIgnoreCase) ||
                        line.Contains("Actif", StringComparison.OrdinalIgnoreCase))
                    {
                        string cleaned = line.TrimStart('>', ' ');
                        var parts = cleaned.Split(' ', StringSplitOptions.RemoveEmptyEntries);
                        if (parts.Length > 0) return parts[0];
                    }
                }
            }
        }
        catch { }
        return null;
    }
}
