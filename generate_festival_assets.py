import os
import math
from PIL import Image, ImageDraw, ImageFilter

RES_DIR = "android-offline-attendance-app/app/src/main/res/drawable-nodpi"
os.makedirs(RES_DIR, exist_ok=True)

def create_circle_mask(size):
    mask = Image.new("L", size, 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse((0, 0, size[0], size[1]), fill=255)
    return mask

def hex_to_rgba(hex_val, alpha=255):
    if isinstance(hex_val, str):
        hex_val = int(hex_val.replace("#", "").replace("0x", ""), 16)
    r = (hex_val >> 16) & 0xFF
    g = (hex_val >> 8) & 0xFF
    b = hex_val & 0xFF
    return (r, g, b, alpha)

# Master definitions of all 21 festival base keys and visual themes
FESTIVAL_THEMES = {
    "janmashtami": {
        "top": 0x0B1026, "bottom": 0x1E2B58, "primary": 0x1976D2, "accent": 0xFFD54F,
        "type": "krishna"
    },
    "diwali": {
        "top": 0x1A0B2E, "bottom": 0x4C1D95, "primary": 0xFFFF8F00, "accent": 0xFFFFD54F,
        "type": "diya"
    },
    "holi": {
        "top": 0x1E0B2B, "bottom": 0x5B21B6, "primary": 0xFFE91E63, "accent": 0xFFFFEB3B,
        "type": "colors"
    },
    "chhath_puja": {
        "top": 0x2A0800, "bottom": 0x7C2D12, "primary": 0xFFFF6F00, "accent": 0xFF29B6F6,
        "type": "sun_river"
    },
    "ganesh_chaturthi": {
        "top": 0x2B0A00, "bottom": 0x7C2D12, "primary": 0xFFFF6F00, "accent": 0xFFFFD54F,
        "type": "ganesha"
    },
    "teachers_day": {
        "top": 0x0F172A, "bottom": 0x1E1B4B, "primary": 0xFF1E88E5, "accent": 0xFFFFB300,
        "type": "education"
    },
    "eid": {
        "top": 0x022C22, "bottom": 0x065F46, "primary": 0xFF00897B, "accent": 0xFFFFD54F,
        "type": "crescent"
    },
    "raksha_bandhan": {
        "top": 0x220A1A, "bottom": 0x701A75, "primary": 0xFFE91E63, "accent": 0xFFFFD54F,
        "type": "rakhi"
    },
    "independence_day": {
        "top": 0x081C15, "bottom": 0x1B2A4A, "primary": 0xFFFF671F, "accent": 0xFFFFFFFF,
        "type": "tricolor"
    },
    "republic_day": {
        "top": 0x081C15, "bottom": 0x1B2A4A, "primary": 0xFFFF671F, "accent": 0xFF06038D,
        "type": "chakra"
    },
    "dussehra": {
        "top": 0x280D00, "bottom": 0x9A3412, "primary": 0xFFFF8F00, "accent": 0xFFFFD54F,
        "type": "bow_arrow"
    },
    "ram_navami": {
        "top": 0x280D00, "bottom": 0x9A3412, "primary": 0xFFFF9800, "accent": 0xFFFFD54F,
        "type": "ram_bow"
    },
    "maha_shivratri": {
        "top": 0x09090B, "bottom": 0x1E1B4B, "primary": 0xFF3F51B5, "accent": 0xFFE040FB,
        "type": "trishul"
    },
    "christmas": {
        "top": 0x1A0505, "bottom": 0x450A0A, "primary": 0xFFD32F2F, "accent": 0xFFFFD700,
        "type": "tree"
    },
    "makar_sankranti": {
        "top": 0x0C1A2B, "bottom": 0x1E3A8A, "primary": 0xFFFF9800, "accent": 0xFFFFEB3B,
        "type": "kite"
    },
    "gandhi_jayanti": {
        "top": 0x1C1917, "bottom": 0x3D2C24, "primary": 0xFF8D6E63, "accent": 0xFFFFF8E1,
        "type": "glasses"
    },
    "childrens_day": {
        "top": 0x180E29, "bottom": 0x4A1245, "primary": 0xFFEC407A, "accent": 0xFFFFCA28,
        "type": "balloons"
    },
    "onam": {
        "top": 0x052E16, "bottom": 0x14532D, "primary": 0xFF43A047, "accent": 0xFFFFD54F,
        "type": "pookalam"
    },
    "guru_nanak_jayanti": {
        "top": 0x1C1300, "bottom": 0x422C00, "primary": 0xFFFFA000, "accent": 0xFFFFF8E1,
        "type": "ek_onkar"
    },
    "mahavir_jayanti": {
        "top": 0x1C1005, "bottom": 0x422006, "primary": 0xFFFB8C00, "accent": 0xFFFFF8E1,
        "type": "wheel"
    },
    "buddha_purnima": {
        "top": 0x180E29, "bottom": 0x4A1245, "primary": 0xFFAB47BC, "accent": 0xFFE1BEE7,
        "type": "lotus"
    }
}

# Drawing detailed artwork motifs for each specific festival
def draw_motif(draw, cx, cy, motif_type, scale, alpha=255, fg_color=None):
    if motif_type == "krishna":
        # Peacock feather eye
        draw.ellipse((cx - 50*scale, cy - 70*scale, cx + 50*scale, cy + 30*scale), fill=(0, 160, 220, alpha))
        draw.ellipse((cx - 32*scale, cy - 55*scale, cx + 32*scale, cy + 18*scale), fill=(20, 40, 130, alpha))
        draw.ellipse((cx - 18*scale, cy - 42*scale, cx + 18*scale, cy + 5*scale), fill=(220, 170, 30, alpha))
        # Golden bansuri
        draw.line([(cx - 120*scale, cy + 40*scale), (cx + 120*scale, cy - 20*scale)], fill=(245, 195, 35, alpha), width=int(14*scale))

    elif motif_type == "diya":
        # Glowing Diya base & flame
        draw.ellipse((cx - 60*scale, cy, cx + 60*scale, cy + 50*scale), fill=(220, 120, 20, alpha))
        draw.polygon([(cx - 70*scale, cy + 10*scale), (cx + 70*scale, cy + 10*scale), (cx, cy + 60*scale)], fill=(240, 150, 30, alpha))
        # Flame
        draw.ellipse((cx - 20*scale, cy - 65*scale, cx + 20*scale, cy + 5*scale), fill=(255, 200, 40, alpha))
        draw.ellipse((cx - 10*scale, cy - 50*scale, cx + 10*scale, cy - 5*scale), fill=(255, 240, 180, alpha))

    elif motif_type == "ganesha":
        # Sacred Ganesha crown & ears
        draw.polygon([(cx, cy - 75*scale), (cx - 45*scale, cy - 15*scale), (cx + 45*scale, cy - 15*scale)], fill=(240, 160, 20, alpha))
        draw.ellipse((cx - 60*scale, cy - 20*scale, cx - 15*scale, cy + 35*scale), fill=(220, 100, 30, alpha))
        draw.ellipse((cx + 15*scale, cy - 20*scale, cx + 60*scale, cy + 35*scale), fill=(220, 100, 30, alpha))
        # Trunk
        draw.arc((cx - 25*scale, cy - 10*scale, cx + 25*scale, cy + 65*scale), start=0, end=180, fill=(245, 180, 40, alpha), width=int(16*scale))

    elif motif_type == "crescent":
        # Crescent Moon & Star
        draw.ellipse((cx - 50*scale, cy - 50*scale, cx + 50*scale, cy + 50*scale), fill=(255, 215, 50, alpha))
        draw.ellipse((cx - 30*scale, cy - 55*scale, cx + 55*scale, cy + 45*scale), fill=(10, 45, 35, alpha if alpha<255 else 0))
        draw.polygon([(cx + 30*scale, cy - 30*scale), (cx + 38*scale, cy - 15*scale), (cx + 55*scale, cy - 15*scale), (cx + 42*scale, cy - 5*scale), (cx + 47*scale, cy + 12*scale), (cx + 30*scale, cy + 2*scale)], fill=(255, 225, 90, alpha))

    elif motif_type == "sun_river":
        # Sun rising over holy river
        draw.ellipse((cx - 55*scale, cy - 45*scale, cx + 55*scale, cy + 65*scale), fill=(255, 140, 20, alpha))
        draw.rectangle((cx - 100*scale, cy + 15*scale, cx + 100*scale, cy + 80*scale), fill=(30, 140, 210, alpha))

    elif motif_type == "colors":
        # Gulal color splashes
        draw.ellipse((cx - 65*scale, cy - 45*scale, cx + 15*scale, cy + 35*scale), fill=(235, 30, 110, alpha))
        draw.ellipse((cx - 15*scale, cy - 25*scale, cx + 65*scale, cy + 55*scale), fill=(0, 185, 210, alpha))
        draw.ellipse((cx - 35*scale, cy - 65*scale, cx + 35*scale, cy + 5*scale), fill=(255, 215, 30, alpha))

    elif motif_type == "education":
        # Books and Graduation Cap
        draw.rectangle((cx - 60*scale, cy + 10*scale, cx + 60*scale, cy + 35*scale), fill=(220, 50, 40, alpha))
        draw.rectangle((cx - 55*scale, cy - 15*scale, cx + 55*scale, cy + 10*scale), fill=(30, 130, 220, alpha))
        # Cap diamond
        draw.polygon([(cx, cy - 65*scale), (cx - 70*scale, cy - 35*scale), (cx, cy - 5*scale), (cx + 70*scale, cy - 35*scale)], fill=(20, 25, 45, alpha))

    elif motif_type == "rakhi":
        # Decorative Rakhi
        draw.line([(cx - 110*scale, cy), (cx + 110*scale, cy)], fill=(220, 30, 80, alpha), width=int(10*scale))
        draw.ellipse((cx - 50*scale, cy - 50*scale, cx + 50*scale, cy + 50*scale), fill=(255, 200, 30, alpha))
        draw.ellipse((cx - 32*scale, cy - 32*scale, cx + 32*scale, cy + 32*scale), fill=(230, 30, 90, alpha))
        draw.ellipse((cx - 15*scale, cy - 15*scale, cx + 15*scale, cy + 15*scale), fill=(255, 255, 255, alpha))

    elif motif_type == "tricolor":
        # Indian Tricolor Flag ribbons
        draw.polygon([(cx - 80*scale, cy - 50*scale), (cx + 80*scale, cy - 50*scale), (cx + 60*scale, cy - 15*scale), (cx - 60*scale, cy - 15*scale)], fill=(255, 103, 31, alpha))
        draw.polygon([(cx - 60*scale, cy - 15*scale), (cx + 60*scale, cy - 15*scale), (cx + 40*scale, cy + 20*scale), (cx - 40*scale, cy + 20*scale)], fill=(255, 255, 255, alpha))
        draw.polygon([(cx - 40*scale, cy + 20*scale), (cx + 40*scale, cy + 20*scale), (cx + 20*scale, cy + 55*scale), (cx - 20*scale, cy + 55*scale)], fill=(4, 106, 56, alpha))

    elif motif_type == "chakra":
        # Ashoka Chakra & Republic Monument
        draw.ellipse((cx - 50*scale, cy - 50*scale, cx + 50*scale, cy + 50*scale), fill=(6, 3, 141, alpha))
        draw.ellipse((cx - 38*scale, cy - 38*scale, cx + 38*scale, cy + 38*scale), fill=(255, 255, 255, alpha))
        draw.ellipse((cx - 12*scale, cy - 12*scale, cx + 12*scale, cy + 12*scale), fill=(6, 3, 141, alpha))

    elif motif_type == "bow_arrow":
        # Golden Bow & Burning Arrow
        draw.arc((cx - 70*scale, cy - 70*scale, cx + 70*scale, cy + 70*scale), start=270, end=90, fill=(245, 180, 30, alpha), width=int(12*scale))
        draw.line([(cx - 10*scale, cy - 70*scale), (cx - 10*scale, cy + 70*scale)], fill=(220, 220, 220, alpha), width=int(4*scale))
        draw.line([(cx - 50*scale, cy), (cx + 60*scale, cy)], fill=(220, 50, 30, alpha), width=int(8*scale))

    elif motif_type == "ram_bow":
        # Kodanda Bow & Divine Lotus
        draw.arc((cx - 80*scale, cy - 60*scale, cx + 80*scale, cy + 60*scale), start=210, end=330, fill=(255, 160, 20, alpha), width=int(14*scale))
        draw.ellipse((cx - 25*scale, cy - 15*scale, cx + 25*scale, cy + 35*scale), fill=(255, 100, 100, alpha))

    elif motif_type == "trishul":
        # Lord Shiva's Trishul & Damru
        draw.line([(cx, cy - 80*scale), (cx, cy + 80*scale)], fill=(210, 210, 230, alpha), width=int(10*scale))
        draw.line([(cx - 35*scale, cy - 50*scale), (cx - 35*scale, cy - 20*scale)], fill=(210, 210, 230, alpha), width=int(8*scale))
        draw.line([(cx + 35*scale, cy - 50*scale), (cx + 35*scale, cy - 20*scale)], fill=(210, 210, 230, alpha), width=int(8*scale))
        draw.polygon([(cx - 40*scale, cy + 10*scale), (cx + 40*scale, cy + 10*scale), (cx, cy + 35*scale)], fill=(200, 130, 40, alpha))

    elif motif_type == "tree":
        # Christmas Tree & Star
        draw.polygon([(cx, cy - 75*scale), (cx - 55*scale, cy - 10*scale), (cx + 55*scale, cy - 10*scale)], fill=(34, 139, 34, alpha))
        draw.polygon([(cx, cy - 40*scale), (cx - 70*scale, cy + 35*scale), (cx + 70*scale, cy + 35*scale)], fill=(0, 100, 0, alpha))
        draw.rectangle((cx - 15*scale, cy + 35*scale, cx + 15*scale, cy + 65*scale), fill=(139, 69, 19, alpha))
        # Star
        draw.polygon([(cx, cy - 85*scale), (cx - 12*scale, cy - 70*scale), (cx + 12*scale, cy - 70*scale)], fill=(255, 215, 0, alpha))

    elif motif_type == "kite":
        # Makar Sankranti Kite
        draw.polygon([(cx, cy - 70*scale), (cx + 55*scale, cy), (cx, cy + 70*scale), (cx - 55*scale, cy)], fill=(255, 140, 0, alpha))
        draw.polygon([(cx - 55*scale, cy), (cx, cy), (cx, cy + 70*scale)], fill=(3, 169, 244, alpha))

    elif motif_type == "glasses":
        # Khadi Spectacles & Charkha Wheel
        draw.ellipse((cx - 60*scale, cy - 25*scale, cx - 10*scale, cy + 25*scale), fill=(0, 0, 0, 0), outline=(220, 220, 220, alpha), width=int(8*scale))
        draw.ellipse((cx + 10*scale, cy - 25*scale, cx + 60*scale, cy + 25*scale), fill=(0, 0, 0, 0), outline=(220, 220, 220, alpha), width=int(8*scale))
        draw.line([(cx - 10*scale, cy), (cx + 10*scale, cy)], fill=(220, 220, 220, alpha), width=int(6*scale))

    elif motif_type == "balloons":
        # Joyful Festive Balloons
        draw.ellipse((cx - 45*scale, cy - 60*scale, cx + 5*scale, cy), fill=(236, 64, 121, alpha))
        draw.ellipse((cx - 5*scale, cy - 45*scale, cx + 45*scale, cy + 15*scale), fill=(41, 182, 246, alpha))
        draw.ellipse((cx - 25*scale, cy - 25*scale, cx + 25*scale, cy + 35*scale), fill=(255, 202, 40, alpha))

    elif motif_type == "pookalam":
        # Floral Petals Circular Pattern
        draw.ellipse((cx - 65*scale, cy - 65*scale, cx + 65*scale, cy + 65*scale), fill=(255, 215, 0, alpha))
        draw.ellipse((cx - 45*scale, cy - 45*scale, cx + 45*scale, cy + 45*scale), fill=(230, 40, 80, alpha))
        draw.ellipse((cx - 25*scale, cy - 25*scale, cx + 25*scale, cy + 25*scale), fill=(67, 160, 71, alpha))

    elif motif_type == "ek_onkar":
        # Spiritual Golden Sun & Khanda Symbol
        draw.ellipse((cx - 60*scale, cy - 60*scale, cx + 60*scale, cy + 60*scale), fill=(255, 160, 0, alpha))
        draw.ellipse((cx - 40*scale, cy - 40*scale, cx + 40*scale, cy + 40*scale), fill=(255, 248, 225, alpha))

    elif motif_type == "wheel":
        # Jain Dharma Chakra
        draw.ellipse((cx - 55*scale, cy - 55*scale, cx + 55*scale, cy + 55*scale), fill=(251, 140, 0, alpha))
        draw.ellipse((cx - 35*scale, cy - 35*scale, cx + 35*scale, cy + 35*scale), fill=(255, 255, 255, alpha))

    elif motif_type == "lotus":
        # Divine Blooming Lotus
        draw.ellipse((cx - 50*scale, cy - 10*scale, cx + 50*scale, cy + 45*scale), fill=(171, 71, 188, alpha))
        draw.ellipse((cx - 30*scale, cy - 35*scale, cx + 30*scale, cy + 30*scale), fill=(244, 143, 177, alpha))
        draw.ellipse((cx - 15*scale, cy - 50*scale, cx + 15*scale, cy + 20*scale), fill=(255, 255, 255, alpha))

# 1. GENERATE BACKGROUND ARTWORK (1080 x 600, Transparent/Soft fade, Large scale)
def generate_background_art(base_key, theme):
    w, h = 1080, 600
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    cx, cy = int(w * 0.72), int(h * 0.45)
    draw_motif(draw, cx, cy, theme["type"], scale=3.8, alpha=75, fg_color=(255, 255, 255, 80))
    draw_motif(draw, int(w * 0.20), int(h * 0.75), theme["type"], scale=2.2, alpha=45, fg_color=(255, 255, 255, 50))

    out_path = os.path.join(RES_DIR, f"festival_{base_key}_background.png")
    img.save(out_path, "PNG")

# 2. GENERATE HERO ARTWORK (800 x 600, Transparent PNG, Prominent right visual)
def generate_hero_art(base_key, theme):
    w, h = 800, 600
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    cx, cy = int(w * 0.65), int(h * 0.48)
    draw_motif(draw, cx, cy, theme["type"], scale=2.8, alpha=255)

    out_path = os.path.join(RES_DIR, f"festival_{base_key}_hero.png")
    img.save(out_path, "PNG")

# 3. GENERATE CALENDAR ARTWORK (256 x 256 Perfect Circular Crop Image)
def generate_calendar_art(base_key, theme):
    w, h = 256, 256
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    p_rgba = hex_to_rgba(theme["primary"], 255)
    draw.ellipse((0, 0, w, h), fill=p_rgba)

    cx, cy = w // 2, h // 2
    draw_motif(draw, cx, cy, theme["type"], scale=1.1, alpha=255)

    mask = create_circle_mask((w, h))
    img.putalpha(mask)

    out_path = os.path.join(RES_DIR, f"festival_{base_key}_calendar.png")
    img.save(out_path, "PNG")

# Process all 21 festivals
for base_key, theme in FESTIVAL_THEMES.items():
    generate_background_art(base_key, theme)
    generate_hero_art(base_key, theme)
    generate_calendar_art(base_key, theme)

print(f"Successfully regenerated all 63 distinct festival assets across 21 supported festivals into {RES_DIR}")
