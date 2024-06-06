package net.dialectech.ftmSdCardHandler.actions;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IM4JavaException;
import org.im4java.core.IMOperation;
import org.im4java.process.ArrayListOutputConsumer;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.thymeleaf.util.StringUtils;

import jakarta.servlet.http.HttpSession;
import net.dialectech.ftmSdCardHandler.actions.suppoters.CHandlerActionFundamental;
import net.dialectech.ftmSdCardHandler.data.CBankEntry;
import net.dialectech.ftmSdCardHandler.data.CData4Upload;
import net.dialectech.ftmSdCardHandler.data.CImageEntry;
import net.dialectech.ftmSdCardHandler.data.CVoiceEntry;
import net.dialectech.ftmSdCardHandler.supporters.CConst;
import net.dialectech.ftmSdCardHandler.supporters.CYsfCodeConverter;
import net.dialectech.ftmSdCardHandler.supporters.CYsfSdCHandlerProperties;
import net.dialectech.ftmSdCardHandler.supporters.dialectechSup.CDltFlowsException;
import net.dialectech.ftmSdCardHandler.supporters.dialectechSup.CDltSpringFileStream;
import net.dialectech.ftmSdCardHandler.supporters.fileSystem.CYsfFileSystem;
import net.dialectech.ftmSdCardHandler.supporters.fileSystem.CYsfFileSystemCorePart;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

@Controller
@RequestScope
@RequestMapping("/execute/images")
public class CHandlerImageAction extends CHandlerActionFundamental {

	@RequestMapping(value = "deleteImageCompletely", method = { RequestMethod.POST, RequestMethod.GET })
	public ModelAndView actDataDeleteImageCompletely(@ModelAttribute CData4Upload param, HttpSession session,
			ModelAndView mav) {
		CYsfFileSystem fs = CYsfFileSystem.getInstance();
		CYsfSdCHandlerProperties prop = CYsfSdCHandlerProperties.getInstance();

		LinkedList<String> errorMessageList = new LinkedList<String>();
		if (!fs.isSdCardMounted()) {
			errorMessageList = new LinkedList<String>();
			errorMessageList.add("SD-CARDが抜かれたか、未だ挿入されたSD-CARDのMOUNT処理がされていません。");
			setAllParameters4Mav(mav, errorMessageList, "", fs, getPresentUsingBranchName4Display(), prop,
					"pages/divImages");
			fs.clearAll();
			return mav;
		}

		int targetDataId = param.getTargetDataId();
		LinkedList<CImageEntry> dirListWDO = fs.getPctDirListWithDisplayOrder();

		String targetDirName = prop.getStrPhotoDirectoryPath();

		for (int index = 0; index < dirListWDO.size(); ++index) {
			CImageEntry data = dirListWDO.get(index);
			if (data.getDataId() == targetDataId) {
				// 完全削除を指示されたターゲットdirEntryはdataに記録されている。
				data.setActive(false);
				// 完全Deleteした場合、戻せないように、通常の流れのimageIdと異なる番号（-1）を付与して、検索されないようにする。
				data.setDataId(-1);

				// 二重に管理されているファイルは削除しない。
				int volumeOfSameName = 0;
				for (int ix = 0; ix < dirListWDO.size(); ++ix) {
					String existing = dirListWDO.get(ix).getFileCoreName();
					if (data.getFileCoreName().equals(existing) && dirListWDO.get(ix).isRealFileExists())
						volumeOfSameName++;
				}

				data.setRealFileExists(false);

				// volumeOfSameNameが２以上あったら、重複管理しているので、ファイルは削除しない。
				if (volumeOfSameName == 1) {
					File file2Delete = new File(targetDirName + data.getFileCoreName());
					if (file2Delete.exists()) {
						try {
							FileUtils.forceDelete(file2Delete);
						} catch (IOException e) {
							// TODO 自動生成された catch ブロック
							e.printStackTrace();
						}
					} else {
						System.out.println("ERROR! on file Delete as File not exists." + data.getFileCoreName());
					}
				}
				fs.removeThisImage(data);
				break;
			}
		}

		fs.saveAllOfFilesOnPict(errorMessageList);
		fs.reNumberAndPrepareForPictureDisplay();
		setAllParameters4Mav(mav, errorMessageList, "", fs, getPresentUsingBranchName4Display(), prop,
				"pages/divImages");
		mav.setViewName("pages/divImages");
		return mav;
	}

	@RequestMapping(value = "deleteImageMarking", method = { RequestMethod.POST, RequestMethod.GET })
	public ModelAndView actDataDeleteImageMarking(@ModelAttribute CData4Upload param, HttpSession session,
			ModelAndView mav) {
		CYsfFileSystem fs = CYsfFileSystem.getInstance();
		CYsfSdCHandlerProperties prop = CYsfSdCHandlerProperties.getInstance();
		LinkedList<String> errorMessageList = new LinkedList<String>();

		if (!fs.isSdCardMounted()) {
			errorMessageList = new LinkedList<String>();
			errorMessageList.add("SD-CARDが抜かれたか、未だ挿入されたSD-CARDのMOUNT処理がされていません。");
			setAllParameters4Mav(mav, errorMessageList, "", fs, "-----", prop, "pages/divImages");
			return mav;
		}

		int targetDataId = param.getTargetDataId();
		LinkedList<CImageEntry> dirList = fs.getPctDirList();
		for (int index = 0; index < dirList.size(); ++index) {
			CImageEntry elem = dirList.get(index);
			if (elem.getDataId() == targetDataId) {
				elem.setActive(false);
				elem.storeOwnData2Buffer();
				break;
			}
		}

		fs.saveAllOfFilesOnPict(errorMessageList);
		fs.reNumberAndPrepareForPictureDisplay();
		setAllParameters4Mav(mav, errorMessageList, "", fs, getPresentUsingBranchName4Display(), prop,
				"pages/divImages");
		return mav;
	}

	@RequestMapping(value = "deleteThisBankDir", method = { RequestMethod.POST, RequestMethod.GET })
	public ModelAndView actDataDeleteThisBankDir(@ModelAttribute CData4Upload param, HttpSession session,
			ModelAndView mav) {
		CYsfFileSystem fs = CYsfFileSystem.getInstance();
		CYsfSdCHandlerProperties prop = CYsfSdCHandlerProperties.getInstance();
		LinkedList<String> errorMessageList = new LinkedList<String>();

//		if (!fs.isSdCardMounted()) {
//			errorMessageList.add("SD-CARDが抜かれたか、未だ挿入されたSD-CARDのMOUNT処理がされていません。");
//			fs.clearAll();
//			setAllParameters4Mav(mav, errorMessageList, "", fs, "", prop, "pages/divResDeleteBankDir");
//			return mav;
//		}

		if (param.getBranchDirName() == null || param.getBranchDirName().equals("")) {
			errorMessageList.add("指示が異常です。");
			setAllParameters4Mav(mav, errorMessageList, "", fs, "", prop, "pages/divResDeleteBankDir");
			return mav;
		}

		File bankDir = new File(prop.getStrBankDirectoryPath() + param.getBranchDirName());

		try {
			FileUtils.forceDelete(bankDir);
			errorMessageList.add("削除完了しました。");
		} catch (IOException e) {
			errorMessageList.add("削除に失敗しました。");
		}

		// 次に、BANK-LIST表示のための処理

		LinkedList<CBankEntry> dirStructureList = createBankDirStructureList();

		setAllParameters4Mav(mav, errorMessageList, param.getBranchDirName(), fs, "", prop, "pages/divShowBankList");
		mav = createContentsList(mav, param.getStartFrom(), param.getSortingOrder(), dirStructureList,
				errorMessageList);

		return mav;
	}

