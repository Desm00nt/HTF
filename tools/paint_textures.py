#!/usr/bin/env python3
"""
Generates the How To Fish entity textures pixel-perfectly matching the Java
model UV layouts (OldManModel 64x64, CrabModel 64x128, FishModel 64x64).
Run from the repo root:  python3 tools/paint_textures.py
Textures written into src/main/resources/assets/howtofish/textures/entity/.
"""
import math, random, struct, zlib, os

def faces(u, v, w, h, d):
    """Texture rects used by MC ModelBox for box (w,h,d) at uv (u,v)."""
    return {
        'top':    (u + d, v, w, d),
        'bottom': (u + d + w, v, w, d),
        'right':  (u, v + d, d, h),
        'front':  (u + d, v + d, w, h),
        'left':   (u + d + w, v + d, d, h),
        'back':   (u + 2*d + w, v + d, d, h),
    }

class Canvas:
    def __init__(self, w, h):
        self.w, self.h = w, h
        self.px = [[None] * w for _ in range(h)]

    def set(self, x, y, c):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.px[y][x] = c

    def get(self, x, y):
        if 0 <= x < self.w and 0 <= y < self.h:
            return self.px[y][x]
        return None

    def rect(self, x, y, w, h, c):
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                self.set(xx, yy, c)

    def fill_box(self, uv, size, c):
        u, v = uv
        w, h, d = size
        for name, (x, y, fw, fh) in faces(u, v, w, h, d).items():
            self.rect(x, y, fw, fh, c)

    def speckle(self, x, y, w, h, cols, p, rnd):
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                if rnd.random() < p:
                    self.set(xx, yy, rnd.choice(cols))

    def shade_box(self, uv, size, base, top_c, bottom_c, rnd, speck=None):
        u, v = uv; w, h, d = size
        f = faces(u, v, w, h, d)
        for name, (x, y, fw, fh) in f.items():
            c = top_c if name == 'top' else bottom_c if name == 'bottom' else base
            self.rect(x, y, fw, fh, c)
            if speck:
                self.speckle(x, y, fw, fh, speck, 0.25, rnd)

    def save_png(self, path, bg=(0, 0, 0, 0)):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        raw = bytearray()
        for y in range(self.h):
            raw.append(0)
            for x in range(self.w):
                c = self.px[y][x] or bg
                a = 255 if len(c) == 3 else c[3]
                r, g, b = c[0], c[1], c[2]
                raw += bytes((r, g, b, a))
        def chunk(t, data):
            return struct.pack('>I', len(data)) + t + data + struct.pack('>I', zlib.crc32(t + data) & 0xffffffff)
        png = b'\x89PNG\r\n\x1a\n' \
            + chunk(b'IHDR', struct.pack('>IIBBBBB', self.w, self.h, 8, 6, 0, 0, 0)) \
            + chunk(b'IDAT', zlib.compress(bytes(raw), 9)) \
            + chunk(b'IEND', b'')
        open(path, 'wb').write(png)
        print('wrote', path)

def mix(c1, c2, t):
    return tuple(int(round(a + (b - a) * t)) for a, b in zip(c1, c2))

# ============================================================ OLD SOL =======
SKIN     = (224, 172, 138); SKIN_D = (198, 143, 110); SKIN_S = (212, 156, 122)
NAVY     = (46, 60, 96);     NAVY_D = (30, 40, 68);    NAVY_L = (66, 84, 124)
HAT      = (36, 46, 78);     HAT_D  = (25, 32, 56)     # hat + brim
BAND     = (196, 158, 74)                               # gold hat band
BEARD    = (216, 216, 222);  BEARD_D = (176, 178, 190)
SHIRT    = (222, 214, 188)
BELT     = (86, 58, 38);     BUCKLE = (176, 176, 184)
GOLD     = (222, 176, 74)
PANTS    = (72, 62, 74);     BOOT   = (40, 30, 26)
WHITE    = (238, 238, 236);  PUPIL  = (40, 44, 66);    LINE = (30, 26, 34)
MOUTH    = (120, 46, 52)
BLUSH    = (226, 150, 128)

