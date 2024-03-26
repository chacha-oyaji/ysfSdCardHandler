package net.dialectech.ftmSdCardHandler.supporters.dialectechSup;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import lombok.Getter;
import lombok.Setter;

public class CDltSimpleFileHandler {
	 
	  @Getter
	  protected File[] file;
	  @Getter
	  @Setter
	  protected String[] contentType;
	  @Getter
	  @Setter
	  protected String[] fileName;
	 
	  public void setFile(MultipartFile[] multipartFile) {
	    MultipartFile[] springTempFiles = multipartFile;
	 
	    if (springTempFiles == null || springTempFiles.length == 0)
	      return;
	    boolean atLeastOneFile = false;
	    for (MultipartFile f : multipartFile) {
	      if (f.getOriginalFilename().equals(""))
	        continue;
	      atLeastOneFile = true;
	      break;
	    }
	    if (atLeastOneFile == false)
	      return;
	    file = new File[springTempFiles.length];
	    contentType = new String[springTempFiles.length];
	    fileName = new String[springTempFiles.length];
	 
	    for (int index = 0; index < springTempFiles.length; ++index) {
	      MultipartFile target = springTempFiles[index];
	      contentType[index] = target.getContentType();
	      fileName[index] = target.getOriginalFilename();
	      file[index] = new File(
	          "FTMSD" + UUID.randomUUID().toString().replaceAll("-", ""));
	      try {
	        target.transferTo(file[index]);
	      } catch (IllegalStateException e) {
	        // TODO 自動生成された catch ブロック
	        e.printStackTrace();
	      } catch (IOException e) {
	        // TODO 自動生成された catch ブロック
	        e.printStackTrace();
	      }
	    }
	  }
}