	private LinkedList<CBankEntry> createBankDirStructureList() {
		CYsfSdCHandlerProperties prop = CYsfSdCHandlerProperties.getInstance();
		String bankDirectoryPosition = prop.getRootDir4Bank();
		File bankDirectory = new File(bankDirectoryPosition);
		LinkedList<CBankEntry> dirStructureList = new LinkedList<CBankEntry>();

		int index = 1 ;
		for (File f : bankDirectory.listFiles()) {
			// BANK統括ディレクトリ（リカバリディレクトリ）には、各BANKに係るフォルダが配置されるとともに、画像データはその個別フォルダに配置するようにした。
			if (f.isDirectory()) {
				CBankEntry data = new CBankEntry(f.getAbsoluteFile(), new Date(f.lastModified()));
				data.setDataId(index++);
				dirStructureList.add(data);
			}
		}
		return dirStructureList;
	}

	@RequestMapping(value = "downloadImage", method = { RequestMethod.POST, RequestMethod.GET })
	public ResponseEntity<StreamingResponseBody> actDataDownloadImage(@ModelAttribute CData4Upload params,
			HttpSession session, ModelAndView mav) {
		CYsfFileSystem fs = CYsfFileSystem.getInstance();
		CYsfSdCHandlerProperties prop = CYsfSdCHandlerProperties.getInstance();

		String fileName;
		if (params.getBranchDirName() == null || params.getBranchDirName().equals("")) {
			fileName = prop.getSdCardBaseDirName() + CConst.PhotoFoldername + File.separator
					+ params.getFileName2Dowload();
		} else {
			fileName = prop.getStrBankDirectoryPath() + File.separator + params.getBranchDirName() + File.separator
					+ params.getFileName2Dowload();
		}
		ResponseEntity<StreamingResponseBody> streamData;
		try {
			File targetFile = new File(fileName);
			String specifiedFileName = params.getFileName2Dowload();
			// 実ファイルパスに基づいてspringFileStreamを生成
			if (targetFile.exists()) {
				springFileStream = new CDltSpringFileStream(targetFile, specifiedFileName);
				streamData = springFileStream.prepareDownload();
				return streamData;
			} else {
				springFileStream = forceStreamAsImage("fileNotFound.jpg");
				return springFileStream.prepareDownload();
			}
		} catch (CDltFlowsException e) {
			springFileStream = forceStreamAsImage("fileNotFound.jpg");
			return springFileStream.prepareDownload();
		}
	}

	@RequestMapping(value = "testA", method = { RequestMethod.POST, RequestMethod.GET })
	public ModelAndView actDataTestA(@ModelAttribute CData4Upload params, HttpSession session, ModelAndView mav) {
		CYsfSdCHandlerProperties prop = CYsfSdCHandlerProperties.getInstance();
		CYsfFileSystem fs = CYsfFileSystem.getInstance();

		LinkedList<CVoiceEntry> vdl = fs.getVoiceDirList();
		File dir = new File(prop.getStrVoiceDirectoryPath());
		String strTest = "!#$%&(t);+-_";
		if (!strTest.matches("[a-zA-Z0-9\\_!#$%&\\(\\)\\+\\-;]+")) {
			System.out.println("所定以外のコードを使用!");
		}
		String newBase = CYsfCodeConverter.getInstance()
				.ysfByte2Utf8(CYsfCodeConverter.getInstance().utf82YsfByte(strTest));
//		for (File elem : dir.listFiles()) {
//			byte[] fName = elem.getName().getBytes();
//			System.out.println("** " + CYsfCodeConverter.getInstance().ysfByte2Utf8(fName));
//		}
		LinkedList<String> errorMessageList = new LinkedList<String>();
		setAllParameters4Mav(mav, errorMessageList, "", fs, getPresentUsingBranchName4Display(), prop,
				"pages/divMessages");
		return mav;
	}

	/**
	 * リクエスト：Mount/Loadボタンに対応するloadPresentImagesでの処理。
	 * 
	 * @param params
	 * @param session
	 * @param mav
	 * @return
	 */
	@RequestMapping(value = "mountAndLoadPresentImages", method = { RequestMethod.POST, RequestMethod.GET })
	public ModelAndView actDataMountAndLoadPresentImages(@ModelAttribute CData4Upload params, HttpSession session,
			ModelAndView mav) {
		CYsfSdCHandlerProperties prop = CYsfSdCHandlerProperties.getInstance();
		CYsfFileSystem fs = CYsfFileSystem.getInstance();
		fs.setMounted(false);

		String radioId = prop.getRadioId();
		String sdCardBaseDirectory = prop.getSdCardBaseDirName();
		// String presentUsingBranchName = getPresentUsingBranchName(cardDrive);

		// System.out.println(">>>" + presentUsingBranchName);
		LinkedList<String> errorMessageList = new LinkedList<String>();
		if (radioId == null || radioId.length() != 5) {
			errorMessageList.add("radioIdの設定が異常です。５文字で指定してください。");
			setAllParameters4Mav(mav, errorMessageList, "", fs, "ERROR", prop, "pages/divImages");
			fs.clearAll();
			return mav;
		}
		if (!(new File(sdCardBaseDirectory + CConst.QsoLogFolderName + File.separator)).exists()) {
			errorMessageList.add("対応するSD-CARDが装着されていません。");
			setAllParameters4Mav(mav, errorMessageList, "", fs, "ERROR", prop, "pages/divImages");
			fs.clearAll();
			return mav;
		}

		String errorMsg = fs.loadFromSDCard();
		if (errorMsg != null) {
			errorMessageList.add(errorMsg);
			errorMessageList.add("SD-CARDの構成が不完全です。");
			setAllParameters4Mav(mav, errorMessageList, "", fs, "ERROR", prop, "pages/divImages");
			return mav;
		}

		// SD-CARD-IDファイルがなかった、新たにIDを生成して、それをSD-CARD-IDファイルに書きこんで、かつFileSystemにも登録する。

		Path sdCardIdFile = Path.of(prop.getStrSdCardIdHolderPath());
		if (sdCardIdFile.toFile().exists()) {
			// SD-CARD-IDファイルがあったら、それを読みこんで、FileSystemに登録する。
			List<String> contents;
			try {
				contents = Files.readAllLines(sdCardIdFile, StandardCharsets.UTF_8);
				String mountedSdCardId = contents.getFirst();
				fs.setSdCardID(mountedSdCardId);
			} catch (IOException e) {
				errorMessageList.add("SD-CARD中のIDファイルの読み出しに失敗しました。＞" + e.getLocalizedMessage());
			}
		} else {
			// SD-CARD-IDファイルがなかった、新たにIDを生成して、それをSD-CARD-IDファイルに書きこんで、かつFileSystemにも登録する。
			String presentSDCardID = UUID.randomUUID().toString().replaceAll("-", "");
			String sdCardIdHolder = prop.getStrSdCardIdHolderPath();
			try (FileOutputStream fo = new FileOutputStream(sdCardIdHolder);) {
				fo.write(presentSDCardID.getBytes(StandardCharsets.UTF_8));
				fs.setSdCardID(presentSDCardID);
			} catch (IOException e) {
				errorMessageList.add("SD-CARD中にIDファイルがなかったので作成しようとしましたが、その書き出しに失敗しました。＞" + e.getLocalizedMessage());
			}
		}

		fs.setMounted(true);
		// 現時点でのSD-CARD上の画像情報はFileSystem:fsにあるから、そのままThymeleafに渡す。
		setAllParameters4Mav(mav, errorMessageList, "", fs, getPresentUsingBranchName4Display(), prop,
				"pages/divImages");
		return mav;
	}

