using Xunit;

namespace MaterielAgent.Tests;

public class ConfigTests
{
    [Fact]
    public void Load_WithLongCommandLineArgs_SetsConfigProperties()
    {
        string[] args =
        [
            "--server", "wss://myserver.example.com/ws/agent",
            "--serial", "UC-CUSTOM-999",
            "--secret", "super-secret-key",
            "--reconnect", "12",
            "--strict-ssl"
        ];

        var config = Config.Load(args);

        Assert.Equal("wss://myserver.example.com/ws/agent", config.ServerUrl);
        Assert.Equal("UC-CUSTOM-999", config.SerialNumber);
        Assert.Equal("super-secret-key", config.SharedSecret);
        Assert.Equal(12, config.ReconnectDelaySec);
        Assert.Equal(TimeSpan.FromSeconds(12), config.ReconnectDelay);
        Assert.False(config.AllowSelfSignedCert);
    }

    [Fact]
    public void Load_WithShortCommandLineArgs_SetsConfigProperties()
    {
        string[] args =
        [
            "-s", "wss://short-server:9000",
            "-k", "another-secret",
            "-r", "20",
            "--insecure"
        ];

        var config = Config.Load(args);

        Assert.Equal("wss://short-server:9000", config.ServerUrl);
        Assert.Equal("another-secret", config.SharedSecret);
        Assert.Equal(20, config.ReconnectDelaySec);
        Assert.True(config.AllowSelfSignedCert);
    }

    [Fact]
    public void Load_WhenEmptyArgs_ProvidesDefaultOrFallbackValues()
    {
        var config = Config.Load(Array.Empty<string>());

        Assert.False(string.IsNullOrWhiteSpace(config.ServerUrl));
        Assert.False(string.IsNullOrWhiteSpace(config.SerialNumber));
        Assert.True(config.ReconnectDelaySec > 0);
    }

    [Fact]
    public void Load_WithConfigFile_LoadsConfiguration()
    {
        string tempFile = Path.GetTempFileName();
        try
        {
            string json = """
            {
                "server_url": "wss://from-file.local:8443/ws",
                "serial_number": "UC-FILE-001",
                "shared_secret": "secret-from-file",
                "reconnect_delay_sec": 8,
                "allow_self_signed_cert": true
            }
            """;
            File.WriteAllText(tempFile, json);

            string[] args = ["--config", tempFile];
            var config = Config.Load(args);

            Assert.Equal("wss://from-file.local:8443/ws", config.ServerUrl);
            Assert.Equal("UC-FILE-001", config.SerialNumber);
            Assert.Equal("secret-from-file", config.SharedSecret);
            Assert.Equal(8, config.ReconnectDelaySec);
            Assert.True(config.AllowSelfSignedCert);
        }
        finally
        {
            if (File.Exists(tempFile))
            {
                File.Delete(tempFile);
            }
        }
    }

    [Fact]
    public void Load_WithInvalidConfigFile_LogsWarningAndKeepsDefaults()
    {
        string tempFile = Path.GetTempFileName();
        try
        {
            File.WriteAllText(tempFile, "invalid-json-content {{{");

            string[] args = ["-c", tempFile];
            var config = Config.Load(args);

            Assert.NotNull(config);
            Assert.False(string.IsNullOrWhiteSpace(config.ServerUrl));
        }
        finally
        {
            if (File.Exists(tempFile))
            {
                File.Delete(tempFile);
            }
        }
    }

    [Fact]
    public void Load_WithEnvironmentVariables_OverwritesDefaults()
    {
        Environment.SetEnvironmentVariable("WD12_SERVER_URL", "wss://env-server.local:9999/ws");
        Environment.SetEnvironmentVariable("WD12_SERIAL_NUMBER", "UC-ENV-1234");
        Environment.SetEnvironmentVariable("WD12_AGENT_SECRET", "env-secret-value");
        Environment.SetEnvironmentVariable("WD12_ALLOW_SELF_SIGNED_CERT", "false");

        try
        {
            var config = Config.Load(Array.Empty<string>());

            Assert.Equal("wss://env-server.local:9999/ws", config.ServerUrl);
            Assert.Equal("UC-ENV-1234", config.SerialNumber);
            Assert.Equal("env-secret-value", config.SharedSecret);
            Assert.False(config.AllowSelfSignedCert);
        }
        finally
        {
            Environment.SetEnvironmentVariable("WD12_SERVER_URL", null);
            Environment.SetEnvironmentVariable("WD12_SERIAL_NUMBER", null);
            Environment.SetEnvironmentVariable("WD12_AGENT_SECRET", null);
            Environment.SetEnvironmentVariable("WD12_ALLOW_SELF_SIGNED_CERT", null);
        }
    }

    [Fact]
    public void Load_WithAllowSelfSignedFlag_SetsTrue()
    {
        string[] args = ["--allow-self-signed"];
        var config = Config.Load(args);
        Assert.True(config.AllowSelfSignedCert);
    }

    [Fact]
    public void GetMacAddress_ReturnsNonEmptyFormattedString()
    {
        string mac = Config.GetMacAddress();

        Assert.False(string.IsNullOrWhiteSpace(mac));
        Assert.Contains(":", mac);
    }

    [Theory]
    [InlineData("docker0", "", true)]
    [InlineData("eth0", "Hyper-V Virtual Ethernet Adapter", true)]
    [InlineData("wsl-bridge", "", true)]
    [InlineData("tailscale0", "", true)]
    [InlineData("eth0", "Intel Ethernet Connection", false)]
    public void IsVirtualInterface_DetectsVirtualAdaptersCorrectly(string name, string desc, bool expected)
    {
        bool result = Config.IsVirtualInterface(name, desc);
        Assert.Equal(expected, result);
    }

    [Fact]
    public void GetActiveSessionUsername_ExecutesWithoutException()
    {
        string? user = Config.GetActiveSessionUsername();
        if (user != null)
        {
            Assert.False(string.IsNullOrWhiteSpace(user));
        }
    }

    [Fact]
    public void DetectWindowsConsoleUser_ExecutesSafely()
    {
        if (OperatingSystem.IsWindows())
        {
            string? user = Config.DetectWindowsConsoleUser();
            // user can be null if running non-interactively or a valid user name
            if (user != null)
            {
                Assert.False(string.IsNullOrWhiteSpace(user));
            }
        }
    }

    [Fact]
    public void DetectLinuxSessionUser_ExecutesSafely()
    {
        string? user = Config.DetectLinuxSessionUser();
        // May be null on Windows / headless
        if (user != null)
        {
            Assert.False(string.IsNullOrWhiteSpace(user));
        }
    }

    [Fact]
    public void DetectMacConsoleUser_ExecutesSafely()
    {
        string? user = Config.DetectMacConsoleUser();
        // May be null on Windows / headless
        if (user != null)
        {
            Assert.False(string.IsNullOrWhiteSpace(user));
        }
    }

    [Fact]
    public void NativeMethods_WTSGetActiveConsoleSessionId_RunsOnWindows()
    {
        if (OperatingSystem.IsWindows())
        {
            uint id = Config.NativeMethods.WTSGetActiveConsoleSessionId();
            // id is either 0xFFFFFFFF or valid session index
            Assert.True(id >= 0);
        }
    }
}
