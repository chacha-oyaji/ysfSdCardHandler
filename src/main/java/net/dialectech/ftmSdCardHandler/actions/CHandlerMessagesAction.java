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
import net.dialectech.ftmSdCardHandler.data.CData4Upload;
import net.dialectech.ftmSdCardHandler.supporters.CConst;
import net.dialectech.ftmSdCardHandler.supporters.CHandlerActionFundamental;
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
		setAllParameters4Mav(mav, errorMessageList, "", fs, "", prop,
				"pages/divMessages");
		return mav;
	}

	
}
