package com.pm.SRserver.schedule;

import java.util.Date;
import java.util.TimerTask;

import com.pm.SRserver.processor.PicProcessor;

public class CacheClear extends TimerTask{
	
	private long period = 1000*60*60*2;
	
	public CacheClear(long cacheClearPeriod) {
		super();
		period = cacheClearPeriod;
	}

	@Override
	public void run() {
		PicProcessor processor = PicProcessor.getInstance();
		processor.clearCache(new Date().getTime() - period);
	}
}
