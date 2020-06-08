package com.pm.SRserver.controller;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.pm.SRserver.processor.*;

@Controller
public class PicController {
	
	private PicProcessor processor = PicProcessor.getInstance();
	
	//默认页面
	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String welcome() throws IOException {
		return "/index.html";
	}
	
	//返回服务器设置的属性
	@ResponseBody
	@RequestMapping(value = "/properties", method = RequestMethod.GET) 
	public Map<String, Object> serverProperties() {
		Map<String, Object> serverProperties = new HashMap<String, Object>();
		serverProperties.put("allowedType", processor.getProperty().getAllowedType());
		serverProperties.put("allowedScale", processor.getProperty().getAllowedScale());
		serverProperties.put("maxW", processor.getProperty().getMaxWidth());
		serverProperties.put("maxH", processor.getProperty().getMaxHeight());
		return serverProperties;
	}

	//上传图片
	@ResponseBody
	@RequestMapping(value = "/upload", method = RequestMethod.POST)
	public String uploadPic(@RequestParam("image") MultipartFile file,
							@RequestParam("enlarge") String enlarge) {
		if (file.isEmpty()) {
			return "";
		}else {
			
			int ret = -1;
			System.out.println(new Date().toString() +"|| Receieved image " + file.getOriginalFilename());
			
			//save LR images
			String saveName = processor.saveLR(file, enlarge);
			if (saveName == null) {
				System.out.println(new Date().toString() +"|| Save image " + file.getOriginalFilename() + " failed");
			} else {
				System.out.println(new Date().toString() +"|| Reveieved LR image is saved as " + saveName);			
				
				//picture processing
				ret = processor.SRprocess(saveName, enlarge);
				if (ret != 0) {
					System.out.println(new Date().toString() +"|| Error in processing " + saveName +", code: " + ret);
				} else {
					System.out.println(new Date().toString() +"|| Processed " + saveName + " successfully");
				}
			}
			
			return ret == 0? saveName: "";
		}		
	}
	
	//返回SR图片
	@ResponseBody
	@RequestMapping(value = "/download/{picid}", method = RequestMethod.GET, produces = MediaType.IMAGE_JPEG_VALUE)
	public byte[] getSRimage(@PathVariable(name = "picid") String fileName) throws IOException {
		byte[] bytes = processor.getSR(fileName);
		if (bytes == null) {
			System.out.println(new Date().toString() +"|| File " + fileName + " doesn't exists");
		} else {
			System.out.println(new Date().toString() +"|| Send back SR image " + fileName);
		}
		return bytes;
	}

}
