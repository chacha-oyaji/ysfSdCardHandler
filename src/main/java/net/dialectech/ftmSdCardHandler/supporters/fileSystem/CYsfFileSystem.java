package net.dialectech.ftmSdCardHandler.supporters.fileSystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import lombok.Getter;
import lombok.Setter;
import net.dialectech.ftmSdCardHandler.supporters.CConst;
import net.dialectech.ftmSdCardHandler.supporters.CYsfSdCHandlerProperties;

/**
 * CYsfFileSystem はSD-CARD用のファイルシステム管理クラス
 * なお、各BANKについても同様にひとつのBANKにひとつのファイルシステム管理を行うようにするものの、
 * それは、CYsfFileSystemCorePartクラスで管理する。このCYsfFileSystemはあくまでもひとつの
 * SD-CARDに対して処理するものなので、Singletonとしている。
 * 
 */
public class CYsfFileSystem extends CYsfFileSystemCorePart {

	@Getter
	private static CYsfFileSystem instance = new CYsfFileSystem();

	// 本ファイルシステムがマウントされているかどうかを示す。但し、SingletonとなっているCYsfFileSystemクラスの場合に限るので、こちらで定義する。
	private boolean mouonted = false;

	/*
	 * MESSAGEの処理と、VOICEの処理はBANKで扱わないので、CYsfFileSystemCorePartクラスでは扱わず、こちらで一括して処理することにした。
	 */

	// MESSAGE関連のデータ列
	@Getter
	@Setter
	protected LinkedList<CMessageEntry> msgDirListWithDisplayOrder;
	@Getter
	@Setter
	protected LinkedList<CMessageEntry> msgDirList;
	@Setter
	@Getter
	protected int presentMaxNumberInMessage = 0;

	// VOICE関連のデータ列
	private byte[] waveMgr; // WAVMGRファイル内のデータをそのまま保持
	@Getter
	@Setter
	private LinkedList<CVoiceEntry> voiceDirListWithDisplayOrder;
	@Getter
	@Setter
	private LinkedList<CVoiceEntry> voiceDirList;
	File voiceFolder = null;
	private byte[] voiceMgr; // QSOMGRファイル内のデータをそのまま保持

	// sdCardIDは、現在把握しているSD-CARDのIDを記録。mount時にSD-CARDから読み込む。各リクエストを処理するときに直前でSD-CARDのIDを読み込んで、
	// このIDと異なるIDであるSD-CARDであれば、MOUNT処理を経ていないので、エラーとして処理できることになる。
	@Setter
	@Getter
	protected String sdCardID = null;

	private CYsfFileSystem() {
		super();
		System.out.println("Singleton Constructor: " + this.toString());
	}

	public CImageEntry addNewFile(String myCallSign, String description2Change) {
		String newFileName = createNewFileNameByIndex(++presentMaxNumberInPictFileName);
		CImageEntry newImageDirData = new CImageEntry(prop.getRadioId(), myCallSign, "", newFileName,
				presentMaxNumberInPictFileName, description2Change);
		newImageDirData.storeOwnData2Buffer();

		pctDirListWithDisplayOrder.addFirst(newImageDirData);
		pctDirList.addLast(newImageDirData);
		includedFileList = photoFolder.listFiles();
		reNumberAndPrepareForPictureDisplay();

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
		msgDirListWithDisplayOrder = new LinkedList<CMessageEntry>();
		msgDirList = new LinkedList<CMessageEntry>();
		presentMaxNumberInPictFileName = 0;
		mouonted = false;
	}

	public String createNewFileNameByIndex(int index) {
		String preCoreName = "000000" + String.valueOf(index);
		String newFileName = prop.getTopLetterOfPhotoFile() + prop.getRadioId()
				+ preCoreName.substring(preCoreName.length() - 6, preCoreName.length()) + ".jpg";
		return newFileName;
	}

