package net.dialectech.ftmSdCardHandler.data;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import lombok.Getter;
import net.dialectech.ftmSdCardHandler.data.supporters.CDataEntry;
import net.dialectech.ftmSdCardHandler.supporters.CConst;
import net.dialectech.ftmSdCardHandler.supporters.CYsfCodeConverter;
import net.dialectech.ftmSdCardHandler.supporters.CYsfSdCHandlerProperties;

public class CMessageEntry extends CDataEntry {

	@Getter
	private int messageSize; // このうち4bytesがDIRに記録される。

	@Getter
	private int startAddressInQSOMSG;

	@Getter
	private byte[] bMessage = new byte[CConst.MAX_MESSAGE_BYTES_PER_PACKET]; //

	/**
	 * Handlerプログラムで追加登録するときに用いる構築子
	 * 
	 * @param radioId
	 * @param destination YSFのMESSAGE機能では、送信メッセージについてはリグ上でのリスト表示で使われるので、逆にこれを利用する。
	 * @param callSign
	 * @param nodeId
	 * @param message
	 * @param dataId
	 */
	public CMessageEntry(String radioId, String destination, String callSign, String nodeId, String message,
			int dataId) {
		sym = 0x61;
		this.dataId = dataId;
		this.radioId = radioId;
		this.myCallSign = callSign;
		this.nodeId = nodeId;
		this.destination = destination;
		this.myCallSign = callSign;
		this.date2Send = new Date();
		this.date2Receive = new Date();
		this.baseDate = new Date();
		duplicateOf = null;
		fillWithSpace(this.bMessage, ' ');
		int messageLength = copyBytes2LimitedBytes(CYsfCodeConverter.getInstance().utf82YsfByte(message),
				this.bMessage);
		if (messageLength > 15)
			this.description = message.substring(0, 15);
		else
			this.description = message;
		generatedUniqString = UUID.randomUUID().toString().replaceAll("\\-", "");
		realFileExists = true; // Messageはオンメモリで展開するので、常に実データはあるものとする。
		active = true;
	}

	private void fillWithSpace(byte[] bMessage, char c) {
		int len = bMessage.length;
		for (int i = 0; i < len; ++i)
			bMessage[i] = (byte) c;
	}

	/**
	 * SDCARDから読み込んだ情報に基づいて生成するときの構築子
	 * 
	 * @param dirEntrySource
	 * @param index
	 * @param realFileName2DuplicationCheck
	 */
	public CMessageEntry(byte[] dirEntrySource, int index, byte[] wholeMessageContents) {
		CYsfCodeConverter converter = CYsfCodeConverter.getInstance();
		// まずは、自分のところに取り込む。
		dataEntry = dirEntrySource;
		sym = dirEntrySource[0];
		dataId = index + 1; // dataIdは１オリジン
		byte[] radioId = subBytes(dirEntrySource, 0x19, 0x1d);
		this.radioId = new String(radioId);
		byte[] nodeId = subBytes(dirEntrySource, 0x04, 0x08);
		this.nodeId = new String(nodeId);
		byte[] destination = subBytes(dirEntrySource, 0x09, 0x12);
		this.destination = converter.ysfByte2Utf8(destination);
		byte[] myCallSign = subBytes(dirEntrySource, 0x1e, 0x27);
		this.myCallSign = converter.ysfByte2Utf8(myCallSign);
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
		copyBytesFromMountedWholeBytes(wholeMessageContents, this.startAddressInQSOMSG,
				CConst.MAX_MESSAGE_BYTES_PER_PACKET, bMessage);

		duplicateOf = null;
		byte[] gpsPosition = subBytes(dirEntrySource, 0x64, 0x77);
		this.gpsPosition = new String(gpsPosition);
		generatedUniqString = UUID.randomUUID().toString().replaceAll("\\-", "");
		realFileExists = true; // Messageはオンメモリで展開するので、常に実データはあるものとする。
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

	/**
	 * YAESUフォーマットで記録されているmessageデータをUTF-8形式で返す。
	 * 
	 * @return
	 */
	public String getMessage() {
		return CYsfCodeConverter.getInstance().ysfByte2Utf8(bMessage);
	}

	/**
	 * YAESUフォーマットで記録されているDestinationデータをUTF-8形式で返す。
	 * 
	 * @return
	 */
	@Override
	public String getDestination() {
		return CYsfCodeConverter.getInstance().ysfByte2Utf8(CYsfCodeConverter.getInstance().utf82YsfByte(destination));
	}

	/**
	 * YAESUフォーマットで記録されているmessageデータをUTF-8形式で返す。
	 * 
	 * @return
	 */
	public String getMessageHtml() {
		String res = CYsfCodeConverter.getInstance().ysfByte2Utf8(bMessage).replaceAll(">", "&gt;")
				.replaceAll("<", "&lt;").replaceAll("\n", "<br>\n").replaceAll(" ", "&nbsp;");
		return res;
	}

	/**
	 * YAESUフォーマットで記録されているmessageデータをUTF-8形式で返す。
	 * 
	 * @return
	 */
	public void setMessage(String strByUtf8) {
		for (int index = 0; index < CConst.MAX_MESSAGE_BYTES_PER_PACKET; ++index)
			bMessage[index] = (byte) ' ';
		byte[] source = CYsfCodeConverter.getInstance().utf82YsfByte(strByUtf8);
		this.copyBytes2LimitedBytes(source, bMessage);
	}

	@Override
	public void storeOwnData2Buffer() {
		CYsfCodeConverter ysfConverter = CYsfCodeConverter.getInstance();

		dataEntry[0] = sym;
		byte[] binData = nodeId.getBytes(StandardCharsets.UTF_8);
		binData = fillWithByteOf(binData, (byte) 0x20, 5);
		bytes2ByfferPoint(binData, 0x04, 5);

		binData = ysfConverter.utf82YsfByte(destination);
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

		dataEntry[0x50] = (byte) ((startAddressInQSOMSG >> 24) & 0x0ff);
		dataEntry[0x51] = (byte) ((startAddressInQSOMSG >> 16) & 0xff);
		dataEntry[0x52] = (byte) ((startAddressInQSOMSG >> 8) & 0xff);
		dataEntry[0x53] = (byte) (startAddressInQSOMSG & 0xff);

//		binData = fileCoreName.getBytes(StandardCharsets.UTF_8);
//		binData = fillWithByteOf(binData, (byte) 0xff, 16);
//		bytes2ByfferPoint(fileCoreName.getBytes(StandardCharsets.UTF_8), 0x54, 16);

		for (int index = 0x64; index <= 0x77; ++index)
			dataEntry[index] = (byte) 0xff;
	}

	public void clearMessage() {

	}

	public void setStartAddressInQSOMessageOf(int index) {
		startAddressInQSOMSG = index * CConst.MAX_MESSAGE_BYTES_PER_PACKET;
	}

	public int getIndexByStartAddressInQSOMessage() {
		return startAddressInQSOMSG / CConst.MAX_MESSAGE_BYTES_PER_PACKET;
	}

	@Override
	public String getRepresentativesName() {
		// TODO 自動生成されたメソッド・スタブ
		return getMessage();
	}

	@Override
	public Date getRepresentativeTime() {
		// TODO 自動生成されたメソッド・スタブ
		if (isThisTransmission())
			return this.date2Send;
		else
			return this.date2Receive;
	}
}
