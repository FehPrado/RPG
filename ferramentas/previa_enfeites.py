"""Prévia dos modelos de item exportados; não é uma captura do Minecraft."""
import json, math
from pathlib import Path
import numpy as np
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / 'target/enfeites'
OUT = ROOT / 'docs/enfeites-2.81.png'
MODELS = DATA / 'assets/rpgatributos/models/item/enfeite'
TEXTURES = DATA / 'assets/rpgatributos/textures/item'
font = ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf', 17)
bold = ImageFont.truetype('C:/Windows/Fonts/segoeuib.ttf', 25)

def preview(ids):
    cubes = [([0, 0, 0], [16, 16, 16], (196, 150, 117), None),
             ([0, 13, 0], [16, 17, 16], (65, 44, 39), None),
             ([3, 9, -.1], [5, 11, 0], (37, 48, 60), None),
             ([11, 9, -.1], [13, 11, 0], (37, 48, 60), None)]
    for ident in ids:
        model = json.loads((MODELS / (ident + '.json')).read_text(encoding='utf-8'))
        texture = Image.open(TEXTURES / ('enfeite_' + ident + '.png')).convert('RGB')
        for element in model['elements']:
            u = element['faces']['north']['uv'][0]
            cubes.append((element['from'], element['to'], texture.getpixel((int(u) + 3, 7)), element.get('rotation')))
    yaw, elev = math.radians(28), math.radians(16)
    right = np.array([math.cos(yaw), 0, math.sin(yaw)])
    view = np.array([math.sin(yaw)*math.cos(elev), math.sin(elev), -math.cos(yaw)*math.cos(elev)])
    up = np.cross(right, view)
    basis = np.array([right, up, view])
    faces, points = [], []
    for lo, hi, color, rotation in cubes:
        x,y,z=lo; a,b,c=hi
        vertices=np.array([[x,y,z],[a,y,z],[a,b,z],[x,b,z],[x,y,c],[a,y,c],[a,b,c],[x,b,c]],float)
        if rotation:
            angle=math.radians(rotation['angle']); axis='xyz'.index(rotation['axis'])
            origin=np.array(rotation['origin']); mat=np.eye(3); u,v=((1,2),(2,0),(0,1))[axis]
            mat[u,u]=mat[v,v]=math.cos(angle);mat[u,v]=-math.sin(angle);mat[v,u]=math.sin(angle)
            vertices=(vertices-origin)@mat.T+origin
        projection=vertices@basis.T; points.extend(projection)
        for indices, shade in [([0,1,2,3],1),([5,4,7,6],.67),([4,0,3,7],.72),([1,5,6,2],.83),([3,2,6,7],1.12),([4,5,1,0],.55)]:
            face=projection[indices]; rgb=tuple(min(255,int(k*shade)) for k in color)
            faces.append((face[:,2].mean(),face,rgb))
    points=np.array(points); mins=points[:,:2].min(axis=0);maxs=points[:,:2].max(axis=0)
    scale=min(170/(maxs[0]-mins[0]),170/(maxs[1]-mins[1]));center=(maxs+mins)/2
    image=Image.new('RGB',(220,195),(23,31,47));draw=ImageDraw.Draw(image)
    for _,face,rgb in sorted(faces,key=lambda f:f[0]):
        xy=[(110+(p[0]-center[0])*scale,95-(p[1]-center[1])*scale) for p in face]
        draw.polygon(xy,fill=rgb)
    return image

entries=[(name,[ident]) for ident,name in (line.split('|',1) for line in (DATA/'catalogo.txt').read_text(encoding='utf-8-sig').splitlines())]
entries += [('Barba + chifres',['barba_ana','chifres_dragao']),('Ancião rúnico',['barba_branca','oculos_redondos','tiara_lunar']),('Aventureiro abissal',['barba_trancada','chifres_curvos','tapa_olho','orelhas_elficas'])]
board=Image.new('RGB',(960,98+math.ceil(len(entries)/4)*245),(15,21,34));d=ImageDraw.Draw(board)
d.text((24,18),'Seu personagem, suas combinações',font=bold,fill=(231,205,145))
d.text((24,55),'27 enfeites · nove categorias · prévia dos modelos, fora do jogo',font=font,fill=(168,185,207))
for i,(name,ids) in enumerate(entries):
    x=12+(i%4)*240;y=92+(i//4)*245
    board.paste(preview(ids),(x+4,y))
    d.text((x+8,y+200),name,font=font,fill=(223,229,239))
OUT.parent.mkdir(exist_ok=True);board.save(OUT);print(OUT)
