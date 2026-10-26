"""Prévia dos modelos nativos exportados, fora do Minecraft."""
import json, math
from pathlib import Path
import numpy as np
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / 'target/beleza/assets/rpgatributos'
FONT = ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf', 17)
TITLE = ImageFont.truetype('C:/Windows/Fonts/segoeuib.ttf', 26)

def render(ident, yaw=.5, stretch=(1,1,1), fit=True):
    model = json.loads((DATA/'models/item'/('beleza_'+ident+'.json')).read_text())
    texture = Image.open(DATA/'textures'/ (model['textures']['t'].split(':')[1]+'.png')).convert('RGB')
    right = np.array([math.cos(yaw),0,math.sin(yaw)])
    view = np.array([math.sin(yaw)*.94,.342,-math.cos(yaw)*.94])
    basis = np.array([right,np.cross(right,view),view])
    faces=[]; points=[]
    for element in model['elements']:
        x,y,z=element['from'];a,b,c=element['to']
        vertices=(np.array([[x,y,z],[a,y,z],[a,b,z],[x,b,z],[x,y,c],[a,y,c],[a,b,c],[x,b,c]])-[8,0,8])*stretch
        projection=vertices@basis.T; points.extend(projection)
        uv=element['faces']['north']['uv'];color=texture.getpixel((int((uv[0]+uv[2])/2),int((uv[1]+uv[3])/2)))
        for ids,shade in [([0,1,2,3],1),([5,4,7,6],.67),([4,0,3,7],.72),([1,5,6,2],.83),([3,2,6,7],1.12),([4,5,1,0],.55)]:
            face=projection[ids];faces.append((face[:,2].mean(),face,tuple(min(255,int(k*shade)) for k in color)))
    points=np.array(points);lo=points[:,:2].min(axis=0);hi=points[:,:2].max(axis=0)
    scale=min(190/max(1,hi[0]-lo[0]),175/max(1,hi[1]-lo[1])) if fit else 9
    center=(lo+hi)/2 if fit else np.array([0,10])
    out=Image.new('RGB',(240,220),(23,31,47));d=ImageDraw.Draw(out)
    for _,face,color in sorted(faces,key=lambda f:f[0]):
        d.polygon([(120+(p[0]-center[0])*scale,112-(p[1]-center[1])*scale) for p in face],fill=color)
    return out

props=[('bandeira','Estandarte'),('estante','Estante'),('mesa','Mesa'),('cadeira','Cadeira'),('armas','Expositor de armas'),('escudo','Escudo'),('braseiro','Braseiro'),('lanterna','Lanterna'),('cristal','Cristais'),('altar','Altar'),('caixotes','Caixotes'),('vaso','Vaso'),('placa','Placa'),('bau','Moldura de baú'),('portico','Pórtico')]
themes=['cripta','caverna','inferno','gelo']
entries=[('decor_'+ident+'_'+themes[n%4],label) for n,(ident,label) in enumerate(props)]
entries += [('corpo_'+i,i.capitalize()) for i in ['mochila','aljava','capa','colar']]
entries += [('buraco_negro','Buraco negro')]
board=Image.new('RGB',(1040,110+math.ceil(len(entries)/4)*260),(15,21,34));d=ImageDraw.Draw(board)
d.text((26,18),'Mais vida para o mundo e para o personagem',font=TITLE,fill='#e7cd91')
d.text((26,60),'Modelos da versão 2.81 · prévia fora do jogo · quatro temas de decoração',font=FONT,fill='#a8b9cf')
for n,(ident,label) in enumerate(entries):
    x=10+n%4*260;y=104+n//4*260;board.paste(render(ident),(x,y));d.text((x+12,y+225),label,font=FONT,fill='#dfe5ef')
board.save(ROOT/'docs/embelezamento-2.81.png')
frames=[]
for n in range(32):
    frame=Image.new('RGB',(780,320),(15,21,34));d=ImageDraw.Draw(frame)
    d.text((20,12),'Prévia dos modelos animados · fora do jogo',font=TITLE,fill='#e7cd91')
    s=max(.01,min(1,n/5)*min(1,(32-n)/5)*(1+math.sin(n*.22)*.08))
    for k,(ident,label,stretch) in enumerate([('buraco_negro','Buraco negro',(s,s*.8,s)),('pilar_fogo','Pilar de fogo',(s*.6,s*1.2,s*.6)),('invocacao_vida','Círculo de invocação',(s,.5,s))]):
        frame.paste(render(ident,n*.12,stretch,False),(10+k*260,58));d.text((24+k*260,287),label,font=FONT,fill='#dfe5ef')
    frames.append(frame)
frames[0].save(ROOT/'docs/magias-2.81.gif',save_all=True,append_images=frames[1:],duration=80,loop=0)
print(ROOT/'docs/embelezamento-2.81.png')
