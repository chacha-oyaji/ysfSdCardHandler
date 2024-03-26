package net.dialectech.ftmSdCardHandler.supporters;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Properties;

import lombok.Getter;
import lombok.Setter;

public class CYsfSDCHandlerPropertiesBaseData extends Properties {
	private static final long serialVersionUID = -1313329941131200513L;

	@Getter
	@Setter
	private String browserPath = CConst.defaultBrowserPath;
	@Getter
	@Setter
	private String imageMagickPath = CConst.defaultImageMagickPath; // ユーザーディレクトリの下。;
	@Getter
	@Setter
	private String firstPage = CConst.defaultFirstPage;
	@Getter
	@Setter
	private String sdCardDrive = CConst.defaultSdCardDrive;
	@Getter
	@Setter
	private String strBankDirectoryPath = CConst.STR_BANK_DIRECTORY_PATH;
	@Getter
	@Setter
	private String strFileWorkingDirectoryPath = CConst.STR_FILE_WORKING_DIRECTORY_PATH;

	@Getter
	@Setter
	private String strOffset4Debug = CConst.defaultOffset4Debug;

	@Getter
	private String strBranchNameHolder = CConst.STR_BRANCH_NAME_HOLDER;

	@Getter
	private String strSdCardIdHolder = CConst.STR_SDCARD_ID_HOLDER;
	@Getter
	@Setter
	private String topLetterOfPhotoFile = CConst.defaultTopLetterOfPhotoFile;
	@Getter
	@Setter
	private String myCallSign = "CallSign";
	@Getter
	@Setter
	private int listStepSize = 20;
	@Getter
	@Setter
	private String maxSizeOfImage = "0";
	@Getter
	@Setter
	private String radioId = CConst.defaultRadioId;
	@Getter
	public LinkedHashMap<String, String> positionOfSuperImposeMap;
	@Getter
	public LinkedHashMap<String, String> colorSelectionMap;
	@Getter
	public LinkedHashMap<String, String> maxSizeOfImageMap;
	@Getter
	public LinkedHashMap<String, String> pcDriveNameMap;
	@Getter
	public LinkedHashMap<String, String> topLetterOfPhotoFileMap;
	@Getter
	public LinkedHashMap<String, String> fillByteInPctDirMap;
	@Getter
	public LinkedHashMap<String, String> listStepSizeMap;

	@Getter
	private boolean debugMode = false;
	@Getter
	public Byte fillByteInPctDir;

	public CYsfSDCHandlerPropertiesBaseData() {
		super();
		positionOfSuperImposeMap = CConst.defaultPositionOfSuperImposeMap;
		colorSelectionMap = CConst.defaultColorSelectionMap;
		maxSizeOfImageMap = CConst.defaultMaxSizeOfImage;
		pcDriveNameMap = CConst.defaultPCDriveName;
		topLetterOfPhotoFileMap = CConst.defaultTopLetterOfPhotoFileMap;
		fillByteInPctDirMap = CConst.defaultFillByteInPctDirMap;
		listStepSizeMap = CConst.defaultListStepSizeMap;
		fillByteInPctDir = (byte) (Integer.parseInt(CConst.defaultFillByteInPctDir, 16) & 0xff);
	}

	public boolean canBeLoad() {
		File propertyFile = new File(CConst.STR_PROPERTY_FILE_PATH);
		if (propertyFile.canRead())
			return true;
		return false;
	}

	public String getPresentFolderInfoFileName() {
		// TODO 自動生成されたメソッド・スタブ
		return getStrBranchNameHolderPath();
	}

	public String getPropertyFileName() {
		return CConst.STR_PROPERTY_FILE_PATH;
	}

	public String getRootDir4Bank() {
		return strBankDirectoryPath;
	}

	public String getSdCardBaseDirName() {
		if (debugMode)
			return sdCardDrive + ":" + File.separator + strOffset4Debug;
		return sdCardDrive + ":" + File.separator;
	}

	private String getStrBranchNameHolderPath() {
		return getStrQsoLogDirectoryPath() + CConst.STR_BRANCH_NAME_HOLDER;
	}

	// public final static String DirOffset4Debug

	// public final static String DirOffset4Debug = "";

	public String getStrFillByteInPctDir() {
		try {
			String res = Integer.toHexString(fillByteInPctDir & 0xff).toUpperCase();
			return res;
		} catch (NumberFormatException e) {
			return "20";
		}
	}

	public String getStrPhotoDirectoryPath() {
		String res;
		if (debugMode) {
			res = getSdCardDrive() + ":" + File.separator + getStrOffset4Debug() + "PHOTO" + File.separator;
		} else {
			res = getSdCardDrive() + ":" + File.separator + "PHOTO" + File.separator;
		}
		return res;
	}

	public String getStrQsoLogDirectoryPath() {
		String res;
		if (debugMode) {
			res = getSdCardDrive() + ":" + File.separator + getStrOffset4Debug() + "QSOLOG" + File.separator;
		} else {
			res = getSdCardDrive() + ":" + File.separator + "QSOLOG" + File.separator;
		}
		return res;
	}

