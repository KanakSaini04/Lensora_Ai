import subprocess
import time
import re
import os

def sample_meminfo_during_run(adb_path, photo_uri):
    # 1. Start foreground service
    start_cmd = [
        adb_path, "-s", "emulator-5554", "shell", "am", "start-foreground-service",
        "-n", "com.lensora.ai/.service.AiProcessorService",
        "--es", "PHOTO_URI", photo_uri,
        "--ez", "IS_HIGH_RES", "true"
    ]
    subprocess.run(start_cmd, capture_output=True, text=True)
    print(f"Triggered AiProcessorService for {photo_uri}")

    time.sleep(2)

    samples = []
    max_pss = 0
    max_rss = 0
    count = 0

    while True:
        # Check meminfo
        mem_cmd = [adb_path, "-s", "emulator-5554", "shell", "dumpsys", "meminfo", "com.lensora.ai"]
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
            print(f"[Sample #{count}] PSS: {pss} kB ({pss/1024.0:.1f} MB) | RSS: {rss} kB ({rss/1024.0:.1f} MB)")

        # Check if service finished in logcat
        log_cmd = [adb_path, "-s", "emulator-5554", "logcat", "-d", "-s", "AiProcessorService:V"]
        log_res = subprocess.run(log_cmd, capture_output=True, text=True)
        if "AiProcessorService destroyed" in log_res.stdout or "Processing finished in" in log_res.stdout:
            print("AiProcessorService completion detected in logcat.")
            break

        time.sleep(2)

    print("\n--- MEMORY SAMPLING SUMMARY ---")
    print(f"Total Samples Taken: {count}")
    print(f"Peak TOTAL PSS:     {max_pss} kB ({max_pss/1024.0:.1f} MB)")
    print(f"Peak TOTAL RSS:     {max_rss} kB ({max_rss/1024.0:.1f} MB)")

if __name__ == "__main__":
    adb = r"C:\Users\kanak\AppData\Local\Android\Sdk\platform-tools\adb.exe"
    uri = "file:///data/data/com.lensora.ai/files/photo_12_5mp.jpg"
    sample_meminfo_during_run(adb, uri)
