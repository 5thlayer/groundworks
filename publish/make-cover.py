# SPDX-FileCopyrightText: 2026 5thlayer
# SPDX-License-Identifier: MIT

# Temporary cover and icon, until an artist draws the real ones, in the style
# of Beltworks' and Craftworks' publish/make-cover.py. Run from the repo root with Pillow installed,
# with the decompiled Minecraft sources (for the block textures) at ../mc-26.1.2.109-src.
from PIL import Image, ImageDraw, ImageFont
T='../mc-26.1.2.109-src/assets/minecraft/textures/block/'
BG=(24,26,32)
ACCENT=(90,165,255)   # the Placement Preview's blue
REFUSED=(235,70,70)   # and its red
FADE=140  # 0 leaves the rows at full strength, 255 hides them
# Each row is a line being laid: blocks already standing, then the preview of the rest.
BLOCKS=['stone_bricks','oak_planks','cobblestone','bricks','spruce_planks','polished_andesite','deepslate_tiles']
_cache={}
def tex(name,s):
    if (name,s) not in _cache:
        _cache[name,s]=Image.open(T+name+'.png').convert('RGBA').resize((s,s),Image.NEAREST)
    return _cache[name,s]
def block(img,x,y,s,name,ghost=None):
    t=tex(name,s)
    if ghost is None:
        img.paste(t,(x,y),t); return
    # a planned block: the texture washed with the preview's colour, translucent, outlined
    wash=Image.alpha_composite(t,Image.new('RGBA',(s,s),ghost+(150,)))
    wash.putalpha(200)
    img.alpha_composite(wash,(x,y))
    d=ImageDraw.Draw(img); lw=max(2,s//20)
    d.rectangle([x,y,x+s-1,y+s-1],outline=(255,255,255,230),width=lw)
def make(W,H,out,title_size,s):
    img=Image.new('RGBA',(W,H),BG+(255,))
    bh=int(title_size*1.6); free=(H-bh)//2; pitch=s+s//4
    n=max(1,free//pitch); top=(free-n*pitch+s//4)//2
    ys=[top+i*pitch for i in range(n)]+[(H-s)//2]+[H-free+top+i*pitch for i in range(n)]
    for row,y in enumerate(ys):
        name=BLOCKS[row%len(BLOCKS)]
        laid=3+(row*5)%6       # how far each line already stands
        x=-((row*s*2)%(s*3)); i=0
        while x<W:
            ghost=None if i<laid else (REFUSED if (i-laid)%9==6 and row%2 else ACCENT)
            block(img,x,y,s,name,ghost); x+=s; i+=1
    img=Image.alpha_composite(img,Image.new('RGBA',(W,H),BG+(FADE,)))
    band=Image.new('RGBA',(W,H),(0,0,0,0)); bd=ImageDraw.Draw(band)
    bd.rectangle([0,(H-bh)//2,W,(H+bh)//2],fill=(16,17,22,225))
    img=Image.alpha_composite(img,band)
    d=ImageDraw.Draw(img)
    lt=max(3,title_size//25)
    d.rectangle([0,(H-bh)//2,W,(H-bh)//2+lt],fill=ACCENT); d.rectangle([0,(H+bh)//2-lt,W,(H+bh)//2],fill=ACCENT)
    f1=ImageFont.truetype('/System/Library/Fonts/Supplemental/Impact.ttf',title_size)
    f2=ImageFont.truetype('/System/Library/Fonts/Supplemental/Arial Black.ttf',int(title_size*0.62))
    a,w='GROUND','works'
    b1=d.textbbox((0,0),a,font=f1); b2=d.textbbox((0,0),w,font=f2)
    w1=b1[2]-b1[0]; w2=b2[2]-b2[0]; h2=b2[3]-b2[1]
    px=int(title_size*0.16); gap=int(title_size*0.1)
    bw=w2+2*px; tot=w1+gap+bw; x=(W-tot)//2
    h1=b1[3]-b1[1]; y=(H-h1)//2-b1[1]; sh=max(3,title_size//20)
    d.text((x+sh-b1[0],y+sh),a,font=f1,fill=(0,0,0))
    d.text((x-b1[0],y),a,font=f1,fill=ACCENT)
    bx=x+w1+gap; bt=(H-h1)//2; bb=bt+h1
    d.rounded_rectangle([bx+sh,bt+sh,bx+bw+sh,bb+sh],radius=px,fill=(0,0,0))
    d.rounded_rectangle([bx,bt,bx+bw,bb],radius=px,fill=(240,240,236))
    ty=bt+(h1-h2)//2-b2[1]
    d.text((bx+px-b2[0],ty),w,font=f2,fill=BG)
    img.convert('RGB').save(out)
make(1280,640,'publish/groundworks-cover.png',150,80)
make(512,512,'publish/groundworks-icon.png',80,64)
