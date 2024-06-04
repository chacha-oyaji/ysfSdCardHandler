package net.dialectech.ftmSdCardHandler.actions.suppoters;

import java.io.ByteArrayOutputStream;
import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Locale;

import org.springframework.web.servlet.ModelAndView;

import lombok.Getter;
import lombok.Setter;
import net.dialectech.ftmSdCardHandler.data.supporters.CDataEntry;
import net.dialectech.ftmSdCardHandler.supporters.CYsfSdCHandlerProperties;
import net.dialectech.ftmSdCardHandler.supporters.dialectechSup.CDltFileUtilities;
import net.dialectech.ftmSdCardHandler.supporters.dialectechSup.CDltFlowsException;
import net.dialectech.ftmSdCardHandler.supporters.dialectechSup.CDltImageIO;
import net.dialectech.ftmSdCardHandler.supporters.dialectechSup.CDltSpringFileStream;
import net.dialectech.ftmSdCardHandler.supporters.fileSystem.CYsfFileSystem;
import net.dialectech.ftmSdCardHandler.supporters.fileSystem.CYsfFileSystemCorePart;

public abstract class CHandlerActionFundamental {

	@Getter
	@Setter
	protected CDltSpringFileStream springFileStream;

	/**
	 * ModelAndViewであるmavに幾つかの設定値を載せて、リクエストのレスポンスメソッドから抜け出る準備をする。
	 * 
	 * @param mav
	 * @param errorMessageList
	 * @param branchName
	 * @param fs
	 * @param pageTitle
	 * @param prop
	 * @param viewName
	 */
	protected void setAllParameters4Mav(ModelAndView mav, LinkedList<String> errorMessageList, String branchName,
			CYsfFileSystemCorePart fs, String pageTitle, CYsfSdCHandlerProperties prop, String viewName) {
		mav.addObject("errorMessageList", errorMessageList);
		mav.addObject("branchDirName", branchName);
		mav.addObject("fs", fs);
		mav.addObject("title", pageTitle);
		mav.addObject("prop", prop);
		mav.setViewName(viewName);
	}

	protected CDltSpringFileStream forceStreamAsImage(String imageName) {
		CDltSpringFileStream springFileStream;
		try {
			CDltImageIO imageHandler = CDltImageIO.getInstance();
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			String imageExtention = CDltFileUtilities.extentionOfPath(imageName);
			bos = imageHandler.readFromInputStream2ByteArrayOutputStreamOf(
					this.getClass().getResourceAsStream("/net/dialectech/commons/dltFlows/images/" + imageName),
					imageExtention);
			springFileStream = new CDltSpringFileStream(bos, "readError." + imageExtention, "image/jpeg");
		} catch (CDltFlowsException e1) {
			springFileStream = null;
		}
		return springFileStream;
	}

	protected <DT extends CDataEntry> ModelAndView createContentsList(ModelAndView mav, int startFrom,
			String sortingOrder, LinkedList<DT> contentsList, LinkedList<String> errorMessageList) {
		CYsfFileSystem fs = CYsfFileSystem.getInstance();
		CYsfSdCHandlerProperties prop = CYsfSdCHandlerProperties.getInstance();

		startFrom = startFrom == 0 ? 0 : startFrom - 1; // LinkedListは0オリジン。

		// 一応、保険程度に。
		if (sortingOrder==null || sortingOrder.isBlank() ) {
			sortingOrder = "timeReverseOrder";
		} 

		Comparator<DT> comparator;
		// この程度なので、Builder Patternとか、Strategy Patternとかは使わなくてよいだろう。
		switch (sortingOrder) {
		case "timeReverseOrder":
			comparator = new Comparator<DT>() {
				@Override
				public int compare(DT o1, DT o2) {
					if (o1.getRepresentativeTime().before(o2.getRepresentativeTime())) {
						return -1;
					} else if (o1.getRepresentativeTime().after(o2.getRepresentativeTime())) {
						return 1;
					} else {
						return 0;
					}
				}
			};
			break;
		case "timeOrder":
			comparator = new Comparator<DT>() {
				@Override
				public int compare(DT o1, DT o2) {
					if (o1.getRepresentativeTime().after(o2.getRepresentativeTime())) {
						return -1;
					} else if (o1.getRepresentativeTime().before(o2.getRepresentativeTime())) {
						return 1;
					} else {
						return 0;
					}
				}
			};
			break;
		case "nameReverseOrder":
			comparator = new Comparator<DT>() {
				@Override
				public int compare(DT o1, DT o2) {
					Collator col = Collator.getInstance(Locale.JAPAN);
					return -col.compare(o1.getRepresentativesName(), o2.getRepresentativesName());
				}
			};
			break;
		case "nameOrder":
		default:
			comparator = new Comparator<DT>() {
				@Override
				public int compare(DT o1, DT o2) {
					Collator col = Collator.getInstance(Locale.JAPAN);
					return col.compare(o1.getRepresentativesName(), o2.getRepresentativesName());
				}
			};
			break;
		}

		// 結果用のLinkedListを生成
		// ここで、結果格納用のLISTを生成しておかないと、もとのLinkedListの方がSORTINGされてしまい、SD-CARDの方の全ファイルをイチイチ書き換えなければならなくなる。		
		LinkedList<DT> resultContentsList = createResultContentsListSeed() ;
		for ( DT element :  contentsList) {
			resultContentsList.add(element);
		}
		Collections.sort(resultContentsList, comparator);

		// 要素削除のためにリスト現在値より少ないファイル数になったときには、ひとつ前の値にする。
		if (resultContentsList.size() > 0) {
			if (startFrom != 1 && startFrom >= resultContentsList.size()) {
				int block = (startFrom - 1) / prop.getListStepSize();
				startFrom = prop.getListStepSize() * block;
			}
		}

		// JUMP用リストを生成
		int listUntil = startFrom + prop.getListStepSize();
		if (listUntil >= resultContentsList.size())
			listUntil = resultContentsList.size();

		LinkedList<Integer> targetPosList = new LinkedList<Integer>();
		for (int briefStop = 0; briefStop < resultContentsList.size(); briefStop += prop.getListStepSize())
			targetPosList.add(briefStop + 1); // 表示は１オリジン。
		resultContentsList = new LinkedList<DT>(resultContentsList.subList(startFrom, listUntil));

		// このメソッドで特有となる各データを更に記録。
		mav.addObject("presentPos", startFrom + 1); // 今表示しようとしている音声ファイルリストのリスト先頭ID候補の値。
		mav.addObject("targetPosList", targetPosList); // リスト先頭ID候補を記録したリスト。
		mav.addObject("contentsList", resultContentsList);
		return mav;
	}
	
	protected abstract  <DT extends CDataEntry> LinkedList<DT> createResultContentsListSeed() ;
}
