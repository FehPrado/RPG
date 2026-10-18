"""Author the 78 RPGAtributos CEM variants from the preserved CEM templates.

Usage: python ferramentas/gerar_modelos_visuais.py
Requires Pillow and numpy. Does not overwrite blockbench/preparados or the Guardians.
The generated JEM/PNG pairs are runtime resources; no downloaded textures are used.
"""
from __future__ import annotations

import copy
import hashlib
import json
import math
from pathlib import Path
import zipfile

import numpy as np
from PIL import Image, ImageDraw, ImageFont
from modelos_criaturas import populate

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / 'blockbench/preparados'
OUT = ROOT / 'src/main/resources/modelos/visual'
PREVIEW = ROOT / 'blockbench/autorais'

# id: palette, signature, Portuguese art direction. All 58 non-elite targets explicit.
DESIGNS = {
 'nian': ('nian','nian','Leão festivo escarlate, chifres, juba dourada e sinos'),
 'lobisomem': ('silver','lobisomem','Lobo corcunda com tórax musculoso, garras longas e presas'),
 'vampiro': ('void','vampiro','Nobre pálido com casaca, gola alta, medalhão e capa vinho'),
 'coracao_da_penumbra': ('shadow','coracao_da_penumbra','Carcaça de pedra negra com coração violeta exposto e raízes'),
 'guardiao_dos_ventos': ('sky','guardiao_dos_ventos','Máscara alada sobre espiral de vento, asas e aros dourados'),
 'sumo_sacerdote': ('ocean','sumo_sacerdote','Sacerdote anfíbio com coral, mitra e tridente cerimonial'),
 'guardiao_da_forja_antiga': ('copper','guardiao_da_forja_antiga','Construto com peito-fornalha, ombreiras de bigorna e martelo'),
 'bibliotecario_amaldicoado': ('violet','bibliotecario_amaldicoado','Arquivista de manto com livros, correntes e grimório aberto'),
 'fera_ancestral': ('forest','fera_ancestral','Besta maciça com placas dorsais, galhadas e presas curvas'),
 'rei_caido': ('steel','rei_caido','Rei esquelético com coroa quebrada, armadura e manto rasgado'),
 'sombra_sem_nome': ('shadow','sombra_sem_nome','Assassino encapuzado com máscara, bandoleiras e lâminas'),
 'arqueiro_espectral': ('frost','arqueiro_espectral','Arqueiro de osso com capuz celeste, arco e aljava'),
 'paladino_corrompido': ('ivory','paladino_corrompido','Paladino de marfim e ouro tomado por raízes da corrupção'),
 'pixiu': ('gold', 'pixiu', 'Leão dourado com chifre, juba e pequenas asas de jade'),
 'baize': ('ivory', 'baize', 'Besta branca com três olhos, chifres ramificados e barba'),
 'kunpeng': ('ocean', 'kunpeng', 'Peixe-pássaro azul com bico, nadadeiras e asas largas'),
 'golem_ancestral': ('forest', 'ancient', 'Pedra musgosa, ombreiras de ruína e coração de jade'),
 'rainha_aracnidea': ('violet', 'queen', 'Carapaça real, coroa e presas de marfim'),
 'senhor_das_chamas': ('ember', 'flame', 'Máscara de obsidiana, coroa de fogo e hastes incandescentes'),
 'lich': ('frost', 'lich', 'Crânio de marfim, coroa gelada e manto azul'),
 'tempestade_viva': ('storm', 'storm', 'Máscara de tempestade e aletas em forma de raios'),
 'arauto_do_fim': ('void', 'herald', 'Armadura negra, chifres e ombreiras do vazio'),
 'lobo_alfa': ('silver', 'alpha', 'Pelagem cinza, juba espessa e presas'),
 'lobo_das_sombras': ('shadow', 'shadowwolf', 'Pelagem escura e crista violeta de sombra'),
 'gato_selvagem': ('sand', 'wildcat', 'Felino ocre com tufos nas orelhas e manchas'),
 'pantera_sombria': ('shadow', 'panther', 'Pantera negra com presas e marcas violetas'),
 'corcel_de_guerra': ('steel', 'warhorse', 'Barda de aço, testeira e penacho vermelho'),
 'corcel_celeste': ('sky', 'pegasus', 'Corcel branco com asas emplumadas e diadema dourado'),
 'golem_de_guerra': ('steel', 'war', 'Placas de aço rebitadas e ombreiras militares'),
 'colosso_de_ferro': ('copper', 'colossus', 'Couraça pesada, ombreiras duplas e núcleo âmbar'),
 'urso_glacial': ('frost', 'icebear', 'Urso branco com placas de gelo na cabeça e no dorso'),
 'urso_do_inverno_eterno': ('sky', 'winterbear', 'Urso ancestral com coroa e cristais longos de gelo'),
 'raposa_astuta': ('ember', 'fox', 'Raposa ruiva com tufos e coleira de explorador'),
 'raposa_espiritual': ('ivory', 'spiritfox', 'Raposa branca com três caudas e marcas de jade'),
 'camelo_do_deserto': ('sand', 'desert', 'Manta de viagem e alforjes de couro'),
 'camelo_das_areias_eternas': ('gold', 'eternal', 'Manta azul, adereços dourados e coroa do deserto'),
 'lhama_guerreira': ('steel', 'warllama', 'Peitoral militar e elmo com penacho'),
 'lhama_ancestral': ('ivory', 'ancestralllama', 'Lã clara, adereços de jade e diadema cerimonial'),
 'papagaio_real': ('royal', 'royalbird', 'Plumagem vermelha e azul, cauda longa e crista dourada'),
 'ave_do_trovao': ('storm', 'thunderbird', 'Plumagem azul e amarela com penas em raios'),
 'lobo_espectral': ('sky', 'spectralwolf', 'Lobo azul-claro com crista etérea'),
 'lobo_ancestral': ('forest', 'ancestorwolf', 'Lobo de musgo com chifres de madeira'),
 'automato': ('copper', 'automaton', 'Autômato de bronze com visor, antena e caldeira'),
 'golem_de_pedra': ('stone', 'stone', 'Pedras sobrepostas e cristais de terra'),
 'elemental': ('violet', 'elemental', 'Núcleo arcano com máscara e aletas cristalinas'),
 'espirito': ('forest', 'spirit', 'Espírito de vida com coroa de folhas e asas claras'),
 'sombra_morto_vivo': ('shadow', 'shadow', 'Soldado sombrio com capuz e peito marcado'),
 'sombra_arqueiro': ('void', 'shadowarcher', 'Arqueiro sombrio com capuz e aljava'),
 'sombra_aranha': ('shadow', 'shadowspider', 'Aranha negra com carapaça e presas violetas'),
 'cronista': ('parchment', 'scribe', 'Cronista com óculos, livros e pena'),
 'mercador_arcano': ('violet', 'merchant', 'Mercador com turbante, cristal e bolsas'),
 'mestre_mineiro': ('copper', 'miner', 'Anão com barba, capacete de lâmpada e picareta'),
 'joalheiro': ('gold', 'jeweler', 'Anão com monóculo e estojo de gemas'),
 'ferreiro_anao': ('ember', 'smith', 'Anão com barba, avental e martelo de forja'),
 'taverneiro': ('sand', 'innkeeper', 'Anão com barba, avental e caneca'),
 'mestre_de_guerra': ('steel', 'warmaster', 'Anão com elmo, ombreiras e machado'),
 'druida': ('forest', 'druid', 'Elfo com galhadas de madeira e manto de folhas'),
 'arqueira': ('forest', 'archer', 'Elfa com capuz, aljava e arco às costas'),
 'curandeira': ('ivory', 'healer', 'Elfa com diadema e bolsas de ervas'),
 'tecela': ('violet', 'weaver', 'Elfa com faixa e carretéis de linhas'),
 'mestra_da_lamina': ('silver', 'blademaster', 'Elfa com tiara, proteção de ombros e espada'),
 'mestre': ('parchment', 'master', 'Mestre de barba branca e colar de contas'),
 'monge_herbalista': ('forest', 'herbalist', 'Monge com cesto de ervas às costas'),
 'guardiao_do_sino': ('gold', 'bellkeeper', 'Monge com chapéu ritual e sino de bronze'),
 'peregrino': ('sand', 'pilgrim', 'Peregrino com chapéu largo e mochila'),
 'mestre_do_bastao': ('ember', 'staffmaster', 'Mestre com faixa, contas e bastão às costas'),
 'velho_pescador': ('ocean', 'fisher', 'Pescador de barba branca, chapéu e cesto'),
 'peixeira': ('ocean', 'fishmonger', 'Peixeira com lenço, avental e peixe no cinto'),
 'carpinteiro_naval': ('copper', 'carpenter', 'Carpinteiro com avental, martelo e prancha'),
 'menina_do_cais': ('sky', 'child', 'Menina com laço, tranças e bolsa de conchas'),
 'mestra_das_mares': ('ocean', 'seamaster', 'Mestra com diadema de coral e arpão'),
}

