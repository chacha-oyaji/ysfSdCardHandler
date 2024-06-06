package net.dialectech.ftmSdCardHandler.actions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpSession;
import net.dialectech.ftmSdCardHandler.actions.suppoters.CHandlerActionFundamental;
import net.dialectech.ftmSdCardHandler.data.CData4Upload;
import net.dialectech.ftmSdCardHandler.data.CMessageEntry;
import net.dialectech.ftmSdCardHandler.supporters.CConst;
import net.dialectech.ftmSdCardHandler.supporters.CYsfSdCHandlerProperties;
import net.dialectech.ftmSdCardHandler.supporters.fileSystem.CYsfFileSystem;

@Controller
@RequestScope
@RequestMapping("/execute/messages")
public class CHandlerMessagesAction extends CHandlerActionFundamental {

	/**
	 * actDataMountAndLoadPresentMessages()は"mountAndLoadPresentMessages"リクエストによってキックされるが、
	 * actDataMountAndLoadPresentVoicesと同様、mountAndLoadPresentImagesリクエストによってキックされた
	 * 上で呼ばれることを前提とする。
	 * 
	 * * @param params
	 * 
	 * @param session
	 * @param mav
	 * @return
	 */
	@RequestMapping(value = "mountAndLoadPresentMessages", method = { RequestMethod.POST, RequestMethod.GET })
	public ModelAndView actDataMountAndLoadPresentMessages(@ModelAttribute CData4Upload params, HttpSession session,
			ModelAndView mav) {
		CYsfSdCHandlerProperties prop = CYsfSdCHandlerProperties.getInstance();
		CYsfFileSystem fs = CYsfFileSystem.getInstance();
		String sdCardBaseDirectory = prop.getSdCardBaseDirName();
		// String presentUsingBranchName = getPresentUsingBranchName(cardDrive);

		// System.out.println(">>>" + presentUsingBranchName);
		LinkedList<String> errorMessageList = new LinkedList<String>();

		if (!(new File(sdCardBaseDirectory + CConst.QsoLogFolderName + File.separator)).exists()) {
			errorMessageList.add("対応するSD-CARDが装着されていません。");
			setAllParameters4Mav(mav, errorMessageList, "", fs, "ERROR", prop, "pages/divImages");
			fs.clearAll();
			return mav;
		}

		String errorMsg = null;

		int mountWaitCounter = 0;
		while (!fs.isMounted()) {
			// 100mS毎に状態をチェック。
			try {
				Thread.sleep(100);
				mountWaitCounter++;
				// System.out.print("*");
			} catch (InterruptedException e) {
				// 何もしなくてもよい。
			}
			// 10秒越えてもmountが終了していないときには、あらためてmount処理をしてチェックを終了する。
			if (mountWaitCounter > 100) {
				errorMsg = fs.loadFromSDCard();
				break;
			}
		}

		if (errorMsg != null) {
			errorMessageList.add(errorMsg);
			errorMessageList.add("SD-CARDの構成が不完全です。");
			setAllParameters4Mav(mav, errorMessageList, "", fs, "ERROR", prop, "pages/divMessages");
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
		mav = createContentsList(mav, params.getStartFrom(), params.getSortingOrder(), fs.getMsgDirList(),
				errorMessageList);
		setAllParameters4Mav(mav, errorMessageList, "", fs, "", prop, "pages/divMessages");
		return mav;
	}

	@RequestMapping(value = "showMessageList", method = { RequestMethod.POST, RequestMethod.GET })
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

		LinkedList<CMessageEntry> messageList = new LinkedList<CMessageEntry>();

		// 元データ”fs.getVoiceDirList()”はソートしたくないので、ここでリストを再作成する。
		if (fs.getMsgDirList() != null)
			for (CMessageEntry f : fs.getMsgDirList()) {
				messageList.add(f);
			}

		setAllParameters4Mav(mav, errorMessageList, "", fs, "", prop, "pages/divMessages");
		mav = createContentsList(mav, param.getStartFrom(), param.getSortingOrder(), messageList, errorMessageList);

		return mav;
	}

	@RequestMapping(value = "deleteMessageCompletely", method = { RequestMethod.POST, RequestMethod.GET })
	public ModelAndView actDataDeleteMessageCompletely(@ModelAttribute CData4Upload params, HttpSession session,
			ModelAndView mav) {
		CYsfSdCHandlerProperties prop = CYsfSdCHandlerProperties.getInstance();
		CYsfFileSystem fs = CYsfFileSystem.getInstance();
		LinkedList<String> errorMessageList = new LinkedList<String>();

		if (!fs.isSdCardMounted()) {
			errorMessageList.add("SD-CARDが抜かれたか、未だ挿入されたSD-CARDのMOUNT処理がされていません。");
			errorMessageList.add("MOUNTしなければ、「画像処理」プレーンでの画像処理ができません。");
			fs.clearAll();
		}

		for (CMessageEntry messageData : fs.getMsgDirList()) {
			if (params.getTargetDataId() == messageData.getDataId()) {
				fs.deleteMessageEntry(messageData);
				break;
			}
		}

		fs.saveAllOfFilesOnMessage(errorMessageList);
		setAllParameters4Mav(mav, errorMessageList, "", fs, "", prop, "pages/divMessages");
		mav = createContentsList(mav, params.getStartFrom(), params.getSortingOrder(), fs.getMsgDirList(),
				errorMessageList);

		return mav;
	}

