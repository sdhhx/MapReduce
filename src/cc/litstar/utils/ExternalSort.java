package cc.litstar.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

//有对一个列表内部排序的必要，这里只是一个文件的
public class ExternalSort {
	
	private List<String> inFilename;
	private String outFilename;
	
	public ExternalSort(List<String> inFilename, String outFilename) {
		super();
		this.inFilename = inFilename; 
		//这里的文件名带目录，中间文件名为依赖输出文件名(不依赖目录部分)
		this.outFilename = outFilename;
	}

	//每次分割文件的缓冲大小
	private static int BUFFER_SIZE = 2000;
	//private static int BUFFER_SIZE = 25600; //正常情况下使用，大约每次分割并内部排序消耗内存50MB
	//用于外部排序中间结果，文件目录需自行指定
	private static String workDir = "./tempDir/";
	
	static {
		File workdir = new File(workDir);
		if(!workdir.isDirectory()) {
			workdir.mkdir();
		}
	}
	
	//即对文件列表中文件进行外部排序，结果输出
	public File sort() throws IOException {
		ArrayList<File> files = new ArrayList<>();
		//列表中所有文件append
		for(String filename : this.inFilename) {
			files.addAll(split(new File(filename)));
		}
		File res = process(files);
		File resrename = new File(this.outFilename);
		//覆盖文件
		if(resrename.exists()) {
			resrename.delete();
		}
		res.renameTo(resrename);
		return resrename;
	}
	
	//采用递归的方法来合并列表，知道仅剩下单个合并的列表
	private File process(ArrayList<File> list) throws IOException {
		//递归出口
		if(list.size() == 1) {
			return list.get(0);
		}
		//递归合并文件，采用两两合并的方式
		ArrayList<File> inter = new ArrayList<File>();
		//递归文件列表，将文件进行合并
		for(Iterator<File> itr = list.iterator(); itr.hasNext();) {
			File one = itr.next();
			if(itr.hasNext()) {
				File two = itr.next();
				inter.add(merge(one, two));
				one.delete();
				two.delete();
			} else {
				inter.add(one);
			}
		}
		return process(inter);
	}

	//将文件切割成若干个子文件
	//读取缓存区，缓存区满后，对缓存区数据内部排序并写入文件
	private ArrayList<File> split(File file) throws IOException {
		ArrayList<File> files = new ArrayList<File>();
		//写入缓存
		String[] buffer = new String[BUFFER_SIZE];	
		BufferedReader fr = new BufferedReader(new FileReader(file));
		boolean fileComplete = false;
		
		//采用输出文件名作为文件名前缀
		String[] tmp = this.outFilename.split("/");
		String filename = tmp[tmp.length - 1];
		
		while(!fileComplete) {
			int index = buffer.length;
			for(int i = 0; i < buffer.length && !fileComplete; i++) {
				buffer[i] = fr.readLine();
				//Buffer满了或者文件读取完成
				if(buffer[i] == null) {
					fileComplete = true;
					index = i;
				}
			}
			//非null开头
			if(buffer[0] != null) {
				//内部排序后写入文件
				Arrays.sort(buffer, 0, index);
				File f = new File(workDir + filename + "-split-" + Math.abs(new Random().nextInt()));
				while(f.exists()) {
					f = new File(workDir + filename + "-split-" + Math.abs(new Random().nextInt()));
				}
				BufferedWriter writer = new BufferedWriter(new FileWriter(f));
				for(int j = 0; j < index; j++) {
					writer.write(buffer[j]);
					if(j != index - 1) {
						writer.write("\n");
					}
				}
				writer.close();
				files.add(f);
			}	
		}
		fr.close();
		return files;
	}
	
	//将两个文件合并成一个文件
	private File merge(File one, File two) throws IOException {
		//采用输出文件名作为文件名前缀
		String[] tmp = this.outFilename.split("/");
		String filename = tmp[tmp.length - 1];
		//文件流
		BufferedReader fis1 = new BufferedReader(new FileReader(one));
		BufferedReader fis2 = new BufferedReader(new FileReader(two));
		File output = new File(workDir + filename + "-merge-" + Math.abs(new Random().nextInt()));
		while(output.exists()) {
			output = new File(workDir + filename + "-split-" + Math.abs(new Random().nextInt()));
		}
		BufferedWriter os = new BufferedWriter(new FileWriter(output));
		String a = fis1.readLine();
		String b = fis2.readLine();
		boolean finished = false;
		while(!finished) {
			/*
			 *  小	大		大	小
			 *  大	  	或		大
			 *  即可以将文件取完，且顺序正确
			 */
			if(a != null && b != null) {
				if(a.compareTo(b) < 0) {
					os.write(a + "\n");
					a = fis1.readLine();
				} else {
					os.write(b + "\n");
					b = fis2.readLine();
				}
			} else if(a == null && b != null) {
				os.write(b + "\n");
				b = fis2.readLine();
			} else if(b == null && a != null) {
				os.write(a + "\n");
				a = fis1.readLine();
			} else {
				finished = true;
			}
		}
		fis1.close();
		fis2.close();
		os.flush();
		os.close();
		return output;
	}
	
	//Usage
	public static void main(String[] args) throws IOException {
		List<String> fileList = new ArrayList<>();
		fileList.add("mrtmp.book.txt-8-1");
		fileList.add("mrtmp.book.txt-9-1");
		ExternalSort sort = new ExternalSort(fileList, "mrtmp.book.txt_sort");
		System.out.println("Original:");
		File f = sort.sort();
		System.out.println("Sorted:");
		System.out.println(f.getName());
	}

}