	/**
	 * QSOPCTDIRについてデフラグを行う。これにより、FATを生成するためのバイト数を決定できるようになる。
	 * 
	 * @return 整理された後に、デフラグされた管理情報（実ファイルが存在するものを対象とした管理情報）のエントリ数
	 */
	private int defragmentationOfPctDirList() {

		// ************************************************
		// 最初に完全整理。抜け番号があるときには、順に詰めていく
		// まずは、DIRだけからファイル名インデックスを生成する。

		presentMaxNumberInPictFileName = 0;
		// new LinkedHashMap<String, LinkedList<CImageEntry>>();
		int efficientDirSize = 0;
		for (CImageEntry data : pctDirList) {
			if (data.isRealFileExists()) {
				efficientDirSize++;
				if (presentMaxNumberInPictFileName < data.getNumberPartOfFileName()) {
					presentMaxNumberInPictFileName = data.getNumberPartOfFileName();
				}
			}
		}

		return efficientDirSize;
	}

	/**
	 * QSOWAVDIRについてデフラグを行う。これにより、FATを生成するためのバイト数を決定できるようになる。
	 * 
	 * @return 整理された後に、デフラグされた管理情報（実ファイルが存在するものを対象とした管理情報）のエントリ数
	 */
	private int defragmentationOfVoiceDirList() {

		// ************************************************
		// 最初に完全整理。抜け番号があるときには、順に詰めていく
		// まずは、DIRだけからファイル名インデックスを生成する。

		int efficientDirSize = 0;
		for (CVoiceEntry data : voiceDirList) {
			if (data.isRealFileExists()) {
				efficientDirSize++;
			}
		}

		return efficientDirSize;
	}

	public synchronized boolean isMounted() {
		return mouonted;
	}

