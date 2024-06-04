package net.dialectech.ftmSdCardHandler.supporters;

import lombok.Getter;

/**
 * CCodeConverter はYAESU SYSTEM FUSION
 * の文字コード体系とUTF-8との間を繋ぐクラスであって、概ねSTATICで使用するものとします。 YAESU SYSTEM FUSION
 * の文字コード体系とUTF-8との対応表は、ysfCodes.xlsxに記録。
 */
public class CYsfCodeConverter {

	@Getter
	private static CYsfCodeConverter instance = new CYsfCodeConverter();
	
	public CYsfCodeConverter() {
	}

	static String ysf2Utf = "ＸＸＸＸＸＸＸＸＸＸ\nＸＸ\rＸＸ"  // BACK-SLASHはESCAPEを含むので２文字多く見える。^^
			+ "ＸＸＸＸＸＸＸＸＸＸＸＸＸＸＸＸ" 
			+ " !'#$%&'()" 
			+ "*+,-./0123" 
			+ "456789:;<=" 
			+ ">?@ABCDEFG"
			+ "HIJKLMNOPQ" 
			+ "RSTUVWXYZ[" 
			+ "\\]^_`abcde" // BACK-SLASHはESCAPEを含むので１文字多く見える。^^
			+ "fghijklmno" 
			+ "pqrstuvwxy" 
			+ "z{|}~ＸＸＸＸＸ" 
			+ "ＸＸＸＸＸＸＸＸＸＸ" 
			+ "ＸＸＸＸＸＸＸＸＸＸ" 
			+ "ＸＸＸＸＸＸＸＸ￥。「" 
			+ "」、・ヲァィゥェォャ"
			+ "ュョッーアイウエオカ" 
			+ "キクケコサシスセソタ" 
			+ "チツテトナニヌネノハ" 
			+ "ヒフヘホマミムメモヤ" 
			+ "ユヨラリルレロワン”" 
			+ "°ヴガギグゲゴザジズ" 
			+ "ゼゾダヂヅデドバビブ"
			+ "ベボパピプペポＸＸＸ" 
			+ "ＸＸＸＸＸＸＸＸＸＸＸＸＸＸＸＸ"; 
	
	public String ysfByte2Utf8(byte[] sourceLetters) {
		if (sourceLetters==null || sourceLetters.length==0)
			return "" ;
		StringBuffer sb = new StringBuffer();
		for ( int index =0  ;index < sourceLetters.length ; ++index  ) {
			int code = ((int) sourceLetters[index]) & 0xff ;
			try {
				char thisCode = ysf2Utf.charAt(code);
				sb.append(thisCode);			
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}

	public byte[] utf82YsfByte(String utf8Letter) {
		if (utf8Letter==null || utf8Letter.length()==0)
			return null ;
		int fullSizeOfString = utf8Letter.length() ;
		byte[] res = new byte[fullSizeOfString];
		
		for ( int index =0  ;index < fullSizeOfString ; ++index  ) {
			int pos = ysf2Utf.indexOf(utf8Letter.charAt(index));
			if (pos==-1) {
				res[index] = (byte)'$';
			}
			else
				res[index] = (byte)pos ;
		}
		return res;
	}
}
