package net.dialectech.ftmSdCardHandler.actions;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import jakarta.servlet.http.HttpSession;
import net.dialectech.ftmSdCardHandler.actions.suppoters.CHandlerActionFundamental;
import net.dialectech.ftmSdCardHandler.data.CData4Upload;
import net.dialectech.ftmSdCardHandler.data.CVoiceEntry;
import net.dialectech.ftmSdCardHandler.supporters.CConst;
import net.dialectech.ftmSdCardHandler.supporters.CYsfSdCHandlerProperties;
import net.dialectech.ftmSdCardHandler.supporters.dialectechSup.CDltFlowsException;
import net.dialectech.ftmSdCardHandler.supporters.dialectechSup.CDltSpringFileStream;
import net.dialectech.ftmSdCardHandler.supporters.fileSystem.CYsfFileSystem;

@Controller
@RequestScope
@RequestMapping("/execute/voices")
public class CHandlerVoicesAction extends CHandlerActionFundamental {

	@RequestMapping(value = "renameVoiceFile", method = { RequestMethod.POST, RequestMethod.GET })
	public ModelAndView actDataRenameVoiceFile(@ModelAttribute CData4Upload params, HttpSession session,
			ModelAndView mav) {
		CYsfSdCHandlerProperties prop = CYsfSdCHandlerProperties.getInstance();
		CYsfFileSystem fs = CYsfFileSystem.getInstance();
		LinkedList<String> errorMessageList = new LinkedList<String>();

		if (!fs.isSdCardMounted()) {
			errorMessageList.add("SD-CARDが抜かれたか、未だ挿入されたSD-CARDのMOUNT処理がされていません。");
			errorMessageList.add("MOUNTしなければ、「画像処理」プレーンでの画像処理ができません。");
			fs.clearAll();
		}

		if (params.getDescription2Change() == null || params.getDescription2Change().equals("")) {
			errorMessageList.add("修正後のファイル名を指定してください。");
			setAllParameters4Mav(mav, errorMessageList, "", fs, "", prop, "pages/divVoices");
			return mav;
		}
		String description = params.getDescription2Change().replaceAll(" ", "_");
		if (!description.matches("[a-zA-Z0-9\\_!#$%&\\(\\)\\+\\-;]+")) {
			errorMessageList.add("所定以外の文字を使用しています。");
			setAllParameters4Mav(mav, errorMessageList, "", fs, "", prop, "pages/divVoices");
			return mav;
		}
		if (fs.findVoiceFile(description) != null) {
			errorMessageList.add("指定されたファイル名「" + description + "」は既に存在します。別名を指定してください。");
			setAllParameters4Mav(mav, errorMessageList, "", fs, "", prop, "pages/divVoices");
			mav = createContentsList(mav, params.getStartFrom(), params.getSortingOrder(),
					fs.getVoiceDirListWithDisplayOrder(), errorMessageList);
			return mav;
		}
		String newNameCore = description.replaceAll("\\.wav$", "");
		newNameCore = cutAndFillString(newNameCore, 12);
		if (fs.findVoiceFile(newNameCore) != null) {
			errorMessageList
					.add("指定されたファイル名「" + params.getDescription2Change() + "」は、システム内で「" + newNameCore + "」に変換されます。");
			errorMessageList.add("ファイル名「" + newNameCore + "」は既に存在します。");
			errorMessageList.add("別名を指定してください。");
			setAllParameters4Mav(mav, errorMessageList, "", fs, "", prop, "pages/divVoices");
			mav = createContentsList(mav, params.getStartFrom(), params.getSortingOrder(),
					fs.getVoiceDirListWithDisplayOrder(), errorMessageList);
			return mav;
		}
		String newName = newNameCore + ".wav";

		for (CVoiceEntry voiceData : fs.getVoiceDirList()) {
			if (params.getTargetDataId() == voiceData.getDataId()) {
				String formerFileName = prop.getStrVoiceDirectoryPath() + voiceData.getFileName();
				String newFileName = prop.getStrVoiceDirectoryPath() + newName;
				File formerFile = new File(formerFileName);
				File newFile = new File(newFileName);
				try {
					FileUtils.moveFile(formerFile, newFile);
					voiceData.setFileName(newName);
				} catch (IOException e) {
					// TODO 自動生成された catch ブロック
					e.printStackTrace();
					break;
				}
			}
		}

		fs.saveAllOfFilesOnVoice(errorMessageList);
		setAllParameters4Mav(mav, errorMessageList, "", fs, "", prop, "pages/divVoices");
		mav = createContentsList(mav, params.getStartFrom(), params.getSortingOrder(),
				fs.getVoiceDirListWithDisplayOrder(), errorMessageList);
		return mav;
	}

