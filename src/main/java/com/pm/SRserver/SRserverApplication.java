package com.pm.SRserver;

import java.util.Date;
import java.util.Timer;

import javax.annotation.PreDestroy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.Banner;

import com.pm.SRserver.processor.PicProcessor;
import com.pm.SRserver.schedule.CacheClear;

@SpringBootApplication
public class SRserverApplication {

	private static PicProcessor processor = PicProcessor.getInstance();
	
	public static void main(String[] args) {
		System.out.println("Photo4x server starting...");
		//SpringApplication.run(SRserverApplication.class, args);
		SpringApplication springApplication = new SpringApplication(SRserverApplication.class);
		springApplication.setBannerMode(Banner.Mode.CONSOLE);
		springApplication.run(args);

		System.out.println("==========================================");
		System.out.println("[Photo4x Server Properties]");
		System.out.println("Tensorflow Use: " + processor.getProperty().getGPUIDStr());	
		System.out.println("Allowed Input Type: " + processor.getProperty().getAllowedType().toString());
		System.out.println("Allowed Input Scale: " + processor.getProperty().getAllowedScale().toString());
		System.out.println("Allowed Input Max Width: " + processor.getProperty().getMaxWidth() + "px");
		System.out.println("Allowed Input Max Height: " + processor.getProperty().getMaxHeight() + "px");
		
		processor.loadCache();
		String period;
		if (processor.getProperty().getCacheClearPeriod() == -1) {
			period = "Never";
		} else if (processor.getProperty().getCacheClearPeriod() == 0) {
			period = "No cache";
		} else {
			period = "Every " + processor.getProperty().getCacheClearPeriod()/(1000*60) + " minutes";
		}
		System.out.println("Cache Clear Period: " + period);	
		System.out.println("Photo4x server is ready for SR processing. Press CTRL+C to SHUTDOWN the server.");
		System.out.println("If you want to modify any Server Properties, please EDIT " + processor.getPath() + "\\server.properties and SAVE, then RESTART Photo4x server.");
		System.out.println("==========================================");

		if (processor.getProperty().getCacheClearPeriod() > 0) {
			Timer timer = new Timer();
			timer.schedule(new CacheClear(processor.getProperty().getCacheClearPeriod()), 0, processor.getProperty().getCacheClearPeriod());
		}
	}

	@PreDestroy
	public void PreDestroy() { //服务器退出前执行
		if (processor.writeCacheToFile() == 0) {
			System.out.println(new Date().toString() +"|| Photo4x exited successfully.");
		} else {
			System.out.println(new Date().toString() +"|| Photo4x exited with failing to save the cache list.");
		}
	}

}