	@RequestMapping(value = "deleteMessageMarking", method = { RequestMethod.POST, RequestMethod.GET })
	public ModelAndView actDataDeleteMessageMarking(@ModelAttribute CData4Upload params, HttpSession session,
			ModelAndView mav) {
		CYsfSdCHandlerProperties prop = CYsfSdCHandlerProperties.getInstance();
		CYsfFileSystem fs = CYsfFileSystem.getInstance();
		LinkedList<String> errorMessageList = new LinkedList<String>();

		if (!fs.isSdCardMounted()) {
			errorMessageList.add("SD-CARDが抜かれたか、未だ挿入されたSD-CARDのMOUNT処理がされていません。");
			errorMessageList.add("MOUNTしなければ、「画像処理」プレーンでの画像処理ができません。");
			fs.clearAll();
		}

		for (CMessageEntry messageData : fs.getMsgDirList()) {
			if (params.getTargetDataId() == messageData.getDataId()) {
				messageData.setActive(false);
			}
		}

		fs.saveAllOfFilesOnMessage(errorMessageList);

		mav = createContentsList(mav, params.getStartFrom(), params.getSortingOrder(), fs.getMsgDirList(),
				errorMessageList);
		setAllParameters4Mav(mav, errorMessageList, "", fs, "", prop, "pages/divMessages");

		return mav;
	}

	@RequestMapping(value = "recoverMessageMarking", method = { RequestMethod.POST, RequestMethod.GET })
	public ModelAndView actDataRecoverMessageMarking(@ModelAttribute CData4Upload params, HttpSession session,
			ModelAndView mav) {
		CYsfSdCHandlerProperties prop = CYsfSdCHandlerProperties.getInstance();
		CYsfFileSystem fs = CYsfFileSystem.getInstance();
		LinkedList<String> errorMessageList = new LinkedList<String>();

		if (!fs.isSdCardMounted()) {
			errorMessageList.add("SD-CARDが抜かれたか、未だ挿入されたSD-CARDのMOUNT処理がされていません。");
			errorMessageList.add("MOUNTしなければ、「画像処理」プレーンでの画像処理ができません。");
			fs.clearAll();
		}

		for (CMessageEntry messageData : fs.getMsgDirList()) {
			if (params.getTargetDataId() == messageData.getDataId()) {
				messageData.setActive(true);
			}
		}

		fs.saveAllOfFilesOnMessage(errorMessageList);

		mav = createContentsList(mav, params.getStartFrom(), params.getSortingOrder(), fs.getMsgDirList(),
				errorMessageList);
		setAllParameters4Mav(mav, errorMessageList, "", fs, "", prop, "pages/divMessages");

		return mav;
	}

	@RequestMapping(value = "createMessage2Send", method = { RequestMethod.POST, RequestMethod.GET })
	public ModelAndView actDataCreateMessage2Send(@ModelAttribute CData4Upload params, HttpSession session,
			ModelAndView mav) {
		CYsfSdCHandlerProperties prop = CYsfSdCHandlerProperties.getInstance();
		CYsfFileSystem fs = CYsfFileSystem.getInstance();
		LinkedList<String> errorMessageList = new LinkedList<String>();

		if (!fs.isSdCardMounted()) {
			errorMessageList.add("SD-CARDが抜かれたか、未だ挿入されたSD-CARDのMOUNT処理がされていません。");
			errorMessageList.add("MOUNTしなければ、「画像処理」プレーンでの画像処理ができません。");
			fs.clearAll();
		}

		boolean foundError = false;
		if (params.getDestination().isBlank()) {
			errorMessageList.add("リスト上への表示欄が空白です。１０文字以内の文字列が必要です。");
			foundError = true;
		}
		if (params.getMessage().isBlank()) {
			errorMessageList.add("メッセージが空白です。５０文字以内の文字列が必要です。");
			foundError = true;
		}
		if (foundError) {
			errorMessageList.add("上記のエラーのため、メッセージの書き込みをしませんでした。");
			setAllParameters4Mav(mav, errorMessageList, "", fs, "", prop, "pages/divMessages");
			mav = createContentsList(mav, params.getStartFrom(), params.getSortingOrder(), fs.getMsgDirList(),
					errorMessageList);
			return mav;
		}
		int newId = fs.getMsgDirList().size() + 1;
		CMessageEntry newMessage = new CMessageEntry(prop.getRadioId(), params.getDestination(), prop.getMyCallSign(),
				prop.getRadioId(), params.getMessage(), newId);

		fs.addMessageEntry(newMessage);
		fs.saveAllOfFilesOnMessage(errorMessageList);

		mav = createContentsList(mav, params.getStartFrom(), params.getSortingOrder(), fs.getMsgDirList(),
				errorMessageList);
		setAllParameters4Mav(mav, errorMessageList, "", fs, "", prop, "pages/divMessages");

		return mav;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected LinkedList<CMessageEntry> createResultContentsListSeed() {
		return new LinkedList<CMessageEntry>();
	}

}
