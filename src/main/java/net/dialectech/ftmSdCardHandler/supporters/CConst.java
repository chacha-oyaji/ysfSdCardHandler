package net.dialectech.ftmSdCardHandler.supporters;

import java.io.File;
import java.util.LinkedHashMap;

public class CConst {

	public static final int BYTESIZE_PER_DIR_ENTRY = 128; // ディレクトリエントリでの１情報あたりのバイト数
	public static final int BYTESIZE_PER_FAT = 4; // FATでの1情報あたりのバイト数
	public static final String defaultOffset4Debug = "DriveF" + File.separator;
	public static final String STR_BRANCH_NAME_HOLDER = "branchNameHolder.dat";
	public static final String STR_SDCARD_ID_HOLDER = "sdCardId.dat";
	
	public static final String STR_QSOPCTDIR = "QSOPCTDIR.dat";
	public static final String STR_QSOPCTFAT = "QSOPCTFAT.dat";
	public static final String STR_QSOMSGDIR = "QSOMSGDIR.dat";
	public static final String STR_QSOMSGFAT = "QSOMSGFAT.dat";
	public static final String STR_QSOMSG = "QSOMSG.dat";
	public static final String STR_QSOMNG = "QSOMNG.dat";

	public static final String STR_WAVMNG = "WAVMNG.dat";
	public static final String STR_QSOWAVFAT = "QSOWAVFAT.dat";
	public static final String STR_QSOWAVDIR = "QSOWAVDIR.dat";

	public static final String STR_DISP_Program_Name = "Yaesu System FusionⓇ SdCard Handler programmed by JI1BXM";

	public static final String STR_WORKING_DIRECTORY_PATH = System.getProperty("user.home") + File.separator + "AppData"
			+ File.separator + "Local" + File.separator + "Yaesu Musen" + File.separator + "ysfSDCHandler"
			+ File.separator;
	public static final String STR_FILE_WORKING_DIRECTORY_PATH = STR_WORKING_DIRECTORY_PATH + "WORKING" ;

			public static final String STR_BANK_DIRECTORY_PATH = STR_WORKING_DIRECTORY_PATH + "BANK" + File.separator;
	public static final String STR_PROPERTY_FILE_PATH = STR_WORKING_DIRECTORY_PATH + "ysfSDCHandler.xml";

	public static final String defaultBrowserPath = "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe";
	public static final String defaultImageMagickPath = System.getProperty("user.home") + File.separator + "Programs"
			+ File.separator + "ImageMagick-7.1.1-29-portable-Q16-x64" + File.separator; // ユーザーディレクトリの下。;

	public static final String defaultFirstPage = "/localhost:48649/execute/base";
	public static final String defaultSdCardDrive = "G";
	public static final String defaultTopLetterOfPhotoFile = "M";
	public static final String defaultRadioId = "XXXXX";
	public static final String defaultFillByteInPctDir = "20"; // HEXADECIMALで指定する。
	public static final String PhotoFoldername = "PHOTO";
	public static final String QsoLogFolderName = "QSOLOG";
	public static final String VoiceFoldername = "VOICE";
	public static final int MAX_MESSAGE_BYTES_PER_PACKET = 80 ;	// １メッセージあたりの最大バイト数は80(=0x50)
	
	public static LinkedHashMap<String, String> defaultPositionOfSuperImposeMap = new LinkedHashMap<String, String>();
	public static LinkedHashMap<String, String> defaultColorSelectionMap = new LinkedHashMap<String, String>();
	public static LinkedHashMap<String, String> defaultMaxSizeOfImage = new LinkedHashMap<String, String>();
	public static LinkedHashMap<String, String> defaultTopLetterOfPhotoFileMap = new LinkedHashMap<String, String>();
	public static LinkedHashMap<String, String> defaultPCDriveName = new LinkedHashMap<String, String>();
	public static LinkedHashMap<String, String> defaultFillByteInPctDirMap= new LinkedHashMap<String, String>();
	public static LinkedHashMap<String, String> defaultListStepSizeMap= new LinkedHashMap<String, String>();
	
