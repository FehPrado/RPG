"""Second art pass: articulated creature anatomy, authored in model-space pixels.

Keeps the original EMF roots/pivots, replaces their geometry and paints new UV tiles.
No changes to the villagers or to the five existing Guardians.
"""
import copy
import hashlib
import math

import numpy as np
from PIL import Image, ImageDraw

MATS = ['fur','light','dark','gold','metal','eye','bone','cloth',
        'stone','moss','crystal','wood','feather','scale','leather','black',
        'ember','rune','furstripe','featherlight','gem','red','ivory','shadow']

def rotation(angles):
    r=np.eye(3)
    for axis,deg in enumerate(angles):
        a=math.radians(deg); c,s=math.cos(a),math.sin(a)
        q=np.eye(3); u,v=((1,2),(2,0),(0,1))[axis]
        q[u,u]=q[v,v]=c;q[u,v]=-s;q[v,u]=s
        r=r@q
    return r

def euler(r):
    y=math.asin(np.clip(r[0,2],-1,1))
    if abs(math.cos(y))>1e-7:
        x=math.atan2(-r[1,2],r[2,2]);z=math.atan2(-r[0,1],r[0,0])
    else: x=math.atan2(r[2,1],r[1,1]);z=0
    return [round(math.degrees(a),5) for a in (x,y,z)]

def tile_uv(mat):
    i=MATS.index(mat);x=i%8*16;y=i//8*16
    return [x,y,x+16,y+16]