	@RequestMapping(value = "deleteVoiceCompletely", method = { RequestMethod.POST, RequestMethod.GET })
	public ModelAndView actDataDeleteVoiceCompletely(@ModelAttribute CData4Upload params, HttpSession session,
			ModelAndView mav) {
		CYsfSdCHandlerProperties prop = CYsfSdCHandlerProperties.getInstance();
		CYsfFileSystem fs = CYsfFileSystem.getInstance();
		LinkedList<String> errorMessageList = new LinkedList<String>();

		if (!fs.isSdCardMounted()) {
			errorMessageList.add("SD-CARDが抜かれたか、未だ挿入されたSD-CARDのMOUNT処理がされていません。");
			errorMessageList.add("MOUNTしなければ、「画像処理」プレーンでの画像処理ができません。");
			fs.clearAll();
		}

		for (CVoiceEntry voiceData : fs.getVoiceDirList()) {
			if (params.getTargetDataId() == voiceData.getDataId()) {
				String formerFileName = prop.getStrVoiceDirectoryPath() + voiceData.getFileName();
				File formerFile = new File(formerFileName);
				try {
					fs.getVoiceDirList().remove(voiceData);
					fs.getVoiceDirListWithDisplayOrder().remove(voiceData);
					FileUtils.forceDelete(formerFile);
				} catch (IOException e) {
					e.printStackTrace();
					break;
				}
				break;
			}
		}

		fs.saveAllOfFilesOnVoice(errorMessageList);
		setAllParameters4Mav(mav, errorMessageList, "", fs, "", prop, "pages/divVoices");
		mav = createContentsList(mav, params.getStartFrom(), params.getSortingOrder(),
				fs.getVoiceDirListWithDisplayOrder(), errorMessageList);

		return mav;
	}

	@RequestMapping(value = "deleteVoiceMarking", method = { RequestMethod.POST, RequestMethod.GET })
	public ModelAndView actDataDeleteVoiceMarking(@ModelAttribute CData4Upload params, HttpSession session,
			ModelAndView mav) {
		CYsfSdCHandlerProperties prop = CYsfSdCHandlerProperties.getInstance();
		CYsfFileSystem fs = CYsfFileSystem.getInstance();
		LinkedList<String> errorMessageList = new LinkedList<String>();

		if (!fs.isSdCardMounted()) {
			errorMessageList.add("SD-CARDが抜かれたか、未だ挿入されたSD-CARDのMOUNT処理がされていません。");
			errorMessageList.add("MOUNTしなければ、「画像処理」プレーンでの画像処理ができません。");
			fs.clearAll();
		}

		for (CVoiceEntry voiceData : fs.getVoiceDirList()) {
			if (params.getTargetDataId() == voiceData.getDataId()) {
				voiceData.setActive(false);
			}
		}

		fs.saveAllOfFilesOnVoice(errorMessageList);
		setAllParameters4Mav(mav, errorMessageList, "", fs, "", prop, "pages/divVoices");
		mav = createContentsList(mav, params.getStartFrom(), params.getSortingOrder(),
				fs.getVoiceDirListWithDisplayOrder(), errorMessageList);

		return mav;
	}

