package net.dialectech.ftmSdCardHandler.supporters.fileSystem;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import lombok.Getter;
import net.dialectech.ftmSdCardHandler.supporters.CYsfCodeConverter;
import net.dialectech.ftmSdCardHandler.supporters.CYsfSdCHandlerProperties;

public class CMessageEntry extends CDataEntry {

	@Getter
	private int messageSize; // このうち4bytesがDIRに記録される。

	@Getter
	private int startAddressInQSOMSG;

	private byte[] message = new byte[80]; //

	/**
	 * Handlerプログラムで追加登録するときに用いる構築子
	 * 
	 * @param radioId
	 * @param callSign
	 * @param nodeId
	 * @param newFileName
	 */
	public CMessageEntry(String radioId, String callSign, String nodeId, String message, int dataId) {
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
		int messageLength = copyBytes2LimitedBytes(CYsfCodeConverter.getInstance().utf82YsfByte(message), this.message);
		if (messageLength > 15)
			this.description = message.substring(0, 15);
		else
			this.description = message;
		generatedUniqString = UUID.randomUUID().toString().replaceAll("\\-", "");
	}

	/**
	 * SDCARDから読み込んだ情報に基づいて生成するときの構築子
	 * 
	 * @param dirEntrySource
	 * @param index
	 * @param realFileName2DuplicationCheck
	 */
	public CMessageEntry(byte[] dirEntrySource, int index, byte[] wholeMessageContents) {
		// まずは、自分のところに取り込む。
		dataEntry = dirEntrySource;
		sym = dirEntrySource[0];
		dataId = index + 1; // dataIdは１オリジン
		byte[] radioId = subBytes(dirEntrySource, 0x19, 0x1d);
		this.radioId = new String(radioId);
		byte[] nodeId = subBytes(dirEntrySource, 0x04, 0x08);
		this.nodeId = new String(nodeId);
		byte[] destination = subBytes(dirEntrySource, 0x09, 0x12);
		this.destination = new String(destination);
		byte[] myCallSign = subBytes(dirEntrySource, 0x1e, 0x27);
		this.myCallSign = new String(myCallSign);
		byte[] date = subBytes(dirEntrySource, 0x2e, 0x33);
		this.date2Send = dateFromYaesuExp(date);
		date = subBytes(dirEntrySource, 0x34, 0x39);
		this.date2Receive = dateFromYaesuExp(date);
		date = subBytes(dirEntrySource, 0x3a, 0x3f);
		this.baseDate = dateFromYaesuExp(date);
		byte[] description = subBytes(dirEntrySource, 0x40, 0x4A);
		this.description = CYsfCodeConverter.getInstance().ysfByte2Utf8(description).trim();
		byte[] startAddressInQSOMSG = subBytes(dirEntrySource, 0x50, 0x53);
		this.startAddressInQSOMSG = (int) ((startAddressInQSOMSG[0] << 24) & 0xff000000)
				+ (int) ((startAddressInQSOMSG[1] << 16) & 0xff0000) + (int) ((startAddressInQSOMSG[2] << 8) & 0xff00)
				+ (int) (startAddressInQSOMSG[3] & 0xff);
		copyBytesFromMountedWholeBytes(wholeMessageContents, this.startAddressInQSOMSG, 80, message);

		duplicateOf = null;
		byte[] gpsPosition = subBytes(dirEntrySource, 0x64, 0x77);
		this.gpsPosition = new String(gpsPosition);
		generatedUniqString = UUID.randomUUID().toString().replaceAll("\\-", "");
	}

	private int copyBytes2LimitedBytes(byte[] source, byte[] destination) {
		int limitedQuantity = source.length < destination.length ? source.length : destination.length;

		for (int index = 0; index < limitedQuantity; ++index) {
			destination[index] = source[index];
		}
		return limitedQuantity;
	}

	private void copyBytesFromMountedWholeBytes(byte[] wholeSource, int startPosition, int volume, byte[] destination) {
		try {
			for (int sourceLocation = startPosition, index = 0; index < volume; ++sourceLocation, ++index) {
				destination[index] = wholeSource[sourceLocation];
			}
		} catch (Exception e) {
			System.out.println("ERROR : " + e.getMessage());
		}

	}
	
	public String getMessage() {
		return CYsfCodeConverter.getInstance().ysfByte2Utf8(message);
	}

	@Override
	public void storeOwnData2BufferedBytes() {
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

		dataEntry[0x50] = (byte) ((messageSize >> 24) & 0x0ff);
		dataEntry[0x51] = (byte) ((messageSize >> 16) & 0xff);
		dataEntry[0x52] = (byte) ((messageSize >> 8) & 0xff);
		dataEntry[0x53] = (byte) (messageSize & 0xff);

//		binData = fileCoreName.getBytes(StandardCharsets.UTF_8);
//		binData = fillWithByteOf(binData, (byte) 0xff, 16);
//		bytes2ByfferPoint(fileCoreName.getBytes(StandardCharsets.UTF_8), 0x54, 16);

		for (int index = 0x64; index <= 0x77; ++index)
			dataEntry[index] = (byte) 0xff;
	}

	public void clearMessage() {

	}
}
