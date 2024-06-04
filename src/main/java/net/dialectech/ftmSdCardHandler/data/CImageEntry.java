package net.dialectech.ftmSdCardHandler.data;

import java.util.Date;
import java.util.LinkedList;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.text.StringEscapeUtils;

import java.nio.charset.StandardCharsets;
import java.text.*;
import lombok.Getter;
import lombok.Setter;
import net.dialectech.ftmSdCardHandler.data.supporters.CDataEntry;
import net.dialectech.ftmSdCardHandler.supporters.CYsfCodeConverter;
import net.dialectech.ftmSdCardHandler.supporters.CYsfSdCHandlerProperties;

public class CImageEntry extends CDataEntry {

	@Getter
	private String qrString = ""; // QRコード内部文字列。文字列無くてもnullにしないこと。
	@Getter
	@Setter
	private LinkedList<String> includedURL = null; // QRコード内部に含まれるURLを記録
	@Getter
	@Setter
	private LinkedList<String> includedEMailAddress = null; // QRコード内部に含まれるE-Mail Addressを記録
	@Getter
	@Setter
	private int includedLinkDataQuantity; // QRコード内部に含まれるURLとE-Mail
											// Addressとの総数。つまり、includedURL.size()+includedEMailAddress.size()を記録
	@Getter
	@Setter
	protected String fileCoreName;

	@Getter
	private int pictureSize; // このうち4bytesがDIRに記録される。

	Pattern pattern4URL = Pattern.compile("http(s)?://([\\w-]+\\.)+[\\w-]+(/[\\w- ./?%&=!]*)?");
	Pattern pattern4EMail = Pattern.compile("[\\w\\-\\._]+@[\\w\\-\\._]+\\.[A-Za-z]+");

	/**
	 * Handlerプログラムで追加登録するときに用いる構築子
	 * 
	 * @param radioId
	 * @param callSign
	 * @param nodeId
	 * @param newFileName
	 */
	public CImageEntry(String radioId, String callSign, String nodeId, String newFileName, int dataId,
			String description2Write) {
		super() ;
		sym = 0;
		this.dataId = dataId;
		this.radioId = radioId;
		this.myCallSign = callSign;
		this.nodeId = nodeId;
		this.radioId = radioId;
		this.nodeId = nodeId;
		this.destination = "ALL";
		this.myCallSign = callSign;
		this.date2Send = new Date();
		this.date2Receive = new Date();
		this.baseDate = new Date();
		duplicateOf = null;
		this.fileCoreName = newFileName;
		if (description2Write == null || description2Write.equals("")) {
			// description2Changeが指定されていないときには、標準的なものとする。
			this.description = getNumberPartOfFileName(newFileName) + ":"
					+ (new SimpleDateFormat("ssmm/MdH").format(new Date()));
		} else {
			if (description2Write.length() > 15)
				this.description = description2Write.substring(0, 15);
			else
				this.description = description2Write;
		}
		generatedUniqString = UUID.randomUUID().toString().replaceAll("\\-", "");
	}

	/**
	 * SDCARDから読み込んだ情報に基づいて生成するときの構築子
	 * 
	 * @param source
	 * @param index
	 * @param realFileName2DuplicationCheck
	 */
	public CImageEntry(byte[] source, int index) {
		super() ;
		CYsfCodeConverter converter = CYsfCodeConverter.getInstance() ;
		// まずは、自分のところに取り込む。
		dataEntry = source;
		sym = source[0];
		dataId = index + 1; // dataIdは１オリジン
		byte[] radioId = subBytes(source, 0x19, 0x1d);
		this.radioId = new String(radioId);
		byte[] nodeId = subBytes(source, 0x04, 0x08);
		this.nodeId = new String(nodeId);
		byte[] destination = subBytes(source, 0x09, 0x12);
		this.destination = converter.ysfByte2Utf8(destination);
		byte[] myCallSign = subBytes(source, 0x1e, 0x27);
		this.myCallSign = converter.ysfByte2Utf8(myCallSign);
		byte[] date = subBytes(source, 0x2e, 0x33);
		this.date2Send = dateFromYaesuExp(date);
		date = subBytes(source, 0x34, 0x39);
		this.date2Receive = dateFromYaesuExp(date);
		date = subBytes(source, 0x3a, 0x3f);
		this.baseDate = dateFromYaesuExp(date);
		byte[] description = subBytes(source, 0x40, 0x4A);
		this.description = CYsfCodeConverter.getInstance().ysfByte2Utf8(description).trim();
		byte[] pictureSize = subBytes(source, 0x50, 0x53);
		this.pictureSize = (int) ((pictureSize[0] << 24) & 0xff000000) + (int) ((pictureSize[1] << 16) & 0xff0000)
				+ (int) ((pictureSize[2] << 8) & 0xff00) + (int) (pictureSize[3] & 0xff);
		byte[] fileName = subBytes(source, 0x54, 0x63);
		duplicateOf = null;
		this.fileCoreName = new String(fileName);
		byte[] gpsPosition = subBytes(source, 0x64, 0x77);
		this.gpsPosition = new String(gpsPosition);
		generatedUniqString = UUID.randomUUID().toString().replaceAll("\\-", "");
	}

