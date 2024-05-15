package net.dialectech.ftmSdCardHandler.supporters.fileSystem;

import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.text.StringEscapeUtils;

import jakarta.xml.bind.DatatypeConverter;

import java.nio.charset.StandardCharsets;
import java.text.*;
import lombok.Getter;
import lombok.Setter;
import net.dialectech.ftmSdCardHandler.supporters.CYsfSdCHandlerProperties;

public class CImageEntry {
	@Getter
	@Setter
	private boolean active = false;
	@Getter
	@Setter
	private boolean realFileExists = false;
	@Getter
	@Setter
	private int imageId; // PCTDIR/PCTFAT等の構造化データには入れない情報、内部のID、１オリジン
	@Getter
	@Setter
	private int absDirPos; // FAT内
	@Getter
	@Setter
	private byte sym = 0x00;
	@Getter
	@Setter
	private byte fatSym = 0x40; // FAT中のSYM.通常は0x40
	@Getter
	@Setter
	private String nodeId;
	@Getter
	@Setter
	private String destination = "ALL       "; // １０文字
	@Getter
	@Setter
	private String radioId;
	@Getter
	@Setter
	private String myCallSign;
	@Getter
	@Setter
	private Date date2Send = new Date();
	@Getter
	@Setter
	private Date date2Receive = new Date();
	@Getter
	@Setter
	private Date baseDate = new Date();
	@Getter
	@Setter
	private String description = ""; // 必ず１６文字
	@Getter
	private String qrString = ""; // QRコード内部
	@Getter
	@Setter
	private String includedURL = null; // QRコード内部に含まれるURLを記録
	@Getter
	@Setter
	private String includedEMailAddress = null; // QRコード内部に含まれるE-Mail Addressを記録
	@Getter
	private int pictureSize; // このうち4bytesがDIRに記録される。
	@Getter
	@Setter
	private String fileCoreName;
	@Getter
	@Setter
	private String gpsPosition;
	@Getter
	@Setter
	private String duplicateOf; // 既登録ファイル名と重複しているばあいにはそのIndexを記録。重複しないときには、null
	@Getter
	private String generatedUniqString;

	@Getter
	@Setter
	private byte[] imageEntry = new byte[128];

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
	public CImageEntry(String radioId, String callSign, String nodeId, String newFileName, int imageId,
			String description2Write) {
		sym = 0;
		this.imageId = imageId;
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
		}
		else {
			if (description2Write.length()>15)
				this.description =description2Write.substring(0,15) ;
			else
				this.description =description2Write ;
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
		// まずは、自分のところに取り込む。
		imageEntry = source;
		sym = source[0];
		imageId = index + 1; // imageIdは１オリジン
		byte[] radioId = subBytes(source, 0x19, 0x1d);
		this.radioId = new String(radioId);
		byte[] nodeId = subBytes(source, 0x04, 0x08);
		this.nodeId = new String(nodeId);
		byte[] destination = subBytes(source, 0x09, 0x12);
		this.destination = new String(destination);
		byte[] myCallSign = subBytes(source, 0x1e, 0x27);
		this.myCallSign = new String(myCallSign);
		byte[] date = subBytes(source, 0x2e, 0x33);
		this.date2Send = dateFromYaesuExp(date);
		date = subBytes(source, 0x34, 0x39);
		this.date2Receive = dateFromYaesuExp(date);
		date = subBytes(source, 0x3a, 0x3f);
		this.baseDate = dateFromYaesuExp(date);
		byte[] description = subBytes(source, 0x40, 0x4A);
		this.description = new String(description);
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

	public int getNumberPartOfFileName() {
		return getNumberPartOfFileName(fileCoreName);
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

		Matcher matcher = pattern4URL.matcher(qrString);
		if (matcher.find()) {
			includedURL = matcher.group(0);
		} else
			includedURL = null;

		Matcher matcher4EMailAddress = pattern4EMail.matcher(qrString);
		if (matcher4EMailAddress.find()) {
			includedEMailAddress = matcher4EMailAddress.group(0);
		} else
			includedEMailAddress = null;
	}

	public String getQrStringEscaped() {
		if (qrString == null)
			return null;
		String htmlConverted = StringEscapeUtils.escapeHtml4(qrString);
		return htmlConverted;
	}

	/**
	 * dateFromYaesuExp は、引数のdate情報からDate型に変換する。
	 * 
	 * 引数のdate情報は、6バイトで構成されたYaesuFusionSystem中での日時情報フォーマットのバイト列と
	 * する。これはBCD形式で、2024/01/30 02:03:04の場合、16進数の、$24 $01 $30 $02 $03 $04という
	 * 形式で記録されている。
	 * 
	 * @param date
	 * @return
	 */
	private Date dateFromYaesuExp(byte[] date) {
		String fullDate = "20";
		fullDate += (stringFromTwoDigit(date[0]) + "/");
		fullDate += (stringFromTwoDigit(date[1]) + "/");
		fullDate += (stringFromTwoDigit(date[2]) + " ");
		fullDate += (stringFromTwoDigit(date[3]) + ":");
		fullDate += (stringFromTwoDigit(date[4]) + ":");
		fullDate += (stringFromTwoDigit(date[5]));
		try {
			Date thisDate = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.JAPAN).parse(fullDate);
			return thisDate;
		} catch (ParseException e) {
			return new Date();
		}
	}

