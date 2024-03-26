package net.dialectech.springCore;

import java.io.IOException;

import javax.swing.JOptionPane;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import net.dialectech.ftmSdCardHandler.supporters.CYsfSdCHandlerProperties;

@SpringBootApplication
@ComponentScan("net.dialectech")
public class YsfSdCardHandlerApplication {

	public static void main(String[] args) {
		SpringApplication.run(YsfSdCardHandlerApplication.class, args);
		ProcessBuilder pb = null;
		// 最初にデフォルト値を設定
		CYsfSdCHandlerProperties props = CYsfSdCHandlerProperties.getInstance();
		try {
			props.loadAll();
		} catch (IOException e) {
			try {
				// 存在しなかったってことだから、新たに生成するためのsave。
				props.saveAll();
			} 
			catch (IOException e2) {
				System.setProperty("java.awt.headless", "false");
				JOptionPane.showMessageDialog(null, "プロパティファイルの生成処理で異常がありました。\n" + props.getPropertyFileName());
			}
		}
		
		if (args.length == 0) {
			pb = new ProcessBuilder("\"" + props.getBrowserPath() + "\" \"" + props.getFirstPage()+ "\"");
		} else {

			if ((args.length != 2) && (args.length != 3)) {
				System.setProperty("java.awt.headless", "false");
				JOptionPane.showMessageDialog(null, "引数部分の指定を見直してください。");
			} else {
				// System.out.println(">>> " + args[0]);
				pb = new ProcessBuilder("\"" + args[0] + "\" \"/localhost:48649/execute/base\"");
				props.setImageMagickPath( args[1] );
				if (args.length == 3) {
					if (args[2].equals("-d")) {
						props.setDebugMode(true);
					} else {
						System.setProperty("java.awt.headless", "false");
						JOptionPane.showMessageDialog(null, "利用できないパラメータが指示されました。  \"" + args[2] + "\"");
						props.setDebugMode(false);
					}
				} else {
					props.setDebugMode(false);
				}
			}
		}
		Process process = null;
		try {
			process = pb.start();
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
	}

}
