"""Thirteen world bosses/hidden-place guardians, with independent visual identities."""
import numpy as np
from modelos_criaturas import Mesh, animal, humanoid, floating_boss, texture


def robe(m,mat='cloth',length=14):
    c,s=m.bound('body');x,y,z=c
    for i in range(5):
        m.box('body',(x+(i-2)*1.55,y-length*.32-(i%2)*.3,z),(1.65,length,5.8),mat,(0,0,(i-2)*2))
    for side in (-1,1):m.box('body',(x+side*3.3,y+1,z-3.1),(.55,9,.3),'gold')


def cloak(m,mat='red',wide=10,length=21):
    c,s=m.bound('body');x,y,z=c
    for i in range(7):
        xx=x+(i-3)*wide/7
        m.box('body',(xx,y-2-(i%2)*.5,z+s[2]/2+1),(wide/7+.15,length-(i%2)*2,.7),mat,(8,0,(i-3)*1.6))


def crown(m,broken=False,mat='gold'):
    c,s=m.bound('head');x,y,z=c;w,h,d=s
    m.box('head',(x,y+h*.43,z),(w+1,1,d+1),mat)
    for i in range(5):
        xx=x+(i-2)*w*.2;height=1.2 if broken and i==3 else 2.7+(i==2)
        m.horn('head',[(xx,y+h*.45,z-d*.45),(xx,y+h*.45+height,z-d*.5)],.6,mat)


def biped(m,face='ivory',coat='cloth'):
    c,s=m.bound('head');x,y,z=c;w,h,d=s
    m.mass('head',c,(w*.9,h*.88,d*.86),face)
    m.eyes('head',(x,y+h*.1,z-d*.45-.2),w)
    m.box('head',(x,y-h*.25,z-d*.45),(w*.35,.4,.3),'dark')
    c,s=m.bound('body');m.mass('body',c,(s[0],s[1],s[2]),coat)
    m.box('body',(c[0],c[1]-s[1]*.3,c[2]),(s[0]+.5,1.2,s[2]+.5),'leather')
    for part in ('left_arm','right_arm','left_leg','right_leg'):
        if part not in m.parts:continue
        c,s=m.bound(part);w,h,d=s
        m.mass(part,(c[0],c[1]+h*.15,c[2]),(w,h*.7,d),coat)
        m.mass(part,(c[0],c[1]-h*.35,c[2]),(w*.85,h*.25,d*.95),face if 'arm' in part else 'dark')
    if 'arms' in m.parts:
        # Illager idle/casting poses switch these vanilla roots independently.
        m.box('arms',(0,18,-3),(11,3,3),coat)
        m.box('arms',(-1.8,18,-4.5),(3,2.5,1),face)
        m.box('arms',(1.8,18,-4.5),(3,2.5,1),face)


def beast(m,nian):
    m.mass('body',(0,29,10),(23,25,34),'fur' if nian else 'scale')
    m.mass('body',(0,34,-2),(27,24,18),'fur' if nian else 'stone')
    m.mass('neck',(0,29,-7),(17,17,16),'fur')
    m.mass('head',(0,25,-17),(17,17,16),'fur')
    m.mass('head',(0,21,-26),(13,8,10),'light')
    m.mass('jaw',(0,16,-23),(13,4,13),'dark')
    m.box('head',(0,24,-31),(7,2,1),'black')
    m.eyes('head',(0,28,-25.1),17)
    for side in (-1,1):
        # Curved horns and lower-jaw tusks animate with their corresponding roots.
        m.horn('head',[(side*6,32,-15),(side*10,38,-13),(side*9,43,-17)],2.5,'gold' if nian else 'bone')
        m.horn('jaw',[(side*5,17,-27),(side*7,21,-30),(side*5,25,-31)],1.8,'bone')
        for i in range(5):
            if nian:
                m.mass('head',(side*(8.5-i*.25),31-i*3,-12),(4.5,4.5,5),'gold')
            else:
                m.horn('body',[(side*8,39,1+i*5),(side*10,43+i%2,3+i*5),(side*8,46+i%2,5+i*5)],2,'bone')
    for part in ('leg1','leg2','leg3','leg4'):
        c,s=m.bound(part);x,y,z=c
        m.mass(part,(x,28,z),(9,19,10),'fur')
        m.beam(part,(x,22,z),(x,6,z+1),5.5,'fur')
        m.mass(part,(x,4,z-1),(9,7,12),'light' if nian else 'dark')
        for dx in (-2.5,0,2.5):m.box(part,(x+dx,2,z-7),(1.4,1.5,3),'bone')
    if nian:
        # Festive eyebrows, whiskers and a jade bell collar give a distinct lion silhouette.
        for side in (-1,1):
            m.horn('head',[(side*5,25,-30),(side*10,26,-29),(side*13,28,-26)],.65,'ivory')
            m.box('head',(side*4,32,-25),(5,1.5,1),'gold',(0,0,-side*15))
        for i in range(5):m.mass('neck',((i-2)*2.5,20+abs(i-2)*.5,-13),(2,2.5,2),'gold')
        m.mass('neck',(0,18,-14),(3,4,2),'gem')
        for i in range(3):m.horn('jaw',[((i-1)*2,15,-27),((i-1)*1.3,10,-27),((i-1),8,-25)],1,'gold')
    else:
        for i in range(4):m.mass('body',(0,41,4+i*6),(11-i,3,5),'stone')
        m.horn('head',[(0,32,-23),(0,38,-25),(0,41,-28)],3,'bone')


