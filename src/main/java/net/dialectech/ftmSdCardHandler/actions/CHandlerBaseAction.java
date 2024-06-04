package net.dialectech.ftmSdCardHandler.actions;

import java.io.IOException;
import java.util.LinkedList;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.servlet.ModelAndView;
import jakarta.servlet.http.HttpSession;
import net.dialectech.ftmSdCardHandler.actions.suppoters.CHandlerActionFundamental;
import net.dialectech.ftmSdCardHandler.data.CData4Upload;
import net.dialectech.ftmSdCardHandler.data.supporters.CDataEntry;
import net.dialectech.ftmSdCardHandler.supporters.CYsfSdCHandlerProperties;
import net.dialectech.ftmSdCardHandler.supporters.fileSystem.CYsfFileSystem;

@Controller
@RequestScope
@RequestMapping("/execute")
public class CHandlerBaseAction extends CHandlerActionFundamental {

	@RequestMapping(value = "base", method = { RequestMethod.POST, RequestMethod.GET })
	public ModelAndView actDataBase(@ModelAttribute CData4Upload param, HttpSession session, ModelAndView mav) {
		CYsfSdCHandlerProperties prop = CYsfSdCHandlerProperties.getInstance();
		CYsfFileSystem fs  = CYsfFileSystem.getInstance();
		fs.setMounted(false);
		mav.addObject("prop", prop);
		mav.setViewName("pages/base");
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
		prop.setMaxSizeOfImage(String.valueOf(param.getImageSize()));

		;
		if (param.getOffset4Debug() != null && !param.getOffset4Debug().equals("")) {
			prop.setStrOffset4Debug(param.getOffset4Debug());
		}
		try {
			prop.saveAllProperties();
		} catch (IOException e) {
			errorMessageList.add("プロパティファイルを書き込めませんでした。");
		}

		setAllParameters4Mav(mav, errorMessageList, "", fs, "", prop, "pages/divResOfSettlement");
		return mav;
	}

	@Override
	protected LinkedList<CDataEntry> createResultContentsListSeed() {
		return new LinkedList<CDataEntry>() ;
	}

}