	public String getQrStringHtml() {
		if (qrString == null)
			return null;
		String htmlConverted = StringEscapeUtils.escapeHtml4(qrString).replaceAll("\n", "<br>\n").replaceAll(" ",
				"&nbsp;");
		return htmlConverted;
	}

	public void setQrString(String qrString) {
		this.qrString = qrString;

		if (qrString == null)
			return;

		includedURL = new LinkedList<String>();
		includedEMailAddress = new LinkedList<String>();
		includedLinkDataQuantity = 0;

		Matcher matcher = pattern4URL.matcher(qrString);

		while (matcher.find()) {
			includedURL.add(matcher.group(0));
			includedLinkDataQuantity++;
		}

		Matcher matcher4EMailAddress = pattern4EMail.matcher(qrString);
		while (matcher4EMailAddress.find()) {
			includedEMailAddress.add(matcher4EMailAddress.group(0));
			includedLinkDataQuantity++;
		}
	}

	public String getQrStringEscaped() {
		if (qrString == null)
			return null;
		String htmlConverted = StringEscapeUtils.escapeHtml4(qrString);
		return htmlConverted;
	}

	public void setPictureSize(int size) {
		pictureSize = size;
		dataEntry[0x50] = (byte) ((pictureSize & 0xff0000) >> 24);
		dataEntry[0x51] = (byte) ((pictureSize & 0xff00) >> 16);
		dataEntry[0x52] = (byte) ((pictureSize & 0xff00) >> 8);
		dataEntry[0x53] = (byte) (pictureSize & 0xff);
	}

	public void storeOwnData2Buffer() {
		dataEntry[0] = sym;
		byte[] binData = nodeId.getBytes(StandardCharsets.UTF_8);
		binData = fillWithByteOf(binData, (byte) 0x20, 5);
		bytes2ByfferPoint(binData, 0x04, 5);

		binData = destination.getBytes(StandardCharsets.UTF_8);
		binData = fillWithByteOf(binData, (byte) 0x20, 10);
		bytes2ByfferPoint(binData, 0x09, 10);
		// 0x20で埋めるところ
		binData = fillWithByteOf(null, (byte) 0x20, 6);
		bytes2ByfferPoint(binData, 0x13, 6);

		binData = radioId.getBytes(StandardCharsets.UTF_8);
		binData = fillWithByteOf(binData, (byte) 0xff, 5);
		bytes2ByfferPoint(binData, 0x19, 5);

		// My Callsign の部分は、FTM-300Dでは、10文字で埋まらない部分を$FFに、FT-5Dでは、$20で埋める。
		// ここでは、FT-5Dに合わせて$20で埋めてみる。
		binData = myCallSign.getBytes(StandardCharsets.UTF_8);
		binData = fillWithByteOf(binData, (byte) CYsfSdCHandlerProperties.getInstance().getFillByteInPctDir(), 10);
		bytes2ByfferPoint(binData, 0x1e, 10);
		// 0x20で埋めるところ
		binData = fillWithByteOf(null, (byte) 0x20, 6);
		bytes2ByfferPoint(binData, 0x28, 6);

		date2DataByfferPoint(date2Send, 0x2e);
		date2DataByfferPoint(date2Receive, 0x34);
		date2DataByfferPoint(baseDate, 0x3a);

		// １１バイトを超えたら、１１バイトでシールする。
//		if (description.length() > 11)
//			description = description.substring(0, 11);
//		binData = description.getBytes(StandardCharsets.UTF_8);
		binData = CYsfCodeConverter.getInstance().utf82YsfByte(description);
		binData = fillWithByteOf(binData, (byte) 0x20, 11);
		bytes2ByfferPoint(binData, 0x40, 11);
		// 0x20で埋めるところ
		binData = fillWithByteOf(null, (byte) 0x20, 5);
		bytes2ByfferPoint(binData, 0x4b, 5);

		dataEntry[0x50] = (byte) ((pictureSize >> 24) & 0x0ff);
		dataEntry[0x51] = (byte) ((pictureSize >> 16) & 0xff);
		dataEntry[0x52] = (byte) ((pictureSize >> 8) & 0xff);
		dataEntry[0x53] = (byte) (pictureSize & 0xff);

		binData = fileCoreName.getBytes(StandardCharsets.UTF_8);
		binData = fillWithByteOf(binData, (byte) 0xff, 16);
		bytes2ByfferPoint(fileCoreName.getBytes(StandardCharsets.UTF_8), 0x54, 16);

		for (int index = 0x64; index <= 0x77; ++index)
			dataEntry[index] = (byte) 0xff;
	}

	public int getNumberPartOfFileName() {
		return getNumberPartOfFileName(fileCoreName);
	}

	public int getNumberPartOfFileName(String fileName) {
		int numberPartOfFileName = 0;
		try {
			String partOfNumber = fileName.substring(6, 12); // M(radioID)XXXXXXX.jpgのうちのXXXXXXの部分の文字列位置。フォーマットが固定されているので、正規表現による解析不要
			numberPartOfFileName = Integer.valueOf(partOfNumber);
		} catch (NumberFormatException e) {
			// 何もせずに、０を返す。
		}

		return numberPartOfFileName;
	}

	@Override
	public String getRepresentativesName() {
		return getFileCoreName();
	}

	@Override
	public Date getRepresentativeTime() {
		// TODO 自動生成されたメソッド・スタブ
		return this.date2Send;
	}

}
