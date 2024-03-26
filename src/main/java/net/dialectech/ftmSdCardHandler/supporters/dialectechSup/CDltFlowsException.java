package net.dialectech.ftmSdCardHandler.supporters.dialectechSup;


public class CDltFlowsException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7445665966441416905L;

	private String coreMessage;
	private String fullMessage;

	public CDltFlowsException() {
		sendThisMessage("CDltFlowsException()を使った一般エラー発生です。");
	}

	public CDltFlowsException(String error_explanation) {
		fullMessage = coreMessage = error_explanation;
	}

	public CDltFlowsException(String error_explanation, boolean reportMail) {
		if (reportMail)
			sendThisMessage(fullMessage = coreMessage = error_explanation);
	}

	public CDltFlowsException(String baseMessage, Exception e) {
		coreMessage = "ライブラリ・エラー検出：\n" + e.toString();
		fullMessage = coreMessage + "\n\nエラー時のスタック-----------\n\n";
		StackTraceElement[] st = e.getStackTrace();
		for (int i = 0; i < st.length; ++i) {
			fullMessage += "\n" + st[i].toString();
		}
		sendThisMessage(fullMessage);
	}

	public String getCoreErrorMessage() {
		return coreMessage;
	}

	public String getErrorMessage() {
		return fullMessage;
	}

	public void reportThisError(String title, String message) {
	}

	private void sendThisMessage(String error_explanation) {
		System.out.println(error_explanation);
		reportThisError("CDltFlowsException", error_explanation);
	}
}
