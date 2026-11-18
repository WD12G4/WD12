using System.Diagnostics;
using System.Net.NetworkInformation;
using System.Runtime.InteropServices;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace MaterielAgent;

public partial class Config
{
    private static readonly string[] VirtualKeywords =
    [
        "virtual", "vbox", "vmware", "hyper-v", "docker", "wsl", "vpn",
        "tap", "tun", "tailscale", "loopback", "teredo", "pseudo", "wireguard",
        "utun", "awdl", "bridge", "veth", "virbr"
    ];

    [JsonPropertyName("server_url")]
    public string ServerUrl { get; set; } = "ws://127.0.0.1:8080/ws/agent";

    [JsonPropertyName("serial_number")]
    public string SerialNumber { get; set; } = string.Empty;

    [JsonPropertyName("shared_secret")]
    public string SharedSecret { get; set; } = "wd12-supervision-secret-change-in-prod";

    [JsonPropertyName("reconnect_delay_sec")]
    public int ReconnectDelaySec { get; set; } = 5;

    [JsonPropertyName("allow_self_signed_cert")]
    public bool AllowSelfSignedCert { get; set; } = true;

    public TimeSpan ReconnectDelay => TimeSpan.FromSeconds(ReconnectDelaySec > 0 ? ReconnectDelaySec : 5);

