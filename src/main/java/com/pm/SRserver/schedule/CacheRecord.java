package com.pm.SRserver.schedule;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CacheRecord {
	private Date time = null;
	private String file = null;

	public CacheRecord(String fileName) {
		time = new Date();
		file = fileName;
	}

	public CacheRecord(String date, String fileName) {		
		file = fileName;
		
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			time = formatter.parse(date);
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
	
	public final long getRecordTime() {
		return time.getTime();
	}
	
	public final String getFileName() {
		return file;
	}

	public final String getFormatTime() {
		return  new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(time);
	}

}	
