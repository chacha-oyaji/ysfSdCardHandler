package net.dialectech.ftmSdCardHandler.supporters.fileSystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import net.dialectech.ftmSdCardHandler.supporters.CConst;
import net.dialectech.ftmSdCardHandler.supporters.CYsfSdCHandlerProperties;

public class CYsfFileSystemCorePart {

	private CYsfSdCHandlerProperties prop = CYsfSdCHandlerProperties.getInstance();

	private byte[] pictMgr;

	@Getter
	@Setter
	private LinkedList<CImageEntry> pctDirListWithDisplayOrder;
	@Getter
	@Setter
	private LinkedList<CImageEntry> pctDirList;
	File dirPhoto = null;
	@Getter
	@Setter
	File[] includedFileList;
	@Getter
	@Setter
	private CImageManager pctManager;

	@Setter
	@Getter
	private int presentMaxNumberInFileName = 0;

	// sdCardIDは、現在把握しているSD-CARDのIDを記録。mount時にSD-CARDから読み込む。各リクエストを処理するときに直前でSD-CARDのIDを読み込んで、
	// このIDと異なるIDであるSD-CARDであれば、MOUNT処理を経ていないので、エラーとして処理できることになる。
	@Setter
	@Getter
	private String sdCardID = null;

	@Getter
	@Setter
	private boolean active;

	public CYsfFileSystemCorePart() {
		super();
		prop = CYsfSdCHandlerProperties.getInstance();
		System.out.println("coreBase Constructor: " + this.toString());
	}

	public CImageEntry addNewFile(String myCallSign) {
		String newFileName = createNewFileNameByIndex(++presentMaxNumberInFileName);
		CImageEntry newImageDirData = new CImageEntry(prop.getRadioId(), myCallSign, "", newFileName,
				presentMaxNumberInFileName);
		newImageDirData.storeOwnData2BufferedBytes();

		pctDirListWithDisplayOrder.addFirst(newImageDirData);
		pctDirList.addLast(newImageDirData);
		includedFileList = dirPhoto.listFiles();
		reNumberAndPrepareForDisplay();

		return newImageDirData;
	}

	/**
	 * void clearAll()
	 * 
	 * clearAll()は、このファイルシステムオブジェクトと、SD-CARDとの関係性を断ち切る。<br>
	 * 但し、いわゆるREMOVE動作とは異なり、ファイルシステムオブジェクトをSD-CARDに書き込むようなことはしない。
	 * 通常は操作前にチェックするSD-CARD-IDと本ファイルシステムオブジェクトが管理するIDとが不一致の場合、即ち、MOUNTしていない、MOUNTしたが
	 * SD-CARDを無断で抜いたなど、があったときに、このメソッドを呼出し、関連性を断ち切ることになる。
	 */
	public void clearAll() {
		pctDirListWithDisplayOrder = new LinkedList<CImageEntry>();
		pctDirList = new LinkedList<CImageEntry>();
		presentMaxNumberInFileName = 0;
		active = false;
	}

	public String createNewFileNameByIndex(int index) {
		String preCoreName = "000000" + String.valueOf(index);
		String newFileName = prop.getTopLetterOfPhotoFile() + prop.getRadioId()
				+ preCoreName.substring(preCoreName.length() - 6, preCoreName.length()) + ".jpg";
		return newFileName;
	}

	/**
	 * いわゆる、QSOPCTDIRについてデフラグを行う。
	 * 
	 * @return 整理された後に、デフラグされた管理情報で、ファイル名が持つ数値部分のうち、最大となる数値を返す。
	 */
	private int defragmentationOfPctDirList() {

		// ************************************************
		// 最初に完全整理。抜け番号があるときには、順に詰めていく
		// まずは、DIRだけからファイル名インデックスを生成する。

		presentMaxNumberInFileName = 0;
		// new LinkedHashMap<String, LinkedList<CImageEntry>>();
		int efficientDirSize = 0;
		for (CImageEntry data : pctDirList) {
			if (data.isRealFileExists()) {
				efficientDirSize++;
				if (presentMaxNumberInFileName < data.getNumberPartOfFileName()) {
					presentMaxNumberInFileName = data.getNumberPartOfFileName();
				}
			}
		}

		return efficientDirSize;
	}

