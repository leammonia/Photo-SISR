import numpy as np
import torch
import torch.utils.data as data
import data.util as util

class LQDataset(data.Dataset):
    '''Read LQ images only in the test phase.'''

    def __init__(self, dataroot):
        super(LQDataset, self).__init__()
        self.paths_LQ, self.paths_GT = None, None

        self.paths_LQ = util.get_image_paths(dataroot)
        assert self.paths_LQ, 'Error: LQ paths are empty.'

    def __getitem__(self, index):
        LQ_path = None

        # get LQ image
        LQ_path = self.paths_LQ[index]
        img_LQ = util.read_img(LQ_path)
        H, W, C = img_LQ.shape

        # BGR to RGB, HWC to CHW, numpy to tensor
        if img_LQ.shape[2] == 3:
            img_LQ = img_LQ[:, :, [2, 1, 0]]
        img_LQ = torch.from_numpy(np.ascontiguousarray(np.transpose(img_LQ, (2, 0, 1)))).float()

        return {'LQ': img_LQ, 'LQ_path': LQ_path}

    def __len__(self):
        return len(self.paths_LQ)
