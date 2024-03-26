package net.dialectech.ftmSdCardHandler.actions;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IM4JavaException;
import org.im4java.core.IMOperation;
import org.im4java.core.ImageMagickCmd;
import org.im4java.process.ArrayListOutputConsumer;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import jakarta.servlet.http.HttpSession;
import lombok.Getter;
import lombok.Setter;
import net.dialectech.ftmSdCardHandler.data.CData4Upload;
import net.dialectech.ftmSdCardHandler.data.CDirStructure;
import net.dialectech.ftmSdCardHandler.supporters.CConst;
import net.dialectech.ftmSdCardHandler.supporters.CYsfSdCHandlerProperties;
import net.dialectech.ftmSdCardHandler.supporters.dialectechSup.CDltFileUtilities;
import net.dialectech.ftmSdCardHandler.supporters.dialectechSup.CDltFlowsException;
import net.dialectech.ftmSdCardHandler.supporters.dialectechSup.CDltImageIO;
import net.dialectech.ftmSdCardHandler.supporters.dialectechSup.CDltSpringFileStream;
import net.dialectech.ftmSdCardHandler.supporters.fileSystem.CImageEntry;
import net.dialectech.ftmSdCardHandler.supporters.fileSystem.CYsfFileSystem;
import net.dialectech.ftmSdCardHandler.supporters.fileSystem.CYsfFileSystemCorePart;

@Controller
@RequestScope
@RequestMapping("/execute")
public class CHandlerAction {

	@Getter
	@Setter
	protected CDltSpringFileStream springFileStream;

	@RequestMapping(value = "base", method = { RequestMethod.POST, RequestMethod.GET })
	public ModelAndView actDataBase(@ModelAttribute CData4Upload param, HttpSession session, ModelAndView mav) {
		CYsfSdCHandlerProperties prop = CYsfSdCHandlerProperties.getInstance();

		mav.addObject("prop", prop);
		mav.setViewName("pages/base");
		return mav;
	}

	@RequestMapping(value = "deleteCompletely", method = { RequestMethod.POST, RequestMethod.GET })
	public ModelAndView actDataDeleteCompletely(@ModelAttribute CData4Upload param, HttpSession session,
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

		int targetImageId = param.getTargetImageId();
		LinkedList<CImageEntry> dirListWDO = fs.getPctDirListWithDisplayOrder();

		String targetDirName = prop.getStrPhotoDirectoryPath();

		for (int index = 0; index < dirListWDO.size(); ++index) {
			CImageEntry data = dirListWDO.get(index);
			if (data.getImageId() == targetImageId) {
				// 完全削除を指示されたターゲットdirEntryはdataに記録されている。
				data.setActive(false);
				// 完全Deleteした場合、戻せないように、通常の流れのimageIdと異なる番号（-1）を付与して、検索されないようにする。
				data.setImageId(-1);

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
				fs.removeThis(data);
				break;
			}
		}

		fs.saveAll(errorMessageList);
		fs.reNumberAndPrepareForDisplay();
		setAllParameters4Mav(mav, errorMessageList, "", fs, getPresentUsingBranchName4Display(), prop,
				"pages/divImages");
		mav.setViewName("pages/divImages");
		return mav;
	}

