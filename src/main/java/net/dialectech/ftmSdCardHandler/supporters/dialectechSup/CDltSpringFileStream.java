package net.dialectech.ftmSdCardHandler.supporters.dialectechSup;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import lombok.Getter;
import lombok.Setter;

/**
 * Struts2 中でファイルダウンロードをサポートする。
 * なお、内部で、thumbnail生成を行う部分はCUploadCore<>クラスでも定義があるので、プログラム を修正する際に注意すること。
 * 
 * @author ji1bxm
 *
 */

public class CDltSpringFileStream {
	protected static final int BUFFER_SIZE = 1 * 1024 * 1024;
	@Getter
	@Setter
	private InputStream inputStream;
	@Getter
	@Setter
	private String contentDisposition;
	@Getter
	@Setter
	private String contentType;
	@Getter
	@Setter
	private long contentLength;

	private boolean inLine = false;
	private String outputFileName;

	public CDltSpringFileStream(byte[] bs, String filename, String contentType) {
		this.inputStream = new ByteArrayInputStream(bs);
		this.contentLength = bs.length;
		this.outputFileName = filename;
		this.contentType = contentType;
	}

	public CDltSpringFileStream(String wholeString, String filename, String contentType) {
		inputStream = new ByteArrayInputStream(wholeString.getBytes());
		contentLength = wholeString.length();
		this.outputFileName = filename;
		this.contentType = contentType;
	}

	public CDltSpringFileStream(byte[] bs, String filename, String contentType, boolean inLine) {
		inputStream = new ByteArrayInputStream(bs);
		contentLength = bs.length;
		this.outputFileName = filename;
		this.contentType = contentType;
	}

	public CDltSpringFileStream(String wholeString, String filename, String contentType, boolean inLine) {
		inputStream = new ByteArrayInputStream(wholeString.getBytes());
		contentLength = wholeString.length();
		this.outputFileName = filename;
		this.contentType = contentType;
	}

	public CDltSpringFileStream(ByteArrayOutputStream byteArrayOutputStream, String filename, String contentType) {
		inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
		contentLength = byteArrayOutputStream.size();
		this.outputFileName = filename;
		this.contentType = contentType;
	}

	public CDltSpringFileStream(ByteArrayOutputStream byteArrayOutputStream, String filename, String contentType,
			boolean inLine) {
		inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
		contentLength = byteArrayOutputStream.size();
		this.outputFileName = filename;
		this.contentType = contentType;
	}

	public CDltSpringFileStream(File file, String fileNameToReturn) throws CDltFlowsException {
		try {
			inputStream = new BufferedInputStream(new FileInputStream(file));
		} catch (FileNotFoundException e) {
			throw new CDltFlowsException("ファイル名指定がされていないか、所定記録場所でのファイルが削除されています。\n" + "取得要求ファイル ： " + fileNameToReturn
					+ "\n" + "実絶対パス ： " + file.getAbsolutePath());
		}
		contentLength = file.length();
		this.outputFileName = fileNameToReturn;
		this.contentType = CDltFileExtensionManager.getInstance().getContentTypeOf(file.getName());
	}


	public CDltSpringFileStream(File file, String fileNameToReturn, boolean inLine)
			throws CDltFlowsException {
		try {
			inputStream = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			throw new CDltFlowsException("ファイル名指定がされていないか、所定記録場所でのファイルが削除されています。\n" + "取得要求ファイル ： " + fileNameToReturn
					+ "\n" + "実絶対パス ： " + file.getAbsolutePath());
		}
		contentLength = file.length();
		this.outputFileName = fileNameToReturn;
		this.contentType = CDltFileExtensionManager.getInstance().getContentTypeOf(file.getName());
		this.inLine = inLine;
	}

	public CDltSpringFileStream(InputStream iStream, String fileNameToReturn, String contentType) {
		inputStream = iStream;
		this.outputFileName = fileNameToReturn;
		this.contentType = CDltFileExtensionManager.getInstance().getContentTypeOf(fileNameToReturn);
	}