    public static Config Load(string[] args)
    {
        var config = new Config();
        string configPath = "config.json";

        // Analyse initiale des arguments simples (ex: chemin de config ou flags initiaux)
        ParseCommandLineArgs(args, config, ref configPath);

        // 1. Lecture du fichier config.json si présent
        // Si le chemin n'est pas absolu et n'existe pas dans le répertoire de travail actuel
        // (cas classique d'une tâche planifiée ou d'un service où CWD = C:\Windows\System32),
        // on cherche également à côté du binaire exécutable.
        if (!File.Exists(configPath) && !Path.IsPathRooted(configPath))
        {
            string appDirConfig = Path.Combine(AppContext.BaseDirectory, configPath);
            if (File.Exists(appDirConfig))
            {
                configPath = appDirConfig;
            }
        }

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
                    if (!string.IsNullOrWhiteSpace(loaded.SharedSecret)) config.SharedSecret = loaded.SharedSecret;
                    if (loaded.ReconnectDelaySec > 0) config.ReconnectDelaySec = loaded.ReconnectDelaySec;
                    config.AllowSelfSignedCert = loaded.AllowSelfSignedCert;
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

        string? envSecret = Environment.GetEnvironmentVariable("WD12_AGENT_SECRET");
        if (!string.IsNullOrWhiteSpace(envSecret))
        {
            config.SharedSecret = envSecret;
        }

        string? envInsecure = Environment.GetEnvironmentVariable("WD12_ALLOW_SELF_SIGNED_CERT");
        if (!string.IsNullOrWhiteSpace(envInsecure) && bool.TryParse(envInsecure, out bool allowCert))
        {
            config.AllowSelfSignedCert = allowCert;
        }

        // 3. Ré-application des flags explicites (priorité maximale sur l'environnement et le fichier)
        ParseCommandLineArgs(args, config, ref configPath);

        // 4. Fallback pour SerialNumber si vide : Adresse MAC sans séparateurs ou Hostname
        if (string.IsNullOrWhiteSpace(config.SerialNumber))
        {
            string mac = GetMacAddress();
            string cleanMac = mac.Replace(":", "", StringComparison.Ordinal).Replace("-", "", StringComparison.Ordinal).ToUpperInvariant();
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

    private static void ParseCommandLineArgs(string[] args, Config config, ref string configPath)
    {
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
            else if ((arg == "--secret" || arg == "-secret" || arg == "-k") && i + 1 < args.Length)
            {
                config.SharedSecret = args[++i];
            }
            else if ((arg == "--reconnect" || arg == "-reconnect" || arg == "-r") && i + 1 < args.Length)
            {
                if (int.TryParse(args[++i], out int sec)) config.ReconnectDelaySec = sec;
            }
            else if (arg == "--insecure" || arg == "--allow-self-signed")
            {
                config.AllowSelfSignedCert = true;
            }
            else if (arg == "--strict-ssl")
            {
                config.AllowSelfSignedCert = false;
            }
        }
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
        catch (Exception)
        {
            return "00:00:00:00:00:00";
        }
    }

    internal static bool IsVirtualInterface(string name, string description)
    {
        string n = name.ToLowerInvariant();
        string d = description.ToLowerInvariant();

        return VirtualKeywords.Any(k => n.Contains(k, StringComparison.Ordinal) || d.Contains(k, StringComparison.Ordinal));
    }

    /// <summary>
    /// Détecte le nom d'utilisateur de la session active sur le poste de travail (cross-platform).
    /// Si l'agent s'exécute en tant que service système (root sous Linux, SYSTEM / MACHINE$ sous Windows),
    /// cette méthode recherche l'utilisateur connecté à la console / session graphique.
    /// </summary>
    public static string? GetActiveSessionUsername()
    {
        try
        {
            string currentUser = Environment.UserName;
            bool isSystemAccount = string.IsNullOrWhiteSpace(currentUser) ||
                currentUser.Equals("root", StringComparison.OrdinalIgnoreCase) ||
                currentUser.Equals("SYSTEM", StringComparison.OrdinalIgnoreCase) ||
                currentUser.Equals("LOCAL SERVICE", StringComparison.OrdinalIgnoreCase) ||
                currentUser.Equals("NETWORK SERVICE", StringComparison.OrdinalIgnoreCase) ||
                currentUser.EndsWith('$') ||
                currentUser.Equals(Environment.MachineName + "$", StringComparison.OrdinalIgnoreCase);

            // 1. Si le processus tourne directement sous une session utilisateur (non-root, non-service système)
            if (!isSystemAccount)
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

            // 4. Windows : détection via session de console si service SYSTEM ou compte machine (ex: DESKTOP-XXX$)
            if (RuntimeInformation.IsOSPlatform(OSPlatform.Windows))
            {
                string? winUser = DetectWindowsConsoleUser();
                if (!string.IsNullOrWhiteSpace(winUser))
                {
                    return winUser;
                }
            }

            // Si c'est un compte système (root, SYSTEM, Machine$) et qu'aucun utilisateur n'est connecté à la console
            if (isSystemAccount)
            {
                return null;
            }

            return !string.IsNullOrWhiteSpace(currentUser) ? currentUser : null;
        }
        catch (Exception)
        {
            return null;
        }
    }

    internal static string? DetectLinuxSessionUser()
    {
        // Méthode A : loginctl (standard systemd)
        try
        {
            string loginctlPath = File.Exists("/usr/bin/loginctl") ? "/usr/bin/loginctl" : "/bin/loginctl";
            using var proc = new Process
            {
                StartInfo = new ProcessStartInfo
                {
                    FileName = loginctlPath,
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
        catch (Exception)
        {
            // loginctl non disponible
        }

        // Méthode B : commande who (utilisateurs loggés)
        try
        {
            string whoPath = File.Exists("/usr/bin/who") ? "/usr/bin/who" : "/bin/who";
            using var proc = new Process
            {
                StartInfo = new ProcessStartInfo
                {
                    FileName = whoPath,
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
        catch (Exception)
        {
            // who non disponible
        }

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
                    catch (Exception)
                    {
                        // Processus fermé ou inaccessible
                    }
                    finally
                    {
                        p.Dispose();
                    }
                }
            }
        }
        catch (Exception)
        {
            // /etc/passwd inaccessible
        }

        return null;
    }

    internal static string? DetectMacConsoleUser()
    {
        try
        {
            using var proc = new Process
            {
                StartInfo = new ProcessStartInfo
                {
                    FileName = "/usr/bin/stat",
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
        catch (Exception)
        {
            // Commande stat macOS inaccessible
        }
        return null;
    }

    internal static partial class NativeMethods
    {
        [LibraryImport("kernel32.dll", SetLastError = true)]
        internal static partial uint WTSGetActiveConsoleSessionId();

        [LibraryImport("wtsapi32.dll", EntryPoint = "WTSQuerySessionInformationW", SetLastError = true)]
        [return: MarshalAs(UnmanagedType.Bool)]
        internal static partial bool WTSQuerySessionInformationW(
            IntPtr hServer,
            uint sessionId,
            int wtsInfoClass,
            out IntPtr ppBuffer,
            out uint pBytesReturned);

        [LibraryImport("wtsapi32.dll")]
        internal static partial void WTSFreeMemory(IntPtr pMemory);
    }

    private const int WtsUserName = 5;

    internal static string? DetectWindowsConsoleUser()
    {
        // Méthode 1 : API native Win32 WTS (extrêmement rapide et fiable depuis Session 0 / SYSTEM)
        try
        {
            uint sessionId = NativeMethods.WTSGetActiveConsoleSessionId();
            if (sessionId != 0xFFFFFFFF)
            {
                IntPtr buffer = IntPtr.Zero;
                uint bytesReturned = 0;
                if (NativeMethods.WTSQuerySessionInformationW(IntPtr.Zero, sessionId, WtsUserName, out buffer, out bytesReturned) && buffer != IntPtr.Zero)
                {
                    try
                    {
                        string? user = Marshal.PtrToStringUni(buffer);
                        if (!string.IsNullOrWhiteSpace(user) &&
                            !user.Equals("SYSTEM", StringComparison.OrdinalIgnoreCase) &&
                            !user.Equals("LOCAL SERVICE", StringComparison.OrdinalIgnoreCase) &&
                            !user.Equals("NETWORK SERVICE", StringComparison.OrdinalIgnoreCase) &&
                            !user.EndsWith('$'))
                        {
                            return user;
                        }
                    }
                    finally
                    {
                        NativeMethods.WTSFreeMemory(buffer);
                    }
                }
            }
        }
        catch (Exception)
        {
            // Échec WTS API
        }

        // Méthode 2 : Fallback via query user
        try
        {
            string cmdPath = Path.Combine(Environment.SystemDirectory, "cmd.exe");
            using var proc = new Process
            {
                StartInfo = new ProcessStartInfo
                {
                    FileName = cmdPath,
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
                foreach (var line in output.Split('\n', StringSplitOptions.RemoveEmptyEntries))
                {
                    if (line.Contains("Active", StringComparison.OrdinalIgnoreCase) ||
                        line.Contains("Actif", StringComparison.OrdinalIgnoreCase))
                    {
                        string cleaned = line.TrimStart('>', ' ');
                        var parts = cleaned.Split(' ', StringSplitOptions.RemoveEmptyEntries);
                        if (parts.Length > 0 &&
                            !parts[0].Equals("SYSTEM", StringComparison.OrdinalIgnoreCase) &&
                            !parts[0].Equals("LOCAL SERVICE", StringComparison.OrdinalIgnoreCase) &&
                            !parts[0].Equals("NETWORK SERVICE", StringComparison.OrdinalIgnoreCase) &&
                            !parts[0].EndsWith('$'))
                        {
                            return parts[0];
                        }
                    }
                }
            }
        }
        catch (Exception)
        {
            // query user inaccessible
        }
        return null;
    }
}
