package net.dialectech.ftmSdCardHandler.data.supporters;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import jakarta.xml.bind.DatatypeConverter;
import lombok.Getter;
import lombok.Setter;

public abstract class CDataEntry {
	@Getter
	@Setter
	protected int dataId; // ～DIR/～FAT等の構造化データには入れない情報、内部のID、１オリジン
	@Getter
	@Setter
	protected boolean active = false;
	@Getter
	@Setter
	protected byte sym = 0x00;

	/* ************************************************ */

	@Getter
	@Setter
	protected boolean realFileExists = false;

	@Getter
	@Setter
	protected int absDirPos; // FAT内
	@Getter
	@Setter
	protected byte fatSym = 0x40; // FAT中のSYM.通常は0x40
	@Getter
	@Setter
	protected String nodeId;
	@Getter
	@Setter
	protected String destination = "ALL       "; // １０文字
	@Getter
	@Setter
	protected String radioId;
	@Getter
	@Setter
	protected String myCallSign;
	@Getter
	@Setter
	protected Date date2Send = new Date();
	@Getter
	@Setter
	protected Date date2Receive = new Date();
	@Getter
	@Setter
	protected Date baseDate = new Date();
	@Getter
	@Setter
	protected String description = ""; // 必ず１６文字

	@Getter
	@Setter
	protected String gpsPosition;
	@Getter
	@Setter
	protected String duplicateOf; // 既登録ファイル名と重複しているばあいにはそのIndexを記録。重複しないときには、null
	@Getter
	protected String generatedUniqString;

	@Getter
	@Setter
	protected byte[] dataEntry = new byte[128];

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
		case 0x62:
			decodedString = "solid 3px #0cc"; // Group Monitor GM受（未）
			break;
		case 0x23:
		case 0x63:
			decodedString = "solid 3px #080"; // Group Monitor
			break;
		case 0x32:
		case 0x72:
			decodedString = "solid 3px #080"; // Group Monitor GM受（未）
			break;
		case 0x33:
		case 0x73:
			decodedString = "solid 3px #080"; // Group Monitor
			break;

		case 0x20:
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
		case 0x31:
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

	public void bytes2ByfferPoint(byte[] source, int startP, int volume) {
		for (int index = 0; index < volume; ++index) {
			dataEntry[index + startP] = source[index];
		}
	}

	public void date2DataByfferPoint(Date soure, int startP) {
		DateFormat dfm = new SimpleDateFormat("yyyyMMddHHmmss");
		String strTime = dfm.format(soure);
		for (int index = 0; index < 6; ++index) {
			byte[] d = DatatypeConverter.parseHexBinary(strTime.substring(2 + index * 2, 2 + index * 2 + 2));
			dataEntry[index + startP] = d[0];
		}
	}