	public CDltSpringFileStream(InputStream iStream, String fileNameToReturn, String contentType, boolean inLine,
			EBrowser browser) {
		inputStream = iStream;
		this.outputFileName = fileNameToReturn;
		this.contentType = CDltFileExtensionManager.getInstance().getContentTypeOf(fileNameToReturn);
		this.inLine = inLine;
	}

	/**
	 * @param filePath         実ファイル名（ディレクトリ・フルパスで）
	 * @param fileNameToReturn サーバから返すときのファイル名（ディレクトリ・パスなしで）
	 * @throws CDltFlowsException
	 */
	public CDltSpringFileStream(String filePath, String fileNameToReturn) throws CDltFlowsException {
		if (filePath.isEmpty()) {
			throw new CDltFlowsException("ファイル名指定がされていません");
		}
		File file = new File(filePath);
		try {
			inputStream = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
			throw new CDltFlowsException(
					"Error on CCoreFilesManager2 in CDltSpringFileStream" + "\n発見できないファイル名：" + filePath);
		}
		contentType = CDltFileExtensionManager.getInstance().getContentTypeOf(file.getName());
		contentLength = file.length();
		if (fileNameToReturn == null || fileNameToReturn.isEmpty())
			fileNameToReturn = file.getName();
		try {
			if (inLine) {
				this.setContentDisposition("inline;filename*=utf-8''" + fileName2Download(fileNameToReturn) );
			}
			else {
				this.setContentDisposition("attachment;filename*=utf-8''" + fileName2Download(fileNameToReturn) );
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		outputFileName = fileNameToReturn;
	}

	public ResponseEntity<StreamingResponseBody> prepareDownload() {
		HttpHeaders responseHeaders = new HttpHeaders();
		String contentType = this.contentType ;
		if (contentType==null) 
			contentType = "application/octet-stream" ;
		responseHeaders.set("Content-Type", contentType);
		if (contentLength != 0)
			responseHeaders.set("Content-Length", Long.toString(this.contentLength));
		try {
			if (inLine) {
				responseHeaders.set("Content-Disposition",
						"inline; filename*=utf-8''" + fileName2Download(outputFileName));
			}
			else {
				responseHeaders.set("Content-Disposition",
						"attachment; filename*=utf-8''" + fileName2Download(outputFileName));
			}
		} catch (UnsupportedEncodingException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

		StreamingResponseBody reponseBody = new StreamingResponseBody() {
			@Override
			public void writeTo(OutputStream outputStream) throws IOException {
				try {
					byte[] buffer = new byte[BUFFER_SIZE];
					// long total = 0;
					int len = 0;
					while ((len = inputStream.read(buffer)) != -1) {
						outputStream.write(buffer, 0, len);
						outputStream.flush();
						// total = total + len;
					}
				} finally {
					inputStream.close();
				}
			}
		};
		return new ResponseEntity<StreamingResponseBody>(reponseBody, responseHeaders, HttpStatus.OK);
	}

	public ResponseEntity<StreamingResponseBody> prepareOpeningErrorMessage() {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set("Content-Type", "    text/plain");
		responseHeaders.set("Content-Disposition", "attachment; filename=FileNotFound.txt");
		StreamingResponseBody reponseBody = new StreamingResponseBody() {
			@Override
			public void writeTo(OutputStream outputStream) throws IOException {
				outputStream.flush();
			}
		};
		return new ResponseEntity<StreamingResponseBody>(reponseBody, responseHeaders, HttpStatus.NOT_FOUND);
	}

	private String fileName2Download(String fileNameToReturn) throws UnsupportedEncodingException {
		// return MimeUtility.encodeWord(fileNameToReturn, "ISO-2022-JP", "B") ;
		String encodedFileName = URLEncoder.encode(fileNameToReturn, StandardCharsets.UTF_8.name());
		return encodedFileName;
	}

}
