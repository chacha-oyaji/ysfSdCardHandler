package net.dialectech.ftmSdCardHandler.data;

import java.util.LinkedList;

import lombok.Getter;
import lombok.Setter;
import net.dialectech.ftmSdCardHandler.supporters.dialectechSup.CDltSimpleFileHandler;

public class CData4Upload extends CDltSimpleFileHandler {

	@Setter
	@Getter
	private String radioId;
	@Setter
	@Getter
	private String sdCardDrive;
	@Setter
	@Getter
	private String rigModelName;
	@Setter
	@Getter
	private String fileName2Dowload;
	@Setter
	@Getter
	private String branchDirName;
	@Setter
	@Getter
	private String newBranchDirName;
	@Setter
	@Getter
	private String sortingOrder;
	@Setter
	@Getter
	private String myCallSign;
	@Setter
	@Getter
	private String volumeTarget;
	@Setter
	@Getter
	private String topLetterOfPhotoFile;
	@Setter
	@Getter
	private String specifiedStn2Send;
	@Setter
	@Getter
	private String specifiedColor;
	@Setter
	@Getter
	private int targetDataId;
	@Setter
	@Getter
	private String description2Change ;
	@Setter
	@Getter
	private LinkedList<String> errorMessageMap = new LinkedList<String>();
	@Setter
	@Getter
	private int startPointOfList; // 画像リスト中、読み始めとなる位置の指示値（client->Server）
	@Setter
	@Getter
	private int posOfSuperImpose; // 画像リスト中、読み始めとなる位置の指示値（client->Server）
	@Setter
	@Getter
	private String imageMagickPath;
	@Setter
	@Getter
	private String browserPath;
	@Getter
	@Setter
	private String offset4Debug;
	@Getter
	@Setter
	private String strFillByteInPctDir;
	@Getter
	@Setter
	private String strQRCode;
	@Getter
	@Setter
	private int imageSize;
	@Getter
	@Setter
	private String errorCorrectionLevel;

	// 以下はshowBankListリクエストにおいて使用される。
	@Getter
	@Setter
	private int startFrom;
	@Getter
	@Setter
	private int presentPos;
	@Getter
	@Setter
	private LinkedList<Integer> targetPosList;
	@Getter
	@Setter
	private int listStepSize;

}