def texture(ident,palette):
    seed=int(hashlib.sha256(ident.encode()).hexdigest()[:8],16)
    rng=np.random.default_rng(seed)
    base,trim,glow,dark=[tuple(bytes.fromhex(x)) for x in palette]
    colors=[base,tuple(min(255,int(c*.45+130)) for c in base),dark,trim,(108,131,147),glow,
            (224,213,181),dark,(102,114,110),(72,112,67),glow,(99,65,42),base,base,
            (99,61,39),(16,22,30),(242,119,35),dark,base,trim,glow,(139,43,47),(230,227,208),dark]
    if ident in ('baize','corcel_celeste','raposa_espiritual','urso_glacial','urso_do_inverno_eterno'):
        colors[0]=(201,214,217);colors[1]=(234,238,224)
    if ident in ('pantera_sombria','lobo_das_sombras'):
        colors[0]=(38,44,62);colors[1]=(73,82,107)
    if ident=='pixiu': colors[0]=(171,111,45);colors[1]=(222,174,80)
    if ident=='raposa_astuta': colors[0]=(173,76,38);colors[1]=(230,201,159)
    if ident.startswith('sombra_') or ident=='arauto_do_fim':
        colors[2]=(54,49,74);colors[7]=(67,57,90);colors[23]=(54,49,74)
    img=Image.new('RGBA',(128,128))
    for idx,c in enumerate(colors):
        px=np.zeros((16,16,4),np.uint8)
        noise=rng.integers(-5,6,(16,16))
        for k in range(3):px[:,:,k]=np.clip(c[k]+noise,0,255)
        px[:,:,3]=255
        im=Image.fromarray(px);d=ImageDraw.Draw(im)
        hi=tuple(min(255,v+27) for v in c);lo=tuple(max(0,v-28) for v in c)
        if idx in (0,1,18):
            for y in range(1,16,4):
                for x in range((y//4)%2*2,16,5):
                    d.line((x,y,x+1,y+2),fill=hi)
                    d.point((x+2,y+2),fill=lo)
            if idx==18:
                for y in (3,10):d.line((0,y,6,y+2,11,y,15,y+3),fill=dark,width=2)
        if idx in (3,4):
            d.rectangle((0,0,15,15),outline=lo)
            d.line((1,1,14,1),fill=hi)
            d.line((1,1,1,14),fill=hi)
            for x,y in ((2,2),(13,2),(2,13),(13,13)):d.rectangle((x,y,x+1,y+1),fill=lo)
        if idx==8:
            d.line((0,5,5,6,8,3,12,5,15,4),fill=lo)
            d.line((6,6,5,12,8,15),fill=lo)
        if idx==9:
            for _ in range(25):
                x,y=rng.integers(0,15,2);d.rectangle((x,y,x+1,y+1),fill=hi)
        if idx in (5,10,20):
            d.polygon([(0,15),(0,4),(8,0),(15,4),(15,15)],fill=lo)
            d.polygon([(2,13),(2,5),(8,1),(11,5),(9,13)],fill=c)
            d.line((3,7,7,3,10,5),fill=(233,252,242),width=2)
        if idx in (12,19):
            d.line((8,0,8,15),fill=hi)
            for y in range(2,16,3):
                d.line((0,y-2,7,y,8,y+2),fill=lo)
                d.line((15,y-2,9,y),fill=lo)
        if idx==13:
            for y in range(0,16,5):
                for x in range(-3+(y//5)%2*3,16,6):
                    d.line((x,y,x+3,y+3,x+6,y),fill=lo)
                    d.point((x+3,y+1),fill=hi)
        if idx in (7,14):
            d.line((1,0,1,15),fill=hi);d.line((14,0,14,15),fill=hi)
            for y in range(1,16,3):d.point((2,y),fill=trim)
        if idx==16:
            d.line((3,15,6,10,4,7,10,4,9,0),fill=(255,207,79),width=2)
        if idx==17:
            d.line((8,2,3,7,8,12,12,7,8,2),fill=glow,width=1)
            d.line((8,0,8,15),fill=glow)
        img.paste(im,(idx%8*16,idx//8*16))
    return img

class Mesh:
    def __init__(self,data):
        self.data=data;self.parts={p['part']:p for p in data['models']}
        self.original=copy.deepcopy(self.parts);self.count=0
        for p in self.parts.values():
            p.pop('boxes',None);p.pop('submodels',None);p.pop('sprites',None)

    def box(self,part,center,size,mat='fur',angles=(0,0,0)):
        assert part in self.parts,part
        assert all(v>0 for v in size),size
        p=self.parts[part];pr=rotation(p.get('rotate',(0,0,0)))
        pivot=-np.array(p.get('translate',(0,0,0)),float)
        pos=pivot+pr.T@(np.array(center)-pivot)
        rot=pr.T@rotation(angles)
        coords=[round(-v/2,4) for v in size]+[round(v,4) for v in size]
        box={'coordinates':coords}
        for face in ('North','South','West','East','Up','Down'):box['uv'+face]=tile_uv(mat)
        self.count+=1
        node={'id':f'rpg_detail_{self.count}','invertAxis':'xy','translate':[round(v,4) for v in pos],
              'rotate':euler(rot),'boxes':[box]}
        p.setdefault('submodels',[]).append(node)

    def beam(self,part,a,b,width,mat='fur',depth=None):
        a=np.array(a,float);b=np.array(b,float);vec=b-a;length=np.linalg.norm(vec)
        if length<.01:return
        direction=vec/length
        x=math.atan2(direction[2],math.hypot(direction[0],direction[1]))
        z=-math.atan2(direction[0],direction[1])
        r=rotation((0,0,math.degrees(z)))@rotation((math.degrees(x),0,0))
        self.box(part,(a+b)/2,(width,length,depth or width),mat,euler(r))

    def mass(self,part,c,size,mat='fur',light=None):
        x,y,z=c;w,h,d=size
        self.box(part,c,(w*.78,h,d*.78),mat)
        self.box(part,c,(w,h*.7,d*.78),mat)
        self.box(part,c,(w*.78,h*.7,d),light or mat)

    def horn(self,part,points,width=1.5,mat='bone'):
        for i,(a,b) in enumerate(zip(points,points[1:])):
            self.beam(part,a,b,max(.2,width*(1-i/len(points))),mat)

    def bound(self,part):
        p=self.original[part];r=rotation(p.get('rotate',(0,0,0)))
        pivot=-np.array(p.get('translate',(0,0,0)),float)
        boxes=p.get('boxes',[])
        if not boxes:return pivot,np.array((3,3,3))
        x,y,z,w,h,d=boxes[0]['coordinates']
        points=np.array([[a,b,c] for a in (x,x+w) for b in (y,y+h) for c in (z,z+d)])
        points=(points-pivot)@r.T+pivot
        lo=points.min(0);hi=points.max(0)
        return (lo+hi)/2,hi-lo

    def eyes(self,part,c,w,mat='eye'):
        x,y,z=c
        for side in (-1,1):
            self.box(part,(x+side*w*.27,y,z),(max(.5,w*.2),max(.4,w*.13),.35),'black')
            self.box(part,(x+side*w*.27,y+.04,z-.2),(max(.25,w*.1),max(.25,w*.08),.16),mat)
            self.box(part,(x+side*w*.27,y+max(.4,w*.13),z+.02),(max(.7,w*.3),.3,.5),'dark',(0,0,-side*12))

    def feather(self,part,a,b,width,mat='feather'):
        a=np.array(a,float);b=np.array(b,float)
        self.beam(part,a,b,width,mat,.45)
        self.beam(part,a*.2+b*.8,b+(b-a)*.12,width*.55,'featherlight',.35)


def animal(m,ident,kind):
    h,hs=m.bound('head'); b,bs=m.bound('body')
    hx,hy,hz=h;hw,hh,hd=hs
    # Rounded rib cage, shoulder mass, haunches, underbelly and independent jaw.
    m.mass('body',b,bs,'fur')
    m.mass('body',(b[0],b[1]+bs[1]*.08,b[2]-bs[2]*.28),(bs[0]*1.1,bs[1]*.9,bs[2]*.4),'fur')
    m.box('body',(b[0],b[1]-bs[1]*.3,b[2]),(bs[0]*.65,bs[1]*.25,bs[2]*.75),'light')
    m.mass('head',h,(hw,hh,hd),'fur')
    muzzle=(hw*.65,hh*.4,hd*.6)
    mz=hz-hd*.65
    m.mass('head',(hx,hy-hh*.2,mz),muzzle,'light')
    m.box('head',(hx,hy-hh*.07,mz-muzzle[2]*.5),(hw*.35,.6,.6),'black')
    m.box('head',(hx,hy-hh*.4,mz),(hw*.5,.35,muzzle[2]*.9),'dark')
    if kind not in ('horse','camel','llama'):
        m.eyes('head',(hx,hy+hh*.12,hz-hd*.5-.12),hw)
    for side in (-1,1):
        # Triangular stepped ears, not a head crown.
        ex=hx+side*hw*.34
        m.box('head',(ex,hy+hh*.56,hz+hd*.12),(hw*.25,hh*.38,hd*.28),'dark',(0,0,-side*12))
        m.box('head',(ex,hy+hh*.76,hz+hd*.12),(hw*.12,hh*.2,hd*.2),'fur')
        m.box('head',(ex,hy+hh*.57,hz-hd*.04),(hw*.12,hh*.2,.2),'light')
    # Preserve every original limb root; new paws and hocks follow its vanilla motion.
    for part in m.parts:
        if 'leg' not in part:continue
        c,sz=m.bound(part);x,y,z=c;w,height,d=sz
        if height<1:continue
        w=max(w,1);d=max(d,1)
        top=y+height/2
        m.mass(part,(x,top-height*.24,z),(w*1.2,height*.48,d*.95),'fur')
        m.beam(part,(x,top-height*.4,z),(x,y-height*.37,z+.45),w*.7,'fur')
        m.mass(part,(x,y-height*.43,z-d*.15),(w*1.2,height*.15+1,d*1.15),'dark' if kind in ('horse','camel','llama') else 'light')
        if kind not in ('horse','camel','llama'):
            for dx in (-.27,0,.27):m.box(part,(x+w*dx,y-height*.47,z-d*.68),(w*.15,.35,.6),'bone')
    if kind in ('horse','camel','llama'):
        # Neck is carried by the head root, with horse-specific neck/mouth parts retained.
        root='neck' if 'neck' in m.parts else 'head'
        shoulder=(b[0],b[1]+bs[1]*.3,b[2]-bs[2]*.37)
        m.beam(root,shoulder,(hx,hy-hh*.3,hz+hd*.3),hw*.83,'fur',hw*.9)
        for side in (-1,1):
            m.box('head',(hx+side*hw*.46,hy+hh*.12,hz-hd*.1),(.35,1,1.5),'black')
            m.box('head',(hx+side*(hw*.5+.08),hy+hh*.14,hz-hd*.2),(.2,.55,.7),'eye')
        if kind=='horse':
            m.mass('head',(hx,hy-hh*.35,hz-hd*.9),(hw*.64,hh*.52,hd*.65),'fur')
            for side in (-1,1):m.box('head',(hx+side*hw*.26,hy-hh*.26,hz-hd*1.2),(.4,.6,.3),'black')
        if kind=='camel' and 'hump' in m.parts:
            c,sz=m.bound('hump');m.mass('hump',c,sz,'fur')
        if kind=='horse':
            root='mane' if 'mane' in m.parts else 'neck'
            for i in range(6):m.box(root,(hx,hy+1-i*2,hz+hd*.6+i*.3),(1.3,2.4,2),'dark',(15,0,0))
    # A connected tapered tail replaces the rectangular vanilla plume.
    if 'tail' in m.parts:
        c,sz=m.bound('tail');root=-np.array(m.original['tail'].get('translate',(0,0,0)),float)
        if kind in ('cat','fox'):
            start=(b[0],b[1]+bs[1]*.2,b[2]+bs[2]*.45)
            end=(start[0],start[1]+2,start[2]+max(6,bs[2]*.7))
            for offset in ((-3,0,0),(0,1,1),(3,0,0)) if ident=='raposa_espiritual' else ((0,0,0),):
                a=np.array(start);tip=np.array(end)+offset
                m.horn('tail',[a,(a+tip)/2,tip],3.2 if kind=='fox' else 1,'light' if ident=='raposa_espiritual' else 'fur')
                m.beam('tail',(a+tip)/2,tip,2.6 if kind=='fox' else .65,'light' if kind=='fox' else 'dark')
        else:
            start=(b[0],b[1]+bs[1]*.15,b[2]+bs[2]*.4)
            end=(start[0],max(2,start[1]-5),start[2]+4)
            m.horn('tail',[start,(start[0],start[1],start[2]+2),end],2 if kind!='horse' else 2.5,'dark')
    if kind=='wolf':
        root='mane' if 'mane' in m.parts else 'body'
        for side in (-1,1):
            for i in range(4):
                m.beam(root,(hx+side*2.4,hy+1-i*.7,hz+hd*.5+i*.6),
                       (hx+side*(3.8-i*.25),hy-1-i*.8,hz+hd*.7+i*.6),1.5,'light' if ident=='lobo_alfa' else 'dark')
        for side in (-1,1):m.horn('head',[(hx+side*hw*.25,hy-hh*.25,mz-.2),(hx+side*hw*.25,hy-hh*.55,mz-.5)],.6)
    if kind=='bear':
        # Broad hump, neck and chunky paws distinguish a bear from a scaled wolf.
        m.mass('body',(0,b[1]+bs[1]*.25,b[2]-bs[2]*.3),(bs[0]*1.05,bs[1]*.7,bs[2]*.55),'light')
        m.beam('head',(hx,hy,hz+hd*.35),(b[0],b[1],b[2]-bs[2]*.45),hw*.9,'fur')
    # Species/evolution-specific signatures.
    if ident in ('gato_selvagem','pantera_sombria'):
        for side in (-1,1):
            for i in range(5):m.box('body',(b[0]+side*bs[0]*.4,b[1]+(.5 if i%2 else -.5),b[2]-bs[2]*.35+i*bs[2]*.17),(.25,1.4,1),'dark' if ident=='gato_selvagem' else 'rune')
        if ident=='pantera_sombria':
            for side in (-1,1):m.horn('head',[(hx+side*hw*.22,hy-hh*.2,mz),(hx+side*hw*.22,hy-hh*.65,mz-.3)],.55,'bone')
    if ident in ('lobo_das_sombras','lobo_espectral'):
        for side in (-1,1):
            for i in range(3):m.horn('body',[(side*bs[0]*.35,b[1]+bs[1]*.25,b[2]+i*2),(side*(bs[0]*.5+1),b[1]+bs[1]*.5+1,b[2]+i*2+2)],.7,'crystal')
    if ident in ('lobo_ancestral','baize'):
        for side in (-1,1):
            start=(hx+side*hw*.3,hy+hh*.45,hz)
            tip=(hx+side*hw*.7,hy+hh*.5+5,hz+2)
            m.horn('head',[start,(start[0]+side,tip[1]-2,hz+1),tip],1.2,'bone' if ident=='baize' else 'wood')
            m.horn('head',[(start[0]+side,tip[1]-3,hz+1),(start[0]+side*3,tip[1]-1,hz)],.7,'bone')
    if ident in ('urso_glacial','urso_do_inverno_eterno'):
        for i in range(4):
            m.horn('body',[(0,b[1]+bs[1]*.4,b[2]-bs[2]*.3+i*bs[2]*.2),(0,b[1]+bs[1]*.6+(3 if ident.endswith('eterno') else 1),b[2]-bs[2]*.3+i*bs[2]*.2+1)],1.8,'crystal')
        if ident=='urso_do_inverno_eterno':
            for side in (-1,1):
                for i in range(4):
                    a=(side*bs[0]*.38,b[1]+bs[1]*.3-i,b[2]-bs[2]*.35)
                    m.horn('body',[a,(a[0]+side*2,a[1]+2,a[2]-1),(a[0]+side*3,a[1]+3,a[2]-2)],1.5,'crystal')
            m.box('head',(hx,hy+hh*.3,hz-hd*.5-.25),(1.2,2.2,.3),'rune')
    if ident in ('corcel_de_guerra','lhama_guerreira'):
        for side in (-1,1):
            for i in range(4):m.box('body',(side*bs[0]*.47,b[1],b[2]-bs[2]*.25+i*bs[2]*.18),(.8,bs[1]*.65,bs[2]*.18),'metal')
        m.box('head',(hx,hy+hh*.3,hz-hd*.4),(hw*.7,hh*.7,.65),'metal')
        m.box('head',(hx,hy+hh*.62,hz),(1,hh*.4,hd*.8),'red')
    if kind in ('camel','llama'):
        for side in (-1,1):
            m.box('body',(side*bs[0]*.5,b[1]-bs[1]*.15,b[2]),(.4,bs[1]*.75,bs[2]*.6),'cloth')
            for i in range(5):m.box('body',(side*bs[0]*.52,b[1]-bs[1]*.52,b[2]-bs[2]*.25+i*bs[2]*.12),(.5,1.7,.6),'gold')
            if kind=='camel':m.mass('body',(side*(bs[0]*.5+1.1),b[1]-1,b[2]+3),(2.5,5,5),'leather')
    if ident=='lhama_ancestral':
        for side in (-1,1):m.box('head',(hx+side*hw*.5,hy-hh*.1,hz),(.6,2,.6),'gem')
    if ident=='camelo_das_areias_eternas':
        m.box('head',(hx,hy+hh*.4,hz-hd*.5),(hw*.8,.6,.5),'gold')
        m.mass('head',(hx,hy+hh*.25,hz-hd*.5-.4),(1.2,1.5,.6),'gem')
        for side in (-1,1):
            for i in range(3):
                m.box('body',(side*bs[0]*.52,b[1]-3,b[2]-6+i*5),(.5,5,3),'rune')
                m.mass('body',(side*bs[0]*.55,b[1]-6,b[2]-6+i*5),(1,1,1),'gem')
    if ident=='corcel_celeste':
        for side in (-1,1):
            shoulder=(side*bs[0]*.35,b[1]+bs[1]*.3,b[2]-bs[2]*.2)
            elbow=(side*9,b[1]+bs[1]*.7,b[2])
            tip=(side*16,b[1]+bs[1]*.5,b[2]+4)
            m.beam('body',shoulder,elbow,1.6,'ivory');m.beam('body',elbow,tip,1,'ivory')
            for i in range(9):
                t=i/8;a=np.array(shoulder)*(1-t)+np.array(tip)*t
                m.feather('body',a,a+np.array((side*(1+t*2),-2-t*2,6-t*2)),1.5,'ivory')
    if ident=='baize':
        m.box('head',(hx,hy+hh*.3,hz-hd*.5-.3),(1,1,.3),'eye')
        for i in range(3):m.horn('head',[(hx+(i-1)*.8,hy-hh*.4,mz),(hx+(i-1)*.4,hy-hh*.4-4,mz+1)],.8,'ivory')
        for side in (-1,1):
            for i in range(5):
                m.feather('neck',(hx+side*hw*.37,hy-2-i*1.7,hz+3),
                          (hx+side*(hw*.65+.5),hy-4-i*1.7,hz+4),1.4,'ivory')
    if ident=='pixiu':
        for side in (-1,1):
            for i in range(5):
                m.mass('head',(hx+side*(hw*.4+1),hy+hh*.3-i*1.1,hz+1),(2.5,2.3,2.5),'gold')
        m.horn('head',[(hx,hy+hh*.4,hz),(hx,hy+hh*.4+3,hz-1),(hx,hy+hh*.4+4,hz-2)],1.4,'gold')
        for side in (-1,1):
            for i in range(5):m.feather('body',(side*bs[0]*.35,b[1]+2,b[2]-1+i*.6),(side*(6+i*.7),b[1]+4-i*.5,b[2]+i*1.2),1.2,'scale')


def spider(m,ident):
    # Two tapered carapace sections, small cephalothorax, eight eyes and mandibles.
    m.mass('body',(0,9,10),(11,9,14),'scale')
    m.mass('body',(0,10,14),(8,7,7),'dark')
    for z in (5,8,11,14):m.box('body',(0,13.1,z),(7,.6,1),'gold' if ident=='rainha_aracnidea' else 'rune')
    m.mass('neck',(0,8,0),(7,6,7),'dark')
    m.mass('head',(0,8,-6),(8,5,7),'scale')
    for x,y in ((-2.5,8.5),(-1,9.7),(1,9.7),(2.5,8.5),(-3,7),(3,7)):
        m.box('head',(x,y,-9.6),(.8,.8,.4),'eye')
    for side in (-1,1):
        m.horn('head',[(side*2,6,-8),(side*3,4,-10),(side*1.5,3,-11)],1.3,'bone')
    for i in range(1,9):
        part='leg'+str(i);p=-np.array(m.original[part].get('translate',(0,0,0)))
        side=1 if p[0]>0 else -1
        # The engine supplies yaw/spread. Bent knees and terminal claws are modeled here.
        knee=p+np.array((side*7,2,0));tip=p+np.array((side*12,-7,0))
        m.beam(part,p,knee,1.5,'scale');m.mass(part,knee,(2,2,2),'gold' if ident=='rainha_aracnidea' else 'dark')
        m.beam(part,knee,tip,.9,'dark');m.horn(part,[tip,tip+np.array((-side*.7,-1,0))],.5,'bone')
    if ident=='rainha_aracnidea':
        for side in (-1,1):m.horn('head',[(side*2.5,10,-5),(side*3,14,-4),(side*1.7,16,-5)],1.1,'gold')


def humanoid(m,ident,golem=False):
    h,hs=m.bound('head');b,bs=m.bound('body');hx,hy,hz=h
    ancient=ident in ('golem_ancestral','golem_de_pedra')
    automaton=ident=='automato'
    skeleton='skeleton' in ident
    zombie='zombie' in ident
    primary='stone' if ancient else 'metal' if golem else 'bone' if skeleton else 'fur' if zombie else 'dark'
    # Chest and waist are separate, with a visible shoulder silhouette.
    m.mass('body',b,(bs[0],bs[1]*.9,bs[2]),primary)
    m.mass('body',(b[0],b[1]-bs[1]*.6,b[2]),(bs[0]*.65,bs[1]*.35,bs[2]*.75),primary)
    for side in (-1,1):
        for i in range(3):
            m.box('body',(side*bs[0]*.27,b[1]+bs[1]*.22-i*bs[1]*.22,b[2]-bs[2]*.5-.3),(bs[0]*.35,1.3,.8),'stone' if ancient else 'bone' if skeleton else 'cloth' if zombie else 'metal',(0,0,side*12))
    if not (skeleton or zombie):
        m.mass('body',(b[0],b[1]+.3,b[2]-bs[2]*.56),(bs[0]*.22,bs[1]*.3,1),'gem')
    m.mass('head',h,(hs[0]*.9,hs[1]*.85,hs[2]*.85),'bone' if ident=='lich' else primary)
    m.eyes('head',(hx,hy+hs[1]*.12,hz-hs[2]*.45-.1),hs[0])
    if not golem and not zombie:
        m.box('head',(hx,hy-hs[1]*.28,hz-hs[2]*.45),(hs[0]*.55,.8,.6),'bone')
        for i in range(4):m.box('head',(hx-1.5+i,hy-hs[1]*.3,hz-hs[2]*.5),(.45,.8,.35),'dark')
    for part in m.parts:
        if not any(k in part for k in ('arm','leg')):continue
        c,sz=m.bound(part);w,hgt,d=sz
        if hgt<2:continue
        is_arm='arm' in part
        m.mass(part,(c[0],c[1]+hgt*.24,c[2]),(w*1.25,hgt*.48,d*1.15),primary)
        m.mass(part,(c[0],c[1]-hgt*.05,c[2]),(w*.85,hgt*.16,d*.85),'gold' if automaton else 'dark')
        m.mass(part,(c[0],c[1]-hgt*.29,c[2]),(w*1.1,hgt*.35,d),'stone' if ancient else primary)
        if is_arm:
            m.mass(part,(c[0],c[1]-hgt*.48,c[2]-.4),(w*1.25,hgt*.16,d*1.1),primary)
            for i in range(3):m.box(part,(c[0]-w*.3+i*w*.3,c[1]-hgt*.5,c[2]-d*.55),(w*.2,hgt*.12,.7),'bone' if not golem else 'metal')
            m.mass(part,(c[0],c[1]+hgt*.43,c[2]),(w*1.7,hgt*.18,d*1.5),'moss' if ident=='golem_ancestral' else 'stone' if ancient else 'bone' if skeleton else 'cloth' if zombie else 'metal')
        else:m.mass(part,(c[0],c[1]-hgt*.47,c[2]-.7),(w*1.2,hgt*.17,d*1.5),primary)
    if ident=='golem_ancestral':
        for side in (-1,1):
            for i in range(3):m.mass('body',(side*bs[0]*.42,b[1]+bs[1]*.37-i*1.7,b[2]+bs[2]*.3),(3,2,3),'moss')
        m.horn('head',[(hx,hy+hs[1]*.3,hz+2),(hx+1,hy+hs[1]*.7,hz+2),(hx+3,hy+hs[1]*.9,hz+1)],1,'wood')
    if ident=='golem_de_pedra':
        for side in (-1,1):m.mass('body',(side*bs[0]*.4,b[1]+bs[1]*.4,b[2]+1),(4,4,5),'stone')
    if ident in ('lich','arauto_do_fim','sombra_arqueiro','sombra_morto_vivo'):
        # Torn cloak stays with torso; thin strips allow the silhouette of moving legs.
        for i in range(5):
            x=(i-2)*bs[0]/5
            m.box('body',(x,b[1]-bs[1]*.35-i%2,b[2]+bs[2]*.6),(bs[0]/5+.1,bs[1]*1.7+(i%2)*2,.6),'cloth',(8,0,0))
        if ident=='lich':
            for side in (-1,1):m.box('head',(hx+side*hs[0]*.51,hy,hz),(.8,hs[1],hs[2]),'cloth')
            for i in range(5):
                x=hx+(i-2)*1.5;m.horn('head',[(x,hy+hs[1]*.42,hz-hs[2]*.3),(x,hy+hs[1]*.6+(2 if i==2 else 0),hz-hs[2]*.4)],.7,'gold')
            m.box('head',(hx,hy+hs[1]*.51,hz),(hs[0]*1.2,1,hs[2]*1.1),'cloth')
            # Staff follows the arm, rather than remaining fixed to the torso.
            arm='right_arm';a,asz=m.bound(arm);sx=a[0]+2.5;sz=a[2]-2
            m.beam(arm,(sx,1,sz),(sx,30,sz),.8,'wood')
            for side in (-1,1):m.horn(arm,[(sx,26,sz),(sx+side*2,29,sz),(sx+side,32,sz)],.7,'gold')
            m.mass(arm,(sx,29,sz),(1.8,2.5,1.8),'gem')
            for i in range(5):m.box('body',((i-2)*1.6,7+(i%2)*.6,0),(1.7,10,4.5),'cloth',(0,0,(i-2)*2))
        if ident=='arauto_do_fim':
            for side in (-1,1):m.horn('head',[(hx+side*3,hy+3,hz),(hx+side*5,hy+6,hz+1),(hx+side*4,hy+8,hz-1)],1.3,'bone')
        if ident=='sombra_arqueiro':
            m.box('body',(b[0]+3,b[1],b[2]+3),(2,9,2),'leather',(0,0,-15))
            for x in (2.7,3.3,3.8):m.beam('body',(x,b[1]+2,b[2]+3),(x,b[1]+7,b[2]+3),.2,'bone')
    if automaton:
        # Brass boiler, chimney, riveted visor and copper gear teeth.
        m.mass('body',(b[0],b[1],b[2]+bs[2]*.65),(bs[0]*.7,bs[1],4),'gold')
        m.box('body',(b[0]+bs[0]*.3,b[1]+bs[1]*.6,b[2]+bs[2]*.55),(2,5,2),'dark')
        for i in range(8):
            a=i*math.pi/4;m.box('body',(b[0]+math.cos(a)*3,b[1]+math.sin(a)*3,b[2]-bs[2]*.6),(1.2,1.2,.6),'gold',(0,0,math.degrees(a)))
    if ident=='colosso_de_ferro':
        for side in (-1,1):m.box('body',(side*bs[0]*.43,b[1]+bs[1]*.5,b[2]),(5,6,bs[2]+2),'metal',(0,0,-side*15))


def bird(m,ident):
    thunder=ident=='ave_do_trovao'
    m.mass('body',(0,5,-.5),(3.7,6,3.6),'feather')
    m.mass('head',(0,9,-1),(3.8,4,3.8),'featherlight' if thunder else 'feather')
    m.box('head',(0,8.5,-3.2),(1.5,1.5,1.5),'gold')
    m.box('head',(0,7.8,-3.7),(.8,1.4,.8),'black')
    for side in (-1,1):
        m.box('head',(side*1.8,9.4,-1.7),(.25,1.1,1.2),'ivory')
        m.box('head',(side*1.98,9.4,-1.8),(.2,.5,.5),'black')
        part='left_wing' if side<0 else 'right_wing'
        for i in range(6):
            m.feather(part,(side*1.5,6.5,-.2+i*.3),(side*(3+i*.35),2-i*.4,1+i*.5),.85,'featherlight' if thunder and i%2 else 'feather')
        part='left_leg' if side<0 else 'right_leg'
        m.beam(part,(side*.8,2,0),(side*.8,.5,-.2),.55,'gold')
        for i in (-1,0,1):m.beam(part,(side*.8,.5,-.2),(side*.8+i*.4,.1,-1.2),.23,'black')
    for i in range(5):m.feather('tail',((i-2)*.5,3,1),((i-2)*.7,-2-abs(i-2)*.5,3),.8,'featherlight' if i%2 else 'feather')
    for i in range(4):m.feather('head',(0,10.5,-.5+i*.4),(0,13+i*.25,1+i*.6),.8,'gold' if thunder else 'red')
    if thunder:
        for side in (-1,1):m.horn('head',[(side*.7,10.3,0),(side*2,12,1),(side*.9,12.3,1),(side*2.4,14,2)],.45,'gold')


def kunpeng(m):
    m.mass('body',(0,14,0),(15,10,25),'scale')
    m.mass('body',(0,12,-7),(12,7,15),'light')
    m.mass('body',(0,15,-13),(10,7,11),'feather')
    m.mass('body',(0,14,-19),(7,3,7),'gold')
    m.box('body',(0,12.8,-19),(5,.5,6),'dark')
    for side in (-1,1):
        m.box('body',(side*4.6,16,-14),(.35,1.3,2),'eye')
        m.horn('body',[(side*4,17,-12),(side*5,20,-10),(side*7,21,-6)],1,'featherlight')
        # Broad bird wings carry individually tapered flight feathers.
        shoulder=(side*5,16,-3);elbow=(side*13,18,0);tip=(side*23,17,6)
        m.beam('body',shoulder,elbow,2.4,'scale');m.beam('body',elbow,tip,1.7,'scale')
        for i in range(11):
            t=i/10;a=np.array(shoulder)*(1-t)+np.array(tip)*t
            m.feather('body',a,a+np.array((side*(2+t*3),-1-t*2,8-t*2)),2,'feather' if i%2 else 'featherlight')
        m.feather('body',(side*2,13,11),(side*9,14,21),3,'scale')
    for i in range(5):m.feather('body',(0,18,-3+i*3),(0,21,-1+i*3),1.3,'featherlight')
    # Tentacles are replaced by short animated underside finlets attached at their pivots.
    for i,part in enumerate(p for p in m.parts if p.startswith('tentacle')):
        if i>=4:continue
        c=-np.array(m.original[part]['translate'],float)
        m.feather(part,c,c+np.array(((-1 if i%2 else 1)*2,-2,2)),1,'scale')


def elemental(m,ident):
    life=ident=='espirito'
    m.mass('head',(0,8,0),(4,4,3),'wood' if life else 'dark')
    m.eyes('head',(0,8,-1.6),4)
    m.mass('body',(0,4,0),(2.8,3.5,2),'moss' if life else 'crystal')
    for side in (-1,1):
        arm='left_arm' if side<0 else 'right_arm'
        m.horn(arm,[(side*1.5,5,0),(side*2.2,3,-.2),(side*2.7,2,-1)],.6,'wood' if life else 'metal')
        wing='left_wing' if side<0 else 'right_wing'
        for i in range(4):m.feather(wing,(side*.8,5,1),(side*(4+i*.65),7-i*1.6,2+i*.3),1.5,'moss' if life else 'crystal')
        m.horn('head',[(side*1.3,9,0),(side*2,11,0),(side*3,11.6,1)],.6,'wood' if life else 'crystal')
    for i in range(3):m.horn('body',[((i-1)*.7,3,0),((i-1)*.9,1,1),((i-1)*.6,-1,2)],.5,'light' if life else 'crystal')


def floating_boss(m,ident):
    flame=ident=='senhor_das_chamas'
    head='head';c,sz=m.bound(head);x,y,z=c
    m.mass(head,c,(8,7,6),'dark' if flame else 'metal')
    m.eyes(head,(x,y+.3,z-3.1),8)
    m.box(head,(x,y-1.5,z-3.1),(3,1,.5),'ember' if flame else 'crystal')
    for side in (-1,1):m.horn(head,[(x+side*3,y+2,z),(x+side*5,y+5,z+1),(x+side*4,y+7,z+2)],1.5,'gold' if flame else 'crystal')
    if flame:
        for part in m.parts:
            if not part.startswith('stick'):continue
            cc,ss=m.bound(part)
            m.mass(part,cc,(2.4,ss[1]*.8,2.4),'dark')
            m.box(part,(cc[0],cc[1],cc[2]-1.25),(1.2,ss[1]*.65,.4),'ember')
            m.horn(part,[(cc[0],cc[1]+ss[1]*.3,cc[2]),(cc[0]+.5,cc[1]+ss[1]*.6,cc[2]),(cc[0],cc[1]+ss[1]*.8,cc[2])],1.2,'ember')
    else:
        # Breeze body root carries a visible faceted cyclone; head and rods retain animation.
        for ring in range(5):
            radius=1.4+ring*.8;yy=4+ring*2.5
            for i in range(7):
                a=i*2*math.pi/7+ring*.3
                m.box('body',(math.cos(a)*radius,yy,math.sin(a)*radius),(radius*.85,1.1,1.2),'light' if i%3 else 'crystal',(0,-math.degrees(a),8))
        for side in (-1,1):
            m.horn('rods',[(side*3,13,0),(side*6,16,0),(side*4,18,0),(side*7,20,0)],.8,'gold')


def creeper(m):
    m.mass('head',(0,20,0),(8,8,7),'scale')
    for side in (-1,1):m.box('head',(side*2,21,-3.6),(2,2,.4),'black')
    m.box('head',(0,18.8,-3.6),(2,2.5,.4),'black')
    for side in (-1,1):m.box('head',(side*1.4,17.5,-3.6),(1,2,.4),'black')
    m.mass('body',(0,11,0),(6,10,5),'scale')
    for part in m.parts:
        if not part.startswith('leg'):continue
        c,sz=m.bound(part);m.mass(part,c,sz,'dark');m.box(part,(c[0],c[1]-1,c[2]-.3),(sz[0],1,sz[2]+.5),'scale')


def affix(m,kind):
    c,sz=m.bound('body');x,y,z=c;w,h,d=sz
    if kind=='blindado':
        for side in (-1,1):
            for i in range(3):m.box('body',(x+side*w*.48,y+h*.3-i*h*.28,z),(.8,h*.3,d*1.05),'metal',(0,0,side*8))
        hc,hs=m.bound('head')
        m.box('head',(hc[0],hc[1]+hs[1]*.42,hc[2]),(hs[0]*1.03,.7,hs[2]*1.03),'metal')
    elif kind=='gelido':
        for i in range(5):
            a=(x+(i-2)*w*.16,y+h*.4,z+d*.2)
            m.horn('body',[a,(a[0]+(i-2)*.3,a[1]+2+i%2,a[2]+1),(a[0]+(i-2)*.5,a[1]+4+i%2,a[2]+1.5)],.9,'crystal')
    elif kind=='incendiario':
        for i in range(3):m.box('body',(x+(i-1)*w*.25,y,z-d*.5-.3),(.5,h*.65,.3),'ember',(0,0,(i-1)*15))
        for side in (-1,1):m.horn('body',[(side*w*.4,y+h*.3,z),(side*w*.6,y+h*.6,z),(side*w*.45,y+h*.8,z)],1,'ember')
    elif kind=='venenoso':
        for side in (-1,1):
            for i in range(3):m.mass('body',(x+side*w*.5,y+h*.2-i*h*.25,z+d*.25),(1.4,1.6,1.5),'gem')
    elif kind=='fantasma':
        for side in (-1,1):
            for i in range(3):m.horn('body',[(side*w*.3,y,z+d*.4+i*.3),(side*w*.4,y-h*.4-i,z+d*.6),(side*w*.6,y-h*.6-i,z+d*.8)],.6,'light')


def populate(data,ident,group,palette):
    if group=='expansao':
        from modelos_expansao import populate as expansion
        return expansion(data,ident,palette)
    m=Mesh(data)
    if group=='bestas':
        if ident=='kunpeng':kunpeng(m)
        else:animal(m,ident,'wolf' if ident=='pixiu' else 'horse')
    elif group.startswith('companheiros/'):
        species=group.split('/')[-1]
        if species=='golem':humanoid(m,ident,True)
        elif species=='papagaio':bird(m,ident)
        else:animal(m,ident,{'lobo':'wolf','gato':'cat','raposa':'fox','urso':'bear','cavalo':'horse','camelo':'camel','lhama':'llama'}[species])
    elif ident in ('golem_ancestral','golem_de_pedra','automato'):humanoid(m,ident,True)
    elif ident in ('lich','arauto_do_fim','sombra_morto_vivo','sombra_arqueiro'):humanoid(m,ident)
    elif ident in ('rainha_aracnidea','sombra_aranha'):spider(m,ident)
    elif ident in ('senhor_das_chamas','tempestade_viva'):floating_boss(m,ident)
    elif ident in ('lobo_ancestral','lobo_espectral'):animal(m,ident,'wolf')
    elif ident in ('elemental','espirito'):elemental(m,ident)
    elif ident.startswith('elite_'):
        kind=ident.split('_')[1]
        if kind=='spider':spider(m,ident)
        elif kind=='creeper':creeper(m)
        else:humanoid(m,ident)
        affix(m,ident.rsplit('_',1)[1])
    else:raise ValueError('Missing creature design: '+ident)
    data['textureSize']=[128,128]
    return texture(ident,palette),m.count
