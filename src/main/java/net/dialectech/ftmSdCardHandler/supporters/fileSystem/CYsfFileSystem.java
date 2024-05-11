package net.dialectech.ftmSdCardHandler.supporters.fileSystem;

import lombok.Getter;

/**
 * CYsfFileSystem はSD-CARD用のファイルシステム管理クラス
 * 　なお、各BANKについても同様にひとつのBANKにひとつのファイルシステム管理を行うようにするものの、
 * それは、CYsfFileSystemCorePartクラスで管理する。このCYsfFileSystemはあくまでもひとつの
 * SD-CARDに対して処理するものなので、Singletonとしている。
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
