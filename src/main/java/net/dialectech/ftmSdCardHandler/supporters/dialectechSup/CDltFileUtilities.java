package net.dialectech.ftmSdCardHandler.supporters.dialectechSup;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;


/**
 * @author ji1bxm
 *
 */
public class CDltFileUtilities {

	private static CDltFileUtilities instance = new CDltFileUtilities();

	/**
	 * 引数で指定したファイルの拡張子を取得する。
	 * @param file
	 * @return
	 */
	public static synchronized String extentionOfPath(File file) {
		String fileName = file.getName().toString();
		return extentionOfPath(fileName) ;
	}

	/**
	 * 引数で指定したファイルの拡張子を取得する。
	 * @param path
	 * @return
	 */
	public static synchronized String extentionOfPath(Path path) {
		String fileName = path.getFileName().toString();
		return extentionOfPath(fileName) ;
	}

	/**
	 * 引数で指定したファイルの拡張子を取得する。
	 * @param fileName
	 * @return
	 */
	public static synchronized String extentionOfPath(String fileName) {
		// TODO 自動生成されたメソッド・スタブ
		Matcher m = Pattern.compile(".+\\.([^\\.]+)$").matcher(fileName);
		if (m.find())
			return m.group(1);
		return "";
	}

	public synchronized static CDltFileUtilities getInstance() {
		return instance;
	}

	/**
	 * 引数で指定した拡張子はテキストを含むファイルの種類を表しているのかどうかを返す。
	 * Solr登録するかどうかを判断するときに利用が可能である。
	 * @param extension
	 * @return
	 */
	public synchronized static boolean includesTextData(String extension) {
		if (extension == null || extension.equals(""))
			return false;
		if ("xml json csv pdf doc docx ppt pptx xls xlsx odt odp ods rtf htm html txt text"
			.contains(extension.toLowerCase()))
			return true;
		return false;
	}
	/**
	 * 指定したディレクトリ名で、ディレクトリがなく、かつ同名のファイルもない場合には、
	 * その名称のディレクトリを生成する。
	 * なお、その名称のディレクトリがない場合で、かつその親ディレクトリもないときであっても、
	 * 親ディレクトリを生成する。
	 * @param dbFile
	 * @throws IOException
	 */
	/*
	private synchronized static void prepareDirectory(String dbFile) throws IOException {
		File dbFileTraget = new File(dbFile);
		if (dbFileTraget.exists() && dbFileTraget.isFile())
			return;
		final File parent = dbFileTraget.getParentFile();
		if (parent == null) {
			return;
		}
		FileUtils.forceMkdir(parent);
	}
	 */
	String fileSeparator = System.getProperties().getProperty("file.separator");
	
	private CDltFileUtilities() {
	}

	public synchronized boolean createDir(Path target, boolean ejectIfExists) {
		if (Files.exists(target) && Files.isDirectory(target)) {
			return true;
		}
		if (Files.exists(target) && !Files.isDirectory(target)) {
			if (ejectIfExists)
				return false;
			try {
				Files.delete(target);
			} catch (IOException e) {
				return false;
			}
		}
		Path parentToBeExisted = target.getParent();
		if (parentToBeExisted == null)
			return false;
		if (createDir(parentToBeExisted, ejectIfExists) == false)
			return false;

		try {
			//System.out.println(target.toUri().toString());
			Files.createDirectory(target);
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	public synchronized boolean forceCopyToDir(Path source, Path targetDir) {
		return forceCopyToDir(source, targetDir, false);
	}
	/**
	 * sourceで規定されたファイルをtargetで示したディレクトリにコピーする。
	 * この際、targetで規程するディレクトリ列と同じ名称のファイルがあったときには、
	 * そのファイルを削除してまでもディレクトリを生成してコピーするので、注意が必要。
	 * 
	 * @param source
	 * @param target
	 * @param ejectIfExists
	 *            true で呼び出したら、もし作成するディレクトリと同一名称のファイルがあったときには、処理を中止して戻る。
	 *            falseで呼び出したら、もし作成するディレクトリと同一名称のファイルがあっても、そのファイルを削除して。
	 * @return
	 */
	public synchronized boolean forceCopyToDir(Path source, Path target, boolean ejectIfExists) {
		if (Files.exists(target) && !Files.isDirectory(target)) {
			if (ejectIfExists)
				return false;
			try {
				Files.delete(target);
			} catch (IOException e) {
				return false;
			}
		}
		if (createDir(target, ejectIfExists) == false)
			return false;
		try {
			Files.copy(source, target.resolve(source.getFileName()), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			return false;
		}
		return true;
	}
	public synchronized void forceDelete(Path source) {
		try {
			if (source!=null) {
				Files.delete(source);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public synchronized void forceDelete(File source) {
		if (source!=null) {
			forceDelete(source.toPath());
		}
	}
	public synchronized void forceDelete(File[] source) {
		if (source!=null) {
			for ( File f : source) {
				forceDelete(f.toPath());
			}
		}
	}
	public synchronized void forceDelete(Path[] source) {
		if (source!=null) {
			for ( Path p : source) {
				forceDelete(p);
			}
		}
	}
	public synchronized boolean forceMove(Path source, Path target, boolean ejectIfExists) {
		if (Files.exists(target) && !Files.isDirectory(target)) {
			if (ejectIfExists)
				return false;
			try {
				Files.delete(target);
			} catch (IOException e) {
				return false;
			}
		}
		try {
			FileUtils.moveFile(source.toFile(), target.toFile());
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	/**
	 * mv src dest/ と等価
	 * src がDirectoryの場合には、その配下のファイル・ディレクトリをdestディレクトリ配下に移送。
	 * src がFileの場合には、そのファイルをdestディレクトリ配下に移送。
	 * destディレクトリがない場合には、作成する。ある場合には、
	 * 
	 * @param src
	 * @param dest
	 * @throws IOException
	 */
	public synchronized void moveRecursiveDirToDir(String src, String dest) throws IOException {
		File srcFile = new File(src);
		if (!srcFile.exists())
			return;
		if (srcFile.isDirectory())
			moveRecursiveDirToDir(src, "", dest);
		else {
			System.out.println("Move file Direct : " + src + " -> " + dest);
			FileUtils.moveFile(srcFile, new File(dest));
		}
	}

	public synchronized void moveRecursiveDirToDir(String srcBase, String srcName, String destBase) {
		File destDir = new File(destBase);
		if (!destDir.exists()) {
			if (destDir.mkdirs() == false) {
				System.out.println("*********************************** " + destBase);
			}
		}
		String srcFullName;
		if (srcName == null || srcName.equals("")) {
			srcFullName = srcBase;
		} else {
			srcFullName = srcBase + fileSeparator + srcName;
		}

		File srcFileSys = new File(srcFullName);
		if (srcFileSys.isDirectory()) {
			File[] fileList = srcFileSys.listFiles();
			for (int i = 0; i < fileList.length; ++i) {
				File srcFile = fileList[i];
				if (srcFile.isDirectory()) {
					String newDestBase = destBase + fileSeparator + srcFile.getName();
					moveRecursiveDirToDir(srcFile.getAbsolutePath(), "", newDestBase);
				} else {
					String targetFileName = destDir.getAbsolutePath() + fileSeparator + srcFile.getName();
					File targetFile = new File(targetFileName);
					if (srcFile.renameTo(targetFile) == false) {
						System.out.println("+++++++++++++++++ " + targetFileName);
					}
					System.out.println("* " + srcFile.getAbsolutePath() + "\n   -> " + targetFileName);
				}
			}
		}
	}
}
