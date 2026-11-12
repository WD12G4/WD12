using System.Text.Json.Serialization;

namespace MaterielAgent;

public record InboundCommand(
    [property: JsonPropertyName("action")] string? Action,
    [property: JsonPropertyName("requestId")] string? RequestId,
    [property: JsonPropertyName("pid")] int? Pid
);

public record ProcessInfo(
    [property: JsonPropertyName("pid")] int Pid,
    [property: JsonPropertyName("name")] string Name,
    [property: JsonPropertyName("startedAt")] long StartedAt
);

public record KillResult(
    [property: JsonPropertyName("success")] bool Success,
    [property: JsonPropertyName("pid")] int Pid,
    [property: JsonPropertyName("processName")] string? ProcessName,
    [property: JsonPropertyName("error")] string? Error
);

public class OutboundResponse
{
    [JsonPropertyName("type")]
    public string Type { get; set; } = string.Empty;

    [JsonPropertyName("requestId")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? RequestId { get; set; }

    [JsonPropertyName("serialNumber")]
    public string SerialNumber { get; set; } = string.Empty;

    [JsonPropertyName("hostname")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? Hostname { get; set; }

    [JsonPropertyName("macAddress")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? MacAddress { get; set; }

    [JsonPropertyName("os")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public string? Os { get; set; }

    [JsonPropertyName("processes")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public List<ProcessInfo>? Processes { get; set; }

    [JsonPropertyName("result")]
    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]
    public KillResult? Result { get; set; }
}