	@RequestMapping(value = "recoverVoiceMarking", method = { RequestMethod.POST, RequestMethod.GET })
	public ModelAndView actDataRecoverVoiceMarking(@ModelAttribute CData4Upload params, HttpSession session,
			ModelAndView mav) {
		CYsfSdCHandlerProperties prop = CYsfSdCHandlerProperties.getInstance();
		CYsfFileSystem fs = CYsfFileSystem.getInstance();
		LinkedList<String> errorMessageList = new LinkedList<String>();

		if (!fs.isSdCardMounted()) {
			errorMessageList.add("SD-CARDが抜かれたか、未だ挿入されたSD-CARDのMOUNT処理がされていません。");
			errorMessageList.add("MOUNTしなければ、「画像処理」プレーンでの画像処理ができません。");
			fs.clearAll();
		}

		for (CVoiceEntry voiceData : fs.getVoiceDirList()) {
			if (params.getTargetDataId() == voiceData.getDataId()) {
				voiceData.setActive(true);
			}
		}

		fs.saveAllOfFilesOnVoice(errorMessageList);
		setAllParameters4Mav(mav, errorMessageList, "", fs, "", prop, "pages/divVoices");
		mav = createContentsList(mav, params.getStartFrom(), params.getSortingOrder(),
				fs.getVoiceDirListWithDisplayOrder(), errorMessageList);

		return mav;
	}

	private String cutAndFillString(String newName, int stringLength) {

		for (int index = newName.length(); index < stringLength; ++index) {
			newName += "_";
		}
		return newName.substring(0, stringLength);
	}

	@RequestMapping(value = "downloadVoice", method = { RequestMethod.POST, RequestMethod.GET })
	public ResponseEntity<StreamingResponseBody> actDataDownloadVoice(@ModelAttribute CData4Upload params,
			HttpSession session, ModelAndView mav) {
		CYsfFileSystem fs = CYsfFileSystem.getInstance();
		CYsfSdCHandlerProperties prop = CYsfSdCHandlerProperties.getInstance();
		int dataId = params.getTargetDataId();
		CVoiceEntry voiceEntry = fs.getVoiceDirList().get(dataId - 1);

		if (!fs.isSdCardMounted()) {
			springFileStream = forceStreamAsVoice("SD-MOUNT.wav");
			return springFileStream.prepareDownload();
		}

		String fileName = prop.getSdCardBaseDirName() + CConst.VoiceFoldername + File.separator
				+ voiceEntry.getFileName();
		ResponseEntity<StreamingResponseBody> streamData;
		try {
			File targetFile = new File(fileName);
			// 実ファイルパスに基づいてspringFileStreamを生成
			if (targetFile.exists()) {
				springFileStream = new CDltSpringFileStream(targetFile, voiceEntry.getFileName());
				streamData = springFileStream.prepareDownload();
				return streamData;
			} else {
				springFileStream = forceStreamAsVoice("fileNotFound.wav");
				return springFileStream.prepareDownload();
			}
		} catch (CDltFlowsException e) {
			springFileStream = forceStreamAsVoice("fileNotFound.wav");
			return springFileStream.prepareDownload();
		}
	}

