package net.dialectech.ftmSdCardHandler.data;

import java.io.File;
import java.nio.file.Path;
import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import net.dialectech.ftmSdCardHandler.data.supporters.CDataEntry;

/**
 * CBankEntryは、BANKのエレメントを表現するためのCLASSで、要素としてはfolderPathとbackupDateしか扱わないのだが、
 * 他のEntｒｙと共通して処理する部分があるために、CDataEntryを継承した。
 * 
 */
public class CBankEntry extends CDataEntry {
	@Getter
	@Setter
	private Path folderPath;

	
	public CBankEntry(File absoluteFile, Date date) {
		// TODO 自動生成されたコンストラクター・スタブ
		folderPath = absoluteFile.toPath() ;
		this.baseDate = date ;
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

	@Override
	public void storeOwnData2Buffer() {
		// とりあえず、これは使わないので無視。
	}

	@Override
	public String getRepresentativesName() {
		return getDirName();
	}

	@Override
	public Date getRepresentativeTime() {
		// TODO 自動生成されたメソッド・スタブ
		Date thisDate = new Date(folderPath.toFile().lastModified());
		return thisDate;
	}

}
