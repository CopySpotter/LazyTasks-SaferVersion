// BusyTasks Safe 1.1 — non-privileged system monitoring for Linux Mint.
// New work (2026), supplied together with the original GPL-3.0-or-later application.
package BusyTasks;

import java.util.*;
import oshi.SystemInfo;
import oshi.hardware.*;
import oshi.software.os.*;

/** Queries OSHI in a background thread. Never executes a command or requests elevation. */
public final class MonitorService {
    private final SystemInfo systemInfo = new SystemInfo();
    private final HardwareAbstractionLayer hardware = systemInfo.getHardware();
    private final OperatingSystem os = systemInfo.getOperatingSystem();
    private final CentralProcessor cpu = hardware.getProcessor();
    private final GlobalMemory memory = hardware.getMemory();
    private final List<HWDiskStore> disks = hardware.getDiskStores();
    private final List<NetworkIF> networks = hardware.getNetworkIFs();
    private final int cores = Math.max(1, cpu.getLogicalProcessorCount());
    private Map<Integer,OSProcess> previousProcesses = Collections.emptyMap();
    private long[] previousCpuTicks;
    private long previousDiskRead = -1, previousDiskWrite = -1;
    private long previousNetRead = -1, previousNetWrite = -1;
    private long previousNano = 0;

    public static final class ProcessEntry {
        public final int pid;
        public final String name;
        public final double cpuPercent;
        public final long residentBytes;
        public final String state;
        public ProcessEntry(int pid, String name, double cpuPercent, long residentBytes, String state) {
            this.pid = pid;
            this.name = name;
            this.cpuPercent = cpuPercent;
            this.residentBytes = residentBytes;
            this.state = state;
        }
    }
    public static final class Snapshot {
        public final double cpuPercent, memoryPercent, diskReadRate, diskWriteRate, netReadRate, netWriteRate;
        public final long usedMemory, totalMemory, totalDiskRead, totalDiskWrite;
        public final int processCount;
        public final List<ProcessEntry> processes;
        Snapshot(double cpuPercent, double memoryPercent, long usedMemory, long totalMemory,
                 double diskReadRate, double diskWriteRate, double netReadRate, double netWriteRate,
                 long totalDiskRead, long totalDiskWrite, int processCount, List<ProcessEntry> processes) {
            this.cpuPercent=cpuPercent; this.memoryPercent=memoryPercent;
            this.usedMemory=usedMemory; this.totalMemory=totalMemory;
            this.diskReadRate=diskReadRate; this.diskWriteRate=diskWriteRate;
            this.netReadRate=netReadRate; this.netWriteRate=netWriteRate;
            this.totalDiskRead=totalDiskRead; this.totalDiskWrite=totalDiskWrite;
            this.processCount=processCount;
            this.processes=Collections.unmodifiableList(new ArrayList<>(processes));
        }
    }

    public static double percent(long used, long total) {
        return total <= 0 ? 0 : Math.max(0, Math.min(100, 100.0 * used / total));
    }
    public static double byteRate(long previous, long current, double elapsedSeconds) {
        if (previous < 0 || current < previous || elapsedSeconds <= 0) return 0;
        return Math.max(0.0, (current - previous) / elapsedSeconds);
    }
    public static double clampPercent(double number) {
        return Double.isFinite(number) ? Math.max(0, Math.min(100, number)) : 0;
    }
    public static String size(double bytes) {
        if (!Double.isFinite(bytes) || bytes < 0) return "–";
        String[] units={"B","KiB","MiB","GiB","TiB"};
        int i=0;
        while (bytes >= 1024 && i < units.length-1) { bytes/=1024; i++; }
        return String.format(Locale.GERMAN, (i==0 ? "%.0f %s" : "%.1f %s"), bytes, units[i]);
    }

    public synchronized Snapshot sample() {
        long now = System.nanoTime();
        double elapsed = previousNano == 0 ? 0 : (now-previousNano)/1_000_000_000.0;
        long[] newCpuTicks=cpu.getSystemCpuLoadTicks();
        double cpuPercent = previousCpuTicks == null ? 0 : clampPercent(cpu.getSystemCpuLoadBetweenTicks(previousCpuTicks)*100);
        previousCpuTicks = newCpuTicks;
        long totalMemory=memory.getTotal();
        long usedMemory=Math.max(0, totalMemory-memory.getAvailable());
        double memoryPercent=percent(usedMemory,totalMemory);

        long diskRead=0, diskWrite=0;
        for (HWDiskStore disk : disks) {
            try {
                if (disk.updateAttributes()) {
                    diskRead+=Math.max(0, disk.getReadBytes());
                    diskWrite+=Math.max(0, disk.getWriteBytes());
                }
            } catch (RuntimeException ignored) { /* some devices lack stats */ }
        }
        long netRead=0, netWrite=0;
        for (NetworkIF network: networks) {
            try {
                if (network.updateAttributes() && !"lo".equals(network.getName())) {
                    netRead+=Math.max(0, network.getBytesRecv());
                    netWrite+=Math.max(0, network.getBytesSent());
                }
            } catch (RuntimeException ignored) { /* intermittent interface */ }
        }
        double diskR=byteRate(previousDiskRead,diskRead,elapsed);
        double diskW=byteRate(previousDiskWrite,diskWrite,elapsed);
        double netR=byteRate(previousNetRead,netRead,elapsed);
        double netW=byteRate(previousNetWrite,netWrite,elapsed);
        previousDiskRead=diskRead; previousDiskWrite=diskWrite;
        previousNetRead=netRead; previousNetWrite=netWrite;
        previousNano=now;

        List<OSProcess> osProcesses=os.getProcesses();
        Map<Integer,OSProcess> current = new HashMap<>();
        List<ProcessEntry> entries=new ArrayList<>(osProcesses.size());
        for (OSProcess process: osProcesses) {
            int pid=process.getProcessID();
            current.put(pid,process);
            double procCpu;
            OSProcess old=previousProcesses.get(pid);
            try {
                // OSHI reports fraction of one core; normalize to the whole system.
                procCpu=old==null ? process.getProcessCpuLoadCumulative()*100.0/cores :
                        process.getProcessCpuLoadBetweenTicks(old)*100.0/cores;
            } catch (RuntimeException ex) { procCpu=0; }
            entries.add(new ProcessEntry(pid, process.getName(), clampPercent(procCpu),
                    Math.max(0,process.getResidentSetSize()), String.valueOf(process.getState())));
        }
        previousProcesses=current;
        entries.sort(Comparator.comparingDouble((ProcessEntry p)->p.cpuPercent).reversed()
                   .thenComparing(Comparator.comparingLong((ProcessEntry p)->p.residentBytes).reversed()));
        return new Snapshot(cpuPercent, memoryPercent, usedMemory, totalMemory,
                diskR,diskW,netR,netW,diskRead,diskWrite,entries.size(),entries);
    }
}