	@RequestMapping(value = "move2MemoryAndCompleteClearAllImages", method = { RequestMethod.POST, RequestMethod.GET })
	public ModelAndView actDataMove2MemoryAndCompleteClearAllImages(@ModelAttribute CData4Upload param,
			HttpSession session, ModelAndView mav) {
		CYsfFileSystem fs = CYsfFileSystem.getInstance();
		CYsfSdCHandlerProperties prop = CYsfSdCHandlerProperties.getInstance();

		LinkedList<String> errorMessageList = null;
		errorMessageList = new LinkedList<String>();

		if (!fs.isSdCardMounted()) {
			errorMessageList.add("SD-CARDが抜かれたか、未だ挿入されたSD-CARDのMOUNT処理がされていません。");
			setAllParameters4Mav(mav, errorMessageList, "", fs, "", prop, "pages/divImages");
			fs.clearAll();
			return mav;
		}

		if (fs.getPctDirList().size() == 0) {
			errorMessageList.add("現在、表示する画像が記録されていません。");
			setAllParameters4Mav(mav, errorMessageList, "", fs, "新規登録", prop, "pages/divImages");
			return mav;
		}

		errorMessageList = backupPresentPhotoDir2Bank();
		// errorMessageListはbackupPresentPhotoDir2Bank()処理内で別オブジェクトに書き換わっているので、あらためてaddObjectに設定。

		if (errorMessageList.size() > 0) {
			// backupPresentPhotoDir2Bank()処理でエラーがあったときの処理
			setAllParameters4Mav(mav, errorMessageList, "", fs, "", prop, "pages/divImages");
			return mav;
		}

		// 本来のPHOTOディレクトリの位置にPHOTOディレクトリを生成する。
		try {
			File photoDirectory = new File(prop.getStrPhotoDirectoryPath());
			if (photoDirectory.exists()) {
				FileUtils.forceDelete(photoDirectory);
				Thread.sleep(100); // ここで一瞬立ち止まらないと、なぜかエラーが出る。
			}
			FileUtils.forceMkdir(new File(prop.getStrPhotoDirectoryPath()));
		} catch (IOException | InterruptedException e) {
			errorMessageList.add("バンク移行は行いましたが、あらためてのPHOTOディレクトリが作れませんでした。");
			setAllParameters4Mav(mav, errorMessageList, "", fs, "-----", prop, "pages/divImages");
			return mav;
		}

		// 最後に記録済みディレクトリをクリアして終わり。
		fs.setPctDirList(new LinkedList<CImageEntry>());

		// あとは表示用の後処理。SDCardにクリア済みのディレクトリデータの記録。
		fs.saveAllOfFilesOnPict(errorMessageList);
		fs.reNumberAndPrepareForPictureDisplay();
		setAllParameters4Mav(mav, errorMessageList, "", fs, getPresentUsingBranchName4Display(), prop,
				"pages/divImages");

		return mav;
	}