def paint_old_man(surprised):
    rnd = random.Random(1337)
    c = Canvas(64, 64)
    # --- head (uv 0,0 size 8x8x8): face on front (8,8,8,8) -----------------
    c.fill_box((0, 0), (8, 8, 8), SKIN)
    # top of head: dark hair line under the hat; bottom: neck skin
    f = faces(0, 0, 8, 8, 8)
    x, y, w, h = f['top'];    c.rect(x, y, w, h, SKIN_S)
    x, y, w, h = f['bottom']; c.rect(x, y, w, h, SKIN_D)
    x, y, w, h = f['back'];   c.rect(x, y, w, h, SKIN_S)
    for yy in range(y + 1, y + h):            # wispy grey hair on the back
        for xx in range(x, x + w):
            if rnd.random() < 0.62: c.set(xx, yy, BEARD_D if rnd.random() < .5 else BEARD)
    # side faces: ears + beard fringe at bottom
    for side in ('right', 'left'):
        x, y, w, h = f[side]
        c.rect(x, y + h - 2, w, 2, BEARD_D)
        c.set(x + (2 if side == 'right' else 1), y + 4, SKIN_D)   # ear dot
    # FRONT face
    fx, fy = 8, 8
    c.rect(fx, fy, 8, 1, mix(SKIN, HAT_D, 0.35))                  # brim shadow band
    if not surprised:                                             # calm, squinting sailor
        c.rect(fx + 1, fy + 1, 2, 1, BEARD_D)                     # brows
        c.rect(fx + 5, fy + 1, 2, 1, BEARD_D)
        c.set(fx + 1, fy + 2, WHITE); c.set(fx + 2, fy + 2, LINE) # sleepy small eyes
        c.set(fx + 6, fy + 2, LINE);  c.set(fx + 7, fy + 2, WHITE)
        c.set(fx, fy + 4, BLUSH); c.set(fx + 7, fy + 4, BLUSH)    # cheeks
        c.rect(fx + 2, fy + 4, 4, 1, mix(SKIN_D, LINE, 0.45))     # mouth (hidden by beard mostly)
    else:                                                          # GOOGLY surprise eyes
        c.rect(fx + 0, fy, 8, 1, BEARD_D)                          # brows raised to the hat
        c.rect(fx + 1, fy + 1, 2, 2, WHITE)                        # big white eyeballs
        c.rect(fx + 5, fy + 1, 2, 2, WHITE)
        c.set(fx + 2, fy + 2, PUPIL); c.set(fx + 5, fy + 2, PUPIL)# tiny pupils at the bottom
        c.set(fx, fy + 2, BLUSH); c.set(fx + 7, fy + 2, BLUSH)    # blushing cheeks
        c.rect(fx + 3, fy + 4, 2, 2, MOUTH)                        # open mouth (jaw drops too)
        c.set(fx + 3, fy + 5, mix(MOUTH, WHITE, .35))             # hint of a tooth
    # nose box (uv 40,24 size 2x2x1)
    c.fill_box((40, 24), (2, 2, 1), SKIN_D)
    f = faces(40, 24, 2, 2, 1)
    x, y, w, h = f['top']; c.rect(x, y, w, h, SKIN)
    # bulging eyeball boxes (uv 36,48 size 3x3x3): white sclera, pupils on
    # every side so the stare follows you; lids get a red rim when surprised.
    c.fill_box((36, 48), (3, 3, 3), WHITE)
    fe = faces(36, 48, 3, 3, 3)
    for name in ('front', 'right', 'left', 'back'):
        x, y, w, h = fe[name]
        c.rect(x, y, w, h, WHITE)
        c.set(x + 1, y + 1, PUPIL)
        c.rect(x, y + h - 1, w, 1, mix(WHITE, LINE, .25))         # lower lid shade
        if surprised:
            c.rect(x, y, w, 1, mix(WHITE, (200, 90, 80), .7))     # bloodshot upper lid
    c.fill_box((36, 48), (3, 1, 3), WHITE)                        # tops stay clean white
    # beard / jaw (uv 40,16 size 6x4x4)
    c.fill_box((40, 16), (6, 4, 4), BEARD)
    for name in ('front', 'right', 'left', 'back'):
        x, y, w, h = f0 = faces(40, 16, 6, 4, 4)[name]
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                if rnd.random() < 0.4:
                    c.set(xx, yy, BEARD_D if rnd.random() < .55 else mix(BEARD, WHITE, .5))
        c.rect(x, y + h - 1, w, 1, mix(BEARD_D, LINE, .25))       # dirty bottom edge
    if surprised:                                                 # fish crumbs!
        c.set(45, 21, (150, 200, 120)); c.set(48, 22, (220, 160, 90))
    # hat: crown (uv 32,0 size 8x3x8) + brim (uv 0,16 size 10x1x10)
    c.fill_box((32, 0), (8, 3, 8), HAT)
    fh = faces(32, 0, 8, 3, 8)
    x, y, w, h = fh['top']
    c.rect(x, y, w, h, mix(HAT, HAT_D, .35))
    c.speckle(x, y, w, h, [HAT_D], 0.2, rnd)
    x, y, w, h = fh['front']
    c.rect(x, y + h - 1, w, 1, BAND)                              # gold band on the brow
    x, y, w, h = fh['back']
    c.rect(x, y + h - 1, w, 1, BAND)
    c.fill_box((0, 16), (10, 1, 10), HAT_D)
    fb = faces(0, 16, 10, 1, 10)
    x, y, w, h = fb['top']
    c.rect(x, y, w, h, HAT)
    c.rect(x, y, w, 1, mix(HAT, NAVY_L, .3))                      # lit front edge of the brim
    # body (uv 0,28 size 8x12x4)
    c.fill_box((0, 28), (8, 12, 4), NAVY)
    fb = faces(0, 28, 8, 12, 4)
    x, y, w, h = fb['top'];    c.rect(x, y, w, h, NAVY_L)
    x, y, w, h = fb['bottom']; c.rect(x, y, w, h, NAVY_D)
    x, y, w, h = fb['front']
    c.rect(x + 3, y, 2, 3, SHIRT)                                 # open collar
    for by in (4, 7, 10):
        c.set(x + 2, y + by, GOLD); c.set(x + 5, y + by, GOLD)    # double-breasted buttons
    c.rect(x, y + 11, w, 1, BELT); c.rect(x + 3, y + 11, 2, 1, BUCKLE)
    x, y, w, h = fb['back']
    c.rect(x + w // 2 - 1, y, 2, h, NAVY_D)                       # coat tail seam
    for side in ('right', 'left'):
        x, y, w, h = fb[side]
        c.rect(x, y + 11, w, 1, BELT)
    # arms (uv 24,28 & 38,28 size 3x12x4)
    for au in (24, 38):
        c.fill_box((au, 28), (3, 12, 4), NAVY)
        fa = faces(au, 28, 3, 12, 4)
        x, y, w, h = fa['top']; c.rect(x, y, w, h, NAVY_L)
        for name in ('front', 'right', 'left', 'back'):
            x, y, w, h = fa[name]
            c.rect(x, y + h - 3, w, 1, SHIRT)                     # cuff
            c.rect(x, y + h - 2, w, 2, SKIN)                      # hands
    # legs (uv 0,44 & 16,44 size 4x12x4)
    for lu in (0, 16):
        c.fill_box((lu, 44), (4, 12, 4), PANTS)
        fl = faces(lu, 44, 4, 12, 4)
        for name in ('front', 'right', 'left', 'back'):
            x, y, w, h = fl[name]
            c.rect(x, y + h - 4, w, 4, BOOT)                      # sea boots
            c.rect(x, y + h - 4, w, 1, mix(BOOT, WHITE, .2))      # boot highlight
    return c

# ======================================================== SPIDER CRAB ========
CR      = (176, 58, 38);  CR_D = (128, 40, 26);  CR_L = (222, 118, 76)
NUB     = (236, 186, 112)
CRM     = (240, 210, 158);  CRM_D = (206, 166, 118)
STK     = (224, 178, 126)
EYE_W   = (242, 240, 232);  EYE_P = (26, 20, 28)
TIP     = (52, 26, 20)

def paint_crab():
    rnd = random.Random(4242)
    c = Canvas(64, 128)

    # shell uv(0,0) size 14x5x10
    c.fill_box((0, 0), (14, 5, 10), CR)
    f = faces(0, 0, 14, 5, 10)
    x, y, w, h = f['top']
    c.rect(x, y, w, h, CR)
    c.speckle(x, y, w, h, [CR_D, CR_L, NUB, CR], 0.34, rnd)
    for i, (nx, ny) in enumerate([(13, 2), (17, 5), (20, 3), (14, 7), (19, 8)]):
        c.rect(nx, ny, 1, 1, NUB); c.set(nx - 1, ny, mix(CR_L, NUB, .4)); c.set(nx + 1, ny, CR_D)
    x, y, w, h = f['front']
    for yy in range(y, y + h):                                    # gradient down to the rim
        t = (yy - y) / max(1, h - 1)
        c.rect(x, yy, w, 1, mix(CR, CRM, t * 0.55))
    for name in ('right', 'left', 'back'):
        x, y, w, h = f[name]
        c.rect(x, y, w, h, CR if name != 'back' else mix(CR, CR_D, .4))
        c.speckle(x, y, w, h, [CR_D, CR_L], 0.25, rnd)
        c.rect(x, y + h - 1, w, 1, CRM_D)

    # rim uv(0,16) size 11x3x8
    c.fill_box((0, 16), (11, 3, 8), CRM)
    f = faces(0, 16, 11, 3, 8)
    x, y, w, h = f['bottom']
    c.rect(x, y, w, h, CRM)
    for xx in range(x + 1, x + w, 2):                             # belly plates
        c.rect(xx, y, 1, h, CRM_D)
    for name in ('front', 'right', 'left', 'back'):
        x, y, w, h = f[name]
        c.rect(x, y, w, h, mix(CRM, CR, .25))
        c.rect(x, y + h - 1, w, 1, CR_D)

    # eye stalks uv(48,0)/(56,0) size 1x3x1 + eyeballs uv(48,4)/(56,4) size 2x2x2
    for su, eu in ((48, 48), (56, 56)):
        c.fill_box((su, 0), (1, 3, 1), STK)
        c.fill_box((eu, 4), (2, 2, 2), EYE_W)
        fe = faces(eu, 4, 2, 2, 2)
        x, y, w, h = fe['front']
        c.rect(x, y, w, 1, EYE_P)                                 # angry flat pupils
        c.set(x, y + 1, EYE_P)
        x, y, w, h = fe['top']
        c.rect(x, y, w, h, mix(EYE_W, CRM, .3))

    # claw arms uv(0,28)/(24,28) size 6x4x4
    for au in (0, 24):
        c.fill_box((au, 28), (6, 4, 4), CR)
        fa = faces(au, 28, 6, 4, 4)
        for name, r in fa.items():
            x, y, w, h = r
            c.rect(x, y, w, h, CR_L if name == 'top' else CR_D if name == 'bottom' else CR)
        c.speckle(au + 4, 28, 6, 4, [CR, CR_D], 0.2, rnd)

    # legs: hips rows 40..52, shins rows 56..68
    def paint_leg(u, v, length):
        f = faces(u, v, length, 2, 2)
        for name, (x, y, w, h) in f.items():
            if name == 'top':    c.rect(x, y, w, h, mix(CR, CR_L, .35))
            elif name == 'bottom': c.rect(x, y, w, h, CRM_D)
            elif name == 'front':
                for xx in range(x, x + w):                        # red->pale along the leg
                    t = (xx - x) / max(1, w - 1)
                    c.set(xx, y, mix(CR, CR_L, t * .3))
                    c.set(xx, y + 1, mix(mix(CR, CRM, .35), TIP, t * t))
            else:
                c.rect(x, y, w, h, CR)
    hip_uv = [(0, 40), (22, 40), (0, 44), (22, 44), (0, 48), (22, 48), (0, 52), (22, 52)]
    shin_uv = [(0, 56), (28, 56), (0, 60), (28, 60), (0, 64), (28, 64), (0, 68), (28, 68)]
    for (u, v) in hip_uv:  paint_leg(u, v, 8)
    for (u, v) in shin_uv: paint_leg(u, v, 11)
    # sharp dark points on the shin tips (both texture ends - joints read fine)
    for (u, v) in shin_uv:
        c.rect(u, v + 2, 1, 2, TIP)
        c.rect(u + 20, v + 2, 1, 2, TIP)

    # pincers: top uv(0,76)/(28,76) 6x3x6 -> area 24x9; bot uv(0,86)/(28,86) 6x2x6 -> 24x8
    for pu in (0, 28):
        c.fill_box((pu, 76), (6, 3, 6), CR)
        ft = faces(pu, 76, 6, 3, 6)
        for name, (x, y, w, h) in ft.items():
            if name == 'top':    c.rect(x, y, w, h, CR_L)
            elif name == 'front':
                c.rect(x, y, w, h, CR)
                c.rect(x, y + h - 1, w, 1, CRM)                   # bite ridge
                for xx in range(x + 1, x + w, 2):
                    c.set(xx, y + h - 2, CRM_D)                   # teeth
            elif name == 'bottom': c.rect(x, y, w, h, CRM_D)
            else: c.rect(x, y, w, h, CR)
        c.speckle(pu + 6, 76, 6, 3, [CR_L, NUB], 0.3, rnd)
        c.fill_box((pu, 86), (6, 2, 6), CR_D)
        fb = faces(pu, 86, 6, 2, 6)
        for name, (x, y, w, h) in fb.items():
            c.rect(x, y, w, h, CR_D if name != 'front' else mix(CR, CR_D, .3))
        x, y, w, h = fb['front']
        for xx in range(x + 1, x + w, 2):
            c.set(xx, y, CRM)                                     # lower teeth
    return c

# ============================================================== FISHES ========
def paint_fish(species):
    """All six species share the FishModel UVs: body uv(0,0) 5x5x12,
       tail uv(0,18) 0x4x5 plane, fins uv(16,18)/(16,22) 4x0x3 planes."""
    rnd = random.Random(sum(ord(ch) for ch in species) * 7 + 11)
    c = Canvas(64, 64)
    P = {
        'anchovy':  dict(top=(94, 122, 142), mid=(140, 168, 186), belly=(222, 230, 236), eye=(30, 34, 44)),
        'herring':  dict(top=(78, 96, 116),  mid=(178, 196, 208), belly=(232, 238, 242), eye=(30, 34, 44)),
        'shrimp':   dict(top=(222, 132, 106), mid=(236, 158, 128), belly=(248, 204, 178), eye=(52, 30, 30)),
        'crab':     dict(top=(186, 66, 40),  mid=(206, 96, 60),  belly=(238, 196, 146), eye=(244, 236, 220)),
        'pufferfish':dict(top=(196, 158, 70), mid=(226, 196, 120), belly=(242, 226, 176), eye=(36, 28, 24)),
        'lobster':  dict(top=(150, 34, 26),   mid=(188, 52, 38),  belly=(226, 158, 128), eye=(26, 18, 18)),
    }[species]
    BW, BH, BD = 5, 5, 12          # body
    c.fill_box((0, 0), (BW, BH, BD), P['mid'])
    f = faces(0, 0, BW, BH, BD)
    x, y, w, h = f['top']
    c.rect(x, y, w, h, P['top'])
    if species in ('anchovy', 'herring'):                          # dark lateral stripe
        c.rect(12, 14, 5, 1, mix(P['top'], (20, 20, 30), .5))
        for side in ('right', 'left'):
            sx, sy, sw, sh = f[side]
            for xx in range(sx + 1, sx + sw - 1, 2):               # little herring spots
                c.set(xx, sy, mix(P['top'], (10, 12, 18), .6))
    if species == 'shrimp':                                         # segmented bands
        for side in ('right', 'left'):
            sx, sy, sw, sh = f[side]
            for xx in range(sx, sx + sw, 3):
                c.rect(xx, sy, 1, sh, mix(P['top'], P['mid'], .3))
    if species == 'lobster':
        c.rect(*f['top'][:2], f['top'][2], 1, mix(P['top'], (10, 5, 5), .4))
    if species == 'pufferfish':
        x, y, w, h = f['top']
        for i in range(w * h // 3):
            c.set(x + rnd.randrange(w), y + rnd.randrange(h), mix(P['top'], (60, 50, 20), .6))
    x, y, w, h = f['front']                                         # face
    c.rect(x, y, w, h, P['mid'])
    c.set(x, y + 1, P['eye']); c.set(x + 1, y + 1, mix(P['eye'], P['belly'], .2))   # left eye
    c.set(x + w - 2, y + 1, P['eye']); c.set(x + w - 1, y + 1, mix(P['eye'], P['belly'], .2))
    c.rect(x + 1, y + 3, w - 2, 1, mix(P['mid'], P['eye'], .55))    # mouth
    x, y, w, h = f['bottom']
    c.rect(x, y, w, h, P['belly'])
    for side in ('right', 'left', 'back'):                          # belly fade
        sx, sy, sw, sh = f[side]
        c.rect(sx, sy + sh - 1, sw, 1, P['belly'])
    # tail fin plane uv(0,18): two 5x4 quads at (0,23) & (5,23)
    c.rect(0, 23, 10, 4, mix(P['mid'], P['top'], .35))
    c.rect(0, 23, 10, 1, P['top'])
    for xx in range(1, 10, 2):
        c.set(xx, 25, mix(P['mid'], (255, 255, 255), .25))
    # side fins uv(16,18)/(16,22): top/bottom 4x3 quads at (19,18)(23,18)(19,22)(23,22)
    for fy in (18, 22):
        c.rect(19, fy, 4, 3, mix(P['mid'], P['top'], .2))
        c.rect(23, fy, 4, 3, mix(P['mid'], P['belly'], .2))
    return c

BASE = 'src/main/resources/assets/howtofish/textures/entity/'
paint_old_man(False).save_png(BASE + 'old_man.png')
paint_old_man(True).save_png(BASE + 'old_man_eating.png')
paint_crab().save_png(BASE + 'fish/spider_crab_boss.png')
for sp in ('anchovy', 'herring', 'shrimp', 'crab', 'pufferfish', 'lobster'):
    paint_fish(sp).save_png(BASE + 'fish/' + sp + '.png')
