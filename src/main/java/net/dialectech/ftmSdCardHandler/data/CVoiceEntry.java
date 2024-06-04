package net.dialectech.ftmSdCardHandler.data;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import org.apache.commons.io.FileUtils;

import lombok.Getter;
import lombok.Setter;
import net.dialectech.ftmSdCardHandler.data.supporters.CDataEntry;
import net.dialectech.ftmSdCardHandler.supporters.CConst;
import net.dialectech.ftmSdCardHandler.supporters.CYsfCodeConverter;
import net.dialectech.ftmSdCardHandler.supporters.CYsfSdCHandlerProperties;

public class CVoiceEntry extends CDataEntry {

	@Getter
	private int messageSize; // このうち4bytesがDIRに記録される。

	@Getter
	@Setter
	private String fileName;

	@Getter
	@Setter
	private long fileSize;

	@Getter
	private String nonAssoc1 ;
	@Getter
	private int nonAssoc2 ;
	
	/**
	 * VOICEについて、Handlerプログラムで追加登録するときに用いる構築子はSPEC上、不要。
	 * 
	 * @param radioId
	 * @param callSign
	 * @param nodeId
	 * @param newFileName
	 * 
	 * 
	 *                    public CVoiceEntry(String radioId, String callSign, String
	 *                    nodeId, String message, int dataId) { sym = 0; this.dataId
	 *                    = dataId; this.radioId = radioId; this.myCallSign =
	 *                    callSign; this.nodeId = nodeId; this.radioId = radioId;
	 *                    this.nodeId = nodeId; this.destination = "ALL";
	 *                    this.myCallSign = callSign; this.date2Send = new Date();
	 *                    this.date2Receive = new Date(); this.baseDate = new
	 *                    Date(); duplicateOf = null; int messageLength =
	 *                    copyBytes2LimitedBytes(CYsfCodeConverter.getInstance().utf82YsfByte(message),
	 *                    this.message); if (messageLength > 15) this.description =
	 *                    message.substring(0, 15); else this.description = message;
	 *                    generatedUniqString =
	 *                    UUID.randomUUID().toString().replaceAll("\\-", ""); }
	 */

	/**
	 * SDCARDから読み込んだ情報に基づいて生成するときの構築子
	 * 
	 * @param dirEntrySource
	 * @param index
	 * @param realFileName2DuplicationCheck
	 */
	public CVoiceEntry(byte[] source, int index) {
		// まずは、自分のところに取り込む。
		dataEntry = source;
		sym = source[0];
		dataId = index + 1; // dataIdは１オリジン
		byte[] radioId = subBytes(source, 0x19, 0x1d);
		this.radioId = new String(radioId);
		byte[] nodeId = subBytes(source, 0x04, 0x08);
		this.nodeId = new String(nodeId);
		if (this.nodeId==null)
			this.nodeId = " ".repeat(5);
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
		this.description = CYsfCodeConverter.getInstance().ysfByte2Utf8(description).trim();
		byte[] pictureSize = subBytes(source, 0x50, 0x53);
		this.nonAssoc2 = (int) ((pictureSize[0] << 24) & 0xff000000) + (int) ((pictureSize[1] << 16) & 0xff0000)
				+ (int) ((pictureSize[2] << 8) & 0xff00) + (int) (pictureSize[3] & 0xff);
		byte[] fileName = subBytes(source, 0x54, 0x63);
		duplicateOf = null;
		this.fileName = new String(fileName);
		byte[] gpsPosition = subBytes(source, 0x64, 0x77);
		this.gpsPosition = new String(gpsPosition);
		generatedUniqString = UUID.randomUUID().toString().replaceAll("\\-", "");
	}

	public void executeChangeFileName(String fileName) {
		try {
			FileUtils.moveFile(convertUtf2FileOfSourceName(this.getFileName()), convertUtf2FileOfSourceName(fileName));
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
	}

	/**
	 * ".wav"を除いたファイル名を返す。主としてThymeleafのTemplateから使用する。
	 * @return
	 */
	public String getFileNameCore() {
		return fileName.replaceAll("\\.wav$","");
	}
	
	@Override
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

		dataEntry[0x50] = (byte) 0;
		dataEntry[0x51] = (byte) 0;
		dataEntry[0x52] = (byte) 0;
		dataEntry[0x53] = (byte) 0;

		binData = fileName.getBytes(StandardCharsets.UTF_8);
		binData = fillWithByteOf(binData, (byte) 0xff, 16);
		bytes2ByfferPoint(fileName.getBytes(StandardCharsets.UTF_8), 0x54, 16);

		for (int index = 0x64; index <= 0x77; ++index)
			dataEntry[index] = (byte) 0xff;
	}

	protected File convertUtf2FileOfSourceName(String sourceName) {
		CYsfSdCHandlerProperties prop = CYsfSdCHandlerProperties.getInstance();

		File newFile = new File(
				prop.getSdCardBaseDirName()  + CConst.VoiceFoldername + File.separator + new String(convertUtfFileName2Bytes(sourceName)));
		return newFile;
	}

	private byte[] convertUtfFileName2Bytes(String newFileName) {

		final int MAXLENGTH_OF_FILENAME = 16 ;
		
		byte[] bytesOfFilenameSource = CYsfCodeConverter.getInstance().utf82YsfByte(newFileName);
		byte[] byteOfFileName = new byte[MAXLENGTH_OF_FILENAME];
		int maxByte = MAXLENGTH_OF_FILENAME;

		if (bytesOfFilenameSource.length < MAXLENGTH_OF_FILENAME)
			maxByte = bytesOfFilenameSource.length;

		for (int i = 0; i < maxByte; ++i) {
			byteOfFileName[i] = bytesOfFilenameSource[i];
		}
		for (int i = maxByte; i < MAXLENGTH_OF_FILENAME; ++i) {
			byteOfFileName[i] = 0x20;
		}
		byteOfFileName[12] = '.';
		byteOfFileName[13] = 'w';
		byteOfFileName[14] = 'a';
		byteOfFileName[15] = 'v';
		
		return byteOfFileName ;		
	}

	@Override
	public String getRepresentativesName() {
		return getFileNameCore();
	}

	@Override
	public Date getRepresentativeTime() {
		// TODO 自動生成されたメソッド・スタブ
		return date2Send;
	}
}