	@RequestMapping(value = "recallDirContentsInBank", method = { RequestMethod.POST, RequestMethod.GET })
	public ModelAndView actDataRecallDirContentsInBank(@ModelAttribute CData4Upload param, HttpSession session,
			ModelAndView mav) {
		CYsfFileSystem fs = CYsfFileSystem.getInstance();
		CYsfSdCHandlerProperties prop = CYsfSdCHandlerProperties.getInstance();
		LinkedList<String> errorMessageList = new LinkedList<String>();

		if (!fs.isSdCardMounted()) {
			errorMessageList.add("SD-CARDが抜かれたか、未だ挿入されたSD-CARDのMOUNT処理がされていません。");
			setAllParameters4Mav(mav, errorMessageList, "", fs, "----", prop, "pages/divImages");
			fs.clearAll();
			return mav;
		}

		// まずは現用PHOTOディレクトリをバックアップする。記録がなければバックアップは不要なので、これをスキップする。
		if (fs.getPctDirList().size() == 0) {
			errorMessageList = new LinkedList<String>();
			System.out.println("記録がないので、バックアップは行いません。");
		} else {
			// 現在SDCARDで管理されているファイルがあれば、BANKにバックアップする。
			errorMessageList = backupPresentPhotoDir2Bank();
		}

		if (errorMessageList.size() > 0) {
			setAllParameters4Mav(mav, errorMessageList, "", fs, "----", prop, "pages/divImages");
			fs.clearAll();
			return mav;
		}

		String sourceDirName = prop.getRootDir4Bank() + param.getBranchDirName() + File.separator;
		File sourceDir = new File(sourceDirName);
		if (!sourceDir.exists()) {
			errorMessageList.add("指定されたディレクトリは、現在記録が壊れている模様です。");
			setAllParameters4Mav(mav, errorMessageList, "", fs, "----", prop, "pages/divImages");
			return mav;
		}
		File photoDir = new File(prop.getStrPhotoDirectoryPath());
		try {
			if (photoDir.exists()) {
				// 未だPHOTOディレクトリが存在していたら、そこにどんなファイルがあろうとも、迷わず削除してbackupされたデータを移送できるようにする。
				FileUtils.forceDelete(photoDir);
			}
			FileUtils.copyDirectory(sourceDir, photoDir);
		} catch (IOException e) {
			errorMessageList.add("バックアップされたディレクトリを戻せませんでした。");
			setAllParameters4Mav(mav, errorMessageList, "", fs, "----", prop, "pages/divImages");
			return mav;
		}

		// QSOLOG内、branchNameHolderファイルに移動させるディレクトリの名称を記録
		setPresentUsingBranchName(param.getBranchDirName());

		// バンク記録時に退避させていたQSOPCTDIR.datとQSOPCTFAT.datを、QSOLOG内に戻す。
		File targetQsopctdir = new File(prop.getStrQsoPctDirFilePath());
		File targetQsopctfat = new File(prop.getStrQsoPctFatFilePath());
		File sourceQsopctdir = new File(prop.getStrPhotoDirectoryPath() + CConst.STR_QSOPCTDIR);
		File sourceQsopctfat = new File(prop.getStrPhotoDirectoryPath() + CConst.STR_QSOPCTFAT);

		try {
			FileUtils.forceDelete(targetQsopctdir);
			FileUtils.forceDelete(targetQsopctfat);
			FileUtils.moveFile(sourceQsopctdir, targetQsopctdir, StandardCopyOption.REPLACE_EXISTING);
			FileUtils.moveFile(sourceQsopctfat, targetQsopctfat, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			errorMessageList.add("バックアップしていたDIR/FATデータを戻せませんでした。");
			setAllParameters4Mav(mav, errorMessageList, "", fs, "----", prop, "pages/divImages");
			return mav;
		}

		// ここまででファイルの復帰だけはしたので、remountしなければならない。
		String errorMsg = fs.loadFromSDCard();
		if (errorMsg != null && !errorMsg.equals(""))
			errorMessageList.add(errorMsg);

		// あとは表示用の後処理。
		fs.saveAllOfFilesOnPict(errorMessageList);
		fs.reNumberAndPrepareForPictureDisplay();
		setAllParameters4Mav(mav, errorMessageList, "", fs, getPresentUsingBranchName4Display(), prop,
				"pages/divImages");
		return mav;
	}

	@RequestMapping(value = "recoverMarking", method = { RequestMethod.POST, RequestMethod.GET })
	public ModelAndView actDataRecoverMarking(@ModelAttribute CData4Upload param, HttpSession session,
			ModelAndView mav) {
		CYsfFileSystem fs = CYsfFileSystem.getInstance();
		CYsfSdCHandlerProperties prop = CYsfSdCHandlerProperties.getInstance();
		LinkedList<String> errorMessageList = new LinkedList<String>();

		if (!fs.isSdCardMounted()) {
			errorMessageList.add("SD-CARDが抜かれたか、未だ挿入されたSD-CARDのMOUNT処理がされていません。");
			setAllParameters4Mav(mav, errorMessageList, "", fs, "", prop, "pages/divImages");
			fs.clearAll();
			return mav;
		}

		int targetDataId = param.getTargetDataId();
		LinkedList<CImageEntry> dirList = fs.getPctDirListWithDisplayOrder();
		for (int index = 0; index < dirList.size(); ++index) {
			if (dirList.get(index).getDataId() == targetDataId) {
				dirList.get(index).setActive(true);
				dirList.get(index).storeOwnData2Buffer();
				break;
			}
		}
		fs.saveAllOfFilesOnPict(errorMessageList);
		fs.reNumberAndPrepareForPictureDisplay();
		setAllParameters4Mav(mav, errorMessageList, "", fs, getPresentUsingBranchName4Display(), prop,
				"pages/divImages");
		mav.setViewName("pages/divImages");
		return mav;
	}

	@RequestMapping(value = "renameOneOfBankDir", method = { RequestMethod.POST, RequestMethod.GET })
	public ModelAndView actDataRenameOneOfBankDir(@ModelAttribute CData4Upload params, HttpSession session,
			ModelAndView mav) {
		CYsfFileSystem fs = CYsfFileSystem.getInstance();
		CYsfSdCHandlerProperties prop = CYsfSdCHandlerProperties.getInstance();
		LinkedList<String> errorMessageList;
		errorMessageList = new LinkedList<String>();

		String newBranchDirName = params.getNewBranchDirName();
		String targetBranchDirName = params.getBranchDirName();

		if (newBranchDirName != null && newBranchDirName.equals(targetBranchDirName)) {
			// Fornt-Endで処理済みなので、ここは通らないはず。
			errorMessageList.add("元の名称と同じなので、何もしません。");
			setAllParameters4Mav(mav, errorMessageList, params.getBranchDirName(), fs, "", prop,
					"pages/divResRenameOneOfBankDir");
			mav.addObject("newBranchDirName", newBranchDirName);
			return mav;
		}
		// 新規名称が既にあるものかどうか？なければそのままOK、あったら、自動で付与された名称（但し酷似^^;）を新バンク名として利用
		newBranchDirName = dupNameCheckAndRename2UniqueName(newBranchDirName);
		File newBranchDir = new File(prop.getRootDir4Bank() + newBranchDirName);
		try {
			FileUtils.moveDirectory(new File(prop.getRootDir4Bank() + targetBranchDirName), newBranchDir);
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

		// 次に、BANK-LIST表示のための処理

		LinkedList<CBankEntry> dirStructureList = createBankDirStructureList();

		setAllParameters4Mav(mav, errorMessageList, params.getBranchDirName(), fs, "", prop, "pages/divShowBankList");
		mav = createContentsList(mav, params.getStartFrom(), params.getSortingOrder(), dirStructureList,
				errorMessageList);

		return mav;
	}

	@RequestMapping(value = "error", method = { RequestMethod.POST, RequestMethod.GET })
	public ModelAndView actDataError(@ModelAttribute CData4Upload param, HttpSession session, ModelAndView mav) {
		mav.addObject("errorMessage", "何らかのエラーが生じました。");
		mav.setViewName("pages/divErrorOnly");
		return mav;
	}

	@RequestMapping(value = "showBankDirContents", method = { RequestMethod.POST, RequestMethod.GET })
	public ModelAndView actDatashowBankDirContents(@ModelAttribute CData4Upload param, HttpSession session,
			ModelAndView mav) {

		CYsfSdCHandlerProperties prop = CYsfSdCHandlerProperties.getInstance();
		CYsfFileSystem fs4SdCard = CYsfFileSystem.getInstance();
		CYsfFileSystemCorePart fs = new CYsfFileSystemCorePart();
		fs.loadFromTargetBankDirFiles(prop.getRootDir4Bank() + param.getBranchDirName());

		String targetBankDirectoryPosition = prop.getRootDir4Bank() + param.getBranchDirName() + File.separator;
		File bankDirectory = new File(targetBankDirectoryPosition);

		LinkedList<String> errorMessageList = new LinkedList<String>();

		if (!fs4SdCard.isSdCardMounted()) {
			errorMessageList = new LinkedList<String>();
			errorMessageList.add("SD-CARDが抜かれたか、未だ挿入されたSD-CARDのMOUNT処理がされていません。");
			errorMessageList.add("MOUNTしなければ、「画像処理」プレーンでの画像処理ができません。");
			setAllParameters4Mav(mav, errorMessageList, "", fs, "", prop, "pages/divImagesOnBank");
//			fs.clearAll();
//			return mav;
		}
		if (!bankDirectory.exists()) {
			errorMessageList.add("未だバンクが作られていません。");
			setAllParameters4Mav(mav, errorMessageList, "", fs, "", prop, "pages/divImagesOnBank");
			return mav;
		}
		fs.loadFromTargetBankDirFiles(targetBankDirectoryPosition);
		LinkedList<String> imageList = new LinkedList<String>();

		setAllParameters4Mav(mav, errorMessageList, param.getBranchDirName(), fs, "", prop, "pages/divImagesOnBank");
		mav.addObject("param", param);
		return mav;
	}

	@RequestMapping(value = "showBankList", method = { RequestMethod.POST, RequestMethod.GET })
	public ModelAndView actDataShowBankList(@ModelAttribute CData4Upload param, HttpSession session, ModelAndView mav) {
		CYsfFileSystem fs = CYsfFileSystem.getInstance();
		CYsfSdCHandlerProperties prop = CYsfSdCHandlerProperties.getInstance();
		LinkedList<String> errorMessageList = new LinkedList<String>();

		if (!fs.isSdCardMounted()) {
			errorMessageList.add("SD-CARDが抜かれたか、未だ挿入されたSD-CARDのMOUNT処理がされていません。");
			errorMessageList.add("MOUNTしなければ、「画像処理」プレーンでの画像処理ができません。");
			fs.clearAll();
// 悩みどころではあるけれど、BANKを見るだけなら、MOUNTしてなくてもよい場合があるので、ここは処理を止めないことにする。
//			setAllParameters4Mav(mav, errorMessageList, param.getBranchDirName(), fs, "", prop,
//					"pages/divShowBankList");
//			return mav;
		}

		String bankDirectoryPosition = prop.getRootDir4Bank();
		File bankDirectory = new File(bankDirectoryPosition);

		if (!bankDirectory.exists()) {
			System.out.println("BANK用の記憶領域が作られていませんので、ここで作成します。");
			try {
				Files.createDirectories(bankDirectory.toPath());
			} catch (IOException e) {
				e.printStackTrace();
				errorMessageList.add("BANK領域を作成できませんでした。");
				setAllParameters4Mav(mav, errorMessageList, "", fs, "", prop, "pages/divShowBankList");
			}
		}

		LinkedList<CBankEntry> dirStructureList = createBankDirStructureList();

		setAllParameters4Mav(mav, errorMessageList, param.getBranchDirName(), fs, "", prop, "pages/divShowBankList");
		mav = createContentsList(mav, param.getStartFrom(), param.getSortingOrder(), dirStructureList,
				errorMessageList);
		return mav;
	}

	/**
	 * QRコード生成リクエストへのレスポンス
	 * 
	 * @param params
	 * @param session
	 * @param mav
	 * @return
	 */
	@RequestMapping(value = "createQRCodeAndLoadPresentImages", method = { RequestMethod.POST, RequestMethod.GET })
	public ModelAndView actDataCreateQRCodeAndLoadPresentImages(@ModelAttribute CData4Upload params,
			HttpSession session, ModelAndView mav) {
		CYsfSdCHandlerProperties prop = CYsfSdCHandlerProperties.getInstance();
		CYsfFileSystem fs = CYsfFileSystem.getInstance();
		LinkedList<String> errorMessageList = new LinkedList<String>();

		if (!fs.isSdCardMounted()) {
			errorMessageList.add("SD-CARDが抜かれたか、未だ挿入されたSD-CARDのMOUNT処理がされていません。");
			fs.clearAll();
			setAllParameters4Mav(mav, errorMessageList, "", fs, "ERROR", prop, "pages/divImages");
			return mav;
		}

		if (params.getStrQRCode() == null || params.getStrQRCode().equals("")) {
			errorMessageList.add("QRコードに変換する文字列が指定されていません。");
			setAllParameters4Mav(mav, errorMessageList, "", fs, "ERROR", prop, "pages/divImages");
			return mav;
		}

		// ImageMagickの存否チェック
		String imageMagickPathName4Check = prop.getImageMagickPath();
		File imProcessFile = new File(imageMagickPathName4Check + "convert.exe");
		if (!imProcessFile.exists()) {
			errorMessageList.add("ImageMagickの指定場所に処理プログラムが見つかりません。");
			errorMessageList.add("「対象設定」タブ中の「ImageMagickの記録場所」の指定を確認してください。");
			errorMessageList.add("ImageMagickの記録場所はフォルダを指定するので、末尾が'\\'で終わる必要があります。");
			setAllParameters4Mav(mav, errorMessageList, "", fs, "ERROR", prop, "pages/divImages");
			return mav;
		}

		String radioId = prop.getRadioId();

		File[] inFiles = new File[1];
		String tempFileNameOfQRCode = "c:\\Temp\\" + "YSF_QRCode4Generation.jpg";
		inFiles[0] = new File(tempFileNameOfQRCode);

		// radioIdについてのチェック
		if (radioId == null || radioId.equals("")) {
			errorMessageList.add("RadioIdが指定されていません。");
		} else if (radioId.length() != 5) {
			errorMessageList.add("RadioIdが5桁ではありません。");
		}

		if (errorMessageList.size() != 0) {
			setAllParameters4Mav(mav, errorMessageList, getPresentUsingBranchName4Display(), fs, "ERROR", prop,
					"pages/divImages");
			return mav;
		}

		if (!fs.isMounted()) {
			errorMessageList.add("先にMOUNT/LOAD機能を使って、SDカードの設定を確認してください。");
			setAllParameters4Mav(mav, errorMessageList, "", fs, "ERROR", prop, "pages/divImages");
			return mav;
		}

		// ここまで来て、準備が出来上がったので、いよいよ具体的な処理を開始する。
		// まずは、QRコードの変換を行う。

		try {
			BarcodeFormat format = BarcodeFormat.QR_CODE;
			int width;
			int height;
			switch (params.getImageSize()) {
			case 160:
				width = 160;
				height = 120;
				break;
			case 320:
			default:
				width = 320;
				height = 240;
				break;
			}

			Hashtable<EncodeHintType, Object> hints = new Hashtable<EncodeHintType, Object>();

			ErrorCorrectionLevel ecLevel = ErrorCorrectionLevel.valueOf(params.getErrorCorrectionLevel());
			hints.put(EncodeHintType.ERROR_CORRECTION, ecLevel);
			hints.put(EncodeHintType.CHARACTER_SET, "utf-8");

			QRCodeWriter writer = new QRCodeWriter();
			String source = new String(params.getStrQRCode().getBytes());
			BitMatrix bitMatrix = writer.encode(source, format, width, height, hints);
			BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);
			ImageIO.write(image, "jpg", inFiles[0]);

			// YAESU SYSTEM FUSIONでの特殊カタカナに特化して対応文字の存否チェック
			// 一旦、UTF8 -> YSF特殊文字 -> UTF8 と変換をして、元に戻るかどうかで検証する。
			CYsfCodeConverter converter = CYsfCodeConverter.getInstance();
			String finalDescription = converter
					.ysfByte2Utf8(converter.utf82YsfByte(params.getDescription2Change().trim()));
			if (finalDescription != null && !finalDescription.equals(params.getDescription2Change())) {
				errorMessageList.add("「記述」に使えない文字がありましたので、「$」に置き換えてあります。");
				params.setDescription2Change(finalDescription);
			}
			// Description 中間にあるSPACE文字は、なぜかFTM-300の暴走を引き起こすので、アンダースコアに置換しておく。
			params.setDescription2Change(finalDescription.replaceAll(" ", "_"));

			boolean qrCodeDetected = convertTempFilesAndStore2Target(inFiles, params, "", 8, "#000");
			if (!qrCodeDetected) {
				errorMessageList.add("指定した文字列では解析可能なQRコードとして十分なものになっていません。");
				errorMessageList.add("文字数を減らす、最大容量制限を拡張するなどの変更を加えて、再度生成する必要がありそうです。");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (WriterException e) {
			e.printStackTrace();
		}

		fs.saveAllOfFilesOnPict(errorMessageList);
		fs.reNumberAndPrepareForPictureDisplay();
		setAllParameters4Mav(mav, errorMessageList, "", fs, getPresentUsingBranchName4Display(), prop,
				"pages/divImages");
		return mav;
	}

	private boolean convertTempFilesAndStore2Target(File[] inFiles, CData4Upload params,
			String specifiedStationName2Send, int posOfSuperImpose, String specifiedColor) {

		String volumeTarget = params.getVolumeTarget();
		int imageSize = params.getImageSize();

		CYsfSdCHandlerProperties prop = CYsfSdCHandlerProperties.getInstance();
		CYsfFileSystem fs = CYsfFileSystem.getInstance();

		LinkedList<String> inFilesList = new LinkedList<String>();
		boolean qrCodeDetected = false;
		for (int index = 0; index < inFiles.length; ++index) {
			try {
				String inAbsoluteName = "c:\\Temp\\" + inFiles[index].getName();
				inFilesList.add(inAbsoluteName);
				CImageEntry ie = fs.addNewPictFile(prop.getMyCallSign(), params.getDescription2Change());

				String targetDirName = prop.getStrPhotoDirectoryPath();

				Path newPath = Paths.get(targetDirName, ie.getFileCoreName());

				// commandの生成
				// ImageMagickCmd cmd = new ImageMagickCmd("magick");
				ConvertCmd cmd = new ConvertCmd();
				cmd.setAsyncMode(false);
				String imageMagickPathName = prop.getImageMagickPath();

				cmd.setSearchPath(imageMagickPathName);
				// operation内容の生成。
				IMOperation op = new IMOperation();
				op.addImage(inAbsoluteName);
				// 出力ファイルはJPEG
				op.define("jpeg:extent=" + volumeTarget.trim() + "kB");
				// 大きさを正規化
				switch (imageSize) {
				case 160:
					op.resize(160, 120, '!');
					break;
				case 320:
				default:
					op.resize(320, 240, '!');
					break;
				}

				if (specifiedStationName2Send != null && !specifiedStationName2Send.trim().equals("")) {
					// 相手局名追加（）
					specifiedStationName2Send = StringUtils.escapeJava(specifiedStationName2Send);
					System.out.print(">> Imposing : \"" + specifiedStationName2Send + " : ");

					String letters = specifiedStationName2Send;
					int fontSize = 640 / (letters.length() + 2); // なぜか、320ではなくその倍でちょうど狙ったあたりになる。
					// font サイズの最大値は36にしておく。
					if (fontSize > 36)
						fontSize = 36;
					op.pointsize(fontSize);

					int startXOfBackground = 0;
					int startYOfBackground = fontSize;
					int startXOfLetter = 1;
					int startYOfLetter = fontSize + 1;
					int fullLengthOfLetters = fontSize * letters.length() / 2;
					int offsetX = 0;
					int offsetY = 0;
					switch (posOfSuperImpose) {
					case 0: // 上左詰
						offsetX = 0;
						offsetY = 0;
						break;
					case 1: // 上中央
						offsetX = (320 - fullLengthOfLetters) / 2;
						offsetY = 0;
						break;
					case 2: // 上右詰
						offsetX = (320 - fullLengthOfLetters) - fontSize / 2;
						offsetY = 0;
						break;
					case 8: // 下左詰
						offsetX = 0;
						offsetY = 235 - fontSize;
						break;
					case 9: // 下中央
						offsetX = (320 - fullLengthOfLetters) / 2;
						offsetY = 235 - fontSize;
						break;
					case 10: // 下右詰
					default:
						offsetX = (320 - fullLengthOfLetters) - fontSize / 2;
						offsetY = 235 - fontSize;
						break;
					}
					op.font("Times-New-Roman"); // font
					op.fill("white"); // font color
					op.draw("text " + (startXOfBackground + offsetX) + "," + (startYOfBackground + offsetY) + " '"
							+ letters + "'");
					op.fill(specifiedColor); // font color
					op.draw("text " + (startXOfLetter + offsetX) + "," + (startYOfLetter + offsetY) + " '" + letters
							+ "'"); // location of text, actual text
				}
				// 余計なメタタグを削除
				op.strip();
				op.addImage(newPath.toString());

				ArrayListOutputConsumer output = new ArrayListOutputConsumer();
				cmd.setOutputConsumer(output);
				// execute the operation
				long startTime = System.currentTimeMillis();
				try {
					cmd.run(op);
				} catch (InterruptedException e) {
					// TODO 自動生成された catch ブロック
					e.printStackTrace();
				} catch (IM4JavaException e) {
					e.printStackTrace();
				}
				output.getOutput(); // 処理終了待ちのための空読み。
				long processTime = System.currentTimeMillis() - startTime;
				System.out.print(" Conversion complete (" + processTime + " mS) >> ");
				long fileSize = newPath.toFile().length();
				ie.setPictureSize((int) (fileSize & 0xffffff));
				ie.setActive(true);

				// 先頭コールサインの抽出
				String hisCall = "ALL";
				Pattern pattern = Pattern.compile("[0-9a-zA-Z\\/]+", Pattern.DOTALL);
				Matcher matcher = pattern.matcher(specifiedStationName2Send);
				try {
					if (matcher.find()) {
						hisCall = matcher.group();
						if (hisCall.toUpperCase().trim().equals("CQ"))
							hisCall = "ALL";
					}
				} catch (Exception e) {
					// hisCall = "ALL"のままとする。
				}
				ie.setDestination(hisCall);
				ie.setRealFileExists(true);
				ie.storeOwnData2Buffer();
				String decodedQRCode = fs.analyzeQRCode(newPath.toFile().toString());
				ie.setQrString(decodedQRCode);
				System.out.println("Registered as " + newPath.toFile().toString());
				if (decodedQRCode != null) {
					qrCodeDetected = true;
					System.out.println(" QR Code Detected.");
				}
			} catch (Exception e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
		}
		// 元の一時ファイルは削除してよい。
		for (String absFileName : inFilesList) {
			try {
				System.out.println("DELETING >" + absFileName);
				FileUtils.forceDelete(new File(absFileName));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return qrCodeDetected;
	}

	@RequestMapping(value = "changeDescription", method = { RequestMethod.POST, RequestMethod.GET })
	public ModelAndView actDataChangeDescription(@ModelAttribute CData4Upload params, HttpSession session,
			ModelAndView mav) {
		CYsfSdCHandlerProperties prop = CYsfSdCHandlerProperties.getInstance();
		CYsfFileSystem fs = CYsfFileSystem.getInstance();
		LinkedList<String> errorMessageList = new LinkedList<String>();

		if (!fs.isSdCardMounted()) {
			errorMessageList.add("SD-CARDが抜かれたか、未だ挿入されたSD-CARDのMOUNT処理がされていません。");
			fs.clearAll();
			setAllParameters4Mav(mav, errorMessageList, "", fs, "ERROR", prop, "pages/divImages");
			return mav;
		}

		// ImageMagickの存否チェック
		String imageMagickPathName4Check = prop.getImageMagickPath();
		File imProcessFile = new File(imageMagickPathName4Check + "convert.exe");
		if (!imProcessFile.exists()) {
			errorMessageList.add("ImageMagickの指定場所に処理プログラムが見つかりません。");
			errorMessageList.add("「対象設定」タブ中の「ImageMagickの記録場所」の指定を確認してください。");
			errorMessageList.add("ImageMagickの記録場所はフォルダを指定するので、末尾が'\\'で終わる必要があります。");
			setAllParameters4Mav(mav, errorMessageList, "", fs, "ERROR", prop, "pages/divImages");
			return mav;
		}

		if (params.getDescription2Change() == null || params.getDescription2Change().equals("")) {
			errorMessageList.add("変更する記述が記載されていません。");
			setAllParameters4Mav(mav, errorMessageList, "", fs, "ERROR", prop, "pages/divImages");
			return mav;
		}
		// YAESU SYSTEM FUSIONでの特殊カタカナに特化して対応文字の存否チェック
		// 一旦、UTF8 -> YSF特殊文字 -> UTF8 と変換をして、元に戻るかどうかで検証する。
		CYsfCodeConverter converter = CYsfCodeConverter.getInstance();
		String finalDescription = converter.ysfByte2Utf8(converter.utf82YsfByte(params.getDescription2Change().trim()));
		if (finalDescription != null && !finalDescription.equals(params.getDescription2Change())) {
			errorMessageList.add("「記述」に使えない文字がありましたので、「$」に置き換えてあります。");
			params.setDescription2Change(finalDescription);
		}
		// Description 中間にあるSPACE文字は、なぜかFTM-300の暴走を引き起こすので、アンダースコアに置換しておく。
		params.setDescription2Change(finalDescription.replaceAll(" ", "_"));

		fs.changeDescription(params.getTargetDataId(), params.getDescription2Change());
		fs.saveAllOfFilesOnPict(errorMessageList);
		fs.reNumberAndPrepareForPictureDisplay();
		setAllParameters4Mav(mav, errorMessageList, "", fs, getPresentUsingBranchName4Display(), prop,
				"pages/divImages");
		return mav;
	}

	@RequestMapping(value = "uploadNewImages", method = { RequestMethod.POST, RequestMethod.GET })
	public ModelAndView actDataUploadNewImages(@ModelAttribute CData4Upload params, HttpSession session,
			ModelAndView mav) {
		CYsfSdCHandlerProperties prop = CYsfSdCHandlerProperties.getInstance();
		CYsfFileSystem fs = CYsfFileSystem.getInstance();
		LinkedList<String> errorMessageList = new LinkedList<String>();

		if (!fs.isSdCardMounted()) {
			errorMessageList.add("SD-CARDが抜かれたか、未だ挿入されたSD-CARDのMOUNT処理がされていません。");
			fs.clearAll();
			setAllParameters4Mav(mav, errorMessageList, "", fs, "ERROR", prop, "pages/divImages");
			return mav;
		}

		// ImageMagickの存否チェック
		String imageMagickPathName4Check = prop.getImageMagickPath();
		File imProcessFile = new File(imageMagickPathName4Check + "convert.exe");
		if (!imProcessFile.exists()) {
			errorMessageList.add("ImageMagickの指定場所に処理プログラムが見つかりません。");
			errorMessageList.add("「対象設定」タブ中の「ImageMagickの記録場所」の指定を確認してください。");
			errorMessageList.add("ImageMagickの記録場所はフォルダを指定するので、末尾が'\\'で終わる必要があります。");
			setAllParameters4Mav(mav, errorMessageList, "", fs, "ERROR", prop, "pages/divImages");
			return mav;
		}

		String radioId = prop.getRadioId();

		File[] inFiles = params.getFile();
		String specifiedStationName2Send = params.getSpecifiedStn2Send();
		String specifiedColor = params.getSpecifiedColor();
		int posOfSuperImpose = params.getPosOfSuperImpose();

		// radioIdについてのチェック
		if (radioId == null || radioId.equals("")) {
			errorMessageList.add("RadioIdが指定されていません。");
		} else if (radioId.length() != 5) {
			errorMessageList.add("RadioIdが5桁ではありません。");
		}

		if (errorMessageList.size() != 0) {
			setAllParameters4Mav(mav, errorMessageList, getPresentUsingBranchName4Display(), fs, "ERROR", prop,
					"pages/divImages");
			return mav;
		}
		/*
		 * Path tempDirPath = null; try { tempDirPath =
		 * Files.createTempDirectory(Paths.get("c:/Temp/"), "FTMSDC"); assert ("test")
		 * != null; } catch (Exception e) {
		 * errorMessageList.add("一時ディレクトリを生成できませんでした。"); return mav; }
		 */
		if (!fs.isMounted()) {
			errorMessageList.add("先にMOUNT/LOAD機能を使って、SDカードの設定を確認してください。");
			setAllParameters4Mav(mav, errorMessageList, "", fs, "ERROR", prop, "pages/divImages");
			return mav;
		}
		// ここまで来て、準備が出来上がったので、いよいよ書き込みを開始する。
		if (inFiles != null) {
			convertTempFilesAndStore2Target(inFiles, params, specifiedStationName2Send, posOfSuperImpose,
					specifiedColor);
		} else {
			errorMessageList.add("少なくとも、ひとつの画像ファイルを指定してください。");
		}
		fs.saveAllOfFilesOnPict(errorMessageList);
		fs.reNumberAndPrepareForPictureDisplay();
		setAllParameters4Mav(mav, errorMessageList, "", fs, getPresentUsingBranchName4Display(), prop,
				"pages/divImages");
		return mav;
	}

	/**
	 * 
	 * 
	 * @param sdCardBaseDirectoryName YSFにおけるルートディレクトリをドライブ名も含めて指定する。たとえば、実機に用いるSDCARDがFドライブにあるなら、"F:/"
	 *                                を指定することになる。
	 * 
	 * @return エラーがあったときには、そのエラーメッセージが含まれる。なければ、size()==0のLinkedListが返ってくる。
	 */
	/**
	 * backupPresentPhotoDirは、YSFのうち、PHOTOディレクトリにあるものをバックアップする。
	 * 
	 * @return
	 */
	private LinkedList<String> backupPresentPhotoDir2Bank() {
		CYsfSdCHandlerProperties prop = CYsfSdCHandlerProperties.getInstance();

		LinkedList<String> errorMessageList = new LinkedList<String>();
		String backUpSourceDirectory = prop.getStrPhotoDirectoryPath();

		// まずは、現時点でのディレクトリファイル(QSOPCTDIRとQSOPCTFAT)をそのままPHOTOディレクトリ中に複写記録しておく。
		try {
			String sdCardPctDirFile = prop.getStrQsoPctDirFilePath();
			String sdCardPctFatFile = prop.getStrQsoPctFatFilePath();
			File photoDirectory = new File(prop.getStrPhotoDirectoryPath());
			FileUtils.copyFileToDirectory(new File(sdCardPctDirFile), photoDirectory);
			FileUtils.copyFileToDirectory(new File(sdCardPctFatFile), photoDirectory);
		} catch (IOException e) {
			errorMessageList.add("DIRデータ(QSOPCTDIR,QSOPCTFAT)をPHOTOフォルダに複写できませんでした。");
			return errorMessageList;
		}

		// 次に、リカバリ用のディレクトリ中に現在の日付日時をディレクトリ名に含めたディレクトリを生成し、PHOTOディレクトリ中にあった全ファイルをそこに移動する。
		try {
			String backUpDestinationParentDirectoryName = prop.getRootDir4Bank();
			String backUpDestinationDirectoryName = prop.getRootDir4Bank()
					+ dupNameCheckAndRename2UniqueName(getPresentUsingBranchName());

			// リカバリ専用ディレクトリルートの存否チェックと、ないときの新規生成
			File destinationParentDirectory = new File(backUpDestinationParentDirectoryName);
			if (!destinationParentDirectory.exists()) {
				// バックアップ用まとめフォルダがないので、これを生成する。
				FileUtils.forceMkdir(destinationParentDirectory);
			}
			File backUpDestinationDirectory = new File(backUpDestinationDirectoryName);
			FileUtils.copyDirectory(new File(backUpSourceDirectory), backUpDestinationDirectory);

			// 戻した（若しくは移行した）ディレクトリのタイムスタンプを直近に設定する。
			FileTime fileTime = FileTime.from(Instant.now());
			Files.setLastModifiedTime(backUpDestinationDirectory.toPath(), fileTime);

			if (getPresentUsingBranchNameWithNullIfNotExists() != null)
				FileUtils.forceDelete(new File(prop.getPresentFolderInfoFileName()));
		} catch (IOException e) {
			errorMessageList.add("ディレクトリのバックアップがとれませんでした。");
			return errorMessageList;
		}
		return errorMessageList;
	}

	/**
	 * dupNameCheckAndRename2UniqueNameは、引数newBranchDirNameと同名称のBANKがあれば、(1),(2)...とサフィックスを付けて
	 * 返す。なお、A(1),A(2),A(3),....という名称のファイルがあるが、Ａ(1)を新名称に指示しても、「Ａ」がないので、これが返るようになっている。
	 * 
	 * @param newBranchDirName
	 * @return
	 */
	private String dupNameCheckAndRename2UniqueName(String newBranchDirName) {
		// 新規名称が既にあるものかどうか？なければそのままOK。
		CYsfSdCHandlerProperties prop = CYsfSdCHandlerProperties.getInstance();
		boolean found = false;
		String bankRootDirName = prop.getRootDir4Bank();
		File recoveryRootDir = new File(bankRootDirName);

		// まずは、同一名称のBANKがあるかをチェック。
		for (File f : recoveryRootDir.listFiles()) {
			if (f.isDirectory()) {
				if (f.getName().equals(newBranchDirName)) {
					found = true;
					break;
				}
			}
		}
		String basicBranchName = new String(newBranchDirName);
		// 新規名称が既にあったら、（n）を付けたものが既にあるかどうか？なければ(1)を付けてOK。
		// nを順に繰り上げていく。
		if (found) {
			// 最初にBranchBase名を抽出する。末尾に"（n）"がついている名称は、これを除いた部分がbasicBranchNameとなっているはず。
			Pattern pattern = Pattern.compile("([^(]+)(\\([0-9]+\\))*", Pattern.DOTALL);
			Matcher matcher = pattern.matcher(basicBranchName);
			if (matcher.find()) {
				basicBranchName = matcher.group(1);
			}

			for (int ix = 1;; ix++) {
				newBranchDirName = basicBranchName + "(" + ix + ")";
				found = false;
				for (File f : recoveryRootDir.listFiles()) {
					if (f.isDirectory()) {
						if (f.getName().equals(newBranchDirName)) {
							found = true;
							break;
						}
					}
				}
				if (!found)
					break;
			}
		}
		return newBranchDirName;
	}

	private String getPresentUsingBranchName() {
		String presentUsingBranchName = getPresentUsingBranchNameWithNullIfNotExists();

		if (presentUsingBranchName == null)
			presentUsingBranchName = "PHOTO_"
					+ (new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance().getTime()));

		return presentUsingBranchName;
	}

	private String getPresentUsingBranchName4Display() {
		String data = getPresentUsingBranchNameWithNullIfNotExists();
		if (data == null)
			data = "新規登録";
		return data;
	}

	/**
	 * 
	 * @return
	 */
	private String getPresentUsingBranchNameWithNullIfNotExists() {
		String presentUsingBranchName = null;
		CYsfSdCHandlerProperties prop = CYsfSdCHandlerProperties.getInstance();

		Path usingBranchName = Paths.get(prop.getPresentFolderInfoFileName());
		if (usingBranchName.toFile().exists()) {
			try {
				List<String> presentBranchName = Files.readAllLines(usingBranchName, StandardCharsets.UTF_8);
				presentUsingBranchName = presentBranchName.getFirst();
				return presentUsingBranchName;
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		} else {
			return null;
		}
	}

	/**
	 * QSOLOG内、branchNameHolderファイル中に、移動させるディレクトリの名称を記録する。branchNameHolderファイルには、現在表「画像処理」面に
	 * 表示されているメモリバンク名のみが記録されている。
	 * 
	 * @param contentFileName
	 */
	private void setPresentUsingBranchName(String contentFileName) {

		CYsfSdCHandlerProperties prop = CYsfSdCHandlerProperties.getInstance();
		File usingBranchNameFile = new File(prop.getPresentFolderInfoFileName());

		if (usingBranchNameFile.exists()) {
			try {
				FileUtils.forceDelete(usingBranchNameFile);
			} catch (IOException e) {
				System.out.println("なんで削除できんの？(setPresentUsingBranchName)" + prop.getPresentFolderInfoFileName());
			}
		}
		try (FileOutputStream fo = new FileOutputStream(usingBranchNameFile);) {
			fo.write(contentFileName.getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		return;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected LinkedList<CImageEntry> createResultContentsListSeed() {
		return new LinkedList<CImageEntry>();
	}

}