# body, trim, luminous accent, secondary / dark material.
PALETTES = {
 'nian': ('ac3b30','edbd5d','76dbaf','482b30'),
 'gold': ('ba8834','f4d57a','6de0b4','40384b'),
 'ivory': ('e0dfc9','ad9863','68c4b3','465975'),
 'ocean': ('316c86','c6b07e','77dedb','233747'),
 'forest': ('50744f','aa8b50','a3e789','283e38'),
 'violet': ('71577f','c6a166','d9a0f6','302c48'),
 'ember': ('98503a','e4a44d','ffcf65','332e35'),
 'frost': ('a0c3d1','edf4ed','70d7f2','3c668c'),
 'storm': ('3f638e','e5bb4f','cff1ff','26364f'),
 'void': ('40394d','83709c','d695ef','201e2b'),
 'silver': ('84939a','d3d9ce','9cd9ca','3d4757'),
 'shadow': ('353343','77629a','b78df0','211f30'),
 'sand': ('b18e5f','e0c58a','82b7a6','55443b'),
 'steel': ('657888','c5c8b5','b25d51','303d4c'),
 'sky': ('c2d9de','e9d291','9ce0ec','567c9c'),
 'copper': ('a57951','e3b57b','87d4c9','424a4c'),
 'royal': ('ae4d4b','e4bc62','639ed2','344269'),
 'stone': ('777c70','b3ac8c','b6d582','3d4b48'),
 'parchment': ('ad9674','e4d4aa','80b6b4','514753'),
}
FACE_NAMES = ('north','south','east','west','up','down')
MATERIALS = ['body','trim','accent','dark','skin','hair','white','wood','metal','eye','cloth','gem','face','beard','mask','leaf']