	/**
	 * 現在対象としているSDCARDがマウントされたものであるのかどうかをチェックする。
	 * 現実には、SDCARD-IDファイルの内容と本ファイルシステム管理オブジェクトが把握しているIDとが一致しているかどうかをチェックする。
	 * 
	 * @return
	 */
	public boolean isSdCardMounted() {
		if (!active)
			return false;
		String presentUsingSdCardId = null;
		CYsfSdCHandlerProperties prop = CYsfSdCHandlerProperties.getInstance();

		Path usingSDCardIDName = Paths.get(prop.getStrSdCardIdHolderPath());
		if (usingSDCardIDName.toFile().exists()) {
			try {
				List<String> presentBranchName = Files.readAllLines(usingSDCardIDName, StandardCharsets.UTF_8);
				presentUsingSdCardId = presentBranchName.getFirst();
				if (sdCardID != null && presentUsingSdCardId != null && sdCardID.equals(presentUsingSdCardId)) {
					return true;
				}
			} catch (IOException e) {
				return false;
			}
		}
		return false;
	}

	/**
	 * 
	 * @param params
	 * @return 正常終了ならnull、エラーがあったら、その内容
	 */
	public String loadFromSDCard() {
		presentMaxNumberInFileName = 0;

		File qsoPctDir;
		qsoPctDir = new File(prop.getStrQsoPctDirFilePath());
		if (!qsoPctDir.exists())
			return "QSOPCTDIR.datが見つかりません。";

		File qsoPctFat;
		qsoPctFat = new File(prop.getStrQsoPctFatFilePath());
		if (!qsoPctFat.exists())
			return "QSOPCTFAT.datが見つかりません";

		File qsoMgr;
		qsoMgr = new File(prop.getStrQsoMngFilePath());
		if (!qsoMgr.exists())
			return "QSOMNG.datが見つかりません";

		dirPhoto = new File(prop.getStrPhotoDirectoryPath());
		if (!dirPhoto.exists())
			return "PHOTOディレクトリが見つかりません";

		try (FileInputStream fisPctDir = new FileInputStream(qsoPctDir);
				FileInputStream fisPctFat = new FileInputStream(qsoPctFat);
				FileInputStream fisMgr = new FileInputStream(qsoMgr)) {

			pctDirListWithDisplayOrder = new LinkedList<CImageEntry>();
			pctDirList = new LinkedList<CImageEntry>();

			// まずはfatを取り込んで、チェッカーを生成する。
			// fat の容量(バイト量でなく、情報数で表している。)から全体のDIRエントリ数を把握してみる。

			int dirEntryVolume = (int) ((qsoPctFat.length() & 0xffff) / CConst.BYTESIZE_PER_FAT); // FATでは１エントリあたり「４」バイトで表現される。
			LinkedHashMap<Integer, Boolean> fatCheckMap = new LinkedHashMap<Integer, Boolean>(); // キーはディレクトリエントリの開始番地（絶対番地）
			for (int index = 0; index < dirEntryVolume; ++index) {
				byte[] pictFatImage = fisPctFat.readNBytes(CConst.BYTESIZE_PER_FAT);
				if (pictFatImage.length == 0)
					break;

				int startAddress = (int) ((pictFatImage[1] << 16) & 0x00ff0000)
						+ (int) ((pictFatImage[2] << 8) & 0xff00) + (int) (pictFatImage[3] & 0xff);
				if (startAddress == 0xffffff) {
					if (fatCheckMap.get(index * CConst.BYTESIZE_PER_DIR_ENTRY) == null)
						fatCheckMap.put(index * CConst.BYTESIZE_PER_DIR_ENTRY, false);
					else
						System.out.println("FAT CORRUPSED? Duplicate location data.");
				} else
					fatCheckMap.put(startAddress, true);
			}

			// 次にdirを読みこむ。特にFATとの整合性は求めずに、QSOPCTDIRのみで構成する。
			for (int index = 0;; ++index) {
				byte[] pictDirectories = fisPctDir.readNBytes(CConst.BYTESIZE_PER_DIR_ENTRY);
				if (pictDirectories.length == 0) {
					// データをすいだせなかったら、もうこれ以上の情報はなし。
					break;
				}
				// まずは管理データを生成。
				CImageEntry ie = new CImageEntry(pictDirectories, index);
				// 生成されたデータが新規ファイルで、ファイル名中の数値が従来分より大きかったら、このファイルシステム中での最大値として記録される。
				if (ie.getNumberPartOfFileName() > presentMaxNumberInFileName) {
					presentMaxNumberInFileName = ie.getNumberPartOfFileName();
				}

				int dirPositionAddress = index * CConst.BYTESIZE_PER_DIR_ENTRY;
				Boolean dataExist = fatCheckMap.get(dirPositionAddress);
				if (dataExist == null) {
					ie.setAbsDirPos(-1); // ファイルがないなら、絶対番地を-1に設定する。
				} else {
					ie.setAbsDirPos(index * CConst.BYTESIZE_PER_DIR_ENTRY);
					if (dataExist) {
						ie.setActive(true);
					} else {
						ie.setActive(false);
					}
				}
				pctDirListWithDisplayOrder.add(ie);
				pctDirList.add(ie);
			}

			// 最後に、pctMgrを読み込む。
			pictMgr = fisMgr.readAllBytes();
		} catch (IOException e) {
			// TODO
			e.printStackTrace();
		}

		// PHOTOディレクトリ内にあるファイル群とｐｃｒDirListとの間の整合性チェック。
		includedFileList = dirPhoto.listFiles();
		if (includedFileList != null) {
			for (CImageEntry pctDir : pctDirList) {
				pctDir.setRealFileExists(false);
				for (File f : includedFileList) {
					if (f.getName().equals(pctDir.getFileCoreName())) {
						pctDir.setRealFileExists(true);
						break;
					}
				}
			}
		}

		// pctDirListWithOrderは時間でソーティングするが、pctDirListの方は配置の通りにするので、弄らない。

		// 次に、二重管理チェック。()

		reNumberAndPrepareForDisplay();
		return null;
	}

