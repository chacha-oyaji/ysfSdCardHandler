package net.dialectech.ftmSdCardHandler.supporters.fileSystem;

import lombok.Getter;

/**
 * CYsfFileSystem はファイルシステム管理クラス
 * 
 */
public class CYsfFileSystem extends CYsfFileSystemCorePart {
	
	@Getter
	private static CYsfFileSystem instance = new CYsfFileSystem();
	
	private CYsfFileSystem() {
		super();
		System.out.println("Singleton Constructor: " + this.toString());
	}

}