	@RequestMapping(value = "deleteMarking", method = { RequestMethod.POST, RequestMethod.GET })
	public ModelAndView actDataDeleteMarking(@ModelAttribute CData4Upload param, HttpSession session,
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

		int targetImageId = param.getTargetImageId();
		LinkedList<CImageEntry> dirList = fs.getPctDirList();
		for (int index = 0; index < dirList.size(); ++index) {
			CImageEntry elem = dirList.get(index);
			if (elem.getImageId() == targetImageId) {
				elem.setActive(false);
				elem.storeOwnData2BufferedBytes();
				break;
			}
		}

		fs.saveAll(errorMessageList);
		fs.reNumberAndPrepareForDisplay();
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
		} catch (IOException e) {
			errorMessageList.add("削除に失敗しました。");
		}
		setAllParameters4Mav(mav, errorMessageList, "", fs, "", prop, "pages/divResDeleteBankDir");
		return mav;
	}

	@RequestMapping(value = "downloadImage", method = { RequestMethod.POST, RequestMethod.GET })
	public ResponseEntity<StreamingResponseBody> actDataDownloadImage(@ModelAttribute CData4Upload params,
			HttpSession session, ModelAndView mav) {
		CYsfFileSystem fs = CYsfFileSystem.getInstance();
		CYsfSdCHandlerProperties prop = CYsfSdCHandlerProperties.getInstance();
		// LinkedList<String> errorMessageList = new LinkedList<String>();

//		if (!fs.isSdCardMounted()) {
//			return forceStreamAsImage("notLoaded.jpg").prepareDownload();
//		}

		String fileName;
		if (params.getBranchDirName() == null || params.getBranchDirName().equals("")) {
			fileName = prop.getSdCardBaseDirName() + "PHOTO" + File.separator + params.getFileName2Dowload();
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
		if (!(new File(sdCardBaseDirectory + "QSOLOG/")).exists()) {
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

		fs.setActive(true);
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
		fs.saveAll(errorMessageList);
		fs.reNumberAndPrepareForDisplay();
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
		fs.saveAll(errorMessageList);
		fs.reNumberAndPrepareForDisplay();
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

		int targetImageId = param.getTargetImageId();
		LinkedList<CImageEntry> dirList = fs.getPctDirListWithDisplayOrder();
		for (int index = 0; index < dirList.size(); ++index) {
			if (dirList.get(index).getImageId() == targetImageId) {
				dirList.get(index).setActive(true);
				dirList.get(index).storeOwnData2BufferedBytes();
				break;
			}
		}
		fs.saveAll(errorMessageList);
		fs.reNumberAndPrepareForDisplay();
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

//		if (!fs.isSdCardMounted()) {
//			errorMessageList.add("SD-CARDが抜かれたか、未だ挿入されたSD-CARDのMOUNT処理がされていません。");
//			setAllParameters4Mav(mav, errorMessageList, "", fs, getPresentUsingBranchName4Display(), prop,
//					"pages/divResRenameOneOfBankDir");
//			fs.clearAll();
//			return mav;
//		}

		String newBranchDirName = params.getNewBranchDirName();
		String targetBranchDirName = params.getBranchDirName();

		if (newBranchDirName != null && newBranchDirName.equals(targetBranchDirName)) {
			errorMessageList.add("元の名称と同じなので、何もしません。");
			setAllParameters4Mav(mav, errorMessageList, params.getBranchDirName(), fs, "", prop,
					"pages/divResRenameOneOfBankDir");

			mav.addObject("newBranchDirName", newBranchDirName);
			return mav;
		}
		// 新規名称が既にあるものかどうか？なければそのままOK、あったら、自動で付与された名称（但し酷似^^;）を新バンク名として利用
		newBranchDirName = dupNameCheckAndRename2UniqueName(newBranchDirName);

		// System.out.println(">>" + newBranchDirName);
		try {
			FileUtils.moveDirectory(new File(prop.getRootDir4Bank() + targetBranchDirName),
					new File(prop.getRootDir4Bank() + newBranchDirName));
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

		setAllParameters4Mav(mav, errorMessageList, params.getBranchDirName(), fs, "", prop,
				"pages/divResRenameOneOfBankDir");

		mav.addObject("newBranchDirName", newBranchDirName);
		return mav;
	}

	@RequestMapping(value = "settleOfProperty", method = { RequestMethod.POST, RequestMethod.GET })
	public ModelAndView actDataSettleOfProperty(@ModelAttribute CData4Upload param, HttpSession session,
			ModelAndView mav) {
		CYsfFileSystem fs = CYsfFileSystem.getInstance();
		CYsfSdCHandlerProperties prop = CYsfSdCHandlerProperties.getInstance();
		LinkedList<String> errorMessageList = new LinkedList<String>();

		prop.setSdCardDrive(param.getSdCardDrive());
		prop.setRadioId(param.getRadioId());
		prop.setMyCallSign(param.getMyCallSign());
		prop.setImageMagickPath(param.getImageMagickPath());
		prop.setBrowserPath(param.getBrowserPath());
		prop.setTopLetterOfPhotoFile(param.getTopLetterOfPhotoFile());
		prop.setStrFillByteInPctDir(param.getStrFillByteInPctDir());
		prop.setListStepSize(param.getListStepSize());
		;
		if (param.getOffset4Debug() != null && !param.getOffset4Debug().equals("")) {
			prop.setStrOffset4Debug(param.getOffset4Debug());
		}
		try {
			prop.saveAll();
		} catch (IOException e) {
			errorMessageList.add("プロパティファイルを書き込めませんでした。");
		}

		setAllParameters4Mav(mav, errorMessageList, "", fs, "", prop, "pages/divResOfSettlement");
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
		LinkedList<CDirStructure> dirStructureList = new LinkedList<CDirStructure>();

		for (File f : bankDirectory.listFiles()) {
			// リカバリディレクトリには、ファイルを置けるようにして、データは更にリカバリディレクトリの下のディレクトリ中に配置するようにした。
			if (f.isDirectory()) {
				CDirStructure data = new CDirStructure(f.getAbsoluteFile(), new Date(f.lastModified()));
				dirStructureList.add(data);
			}
		}

		String sortingOrder = param.getSortingOrder();
		// 一応、保険程度に。
		if (sortingOrder == null)
			sortingOrder = "";

		Comparator<CDirStructure> comparator;
		// この程度なので、Builder Patternとか、Strategy Patternとかは使わなくてよいだろう。
		switch (sortingOrder) {
		case "timeReverseOrder":
			comparator = new Comparator<CDirStructure>() {
				@Override
				public int compare(CDirStructure o1, CDirStructure o2) {
					if (o1.getFolderPath().toFile().lastModified() < o2.getFolderPath().toFile().lastModified()) {
						return -1;
					} else if (o1.getFolderPath().toFile().lastModified() > o2.getFolderPath().toFile()
							.lastModified()) {
						return 1;
					} else {
						return 0;
					}
				}
			};
			break;
		case "timeOrder":
			comparator = new Comparator<CDirStructure>() {
				@Override
				public int compare(CDirStructure o1, CDirStructure o2) {
					if (o1.getFolderPath().toFile().lastModified() > o2.getFolderPath().toFile().lastModified()) {
						return -1;
					} else if (o1.getFolderPath().toFile().lastModified() < o2.getFolderPath().toFile()
							.lastModified()) {
						return 1;
					} else {
						return 0;
					}
				}
			};
			break;
		case "nameReverseOrder":
			comparator = new Comparator<CDirStructure>() {
				@Override
				public int compare(CDirStructure o1, CDirStructure o2) {
					Collator col = Collator.getInstance(Locale.JAPAN);
					return -col.compare(o1.getFolderPath().toFile().getName(), o2.getFolderPath().toFile().getName());
				}
			};
			break;
		case "nameOrder":
		default:
			comparator = new Comparator<CDirStructure>() {
				@Override
				public int compare(CDirStructure o1, CDirStructure o2) {
					Collator col = Collator.getInstance(Locale.JAPAN);
					return col.compare(o1.getFolderPath().toFile().getName(), o2.getFolderPath().toFile().getName());
				}
			};
			break;
		}

		Collections.sort(dirStructureList, comparator);

		// リスト表示の対象となる部分を抽出する。
		if (param.getStartFrom() != 1 && param.getStartFrom() > dirStructureList.size()) {
			// 指定された開始位置がリストの最長より大きいときハッキングか？
			errorMessageList.add("リスト表示開始位置の指示が異常です。");
			setAllParameters4Mav(mav, errorMessageList, param.getBranchDirName(), fs, "", prop,
					"pages/divShowBankList");
			return mav;
		}

		int startFrom = param.getStartFrom() == 0 ? 0 : param.getStartFrom() - 1; // LinkedListは0オリジン。
		int listUntil = startFrom + prop.getListStepSize();
		if (listUntil >= dirStructureList.size())
			listUntil = dirStructureList.size();

		LinkedList<Integer> targetPosList = new LinkedList<Integer>();
		for (int briefStop = 0; briefStop < dirStructureList.size(); briefStop += prop.getListStepSize())
			targetPosList.add(briefStop + 1);
		dirStructureList = new LinkedList<CDirStructure>(dirStructureList.subList(startFrom, listUntil));

		setAllParameters4Mav(mav, errorMessageList, param.getBranchDirName(), fs, "", prop, "pages/divShowBankList");
		// このメソッドで特有となる各データを更に記録。
		mav.addObject("presentPos", param.getStartFrom());
		mav.addObject("targetPosList", targetPosList);
		mav.addObject("dirStructureList", dirStructureList);
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
		if (!fs.isActive()) {
			errorMessageList.add("先にMOUNT/LOAD機能を使って、SDカードの設定を確認してください。");
			setAllParameters4Mav(mav, errorMessageList, "", fs, "ERROR", prop, "pages/divImages");
			return mav;
		}
		// ここまで来て、準備が出来上がったので、いよいよ書き込みを開始する。
		if (inFiles != null) {
			LinkedList<String> inFilesList = new LinkedList<String>();
			for (int index = 0; index < inFiles.length; ++index) {
				try {
					String inAbsoluteName = "c:\\Temp\\" + inFiles[index].getName();
					inFilesList.add(inAbsoluteName);
					CImageEntry ie = fs.addNewFile(prop.getMyCallSign());

					String targetDirName = prop.getStrPhotoDirectoryPath();

					//					File fileWorkingDir = new File(prop.getStrFileWorkingDirectoryPath());
					//					if (!fileWorkingDir.exists()) {
					//						FileUtils.forceMkdir(fileWorkingDir);
					//					}
					Path newPath = Paths.get(targetDirName, ie.getFileCoreName());

					//  commandの生成
					//ImageMagickCmd cmd = new ImageMagickCmd("magick");
					ConvertCmd cmd = new ConvertCmd();
					cmd.setAsyncMode(false);
					String imageMagickPathName = prop.getImageMagickPath();

					cmd.setSearchPath(imageMagickPathName);
					// operation内容の生成。
					IMOperation op = new IMOperation();
					op.addImage(inAbsoluteName);
					// 出力ファイルはJPEG
					op.define("jpeg:extent=" + params.getVolumeTarget().trim() + "kB");
					// 大きさを正規化
					switch (params.getImageSize()) {
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
					} catch (InterruptedException | IM4JavaException e) {
						// TODO 自動生成された catch ブロック
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
					ie.storeOwnData2BufferedBytes();
					System.out.println("Registered as " + newPath.toFile().toString());

				} catch (IOException e) {
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
		} else {
			errorMessageList.add("少なくとも、ひとつの画像ファイルを指定してください。");
		}
		fs.saveAll(errorMessageList);
		fs.reNumberAndPrepareForDisplay();
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

	protected CDltSpringFileStream forceStreamAsImage(String imageName) {
		CDltSpringFileStream springFileStream;
		try {
			CDltImageIO imageHandler = CDltImageIO.getInstance();
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			String imageExtention = CDltFileUtilities.extentionOfPath(imageName);
			bos = imageHandler.readFromInputStream2ByteArrayOutputStreamOf(
					this.getClass().getResourceAsStream("/net/dialectech/commons/dltFlows/images/" + imageName),
					imageExtention);
			springFileStream = new CDltSpringFileStream(bos, "readError." + imageExtention, "image/jpeg");
		} catch (CDltFlowsException e1) {
			springFileStream = null;
		}
		return springFileStream;
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
	 * ModelAndViewであるmavに幾つかの設定値を載せて、リクエストのレスポンスメソッドから抜け出る準備をする。
	 * 
	 * @param mav
	 * @param errorMessageList
	 * @param branchName
	 * @param fs
	 * @param pageTitle
	 * @param prop
	 * @param viewName
	 */
	void setAllParameters4Mav(ModelAndView mav, LinkedList<String> errorMessageList, String branchName,
			CYsfFileSystemCorePart fs, String pageTitle, CYsfSdCHandlerProperties prop, String viewName) {
		mav.addObject("errorMessageList", errorMessageList);
		mav.addObject("branchDirName", branchName);
		mav.addObject("fs", fs);
		mav.addObject("title", pageTitle);
		mav.addObject("prop", prop);
		mav.setViewName(viewName);
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

}
