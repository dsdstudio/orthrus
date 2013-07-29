package com.serivires.orthrus.downloader;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.http.client.utils.URIBuilder;

import com.serivires.orthrus.commons.HttpClientUtils;
import com.serivires.orthrus.model.Webtoon;
import com.serivires.orthrus.parse.WebtoonParser;

public class WebtoonDownloader {
	private final WebtoonParser webtoonPage = new WebtoonParser();

	private static final String NAVER_WEBTOON_SCHEME = "http";
	private static final String NAVER_WEBTOON_HOST = "comic.naver.com";

	/**
	 * 실제 웹툰이 보여지는 페이지 주소를 반환합니다.
	 * 
	 * @param titleId
	 * @param no
	 * @return
	 */
	public URI buildWebtoonDetailPageURI(String titleId, String no) {
		URIBuilder uriBuilder = new URIBuilder();
		uriBuilder.setScheme(NAVER_WEBTOON_SCHEME).setHost(NAVER_WEBTOON_HOST).setPath("/webtoon/detail.nhn");
		uriBuilder.setParameter("titleId", titleId).setParameter("no", no);

		URI uri = null;
		try {
			uri = uriBuilder.build();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		return uri;
	}

	/**
	 * 웹툰 검색페이지 주소를 반환합니다.
	 * 
	 * @param title
	 * @return
	 */
	protected URI buildWebtoonSearchPageURI(String title) {
		URIBuilder uriBuilder = new URIBuilder();
		uriBuilder.setScheme(NAVER_WEBTOON_SCHEME).setHost(NAVER_WEBTOON_HOST).setPath("/search.nhn");
		uriBuilder.setParameter("m", "webtoon").setParameter("type", "title").setParameter("keyword", title);

		URI uri = null;
		try {
			uri = uriBuilder.build();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		return uri;
	}

	public void autoSave(String title) throws Exception {
		Webtoon webToon = getWebToonInfo(title);
		if (Webtoon.emptyObject.equals(webToon)) {
			return;
		}

		String prePath = File.separator + webToon.getTitle() + File.separator;
		int downloadCount = 0;
		for (int i = 1; i <= webToon.getLastPage(); i++) {
			URI uri = buildWebtoonDetailPageURI(webToon.getId(), i + "");
			downloadCount += saveByPage(uri, prePath + i + "화" + File.separator);
		}

		System.out.println("총 " + downloadCount + "개의파일이 다운로드 되었습니다.");
	}

	/**
	 * 한 페이지 내에 있는 유효한 이미지 파일을 저장합니다.
	 * 
	 * @param uri
	 * @param path
	 * @return
	 * @throws Exception
	 * @throws
	 */
	public int saveByPage(URI uri, String path) throws Exception {
		String html = HttpClientUtils.readHtmlPage(uri);
		List<String> imageUrlList = webtoonPage.selectImgByHtmlPage(html);
		imageFileSave(imageUrlList, path);

		return imageUrlList.size();
	}

	/**
	 * 이미지파일을 다운받아 저장합니다.
	 * 
	 * @param imageUrlList
	 * @param path
	 */
	public void imageFileSave(List<String> imageUrlList, String path) {
		for (String url : imageUrlList) {
			String fileName = getFileNameByURL(url);
			File file = new File(path, fileName);

			try {
				FileUtils.copyURLToFile(new URL(url), file);
				System.out.println(file.getAbsolutePath());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * URL에서 파일 이름을 반환합니다.
	 * 
	 * @param fileUrl
	 * @return
	 */
	private String getFileNameByURL(String fileUrl) {
		String[] depths = fileUrl.split("/");
		return depths[depths.length - 1];
	}

	/**
	 * 제목과 일치하는 웹툰 정보를 반환합니다.
	 * 
	 * @param title
	 * @return
	 * @throws Exception
	 */
	public Webtoon getWebToonInfo(String title) throws Exception {
		URI uri = buildWebtoonSearchPageURI(title);
		String htmlString = HttpClientUtils.readHtmlPage(uri);

		Webtoon webtoon = webtoonPage.getWebToonInfo(htmlString);
		if (Webtoon.emptyObject.equals(webtoon)) {
			System.out.println("검색 결과가 없습니다.");
			return Webtoon.emptyObject;
		}

		int lastPage = getLastPageNumber(webtoon.getId());
		if (lastPage <= 0) {
			System.out.println("접속이 차단되었습니다.");
			return Webtoon.emptyObject;
		}
		webtoon.setLastPage(lastPage);

		return webtoon;
	}

	/**
	 * 웹툰의 마지막화 번호를 반환합니다.
	 * 
	 * @param titleid
	 * @return
	 * @throws Exception
	 */
	public int getLastPageNumber(String titleid) throws Exception {
		URI uri = buildWebtoonDetailPageURI(titleid, 0 + "");
		String htmlString = HttpClientUtils.readHtmlPage(uri);
		return webtoonPage.getLastPageNumber(htmlString);
	}
}
