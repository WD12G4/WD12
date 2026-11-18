using Xunit;

namespace MaterielAgent.Tests;

public class ProcessManagerTests
{
    [Fact]
    public void KillUserProcess_WhenInvalidPid_ReturnsFailureResult()
    {
        // PID 99999999 is invalid/non-existent
        var result = ProcessManager.KillUserProcess(99999999);

        Assert.False(result.Success);
        Assert.Equal(99999999, result.Pid);
        Assert.NotNull(result.Error);
    }

    [Fact]
    public void GetUserProcesses_ExecutesWithoutException()
    {
        var processes = ProcessManager.GetUserProcesses();

        Assert.NotNull(processes);
        // Can be empty or contain processes depending on OS and current user
    }
}
