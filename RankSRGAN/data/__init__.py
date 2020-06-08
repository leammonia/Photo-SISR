"""create dataset and dataloader"""
import logging
import torch
import torch.utils.data


def create_dataloader(dataset):
    return torch.utils.data.DataLoader(dataset, batch_size=1, shuffle=False, num_workers=0,pin_memory=False)

def create_dataset(dataroot):
    from data.LQ_dataset import LQDataset as D
    dataset = D(dataroot)
    logger = logging.getLogger('base')
    return dataset
