import os
import sys
import numpy as np
import cv2
from collections import OrderedDict

from data import create_dataset, create_dataloader
from models import create_model

#### path information
PATH = ''
index = sys.argv[0].rfind('/')
picid = sys.argv[1]
enlarge = sys.argv[2]
gpuid = sys.argv[3]
if index != -1:
    PATH = sys.argv[0][:index] + '/'
LRimage_path = PATH + 'LRimage/' + picid
SRimage_path = PATH + 'SRimage/' + picid

#### options    
model_info = OrderedDict()
model_info['path'] = PATH + 'models/'
model_info['in_nc'] = 3
model_info['out_nc'] = 3
model_info['nf'] = 64
model_info['nb'] = 16
if enlarge == '3_0':
	model_info['path'] += '3_0_mmsr_RankSRGAN_NIQE.pth'
	model_info['scale'] = 3
elif enlarge == '2_0':
	model_info['path'] += '2_0_mmsr_RankSRGAN_NIQE.pth'
	model_info['scale'] = 2
else:
	model_info['path'] += '4_0_mmsr_RankSRGAN_NIQE.pth'
	model_info['scale'] = 4

if gpuid == '-1':
    model_info['gpu_ids'] = None # Node if Server doesn't have GPU so use CPU
elif gpuid.find('/') == -1:
    model_info['gpu_ids'] = [int(gpuid)]
else:
    model_info['gpu_ids'] = [int(x) for x in gpuid.split('/')]
    
def tensor2img(tensor, out_type=np.uint8, min_max=(0, 1)):
    '''
    Converts a torch Tensor into an image Numpy array
    Input: 4D(B,(3/1),H,W), 3D(C,H,W), or 2D(H,W), any range, RGB channel order
    Output: 3D(H,W,C) or 2D(H,W), [0,255], np.uint8 (default)
    '''
    tensor = tensor.squeeze().float().cpu().clamp_(*min_max)  # clamp
    tensor = (tensor - min_max[0]) / (min_max[1] - min_max[0])  # to range [0,1]
    n_dim = tensor.dim()
    if n_dim == 4:
        n_img = len(tensor)
        img_np = make_grid(tensor, nrow=int(math.sqrt(n_img)), normalize=False).numpy()
        img_np = np.transpose(img_np[[2, 1, 0], :, :], (1, 2, 0))  # HWC, BGR
    elif n_dim == 3:
        img_np = tensor.numpy()
        img_np = np.transpose(img_np[[2, 1, 0], :, :], (1, 2, 0))  # HWC, BGR
    elif n_dim == 2:
        img_np = tensor.numpy()
    else:
        raise TypeError(
            'Only support 4D, 3D and 2D tensor. But received with dimension: {:d}'.format(n_dim))
    if out_type == np.uint8:
        img_np = (img_np * 255.0).round()
        # Important. Unlike matlab, numpy.unit8() WILL NOT round by default.
    return img_np.astype(out_type)

#### Create test dataset and dataloader
test_set = create_dataset(LRimage_path)
test_loader = create_dataloader(test_set)

model = create_model(model_info)

if not os.path.exists(SRimage_path):
        os.makedirs(SRimage_path)
        
for data in test_loader:
    model.feed_data(data, need_GT=False)
    img_path = data['LQ_path'][0]
    img_name = os.path.splitext(os.path.basename(img_path))[0]

    model.test()
    visuals = model.get_current_visuals()

    sr_img = tensor2img(visuals['rlt'])  # uint8

    # save images
    save_img_path = os.path.join(SRimage_path, img_name + '.png')
    cv2.imwrite(save_img_path, sr_img)