	/**
	 * dateFromYaesuExp は、引数のdate情報からDate型に変換する。
	 * 
	 * 引数のdate情報は、6バイトで構成されたYaesuSystemFusion中での日時情報フォーマットのバイト列と
	 * する。これはBCD形式で、2024/01/30 02:03:04の場合、16進数の、$24 $01 $30 $02 $03 $04という
	 * 形式で記録されている。
	 * 
	 * @param date
	 * @return
	 */
	protected Date dateFromYaesuExp(byte[] date) {
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
	protected byte[] fillWithByteOf(byte[] binData, byte byte2Fill, int indexVolume) {
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

		case 0x62:
			decodedString = "#8cc"; // GM受信（未読）
			break;
		case 0x23:
		case 0x63:
			decodedString = "#484"; // GM送信（成功）
			break;
		case 0x32:
		case 0x72:
			decodedString = "#88C"; // GM受信（既読）
			break;
		case 0x33:
		case 0x73:
			decodedString = "#846"; // GM送信（失敗）
			break;

		case 0x20:
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
		case 0x31:
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

	/**
	 * LIST生成時、SORTINGをする際に利用する本データを代表する「名称」
	 * 
	 * @return
	 */
	abstract public String getRepresentativesName();

	/**
	 * LIST生成時、SORTINGをする際に利用する本データを代表する「時刻」
	 * 
	 * @return
	 */
	abstract public Date getRepresentativeTime();

	/**
	 * getSymMeaning()は、SYM(QSO～DIR.datの先頭バイト)の定義を文字列に変換する。<br>
	 * 各ビットの定義は以下の通り。<br>
	 * <br>
	 * 0x01： ０：受信、１：送信<br>
	 * 0x02： ０：通常、１：GM<br>
	 * 0x04： ０：通常、１：Wires経由<br>
	 * 0x10: エラービット（ただし、受信時エラーは不問、送信時エラーは記録）<br>
	 * 0x20： ０：無線回線でない、１：無線回線である。(もしかして、1:相手局がGM)<br>
	 * 0x40： ０：既読、１：未読（ただし、送信時は不問、受信時は記録）<br>
	 * <br>
	 * もっとも、これらのうち、同時に「１」となった場合に、各RIGでの取り扱いは異なる。おそらく、IF文で扱いを分岐しており、その分岐の順は各RIGで異なるからであると推認される。<br>
	 * たとえば、0x06の場合、FT-5Dでは、「WIRES-X経由、ＧＭ付きで、受信したデータの既読」となるが、FTM-300では、「WIRES-X経由、ＧＭ付きで、受信したデータの未読」となる。<br>
	 * また、 0x04の場合、FT-5Dでは、「無線経由、受信したデータの既読」となるが、FTM-300では、「無線経由、受信したデータの未読」となる。<br>
	 * また、
	 * 0x44の場合、FT-5Dでは、「WIRES-X経由、受信したデータの既読」となるが、FTM-300では、「WIRES-X経由、受信したデータの未読」となる。<br>
	 * また、 0x84の場合、FT-5Dでは、「--------」となるが、FTM-300では、「WIRES-X経由、受信したデータの未読」となる。<br>
	 * <br>
	 * 
	 * @return
	 */
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
		case 0x62:
			decodedString = "GM受（未）"; // Group Monitor
			break;
		case 0x23:
		case 0x63:
			decodedString = "GM送（S）"; // Group Monitor
			break;
		case 0x32:
		case 0x72:
			decodedString = "GM受（既）"; // Group Monitor
			break;
		case 0x33:
		case 0x73:
			decodedString = "GM送（F）"; // Group Monitor
			break;

		case 0x20:
		case 0x60:
			decodedString = "受信（未）";
			break;
		case 0x61:
			decodedString = "既送信（S）";
			break;
		case 0x70:
			decodedString = "受信（既）";
			break;
		case 0x31:
		case 0x71:
			decodedString = "既送信（F）";
			break;
		default:
			decodedString = "0X" + Integer.toHexString(((int) sym) & 0xff);
			break;
		}
		return decodedString;
	}

	/**
	 * isThisTransmission()は、SYM(QSO～DIR.datの先頭バイト)の定義に基づいて、
	 * （１）　送信したもの/するものの場合にはtrueを,
	 * （２）　受信したものの場合にはfalseを、
	 * それぞれ返す。
	 * 
	 * @return
	 */
	public boolean isThisTransmission() {
		boolean res;
		switch (sym) {
//送信側
		case 0x00:
		case 0x04:
		case 0x14:
		case 0x23:
		case 0x63:
		case 0x33:
		case 0x73:
		case 0x61:
		case 0x71:
			res = true;
			break;

//　受信側
		case 0x05:
		case 0x15:
		case 0x22:
		case 0x62:
		case 0x32:
		case 0x72:
		case 0x60:
		case 0x70:
		default:
			res = false;
			break;
		}
		return res;
	}

	abstract public void storeOwnData2Buffer();

	/**
	 * 引き数となるbをBCD(Binary Coded Digital)として、これを数値表示する文字列を返す。
	 * 
	 * @param b
	 * @return
	 */
	protected String stringFromTwoDigit(byte b) {
		byte[] res = new byte[2];
		byte data = (byte) (((b & 0xf0) >> 4) + 0x30);
		res[0] = data;
		data = (byte) ((b & 0xf) + 0x30);
		res[1] = data;
		return new String(res);
	}

	/**
	 * subBytesは、subStringと同様、バイト列sourceから、start番目から始まりendP番目で終わる byte[]列に複写する。
	 * もし、sourceバイト列の範囲を超えるstart/endが指定されたときには、nullを返す。
	 * 
	 * @param source
	 * @param start
	 * @param endP
	 * @return sourceからコピーされたバイト列（0オリジン）
	 */
	protected byte[] subBytes(byte[] source, int start, int endP) {
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
}
