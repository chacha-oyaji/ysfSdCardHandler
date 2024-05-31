package net.dialectech.ftmSdCardHandler.supporters.fileSystem;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import lombok.Getter;
import lombok.Setter;
import net.dialectech.ftmSdCardHandler.supporters.CConst;
import net.dialectech.ftmSdCardHandler.supporters.CYsfSdCHandlerProperties;
import net.dialectech.ftmSdCardHandler.supporters.dialectechSup.CDltFlowsException;
import net.dialectech.ftmSdCardHandler.supporters.dialectechSup.CDltImageIO;

public class CYsfFileSystemCorePart {

	protected CYsfSdCHandlerProperties prop = CYsfSdCHandlerProperties.getInstance();

	protected byte[] generalMgr; // QSOMGRファイル内のデータをそのまま保持
	// WAVMGRについては、本システムでは常時参照の必要がないので、書き込みの際のみアクセスする。
	
	// PICT関連のデータ列
	@Getter
	@Setter
	protected LinkedList<CImageEntry> pctDirListWithDisplayOrder;
	@Getter
	@Setter
	protected LinkedList<CImageEntry> pctDirList;
	File photoFolder = null;
	@Setter
	@Getter
	protected int presentMaxNumberInPictFileName = 0;
	@Getter
	@Setter
	File[] includedFileList;

	public CYsfFileSystemCorePart() {
		super();
		prop = CYsfSdCHandlerProperties.getInstance();
		System.out.println("coreBase Constructor: " + this.toString());
	}

	public String analyzeQRCode(String partBim) {
		Hashtable<DecodeHintType, Object> decodeHints = new Hashtable<DecodeHintType, Object>();
		decodeHints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
		BufferedImage image;
		String context;
		try (FileInputStream fileInputStream = new FileInputStream(partBim);) {
			CDltImageIO imageHandler = CDltImageIO.getInstance();
			image = imageHandler.readInputStream2BufferedImage(fileInputStream);
			// image = ImageIO.read(sourceFile[0]);
			LuminanceSource source = new BufferedImageLuminanceSource(image);
			BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
			Reader reader = new MultiFormatReader();
			Result decodeResult = reader.decode(bitmap);
			context = decodeResult.getText();
		} catch (FormatException | IOException | NotFoundException | ChecksumException | CDltFlowsException e) {
			// e.printStackTrace();
			return null;
		}
		return context;
	}

	public void changeDescription(int targetDataId, String description2Change) {
		for (CImageEntry d : pctDirList) {
			if (d.getDataId() == targetDataId) {
				d.setDescription(description2Change);
				break;
			}
		}
	}

	public String loadFromTargetBankDirFiles(String sourceDirectory) {
		presentMaxNumberInPictFileName = 0;

		File qsoPctDir;
		qsoPctDir = new File(sourceDirectory + CConst.STR_QSOPCTDIR);
		if (!qsoPctDir.exists())
			return "loadFromTargetBankDirFiles::QSOPCTDIR.datが見つかりません。";

		File qsoPctFat;
		qsoPctFat = new File(sourceDirectory + CConst.STR_QSOPCTFAT);
		if (!qsoPctFat.exists())
			return "loadFromTargetBankDirFiles::QSOPCTFAT.datが見つかりません";

		try (FileInputStream fisPctDir = new FileInputStream(qsoPctDir);
				FileInputStream fisPctFat = new FileInputStream(qsoPctFat)) {

			pctDirListWithDisplayOrder = new LinkedList<CImageEntry>();
			pctDirList = new LinkedList<CImageEntry>();

			// まずはfatを取り込んで、チェッカーを生成する。
			// fat の容量(バイト量でなく、情報数で表している。)から全体のDIRエントリ数を把握してみる。

			int dirEntryVolume = (int) ((qsoPctFat.length() & 0xffff) / CConst.BYTESIZE_PER_FAT); // FATでは１エントリあたり「４」バイトで表現される。
			LinkedHashMap<Integer, Boolean> fatCheckMap = new LinkedHashMap<Integer, Boolean>(); // キーはディレクトリエントリの開始番地（絶対番地）
			for (int index = 0; index < dirEntryVolume; ++index) {
				byte[] pictFatImage = fisPctFat.readNBytes(CConst.BYTESIZE_PER_FAT);
				if (pictFatImage.length == 0)
					break;

				int startAddress = (int) ((pictFatImage[1] << 16) & 0x00ff0000)
						+ (int) ((pictFatImage[2] << 8) & 0xff00) + (int) (pictFatImage[3] & 0xff);
				if (startAddress == 0xffffff) {
					if (fatCheckMap.get(index * CConst.BYTESIZE_PER_DIR_ENTRY) == null)
						fatCheckMap.put(index * CConst.BYTESIZE_PER_DIR_ENTRY, false);
					else
						System.out.println("FAT CORRUPSED? Duplicate location data.");
				} else
					fatCheckMap.put(startAddress, true);
			}

			// 次にdirを読みこむ。特にFATとの整合性は求めずに、QSOPCTDIRのみで構成する。
			for (int index = 0;; ++index) {
				byte[] pictDirectories = fisPctDir.readNBytes(CConst.BYTESIZE_PER_DIR_ENTRY);
				if (pictDirectories.length == 0) {
					// データをすいだせなかったら、もうこれ以上の情報はなし。
					break;
				}
				// まずは管理データを生成。
				CImageEntry ie = new CImageEntry(pictDirectories, index);
				// 生成されたデータが新規ファイルで、ファイル名中の数値が従来分より大きかったら、このファイルシステム中での最大値として記録される。
				if (ie.getNumberPartOfFileName() > presentMaxNumberInPictFileName) {
					presentMaxNumberInPictFileName = ie.getNumberPartOfFileName();
				}

				int dirPositionAddress = index * CConst.BYTESIZE_PER_DIR_ENTRY;
				Boolean dataExist = fatCheckMap.get(dirPositionAddress);
				if (dataExist == null) {
					ie.setAbsDirPos(-1); // ファイルがないなら、絶対番地を-1に設定する。
				} else {
					ie.setAbsDirPos(index * CConst.BYTESIZE_PER_DIR_ENTRY);
					if (dataExist) {
						ie.setActive(true);
					} else {
						ie.setActive(false);
					}
				}
				pctDirListWithDisplayOrder.addFirst(ie);
				pctDirList.add(ie);
			}

		} catch (IOException e) {
			// TODO
			e.printStackTrace();
		}

		// PHOTOディレクトリ内にあるファイル群とｐｃｒDirListとの間の整合性チェック。
		File[] includedFileList;

		includedFileList = new File(sourceDirectory).listFiles();
		if (includedFileList != null) {
			for (CImageEntry pctDir : pctDirList) {
				pctDir.setRealFileExists(false);
				for (File f : includedFileList) {
					if (f.getName().equals(pctDir.getFileCoreName())) {
						pctDir.setRealFileExists(true);
						break;
					}
				}
			}
		}

		// pctDirListWithOrderは時間でソーティングするが、pctDirListの方は配置の通りにするので、弄らない。

		// 次に、二重管理チェック。()

		reNumberAndPrepareForPictureDisplay();
		return null;
	}