def color(h): return tuple(bytes.fromhex(h))

def atlas(palette, seed, npc=False, beard=False, ident=''):
    """Paint a fresh 128px pixel atlas. No vanilla PNGs are copied or recolored."""
    body, trim, accent, dark = map(color, PALETTES[palette])
    skin = (186,139,106)
    colors = [body,trim,accent,dark,skin,(76,58,49),(225,225,205),(111,79,49),
              (139,151,150),accent,body,accent,skin, (190,189,176) if beard else (91,65,47),dark,(66,112,67)]
    rng = np.random.default_rng(seed)
    img = Image.new('RGBA',(128,128),(0,0,0,0))
    for i, c in enumerate(colors):
        tile = np.zeros((16,16,4),dtype=np.uint8)
        noise = rng.integers(-7,8,(16,16))
        if i in (0,10): noise += ((np.indices((16,16)).sum(axis=0) % 4)==0)*4
        for k in range(3): tile[:,:,k] = np.clip(c[k]+noise,0,255)
        tile[:,:,3] = 255
        im = Image.fromarray(tile)
        d = ImageDraw.Draw(im)
        if i in (1,8):
            d.line((0,0,15,0),fill=tuple(min(255,x+30) for x in c))
            d.line((0,0,0,15),fill=tuple(min(255,x+20) for x in c))
            d.line((0,15,15,15),fill=tuple(max(0,x-30) for x in c))
            for x,y in ((2,2),(13,2),(2,13),(13,13)): d.point((x,y),fill=dark)
        if i==11:
            d.polygon([(8,1),(14,7),(8,14),(1,7)],fill=accent)
            d.line((8,2,3,7),fill=(235,255,245),width=2)
        if i==12:
            if npc:
                d.rectangle((0,0,15,3),fill=colors[5])
                for x in (3,10):
                    d.rectangle((x,6,x+2,7),fill=(235,230,213))
                    d.point((x+1,7),fill=dark)
                d.line((6,12,9,12),fill=(113,68,57))
            else:
                d.rectangle((0,0,15,15),fill=body)
                for x in (2,10):
                    d.rectangle((x,5,x+3,7),fill=dark)
                    d.rectangle((x+1,5,x+2,6),fill=accent)
                d.rectangle((6,10,9,11),fill=dark)
                if 'creeper' in ident:
                    for x in (2,10): d.rectangle((x,4,x+3,7),fill=dark)
                    d.rectangle((6,7,9,12),fill=dark)
                    d.rectangle((4,9,5,14),fill=dark)
                    d.rectangle((10,9,11,14),fill=dark)
                elif 'skeleton' in ident or ident=='lich':
                    skull = (206,214,211) if ident=='lich' else body
                    d.rectangle((0,0,15,15),fill=skull)
                    for x in (2,10):
                        d.rectangle((x,4,x+3,8),fill=dark)
                        d.line((x+1,6,x+2,6),fill=accent)
                    d.rectangle((7,8,8,10),fill=dark)
                    for x in (4,7,10): d.rectangle((x,12,x+1,14),fill=dark)
        if i==14:
            for x in (2,10): d.rectangle((x,5,x+3,6),fill=accent)
            d.rectangle((6,10,9,13),fill=dark)
        img.paste(im,((i%8)*16,(i//8)*16))
    return img

def uv(mat):
    i = MATERIALS.index(mat)
    x,y = i%8*16,i//8*16
    return [x,y,x+16,y+16]

def paint(box, mat, face=None):
    box.pop('textureOffset',None)
    for f in FACE_NAMES: box['uv'+f.title()] = uv(face if face and f=='north' else mat)

def nodes(m):
    yield m
    for s in m.get('submodels',[]): yield from nodes(s)

class Sculpt:
    def __init__(self, data):
        self.data = data
        self.parts = {m['part']:m for m in data['models']}
        self.additions = 0

    def box(self, part, xyz, size, mat='trim'):
        assert part in self.parts, part
        b = {'coordinates':[round(v,3) for v in (*xyz,*size)]}
        paint(b,mat)
        self.parts[part].setdefault('boxes',[]).append(b)
        self.additions += 1
        return b

    def bound(self, part):
        boxes = self.parts[part].get('boxes',[])
        if not boxes: return (-3,12,-3,6,8,6)
        return tuple(boxes[0]['coordinates'])

    def crown(self, part='head', mat='trim', tall=3):
        x,y,z,w,h,d = self.bound(part)
        self.box(part,(x-.3,y+h-.7,z-.3),(w+.6,1,d+.6),mat)
        for a in (0,.42,.84): self.box(part,(x+w*a,y+h,z-.4),(max(.7,w*.16),tall,1),mat)
        self.box(part,(x+w*.4,y+h-.7,z-.55),(max(1,w*.2),1.5,.35),'gem')

    def horns(self, part='head', mat='trim', height=4, branch=False):
        x,y,z,w,h,d = self.bound(part)
        for side in (-1,1):
            xx=x if side<0 else x+w-1
            self.box(part,(xx,y+h-.5,z+d*.5),(1.3,height,1.3),mat)
            self.box(part,(xx+side*.65,y+h+height-1,z+d*.5),(.8,1.5,.8),mat)
            if branch:
                self.box(part,(xx-1,y+h+height*.5,z+d*.5),(3,.8,.8),mat)

    def armor(self, part='body', mat='metal', heavy=False):
        x,y,z,w,h,d = self.bound(part)
        if part=='head':
            # A helmet leaves the face/eyes exposed; a breastplate would obscure them.
            self.box(part,(x-.35,y+h-.8,z-.35),(w+.7,1.1,d+.7),mat)
            for xx in (x-.4,x+w): self.box(part,(xx,y+h*.35,z+d*.3),(.6,h*.55,d*.65),mat)
            return
        self.box(part,(x-.35,y+h*.2,z-.55),(w+.7,h*.7,.8),mat)
        self.box(part,(x+w*.35,y+h*.55,z-.8),(w*.3,h*.2,.5),'gem')
        if heavy:
            for xx in (x-2,x+w-.5): self.box(part,(xx,y+h-3,z-1),(2.5,3,d+2),mat)

    def crest(self, part='head', mat='accent', height=3):
        x,y,z,w,h,d = self.bound(part)
        for i in range(3):
            self.box(part,(x+w/2-.5,y+h-.3,z+d*i/3),(1,height+i*.5,max(.8,d/3)),mat)

    def wings(self, part='body', mat='white', span=10):
        x,y,z,w,h,d = self.bound(part)
        for side in (-1,1):
            for i in range(4):
                xx=x-(i+1)*span/4 if side<0 else x+w+i*span/4
                self.box(part,(xx,y+h*.65+i*.8,z+d*.25+i*.7),(span/4+.2,1.2,d*.65-i*.6),mat)


def npc(s, name, group, role):
    # Replace the generic hats/outer robes; keep the named animation parts and pivots.
    for part in ('headwear','headwear2','bodywear'):
        if part in s.parts:
            s.parts[part]['boxes']=[]
            s.parts[part].pop('submodels',None)
    elf = 'elfica' in group
    dwarf = 'anoes' in group
    if elf:
        for x in (-6,4): s.box('head',(x,28,-.5),(2,2,1),'skin')
    if dwarf or role in ('master','fisher'):
        s.box('head',(-3,23,-5),(6,5,1.2),'beard')
        for x in (-2,1): s.box('head',(x,21.5,-5),(1.2,3,1.3),'beard')
    # Coat, belt and clasp, with openings at the knees.
    s.box('body',(-4.25,8,-3.25),(8.5,13,6.5),'cloth')
    s.box('body',(-4.5,13,-3.5),(9,1.4,7),'dark')
    s.box('body',(-.8,13,-3.8),(1.6,1.4,.5),'trim')
    if role in ('smith','carpenter','innkeeper','fishmonger','weaver','jeweler'):
        s.box('body',(-3,9,-3.7),(6,12,.5),'dark')
        s.box('body',(-2,11,-4),(4,3,.5),'trim')
    if role in ('miner','warmaster','blademaster','seamaster'):
        s.armor(heavy=True)
    if role in ('miner','warmaster','bellkeeper'):
        s.box('head',(-4.5,31,-4.5),(9,3,9),'metal' if role!='bellkeeper' else 'trim')
        if role=='miner': s.box('head',(-1,31,-5),(2,2,1),'gem')
        else: s.crest(mat='accent')
    elif role in ('pilgrim','fisher'):
        s.box('head',(-6,33,-6),(12,1,12),'wood')
        s.box('head',(-3.5,34,-3.5),(7,2,7),'cloth')
    elif role in ('archer','merchant','shadow'):
        s.box('head',(-4.6,33,-4.4),(9.2,1.2,9),'cloth')
        for x in (-4.6,4): s.box('head',(x,25,-3.8),(.7,8,8.2),'cloth')
        if role=='merchant': s.box('head',(-1,31,-4.7),(2,2,.8),'gem')
    elif role=='druid': s.horns(mat='wood',height=5,branch=True)
    else:
        s.box('head',(-4.25,31.5,-4.25),(8.5,1,8.5),'trim')
        if role in ('healer','blademaster','seamaster'): s.crown(tall=1.5)
    # Profession-specific props are attached to the torso, leaving folded-arm animation intact.
    if role in ('archer','blademaster','seamaster','staffmaster','warmaster','carpenter','miner','smith'):
        s.box('body',(4,7,2),(1,19,1),'wood')
        if role in ('miner','smith','warmaster'):
            s.box('body',(2,23,1.4),(5,2.5,2),'metal')
            if role=='warmaster': s.box('body',(1,21,1.4),(2,5,2),'metal')
        elif role=='blademaster': s.box('body',(3.5,15,1.5),(2,13,1),'metal')
        elif role=='seamaster':
            for x in (3,4.5,6): s.box('body',(x,24,2),(0.6,4,0.6),'trim')
            s.box('body',(3,24,2),(3.6,.7,.7),'trim')
        elif role=='archer':
            s.box('body',(-3,13,3.5),(3,9,2.5),'wood')
            for x in (-2.7,-1.7,-.7): s.box('body',(x,20,4),(.4,5,.4),'white')
            s.box('body',(5.2,10,2),(.5,13,.5),'trim')
        elif role=='carpenter': s.box('body',(-3,10,3.5),(4,15,1),'wood')
    if role in ('pilgrim','herbalist','fisher','merchant'):
        s.box('body',(-3.5,12,3.6),(7,9,3),'wood' if role!='merchant' else 'dark')
        s.box('body',(-3.8,20,3.4),(7.6,1,3.4),'trim')
        if role=='herbalist':
            for x in (-2,0,2): s.box('body',(x,20.5,4),(1.5,4,1.5),'leaf')
    if role in ('master','staffmaster','bellkeeper'):
        for x in (-3,-1.5,0,1.5,3): s.box('body',(x,22-abs(x)*.3,-4),(1,1,1),'wood')
        if role=='bellkeeper': s.box('body',(-2,16,-4),(4,4,2),'trim')
    if role in ('scribe','jeweler'):
        for x in (-3,1): s.box('head',(x,28,-4.3),(2,.5,.5),'metal')
        s.box('head',(-1,28,-4.3),(2,.4,.5),'metal')
        s.box('body',(-5,13,-2),(2,5,4),'accent' if role=='jeweler' else 'white')
        if role=='scribe':
            s.box('head',(3.5,32,0),(.5,5,1),'white')
            s.box('body',(-5.2,13,-2.2),(.4,5,4.4),'dark')
    if role in ('healer','weaver','fishmonger','innkeeper','child'):
        s.box('body',(-5.5,12,-1),(2,4,3),'wood')
        if role=='weaver':
            for x in (-5.2,-4.2): s.box('body',(x,16,-.6),(.8,2,1.5),'accent')
        elif role=='fishmonger':
            s.box('body',(4.2,12,-2),(1.5,5,1),'metal')
            s.box('body',(3.8,11,-2),(2.3,1,1),'accent')
        elif role=='innkeeper':
            s.box('body',(4.2,14,-2),(2,3,2),'wood')
            s.box('body',(6.2,14.5,-1.6),(.6,2,1.2),'trim')
        elif role=='child':
            for x in (-4.5,3.5): s.box('head',(x,24,1),(1.5,7,2),'hair')
            s.box('head',(-2,33,-4.5),(4,2,1),'accent')
    if role=='druid':
        for x in (-4,2): s.box('body',(x,22,-3.5),(3,2,7),'leaf')


def creature(s, sig):
    head = 'head' if 'head' in s.parts else 'body'
    body = 'body' if 'body' in s.parts else head
    if sig=='kunpeng':
        # A fish-bird silhouette replaces the cube and dangling ghast tentacles.
        for p in s.parts.values(): p['boxes']=[]
        s.box('body',(-7,8,-11),(14,11,24),'body')
        s.box('body',(-5,6,-8),(10,3,19),'white')
        s.box('body',(-5,12,-17),(10,7,8),'body')
        s.box('body',(-3,12,-22),(6,3,5),'trim')
        for x in (-5.3,4.7): s.box('body',(x,16,-15),(.6,1.5,2),'eye')
        s.box('body',(-.7,19,-1),(1.4,5,8),'trim')
        s.wings(span=15,mat='body')
        for i,p in enumerate(list(s.parts)[1:]):
            if i<4:
                # Use the real tentacle pivot to keep these short finlets attached when animated.
                t=s.parts[p].get('translate',[0,0,0]); q=[-v for v in t]
                s.box(p,(q[0]-1,q[1]-2,q[2]),(2,2,3),'trim')
        s.box('body',(-3,10,13),(6,4,8),'body')
        for x in (-7,1): s.box('body',(x,10,19),(6,2,5),'accent')
        return
    if sig in ('pixiu','baize','ancestorwolf','winterbear'):
        s.horns(head,mat='trim' if sig!='ancestorwolf' else 'wood',height=4,branch=sig in ('baize','ancestorwolf'))
    if sig in ('pixiu','pegasus'): s.wings(body,span=7 if sig=='pixiu' else 13,mat='trim' if sig=='pixiu' else 'white')
    if sig=='baize':
        x,y,z,w,h,d=s.bound(head)
        s.box(head,(x+w/2-.6,y+h*.7,z-.3),(1.2,1,.4),'eye')
        for xx in (x-.2,x+w): s.box(head,(xx,y+h*.6,z+d*.3),(.25,1,1),'eye')
        s.box(head,(x+w*.25,y-3,z),(w*.5,3,1),'white')
    if sig in ('alpha','shadowwolf','spectralwolf','pixiu','panther'):
        x,y,z,w,h,d=s.bound(head)
        for xx in (x-.7,x+w-.5): s.box(head,(xx,y-1,z+d*.5),(1.2,h*.8,2),'dark' if sig=='panther' else 'trim')
        for xx in (x+1,x+w-2): s.box(head,(xx,y-1,z-1),(.7,2,1),'white')
    if sig in ('shadowwolf','spectralwolf','wildcat','fox','spiritfox','royalbird','thunderbird','storm'):
        s.crest(head,height=2 if sig in ('wildcat','fox') else 3)
    if sig in ('queen','lich','eternal','ancestralllama','winterbear','pegasus'):
        s.crown(head,tall=4 if sig in ('queen','lich') else 2)
    if sig in ('ancient','war','colossus','warhorse','warllama','herald','automaton','stone'):
        s.armor(body,mat='body' if sig in ('ancient','stone') else 'metal',heavy=True)
    if sig in ('warhorse','warllama','herald'):
        s.armor(head,mat='metal')
        s.crest(head,mat='accent',height=2)
    if sig=='herald': s.horns(head,mat='dark',height=4)
    if sig in ('ancient','stone','colossus','icebear','winterbear'):
        # Insets follow the local frame of the original body, including the bear torso rotation.
        x,y,z,w,h,d=s.bound(body)
        for i in range(3):
            s.box(body,(x+w*.15+i*w*.27,y+h*.7,z-.8),(w*.16,h*.25,1.2),'accent' if sig not in ('ancient','stone') else 'leaf')
        s.crest(head,mat='trim',height=2 if sig=='icebear' else 4)
    if sig=='automaton':
        x,y,z,w,h,d=s.bound(head)
        s.box(head,(x-.3,y+h*.4,z-.5),(w+.6,2,1),'dark')
        s.box(head,(x+w*.2,y+h*.45,z-.8),(w*.6,1,.5),'eye')
        s.box(head,(x+w/2-.4,y+h,z+d/2),(.8,4,.8),'metal')
        s.box(head,(x+w/2-1,y+h+3,z+d/2-.5),(2,1,2),'gem')
        x,y,z,w,h,d=s.bound(body)
        s.box(body,(x+2,y+2,z+d),(w-4,h-2,3),'wood')
    if sig in ('lich','shadow','shadowarcher'):
        x,y,z,w,h,d=s.bound(head)
        s.box(head,(x-.4,y+h-.2,z-.3),(w+.8,1,d+.7),'dark')
        for xx in (x-.5,x+w): s.box(head,(xx,y,z),(.6,h,d),'dark')
        x,y,z,w,h,d=s.bound(body)
        s.box(body,(x-.5,y-3,z+d),(w+1,h+3,.7),'cloth')
        s.armor(body,mat='dark')
        if sig=='shadowarcher': s.box(body,(x+w,y,z+d),(2,10,2),'wood')
        if sig=='lich':
            for b in s.parts[head].get('boxes',[])[:1]: paint(b,'white','face')
            s.box(body,(x-.5,y-6,z-.4),(w+1,6,d+.8),'cloth')
    if sig in ('queen','shadowspider'):
        s.armor(body,mat='dark')
        x,y,z,w,h,d=s.bound(head)
        for xx in (x+1,x+w-2): s.box(head,(xx,y-2,z-1),(1,3,2),'trim')
    if sig in ('flame','storm'):
        s.crown(head,mat='accent',tall=5)
        s.horns(head,mat='dark',height=3)
    if sig in ('desert','eternal'):
        s.armor(body,mat='cloth')
        x,y,z,w,h,d=s.bound(body)
        for xx in (x-2.5,x+w): s.box(body,(xx,y+h*.15,z+d*.45),(2.5,h*.45,d*.35),'wood')
        s.box(head,tuple(s.bound(head)[:3]),(s.bound(head)[3],1,s.bound(head)[5]),'trim')
    if sig=='spiritfox' and 'tail' in s.parts:
        x,y,z,w,h,d=s.bound('tail')
        for xx in (x-w-1,x+w+1): s.box('tail',(xx,y,z),(w,h*.85,d),'white')
    if sig in ('royalbird','thunderbird'):
        if 'tail' in s.parts:
            x,y,z,w,h,d=s.bound('tail')
            for i in range(3): s.box('tail',(x+i*w/3,y-3-i*.5,z),(w/3,4,1),'accent' if i%2==0 else 'trim')
    if sig in ('elemental','spirit'):
        s.crown(head,mat='leaf' if sig=='spirit' else 'accent',tall=1.5)
        s.armor(body,mat='trim')
    if sig=='fox':
        x,y,z,w,h,d=s.bound(head)
        s.box(head,(x,y-.5,z+d*.6),(w,1.3,1.3),'trim')


def elite(s, affix):
    if affix=='blindado':
        s.armor(heavy=True)
        s.armor('head')
    elif affix=='gelido':
        s.crown(mat='accent',tall=3)
        s.crest('body',mat='trim',height=3)
    elif affix=='incendiario':
        s.crest(height=4)
        s.armor(mat='dark')
    elif affix=='venenoso':
        x,y,z,w,h,d=s.bound('body')
        for xx in (x-.8,x+w-1.2): s.box('body',(xx,y+h*.4,z+d-.5),(2,3,2),'gem')
        s.horns(mat='leaf',height=2)
    elif affix=='fantasma':
        s.crown(mat='white',tall=1)
        x,y,z,w,h,d=s.bound('body')
        for i in range(3): s.box('body',(x+w*i/3,y-2-i*.4,z+d),(w*.22,h*.8,.5),'accent')


def build(path, source):
    ident=path.stem
    group=path.parent.relative_to(BASE).as_posix()
    if ident.startswith('elite_'):
        affix=ident.rsplit('_',1)[1]
        pal={'blindado':'steel','gelido':'frost','incendiario':'ember','venenoso':'forest','fantasma':'sky'}[affix]
        sig=affix
        desc=f'Elite {affix}: placas, cristais ou adereços próprios do afixo'
    else: pal,sig,desc=DESIGNS[ident]
    data=copy.deepcopy(source)
    data['texture']=ident+'.png'
    data['textureSize']=[128,128]
    is_npc=group.startswith('vilas') or group=='personagens'
    for part in data['models']:
        pn=part['part']
        if pn in ('headwear','headwear2'):
            # Original outer skin UVs contain transparency. Repainting them opaque
            # would hide the face; our own hats/hoods are actual geometry instead.
            part['boxes']=[]
            part.pop('submodels',None)
        mat='skin' if is_npc and pn in ('head','nose') else 'body'
        if is_npc and 'leg' in pn: mat='dark'
        if pn in ('mane','tail'): mat='dark'
        for n in nodes(part):
            for i,b in enumerate(n.get('boxes',[])):
                face='face' if pn=='head' and i==0 else None
                paint(b,mat,face)
    if is_npc:
        s=Sculpt(data)
        npc(s,ident,group,sig)
        seed=int(hashlib.sha256(ident.encode()).hexdigest()[:8],16)
        tex=atlas(pal,seed,True,sig in ('master','fisher','scribe'),ident)
        pieces=s.additions
    else:
        tex,pieces=populate(data,ident,group,PALETTES[pal])
    return data,tex,{'id':ident,'grupo':group,'paleta':pal,'conceito':desc,'pecas_adicionadas':pieces,
                     'revisao_visual':1 if is_npc else 2}


def validate(data, source):
    assert data['textureSize']==[128,128]
    assert [(p['part'],p.get('translate'),p.get('rotate')) for p in data['models']]==[(p['part'],p.get('translate'),p.get('rotate')) for p in source['models']], 'Animation pivots changed'
    ids=set(); count=0
    for part in data['models']:
        for n in nodes(part):
            if 'id' in n:
                assert n['id'] not in ids, 'Duplicate part ID'
                ids.add(n['id'])
            for b in n.get('boxes',[]):
                count+=1
                c=b['coordinates']
                assert len(c)==6 and all(math.isfinite(v) for v in c)
                assert all(v>=0 for v in c[3:]) and sum(v>0 for v in c[3:])>=2
                for f in FACE_NAMES:
                    u=b['uv'+f.title()]
                    assert len(u)==4 and all(0<=v<=128 for v in u)
    assert count>0
    return count


def render(data, tex, dest, size=384):
    """Orthographic texture preview of the exported boxes, in template/rest coordinates.

    Not an EMF animation simulator. Vanilla parent transforms are not available here.
    """
    boxes=[]
    def gather(n, parent=np.eye(4), top=False):
        # Top-level template boxes use model-space coordinates. A local rotation is
        # about the declared pivot. Submodels use their own local coordinate frame.
        t=np.array(n.get('translate',[0,0,0]),float)
        r=np.eye(4)
        for axis,deg in enumerate(n.get('rotate',[0,0,0])):
            a=math.radians(deg); c,ss=math.cos(a),math.sin(a)
            rr=np.eye(4); u,v=((1,2),(2,0),(0,1))[axis]
            rr[u,u]=rr[v,v]=c; rr[u,v]=-ss; rr[v,u]=ss
            r=r@rr
        trans=np.eye(4)
        if top:
            pivot=-t
            trans[:3,3]=pivot-r[:3,:3]@pivot
            matrix=parent@trans@r
        else:
            # Template subparts invert xy independently; translation is in CEM coordinates.
            trans[:3,3]=t
            matrix=parent@trans@r
        for b in n.get('boxes',[]): boxes.append((b,matrix))
        for sub in n.get('submodels',[]): gather(sub,matrix)
    spider_preview = any(t in data.get('texture','') for t in ('spider','aranha','aracnidea'))
    for p in data['models']:
        if data.get('texture') in ('bibliotecario_amaldicoado.png','sombra_sem_nome.png') and p.get('part') in ('left_arm','right_arm'):
            # Illagers use their folded arms at rest; the engine reveals individual
            # arm roots while attacking/casting. This only affects the static preview.
            continue
        parent=np.eye(4)
        if spider_preview and p.get('part','').startswith('leg'):
            # Neutral yaw spread supplied by the spider renderer in-game. This is
            # preview-only; the JEM roots keep their vanilla animation settings.
            i=int(p['part'][3:])-1
            a=math.radians((-42,-18,18,42)[i//2]*(1 if i%2==0 else -1))
            c,ss=math.cos(a),math.sin(a)
            parent[:3,:3]=[[c,0,ss],[0,1,0],[-ss,0,c]]
            pivot=-np.array(p.get('translate',[0,0,0]),float)
            parent[:3,3]=pivot-parent[:3,:3]@pivot
        gather(p,parent,top=True)
    # Camera from front-right, looking down. Positive depth points toward the camera.
    yaw=math.radians(28); elev=math.radians(14)
    right=np.array([math.cos(yaw),0,math.sin(yaw)])
    view=np.array([math.sin(yaw)*math.cos(elev),math.sin(elev),-math.cos(yaw)*math.cos(elev)])
    up=np.cross(right,view)
    basis=np.array([right,up,view])
    allverts=[]; faces=[]
    face_idx={'north':[0,1,2,3],'south':[5,4,7,6],'west':[4,0,3,7],
              'east':[1,5,6,2],'up':[3,2,6,7],'down':[4,5,1,0]}
    for b,mat in boxes:
        x,y,z,w,h,d=b['coordinates']; expand=b.get('sizeAdd',0)
        x-=expand;y-=expand;z-=expand;w+=2*expand;h+=2*expand;d+=2*expand
        vv=np.array([[x,y,z,1],[x+w,y,z,1],[x+w,y+h,z,1],[x,y+h,z,1],
                     [x,y,z+d,1],[x+w,y,z+d,1],[x+w,y+h,z+d,1],[x,y+h,z+d,1]],float)
        world=(mat@vv.T).T[:,:3]
        proj=world@basis.T
        allverts.extend(proj)
        for f,idx in face_idx.items():
            shade={'north':.9,'south':.68,'west':.73,'east':.8,'up':1.1,'down':.55}[f]
            faces.append((proj[idx],b['uv'+f.title()],shade))
    verts=np.array(allverts)
    lo=verts[:,:2].min(0); hi=verts[:,:2].max(0)
    scale=(size-40)/max(hi-lo); mid=(lo+hi)/2
    pixels=np.zeros((size,size,3),dtype=np.uint8); pixels[:]=(24,31,42)
    zbuffer=np.full((size,size),-np.inf)
    texture=np.asarray(tex)
    for pts,uvs,shade in faces:
        xy=(pts[:,:2]-mid)*scale
        xy[:,0]+=size/2; xy[:,1]=size/2-xy[:,1]
        u0,v0,u1,v1=uvs
        uvcoords=np.array([[u0,v1-.01],[u1-.01,v1-.01],[u1-.01,v0],[u0,v0]])
        for inds in ((0,1,2),(0,2,3)):
            tri=xy[list(inds)]; depths=pts[list(inds),2]; tx=uvcoords[list(inds)]
            xmin=max(0,int(tri[:,0].min())); xmax=min(size-1,int(tri[:,0].max())+1)
            ymin=max(0,int(tri[:,1].min())); ymax=min(size-1,int(tri[:,1].max())+1)
            if xmax<xmin or ymax<ymin: continue
            yy,xx=np.mgrid[ymin:ymax+1,xmin:xmax+1]
            a,b,c=tri
            denom=(b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1])
            if abs(denom)<1e-8: continue
            wa=((b[1]-c[1])*(xx-c[0])+(c[0]-b[0])*(yy-c[1]))/denom
            wb=((c[1]-a[1])*(xx-c[0])+(a[0]-c[0])*(yy-c[1]))/denom
            wc=1-wa-wb
            depth=wa*depths[0]+wb*depths[1]+wc*depths[2]
            mask=(wa>=0)&(wb>=0)&(wc>=0)&(depth>zbuffer[ymin:ymax+1,xmin:xmax+1])
            u=(wa*tx[0,0]+wb*tx[1,0]+wc*tx[2,0]).astype(int).clip(0,127)
            v=(wa*tx[0,1]+wb*tx[1,1]+wc*tx[2,1]).astype(int).clip(0,127)
            mask &= texture[v,u,3]>0
            rgb=np.clip(texture[v,u,:3].astype(float)*shade,0,255).astype(np.uint8)
            pixels[ymin:ymax+1,xmin:xmax+1][mask]=rgb[mask]
            zbuffer[ymin:ymax+1,xmin:xmax+1][mask]=depth[mask]
    Image.fromarray(pixels).save(dest)


def main():
    # A tracked geometry snapshot makes regeneration independent of ignored local files.
    bases=json.loads((ROOT/'ferramentas/modelos_bases.json').read_text(encoding='utf-8'))
    paths=sorted(BASE / b['grupo'] / (ident+'.jem') for ident,b in bases.items())
    assert len(paths)==91, f'Expected 91 bases, found {len(paths)}'
    assert set(DESIGNS)=={p.stem for p in paths if not p.stem.startswith('elite_')}
    OUT.mkdir(parents=True,exist_ok=True); PREVIEW.mkdir(parents=True,exist_ok=True)
    manifest=[]
    for p in paths:
        source=bases[p.stem]['modelo']
        data,tex,entry=build(p,source)
        entry['cubos']=validate(data,source)
        dest=OUT/(p.stem+'.jem')
        dest.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
        tex.save(dest.with_suffix('.png'))
        render(data,tex,PREVIEW/(p.stem+'.png'))
        manifest.append(entry)
    # Portable source pair archive for an existing server; the plugin makes the resource pack.
    with zipfile.ZipFile(PREVIEW/f'Modelos_RPGAtributos_{len(paths)}.zip','w',zipfile.ZIP_DEFLATED) as z:
        for p in sorted(OUT.iterdir()):
            if p.suffix in ('.jem','.png'): z.write(p,p.name)
    report={'modelos':len(manifest),'validacao':'JSON, pares PNG, dimensões, UV, IDs e pivôs preservados',
            'jogo':'Ainda requer validação no Minecraft com EMF/ETF', 'itens':manifest}
    (PREVIEW/'validacao.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
    # Small contact sheets, readable without a browser or a 3D editor.
    try: font=ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',15)
    except OSError: font=ImageFont.load_default()
    for page in range(math.ceil(len(manifest)/12)):
        chunk=manifest[page*12:(page+1)*12]
        sheet=Image.new('RGB',(1000,math.ceil(len(chunk)/4)*280),(18,24,33)); d=ImageDraw.Draw(sheet)
        for i,e in enumerate(chunk):
            x=(i%4)*250; y=(i//4)*280
            im=Image.open(PREVIEW/(e['id']+'.png')).convert('RGB').resize((246,246))
            sheet.paste(im,(x+2,y))
            label=e['id'].replace('_',' ')
            if len(label)>29:
                cut=label.rfind(' ',0,29); label=label[:cut]+'\n'+label[cut+1:]
            d.text((x+7,y+247),label,font=font,fill=(221,228,237),spacing=1)
        sheet.save(PREVIEW/f'catalogo-{page+1:02}.png')
    print(json.dumps({'modelos':len(manifest),'cubos':sum(e['cubos'] for e in manifest),'saida':str(OUT),'previas':str(PREVIEW)},ensure_ascii=False))

if __name__=='__main__': main()