	public String loadFromTargetBankDirFiles(String sourceDirectory) {
		presentMaxNumberInFileName = 0;

		File qsoPctDir;
		qsoPctDir = new File(sourceDirectory + CConst.STR_QSOPCTDIR);
		if (!qsoPctDir.exists())
			return "loadFromTargetBankDirFiles::QSOPCTDIR.datが見つかりません。";

		File qsoPctFat;
		qsoPctFat = new File(sourceDirectory + CConst.STR_QSOPCTFAT);
		if (!qsoPctFat.exists())
			return "loadFromTargetBankDirFiles::QSOPCTFAT.datが見つかりません";

		try (FileInputStream fisPctDir = new FileInputStream(qsoPctDir);
				FileInputStream fisPctFat = new FileInputStream(qsoPctFat)) {

			pctDirListWithDisplayOrder = new LinkedList<CImageEntry>();
			pctDirList = new LinkedList<CImageEntry>();

			// まずはfatを取り込んで、チェッカーを生成する。
			// fat の容量(バイト量でなく、情報数で表している。)から全体のDIRエントリ数を把握してみる。

			int dirEntryVolume = (int) ((qsoPctFat.length() & 0xffff) / CConst.BYTESIZE_PER_FAT); // FATでは１エントリあたり「４」バイトで表現される。
			LinkedHashMap<Integer, Boolean> fatCheckMap = new LinkedHashMap<Integer, Boolean>(); // キーはディレクトリエントリの開始番地（絶対番地）
			for (int index = 0; index < dirEntryVolume; ++index) {
				byte[] pictFatImage = fisPctFat.readNBytes(CConst.BYTESIZE_PER_FAT);
				if (pictFatImage.length == 0)
					break;

				int startAddress = (int) ((pictFatImage[1] << 16) & 0x00ff0000)
						+ (int) ((pictFatImage[2] << 8) & 0xff00) + (int) (pictFatImage[3] & 0xff);
				if (startAddress == 0xffffff) {
					if (fatCheckMap.get(index * CConst.BYTESIZE_PER_DIR_ENTRY) == null)
						fatCheckMap.put(index * CConst.BYTESIZE_PER_DIR_ENTRY, false);
					else
						System.out.println("FAT CORRUPSED? Duplicate location data.");
				} else
					fatCheckMap.put(startAddress, true);
			}

			// 次にdirを読みこむ。特にFATとの整合性は求めずに、QSOPCTDIRのみで構成する。
			for (int index = 0;; ++index) {
				byte[] pictDirectories = fisPctDir.readNBytes(CConst.BYTESIZE_PER_DIR_ENTRY);
				if (pictDirectories.length == 0) {
					// データをすいだせなかったら、もうこれ以上の情報はなし。
					break;
				}
				// まずは管理データを生成。
				CImageEntry ie = new CImageEntry(pictDirectories, index);
				// 生成されたデータが新規ファイルで、ファイル名中の数値が従来分より大きかったら、このファイルシステム中での最大値として記録される。
				if (ie.getNumberPartOfFileName() > presentMaxNumberInFileName) {
					presentMaxNumberInFileName = ie.getNumberPartOfFileName();
				}

				int dirPositionAddress = index * CConst.BYTESIZE_PER_DIR_ENTRY;
				Boolean dataExist = fatCheckMap.get(dirPositionAddress);
				if (dataExist == null) {
					ie.setAbsDirPos(-1); // ファイルがないなら、絶対番地を-1に設定する。
				} else {
					ie.setAbsDirPos(index * CConst.BYTESIZE_PER_DIR_ENTRY);
					if (dataExist) {
						ie.setActive(true);
					} else {
						ie.setActive(false);
					}
				}
				pctDirListWithDisplayOrder.add(ie);
				pctDirList.add(ie);
			}

		} catch (IOException e) {
			// TODO
			e.printStackTrace();
		}

		// PHOTOディレクトリ内にあるファイル群とｐｃｒDirListとの間の整合性チェック。
		File[] includedFileList;
		
		includedFileList = new File(sourceDirectory).listFiles();
		if (includedFileList != null) {
			for (CImageEntry pctDir : pctDirList) {
				pctDir.setRealFileExists(false);
				for (File f : includedFileList) {
					if (f.getName().equals(pctDir.getFileCoreName())) {
						pctDir.setRealFileExists(true);
						break;
					}
				}
			}
		}

		// pctDirListWithOrderは時間でソーティングするが、pctDirListの方は配置の通りにするので、弄らない。

		// 次に、二重管理チェック。()

		reNumberAndPrepareForDisplay();
		return null;
	}