	private CDltSpringFileStream forceStreamAsVoice(String imageName) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] data;
		try {
			data = this.getClass().getResourceAsStream("/net/dialectech/commons/dltFlows/voice/" + imageName)
					.readAllBytes();
			bos.write(data);
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		return springFileStream = new CDltSpringFileStream(bos, "readError.wav", "image/jpeg");
	}

	/**
	 * actDataMountAndLoadPresentVoices()は"mountAndLoadPresentVoices"リクエストによってキックされるが、
	 * actDataMountAndLoadPresentMessagesと同様、mountAndLoadPresentImagesリクエストによってキックされた
	 * 上で呼ばれることを前提とする。
	 * 
	 * @param params
	 * @param session
	 * @param mav
	 * @return
	 */
	@RequestMapping(value = "mountAndLoadPresentVoices", method = { RequestMethod.POST, RequestMethod.GET })
	public ModelAndView actDataMountAndLoadPresentVoices(@ModelAttribute CData4Upload params, HttpSession session,
			ModelAndView mav) {
		CYsfSdCHandlerProperties prop = CYsfSdCHandlerProperties.getInstance();
		CYsfFileSystem fs = CYsfFileSystem.getInstance();
		String sdCardBaseDirectory = prop.getSdCardBaseDirName();
		LinkedList<String> errorMessageList = new LinkedList<String>();
		String errorMsg = null;

		int mountWaitCounter = 0;
		while (!fs.isMounted()) {
			try {
				Thread.sleep(100);
				mountWaitCounter++;
			} catch (InterruptedException e) {
				// 何もしなくてもよい。
			}
			// 10秒越えてもmountが終了していないときには、あらためてmount処理をする。
			if (mountWaitCounter > 100) {
				errorMsg = fs.loadFromSDCard();
				break;
			}
		}

		if (errorMsg != null) {
			errorMessageList.add(errorMsg);
			errorMessageList.add("SD-CARDの構成が不完全です。");
			setAllParameters4Mav(mav, errorMessageList, "", fs, "ERROR", prop, "pages/divVoices");
			fs.clearAll();
			return mav;
		}

		if (!(new File(sdCardBaseDirectory + CConst.QsoLogFolderName + File.separator)).exists()) {
			errorMessageList.add("対応するSD-CARDが装着されていません。");
			setAllParameters4Mav(mav, errorMessageList, "", fs, "ERROR", prop, "pages/divImages");
			fs.clearAll();
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
		setAllParameters4Mav(mav, errorMessageList, "", fs, "", prop, "pages/divVoices");

		LinkedList<Integer> targetPosList = new LinkedList<Integer>();
		LinkedList<CVoiceEntry> voiceList = fs.getVoiceDirListWithDisplayOrder();

		for (int briefStop = 0; briefStop < voiceList.size(); briefStop += prop.getListStepSize())
			targetPosList.add(briefStop + 1); // 表示は１オリジン。

		setAllParameters4Mav(mav, errorMessageList, "", fs, "", prop, "pages/divVoices");
		// このメソッドで特有となる各データを更に記録。
		mav.addObject("presentPos", 1); // 今表示しようとしている音声ファイルリストのリスト先頭ID候補の値。
		mav.addObject("targetPosList", targetPosList); // リスト先頭ID候補を記録したリスト。
		mav.addObject("voiceList", voiceList);

		// Dummy parameterを設定。
		mav = createContentsList(mav, 1, params.getSortingOrder(), voiceList, errorMessageList);
		return mav;
	}

	@RequestMapping(value = "showVoiceList", method = { RequestMethod.POST, RequestMethod.GET })
	public ModelAndView actDataShowVoiceList(@ModelAttribute CData4Upload param, HttpSession session,
			ModelAndView mav) {
		CYsfFileSystem fs = CYsfFileSystem.getInstance();
		CYsfSdCHandlerProperties prop = CYsfSdCHandlerProperties.getInstance();
		LinkedList<String> errorMessageList = new LinkedList<String>();

		if (!fs.isSdCardMounted()) {
			errorMessageList.add("SD-CARDが抜かれたか、未だ挿入されたSD-CARDのMOUNT処理がされていません。");
			errorMessageList.add("MOUNTしなければ、「画像処理」プレーンでの画像処理ができません。");
			fs.clearAll();
		}

		LinkedList<CVoiceEntry> voiceList = new LinkedList<CVoiceEntry>();

		// 元データ”fs.getVoiceDirList()”はソートしたくないので、ここでリストを再作成する。
		if (fs.getVoiceDirList() != null)
			for (CVoiceEntry f : fs.getVoiceDirListWithDisplayOrder()) {
				voiceList.add(f);
			}

		setAllParameters4Mav(mav, errorMessageList, "", fs, "", prop, "pages/divVoices");
		mav = createContentsList(mav, param.getStartFrom(), param.getSortingOrder(), voiceList, errorMessageList);
		return mav;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected LinkedList<CVoiceEntry> createResultContentsListSeed() {
		return new LinkedList<CVoiceEntry>();
	}

}
