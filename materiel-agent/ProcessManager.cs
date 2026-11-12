using System.Diagnostics;
using System.Runtime.InteropServices;

namespace MaterielAgent;

public static class ProcessManager
{
    private static readonly HashSet<string> ProtectedNames = new(StringComparer.OrdinalIgnoreCase)
    {
        // Windows
        "system",
        "registry",
        "smss.exe",
        "csrss.exe",
        "wininit.exe",
        "services.exe",
        "lsass.exe",
        "svchost.exe",
        "fontdrvhost.exe",
        "winlogon.exe",
        "dwm.exe",
        "explorer.exe",
        "spoolsv.exe",
        "taskhostw.exe",
        "sihost.exe",
        "runtimebroker.exe",

        // Linux
        "systemd",
        "init",
        "kthreadd",
        "sshd",
        "dbus-daemon",
        "networkmanager",
        "xorg",
        "wayland",
        "gnome-shell",
        "polkitd",
        "rsyslogd",
        "cron",
        "crond",

        // macOS
        "launchd",
        "kernel_task",
        "windowserver",
        "dock",
        "finder",
        "loginwindow",
        "coreaudiod",
        "systemstatusd",
        "controlcenter",
        "notificationcenter",
        "spotlight",
        "mds",
        "opendirectoryd"
    };

    public static bool IsSystemOrProtected(Process p)
    {
        try
        {
            // 1. PIDs système universels et PID de l'agent lui-même
            if (p.Id <= 4 || p.Id == Environment.ProcessId)
            {
                return true;
            }

            // 2. Nom dans la liste noire
            string name = p.ProcessName;
            if (ProtectedNames.Contains(name) || ProtectedNames.Contains(name + ".exe"))
            {
                return true;
            }

            // 3. Spécifique Linux : vérifier UID si accessible dans /proc/{id}/status
            if (RuntimeInformation.IsOSPlatform(OSPlatform.Linux))
            {
                int uid = GetLinuxProcessUid(p.Id);
                if (uid >= 0 && uid < 1000)
                {
                    return true;
                }
            }

            // 4. Spécifique macOS : filtrer les démons système et sous-processus helpers
            if (RuntimeInformation.IsOSPlatform(OSPlatform.OSX))
            {
                string? mainExe = GetProcessMainModuleFileName(p);
                if (IsMacSystemOrHelper(mainExe, name))
                {
                    return true;
                }
            }

            return false;
        }
        catch
        {
            // En cas d'erreur de lecture (accès refusé, processus système protégé), considérer protégé
            return true;
        }
    }

    private static int GetLinuxProcessUid(int pid)
    {
        try
        {
            string statusPath = $"/proc/{pid}/status";
            if (!File.Exists(statusPath)) return -1;

            foreach (var line in File.ReadLines(statusPath))
            {
                if (line.StartsWith("Uid:", StringComparison.OrdinalIgnoreCase))
                {
                    var parts = line.Split('\t', StringSplitOptions.RemoveEmptyEntries);
                    if (parts.Length > 1 && int.TryParse(parts[1], out int realUid))
                    {
                        return realUid;
                    }
                }
            }
        }
        catch
        {
            // Ignore
        }
        return -1;
    }

    private static string? GetProcessMainModuleFileName(Process p)
    {
        try
        {
            return p.MainModule?.FileName;
        }
        catch
        {
            return null;
        }
    }

    private static bool IsMacSystemOrHelper(string? exe, string processName)
    {
        if (string.IsNullOrEmpty(exe))
        {
            // Beaucoup de démons système macOS n'autorisent pas la lecture de MainModule
            return false;
        }

        // Composants internes du système Apple
        if (exe.StartsWith("/System/Library/") || exe.StartsWith("/Library/Apple/") || exe.StartsWith("/usr/"))
        {
            return true;
        }

        // Processus d'aide / helpers imbriqués (ex: Chrome Helper, Code Helper)
        if (processName.EndsWith("Helper", StringComparison.OrdinalIgnoreCase) ||
            processName.Contains("Helper (", StringComparison.OrdinalIgnoreCase))
        {
            return true;
        }

        return false;
    }

