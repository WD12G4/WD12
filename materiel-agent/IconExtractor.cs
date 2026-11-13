using System.Collections.Concurrent;
using System.Diagnostics;
using System.Runtime.Versioning;

namespace MaterielAgent;

public static class IconExtractor
{
    private static readonly ConcurrentDictionary<string, string?> IconCache = new(StringComparer.OrdinalIgnoreCase);

    /// <summary>
    /// Récupère l'icône de l'application au format Data URI (PNG Base64) avec mise en cache mémoire.
    /// Renvoie null si le système ne supporte pas l'extraction ou si l'icône n'a pas pu être extraite.
    /// </summary>
    public static string? GetIcon(string processName, Process process)
    {
        if (IconCache.TryGetValue(processName, out var cached))
        {
            return cached;
        }

        string? iconBase64 = null;
        try
        {
            if (OperatingSystem.IsWindows())
            {
                iconBase64 = ExtractWindowsIcon(process);
            }
        }
        catch
        {
            iconBase64 = null;
        }

        IconCache[processName] = iconBase64;
        return iconBase64;
    }

    [SupportedOSPlatform("windows")]
    private static string? ExtractWindowsIcon(Process process)
    {
        try
        {
            string? exePath = null;
            try
            {
                exePath = process.MainModule?.FileName;
            }
            catch
            {
                // Processus protégé, 32/64 bit mismatch ou droits insuffisants
            }

            if (string.IsNullOrEmpty(exePath) || !File.Exists(exePath))
            {
                return null;
            }

            using var icon = System.Drawing.Icon.ExtractAssociatedIcon(exePath);
            if (icon == null)
            {
                return null;
            }

            using var bitmap = icon.ToBitmap();
            using var ms = new MemoryStream();
            bitmap.Save(ms, System.Drawing.Imaging.ImageFormat.Png);
            return "data:image/png;base64," + Convert.ToBase64String(ms.ToArray());
        }
        catch
        {
            return null;
        }
    }
}
