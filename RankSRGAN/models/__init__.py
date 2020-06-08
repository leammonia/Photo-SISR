import logging
logger = logging.getLogger('base')


def create_model(opt):
    from .RankSRGAN_model import SRGANModel as M
    m = M(opt)
    return m