    private static string GetDisplayName(Process p)
    {
        string name = p.ProcessName;

        // macOS : extraire le nom lisible de l'application (.app) si disponible
        if (RuntimeInformation.IsOSPlatform(OSPlatform.OSX))
        {
            try
            {
                string? exe = p.MainModule?.FileName;
                if (!string.IsNullOrEmpty(exe))
                {
                    const string marker = ".app/Contents/MacOS/";
                    int idx = exe.IndexOf(marker, StringComparison.Ordinal);
                    if (idx > 0)
                    {
                        string bundlePath = exe.Substring(0, idx);
                        string appName = Path.GetFileName(bundlePath);
                        if (!string.IsNullOrEmpty(appName))
                        {
                            return appName;
                        }
                    }
                }
            }
            catch { }
        }

        // Windows : suffixer .exe
        if (RuntimeInformation.IsOSPlatform(OSPlatform.Windows) && !name.EndsWith(".exe", StringComparison.OrdinalIgnoreCase))
        {
            name += ".exe";
        }

        return name;
    }

    public static List<ProcessInfo> GetUserProcesses()
    {
        var result = new List<ProcessInfo>();
        var processes = Process.GetProcesses();

        foreach (var p in processes)
        {
            try
            {
                if (IsSystemOrProtected(p))
                {
                    continue;
                }

                string name = GetDisplayName(p);

                long startedAt;
                try
                {
                    startedAt = new DateTimeOffset(p.StartTime).ToUnixTimeMilliseconds();
                }
                catch
                {
                    startedAt = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
                }

                result.Add(new ProcessInfo(p.Id, name, startedAt));
            }
            catch
            {
                // Processus inaccessible ou terminé
            }
            finally
            {
                p.Dispose();
            }
        }

        return result;
    }

    public static KillResult KillUserProcess(int pid)
    {
        Process? process = null;
        try
        {
            process = Process.GetProcessById(pid);
        }
        catch (ArgumentException)
        {
            return new KillResult(false, pid, null, "Processus introuvable (déjà arrêté ou inexistant)");
        }
        catch (Exception ex)
        {
            return new KillResult(false, pid, null, $"Erreur d'accès au processus : {ex.Message}");
        }

        using (process)
        {
            string name = GetDisplayName(process);

            // Sécurité : refus formel d'arrêter un processus système ou protégé
            if (IsSystemOrProtected(process))
            {
                Console.WriteLine($"[SÉCURITÉ] Tentative d'arrêt bloquée pour PID={pid} ({name}) : processus protégé.");
                return new KillResult(false, pid, name, "Action refusée : ce processus est un composant système ou protégé");
            }

            try
            {
                // Tentative 1 : Demande de fermeture propre si applicable
                try
                {
                    if (process.MainWindowHandle != IntPtr.Zero)
                    {
                        process.CloseMainWindow();
                    }
                }
                catch { }

                // Attente de confirmation de sortie
                if (!WaitForExit(process, 1500))
                {
                    Console.WriteLine($"Arrêt gracieux inefficace pour PID={pid} ({name}), déclenchement de Kill()...");
                    // Tentative 2 : Arrêt forcé (tue également l'arbre des sous-processus sur tous les OS)
                    process.Kill(entireProcessTree: true);

                    if (!WaitForExit(process, 1000))
                    {
                        return new KillResult(false, pid, name, "Le processus n'a pas pu être arrêté.");
                    }
                }

                Console.WriteLine($"[SUCCÈS] Processus arrêté : [PID={pid}, Nom={name}]");
                return new KillResult(true, pid, name, null);
            }
            catch (Exception ex)
            {
                Console.WriteLine($"[ERREUR] Échec de l'arrêt pour PID={pid} ({name}) : {ex.Message}");
                return new KillResult(false, pid, name, $"Échec de l'arrêt forcé : {ex.Message}");
            }
        }
    }

    private static bool WaitForExit(Process p, int timeoutMs)
    {
        try
        {
            return p.WaitForExit(timeoutMs);
        }
        catch
        {
            return true;
        }
    }
}