	private String stringFromTwoDigit(byte b) {
		byte[] res = new byte[2];
		byte data = (byte) (((b & 0xf0) >> 4) + 0x30);
		res[0] = data;
		data = (byte) ((b & 0xf) + 0x30);
		res[1] = data;
		return new String(res);
	}

	/**
	 * subBytesは、subStringと同様、バイト列sourceから、start番目から始まりendP番目で終わる byte[]列に複写する。
	 * 
	 * @param source
	 * @param start
	 * @param endP
	 * @return
	 */
	private byte[] subBytes(byte[] source, int start, int endP) {
		byte[] res = new byte[endP - start + 1];
		int count = 0;
		try {
			for (int index = start; index <= endP; ++index) {
				res[count++] = (source[index] == -1 ? 0x20 : source[index]);
			}
		} catch (Exception e) {
			return null;
		}
		return res;
	}

	public void setPictureSize(int size) {
		pictureSize = size;
		imageEntry[0x50] = (byte) ((pictureSize & 0xff0000) >> 24);
		imageEntry[0x51] = (byte) ((pictureSize & 0xff00) >> 16);
		imageEntry[0x52] = (byte) ((pictureSize & 0xff00) >> 8);
		imageEntry[0x53] = (byte) (pictureSize & 0xff);
	}

	public void date2ByfferPoint(Date soure, int startP) {
		DateFormat dfm = new SimpleDateFormat("yyyyMMddHHmmss");
		String strTime = dfm.format(soure);
		for (int index = 0; index < 6; ++index) {
			byte[] d = DatatypeConverter.parseHexBinary(strTime.substring(2 + index * 2, 2 + index * 2 + 2));
			imageEntry[index + startP] = d[0];
		}
	}

	public void bytes2ByfferPoint(byte[] source, int startP, int volume) {
		for (int index = 0; index < volume; ++index) {
			imageEntry[index + startP] = source[index];
		}
	}

	public void data2Block() {
		imageEntry[0] = sym;
	}

	public String getSymMeaning() {
		String decodedString;
		switch (sym) {
		case 0x00:
			decodedString = "自登録画像";
			break;
		case 0x04:
			decodedString = "WD（未）"; // Get From Wires
			break;
		case 0x14:
			decodedString = "WD（既）"; // Get From Wires
			break;
		case 0x05:
			decodedString = "WU（S）"; // Get From Wires
			break;
		case 0x15:
			decodedString = "WU（F）"; // Get From Wires
			break;

		case 0x22:
			decodedString = "GM受（未）"; // Group Monitor
			break;
		case 0x23:
			decodedString = "GM送（S）"; // Group Monitor
			break;
		case 0x32:
			decodedString = "GM受（既）"; // Group Monitor
			break;
		case 0x33:
			decodedString = "GM送（F）"; // Group Monitor
			break;

		case 0x60:
			decodedString = "受信（未）";
			break;
		case 0x61:
			decodedString = "既送信（S）";
			break;
		case 0x70:
			decodedString = "受信（既）";
			break;
		case 0x71:
			decodedString = "既送信（F）";
			break;
		default:
			decodedString = "0X" + Integer.toHexString(((int) sym) & 0xff);
			break;
		}
		return decodedString;
	}

	public void storeOwnData2BufferedBytes() {
		imageEntry[0] = sym;
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

		date2ByfferPoint(date2Send, 0x2e);
		date2ByfferPoint(date2Receive, 0x34);
		date2ByfferPoint(baseDate, 0x3a);

		// １１バイトを超えたら、１１バイトでシールする。
		if (description.length() > 11)
			description = description.substring(0, 11);
		binData = description.getBytes(StandardCharsets.UTF_8);
		binData = fillWithByteOf(binData, (byte) 0x20, 11);
		bytes2ByfferPoint(binData, 0x40, 11);
		// 0x20で埋めるところ
		binData = fillWithByteOf(null, (byte) 0x20, 5);
		bytes2ByfferPoint(binData, 0x4b, 5);

		imageEntry[0x50] = (byte) ((pictureSize >> 24) & 0x0ff);
		imageEntry[0x51] = (byte) ((pictureSize >> 16) & 0xff);
		imageEntry[0x52] = (byte) ((pictureSize >> 8) & 0xff);
		imageEntry[0x53] = (byte) (pictureSize & 0xff);

		binData = fileCoreName.getBytes(StandardCharsets.UTF_8);
		binData = fillWithByteOf(binData, (byte) 0xff, 16);
		bytes2ByfferPoint(fileCoreName.getBytes(StandardCharsets.UTF_8), 0x54, 16);

		for (int index = 0x64; index <= 0x77; ++index)
			imageEntry[index] = (byte) 0xff;
	}

