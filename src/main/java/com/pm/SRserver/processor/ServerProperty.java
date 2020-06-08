package com.pm.SRserver.processor;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

public class ServerProperty {

	private static int maxW;
	private static int maxH;
	private static ArrayList<String> allowedType;
    private static ArrayList<String> allowedScale;
    private static long cacheClearPeriod;
    private static String gpuID;
    
    public ServerProperty(InputStream inputStream) {
        //加载设置
		Properties properties = new Properties();
		try {
			properties.load(inputStream);
		} catch (Exception e) {
			e.printStackTrace();
			allowedType = new ArrayList<String>(Arrays.asList("jpg","jpeg","png","gif"));
			maxW = 200;
			maxH = 200;
			cacheClearPeriod = -1;
		}
		String type = properties.getProperty("allowedInputType").toLowerCase();
		allowedType = new ArrayList<String>(Arrays.asList(type.indexOf("/") == -1? new String[]{type} : type.split("/")));

		String scale = properties.getProperty("allowedScale").toLowerCase();
		allowedScale = new ArrayList<String>(Arrays.asList(scale.indexOf("/") == -1? new String[]{scale} : scale.split("/")));
		
		maxW = Integer.parseInt(properties.getProperty("allowedMaxWidth"));
		maxH = Integer.parseInt(properties.getProperty("allowedMaxHeight"));
        cacheClearPeriod = 1000*60*Long.parseLong(properties.getProperty("cacheClearPeriod"));
        gpuID = properties.getProperty("gpuID");
    }
    
    public final ArrayList<String> getAllowedType() {
		return allowedType;
    }

	public final ArrayList<String> getAllowedScale() {
		return allowedScale;
    }

	public final int getMaxWidth() {
		return maxW;
	}

	public final int getMaxHeight() {
		return maxH;
	}

	public final long getCacheClearPeriod() {
		return cacheClearPeriod;
    }

    public final String getGPUID() {
        return gpuID;
    }

    public final String getGPUIDStr() {
        if (gpuID.equals("-1")) {
            return "CPU Only";
        } else if (gpuID.indexOf("/") != -1){
            return "GPU" + gpuID.split("/").toString();
        } else {
            return "GPU[" + gpuID + "]";
        }
    }
}