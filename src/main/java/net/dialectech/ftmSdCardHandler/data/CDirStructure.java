package net.dialectech.ftmSdCardHandler.data;

import java.io.File;
import java.nio.file.Path;
import java.util.Date;

import lombok.Getter;
import lombok.Setter;

public class CDirStructure {
	@Getter
	@Setter
	private Path folderPath;
	@Getter
	@Setter
	private Date backupDate;

	public CDirStructure(File absoluteFile, Date date) {
		// TODO 自動生成されたコンストラクター・スタブ
		folderPath = absoluteFile.toPath() ;
		backupDate = date ;
	}
	
	public String getDirName() {
		try {
			String dirName  = folderPath.getName(folderPath.getNameCount()-1).toString();
			return dirName ;
		} catch (Exception e) {
			// TODO 自動生成された catch ブロック
			return "Some Kind Of Error";
		}
	}

}
