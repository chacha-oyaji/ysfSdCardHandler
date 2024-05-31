package net.dialectech.ftmSdCardHandler.supporters;

import java.io.ByteArrayOutputStream;
import java.util.LinkedList;

import org.springframework.web.servlet.ModelAndView;

import lombok.Getter;
import lombok.Setter;
import net.dialectech.ftmSdCardHandler.supporters.dialectechSup.CDltFileUtilities;
import net.dialectech.ftmSdCardHandler.supporters.dialectechSup.CDltFlowsException;
import net.dialectech.ftmSdCardHandler.supporters.dialectechSup.CDltImageIO;
import net.dialectech.ftmSdCardHandler.supporters.dialectechSup.CDltSpringFileStream;
import net.dialectech.ftmSdCardHandler.supporters.fileSystem.CYsfFileSystemCorePart;

public class CHandlerActionFundamental {

	@Getter
	@Setter
	protected CDltSpringFileStream springFileStream;
	
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
	protected void setAllParameters4Mav(ModelAndView mav, LinkedList<String> errorMessageList, String branchName,
			CYsfFileSystemCorePart fs, String pageTitle, CYsfSdCHandlerProperties prop, String viewName) {
		mav.addObject("errorMessageList", errorMessageList);
		mav.addObject("branchDirName", branchName);
		mav.addObject("fs", fs);
		mav.addObject("title", pageTitle);
		mav.addObject("prop", prop);
		mav.setViewName(viewName);
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

}
