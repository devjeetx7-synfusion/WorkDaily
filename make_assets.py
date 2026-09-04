import math
import os
from PIL import Image, ImageDraw, ImageFilter, ImageFont

RES_DIR = "android-offline-attendance-app/app/src/main/res/drawable-nodpi"
os.makedirs(RES_DIR, exist_ok=True)

def create_circle_mask(size):
    mask = Image.new("L", size, 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse((0, 0, size[0], size[1]), fill=255)
    return mask

def draw_peacock_feather(draw, center, scale, alpha=255):
    cx, cy = center
    # Eye outer teal/blue
    draw.ellipse((cx - 60*scale, cy - 80*scale, cx + 60*scale, cy + 40*scale), fill=(0, 150, 210, alpha))
    # Eye middle royal blue
    draw.ellipse((cx - 40*scale, cy - 65*scale, cx + 40*scale, cy + 25*scale), fill=(25, 45, 140, alpha))
    # Eye inner bronze/gold
    draw.ellipse((cx - 25*scale, cy - 50*scale, cx + 25*scale, cy + 10*scale), fill=(210, 160, 30, alpha))
    # Eye center deep blue/indigo
    draw.ellipse((cx - 15*scale, cy - 40*scale, cx + 15*scale, cx - 5*scale if False else cy - 5*scale), fill=(10, 20, 70, alpha))

def draw_flute_and_matki(img, scale=1.0):
    w, h = img.size
    draw = ImageDraw.Draw(img)
    # Matki (pot)
    cx, cy = int(w * 0.5), int(h * 0.65)
    draw.ellipse((cx - 140*scale, cy - 80*scale, cx + 140*scale, cy + 180*scale), fill=(180, 100, 30, 255))
    draw.ellipse((cx - 120*scale, cy - 100*scale, cx + 120*scale, cy - 40*scale), fill=(220, 140, 50, 255))
    # White butter/makkhan overflow
    draw.ellipse((cx - 90*scale, cy - 120*scale, cx + 90*scale, cy - 60*scale), fill=(250, 250, 245, 255))
    # Golden ornaments on matki
    draw.arc((cx - 130*scale, cy - 20*scale, cx + 130*scale, cy + 120*scale), start=0, end=180, fill=(255, 215, 0, 255), width=int(8*scale))

    # Golden Flute (Bansuri) angled across
    f_start = (int(cx - 220*scale), int(cy - 120*scale))
    f_end = (int(cx + 220*scale), int(cy + 60*scale))
    draw.line([f_start, f_end], fill=(240, 190, 40, 255), width=int(22*scale))
    # Flute holes & peacock feather
    draw_peacock_feather(draw, (int(cx + 180*scale), int(cy - 140*scale)), scale=scale*1.1)

print("Helper script ready")
