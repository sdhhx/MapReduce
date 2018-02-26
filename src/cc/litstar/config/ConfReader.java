package cc.litstar.config;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

//当前目录信息由通信节点制定
public class ConfReader {
	private static String confPath = "conf/config.json";
	
	private final static Logger logger = LoggerFactory.getLogger(ConfReader.class);
	
	private static Configuration config = null;
	
	private ConfReader() {
		;
	}
	
	public static Configuration getConf() {
		if(config == null) {
			readConf();
		}
		return config;
	}
	
	private static void readConf(){
		//读取Json字符串，允许以;开始作为注释
		String json = "";
		String line = null;
		logger.info("Starting reading information from config file");
		try(BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(confPath), "UTF-8")) ){
			while((line=in.readLine())!=null){
				if(!line.startsWith(";")){
					json += line;
				}	
			}
		} catch (FileNotFoundException e) {
			logger.error("Cannot find config.json");
			e.printStackTrace();
		} catch (IOException e) {
			logger.error("Cannot read config.json");
			e.printStackTrace();
		}
		json = json.replaceAll("\\s+", "");
		
		//将字符串读入配置文件
		Gson gson = new Gson();  
		Type type = new TypeToken<Configuration>(){}.getType();  
		try{
			config = gson.fromJson(json, type);
		}catch (Exception e) {
			logger.error("Cannot read configure file -- errors found in the file");
			e.printStackTrace();
			System.exit(0);
		}

		logger.info("Reading config.json finished");
	}
	
	//测试读取
	public static void main(String[] args) {
		//System.out.println(new ConfReader().readConf().getConf());
		ConfReader.getConf();
	}
}
