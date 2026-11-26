import subprocess
import time
import re
import sys

def get_battery_temp(adb_path, device_id):
    cmd = [adb_path, "-s", device_id, "shell", "dumpsys", "battery"]
    res = subprocess.run(cmd, capture_output=True, text=True)
    m = re.search(r"temperature:\s+(\d+)", res.stdout)
    if m:
        val = int(m.group(1))
        return val / 10.0
    return 0.0

def sample_meminfo_during_run(adb_path, device_id, photo_uri, force_cpu=False):
    start_temp = get_battery_temp(adb_path, device_id)
    start_time = time.time()
    mode_str = "CPU-FORCED" if force_cpu else "AUTO (GPU/CPU)"
    print(f"[{device_id}] Starting {mode_str} Pass... Initial Battery Temp: {start_temp:.1f} °C")

    if start_temp >= 42.0:
        print(f"ABORT CRITERION MET: Battery temperature ({start_temp:.1f} °C) >= 42.0 °C before starting pass.")
        return 0, 0, 0, 0.0, start_temp, start_temp

    # Clear logcat buffer prior to run
    subprocess.run([adb_path, "-s", device_id, "logcat", "-c"], capture_output=True, text=True)

    # 1. Start foreground service
    start_cmd = [
        adb_path, "-s", device_id, "shell", "am", "start-foreground-service",
        "-n", "com.lensora.ai/.service.AiProcessorService",
        "--es", "PHOTO_URI", photo_uri,
        "--ez", "IS_HIGH_RES", "true",
        "--ez", "FORCE_CPU", "true" if force_cpu else "false"
    ]
    subprocess.run(start_cmd, capture_output=True, text=True)

    time.sleep(2)

    samples = []
    max_pss = 0
    max_rss = 0
    count = 0

    while True:
        curr_temp = get_battery_temp(adb_path, device_id)
        if curr_temp >= 42.0:
            print(f"\n⚠️ ABORTING RUN ON REDMI: Battery temperature reached {curr_temp:.1f} °C (>= 42.0 °C safety limit)!")
            subprocess.run([adb_path, "-s", device_id, "shell", "am", "force-stop", "com.lensora.ai"])
            break

        mem_cmd = [adb_path, "-s", device_id, "shell", "dumpsys", "meminfo", "com.lensora.ai"]
        res = subprocess.run(mem_cmd, capture_output=True, text=True)
        out = res.stdout

        if "No process found" in out:
            print("Process terminated or service stopped.")
            break

        pss_match = re.search(r"TOTAL PSS:\s+(\d+)", out)
        rss_match = re.search(r"TOTAL RSS:\s+(\d+)", out)

        if pss_match and rss_match:
            pss = int(pss_match.group(1))
            rss = int(rss_match.group(1))
            count += 1
            if pss > max_pss:
                max_pss = pss
            if rss > max_rss:
                max_rss = rss
            samples.append((pss, rss))
            print(f"[Sample #{count} | Temp: {curr_temp:.1f}°C] PSS: {pss} kB ({pss/1024.0:.1f} MB) | RSS: {rss} kB ({rss/1024.0:.1f} MB)")

        log_cmd = [adb_path, "-s", device_id, "logcat", "-d", "-t", "50"]
        log_res = subprocess.run(log_cmd, capture_output=True, text=True)
        if "AiProcessorService destroyed" in log_res.stdout or "L3 Service Real Image-to-Image AI Processing finished" in log_res.stdout:
            print("AiProcessorService completion detected in logcat.")
            break

        time.sleep(2)

    elapsed_sec = time.time() - start_time
    end_temp = get_battery_temp(adb_path, device_id)

    print(f"\n--- {mode_str} SAMPLING SUMMARY ---")
    print(f"Device:             {device_id}")
    print(f"Execution Time:     {elapsed_sec:.1f} seconds ({elapsed_sec/60.0:.2f} minutes)")
    print(f"Total Samples Taken: {count}")
    print(f"Peak TOTAL PSS:     {max_pss} kB ({max_pss/1024.0:.1f} MB)")
    print(f"Peak TOTAL RSS:     {max_rss} kB ({max_rss/1024.0:.1f} MB)")
    print(f"Battery Temp Start: {start_temp:.1f} °C")
    print(f"Battery Temp End:   {end_temp:.1f} °C (Delta: {end_temp - start_temp:+.1f} °C)")

    return count, max_pss, max_rss, elapsed_sec, start_temp, end_temp

if __name__ == "__main__":
    adb = r"C:\Users\kanak\AppData\Local\Android\Sdk\platform-tools\adb.exe"
    dev_id = sys.argv[1] if len(sys.argv) > 1 else "adb-5ceab3e6-1ROl7m._adb-tls-connect._tcp"
    force_cpu_flag = sys.argv[2].lower() == "true" if len(sys.argv) > 2 else False
    uri = "file:///data/data/com.lensora.ai/files/photo_12_5mp.jpg"
    sample_meminfo_during_run(adb, dev_id, uri, force_cpu=force_cpu_flag)
