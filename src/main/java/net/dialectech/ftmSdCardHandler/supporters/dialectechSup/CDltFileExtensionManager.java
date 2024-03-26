package net.dialectech.ftmSdCardHandler.supporters.dialectechSup;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CDltFileExtensionManager {

	private static CDltFileExtensionManager instance = new CDltFileExtensionManager();
	private static LinkedHashMap<String,String> contentTypeMap ;
	
	private CDltFileExtensionManager() {
		contentTypeMap = new LinkedHashMap<String,String>() ;
		contentTypeMap.put("doc", "application/msword");
		contentTypeMap.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
		contentTypeMap.put("dot", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
		contentTypeMap.put("dotx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
		contentTypeMap.put("docm", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
		
		contentTypeMap.put("xls", "application/vnd.ms-excel");
		contentTypeMap.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		contentTypeMap.put("ppt", "application/vnd.ms-powerpoint");
		contentTypeMap.put("pptx", "application/vnd.ms-powerpoint");
		contentTypeMap.put("pdf", "application/pdf");
		contentTypeMap.put("xml", "text/xml");
		contentTypeMap.put("jpg", "image/jpeg");
		contentTypeMap.put("jpeg", "image/jpeg");
		contentTypeMap.put("tif", "image/tif");
		contentTypeMap.put("gif", "image/gif");
		contentTypeMap.put("png", "image/png");
		contentTypeMap.put("bmp", "image/bmp");
		contentTypeMap.put("odg", "application/vnd.oasis.opendocument.graphics");
		contentTypeMap.put("odt", "application/vnd.oasis.opendocument.text");
		contentTypeMap.put("odp", "application/vnd.oasis.opendocument.presentation");
		contentTypeMap.put("mp4", "video/mp4");
		contentTypeMap.put("mpe", "video/mpeg");
		contentTypeMap.put("mpeg", "video/mpeg");
		contentTypeMap.put("avi", "video/x-msvideo");
		contentTypeMap.put("zip", "application/x-zip-compressed");
	}
	
	public static CDltFileExtensionManager getInstance() {
		return instance ;
	}
	/**
	 * このクラスのオブジェクトが管理するファイルのコンテンツタイプを返す。 なお、返すコンテンツタイプは、 application/msword
	 * application/vnd.ms-excel
	 * application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
	 * application/pdf text/xml image/jpeg image/gif image/png text/csv まで。
	 * 
	 * @return
	 */
	public synchronized String getContentTypeOf(String nameToCheck) {
		String res = contentTypeMap.get(nameToCheck.toLowerCase()) ;
		return res ;
	}
//	public static synchronized String getContentTypeOf(String nameToCheck) {
//		FileTypeMap filetypeMap = FileTypeMap.getDefaultFileTypeMap()  ;
//		FileTypeMap.setDefaultFileTypeMap(filetypeMap);
//		((MimetypesFileTypeMap) filetypeMap).addMimeTypes("application/msword doc DOC") ;
//		((MimetypesFileTypeMap) filetypeMap)
//			.addMimeTypes("application/vnd.openxmlformats-officedocument.wordprocessingml.document docx DOCX dot dotx DOCM docm");
//		((MimetypesFileTypeMap) filetypeMap).addMimeTypes("application/vnd.ms-excel xls XLS");
//		((MimetypesFileTypeMap) filetypeMap)
//			.addMimeTypes("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet xlsx XLSX");
//		((MimetypesFileTypeMap) filetypeMap).addMimeTypes("application/vnd.ms-powerpoint ppt pptx");
//		((MimetypesFileTypeMap) filetypeMap).addMimeTypes("application/pdf pdf PDF");
//		((MimetypesFileTypeMap) filetypeMap).addMimeTypes("text/xml xml XML");
//		((MimetypesFileTypeMap) filetypeMap).addMimeTypes("image/jpeg jpg JPG jpeg JPEG");
//		((MimetypesFileTypeMap) filetypeMap).addMimeTypes("image/tiff tif TIF");
//		((MimetypesFileTypeMap) filetypeMap).addMimeTypes("image/gif gif GIF");
//		((MimetypesFileTypeMap) filetypeMap).addMimeTypes("image/png png PNG");
//		((MimetypesFileTypeMap) filetypeMap).addMimeTypes("image/bmp bmp BMP");
//		((MimetypesFileTypeMap) filetypeMap).addMimeTypes("application/vnd.oasis.opendocument.graphics odg");
//		((MimetypesFileTypeMap) filetypeMap).addMimeTypes("application/vnd.oasis.opendocument.text odt");
//		((MimetypesFileTypeMap) filetypeMap).addMimeTypes("application/vnd.oasis.opendocument.presentation odp");
//		((MimetypesFileTypeMap) filetypeMap).addMimeTypes("video/mp4 mp4");
//		((MimetypesFileTypeMap) filetypeMap).addMimeTypes("video/mpeg mpeg mpe");
//		((MimetypesFileTypeMap) filetypeMap).addMimeTypes("video/x-msvideo avi AVI");
//		((MimetypesFileTypeMap) filetypeMap).addMimeTypes("application/x-zip-compressed zip ZIP");
//
//		String mimetype = filetypeMap.getContentType(nameToCheck);
//
//		return mimetype;
//	}

	/**
	 * 主として、JSPの中から呼び出すので、名称は変則的・・。気持ち悪いがこうする。
	 * 
	 * @param thisFileName
	 * @return
	 */
	/**
	 * @param thisFileName
	 * @return
	 */
	public static synchronized boolean getIsImageByDirectedFileName(String thisFileName) {
		int extensionIndex = 0;
		if (thisFileName != null) {
			extensionIndex = thisFileName.lastIndexOf(".");
		}
		if (extensionIndex >= 0) {
			String extension = thisFileName.substring(extensionIndex);
			if (extension.equalsIgnoreCase(".jpg"))
				return true;
			if (extension.equalsIgnoreCase(".gif"))
				return true;
			if (extension.equalsIgnoreCase(".png"))
				return true;
			if (extension.equalsIgnoreCase(".jpeg"))
				return true;
			if (extension.equalsIgnoreCase(".bmp"))
				return true;
			// if (extension.equalsIgnoreCase(".tif"))
			// return true;
		}
		return false;
	}

	public static synchronized boolean getIsPdfByDirectedFileName(String targetFileName) {
		if (targetFileName == null || targetFileName.equals(""))
			return false;
		if (targetFileName.toLowerCase().matches(".+\\.pdf$"))
			return true;
		return false;
	}

	public static synchronized String extractFileNameExtension(String targetFileName) {
		if (targetFileName == null || targetFileName.equals(""))
			return "";
		String[] fileNameParts = targetFileName.split("\\.");
		if (fileNameParts == null || fileNameParts.length == 0)
			return "";
		return fileNameParts[fileNameParts.length - 1];
	}

	/**
	 * templatePathに指定されたファイル名のExtensionから、そのファイルの種類をEnumDocTypeから選んで返す。
	 * 含まれない場合、指定がない場合には、EnumDocType.OTHERSを返す。
	 * 
	 * @param templatePath
	 * @return
	 */
	public static synchronized EnumDocType path2EnumDocType(Path templatePath) {
		if (templatePath==null) {
			return EnumDocType.OTHERS ;
		}
		EnumDocType docType;
		Pattern pattern = Pattern.compile(".+\\.([^\\.]*)$"); // 拡張子部分取得
		Matcher matcher = pattern.matcher(templatePath.getFileName().toString());
		String fileExtension = "";
		if (matcher.find()) {
			fileExtension = matcher.group(1).toUpperCase();
		}
		try {
			docType = EnumDocType.valueOf(fileExtension); // 予め登録されている拡張子ならこちらが適用される。
		} catch (Exception e) {
			docType = EnumDocType.OTHERS;
		}
		return docType;
	}


}
