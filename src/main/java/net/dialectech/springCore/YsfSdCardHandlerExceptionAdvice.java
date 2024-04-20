package net.dialectech.springCore;

import java.util.LinkedList;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class YsfSdCardHandlerExceptionAdvice {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ModelAndView handleMaxSizeException(MaxUploadSizeExceededException e) {
        ModelAndView mav = new ModelAndView();
        LinkedList<String> errorMessageList = new LinkedList<String>() ;
        errorMessageList.add("アップロードする個々のファイルで制限値を超えたものがあります。");
        errorMessageList.add("=> 50MB以下の画像ファイルをアップロードしてください。");
        mav.addObject("errorMessageList",errorMessageList);
        mav.setViewName("pages/divErrorOnly");
        return mav;
    }

}
