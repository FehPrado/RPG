"""Checks the coordinate conversion used to attach new anatomy to animated CEM roots."""
import copy
import json
from pathlib import Path
import unittest

import numpy as np

from modelos_criaturas import Mesh, rotation


class AttachmentTests(unittest.TestCase):
    def test_rotated_root_keeps_requested_world_position_and_orientation(self):
        bases=json.loads((Path(__file__).parent/'modelos_bases.json').read_text(encoding='utf-8'))
        for ident,part in [('gato_selvagem','body'),('urso_glacial','body'),
                           ('raposa_astuta','tail'),('camelo_do_deserto','left_ear'),
                           ('golem_ancestral','right_arm')]:
            with self.subTest(ident=ident,part=part):
                mesh=Mesh(copy.deepcopy(bases[ident]['modelo']))
                center=np.array([2.5,13.5,-4.25]);angles=[23,34,-17]
                mesh.box(part,center,[2,4,3],'bone',angles)
                root=mesh.parts[part];child=root['submodels'][0]
                pivot=-np.array(root.get('translate',[0,0,0]),float)
                rr=rotation(root.get('rotate',[0,0,0]))
                actual=pivot+rr@(np.array(child['translate'])-pivot)
                np.testing.assert_allclose(actual,center,atol=1e-4)
                np.testing.assert_allclose(rr@rotation(child['rotate']),rotation(angles),atol=1e-6)

    def test_beams_reach_both_authored_endpoints(self):
        for a,b in [([0,1,2],[7,12,-3]),([0,0,0],[0,-5,0]),([1,2,3],[-2,2,9])]:
            data={'models':[{'part':'body','id':'body','invertAxis':'xy','translate':[0,0,0]}]}
            mesh=Mesh(data);mesh.beam('body',a,b,1)
            child=mesh.parts['body']['submodels'][0]
            length=child['boxes'][0]['coordinates'][4]
            r=rotation(child['rotate']);center=np.array(child['translate'])
            np.testing.assert_allclose(center-r[:,1]*length/2,a,atol=1e-4)
            np.testing.assert_allclose(center+r[:,1]*length/2,b,atol=1e-4)


if __name__=='__main__':unittest.main()
