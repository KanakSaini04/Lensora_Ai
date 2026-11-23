import sys
import os
import math
from PIL import Image

def analyze_seams_and_colors(input_path, output_path, tile_core_out=448):
    if not os.path.exists(input_path) or not os.path.exists(output_path):
        print(f"Error: Missing input or output file ({input_path}, {output_path})")
        return

    in_img = Image.open(input_path).convert("RGB")
    out_img = Image.open(output_path).convert("RGB")

    in_w, in_h = in_img.size
    out_w, out_h = out_img.size

    scale_x = out_w / float(in_w)
    scale_y = out_h / float(in_h)

    print(f"INPUT DIMENSIONS: {in_w} x {in_h}")
    print(f"OUTPUT DIMENSIONS: {out_w} x {out_h} (Net scale: {scale_x:.2f}x width, {scale_y:.2f}x height)")

    # 1. Color Shift Analysis: Compute average RGB channels
    in_pixels = list(in_img.getdata())
    out_pixels = list(out_img.getdata())

    avg_in_r = sum(p[0] for p in in_pixels) / float(len(in_pixels))
    avg_in_g = sum(p[1] for p in in_pixels) / float(len(in_pixels))
    avg_in_b = sum(p[2] for p in in_pixels) / float(len(in_pixels))

    avg_out_r = sum(p[0] for p in out_pixels) / float(len(out_pixels))
    avg_out_g = sum(p[1] for p in out_pixels) / float(len(out_pixels))
    avg_out_b = sum(p[2] for p in out_pixels) / float(len(out_pixels))

    shift_r = abs(avg_out_r - avg_in_r)
    shift_g = abs(avg_out_g - avg_in_g)
    shift_b = abs(avg_out_b - avg_in_b)
    color_dist = math.sqrt(shift_r**2 + shift_g**2 + shift_b**2)

    print(f"COLOR CHANNEL ANALYSIS:")
    print(f"  Input Avg RGB:  R={avg_in_r:.2f}, G={avg_in_g:.2f}, B={avg_in_b:.2f}")
    print(f"  Output Avg RGB: R={avg_out_r:.2f}, G={avg_out_g:.2f}, B={avg_out_b:.2f}")
    print(f"  Mean Shift:     dR={shift_r:.2f}, dG={shift_g:.2f}, dB={shift_b:.2f} | Euclidean RGB Distance = {color_dist:.2f}")

    # 2. Seam Discontinuity Analysis at Tile Core Boundaries (every tile_core_out px)
    max_h_discontinuity = 0.0
    max_v_discontinuity = 0.0

    # Vertical Seams (scanning x = col * tile_core_out)
    col = 1
    while col * tile_core_out < out_w - 1:
        x_seam = col * tile_core_out
        for y in range(0, out_h, 10):
            p_left = out_img.getpixel((x_seam - 1, y))
            p_right = out_img.getpixel((x_seam, y))
            diff = math.sqrt((p_left[0] - p_right[0])**2 + (p_left[1] - p_right[1])**2 + (p_left[2] - p_right[2])**2)
            if diff > max_h_discontinuity:
                max_h_discontinuity = diff
        col += 1

    # Horizontal Seams (scanning y = row * tile_core_out)
    row = 1
    while row * tile_core_out < out_h - 1:
        y_seam = row * tile_core_out
        for x in range(0, out_w, 10):
            p_top = out_img.getpixel((x, y_seam - 1))
            p_bottom = out_img.getpixel((x, y_seam))
            diff = math.sqrt((p_top[0] - p_bottom[0])**2 + (p_top[1] - p_bottom[1])**2 + (p_top[2] - p_bottom[2])**2)
            if diff > max_v_discontinuity:
                max_v_discontinuity = diff
        row += 1

    print(f"SEAM DISCONTINUITY ANALYSIS:")
    print(f"  Max Horizontal Seam Jump: {max_h_discontinuity:.2f} RGB Euclidean px")
    print(f"  Max Vertical Seam Jump:   {max_v_discontinuity:.2f} RGB Euclidean px")

    color_verdict = "PASS (NO SHIFT)" if color_dist < 15.0 else "WARNING (COLOR SHIFT)"
    seam_verdict = "PASS (NO SEAMS)" if max(max_h_discontinuity, max_v_discontinuity) < 45.0 else "WARNING (SEAM DETECTED)"

    print(f"VERDICT: Color Shift -> {color_verdict} | Tile Seams -> {seam_verdict}")

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: python check_seams_and_colors.py <input_png_or_jpg> <output_png_or_jpg>")
        sys.exit(1)

    in_file = sys.argv[1]
    out_file = sys.argv[2]
    analyze_seams_and_colors(in_file, out_file)