	/**
	 * 現在対象としているSDCARDがマウントされたものであるのかどうかをチェックする。
	 * 現実には、SDCARD-IDファイルの内容と本ファイルシステム管理オブジェクトが把握しているIDとが一致しているかどうかをチェックする。
	 * 
	 * @return
	 */
	public boolean isSdCardMounted() {
		if (!mouonted)
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

		presentMaxNumberInPictFileName = 0;

		File qsoPctDir;
		qsoPctDir = new File(prop.getStrQsoPctDirFilePath());
		if (!qsoPctDir.exists())
			return "QSOPCTDIR.datが見つかりません。";

		File qsoPctFat;
		qsoPctFat = new File(prop.getStrQsoPctFatFilePath());
		if (!qsoPctFat.exists())
			return "QSOPCTFAT.datが見つかりません";

		File qsoMsgDir;
		qsoMsgDir = new File(prop.getStrQsoMsgDirFilePath());
		if (!qsoMsgDir.exists()) {
			return "QSOMSGDIR.datが見当たりません。";
		}

		File qsoMsgFat;
		qsoMsgFat = new File(prop.getStrQsoMsgFatFilePath());
		if (!qsoMsgFat.exists()) {
			return "QSOMSGFAT.datが見当たりません。";
		}

		File qsoMsg;
		qsoMsg = new File(prop.getStrQsoMsgFilePath());
		if (!qsoMsg.exists()) {
			return "QSOMSG.datが見当たりません。";
		}

		File qsoVoiceDir;
		qsoVoiceDir = new File(prop.getStrQsoVoiceDirFilePath());
		if (!qsoVoiceDir.exists()) {
			return "QSOWAVDIR.datが見当たりません。";
		}

		File qsoVoiceFat;
		qsoVoiceFat = new File(prop.getStrQsoVoiceFatFilePath());
		if (!qsoVoiceFat.exists()) {
			return "QSOWAVFAT.datが見当たりません。";
		}

		File qsoMgrFile;
		qsoMgrFile = new File(prop.getStrQsoMngFilePath());
		if (!qsoMgrFile.exists())
			return "QSOMNG.datが見つかりません";

		File voiceMngFile;
		voiceMngFile = new File(prop.getStrQsoVoiceMngFilePath());
		if (!voiceMngFile.exists())
			return "WAVMNG.datが見つかりません";

		photoFolder = new File(prop.getStrPhotoDirectoryPath());
		if (!photoFolder.exists())
			return "PHOTOディレクトリが見つかりません";

		voiceFolder = new File(prop.getStrVoiceDirectoryPath());
		if (!photoFolder.exists())
			return "VOICEディレクトリが見つかりません";

		// *********************************************************************************************************
		// 最初に QSOMGRの取り込みを行う。
		// *********************************************************************************************************

		try (FileInputStream fisMgr = new FileInputStream(qsoMgrFile)) {
			generalMgr = fisMgr.readAllBytes();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		// *********************************************************************************************************
		// 次に WAVMGRの取り込みを行う。
		// *********************************************************************************************************

		try (FileInputStream fisMgr = new FileInputStream(voiceMngFile)) {
			voiceMgr = fisMgr.readAllBytes();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		// *********************************************************************************************************
		// ここから、Pictures(Photo/Images)関連処理を行う。
		// *********************************************************************************************************

		try (FileInputStream fisPctDir = new FileInputStream(qsoPctDir);
				FileInputStream fisPctFat = new FileInputStream(qsoPctFat)) {

			pctDirList = new LinkedList<CImageEntry>(); // こちらは記録順を維持しつつ、dirContentsを記録する。
			pctDirListWithDisplayOrder = new LinkedList<CImageEntry>(); // こちらは表示順を維持しつつ、dirContentsを記録する。

			// まずはfatを取り込んで、チェッカーを生成する。
			// fat の容量(バイト量でなく、情報数で表している。)から全体のDIRエントリ数を把握してみる。

			int dirEntryVolume = (int) ((qsoPctFat.length() & 0xffffff) / CConst.BYTESIZE_PER_FAT); // FATでは１エントリあたり「４」バイトで表現される。
			LinkedHashMap<Integer, Boolean> fatCheckMap = new LinkedHashMap<Integer, Boolean>(); // キーはディレクトリエントリの開始番地（絶対番地）
			for (int index = 0; index < dirEntryVolume; ++index) {
				byte[] pictFatImage = fisPctFat.readNBytes(CConst.BYTESIZE_PER_FAT);
				if (pictFatImage.length == 0)
					break;

				int startAddressOrDirectoryEntry = (int) ((pictFatImage[1] << 16) & 0x00ff0000)
						+ (int) ((pictFatImage[2] << 8) & 0xff00) + (int) (pictFatImage[3] & 0xff);
				if (startAddressOrDirectoryEntry == 0xffffff) {
					if (fatCheckMap.get(index * CConst.BYTESIZE_PER_DIR_ENTRY) == null)
						fatCheckMap.put(index * CConst.BYTESIZE_PER_DIR_ENTRY, false);
					else
						System.out.println("Picture FAT CORRUPSED? Duplicate location data.");
				} else
					fatCheckMap.put(startAddressOrDirectoryEntry, true);
			}

			// 次にdirを読みこむ。特にFATとの整合性は求めずに、QSOPCTDIRで構成する。
			for (int index = 0;; ++index) {
				byte[] pictDirectories = fisPctDir.readNBytes(CConst.BYTESIZE_PER_DIR_ENTRY);
				if (pictDirectories.length == 0) {
					// データをすいだせなかったら、もうこれ以上の情報はなし。
					break;
				}
				// まずは管理データを生成。
				CImageEntry ie = new CImageEntry(pictDirectories, index);

				// 生成されたデータが新規ファイルで、ファイル名中の数値が従来分より大きかったら、このファイルシステム中での最大値として記録される。
				if (ie.getNumberPartOfFileName() > presentMaxNumberInPictFileName) {
					presentMaxNumberInPictFileName = ie.getNumberPartOfFileName();
				}

				// fat内に見当たらないなら、削除したものとして取り扱う。FAT内に対象データがあっても、「削除扱い」であったら、activeフラグをfalseにする。
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
		includedFileList = photoFolder.listFiles();
		if (includedFileList != null) {
			for (CImageEntry pctDir : pctDirList) {
				pctDir.setRealFileExists(false);
				String fileName = prop.getSdCardBaseDirName() + CConst.PhotoFoldername + File.separator
						+ pctDir.getFileCoreName();
				pctDir.setQrString(analyzeQRCode(fileName));
				for (File f : includedFileList) {
					if (f.getName().equals(pctDir.getFileCoreName())) {
						pctDir.setRealFileExists(true);
						break;
					}
				}
			}
		}
		// 次に、PICT絡みでの二重管理チェック。()
		reNumberAndPrepareForPictureDisplay();

		// *********************************************************************************************************
		// ここから、Message関連処理を行う。
		// *********************************************************************************************************

		try (FileInputStream fisMsgDir = new FileInputStream(qsoMsgDir);
				FileInputStream fisMsgFat = new FileInputStream(qsoMsgFat);
				FileInputStream fisMsg = new FileInputStream(qsoMsg);) {

			msgDirListWithDisplayOrder = new LinkedList<CMessageEntry>();
			msgDirList = new LinkedList<CMessageEntry>();

			// まずはfatを取り込んで、チェッカーを生成する。
			// fat の容量(バイト量でなく、情報数で表している。)から全体のDIRエントリ数を把握してみる。
			int dirEntryVolume = (int) ((qsoMsgFat.length() & 0xffffff) / CConst.BYTESIZE_PER_FAT); // FATでは１エントリあたり「４」バイトで表現される。
			LinkedHashMap<Integer, Boolean> fatCheckMap = new LinkedHashMap<Integer, Boolean>(); // キーはディレクトリエントリの開始番地（絶対番地）
			for (int index = 0; index < dirEntryVolume; ++index) {
				byte[] msgFat = fisMsgFat.readNBytes(CConst.BYTESIZE_PER_FAT);
				if (msgFat.length == 0)
					break;

				int startAddressOrDirectoryEntry = (int) ((msgFat[1] << 16) & 0x00ff0000)
						+ (int) ((msgFat[2] << 8) & 0xff00) + (int) (msgFat[3] & 0xff);
				if (startAddressOrDirectoryEntry == 0xffffff) {
					if (fatCheckMap.get(index * CConst.BYTESIZE_PER_DIR_ENTRY) == null)
						fatCheckMap.put(index * CConst.BYTESIZE_PER_DIR_ENTRY, false);
					else
						System.out.println("Message FAT CORRUPSED? Duplicate location data.");
				} else
					fatCheckMap.put(startAddressOrDirectoryEntry, true);
			}

			// 全「メッセージ」の本体を一気読みしてwholeMessageDataに記録する。
			byte[] wholeMessageData = fisMsg.readAllBytes();

			// 次にdirを読みこむ。エントリは、QSOMSGDIRのみから決する。
			// 続いて、各メッセージの管理データを記録する。
			for (int index = 0;; ++index) {
				// Directoryの１エントリ分を取得する。
				byte[] msgDirectories = fisMsgDir.readNBytes(CConst.BYTESIZE_PER_DIR_ENTRY);
				if (msgDirectories.length == 0) {
					// データをすいだせなかったら、もうこれ以上の情報はなし。
					break;
				}
				// まずは管理データを生成。
				CMessageEntry messageEntry = new CMessageEntry(msgDirectories, index, wholeMessageData);

				// 生成されたデータが新規ファイルで、ファイル名中の数値が従来分より大きかったら、このファイルシステム中での最大値として記録される。

				int dirPositionAddress = index * CConst.BYTESIZE_PER_DIR_ENTRY;
				Boolean dataExist = fatCheckMap.get(dirPositionAddress);
				if (dataExist == null) {
					messageEntry.setAbsDirPos(-1); // ファイルがないなら、絶対番地を-1に設定する。
				} else {
					messageEntry.setAbsDirPos(index * CConst.BYTESIZE_PER_DIR_ENTRY);
					if (dataExist) {
						messageEntry.setActive(true);
					} else {
						messageEntry.setActive(false);
					}
				}
				msgDirListWithDisplayOrder.addFirst(messageEntry);
				msgDirList.add(messageEntry);
			}

		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

		// *********************************************************************************************************
		// 最後に、Voices関連処理を行う。
		// *********************************************************************************************************

		try (FileInputStream fisVoiceDir = new FileInputStream(qsoVoiceDir);
				FileInputStream fisVoiceFat = new FileInputStream(qsoVoiceFat);
				FileInputStream fisMng = new FileInputStream(voiceMngFile);) {

			voiceDirListWithDisplayOrder = new LinkedList<CVoiceEntry>();
			voiceDirList = new LinkedList<CVoiceEntry>();

			// まずはfatを取り込んで、チェッカーを生成する。
			// fat の容量(バイト量でなく、情報数で表している。)から全体のDIRエントリ数を把握してみる。
			int dirEntryVolume = (int) ((qsoVoiceFat.length() & 0xffffff) / CConst.BYTESIZE_PER_FAT); // FATでは１エントリあたり「４」バイトで表現される。
			LinkedHashMap<Integer, Boolean> fatCheckMap = new LinkedHashMap<Integer, Boolean>(); // キーはディレクトリエントリの開始番地（絶対番地）
			for (int index = 0; index < dirEntryVolume; ++index) {
				byte[] voiceFat = fisVoiceFat.readNBytes(CConst.BYTESIZE_PER_FAT);
				if (voiceFat.length == 0)
					break;

				int startAddressOrDirectoryEntry = (int) ((voiceFat[1] << 16) & 0x00ff0000)
						+ (int) ((voiceFat[2] << 8) & 0xff00) + (int) (voiceFat[3] & 0xff);
				if (startAddressOrDirectoryEntry == 0xffffff) {
					if (fatCheckMap.get(index * CConst.BYTESIZE_PER_DIR_ENTRY) == null)
						fatCheckMap.put(index * CConst.BYTESIZE_PER_DIR_ENTRY, false);
					else
						System.out.println("Message FAT CORRUPSED? Duplicate location data.");
				} else
					fatCheckMap.put(startAddressOrDirectoryEntry, true);
			}
			// 次にdirを読みこむ。エントリは、QSOWAVDIRのみから決する。
			// 続いて、各メッセージの管理データを記録する。
			for (int index = 0;; ++index) {
				// Directoryの１エントリ分を取得する。
				byte[] voiceDirectories = fisVoiceDir.readNBytes(CConst.BYTESIZE_PER_DIR_ENTRY);
				if (voiceDirectories.length == 0) {
					// データをすいだせなかったら、もうこれ以上の情報はなし。
					break;
				}
				// まずは管理データを生成。
				CVoiceEntry voiceEntry = new CVoiceEntry(voiceDirectories, index + 1);

				int dirPositionAddress = index * CConst.BYTESIZE_PER_DIR_ENTRY;
				Boolean dataExist = fatCheckMap.get(dirPositionAddress);
				if (dataExist == null) {
					voiceEntry.setAbsDirPos(-1); // FATに対応エントリがないなら、絶対番地を-1に設定する。
				} else {
					voiceEntry.setAbsDirPos(index * CConst.BYTESIZE_PER_DIR_ENTRY);
					if (dataExist) {
						voiceEntry.setActive(true);
					} else {
						voiceEntry.setActive(false);
					}
				}
				voiceEntry.setDataId(index+1);
				// 次にここで仮設定したDirEntryにおいて対応させられている音声情報が確定したので、これをエントリさせる。
				voiceDirList.add(voiceEntry);
				voiceDirListWithDisplayOrder.addFirst(voiceEntry);
			}

			// VOICEフォルダ内にあるファイル群とvoiceDirListとの間の整合性チェック。
			includedFileList = voiceFolder.listFiles();
			if (includedFileList != null) {
				for (CVoiceEntry voiceDir : voiceDirList) {
					voiceDir.setRealFileExists(false);
					for (File f : includedFileList) {
						if (f.getName().equals(voiceDir.getFileName())) {
							voiceDir.setRealFileExists(true);
							voiceDir.setFileSize(f.length());
							break;
						}
					}
				}
			}
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

		return null;
	}

	public void saveAllOfFilesOnMessage(LinkedList<String> errorMessageList) {
		CYsfSdCHandlerProperties prop = CYsfSdCHandlerProperties.getInstance();

		if (errorMessageList == null || errorMessageList.size() > 0) {
			return;
		}
		if (!isMounted()) {
			errorMessageList.add("先にMOUNT/LOADを実行してください。");
			return;
		}

		File qsoMsgDir;
		qsoMsgDir = new File(prop.getStrQsoMsgDirFilePath());
		if (!qsoMsgDir.exists()) {
			errorMessageList.add("QSOMSGDIR.datが見当たりません。");
			return;
		}

		File qsoMsgFat;
		qsoMsgFat = new File(prop.getStrQsoMsgFatFilePath());
		if (!qsoMsgFat.exists()) {
			errorMessageList.add("QSOMSGFAT.datが見当たりません。");
			return;
		}

		File qsoMgr;
		qsoMgr = new File(prop.getStrQsoMngFilePath());
		if (!qsoMgr.exists()) {
			errorMessageList.add("QSOMNG.datが見当たりません。");
			return;
		}

		int efficientDirSize = defragmentationOfPctDirList();

		try (FileOutputStream fos4Dir = new FileOutputStream(qsoMsgDir);
				FileOutputStream fos4Fat = new FileOutputStream(qsoMsgFat);) {

			saveDirAndFatOn(msgDirList, fos4Dir, fos4Fat, efficientDirSize);
			saveGeneralManager(generalMgr, qsoMgr, presentMaxNumberInMessage, presentMaxNumberInPictFileName,
					efficientDirSize);
		} catch (Exception e) {
			System.out.println("ERROR on PICT Concerning File Manupilation: " + e.getLocalizedMessage());
		}

	}

	public void saveAllOfFilesOnPict(LinkedList<String> errorMessageList) {
		CYsfSdCHandlerProperties prop = CYsfSdCHandlerProperties.getInstance();

		if (errorMessageList == null || errorMessageList.size() > 0) {
			return;
		}
		if (!isMounted()) {
			errorMessageList.add("先にMOUNT/LOADを実行してください。");
			return;
		}

		File qsoPctDir;
		qsoPctDir = new File(prop.getStrQsoPctDirFilePath());
		if (!qsoPctDir.exists()) {
			errorMessageList.add("QSOPCTDIR.datが見当たりません。");
			return;
		}

		File qsoPctFat;
		qsoPctFat = new File(prop.getStrQsoPctFatFilePath());
		if (!qsoPctFat.exists()) {
			errorMessageList.add("QSOPCTFAT.datが見当たりません。");
			return;
		}

		File qsoMgr;
		qsoMgr = new File(prop.getStrQsoMngFilePath());
		if (!qsoMgr.exists()) {
			errorMessageList.add("QSOMNG.datが見当たりません。");
			return;
		}

		int efficientDirSize = defragmentationOfPctDirList();

		try (FileOutputStream fos4Dir = new FileOutputStream(qsoPctDir);
				FileOutputStream fos4Fat = new FileOutputStream(qsoPctFat);) {

			saveDirAndFatOn(pctDirList, fos4Dir, fos4Fat, efficientDirSize);
			saveGeneralManager(generalMgr, qsoMgr, presentMaxNumberInMessage, presentMaxNumberInPictFileName,
					efficientDirSize);
		} catch (Exception e) {
			// TODO 自動生成された catch ブロック
			System.out.println("ERROR on PICT Concerning File Manupilation: " + e.getLocalizedMessage());
		}

	}

	private <CDE extends CDataEntry> void saveDirAndFatOn(LinkedList<CDE> dirList, FileOutputStream fos4Dir,
			FileOutputStream fos4Fat, int efficientDirSize) throws Exception {
		// QSOMSGFAT再生成
		byte[] fatData = new byte[4 * efficientDirSize];
		int index = 0;
		int indexCount = 0;
		int presentWholeDirVolume = dirList.size();
		for (int i = 0; i < presentWholeDirVolume; ++i) {
			CDataEntry target = dirList.get(i);
			if (target.isRealFileExists()) {
				if (target.isActive()) {
					fatData[index++] = 0x40;
					fatData[index++] = (byte) (((indexCount * 128) & 0xff0000) >> 16);
					fatData[index++] = (byte) (((indexCount * 128) & 0xff00) >> 8);
					fatData[index++] = (byte) ((indexCount * 128) & 0xff);
					target.setDataId(indexCount + 1);
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

		// DIRの生成
		for (CDataEntry elem : dirList) {
			if (elem.isRealFileExists()) {
				elem.storeOwnData2Buffer();
				fos4Dir.write(elem.getDataEntry());
			}
		}

	}

	private void saveGeneralManager(byte[] manager, File managerFile, int presentMaxNumberInMessage,
			int presentMaxNumberInPictFileName, int efficientDirSize) throws IOException {
		// 最後にQSOPCTMNGの生成
		// 次の書き込みファイル名が決定されるので、これで対応。デフラグしないなら、これも書き換える必要がある。
		try (FileOutputStream fos4Mng = new FileOutputStream(managerFile);) {

			manager[0x00] = (byte) ((presentMaxNumberInMessage >> 8) & 0xff);
			manager[0x01] = (byte) ((presentMaxNumberInMessage) & 0xff);

			if (efficientDirSize > 0) {
				manager[0x10] = (byte) ((efficientDirSize >> 8) & 0xff);
				manager[0x11] = (byte) (efficientDirSize & 0xff);
			} else {
				manager[0x10] = (byte) 0xff;
				manager[0x11] = (byte) 0xff;
			}

			int nextNumberInFileName2Write = presentMaxNumberInPictFileName + 1;
			manager[0x12] = (byte) ((nextNumberInFileName2Write >> 8) & 0xff);
			manager[0x13] = (byte) ((nextNumberInFileName2Write) & 0xff);

			fos4Mng.write(manager); // 実ファイルへの書き込みはMSG関連の処理をする時点で。
		} catch (IOException e) {
			System.out.println("ERROR on Manager Concerning File Manupilation: QSOMGR.dat : " + e.getLocalizedMessage());
			throw new IOException();
		}
	}

	public synchronized void setMounted(boolean state) {
		mouonted = state;
	}

	public CVoiceEntry findVoiceFile(String description2Change) {
		CVoiceEntry res = null;
		for (CVoiceEntry data : voiceDirList) {
			if (description2Change.equals(data.getFileNameCore()))
				return data;
		}
		return null;
	}

	public void saveAllOfFilesOnVoice(LinkedList<String> errorMessageList) {
		CYsfSdCHandlerProperties prop = CYsfSdCHandlerProperties.getInstance();

		if (errorMessageList == null || errorMessageList.size() > 0) {
			return;
		}
		if (!isMounted()) {
			errorMessageList.add("先にMOUNT/LOADを実行してください。");
			return;
		}

		File qsoVoiceDir;
		qsoVoiceDir = new File(prop.getStrQsoVoiceDirFilePath());
		if (!qsoVoiceDir.exists()) {
			errorMessageList.add("QSOWAVDIR.datが見当たりません。");
			return;
		}

		File qsoVoiceFat;
		qsoVoiceFat = new File(prop.getStrQsoVoiceFatFilePath());
		if (!qsoVoiceFat.exists()) {
			errorMessageList.add("QSOWAVFAT.datが見当たりません。");
			return;
		}

		File wavMgr;
		wavMgr = new File(prop.getStrQsoVoiceMngFilePath());
		if (!wavMgr.exists()) {
			errorMessageList.add("WAVMNG.datが見当たりません。");
			return;
		}

		// まずは、DIRを記録する。
		int efficientDirSize = defragmentationOfVoiceDirList();

		try (FileOutputStream fos4Dir = new FileOutputStream(qsoVoiceDir);
				FileOutputStream fos4Fat = new FileOutputStream(qsoVoiceFat);) {

			saveDirAndFatOn(voiceDirList, fos4Dir, fos4Fat, efficientDirSize);
			
			// voiceMgrは0x10～0x1f番地までは変則的に０を設定。
			for ( int index=0x10 ; index<0x20 ; ++index)
				voiceMgr[index]= (byte) 0 ;
			saveGeneralManager(voiceMgr, wavMgr, efficientDirSize, 0, efficientDirSize);
		} catch (Exception e) {
			// TODO 自動生成された catch ブロック
			System.out.println("ERROR on Voice Concerning File Manupilation: " + e.getLocalizedMessage());
		}

		// 次にFATを記録

		// 最後にWAVMGRを記録
		byte[] generalMgr4Voice = new byte[32];

	}

}
