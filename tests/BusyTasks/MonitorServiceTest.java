package BusyTasks;

public final class MonitorServiceTest {
    private static void eq(double actual, double expected, String message) {
        if (Math.abs(actual-expected)>0.001) throw new AssertionError(message + " expected " + expected + " got " + actual);
    }
    public static void main(String[] args) {
        eq(MonitorService.percent(25,100),25,"percent");
        eq(MonitorService.percent(15,0),0,"zero memory");
        eq(MonitorService.percent(300,100),100,"overflow");
        eq(MonitorService.byteRate(-1,500,1),0,"first sample");
        eq(MonitorService.byteRate(400,100,1),0,"counter reset");
        eq(MonitorService.byteRate(100,500,2),200,"bytes/second");
        eq(MonitorService.clampPercent(Double.NaN),0,"nan");
        if (!MonitorService.size(2048).contains("KiB")) throw new AssertionError("units");
        System.out.println("PASS MonitorServiceTest");
    }
}
