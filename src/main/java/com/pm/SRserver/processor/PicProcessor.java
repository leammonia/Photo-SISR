package com.pm.SRserver.processor;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Queue;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.boot.system.ApplicationHome;
import org.springframework.web.multipart.MultipartFile;

import com.pm.SRserver.schedule.CacheRecord;

public class PicProcessor {

	private final String PATH = (new ApplicationHome(getClass())).getSource().getParentFile().getParent();
	private static final PicProcessor processor = new PicProcessor();
	private static ServerProperty setting;
	private static Queue<CacheRecord> cache;
	
	public PicProcessor() {		
		//加载服务器属性
		try {
			InputStream inputStream = new FileInputStream(PATH + "/server.properties");
			setting = new ServerProperty(inputStream);
			inputStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static PicProcessor getInstance() { //单例
		return processor;
	}

	public final ServerProperty getProperty() {
		return setting;
	}

	public final String getPath() {
		return PATH;
	}
	
	public void loadCache() {
		cache = new LinkedList<CacheRecord>();

		Properties properties = new Properties();
		try {
			File cacheFile = new File(PATH + "/RankSRGAN/cacheList.properties");
			if (setting.getCacheClearPeriod() == -1) { //不处理缓存 且清除旧的记录
				if (cacheFile.exists()) {
					cacheFile.delete();
				}
				return;
			} else if (setting.getCacheClearPeriod() == 0) { //清空缓存 清除旧的记录
				clearAll("/RankSRGAN/LRimage");
				clearAll("/RankSRGAN/SRimage");
				if (cacheFile.exists()) {
					cacheFile.delete();
				}
				return;
			} else if (!cacheFile.exists()){ //不存在缓存名单 清空缓存
				clearAll("/RankSRGAN/LRimage");
				clearAll("/RankSRGAN/SRimage");
				return;
			}
			
			InputStream inputStream = new FileInputStream(cacheFile);
			properties.load(inputStream);
			inputStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		for (String fileName: properties.stringPropertyNames()) {
			String date = properties.getProperty(fileName);
			cache.offer(new CacheRecord(date, fileName));
		}
	}

	public int writeCacheToFile() { //退出前保存缓存列表
		if (setting.getCacheClearPeriod() == -1) {
			return 0;
		} else if (setting.getCacheClearPeriod() == 0) {
			clearAll("/RankSRGAN/LRimage");
			clearAll("/RankSRGAN/SRimage");
			return 0;
		}
		Properties properties = new Properties();
		try {
			File cacheFile = new File(PATH + "/RankSRGAN/cacheList.properties");
			if (cache.size() == 0 && cacheFile.exists()) {
				cacheFile.delete();
				return 0;
			}
			FileOutputStream outputStream = new FileOutputStream(cacheFile);
			while (!cache.isEmpty()) {
				CacheRecord record = cache.poll();
				properties.setProperty(record.getFileName(), record.getFormatTime());
			}
			properties.store(outputStream, "Format: key = cache name, value = save time");
			outputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		}
		return 0;
	}

	private String renamePic () {
		String uuid = UUID.randomUUID().toString();
		String saveName = uuid.substring(0,8) + uuid.substring(9,13) + uuid.substring(14,18) + uuid.substring(19,23) + uuid.substring(24);
		return saveName;
	}
	
	private boolean gifTojpg(FileInputStream is, String path, String saveName) throws IOException {
		GifDecoder decoder = new GifDecoder();
		if (decoder.read(is) != 0) {
			System.out.println(new Date().toString() +"|| Read gif " + saveName + " failed");
			return false;
		}
		is.close();
		for (int i = 0; i < decoder.getFrameCount(); i++) {
			BufferedImage frame = decoder.getFrame(i);
			if (frame.getWidth() > setting.getMaxWidth() || frame.getHeight() > setting.getMaxHeight()) {
				System.out.println(new Date().toString() +"|| Input image " + saveName + " is too large");
				return false;
			}
			int delay = decoder.getDelay(i);
			FileOutputStream out = new FileOutputStream(path + "/" + String.format("%04d", i) + "_" + delay + ".png");
			ImageIO.write(frame, "png", out);// 将frame 按jpg格式 写入out中
			out.flush();
			out.close();
		}
		return true;
	}
	

	private int pngToGif(String path) {
		File[] files = new File(path).listFiles();
		if (files.length <= 1) { //不需要转换 直接返回
			return 0;
		}
		try {
			AnimatedGifEncoder e = new AnimatedGifEncoder();
			e.start(path + "/result.gif");//生成gif图片位置名称
			e.setRepeat(0);

			for (File file: files) {
				String name = file.getName();
				int delay = Integer.parseInt(name.substring(name.lastIndexOf('_') + 1, name.lastIndexOf('.')));
				BufferedImage src = ImageIO.read(file);
				e.setDelay(delay);
				e.addFrame(src);
				if (!file.delete()) {
					return -1;
				}
			}
			e.finish();
			
		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		}
		
		return 0;
	}

	private void clearAll(String type) { //清除所有SR/LR图像
		File dir = new File(PATH + type);
		if (dir.exists() && dir.isDirectory()) {
			File[] folders = dir.listFiles();
			for (File folder: folders) {
				clearImage(type, folder.getName());
			}
		}
	}
	
	private int clearImage (String typePath, String saveName) {
		String path = PATH + typePath + "/" + saveName;
		File dirFile = new File(path);
		if (!dirFile.exists() || !dirFile.isDirectory()) {
			return 0;
		}
		
		File[] files = dirFile.listFiles();
		boolean flag = true;
		for (File file: files) {
			flag = (flag && file.delete());
		}
		if (flag && dirFile.delete()) {
			return 0;
		} else {
			return -1;
		}
	}

	public String saveLR (MultipartFile originalFile, String enlarge) {
		String fileName = originalFile.getOriginalFilename();
		String suffix = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
		if (!setting.getAllowedType().contains(suffix) || !setting.getAllowedScale().contains(enlarge)) {
			return null;
		}
		
		String saveName = renamePic();
		String path = PATH +"/RankSRGAN/LRimage/" + saveName;
		File dir = new File(path);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		
		if (suffix.equals("gif")) { //处理gif
			try {
				if (!gifTojpg((FileInputStream) originalFile.getInputStream(), path, saveName)) {
					return null;
				}
			} catch (IOException e) {
				return null;
			}
		} else {
			File file = new File(path + "/result." + suffix);
			try {
				originalFile.transferTo(file);
				BufferedImage tmpImg = ImageIO.read(file);
				if (tmpImg.getWidth() > setting.getMaxWidth() || tmpImg.getHeight() > setting.getMaxHeight()) {
					System.out.println(new Date().toString() +"|| Input image " + saveName + " is too large");
					return null;
				}
			} catch (IOException e) {
				return null;
			}
		}
		
		if (setting.getCacheClearPeriod() > 0) {
			cache.offer(new CacheRecord(saveName));
		}
		
		return saveName;
	}
	
	public int SRprocess (String saveName, String enlarge) {
		int ret = 0;
		String pyPath = PATH + "/RankSRGAN/test.py";
		String[] argv =  {"python", pyPath, saveName, enlarge.replace('.', '_'), getProperty().getGPUID()};
		try {
			Process proc = Runtime.getRuntime().exec(argv);
			ret = proc.waitFor();//normal:0 error:1
			if (ret != 0) {
				System.out.println(new Date().toString() +"|| Python executed faild, command: python " + pyPath + " " + saveName + " " + enlarge.replace('.', '_') + " " + getProperty().getGPUID());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (ret == 0) {
			ret = pngToGif(PATH + "/RankSRGAN/SRimage/" + saveName);
		}

		if (setting.getCacheClearPeriod() == 0) { //不缓存图像
			//clear LR images
			if (clearImage("/RankSRGAN/LRimage", saveName) != 0) {
				System.out.println(new Date().toString() +"|| Clear LR image " + saveName + " failed");
			} else {
				System.out.println(new Date().toString() +"|| Clear LR image " + saveName + " successfully");
			}
		}

		return ret;
	}
	
	public byte[] getSR (String fileName) throws IOException {
		String path = PATH +"/RankSRGAN/SRimage/" + fileName;
		File dir = new File(path);
		if (!dir.exists()) {
			return null;
		}
		File file = dir.listFiles()[0];

		FileInputStream fileis = new FileInputStream(file);
		byte[] bytes = new byte[fileis.available()];
		fileis.read(bytes, 0, fileis.available());
		fileis.close();

		if (setting.getCacheClearPeriod() == 0) { //不缓存图像
			//clear SR images
			if (clearImage("/RankSRGAN/SRimage", fileName) != 0) {
				System.out.println(new Date().toString() +"|| Clear SR image " + fileName + " failed");
			} else {
				System.out.println(new Date().toString() +"|| Clear SR image " + fileName + " successfully");
			}
		}
		return bytes;
	}

	public void clearCache(long time) {
		if (cache.isEmpty() || cache.peek().getRecordTime() >= time) {
			return;
		}
		while (!cache.isEmpty() && cache.peek().getRecordTime() < time) {
			String fileName = cache.poll().getFileName();
			//clear LR images
			if (clearImage("/RankSRGAN/LRimage", fileName) != 0) {
				System.out.println(new Date().toString() +"|| Clear LR image " + fileName + " failed");
			} else {
				System.out.println(new Date().toString() +"|| Clear LR image " + fileName + " successfully");
			}
			
			//clear SR images
			if (clearImage("/RankSRGAN/SRimage", fileName) != 0) {
				System.out.println(new Date().toString() +"|| Clear SR image " + fileName + " failed");
			} else {
				System.out.println(new Date().toString() +"|| Clear SR image " + fileName + " successfully");
			}
		}
	}
		
}
