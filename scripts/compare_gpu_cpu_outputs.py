import sys
import os
import math
from PIL import Image

def compare_bitmaps(gpu_path, cpu_path):
    if not os.path.exists(gpu_path) or not os.path.exists(cpu_path):
        print(f"Error: Missing image file ({gpu_path}, {cpu_path})")
        return

    gpu_img = Image.open(gpu_path).convert("RGB")
    cpu_img = Image.open(cpu_path).convert("RGB")

    gw, gh = gpu_img.size
    cw, ch = cpu_img.size

    print(f"GPU IMAGE DIMENSIONS: {gw} x {gh}")
    print(f"CPU IMAGE DIMENSIONS: {cw} x {ch}")

    if gw != cw or gh != ch:
        print("Error: Image dimensions do not match!")
        return

    num_pixels = gw * gh

    get_gpu_data = getattr(gpu_img, "get_flattened_data", gpu_img.getdata)
    get_cpu_data = getattr(cpu_img, "get_flattened_data", cpu_img.getdata)

    gpu_pixels = list(get_gpu_data())
    cpu_pixels = list(get_cpu_data())

    sum_abs_diff_r = 0.0
    sum_abs_diff_g = 0.0
    sum_abs_diff_b = 0.0
    sum_euclidean_dist = 0.0

    max_diff_r = 0
    max_diff_g = 0
    max_diff_b = 0
    max_euclidean_dist = 0.0

    for i in range(num_pixels):
        gp = gpu_pixels[i]
        cp = cpu_pixels[i]

        dr = abs(gp[0] - cp[0])
        dg = abs(gp[1] - cp[1])
        db = abs(gp[2] - cp[2])

        euc = math.sqrt(dr*dr + dg*dg + db*db)

        sum_abs_diff_r += dr
        sum_abs_diff_g += dg
        sum_abs_diff_b += db
        sum_euclidean_dist += euc

        if dr > max_diff_r: max_diff_r = dr
        if dg > max_diff_g: max_diff_g = dg
        if db > max_diff_b: max_diff_b = db
        if euc > max_euclidean_dist: max_euclidean_dist = euc

    mean_diff_r = sum_abs_diff_r / float(num_pixels)
    mean_diff_g = sum_abs_diff_g / float(num_pixels)
    mean_diff_b = sum_abs_diff_b / float(num_pixels)
    mean_euclidean = sum_euclidean_dist / float(num_pixels)

    print("\n--- GPU vs CPU NUMERICAL PIXEL COMPARISON ---")
    print(f"Mean Abs RGB Diff:   dR={mean_diff_r:.2f}, dG={mean_diff_g:.2f}, dB={mean_diff_b:.2f}")
    print(f"Mean Euclidean Dist: {mean_euclidean:.2f} px")
    print(f"Max Channel Diff:    dR={max_diff_r}, dG={max_diff_g}, dB={max_diff_b}")
    print(f"Max Euclidean Dist:  {max_euclidean_dist:.2f} px")

    verdict = "PASS (MATCHES CPU)" if mean_euclidean < 10.0 else "WARNING (DISCREPANCY DETECTED)"
    print(f"VERDICT: {verdict}")

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: python compare_gpu_cpu_outputs.py <gpu_output.jpg> <cpu_output.jpg>")
        sys.exit(1)
    compare_bitmaps(sys.argv[1], sys.argv[2])
