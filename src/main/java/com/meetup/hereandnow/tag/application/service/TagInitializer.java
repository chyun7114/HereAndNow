package com.meetup.hereandnow.tag.application.service;

import com.meetup.hereandnow.tag.domain.entity.PlaceGroup;
import com.meetup.hereandnow.tag.domain.entity.Tag;
import com.meetup.hereandnow.tag.domain.entity.TagValue;
import com.meetup.hereandnow.tag.domain.value.TagGroup;
import com.meetup.hereandnow.tag.infrastructure.repository.PlaceGroupRepository;
import com.meetup.hereandnow.tag.infrastructure.repository.TagRepository;
import com.meetup.hereandnow.tag.infrastructure.repository.TagValueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TagInitializer implements ApplicationListener<ContextRefreshedEvent> {

    private final PlaceGroupRepository placeGroupRepository;
    private final TagRepository tagRepository;
    private final TagValueRepository tagValueRepository;

    private final Map<String, TagValue> tagValueMap = new HashMap<>();

    @Override
    @Transactional
    public void onApplicationEvent(ContextRefreshedEvent event) {

        if (tagRepository.count() > 0) {
            System.out.println("태그가 이미 존재해 초기화를 건너뜁니다.");
            return;
        }

        System.out.println("태그를 생성합니다...");

        PlaceGroup ct1 = placeGroupRepository.save(PlaceGroup.builder().code("CT1").name("문화시설").build());
        PlaceGroup p03 = placeGroupRepository.save(PlaceGroup.builder().code("P03").name("공공기관").build());
        PlaceGroup at4 = placeGroupRepository.save(PlaceGroup.builder().code("AT4").name("관광명소").build());
        PlaceGroup ad5 = placeGroupRepository.save(PlaceGroup.builder().code("AD5").name("숙박").build());
        PlaceGroup fd6 = placeGroupRepository.save(PlaceGroup.builder().code("FD6").name("음식점").build());
        PlaceGroup ol7 = placeGroupRepository.save(PlaceGroup.builder().code("OL7").name("주유소, 충전소").build());
        PlaceGroup sw8 = placeGroupRepository.save(PlaceGroup.builder().code("SW8").name("지하철역").build());
        PlaceGroup pk6 = placeGroupRepository.save(PlaceGroup.builder().code("PK6").name("주차장").build());

        PlaceGroup mt1 = placeGroupRepository.save(PlaceGroup.builder().code("MT1").name("대형마트").build());
        PlaceGroup cs2 = placeGroupRepository.save(PlaceGroup.builder().code("CS2").name("편의점").build());
        PlaceGroup ps3 = placeGroupRepository.save(PlaceGroup.builder().code("PS3").name("어린이집, 유치원").build());
        PlaceGroup sc4 = placeGroupRepository.save(PlaceGroup.builder().code("SC4").name("학교 (초/중/고)").build());
        PlaceGroup ac5 = placeGroupRepository.save(PlaceGroup.builder().code("AC5").name("학원").build());
        PlaceGroup bk9 = placeGroupRepository.save(PlaceGroup.builder().code("BK9").name("은행").build());
        PlaceGroup ag2 = placeGroupRepository.save(PlaceGroup.builder().code("AG2").name("중개업소").build());
        PlaceGroup ce7 = placeGroupRepository.save(PlaceGroup.builder().code("CE7").name("카페").build());
        PlaceGroup hp8 = placeGroupRepository.save(PlaceGroup.builder().code("HP8").name("병원").build());
        PlaceGroup pm9 = placeGroupRepository.save(PlaceGroup.builder().code("PM9").name("약국").build());

        link(ct1, TagGroup.ATMOSPHERE, "분위기 맛집", "사진 찍기 좋아요", "특별한 날 오기 좋아요", "야경이 예뻐요", "뷰가 좋아요", "이색 데이트");
        link(ct1, TagGroup.FACILITY, "산책하기 좋아요", "실내 데이트하기 좋아요", "주차하기 편해요");
        link(ct1, TagGroup.ETC, "함께 체험하기 좋아요");

        link(p03, TagGroup.ATMOSPHERE, "이색 데이트", "건물이 멋져요", "사진 찍기 좋아요", "전망(뷰)이 좋아요");
        link(p03, TagGroup.FACILITY, "산책하기 좋아요", "시설이 깨끗해요", "주차하기 편해요");
        link(p03, TagGroup.ETC, "문화 행사가 있어요", "접근성이 좋아요");

        link(at4, TagGroup.ATMOSPHERE, "사진 찍기 좋아요", "로맨틱해요", "야경이 예뻐요", "뷰가 좋아요", "분위기 맛집");
        link(at4, TagGroup.FACILITY, "산책하기 좋아요", "실내 데이트", "야외 데이트", "주차하기 편해요");
        link(at4, TagGroup.ETC, "볼거리가 많아요", "함께 체험하기 좋아요", "오래 머물기 좋아요", "뚜벅이도 가기 좋아요");

        link(ad5, TagGroup.ATMOSPHERE, "로맨틱해요", "기념일에 오기 좋아요", "감성 숙소", "뷰가 좋아요", "인테리어가 예뻐요", "프라이빗해요", "조용히 쉬기 좋아요", "고급스러워요", "친절해요");
        link(ad5, TagGroup.FACILITY, "시설이 깨끗해요", "주차하기 편해요");
        link(ad5, TagGroup.ETC, "호캉스하기 좋아요", "조식 맛있어요", "룸서비스 좋아요", "위치가 좋아요");

        link(fd6, TagGroup.FOOD_PRICE, "음식이 맛있어요", "메뉴가 다양해요", "특별한 메뉴가 있어요");
        link(fd6, TagGroup.ATMOSPHERE, "분위기 맛집", "데이트하기 좋아요", "로맨틱해요", "특별한 날 오기 좋아요", "고급스러워요", "인테리어가 예뻐요", "사진 찍기 좋아요", "뷰가 좋아요");
        link(fd6, TagGroup.ETC, "친절해요", "주차하기 편해요", "위치가 좋아요");

        String[] infraFacility = {"시설이 깨끗해요", "관리가 잘 돼요", "화장실이 깨끗해요", "표지판이 잘 돼있어요", "편의시설이 있어요", "휴식 공간이 있어요"};
        String[] infraEtc = {"접근성이 좋아요", "찾기 쉬워요", "한적해요", "안전해요", "결제/정산이 편해요"};

        link(ol7, TagGroup.FACILITY, infraFacility);
        link(ol7, TagGroup.ETC, infraEtc);
        link(sw8, TagGroup.FACILITY, infraFacility);
        link(sw8, TagGroup.ETC, infraEtc);
        link(pk6, TagGroup.FACILITY, infraFacility);
        link(pk6, TagGroup.ETC, infraEtc);

        link(mt1, TagGroup.FACILITY, "주차하기 편해요", "시설이 깨끗해요", "푸드코트가 좋아요", "카트/유모차 이용이 편해요");
        link(mt1, TagGroup.ETC, "물건이 다양해요", "신선식품이 좋아요", "행사를 많이 해요", "가성비가 좋아요", "창고형 매장이에요", "접근성이 좋아요", "계산이 빨라요", "직원 응대가 좋아요", "오래 머물기 좋아요");

        link(cs2, TagGroup.FACILITY, "매장이 깨끗해요", "취식 공간이 있어요", "ATM 기기가 있어요");
        link(cs2, TagGroup.ETC, "간편식이 다양해요", "신상품이 빨리 들어와요", "행사를 많이 해요", "24시간 운영해요", "접근성이 좋아요", "찾기 쉬워요", "친절해요", "택배 이용이 편해요");

        link(ps3, TagGroup.FACILITY, "시설이 깨끗해요", "놀이터/놀이 공간이 잘 돼있어요", "안전시설이 좋아요", "급식이 잘 나와요", "등/하원이 편해요");
        link(ps3, TagGroup.ETC, "선생님이 친절해요", "아이들을 잘 돌봐줘요", "교육 프로그램이 다양해요", "상담이 편해요", "접근성이 좋아요");

        link(sc4, TagGroup.FACILITY, "시설이 깨끗해요", "운동장이 넓어요", "안전시설이 좋아요", "주차하기 편해요");
        link(sc4, TagGroup.ETC, "조용해요", "주변 환경이 깨끗해요", "접근성이 좋아요");

        link(ac5, TagGroup.FACILITY, "시설이 깨끗해요", "학습 분위기가 좋아요", "휴식 공간이 있어요", "주차/통학이 편해요");
        link(ac5, TagGroup.ETC, "수업이 알차요", "상담을 잘해줘요", "선생님이 친절해요", "관리가 잘 돼요", "접근성이 좋아요", "수강료가 합리적이에요");

        link(bk9, TagGroup.FACILITY, "시설이 깨끗해요", "대기 공간이 편해요", "ATM 이용이 편해요", "주차하기 편해요");
        link(bk9, TagGroup.ETC, "업무 처리가 빨라요", "친절해요", "상담을 잘해줘요", "접근성이 좋아요", "찾기 쉬워요");

        link(ag2, TagGroup.FACILITY, "주차하기 편해요");
        link(ag2, TagGroup.ETC, "친절해요", "설명이 꼼꼼해요", "매물을 많이 보여줘요", "전문성이 느껴져요", "좋은 매물이 많아요", "접근성이 좋아요");

        link(ce7, TagGroup.FACILITY, "주차하기 편해요", "좌석이 편해요", "실내 데이트 하기 좋아요", "야외 좌석이 있어요", "분위기 맛집", "인테리어가 예뻐요", "뷰가 좋아요");
        link(ce7, TagGroup.ETC, "커피가 맛있어요", "디저트가 맛있어요", "메뉴가 다양해요", "특별한 메뉴가 있어요", "가성비가 좋아요", "사진 찍기 좋아요", "데이트하기 좋아요", "조용해요", "공부/작업하기 좋아요", "친절해요", "위치가 좋아요", "오래 머물기 좋아요");

        link(hp8, TagGroup.FACILITY, "시설이 깨끗해요", "주차하기 편해요", "대기 공간이 편해요");
        link(hp8, TagGroup.ETC, "진료를 꼼꼼하게 봐줘요", "설명을 잘해줘요", "친절해요", "전문성이 느껴져요", "야간/주말 진료해요", "대기 시간이 짧아요", "접근성이 좋아요", "예약이 편해요");

        link(pm9, TagGroup.FACILITY, "매장이 깨끗해요", "주차하기 편해요");
        link(pm9, TagGroup.ETC, "설명을 잘해줘요", "친절해요", "약 종류가 다양해요", "야간/주말 운영해요", "접근성이 좋아요", "찾기 쉬워요");
    }

    // 같은 태그 값 중복 방지 (e.g., 접근성이 좋아요, 뷰가 좋아요 등)
    private TagValue findOrCreateTagValue(String name) {
        TagValue tagValue = tagValueMap.get(name);
        if (tagValue != null) {
            return tagValue;
        }
        TagValue existingTagValue = tagValueRepository.findByName(name).orElse(null);
        if (existingTagValue != null) {
            tagValueMap.put(name, existingTagValue);
            return existingTagValue;
        }

        TagValue newTagValue = TagValue.builder().name(name).build();
        TagValue savedTagValue = tagValueRepository.save(newTagValue);
        tagValueMap.put(name, savedTagValue);
        return savedTagValue;
    }

    private void link(PlaceGroup placeGroup, TagGroup tagGroup, String... tagNames) {
        for (String tagName : tagNames) {
            TagValue tagValue = findOrCreateTagValue(tagName);
            Tag createdTag = Tag.builder()
                    .placeGroup(placeGroup)
                    .tagValue(tagValue)
                    .tagGroup(tagGroup)
                    .build();
            tagRepository.save(createdTag);
        }
    }
}