	private void markDuplicateFileHandling() {
		LinkedHashMap<String, CImageEntry> realFileName2DuplicationCheck = new LinkedHashMap<String, CImageEntry>(); // ファイル名、pctDirEntry実体
		CImageEntry matchedData;
		for (CImageEntry ie : pctDirList) {
			if (ie.getImageId() == -1)
				continue;
			ie.setDuplicateOf(null);
			if ((matchedData = realFileName2DuplicationCheck.get(ie.getFileCoreName())) != null) {
				ie.setDuplicateOf(String.valueOf(matchedData.getImageId()));
			} else {
				realFileName2DuplicationCheck.put(ie.getFileCoreName(), ie);
			}
		}

	}

	public void removeThis(CImageEntry data) {
		for (CImageEntry d : pctDirList) {
			if (d==data) {
				pctDirList.remove(data);
				break ;
			}
		}
		for (CImageEntry d : pctDirListWithDisplayOrder) {
			if (d==data) {
				pctDirListWithDisplayOrder.remove(data);
				break ;
			}
		}
	}

	/**
	 * reNumbering
	 * reNumberingは、pctDirListから、管理対象を現存するファイルに対するもののみを抽出し、pctDirListWithDisplayOrderへの転写を行う。
	 * 
	 * 但し、転写の際には、新しいファイルを前、古いファイルが後になるように、逆順ソートを行う。
	 * 
	 */
	public void reNumberAndPrepareForDisplay() {
		// この時点で、pctDirListからpctDirListWithDisplayOrderにデータを移動させた上で、ソーティングしてから管理ＩＤを振ることにする。
		pctDirListWithDisplayOrder = new LinkedList<CImageEntry>();
		for (CImageEntry elem : pctDirList) {
			if (elem.isRealFileExists())
				pctDirListWithDisplayOrder.addFirst(elem);
		}

		Collections.sort(pctDirListWithDisplayOrder, new Comparator<CImageEntry>() {

			/**
			 * ファイル名のうち、数値部分で比較する。ファイル名自体で比較しなかったのは、異なるリグ間で同じSD-CARDを使いまわしたときでも
			 * 混乱しないようにするため。
			 */
			@Override
			public int compare(CImageEntry o1, CImageEntry o2) { // ファイル名の若いものが後
				// int fileNameComparizon =
				// o1.getFileCoreName().compareTo(o2.getFileCoreName());
				int fileNameComparizon = 0;
				if (o1.getNumberPartOfFileName() > o2.getNumberPartOfFileName())
					return -1;
				if (o1.getNumberPartOfFileName() < o2.getNumberPartOfFileName())
					return 1;

				if (fileNameComparizon == 0) { // ファイル名同一ならば、受信時が早いものが後
					if (o1.getDate2Receive().after(o2.getDate2Receive()))
						return -1;
					else if (o1.getDate2Receive().before(o2.getDate2Receive()))
						return 1;
					else
						return 0;
				} else
					return -(fileNameComparizon);
			}
		});

		// Browserから画像を特定するための管理番号を再付与する。
		int index = 0;
		for (CImageEntry elem : pctDirListWithDisplayOrder) {
			elem.setImageId(++index);
			elem.storeOwnData2BufferedBytes();
		}
		// 重複管理のマーキングをする。
		markDuplicateFileHandling();
	}
	