	static {
		defaultPositionOfSuperImposeMap.put("0", "上左詰");
		defaultPositionOfSuperImposeMap.put("1", "上中央");
		defaultPositionOfSuperImposeMap.put("2", "上右詰");
		// defaultMaxSizeOfImageMap.put("4", "中左詰") ;
		// defaultMaxSizeOfImageMap.put("5", "中中央") ;
		// defaultMaxSizeOfImageMap.put("6", "中左詰") ;
		defaultPositionOfSuperImposeMap.put("8", "下左詰");
		defaultPositionOfSuperImposeMap.put("9", "下中央");
		defaultPositionOfSuperImposeMap.put("10", "下右詰");

		defaultColorSelectionMap.put("#fff", "白");
		defaultColorSelectionMap.put("#888", "灰");
		defaultColorSelectionMap.put("#000", "黒");
		defaultColorSelectionMap.put("#f00", "赤");
		defaultColorSelectionMap.put("#0f0", "緑");
		defaultColorSelectionMap.put("#080", "深緑");
		defaultColorSelectionMap.put("#00f", "青");
		defaultColorSelectionMap.put("#f0f", "紫");
		defaultColorSelectionMap.put("#0ff", "シアン");
		defaultColorSelectionMap.put("#C60", "濃オレンジ");
		defaultColorSelectionMap.put("#F80", "オレンジ");
		defaultColorSelectionMap.put("#ff0", "黄");

		defaultMaxSizeOfImage.put("2", "2k Byte");
		defaultMaxSizeOfImage.put("3", "3k Byte");
		defaultMaxSizeOfImage.put("4", "4k Byte");
		defaultMaxSizeOfImage.put("5", "5k Byte");
		defaultMaxSizeOfImage.put("6", "6k Byte");
		defaultMaxSizeOfImage.put("7", "7k Byte");
		defaultMaxSizeOfImage.put("8", "8k Byte");
		defaultMaxSizeOfImage.put("10", "10k Byte");
		defaultMaxSizeOfImage.put("12", "12k Byte");
		defaultMaxSizeOfImage.put("16", "16k Byte");
		defaultMaxSizeOfImage.put("20", "20k Byte");
		defaultMaxSizeOfImage.put("24", "24k Byte");
		defaultMaxSizeOfImage.put("28", "28k Byte");
		defaultMaxSizeOfImage.put("32", "32k Byte");
		defaultMaxSizeOfImage.put("48", "48k Byte");
		defaultMaxSizeOfImage.put("64", "64k Byte");
		defaultMaxSizeOfImage.put("96", "96k Byte");
		defaultMaxSizeOfImage.put("128", "128k Byte");

		defaultPCDriveName.put("D", "D");
		defaultPCDriveName.put("E", "E");
		defaultPCDriveName.put("F", "F");
		defaultPCDriveName.put("G", "G");
		defaultPCDriveName.put("H", "H");
		defaultPCDriveName.put("I", "I");
		defaultPCDriveName.put("J", "J");
		defaultPCDriveName.put("K", "K");
		defaultPCDriveName.put("L", "L");
		defaultPCDriveName.put("M", "M");
		defaultPCDriveName.put("N", "N");
		defaultPCDriveName.put("O", "O");
		defaultPCDriveName.put("P", "P");
		defaultPCDriveName.put("Q", "Q");
		defaultPCDriveName.put("R", "R");
		defaultPCDriveName.put("S", "S");
		defaultPCDriveName.put("T", "T");
		defaultPCDriveName.put("U", "U");
		defaultPCDriveName.put("V", "V");
		defaultPCDriveName.put("W", "W");
		defaultPCDriveName.put("X", "X");
		defaultPCDriveName.put("Y", "Y");
		defaultPCDriveName.put("Z", "Z");

		defaultTopLetterOfPhotoFileMap.put("H", "H : Handy Gear");
		defaultTopLetterOfPhotoFileMap.put("M", "M : Mobile Gear");
		
		defaultFillByteInPctDirMap.put("20", "20　(FT-5D等)") ;
		defaultFillByteInPctDirMap.put("FF", "FF　(FTM-300D等)") ;
		
		defaultListStepSizeMap.put("5", "5 行/頁") ;
		defaultListStepSizeMap.put("10", "10 行/頁") ;
		defaultListStepSizeMap.put("15", "15 行/頁") ;
		defaultListStepSizeMap.put("20", "20 行/頁") ;
		defaultListStepSizeMap.put("25", "25 行/頁") ;
		defaultListStepSizeMap.put("50", "50 行/頁") ;
		defaultListStepSizeMap.put("100", "100 行/頁") ;
		defaultListStepSizeMap.put("250", "250 行/頁") ;
	}


}