def sculpt(m,ident):
    if ident in ('nian','fera_ancestral'):
        beast(m,ident=='nian');return
    if ident=='lobisomem':
        animal(m,'lobo_alfa','wolf')
        m.mass('body',(0,13,-1),(9,10,9),'dark')
        for side in (-1,1):
            m.mass('mane',(side*3,11,-4),(5,8,6),'fur')
            m.horn('head',[(side*1.8,8,-10),(side*1.8,5.5,-11)],1,'bone')
        for part in ('leg3','leg4'):
            c,s=m.bound(part)
            for dx in (-.7,0,.7):m.horn(part,[(c[0]+dx,1,c[2]-1),(c[0]+dx,.4,c[2]-3)],.4,'bone')
        return
    if ident=='guardiao_da_forja_antiga':
        humanoid(m,'automato',True)
        m.box('body',(0,26,-6.8),(9,8,1.1),'black')
        m.box('body',(0,26,-7.5),(6.8,6,.35),'ember')
        for x in (-3,-1,1,3):m.box('body',(x,26,-7.8),(.45,7,.5),'metal')
        for side,part in ((-1,'left_arm'),(1,'right_arm')):
            m.box(part,(side*11,33,0),(9,2,8),'metal')
            m.box(part,(side*11,35,0),(11,2.5,6),'dark')
        m.beam('right_arm',(15,1,-4),(15,25,-4),1.8,'wood')
        m.mass('right_arm',(15,23,-4),(10,7,7),'metal')
        return
    if ident=='guardiao_dos_ventos':
        floating_boss(m,'guardiao_dos_ventos')
        for side in (-1,1):
            for i in range(6):m.feather('head',(side*3,24,1+i*.2),(side*(8+i*.8),26-i*.7,3+i),1.3,'ivory')
        for i in range(8):
            a=i*np.pi/4;m.box('head',(np.cos(a)*6,31,np.sin(a)*6),(2,.65,1),'gold',(0,-i*45,0))
        return
    if ident in ('rei_caido','coracao_da_penumbra','arqueiro_espectral','paladino_corrompido'):
        humanoid(m,'elite_skeleton_fantasma' if ident in ('rei_caido','arqueiro_espectral') else ident)
        if ident=='rei_caido':
            crown(m,True);cloak(m,'red',12,24)
            m.mass('left_arm',(-6,22,0),(6,5,7),'metal')
            for i in range(4):m.box('body',((i-1.5)*2,22,3),(2.4,3,2),'ivory')
        elif ident=='coracao_da_penumbra':
            m.mass('body',(0,19,-3),(4.2,5,3),'crystal')
            for side in (-1,1):
                for i in range(3):m.horn('body',[(side*1,17+i*2,-3.5),(side*4,18+i*2,-4),(side*5,20+i*2,-1)],.8,'dark')
                m.horn('head',[(side*2,30,1),(side*4,35,2),(side*3,38,0)],1.2,'shadow')
            cloak(m,'shadow',11,21)
        elif ident=='arqueiro_espectral':
            cloak(m,'cloth',10,20)
            for side in (-1,1):m.box('head',(side*4.1,28,0),(.7,8,8),'cloth')
            m.box('head',(0,32.2,0),(9,.7,8.5),'cloth')
            m.horn('right_arm',[(8,7,-3),(11,12,-4),(12,19,-4),(10,25,-3),(8,28,-3)],.7,'gold')
            m.beam('right_arm',(8,7,-3),(8,28,-3),.12,'ivory')
            m.box('body',(-3,18,4),(3,10,3),'leather',(0,0,15))
            for x in (-4,-3,-2):m.beam('body',(x,22,4),(x,29,4),.25,'bone')
        else:
            # Asymmetric pale plate and black corruption, rather than a recolored zombie.
            m.mass('right_arm',(6,22,0),(6,6,6),'ivory')
            m.mass('left_arm',(-6,22,0),(6,6,6),'shadow')
            m.box('body',(0,17,-3),(5,12,.7),'ivory')
            m.box('body',(0,20,-3.5),(1,5,.4),'gold');m.box('body',(0,20,-3.6),(4,1,.4),'gold')
            for i in range(4):m.horn('left_arm',[(-5,20-i*2,-2),(-8,21-i*2,-3),(-9,23-i*2,-2)],.7,'shadow')
            crown(m,broken=True);cloak(m,'cloth',11,19)
        return
    # The three robed characters have different headgear, silhouettes and props.
    biped(m,'ivory' if ident=='vampiro' else 'light', 'cloth')
    if ident=='vampiro':
        cloak(m,'red',15,25)
        for side in (-1,1):
            m.box('body',(side*4,24,1),(3,7,1),'red',(0,0,-side*20))
            m.box('head',(side*.8,26,-3.7),(.45,1.4,.4),'bone')
            m.box('head',(side*3.4,30,1),(1,5,5),'black')
        m.box('head',(0,31.5,0),(7,1.5,7),'black')
        m.mass('body',(0,22,-2.5),(1.7,2.4,.7),'gem')
    elif ident=='bibliotecario_amaldicoado':
        robe(m,length=20);cloak(m,'cloth',10,23)
        for side in (-1,1):
            m.box('head',(side*2,29,-4.3),(2,.5,.3),'gold')
            m.box('body',(side*5,15,0),(2,5,4),'leather')
            m.box('body',(side*5,15,-.2),(1.3,4,4),'ivory')
        m.box('head',(0,34.5,0),(8.5,2,8.5),'cloth')
        for side in (-1,1):
            m.box('body',(side*2,20,-6),(4,.6,5),'gold',(0,0,side*15))
            m.box('body',(side*2,20.4,-6),(3.6,.5,4.5),'ivory',(0,0,side*15))
            for i in range(3):m.box('body',(side*2,20.8,-7.5+i),(2.5,.12,.12),'dark',(0,0,side*15))
    elif ident=='sombra_sem_nome':
        cloak(m,'shadow',11,24)
        m.box('head',(0,27,-4.2),(7,4,.65),'black')
        for side in (-1,1):m.box('head',(side*4.1,29,0),(.8,10,9),'shadow')
        m.box('head',(0,34,0),(9,1,9),'shadow')
        for i in range(4):m.box('body',(-2+i*1.3,22-i*2,-3.3),(1.5,2.5,.5),'leather',(0,0,-30))
        m.horn('right_arm',[(6,13,-3),(6,8,-4),(6,5,-3)],.7,'metal')
    elif ident=='sumo_sacerdote':
        robe(m,'scale',20);cloak(m,'cloth',10,22);crown(m,mat='gold')
        for side in (-1,1):
            m.horn('head',[(side*3,31,1),(side*5,35,2),(side*4,38,1)],.9,'red')
            m.horn('head',[(side*5,35,2),(side*7,36,1)],.7,'red')
            for i in range(3):m.feather('head',(side*3.5,27+i*.8,1),(side*6,28+i*.8,3),.8,'scale')
        m.beam('right_arm',(8,0,-3),(8,32,-3),.8,'gold')
        m.box('right_arm',(8,29,-3),(6,.8,.8),'gold')
        for x in (5.5,8,10.5):m.horn('right_arm',[(x,29,-3),(x,34+(x==8)*2,-3)],.65,'gold')
        m.mass('body',(0,22,-3),(2,3,.8),'gem')
    else:raise ValueError(ident)


def populate(data,ident,palette):
    # Template-only expressions can refer to nodes replaced by this new geometry.
    for part in data['models']:part.pop('animations',None)
    m=Mesh(data);sculpt(m,ident)
    tex=texture(ident,palette)
    return tex,m.count