	public void saveAll(LinkedList<String> errorMessageList) {
		CYsfSdCHandlerProperties prop = CYsfSdCHandlerProperties.getInstance();

		if (errorMessageList == null || errorMessageList.size() > 0) {
			return;
		}
		if (!isActive()) {
			errorMessageList.add("先にMOUNT/LOADを実行してください。");
			return;
		}

		File qsoPctDir;
		qsoPctDir = new File(prop.getStrQsoPctDirFilePath());
		if (!qsoPctDir.exists()) {
			errorMessageList.add("QSOPCTDIR.datが見当たりません。");
			return;
		}

		File qsoPctMgr;
		qsoPctMgr = new File(prop.getStrQsoMngFilePath());
		if (!qsoPctMgr.exists()) {
			errorMessageList.add("QSOMNG.datが見当たりません。");
			return;
		}

		File qsoPctFat;
		qsoPctFat = new File(prop.getStrQsoPctFatFilePath());
		if (!qsoPctFat.exists()) {
			errorMessageList.add("QSOPCTFAT.datが見当たりません。");
			return;
		}

		int efficientDirSize = defragmentationOfPctDirList();

		try (FileOutputStream fos4Dir = new FileOutputStream(qsoPctDir);
				FileOutputStream fos4Fat = new FileOutputStream(qsoPctFat);
				FileOutputStream fos4Mng = new FileOutputStream(qsoPctMgr)) {

			// QSOPCTFAT再生成
			byte[] fatData = new byte[4 * efficientDirSize];
			int index = 0;
			int indexCount = 0;
			int presentWholeDirVolume = pctDirList.size();
			for (int i = 0; i < presentWholeDirVolume; ++i) {
				CImageEntry target = pctDirList.get(i);
				if (target.isRealFileExists()) {
					if (target.isActive()) {
						fatData[index++] = 0x40;
						fatData[index++] = (byte) (((indexCount * 128) & 0xff0000) >> 16);
						fatData[index++] = (byte) (((indexCount * 128) & 0xff00) >> 8);
						fatData[index++] = (byte) ((indexCount * 128) & 0xff);
						target.setImageId(indexCount + 1);
					} else {
						fatData[index++] = (byte) 0xff;
						fatData[index++] = (byte) 0xff;
						fatData[index++] = (byte) 0xff;
						fatData[index++] = (byte) 0xff;
					}
					indexCount++;
				}
			}
			fos4Fat.write(fatData);

			// QSOPCTDIRの生成
			for (CImageEntry elem : pctDirList) {
				if (elem.isRealFileExists()) {
					elem.storeOwnData2BufferedBytes();
					fos4Dir.write(elem.getImageEntry());
				}
			}

			// QSOPCTMNGの生成
			// 次の書き込みファイル名が決定されるので、これで対応。デフラグしないなら、これも書き換える必要がある。
			int nextPointer2Write = presentMaxNumberInFileName + 1;
			pictMgr[0x12] = (byte) (((nextPointer2Write + 1) >> 8) & 0xff);
			pictMgr[0x13] = (byte) (nextPointer2Write & 0xff);

			if (efficientDirSize > 0) {
				pictMgr[0x10] = (byte) ((efficientDirSize >> 8) & 0xff);
				pictMgr[0x11] = (byte) (efficientDirSize & 0xff);
			} else {
				pictMgr[0x10] = (byte) 0xff;
				pictMgr[0x11] = (byte) 0xff;
			}
			fos4Mng.write(pictMgr);

		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			System.out.println("ERROR : " + e.getLocalizedMessage());
		}
	}

}