	/**
	 * indexVolumeで指定するバイト数のバイト列を生成する。この際、binDataで与えられたバイト列をコピーするが、
	 * indexVolumeを超えるバイト数である場合には、indexVolumeまでをコピー、これに満たない場合には、残りを
	 * byte2Fillで埋めていく。バイト列が終わったところでも、特別にターミネータ文字を埋め込むようなことはしない。
	 * 
	 * @param binData
	 * @param byte2Fill
	 * @param indexVolume
	 * @return
	 */
	private byte[] fillWithByteOf(byte[] binData, byte byte2Fill, int indexVolume) {
		// System.out.println("indexVolume:" + indexVolume + " , binData-Length" +
		// binData.length);
		byte[] res = new byte[indexVolume];
		int startP = 0;
		if (binData != null) {
			for (int index = 0; index < binData.length; ++index) {
				if (index >= indexVolume)
					break;
				res[index] = binData[index];
			}
			startP = binData.length;
		}
		for (int index = startP; index < indexVolume; ++index) {
			res[index] = byte2Fill;
		}

		return res;
	}

	public String borderDefinition() {
		// active?'solid 5px #00f':${(entity.sym==96||entity.sym==97)?'solid 5px
		// #ff0':'solid 5px #f00'}
		if (active == false)
			return "solid 3px #000";

		String decodedString;
		switch (sym) {
		case 0x00:
			// 自作画像
			decodedString = "solid 3px #0d0";
			break;
		case 0x04:
			// Wires D/L 未読
			decodedString = "solid 3px #0dd";
			break;
		case 0x14:
			// Wires D/L 既読
			decodedString = "solid 3px #00d";
			break;
		case 0x05:
			// Wires U/L 成功
			decodedString = "solid 3px #080"; // Get From Wires
			break;
		case 0x15:
			// Wires U/L 失敗
			decodedString = "solid 3px #080"; // Get From Wires
			break;

		case 0x22:
			decodedString = "solid 3px #0cc"; // Group Monitor GM受（未）
			break;
		case 0x23:
			decodedString = "solid 3px #080"; // Group Monitor
			break;
		case 0x32:
			decodedString = "solid 3px #080"; // Group Monitor GM受（未）
			break;
		case 0x33:
			decodedString = "solid 3px #080"; // Group Monitor
			break;

		case 0x60:
			// 受信（未読）
			decodedString = "solid 3px #0ff";
			break;
		case 0x61:
			// 既送信（成功）
			decodedString = "solid 3px #080";
			break;
		case 0x70:
			// 受信（既読）
			decodedString = "solid 3px #00f";
			break;
		case 0x71:
			// 既送信（失敗）
			decodedString = "solid 3px #880";
			break;
		default:
			decodedString = "solid 3px #f44";
			break;
		}
		return decodedString;
	}

	public String getBackgroundDefinition() {
		// active?'solid 5px #00f':${(entity.sym==96||entity.sym==97)?'solid 5px
		// #ff0':'solid 5px #f00'}
		if (active == false)
			return "#888";

		String decodedString;
		switch (sym) {
		case 0x00:
			// 自作画像
			decodedString = "#cfc";
			break;
		case 0x04:
			// Wires D/L（未読）
			decodedString = "#8dd";
			break;
		case 0x14:
			// Wires D/L (既読）
			decodedString = "#8d8";
			break;

		case 0x05:
			// Wires U/L 成功
			decodedString = "#080";
			break;
		case 0x15:
			// Wires U/L 失敗
			decodedString = "#080";
			break;

		case 0x22:
			decodedString = "#8cc"; // GM受信（未読）
			break;
		case 0x23:
			decodedString = "#484"; // GM送信（成功）
			break;
		case 0x32:
			decodedString = "#88C"; // GM受信（既読）
			break;
		case 0x33:
			decodedString = "#846"; // GM送信（失敗）
			break;

		case 0x60:
			// 受信（未読）
			decodedString = "#cff";
			break;
		case 0x61:
			// 既送信（成功）
			decodedString = "#6f6";
			break;
		case 0x70:
			// 受信（既読）
			decodedString = "#ccf";
			break;
		case 0x71:
			// 既送信（失敗）
			decodedString = "#fc4";
			break;
		default:
			decodedString = "#f44";
			break;
		}
		return decodedString;
	}

}