	private void markDuplicateFileHandling() {
		LinkedHashMap<String, CImageEntry> realFileName2DuplicationCheck = new LinkedHashMap<String, CImageEntry>(); // ファイル名、pctDirEntry実体
		CImageEntry matchedData;
		for (CImageEntry ie : pctDirList) {
			if (ie.getDataId() == -1)
				continue;
			ie.setDuplicateOf(null);
			if ((matchedData = realFileName2DuplicationCheck.get(ie.getFileCoreName())) != null) {
				ie.setDuplicateOf(String.valueOf(matchedData.getDataId()));
			} else {
				realFileName2DuplicationCheck.put(ie.getFileCoreName(), ie);
			}
		}

	}

	/**
	 * reNumbering
	 * reNumberingは、pctDirListから、管理対象を現存するファイルに対するもののみを抽出し、pctDirListWithDisplayOrderへの転写を行う。
	 * 
	 * 但し、転写の際には、新しいファイルを前、古いファイルが後になるように、逆順ソートを行う。また、名称でソートするため、同じファイル名で管理しているものは
	 * まとまって続くようになる。よって、たとえば、ひとつの画像をリグから送信すると、そのファイル名と同名の画像がエントリに記録されるので、
	 * 
	 */
	public void reNumberAndPrepareForPictureDisplay() {
		// この時点で、pctDirListからpctDirListWithDisplayOrderにデータを移動させた上で、ソーティングしてから管理ＩＤを振ることにする。
		pctDirListWithDisplayOrder = new LinkedList<CImageEntry>();
		for (CImageEntry elem : pctDirList) {
			if (elem.isRealFileExists())
				pctDirListWithDisplayOrder.addFirst(elem);
		}

		Collections.sort(pctDirListWithDisplayOrder, new Comparator<CImageEntry>() {

			/**
			 * ファイル名のうち、数値部分で比較する。ファイル名自体で比較しなかったのは、異なるリグ間で同じSD-CARDを使いまわしたときでも
			 * 混乱しないようにするため。
			 */
			@Override
			public int compare(CImageEntry o1, CImageEntry o2) { // ファイル名の若いものが後
				// int fileNameComparizon =
				// o1.getFileCoreName().compareTo(o2.getFileCoreName());
				int fileNameComparizon = 0;
				if (o1.getNumberPartOfFileName() > o2.getNumberPartOfFileName())
					return -1;
				if (o1.getNumberPartOfFileName() < o2.getNumberPartOfFileName())
					return 1;

				if (fileNameComparizon == 0) { // ファイル名同一ならば、受信時が早いものが後
					if (o1.getDate2Receive().after(o2.getDate2Receive()))
						return -1;
					else if (o1.getDate2Receive().before(o2.getDate2Receive()))
						return 1;
					else
						return 0;
				} else
					return -(fileNameComparizon);
			}
		});

		// Browserから画像を特定するための管理番号を再付与する。
		int index = 0;
		for (CImageEntry elem : pctDirListWithDisplayOrder) {
			elem.setDataId(++index);
			elem.storeOwnData2Buffer();
		}
		// 重複管理のマーキングをする。
		markDuplicateFileHandling();
	}

	public void removeThisImage(CImageEntry data) {
		for (CImageEntry d : pctDirList) {
			if (d == data) {
				pctDirList.remove(data);
				break;
			}
		}
		for (CImageEntry d : pctDirListWithDisplayOrder) {
			if (d == data) {
				pctDirListWithDisplayOrder.remove(data);
				break;
			}
		}
	}
}