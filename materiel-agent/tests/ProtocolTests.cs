using System.Text.Json;
using Xunit;

namespace MaterielAgent.Tests;

public class ProtocolTests
{
    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        PropertyNameCaseInsensitive = true
    };

    [Fact]
    public void InboundCommand_CanDeserializeFromJson()
    {
        string json = """
        {
            "action": "KILL_PROCESS",
            "requestId": "req-1234",
            "pid": 5678
        }
        """;

        var cmd = JsonSerializer.Deserialize<InboundCommand>(json, JsonOptions);

        Assert.NotNull(cmd);
        Assert.Equal("KILL_PROCESS", cmd.Action);
        Assert.Equal("req-1234", cmd.RequestId);
        Assert.Equal(5678, cmd.Pid);
    }

    [Fact]
    public void OutboundResponse_SerializesExpectedJsonStructure()
    {
        var resp = new OutboundResponse
        {
            Type = "REGISTER",
            SerialNumber = "UC-TEST-001",
            Hostname = "PC-LAB-01",
            MacAddress = "00:11:22:33:44:55",
            Username = "user1",
            Os = "windows"
        };

        string json = JsonSerializer.Serialize(resp);

        Assert.Contains("\"type\":\"REGISTER\"", json);
        Assert.Contains("\"serialNumber\":\"UC-TEST-001\"", json);
        Assert.Contains("\"hostname\":\"PC-LAB-01\"", json);
        Assert.Contains("\"macAddress\":\"00:11:22:33:44:55\"", json);
        Assert.Contains("\"username\":\"user1\"", json);
        Assert.Contains("\"os\":\"windows\"", json);
    }

    [Fact]
    public void ProcessInfo_OmitsNullIconWhenSerialized()
    {
        var info = new ProcessInfo(100, "notepad.exe", 1700000000);
        string json = JsonSerializer.Serialize(info);

        Assert.Contains("\"pid\":100", json);
        Assert.Contains("\"name\":\"notepad.exe\"", json);
        Assert.DoesNotContain("\"icon\"", json);
    }

    [Fact]
    public void KillResult_SerializesCorrectly()
    {
        var successResult = new KillResult(true, 1234, "notepad.exe", null);
        string successJson = JsonSerializer.Serialize(successResult);
        Assert.Contains("\"success\":true", successJson);
        Assert.Contains("\"pid\":1234", successJson);

        var failResult = new KillResult(false, 9999, null, "Process not found");
        string failJson = JsonSerializer.Serialize(failResult);
        Assert.Contains("\"success\":false", failJson);
        Assert.Contains("\"error\":\"Process not found\"", failJson);
    }
}
