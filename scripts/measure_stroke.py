import sys
import os
from PIL import Image

def measure_stroke_thickness(image_path, target_x=500):
    if not os.path.exists(image_path):
        print(f"Error: File not found {image_path}")
        return

    img = Image.open(image_path).convert("RGB")
    width, height = img.size

    # Find vertical stroke thickness around target_x +/- 200
    best_x = target_x
    max_count = 0
    best_first_y = -1
    best_last_y = -1

    for x in range(max(0, target_x - 100), min(width, target_x + 100)):
        first_y = -1
        last_y = -1
        count = 0
        for y in range(200, height - 400):
            r, g, b = img.getpixel((x, y))
            # Pink/Red mask condition: High Red (>= 150), dominant over Green/Blue
            if r >= 150 and r > g + 35 and r > b + 35:
                if first_y == -1:
                    first_y = y
                last_y = y
                count += 1
        if count > max_count:
            max_count = count
            best_x = x
            best_first_y = first_y
            best_last_y = last_y

    if max_count == 0:
        print(f"FILE: {os.path.basename(image_path)} | No mask stroke detected around x={target_x}")
    else:
        measured_thickness = best_last_y - best_first_y + 1
        print(f"FILE: {os.path.basename(image_path)} | Column: x={best_x} | First Y: {best_first_y} | Last Y: {best_last_y} | Measured Stroke Thickness: {measured_thickness} px")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python measure_stroke.py <path_to_png> [target_x]")
        sys.exit(1)

    path = sys.argv[1]
    x_val = int(sys.argv[2]) if len(sys.argv) > 2 else 500
    measure_stroke_thickness(path, x_val)