	public String getStrQsoMngFilePath() {
		return getStrQsoLogDirectoryPath() + CConst.STR_QSOMNG;
	}

	public String getStrQsoPctDirFilePath() {
		return getStrQsoLogDirectoryPath() + CConst.STR_QSOPCTDIR;
	}

	public String getStrQsoPctFatFilePath() {
		return getStrQsoLogDirectoryPath() + CConst.STR_QSOPCTFAT;
	}

	public String getStrSdCardIdHolderPath() {
		return getStrQsoLogDirectoryPath() + CConst.STR_SDCARD_ID_HOLDER;
	}

	public void loadAll() throws IOException {
		Path propertyFilePath = Paths.get(CConst.STR_PROPERTY_FILE_PATH);

		FileInputStream inputStream = new FileInputStream(propertyFilePath.toFile());
		this.loadFromXML(inputStream);

		browserPath = getProperty("browserPath", CConst.defaultBrowserPath);
		imageMagickPath = getProperty("imageMagickPath", CConst.defaultImageMagickPath);
		sdCardDrive = getProperty("sdCardDrive", CConst.defaultSdCardDrive);
		debugMode = Boolean.valueOf(getProperty("debugMode", String.valueOf(false)));
		firstPage = this.getProperty("firstPage", CConst.defaultFirstPage);
		// strBankDirectoryPath = this.getProperty("bankDirName",
		// CConst.STR_BANK_DIRECTORY_PATH);
		strOffset4Debug = this.getProperty("offset4Debug", CConst.defaultOffset4Debug);
		// strBranchNameHolderPath = this.getProperty("branchNameHolder",
		// CConst.STR_BRANCH_NAME_HOLDER);// これは固定的なので、設定できないことにする。
		topLetterOfPhotoFile = this.getProperty("topLetterOfPhotoFile", CConst.defaultTopLetterOfPhotoFile);
		radioId = this.getProperty("radioId", CConst.defaultRadioId);
		myCallSign = this.getProperty("myCallSign");
		maxSizeOfImage = this.getProperty("maxSizeOfImage", "8k");
		setStrFillByteInPctDir(this.getProperty("fillByteInPctDir", "FF"));
		listStepSize = Integer.parseInt(this.getProperty("listStepSize", "20"));
	}

	public void saveAll() throws IOException {
		this.clear();
		this.setProperty("browserPath", browserPath);
		this.setProperty("imageMagickPath", imageMagickPath);
		this.setProperty("sdCardDrive", sdCardDrive);
		if (debugMode) {
			this.setProperty("debugMode", String.valueOf(debugMode));
			this.setProperty("offset4Debug", strOffset4Debug);
		}
		this.setProperty("firstPage", firstPage);
		// this.setProperty("bankDirName", strBankDirectoryPath);
		// this.setProperty("branchNameHolder", getStrBranchNameHolderPath()); //
		// 所詮固定値なので、saveしない。
		this.setProperty("topLetterOfPhotoFile", topLetterOfPhotoFile);
		this.setProperty("radioId", radioId);
		this.setProperty("myCallSign", myCallSign);
		this.setProperty("imageMagickPath", imageMagickPath);
		this.setProperty("browserPath", browserPath);
		this.setProperty("maxSizeOfImage", maxSizeOfImage);
		this.setProperty("fillByteInPctDir", getStrFillByteInPctDir());
		this.setProperty("listStepSize", String.valueOf(listStepSize));

		Path propertyFilePath = Paths.get(CConst.STR_PROPERTY_FILE_PATH);
		Path propertyFileDirectoryPath = Paths.get(CConst.STR_WORKING_DIRECTORY_PATH);
		if (!Files.exists(propertyFilePath)) {
			// 所定のディレクトリすらないのであれば、ここに生成する。
			Files.createDirectories(propertyFileDirectoryPath);
		}
		FileOutputStream outputStream = new FileOutputStream(propertyFilePath.toFile());
		this.storeToXML(outputStream, CConst.STR_DISP_Program_Name);
	}

	public void setDebugMode(boolean modeSpec) {
		this.setProperty("debugMode", String.valueOf(modeSpec));
	}

	public void setDebugMode(String modeSpec) {
		if (modeSpec != null) {
			debugMode = Boolean.parseBoolean(modeSpec);
			this.setProperty("debugMode", modeSpec.trim());
		}
	}

	public void setStrFillByteInPctDir(String source) {
		try {
			fillByteInPctDir = (byte) (Integer.parseInt(source, 16) & 0xff);
		} catch (NumberFormatException e) {
			fillByteInPctDir = (byte) 0x20;
		}
	}

	public String strStrBranchNameHolderPath() {
		return getStrQsoLogDirectoryPath() + CConst.STR_BRANCH_NAME_HOLDER;
	}

}
