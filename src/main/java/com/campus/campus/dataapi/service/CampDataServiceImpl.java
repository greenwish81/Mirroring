package com.campus.campus.dataapi.service;

import com.campus.campus.dataapi.dto.LoadDataResponseDto;
import com.campus.campus.dataapi.dto.SaveCampRequestDto;
import com.campus.campus.dataapi.entity.CampBaseInfo;
import com.campus.campus.dataapi.exception.DataLoadFailedException;
import com.campus.campus.dataapi.exception.WrongURLException;
import com.campus.campus.dataapi.repository.SaveCampRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampDataServiceImpl implements CampDataService {

    public final SaveCampRepository campRepository;

    public LoadDataResponseDto loadAndSaveFromApiWithJson() {
        try {
            int page = 1;
            int numOfRows = 10;
            String apiUrl = "https://apis.data.go.kr/B551011/GoCamping/basedList?MobileOS=WIN&MobileApp=TadakTadak&_type=json";
            String apikey = "eqzJCAvqSy0VmYJ77GE51mGpqo4PFub0OrAs%2Fhw1S0COTrvYFwPULfG4K%2Bixr0uYch4uw3ciXr4PhRI%2F%2FdDQ%2FQ%3D%3D";

            int totalCnt = 0;
            int savedDataAmount = 0;

            boolean isLastPage = false;

            while (!isLastPage) {
                URL url = new URL(apiUrl + "&serviceKey=" + apikey + "&pageNo=" + page + "&numOfRows=" + numOfRows);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty("content-type", "application/json");

                BufferedReader bf = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
                StringBuffer result = new StringBuffer();
                result.append(bf.readLine());
                JSONParser jsonParser = new JSONParser();
                JSONObject jsonObject = (JSONObject) jsonParser.parse(result.toString());

                checkURLKey(jsonObject);

                JSONObject response = (JSONObject) jsonObject.get("response");
                JSONObject body = (JSONObject) response.get("body");
                JSONObject item = (JSONObject) body.get("items");

                JSONArray jsonArray = (JSONArray) item.get("item");

                for (int i = 0; i < jsonArray.size(); i++) {
                    JSONObject object = (JSONObject) jsonArray.get(i);
                    Long contentId = Long.parseLong(object.get("contentId").toString());
                    String campingName = (String) object.get("facltNm");
                    String summaryIntro = (String) object.get("lineIntro");
                    String intro = (String) object.get("intro");
                    String category = (String) object.get("induty");
                    String doNm = (String) object.get("doNm");
                    String sigunguNm = (String) object.get("sigunguNm");
                    String addr = (String) object.get("addr1");


                    SaveCampRequestDto saveCampRequestDto = new SaveCampRequestDto(contentId, campingName, summaryIntro
                            , intro, category, doNm, sigunguNm, addr);

                    CampBaseInfo campBaseInfo = CampBaseInfo.builder()
                            .contentId(contentId)
                            .campingName(campingName)
                            .summaryIntro(summaryIntro)
                            .intro(intro)
                            .category(category)
                            .doNm(doNm)
                            .sigunguNm(sigunguNm)
                            .addr(addr)
                            .build();

                    campRepository.save(campBaseInfo);
                }
                totalCnt = ((Number) body.get("totalCount")).intValue();
                // 현재 페이지가 마지막 페이지인지 확인합니다.
                if (page * numOfRows >= totalCnt) {
                    isLastPage = true;
                } else {
                    page++; // 다음 페이지로 이동합니다.
                }
            }
            savedDataAmount = ((Number) campRepository.count()).intValue(); // 현재 db에 저장된 데이터 갯수

            log.info("Page: {}", page);
            log.info("totalCnt: {} & savedDataAmount : {}", totalCnt, savedDataAmount);
            return new LoadDataResponseDto(totalCnt == savedDataAmount);

        } catch (Exception e) {
            throw new DataLoadFailedException(e);
        }
    }

    private void checkURLKey(JSONObject jsonObject) {
        if (jsonObject.get("code") != null && (int) jsonObject.get("code") == -4) {
            throw new WrongURLException();
        }
    }


}