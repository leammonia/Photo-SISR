import logging
from collections import OrderedDict
import torch
import torch.nn as nn
from torch.nn.parallel import DataParallel, DistributedDataParallel
import models.RankSRGAN_arch as RankSRGAN_arch

class SRGANModel():
    def __init__(self, opt):
        self.opt = opt
        self.device = torch.device('cuda' if opt['gpu_ids'] is not None else 'cpu')

        # define networks and load pretrained models
        if opt['scale'] == 4:
            self.netG = RankSRGAN_arch.SRResNet(in_nc=opt['in_nc'], out_nc=opt['out_nc'], nf=opt['nf'], nb=opt['nb'], upscale=opt['scale'])
        else:
            self.netG = RankSRGAN_arch.MSRResNet(in_nc=opt['in_nc'], out_nc=opt['out_nc'], nf=opt['nf'], nb=opt['nb'], upscale=opt['scale'])
        self.netG = DataParallel(self.netG)

        self.load()  # load G and D if needed

    def feed_data(self, data, need_GT=True):
        self.var_L = data['LQ'].to(self.device)  # LQ

    def test(self):
        self.netG.eval()
        with torch.no_grad():
            self.fake_H = self.netG(self.var_L)
        self.netG.train()

    def get_current_visuals(self):
        out_dict = OrderedDict()
        out_dict['LQ'] = self.var_L.detach()[0].float().cpu()
        out_dict['rlt'] = self.fake_H.detach()[0].float().cpu()
        return out_dict

    def load(self):
        load_path_G = self.opt['path']
        if load_path_G is not None:
            if isinstance(self.netG, nn.DataParallel) or isinstance(self.netG, DistributedDataParallel):
                self.netG = self.netG.module
            load_net = torch.load(load_path_G)
            load_net_clean = OrderedDict()  # remove unnecessary 'module.'
            for k, v in load_net.items():
                if k.startswith('module.'):
                    load_net_clean[k[7:]] = v
                else:
                    load_net_clean[k] = v
            self.netG.load_state_dict(load_net_clean, True)

