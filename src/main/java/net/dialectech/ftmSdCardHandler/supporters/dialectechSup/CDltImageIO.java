package net.dialectech.ftmSdCardHandler.supporters.dialectechSup;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

/**
 * このクラスは、javax.imageio.ImageIO.read(〜)においてメモリーリークが発生するように見られる
 * 点から回避するため、staticメソッドの調整を行えるように一皮被せることが目的となっている。 ある意味で緊急避難なため、当面、このクラスオブジェクトは
 * singletonで機能させる。<br>
 * 
 * @author ji1bxm
 *
 */
public class CDltImageIO {

	private static CDltImageIO instance = new CDltImageIO();

	public synchronized static CDltImageIO getInstance() {
		return instance;
	}

	private CDltImageIO() {
		ImageIO.scanForPlugins();	// システムで一回動けばOKなので、ここでやっておく。
		if (imageReaderMap == null)
			imageReaderMap = new HashMap<String, ImageReader>();
		if (imageWriterMap == null)
			imageWriterMap = new HashMap<String, ImageWriter>();
	}

	private Map<String, ImageReader> imageReaderMap = null;
	private Map<String, ImageWriter> imageWriterMap = null;

	// ************ ここから下は、しばらく、読み込み関連
	//
	/**
	 * 
	 * @param st
	 * @param fileExtension
	 * @return
	 * @throws IOException
	 */
	protected synchronized ImageReader getImageReaderOf(InputStream st) throws IOException {
		ImageInputStream imageInputStream = ImageIO.createImageInputStream(st);

		Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);
		if (!readers.hasNext())
			throw new IOException();
		ImageReader readerTemplate = readers.next();
		// System.out.println(" Readerのオブジェクト・テンプレートはこれ＞" + readerTemplate.toString());
		String formatName = readerTemplate.getFormatName();
		ImageReader reader = imageReaderMap.get(formatName);
		if (reader == null) {
			imageReaderMap.put(formatName, readerTemplate);
			reader = readerTemplate;
		} else {
			readerTemplate = null;
		}
		reader.setInput(imageInputStream);
		return reader;
	}
	
	protected synchronized ImageReader getImageReaderOf(InputStream st,String formatName) throws IOException {
		ImageInputStream imageInputStream = ImageIO.createImageInputStream(st);

		Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(formatName);
		if (!readers.hasNext())
			throw new IOException();
		ImageReader readerTemplate = readers.next();
		// System.out.println(" Readerのオブジェクト・テンプレートはこれ＞" + readerTemplate.toString());
		ImageReader reader = imageReaderMap.get(formatName);
		if (reader == null) {
			imageReaderMap.put(formatName, readerTemplate);
			reader = readerTemplate;
		} else {
			readerTemplate = null;
		}
		reader.setInput(imageInputStream);
		return reader;
	}
	/**
	 * read InputStream and convert into BufferedImage
	 * 
	 * @param st
	 * @return
	 * @throws IOException
	 * @throws CDltFlowsException
	 */
	public synchronized BufferedImage readInputStream2BufferedImage(InputStream st) throws CDltFlowsException {
		// これがやりたいところだが、メモリーリークあるようなので、回避するのです。
		// BufferedImage bim = null;
		// try {
		// bim = ImageIO.read(st);
		// } catch (IOException e) {
		// // TODO 自動生成された catch ブロック e.printStackTrace();
		// }
		// return bim;

		ImageReader imageReader = null;
		try {
			imageReader = getImageReaderOf(st);
		} catch (IOException e1) {
			throw new CDltFlowsException("対象となるImage readerが見つかりません。標準の画像ではないようです。::CDltImageIO:readInputStream2BufferedImage(InputStream) ");
		}
		if (imageReader == null)
			return null;

		BufferedImage bim = null;
		try {
			bim = imageReader.read(0);
		} catch (IOException | IllegalStateException e) {
			throw new CDltFlowsException("画像変換でエラーです。CDltImageIO::readInputStream2BufferedImage()#1");
		}
		// imageReader.dispose();
		if (bim == null)
			throw new CDltFlowsException("画像変換でエラーです。CDltImageIO::readInputStream2BufferedImage()#2");
		return bim;

	}
	public synchronized BufferedImage readInputStream2BufferedImage(InputStream st,String formatName) throws CDltFlowsException {
		// これがやりたいところだが、メモリーリークあるようなので、回避するのです。
		// BufferedImage bim = null;
		// try {
		// bim = ImageIO.read(st);
		// } catch (IOException e) {
		// // TODO 自動生成された catch ブロック e.printStackTrace();
		// }
		// return bim;

		ImageReader imageReader = null;
		try {
			imageReader = getImageReaderOf(st,formatName);
		} catch (IOException e1) {
			// DO Nothing
		}
		if (imageReader == null)
			return null;

		BufferedImage bim = null;
		try {
			bim = imageReader.read(0);
		} catch (IOException e) {
			// throw new CDltFlowsExceptionすればよいので、何もしない
		}
		// imageReader.dispose();
		if (bim == null)
			throw new CDltFlowsException("画像変換でエラーです。CDltImageIO::readInputStream2BufferedImage()");
		return bim;

	}
	/**
	 * read InputStream and convert into ByteArrayOutputStream
	 * 
	 * @param st
	 * @return
	 */
	public synchronized ByteArrayOutputStream readFromInputStream2ByteArrayOutputStreamOf(InputStream st,String format)
			throws CDltFlowsException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		BufferedOutputStream os = new BufferedOutputStream(bos);
		BufferedImage bim;
		try {
			bim = readInputStream2BufferedImage(st,format);
		} catch (CDltFlowsException e1) {
			e1.printStackTrace();
			return null;
		}
		bim.flush();
		this.write(bim, format, os);
		// imageReader.dispose();
		try {
			os.flush();
			os.close();
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		if (bos.size() == 0)
			throw new CDltFlowsException("CDltImageIO::readFromInputStream2ByteArrayOutputStreamOf()のエラー");
		return bos;
	}

	// ************ ここから下は、しばらく、書き込み関連
	//
	protected synchronized void writeBase(BufferedImage bim, String formatType, ImageOutputStream imageOutputStream)
			throws CDltFlowsException {
		ImageWriter writer = imageWriterMap.get(formatType.toLowerCase());
		if (writer == null) {
			Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(formatType.toLowerCase());
			if (!writers.hasNext())
				throw new CDltFlowsException("この画像形式は処理できません。");
			ImageWriter writerTemplate = writers.next();
			writer = writerTemplate;
			imageWriterMap.put(formatType.toLowerCase(), writer);
		}
		if (writer == null)
			throw new CDltFlowsException("この画像形式は処理できません。");
		writer.setOutput(imageOutputStream);
		try {
			writer.write(bim);
		} catch (IOException e) {
			throw new CDltFlowsException(e.getMessage());
		}
	}

	protected synchronized void write(BufferedImage bim, String formatType, ImageOutputStream imageOutputStream)
			throws CDltFlowsException {
		writeBase(bim, formatType, imageOutputStream);
	}

	public synchronized void write(BufferedImage bim, String formatType, ByteArrayOutputStream st) throws CDltFlowsException {
		ImageOutputStream imageOutputStream = null;
		try {
			imageOutputStream = ImageIO.createImageOutputStream(st);
		} catch (IOException e) {
			e.printStackTrace();
			throw new CDltFlowsException("この画像形式は処理できません。");
		}

		writeBase(bim, formatType, imageOutputStream);
	}

	public synchronized void write(BufferedImage bim, String formatType, OutputStream st) throws CDltFlowsException {
		ImageOutputStream imageOutputStream = null;
		try {
			imageOutputStream = ImageIO.createImageOutputStream(st);
		} catch (IOException e) {
			e.printStackTrace();
			throw new CDltFlowsException("この画像形式は処理できません。");
		}
		writeBase(bim, formatType, imageOutputStream);
	}

	public synchronized void write(BufferedImage bim, String formatType, File newThumbnailTarget) throws CDltFlowsException {
		ImageOutputStream imageOutputStream;
		try {
			imageOutputStream = ImageIO.createImageOutputStream(newThumbnailTarget);
		} catch (IOException e) {
			e.printStackTrace();
			throw new CDltFlowsException("この画像形式は処理できません。");
		}
		writeBase(bim, formatType, imageOutputStream);
	}
}
