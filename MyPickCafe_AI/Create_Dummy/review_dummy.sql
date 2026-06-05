-- Review + ReviewTag Dummy Data

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '쏘스윗 홍대'), (SELECT member_id FROM member WHERE email = 'user00050@test.com'), '당일케이크 좋아서 항상 시켜먹는 곳인데, 케이크 받으러 갔더니 문 닫혀있더라고요. 제가 예약시간보다 조금 늦게 간 건 맞지만, 그렇다면 사전에 연락을 주셨어야 한다고 봐요 어쨌든 저는 결제를 이미 완료한 상태인데, 문 앞에서 15분을 기다렸어요. 두드려도 안나오더라고요 여기 전화통화 연결도 안되잖아요 겨우겨우 아예 쏘스윗 사장님이랑 연결돼서 문 열고 받아왔네요 알고보니 무인픽업 방법이 따로 있었는데 직원이 안내를 안한거래요. 제 시간이며 돈이며 다 어떻게 할 뻔 했나요? 제가 사장님 번호를 알아내지 못했더라면요. 직원분들 항상 불친절하다고 생각했는데, 손님 응대 잘 해주셨으면 좋겠어요 그리고 늦었을 때 어떻게 해야하는지도 업장 측에서 연락 줘야한다고 봐요.', 0, 1, 'BAD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'CAKE');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '쏘스윗 홍대'), (SELECT member_id FROM member WHERE email = 'user00078@test.com'), '동생 생일이라 급하게 당일 방문했는데 바로 당일에도 픽업할수 있어 진짜 리뷰데로 p가 방문한다면 고민없이 여기로 선택하라 말하고싶어요 급한대로 준비한거라 맛은 기대없었는데 빵 시트 진짜 촉촉하고 크림치즈 맛있었고 ㅠㅠㅠ 감동 그자체였어요 리뷰 곰돌이 초도 너무 귀엽고 덕분에 생일 잘 보냈네요 자주자주 이용할게요 감사합니다 사장님 🩷🩷🤍🤍🤍💞💞', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'BAKERY');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'CAKE');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '쏘스윗 홍대'), (SELECT member_id FROM member WHERE email = 'user00015@test.com'), '친절하시고 요청 사항 잘 반영해 이쁜 케잌 만들어주셨어요!! 받는 친구도 너무 행복해 해서 주는 저도 뿌듯했습니다!!:) 케잌 이쁘게 만들어 주셔서 감사하고 다음에 또 이용할께요!!', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'CAKE');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '쏘스윗 홍대'), (SELECT member_id FROM member WHERE email = 'user00040@test.com'), '친구 생파겸 이벤트로 준비해 갔는데 친구가 조아라 해줬어요 여자넷이서 디저트로 먹기에 딱 좋은 사이즈이구요~ 치즈케이크맛이 나는것이 신기했습니다 가성비 좋은거 같아요', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'CAKE');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '쏘스윗 홍대'), (SELECT member_id FROM member WHERE email = 'user00021@test.com'), '당일제작 가능한 쏘스윗❣️ 아무런 예약 없이 당일에 방문해서 원하는 문구 써주시고 바로 픽업 가능해서 너무 좋네용!!! 저 같이 즉흥적인 P들에게 너무 필요한 곳입니다ㅠㅠㅠㅠ포장도 너무 예쁘게 해주셔서 감사합니다ㅠㅠ다음에 또 이용하겠습니다!!', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'BAKERY');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'CAKE');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '쏘스윗 홍대'), (SELECT member_id FROM member WHERE email = 'user00002@test.com'), '친구생일이라 급하게 축하해준다고 찾았는데 당일예약이 있어서 너무 좋았구요! 맛은 제가 안먹어서 모르겠지만 초랑 케이크 너무 귀여워서 친구가 너무 좋아해줬어요❤️', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'CAKE');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '쏘스윗 홍대'), (SELECT member_id FROM member WHERE email = 'user00027@test.com'), '친구 청첩장모임에 들고갈 케이크를 찾다가 발견했어요 전날에 예약하려는데 다른 곳은 전날, 당일예약 된다고 해놓고 알고보니 제약이 많아서 어려운 경우가 많더라구요. 그 중 쏘스윗을 발견하게 되어 케이크를 픽업했는데 종류도 많고 12시 30분 이후 가장 빠른 시간에 픽업이 가능해서 좋았어요. 케이크도 예쁘고 맛도 좋아서 친구가 좋아했습니다', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'BAKERY');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'CAKE');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '쏘스윗 홍대'), (SELECT member_id FROM member WHERE email = 'user00054@test.com'), '도시락 케이크 예약했어요! 이쁘고 친구도 좋아해요~ 맛있다고 하네요! 다른 지점도 이용해 봤는데 여기도 좋네용 직원 친절해요 (곰돌이 초는 기본으로 포함이었어요)', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'CAKE');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '쏘스윗 홍대'), (SELECT member_id FROM member WHERE email = 'user00002@test.com'), '친구 생일이라 급하게 예약했는데 너무 예쁘게 제작해주셔서 좋았어요 !!! 다음에 기념일에 홍대 오면 또 방문할 거 같아요 🤤🤤 사장님 감사합니다 🙇‍♀️', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'CAKE');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '쏘스윗 홍대'), (SELECT member_id FROM member WHERE email = 'user00081@test.com'), '친구 선물 케이크로 이용했어요! 비주얼도 정말 예쁘고 게다가 맛까지 훌륭해서 먹으면서 반했어요🎂 홍대 초코 케이크 맛집으로 추천이요!', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'CAKE');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '쏘스윗 홍대'), (SELECT member_id FROM member WHERE email = 'user00032@test.com'), '친구 생일기념으로 예약했습니다! 케이크가 너무 예쁘고 귀여워서 덕분에 잘 축하해줬습니다! 축하받은 친구도 너무 좋아해서 제 기분도 좋았어요! 감사합니다!', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'CAKE');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '쏘스윗 홍대'), (SELECT member_id FROM member WHERE email = 'user00041@test.com'), '당일 주문할 수 있는 레터링 케이크라 좋아요! 사실 케이크 맛은 기대 안했는데 맛있더라구요 ㅎㅎㅎ 선물하기에도 좋구 맛있어서 다음에도 주문하고 싶어요 ><', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'CAKE');

-- DONE:100_쏘스윗 홍대_리뷰.csv

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00009@test.com'), '▶️아이스라떼 크레마 장난아니에요!☕️ 연하게⚡️주문했는데 커피맛 충분히 나오고, 깊이감 있게 맛있어요.👍 산미 없어도 향이 그윽하게 올라와서 좋았습니다.🤎 홍대입구역에서 이렇게 분위기 있고🪽, 카공에 최적화된💻 카페는 처음 본 것 같아요.👍 다음엔 디저트류🍰도 먹어보고 싶어요. 재방문 의사 있습니다.🫶😊 ✔️1층은 얘기나누기 좋고, 2층은 카공하기 좋아요. ✔️1층, 2층 모두 콘센트🔌많은 편이에요. ✔️바테이블 좌석 크기가 넓어서 카공에 최적이에요.🤍 ✔️의자가 폭신폭신, 회전가능 하고 정말 편해요.💺💙 ✔️화장실은 2층 안쪽에 있어요.', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'FACILITY', 'PLUG');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'LATTE');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'PURPOSE', 'STUDY');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'PURPOSE', 'TALK');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00015@test.com'), '연남동에 있는 느좋 카페 노출 콘크리트의 쿨한 감성에 플레이리스트 선곡이 콜라보 되어서 분위기가 꽤나 괜춘하다 2층도 있는데 거긴 약간 췰한 무드로 자기 할 거 하는 공간의 느낌', 0, 0, NULL, SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MOOD', 'INDUSTRIAL');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00057@test.com'), '홍대 시티 뷰를 보며 작업하기 좋은 카페🏙️ 낡고 허름해보이는 힙한 인더스트리얼 감성과 어둑한 기운이 감도는 곳이에요 2층 규모로 널찍하고 자리도 많아서 작업하기에 최적이고 창가 자리는 동교동삼거리 뷰를 감상할 수 있어 멍때리기에도 좋았습니다🏢', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MOOD', 'INDUSTRIAL');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'PURPOSE', 'STUDY');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00032@test.com'), '홍대에 삼성폰 서비스센터에 급하게 일보고, 휴식하려고 방문했어요. 1층과 2층 테이블 자리가 많고, 손님들 가득하네요. 밀크티와 고르곤졸라소금빵 맛있게 먹고 있답니다^^', 0, 0, NULL, SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'ADE');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'BAKERY');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00075@test.com'), '빨간안경낀 여자분 손님 응대하지 마세요 ㅋㅋㅋ 아니 픽업하러 갔는데 어딘지 모르고 돌고있으면 부르던가 해야지 훑어보듯이 계속 어디가냐는듯이 그냥 보고있네 ㅋㅋ 바쁜거 같지도 않은데 손님들한테 띠겁게 할꺼면 본인 위해서 퇴사하시는게 나을듯 ^*^', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00007@test.com'), '1층도 좁지 않은데 2층까지 있어서 생각보다 매장이 꽤 컸어요!! 바스크 치즈케잌도 맛있도 아메리카노도 제 취향이였어요 분위기도 좋아서 다음엔 지인도 데려오고 싶어요!', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'AMERICANO');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'CAKE');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00056@test.com'), '여자 알바생분 슬픈일 가득한 사람처럼보임.. 너무 화나있어 보이길래 다른손님들 대하는거 지켜보니.. 원래 그런사람인거같음..누가 알바 억지로 시키는듯한 느낌.. 젊은 친구가.. 웃으면서 일해봐요~ 앵간해서 리뷰 안남기는데.. 올해 제일 일하기 싫어보이는 알바 상 주고싶음ㅋㅋ', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00029@test.com'), '디저트 베이커리가 다양해서 선택지가 많아 좋아요. 지역 특성상 매장이 넓은 곳이 잘 없는데 넓어서 좋았어요. 바스크치즈케이크가 특히 맛있어요! 뭔가 촉촉폭닥한 맛... 커피도 원두가 다양해 선택지가 많아서 좋았고, 1층은 대화하고 앉아서 수다떠는 분위기면 2층은 좀 공부하는 분위기예요. 바스크 치즈케이크 꼭 드셔보세용', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'BAKERY');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'CAKE');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'PURPOSE', 'STUDY');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'PURPOSE', 'TALK');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00026@test.com'), '바스크치즈케익이 비싼만큼 한조각 양이 어마어마하네요 2층은 팀플하는곳같이 넓은 테이블이 다 공부하고있어서 1층 왔는데 넓어서 대화하기 좋아여😗 늦게까지 하는 몇안되는 곳', 0, 0, NULL, SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'CAKE');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'PURPOSE', 'REST');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00008@test.com'), '2층에 자리잡았는데 와이파이가 느려서 속터짐 원래 이런건지 창가쪽 자리만 그런건지 콜드브루 커피 주문해봤는데 만족 항상 쇼케이스에 딸기타르트 보고 들어가서 커피만 주문하네; 편하게 가기엔 좋은 카페임', 0, 1, 'BAD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'FACILITY', 'WIFI');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'COLDBREW');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'ADE');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'PURPOSE', 'STUDY');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00053@test.com'), '너무 불친절해요 스투시입은 여자분 주문받으러 와서는 멀뚱멀뚱 서 있기만 하고, 주문해도 되냐깐 똥씹은 표정으로 있고 브루잉 메뉴 중에 산미 뭐가 가장 강하냐고 물으니 에티오피아랑, 엘살바도르라고 해서 엘살바도르 달라닌깐 없다고 하고 에티오피아 달라닌깐 없다고 하고 주문 받자마자 진동벨도 안주고 휙 가버리고 계산대 옆에 서 있으닌낀 그제서야 진동벨도 주고 첨에는 벨이 없는 카페인줄,, 인스타에서만 보던 힙합뽕 실제로 처음봐요.', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00006@test.com'), '작업하기 좋은 카페 콘하스연남 지나가면서 맛있는 타르트 보기만 하다 들러봤어요. 밤에 들러서 디카페인 아메리카노 주문 작업하기 좋은 분위기이고 집중 잘되어 좋았습니다. 일행이 케이크 주문했는데 맛있었어요', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'AMERICANO');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'CAKE');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'PURPOSE', 'STUDY');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00040@test.com'), '케이크(바스크치즈.빅토리아), 라떼 진한편! 둘다 맛있어요 :) 바닐라라떼도 맛있었지만 생각보다 달아서 조금 덜 달았으면..!', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'LATTE');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'CAKE');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00062@test.com'), '공간이 감각적이고 테이블 의자 모든 공간을 신경 쓰신게 보여서 너무 좋았어요 ! 빈티지한 분위기를 좋아하는데 분위기가 빈티지해서 오래 머물고 싶었어요 곳곳에 있는 식물들도 너무 예뻤어요 디저트도 종류가 많고 우유 종류도 여러가지 있어서 유당불내증 있는 사람들도 먹을 수 있어서 너무 좋은거같아요 ! 🥹 공간도 이쁘지만 디저트도 너무 맛있어서 종종 방문 할거같아요 ! 다음에 또 방문할께요😎', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'DESSERT');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MOOD', 'RETRO');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MOOD', 'NATURE');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00040@test.com'), '분위기 좋고 2층도 있어서 엄청 큽니다. 디카페인있어서 좋았고 커피맛 나쁘진않아요~ 분위기 좀 어둑어둑하니 대화나누기 좋고 노트북 하시는 분들도 많았어요. 다만 큰거에비해 화장실은 한칸인듯', 0, 0, NULL, SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'AMERICANO');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'PURPOSE', 'TALK');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'PURPOSE', 'STUDY');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00037@test.com'), '분위기 좋고 커피 맛도 좋습니다 사람이 다 챴는데도 여유로운 분위기가 나는 거 보면 조명이나 인테리어가 여유로운 분위기를 내는 것 같아요 홍대 지나갈 때 사람 없으면 가끔 들리고 싶어지는 곳입니다', 1, 0, 'GOOD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00017@test.com'), '맛, 분위기 다 떠나서 직원분들 응대가 너무 불친철합니다 메뉴 고민하는 거에 있어 시간이 오래 걸렸는데 빨리 고르라는 뉘양스로 재촉하시고 실수로 음료수 쏟았다고 도움 요청에 한숨 푹푹 쉬시고 얼굴 표정으로 기분 나쁜티 다 내셨습니다 기분이 너무 불쾌해 다신 안갈 거 같습니다', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00007@test.com'), '연남동에서 노트북 작업하기 좋은 카페를 찾다가 이곳을 방문하게 되었습니다. 카페는 1층과 2층으로 나뉘어 있으며, 특히 2층은 많은 콘센트와 좌석이 마련되어 있어 많은 사람들이 집중하여 작업하고 있었습니다. 음료 또한 매우 맛있어서, 작업하는 동안 즐거운 휴식을 취할 수 있었습니다.', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'FACILITY', 'PLUG');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'PURPOSE', 'STUDY');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00004@test.com'), '1층도 규모가 넓은데 2층은 더 넓어용 작업하기 좋은 분위기라서 노트북 가지고 오시는 분들이 많더라구여 ㅋㅋ 음료도 맛있고 좋습니다!', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'FACILITY', 'PLUG');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'PURPOSE', 'STUDY');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00069@test.com'), '동생이랑 연남동에서 밥먹고 근처 카페 찾다가 믿고 오는 콘하스 방문했어요 !! 무화과 타르트 주문했는데 시트 사이에 “밤 필링”? 이 있는데 약간 호불호는 있을 수 있을 것 같은데 전 호였어요 ㅎㅎㅎ 분위기도 좋고 직원분들도 친절해서 믿고 옵니다', 1, 0, 'GOOD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00063@test.com'), '12월 9일 평일 오랜만에 왔고 초콜릿 라떼 6천원짜리 한입도 안 먹은건데 양이…………. 받고 당황스러웠네요 반 밖에 안 차있어요 원래 이런거겠죠?', 0, 1, 'BAD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'LATTE');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00021@test.com'), '2층 좌석이 넓어서 작업하러 오기 너무좋아요. 좀 인원수 많아졌을때 와도 좋습니다.. 타르트가 너무 맛있어보여서 샀는데 좋았고 치즈케이크도 산미가 덜느껴져서 좋더라구요', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'CAKE');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'BAKERY');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'PURPOSE', 'STUDY');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00023@test.com'), '이렇게 맛없는 밀크티 처음먹어봐요;; 가격은 6000원인데 여자직원분 성의없이 대충만드셔서 1분만에 나왔습니다. 바리스타 맞으신가요? 집에서 티백섞어먹는것보다 맛없어요. 그리고 매장이용인데 일회용컵주시네요 평일 오후엔 비추합니다..^^', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00041@test.com'), '토욜날 방문했더니 사람 짱 많아요. 2층까지 자리 있는데 사람들 다 차있어요. 2층은 약간 워크존 느낌? 디저트 조금 비싸긴 한데 맛있습니다. 치즈케이크 부드럽고 감귤타르트 달곰 고소하니 맛납니다 :> 아메리카노 꼬소하고 디저트랑 딱이에요.', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'CAKE');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'LATTE');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'PURPOSE', 'STUDY');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00063@test.com'), '아이스초콜릿 6,000원 레몬에이드 6,000원 바스크치즈케이크 7,000원 늦은 시간 문여는 카페 찾다가 우연히 들어간 곳! 인테리어 독특하고 2층은 공부하기 좋은 곳 같다! 초콜릿은 생초콜릿이 들어가서 찐하구 레몬에이드도 왕상큼했다! 케이크도 왕부드럽고 맛있었당! 2층은 뷰도 좋았당!', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'ADE');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'CAKE');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'PURPOSE', 'STUDY');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00014@test.com'), '☕️ 아메리카노 5,500 🥖 소금빵 3,000 좌석이 많은 편인데 사람들로 꽤나 가득찼더라구요 🥹 커피는 무난하게 맛있었고 소금빵도 합리적인 가격에 무난하게 맛있었어요! 소금빵 외에도 베이커리류, 케이크같은 디저트류 종류가 많아서 빵순이들이시라면 좋아하실 것 같아요 ❤️', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'AMERICANO');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'BAKERY');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00060@test.com'), '1,2층으로 되어있고 1층은 편하게 이야기할 수 있는 분위기였고 2층은 정숙하는 분위기였어요! 커피, 디저트 둘 다 동시에 맛 볼 수 있어요~', 0, 0, NULL, SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'PURPOSE', 'TALK');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'PURPOSE', 'REST');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00035@test.com'), '맛좋은 커피를 즐겨먹는 편으로 콘하스 커피 맛있습니다. 하지만 메뉴판에 한국어가 없네요 집중해서 봐야하고 무슨말인지 가늠이 안가는것은 직원분께 물어봐야하고 불편했습니다. 개선이 필요할듯해요 한국어가 메인이고 영어는 서브가 돼야하지 않을까요?', 0, 1, 'BAD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'AMERICANO');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00054@test.com'), '연남동에서 작업할 일 있을 때 가기 좋아요 !!! 노트북 하려고 찾은 카펜데 음료도 다양하고 베이커리도 다양했어요 2층 인테리어는 완전 사무실 분위기 ㅋㅋㅋㅋㅋ 힙한 예술인 분들 많으시더라고요 ㅎㅎ 오래 머무르기 좋은 카페라서 노트북이나 공부하기에도 좋을 거 같아요 ㅎㅎ 더 자세한 후기는 아래 링크로 놀러오세요 ㅎㅎ https://m.blog.naver.com/23-04-23/223256782219', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'BAKERY');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'PURPOSE', 'STUDY');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00055@test.com'), '아홉시 이후 커피 안됨, 일회용잔만 가능 가격 비싸고 맛도 그닥 별로 직원 싸가지 없고 손님 없고 자리 널려있는데 굳이굳이 2인 좌석으로 옮기라고 고나리질함 날파리 개많이 날라다녀서 비위상함;', 0, 1, 'BAD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'AMERICANO');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00006@test.com'), '작업하기 좋은 카페라고 친구한테 추천받아서 갔는데 ,,정말 좋았어요 !! ☺ 집중하기 좋았고 직원분들도 친절하셨습니다 ㅎㅎ 짱짱', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'PURPOSE', 'STUDY');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00054@test.com'), '인테리어 너무 감성적이고 좋아요!! 그리고 사장님 너무 친절스윗하심요 ㅠㅠ 라떼 시켰는데 사장님이 믿고 시켜보라는 원두로 주문했는데 마시자마자 너무 맛있고진하고 꼬소해서 친구랑 둘이 눈이 띠용했어요👀👀♥️♥️ 커피 뿐만 아니라 딸기타르트도 너무 맛있구여 치아바타도 담백하니 최고에용🥹', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'LATTE');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'CAKE');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00097@test.com'), '무화과 타르트 먹으러 갔어용. 넘 쫄깃달달~ 맛있었네요. 진짜 순삭…💕 1층은 사람이 많아서 좀 시끌벅적 했는데 2층은 조용조용~ 담번엔 이층으로..', 0, 0, NULL, SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'CAKE');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00087@test.com'), '1층은 대화하기 좋고, 2층은 책을 읽거나 노트북 하기 좋습니다. 콘센트가 많아서 전자기기 쓰기에 좋아요. 푸딩빵.. 저거는 한번 맛보기엔 좋은 것 같아요', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'FACILITY', 'PLUG');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'BAKERY');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'PURPOSE', 'STUDY');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'PURPOSE', 'TALK');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00091@test.com'), '수요일 저녁시간때 일하는 안경낀 남성분 정말 불친절하네요 알바인지 사장인지 모르겠지만 주문 받는 말투부터 시작해서 음료 줄때도 귀찮다는 듯이 짜증나는 말투로 응대해서 기분 좋게 왔다가 기분 다 상해서 나갑니다. 앞으로 다신 안올듯 하네요~', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00062@test.com'), '매장이 2층까지 있고 1층은 대화공간 2층은 일이나 공부하기 좋은 카페에요. 콜드브루 크림 라떼도 맛있었지만 원두 커피 종류도 많았어요! 재방문 의사 100%😋', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'COLDBREW');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'AMERICANO');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'PURPOSE', 'STUDY');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00085@test.com'), '푸딩 먹으러 왔는데 눈앞에서 품절돼서 너무 슬펐어요🥺 그래도 무화과 타르트 존맛탱...♡ 너무나도 빨리 순삭했어요ㅋㅋㅋㅋㅋㅋ 매장 진짜 넓고 직원 분들 전부 다 친절하셔서 좋아요!! 다음에도 오려고요! 다음엔 꼭 푸딩을 먹어야지...!', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'CAKE');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00078@test.com'), '추워서 급들어갔는데 크로와상 퀸아망 타르트 조각케이크 등 베이커리 종류가 많았다 그리고 1층보다 2층이 훨씬 자리가 많아서 좀 오래 있을 요량이면 2층도 괜찮을듯!', 0, 0, NULL, SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'BAKERY');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00082@test.com'), '그냥 무화과 타르트 맛인줄 알았는데, 중간에 팥같은게 들어가 있어서 맛이 특이해서 맛있네요. 커피 맛도 괜찮네요. 카페가 넓어서 작업하기도 좋을것 같아요', 0, 0, NULL, SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'AMERICANO');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'PURPOSE', 'STUDY');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00022@test.com'), '안녕하세요:) 얼마전 뚝딱뚝딱 작업하는 분들이 있었는데 콘하스가 생기더군요. 그래서 가야지 가야지 했습니다. - - 좋은 위치 넓고 쾌적한 환경 맛있는 커피. 연남동으로 위치한 콘하스의 도전을 박수로 응원하며 이 곳을 소개하겠습니다:) 많이 파셔요!', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'FACILITY', 'PLUG');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'AMERICANO');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'PURPOSE', 'STUDY');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00014@test.com'), '콘하스가 연남에도 생겼네요!? 합정이었나 상수쪽에 콘하스 있을 때 한 10년 전? 그 때 콘하스 처음 방문했었는데! 반가운 마음에 연남점도 방문했어요. 1,2층으로 테이블이 굉장히 많고 공간이 넓어서 좋더라고요. 메뉴에 ’콘하스커피‘라고 적혀있는 것이 바로 아메리카노이고 콘하스커피D는 고소한 원두, 콘하스커피L는 산미 원두의 커피입니다. 베이커리 종류도 굉장히 다양해서 다음엔 디저트도 함께 먹어보려고요 :) 테이크아웃은 -1,000원 할인되서 좋아요!', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'AMERICANO');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'BAKERY');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00073@test.com'), '늦게까지하는 카페 찾다 방문. 11시까지 영업. 커피맛 좋고요~ 아포가토 양도 많고 맛나요!! 다만 디저트 가격이 ㅎㄷㄷ😱 2층 넓어요~ 못먹고 포장했는데 가격대비 맛있으면 좋겠네요~', 0, 0, NULL, SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'LATTE');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00030@test.com'), '매장이 1층과 2층 둘다 넓어서 단체모임하기 좋을듯~ 딸기쇼트 케익은 평범한 딸기케익 맛이였고, 고소한 아메랑 잘 어울리네요😋', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'CAKE');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'AMERICANO');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00058@test.com'), '디카페인 원두를 선택할 수 있어서 좋았어요. 자리 종류가 엄청 많아요🙃 날이 선선해서 야외자리 앉았는데 오셔서 난로도틀어주시고 ㅎㅎ 친절하세요. (케이크 이름 생각 안나는…) 초코시트와 딸기와 라즈베리 잼의 조화가 좋았어요👍🏻 그리고 탄산 잘 안마시는 편이지만 요기 레몬에이드는 탄산이 세지 않아서 짝꿍꺼 뺏어마셨어옄ㅋㅋ🤣🤣', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'FACILITY', 'TERRACE');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'CAKE');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'ADE');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00078@test.com'), '커피맛은 좋았어요. 그런데 어디에 쓰이는지는 모르겠으나 레몬 착즙하시기전에 씻지도 않고 착즙하시더라구요;; 마트에서 사온 거 비닐 벗긴후에 바로..', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00021@test.com'), '밀크티, 디저트 둘 다 너무 맛있고 사장님이 친절하세요🫶 늦게까지 하는 카페 찾고 있었는데 쾌적하고 좋은 카페 찾아서 좋습니다!', 1, 0, 'GOOD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00081@test.com'), '가게가 2층까지 있어 좌석은 많아요. 다만 지도상 표시된 위치보다 더 윗쪽이라 찾기 힘들었어요. 지도보다 더 연희동 방향으로 가야해요', 0, 0, NULL, SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00064@test.com'), '요즘 매장 내 테이크아웃컵 사용 안되지 않나요? 뜨거운 커피 시켰는데 일회용컵에 담아주셔서요. 미세플라스틱 엄청 나와요 사람이 많아서 직원분들 힘든건 백번 이해하는데 손님이 앞에 서 있으면 뭐 마실건지는 좀 물어봐주세요. 뚱하게 계시다가 제가 먼저 뭐 달라고 하니 그건 안된다고 하시네요.', 0, 1, 'BAD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'AMERICANO');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00058@test.com'), '사람이 꽤 많은데 의외로 회전율 좋은 느낌인곳,, 커피맛은 괜찮았다 :) 천장 높고 넓고 햇빛 잘들어오고 분위기가 좋아서 편하게 지인과 수다떨기 좋은곳같다!', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'AMERICANO');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'PURPOSE', 'TALK');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00082@test.com'), '조용한 동네에 멋있는 카페 ! 매장에 흘러나오는 선곡도 다 좋았어요 ㅠ.ㅠ 내부가 넓지만 공간 활용을 잘해서 답답한 느낌이 없이 곳곳마다 다른 느낌으로 인테리어 되어있어서 구경하는 재미가 있었어요 ! 특별한 디자인 없는 일회용컵이 아쉬웠지만 커피는 맛있었어요 디저트는 거의 품절이라서 먹지 않았지만 다음번엔 디저트도 같이 먹어보고 싶어요 !!', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'AMERICANO');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MOOD', 'MODERN');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00088@test.com'), '얼그레이 케이크랑 아메리카노 마셨어요 설연휴여서 그랬는지 왠일로 사람이 없어서 아늑한 느낌 잔뜩 느끼고 왔어요 언제가도 따뜻한 느낌', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'CAKE');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'AMERICANO');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00041@test.com'), '관람료 내야하는 거 아닌가 싶을 정도로 멋지고 황홀한 카페♡ 이런 집에 살던 사람은 대체 어떤 사람인지 궁금해질 정도로 어마어마한 대저택을 카페로 개조한 곳이랍니다 (알고보니 중국 부호가 살던 집이었다 하네요 역시 대륙인의 스케일이란ㅎ) 연희동의 아름답고 멋진 저택들로 둘러싸인 뷰도 운치 있어요. 특히 야외 수영장 옆 의자에 앉아 커피와 디저트를 즐기는 맛이 일품이네요♡ 참, 커피, 디저트 모두 맛있는데 특히 디저트는 왠만한 고급제과 케잌 이상으로 맛있었어요', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'DESSERT');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00056@test.com'), '카페 넓고 층고 높아서 좋음. 커피도 맛있고 케이크도 맛있고… 🤭 야외 테라스도 좋아보였는데 이날 바람이 좀 차서 못나감 ㅠ 안추울 때 햇살 받으면서 커피 마실 수 있을 것 같아서 날 좋을 때 연희동 가면 생각날 듯', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'FACILITY', 'TERRACE');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'CAKE');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'AMERICANO');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00004@test.com'), '자리싸움 치열,,, 오전에 가세용 독특한 구조로 제일 사랑하는 카페 다만 다들 엄청 큰 소리로 웃고 떠드는 분위기라서 공부하긴 좀 그래용', 0, 1, 'BAD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'PURPOSE', 'STUDY');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00016@test.com'), '인테리어 멋지고 생각보다 앉을 자리는 없어요. 커피맛은 괜찮은데, 실내에서 먹고가는데 컵도 일회용이고 커피양이.....저래서 열었을때 기분이 안좋았어요ㅠ 다시갈것같지는않아요... 인테리어에비해 커피 잔과 양은 너무 별로였어요', 0, 1, 'BAD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'AMERICANO');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00036@test.com'), '분위기가 좋아요 카페 뒷마당?에ㅎㅎ 작은 수영장?도 있구 나무도 있구 모기들이 좀 공격하지만 낮에도 밤에도 각기 다른 매력이 있어서 좋더라구요 주차는 전화하면 자세하게 안내해주셔서 매장 앞 쪽에 주차했어요 커피는 아메리카노 D랑 L을 다 마셨는데 글쎄요; 제 타입은 아니었어요 커피맛은 그닥;ㅠ 무화과바게트는 맛있었고 롤케이크도 나쁘지 않았어요 색다른 분위기를 즐기고 싶을 때 한번쯤 방문할만 하지만 커피는 많이 기대하지 않으시는 게 좋을 거 같아요', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'FACILITY', 'PARKING');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'AMERICANO');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'BAKERY');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00005@test.com'), '난방이 안되어서 추웠고, 직원 분 불친절해요ㅠㅠㅠㅠ 돈 쓰고 기분 나쁘면 우울해집니다. 콘하스 서교동 시절부터 단골인데 슬프지만 다시는 안가기로 결심..!', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00036@test.com'), '예전에 홍대에 콘테이너 컨셉으로 있었던 것 같은데 제 기억이 맞다면요. 이곳 콘트리트 컨셉의 분위기도 마음에 듭니다. 미로 같은 공간들 덕분에 갈때마다 색다른 기분이 들 것 같아요. 에스프레소도 맛있었습니다~', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'AMERICANO');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MOOD', 'INDUSTRIAL');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00003@test.com'), '종종 연희동 올 때 콘하스 매장 들리는데, 들릴 때마다 직원분들이 불친절 한 것 같아요. 오늘도 그런 거 같아서 리뷰 남깁니다.', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00044@test.com'), '조명이 신의 한수인 카페에요. 다들 이곳에 카페가 있다는걸 어떻게 알고 오는지 모든 공간이 다 감각적으로 꾸며져 있어요. 평ㅣㄹ 저녁이어서 사람이 많지 않아 조용히 편히 얘기하다 왔어요. 엄청 넓어요. 실내, 실외 모두 편해요. 실외엔 물멍도 할 수 있어요.', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'PURPOSE', 'TALK');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00084@test.com'), '9/13 화요일 오후 5시 방문. 이렇게 비언어적인 표현으로 온몸을 다해 손님을 방어적으로 대하는 직원은 처음봐요. 준언어적인 주문을 받는 목소리나 억양, 말투나 안내하는 목소리도 친절은 나에게 1도 기대하지 말라. 아니, 기본도 기대하지 말라는 투입니다. 제가 자리에 앉을때까지 쳐다봐놓고(말이좋아 쳐다본거지 눈을 마주쳐서 째려봄)욕설을 들은거 같아 물어보니 아무 말도 안했다고 하시니 제가 가는귀가 먹었나보죠 ^^ 왜 쳐다봤냐 물으니 빈 자리가 몇개인지 본거라는 어불성설을 시전. 아 그리고 욕설을 들은거 같아 확인하러 갔더니 아니라 하시던데 뭐 그건 제가 귀가 멀었나보죠 저랑 눈맞아 눈싸움 한것도판단 못하는 눈도 멀고 ^^ 첫 방문고객이 다시는 안오게 만드는 매직을 가진 직원분을 고용중이시네요^^', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00046@test.com'), '리뷰 잘 안쓰는데 너무 어이가 없어서 써요 메뉴보고 고르는 그 1분도 안되는 시간에도 기분나쁜티 팍팍 내시면서 있는데 이런 카페 처음이예요! 리뷰 안보고 간 제 잘못이겠지만 야외 자리가 있다는거말고 장점이 1도 없음 음료도 다 5천원 이상하는 가격인데 양도 컵도 편의점 천원짜리만도 못하네요ㅜ안좋은 리뷰가 몇년 전부터 이렇게 많은데 개선도 없고 아직도 운영되는게 신기해요!', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00038@test.com'), '사장님 왜이렇게 불친절하신지.. 저런 마인드로 가게를 운영하시는게 신기할 정도로 말투도 별로고  , 자기 직원들 대하는 태도, 손님 대하는 태도들도 다 별로입니다 . 기분좋게 커피 마시러 왔다가 기분 나쁘게 집에 가네요. 카페 사장님이라면 기본적으로 손님들이 즐겁게 커피 마시는 걸 보는게 좋아야 하는거 아닌가요? 좌식테이블 앉았는데 테이블 아래 턱에 신발 올려놨다고 거기 올리지 마라고 말씀하셨는데 말투랑 시선이 너무 기분나빴습니다 신발 치울때까지 지켜보고 나중에 와서 감시하고 계속 또 말하고...진짜 기분나빴어요 사장님인데 반바지에 슬리퍼 끌고 돌아다니면서 핀잔 늘어놓는거 보니까 카페 분위기도 완전 별로처럼 느껴졌어요', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00044@test.com'), '콘하스 커피도 맛있고 여기 밀크티 맛있어요 ! 근데 여긴 직원들이 어쩜 이렇게 불친절할까요 참 이러기도 쉽지않은데… 컨셉인건지.. 기분좋게 카페왔다가 이렇게 기분 상한적은 또 처음이네여.. 허허~ 남자사장님은 사장인지 알바생인지 뭔지 모르겠는데 너무 무뚝뚝하시구.. 여자 알바생인지 사장인지 동업잔지 뭔지 모르겠는데 아주 무서워서 지릴까봐 못가겠어용ㅠㅠ 예전엔 여자 알바생분 나름 친절했던걸로 기억하는데 뭐지 기억조작인가 허허 이렇게 예쁜 카페에 어찌그런.. 안타깝네요 ~ 그래도 음료맛있고 조경도 잘해놨고 공간이 예뻐서 또 방문하겠습니다 ~ 부디 친절하게 대해주시길 :)', 0, 1, 'BAD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'AMERICANO');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'LATTE');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00085@test.com'), '커피는 맛있는데 디저트는 그냥 그래요. 근데 무엇보다 일하시는 분들이 너무 무뚝뚝해요ㅠ 질문을 나이스하게 드려도 답변을 너무 귀찮아하셨어요', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00047@test.com'), '오픈주방인데 당당히 손님 흉보는 카페는 여기뿐. 순간 잘못들은건가 내귀를 의심했던;;; 평일이라 손님도 거의없고 안바빠보이던데 힘드셨나..? 친절은 바라지않지만 기분나쁘게 하면안된다 생각하는데 :;; 인테리어 훌륭하지만 직원분들때문에 평판 다떨어지는 최악의 카페 ...왠만하면 힘든 시국에 이런글 안남기는데 정말 심각함 이 많은 리뷰들을 보고도 수정안하시는거 보면 딱 사이즈 나옴', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00002@test.com'), '종종 직원분 응대가 피곤하고 귀찮아 보이기도 하지만… 음료도 괜찮고 디저트 종류도 많습니다. 공간이 예뻐서 날씨 좋은 날에 야외에 있기 좋아요! 요즘은 안 보이지만 귀여운 멍멍이도 있어요. 아프다고 적혀있었는데 건강하게 나아서 돌아오길 바라요…! 🐶', 0, 0, NULL, SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'FACILITY', 'PET');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00007@test.com'), '초콜릿은 그냥 초콜릿음료맛 / 자연광 미쳤음 / 날씨 좋은날 매일오고 싶음/ 야외관리안되는듯 더러움/ 화장실 냄새 좋음 / 디게 큼 / 외부음식 취식안돼서 생일이신분 케이크 들고 반입 불가능 ㅠㅠ / 수영장 굿 안에 공부하시는분들 많은걸로 보아 카공으로도 좋은들', 0, 0, NULL, SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00093@test.com'), '평일 낮. 진짜 불친절해서 나까지 불친절하게 대응하게 됨. 리뷰 안좋은거 알고도 걍 노트북할 큰 카페라 왔는데, 전층을 통틀어 2.4g 와이파이 딱 하나만 제공하고 딴거 없다함 내가 갔을땐 이용자가 많아서인지 뭔지 홈페이지 하나도 못띄워서 암것도 못하고 걍 돈버리고 스벅 갔음', 0, 1, 'BAD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'FACILITY', 'WIFI');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'PURPOSE', 'STUDY');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00004@test.com'), '딸기디저트 진짜 맛잇어요!!! 커피류는 잘 모르겠구 라벤더민트 마셨는데 심장 콩닥콩닥해서 카페인 좀 많이 들어있는거같아오! ;-;인테리어 분위기 무엇 완전 좋아영', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'CAKE');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'ADE');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MOOD', 'NATURE');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00042@test.com'), '사장님 친절하시고 분위기 정말 좋아요 ! 평일 저녁에 갔는데 공부하시는 분들도 정말 많더라구요 ! 카페에서 먹은 얼그레이롤케이크도 맛있었지만 포장해서 먹은 치즈케이크가 존맛탱이었어요 ,,추천 !', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'CAKE');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'PURPOSE', 'STUDY');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00097@test.com'), '음료나 휘낭시에는 보통맛이에요 아 휘낭시엔 설탕 코팅땜에 달았구요.... 정원의 야외 자리가 좋은거 말고는 실내 좌석이랑 쏘쏘에요 직원은 걍...', 0, 0, NULL, SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'LATTE');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'ADE');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00041@test.com'), '주말 12시쯤 가니 야외 공사중이라 개방 안한다고해서 실내에 앉음. 근데 나갈때 보니 야외 문 개방되어있고 이미 많은 사람들이 앉아있었음. 직원도 불친절하고 빵도 안나와있었음.', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00056@test.com'), '공간이 넓고 좋아요~ 커피도 맛있고 안에 있는 흰 리트리버가 햇볕에 늘어지고 있는 것도 넘 좋아요. 주말이라 사람은 많았지만 공간은 좋았어요!', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'FACILITY', 'PET');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'CAKE');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00049@test.com'), '커피를 빵그릇쪽에 쏟아서 죄송하다고 흘린거버려달라고말함 남자알바가 바로안받고어쩌라는식으로 쳐다보다 쟁반을 받음.싱크대에 커피랑 빵을 다 붓고 물로씻더니 둘이 커피는되는데 빵은안된다 이런식으로 얘기하는데 다들림 나도내실수니까 공짜로 얻어먹을생각없었음 친구랑 뒤에서 보고있는데 혹시빵버린거냐니까 둘이눈치보더니 아니라고 싱크대에서 빵다시꺼냄 설마저거우리주는거아니겠지하고 우린손에묻은커피때매 화장실가서 손씻고나옴 근데 빵이랑 다 깨끗하게 쟁반에있길래 다시주셨나보다하고 들고가는데 빵들어서보니 밑에축축함 다시가서 빵버리고 다시결제하고 새로달라고함 흘린거괜찮냐고물어보지도않고 다신가고싶지도않음 진짜 서비스업에서 일할거면 그런식으로 하지말라고 말하고싶음 기분만 안좋아지고 빵도 딱딱하고 가서말할까하다 그냥다신안오고말지하고 나옴', 0, 1, 'BAD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'AMERICANO');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'BAKERY');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00049@test.com'), '연희동 대형 주택개조 카페 입니당 공부하기 너무 좋은 곳이고   사진 찍기 좋은 곳 입니다 마당에 미니 수영장 있어요 다들 거기서 사진 찍으시더라고용 !! 디저트도 맛있는데  다이어트 때문에 못 먹어서 눙물 😭😭😭😭😭😭😭', 0, 0, NULL, SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'DESSERT');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'PURPOSE', 'PHOTO');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00006@test.com'), '이번이 두 번째 방문인데 평일 오후에 카운터에 계신 직원분 태도나 말투가 너무 불친절해서 재방문 의사 없음. 귀찮고 짜증난듯한 어투로 같이간 지인들도 올 때 마다 느꼈다고 합니다. 개선하셔야 할 듯 해요 인테리어 좋고 넓지만 다신 안 올 것 같네요', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00067@test.com'), '알바생 교육이 다시 필요합니다 서비스 개념이많이 부족한듯 하네요 불친절하고  업주가 그런 알바생을 돈주고 고용하실 필요가 있나 싶네요 ㅎㅎ 사장님 업장 망하겠어요~~ 좀만 더 분발하세요 상전 인줄 알았어요', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00069@test.com'), '사장님이 되게 친절했던 기억에 종종 들렸는데 오늘 남자 직원분 너무 불친절하네요 서비스직이 안맞으면 카페에서 일하지마세요 제발 ㅎㅎ 주문하나 하는데 너무 불쾌했네요 감정 티내지말고 좋게 말해주세요. 당신한텐 여러명의 손님들이겠지만 저는 오늘 기분좋게 나와서 처음 들린곳이니까요;', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00036@test.com'), '새로나온 콘하스 블렌드에 대해서 설명도 친절히 잘해주셨고 남자직원 두분이 굉장히 응대를 잘해주셔서 편하게 커피한잔 즐기다 가네요 ㅎㅎ', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'AMERICANO');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00004@test.com'), '유일하게 단골로 오는 카페입니다. 올 때마다 항상 직원들은 친철하고 응대를 잘 해주셨습니다. 그리고 커피는 여기가 최고입니다.', 0, 0, NULL, SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00055@test.com'), '층고가 높고 좌석도 다양해서 좋아요. 다만 여름 한낮의 1층 좌석은 더웠어요. 2층은 말할것도 없고요. 시원한 곳은 지하라고 해서 갔더니 시원했어요. 다만 직원분들이 따로 와서 환기 같은건 안해줘서 오래 있기엔 조금 불안한것 같아요. 에어컨 낮추면 누구는 춥다고 이야기한다해도 실내에 머물기에는 좀 더웠으니 참고하세요! 마음이는 너무 예쁘네요 ㅎㅎ', 0, 0, NULL, SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00019@test.com'), '커피가 맛있다는게 원두뿐 아니라 그곳에서 경험한 모든 것이 커피 한잔에 녹아들어 기억된다는 문구를 떠올리게 하는 곳 인테리어 조명 테이블 실외 및 야외 분위기 등 모든게 커피 한잔에 내려있다 애정하는 곳', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'FACILITY', 'TERRACE');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00093@test.com'), '콘하스 사장님의 안목과 감각은 존경스러울 정도네요. 특히 장농을 뒤집어 테이블로 활용 하신 센스는 정말 엄지 척! 호리호리하신 남자 직원분께서는 호두 알러지 있는 거 바게트 빵 좋아하는 거 뿐 아니라 직전 먹었던 원두 종류까지 기억 하시고 새로 들어온 원두 추천까지 해주셔서 콘하스 방문후에는 늘 대접 받고 편히 쉬다 오는 느낌입니다. 다양한 원두를 맛볼수 있고 특히 디저트는 맛과 모양뿐 아니라 재료를 아낌 없이 쓰는 점(특히 무화과 바게트에 든 무화과 양은 대한민국 최고라 해도 과언이 이니네요)  콘하스 좋아요.', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'COLDBREW');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'DESSERT');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00044@test.com'), '오늘 첫 방문했는데 역시나 소문대로 뷰와 인테리어도 정말 멋있었고 , 커피나 디저트류의 맛도 정말 맛있었습니다. 특히 오늘 마신 커피가 정말 제입맛에 잘 맞았습니다ㅎㅎ 사진도 정말 잘나오는 연희동 명소가 맞네요 다음에 재방문 하겠습니다!!', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'AMERICANO');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'DESSERT');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'PURPOSE', 'PHOTO');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00064@test.com'), '저녁에는 직원들을 막 뽑으시나봐요 우헤헤 여자분 사장님이면 진짜 큰일이네요..사장님 아니시길 간절히 기원합니다.. 아 그리고 남자분 계속 쓰시면 손님 줄어서 매출 떨어지는 소리 들릴거 같아요ㅠ', 0, 0, NULL, SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00025@test.com'), '인테리어 이쁘고 신기해요 화장실 내부가 특이하고 전면 거울있는ㄷㅔ 사진잘나와요 공휴일애가서 사람이많았어요 자리찾기가 어려웠습니당 그리고 베이커리가 다나가서 슬펏어요 ㅠㅠ', 0, 0, NULL, SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'BAKERY');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00063@test.com'), '야외 정원이 있다는 장점 외에는 매장도 관리가 덜 되었고. 커피맛이 별로에다 남자 직원분이 계속 인상을 쓰고 손님을 대해서 불쾌함을 지우기 힘들었습니다. 과잉친절은 필요없지만 화가 난듯 질문에 퉁명스럽게 답하고 고객과 이야기 하며 시선은 허공에 가 있고 계속 돌아다니며 얼굴엔 인상 가득해서 분위기가 불편했어요ㅠ 한남동에 비해 많이 아쉬웠고 재방문 의사는 없습니다.', 0, 1, 'BAD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'FACILITY', 'TERRACE');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'AMERICANO');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00044@test.com'), '위생 진심 심각하네요.. ㅋㅋ..세게 닦지도 않고 그냥 물티슈로 한 번 닦은 책상이에요. 수박 겉핥기식으로 청소 하실거면 왜 하시는지.. 물티슈는 주니까 자리 한번씩들은 닦고 앉으세요.. 돈이 많으셔서 굳이 손님 하나 하나에 연연하지 않아도 되는 상황이신지 뭔지는 모르겠지만, 너무 불친절 하세요 특히 4월2일 4시쯤 결제 보셨던 직원분이요 너무 불친절 하시네요. 제 돈 주고 커피랑 빵 사먹는데 죄 짓는 느낌이었습니다~ 주문 다 하기도 전에 중간에 말 자르고 본인 할 말 하시는거 하며, 귀찮고 짜증나는 듯 한 목소리와  표정.. 짜증나시면 집 가세요 그냥 ..; 서비스가 너무 엉망인 곳 ㅠ', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00094@test.com'), '외관부터 넘 예쁜 카페 규모도 엄청 넓고 수영장도 있고 베이커리 다양해 커피랑 즐기기 좋아요. 아메리카노 진해서 맛있는데 양이 넘 적어요.', 0, 0, NULL, SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'AMERICANO');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'BAKERY');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00075@test.com'), '평일에 왓으면 한적하게 여유있게 즐길 수 있을 것 같아요! 사장님이 친절하게 하나하나 설명해주시고 추천해주셔서 맛있게 빵과 커피 즐겼습니다!', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'BAKERY');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'PURPOSE', 'REST');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00063@test.com'), '가오픈 한남점 다녀왔는데 공간 인테리어는 좋으나 주차 공간이 없어서 한참 먼곳에 주차하고 걸어가는게 너무 힘들었어요 커피는 맛있었어요', 0, 1, 'BAD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'FACILITY', 'PARKING');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'LATTE');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00051@test.com'), '분위기는 색다르고 좋아요 특히 바깥쪽 조그만한 수영장과 마당이 예쁩니다 앉을 곳은 딱히 많진 않아요 그리고 무심한강아지 귀여워요', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'FACILITY', 'TERRACE');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'FACILITY', 'PET');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00084@test.com'), '접근성은 떨어지지만, 크고 예쁘고 좋아요. 주택개조카페라 화장실에 욕조랑 샤워기도 그대로 있네요. 들어갈 수는 없지만 작은 수영장도 있구요. 구석구석 지하부터 지상, 마당까지 구조를 보는 재미도 있어요. 덤으로 커피 맛도 좋습니다^.^', 1, 0, 'GOOD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00097@test.com'), '아이스 카페라테, 밀크티, 아포가토를 먹어봤어요. 연희동 카페 중에 단연코 추천할만한 카페입니다. 들어오면 넓은 내부에 깜짝 놀라실 거예요. 날이 시원하면 야외 좌석도 굉장히 좋습니다. 안에서 보이는 바깥 풍경도 정말 아름다워요. 음악도 조용한 공간과 잘 어울려요.', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'FACILITY', 'TERRACE');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'LATTE');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'ADE');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MOOD', 'NATURE');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00049@test.com'), '사장님 추천으로 호두 찰식빵 시켰는데 존맛탱!!!!!!! 커피는 맛 괜찮지만 너무 비싸여 ㅋㅋㅋ 그래도 직원분들 다들 친절하세요', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'BAKERY');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00042@test.com'), '주차 장소가 부족한게 유일한 아쉬운점. 어떻게하냐 물어보면 사장님이 친절하게 주차 방법 알려주심. 공간이 다양해서 앉아보고싶은곳이 많은데 사람이 항상많아서 ㅠㅠ 다음에 꼭 창가자리 도전.. 빵은 아몬드크루아상이 제일좋았어요', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'FACILITY', 'PARKING');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'BAKERY');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00007@test.com'), '수영장이 있는 예쁜 카페라 유명해서 가보고 싶었던 곳인데 정말 예뻐요. 주택을 개조해서 만든거라 인테리어도 독특하고 무엇보다 바닐라크림치즈케익 진짜 너무 맛있어서 2조각 먹고 포장까지 해왔어요. #바닐라크림치즈케익 #콘하스 #연희동카페추천', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'CAKE');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00054@test.com'), '주차가능이지만 가게 주변에 총 5대 정도 가능 웬만하면 도보 추천 가게안이 넓진 않지만 다양한 공간으로 구성되어 있음 음료는 5분 안에ㅜ나오고 케익이 맛있어요', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'FACILITY', 'PARKING');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'CAKE');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00036@test.com'), '인테리어로는 별 네개이상이지만.. 제조 음료가 좀 많이 달고.. 근데 3~4인 만 앉을 수 있는 곳이 많아 둘이 오면 자리 잡기 좀 힘들어요..', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '콘하스 연남점'), (SELECT member_id FROM member WHERE email = 'user00037@test.com'), '서울하늘아래 이렇게 여유로운 공간이 있다니! 커피도 맛있고 실내공간도 정말 다채롭게 만들어 놓았네요~ 작은 수영장 앞에 앉아있노라니 생각이 정리되고 근심이 사라지는것 같습니다~ 맛있는 빵도 많아요! 강추', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'BAKERY');

-- DONE:101_콘하스 연남점_리뷰.csv

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00047@test.com'), '우와 이렇게 사람 많은 설빙 매장도 넓고 시원...!!! 메뉴도 생각보다 빨리 나옴 👍👍 역시 여름이라서 빙수 먹고싶었는데 설빙 밖에 생각 안남..! 요즘 설빙 진짜 가성비 좋고 메론빙수 ㄹㅇ 맛나요💕❤️ 이날 먹고 담날 배달로도먹음 ㅎ ㅋㅋㅋ', 0, 0, NULL, SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00004@test.com'), '여름만 되면 꼭 먹어야하는 메론빙수,, 지~~~~인짜 맛있어요 다들 꼭 먹어보면 좋게따.. +아무리 주말이라지만 사람이 이렇게 많을줄 몰랐어요 이렇게 꽉찬 설빙 난생 처음봐요 그치만 다들 멜론빙수 많이드셔서 뿌듯', 0, 0, NULL, SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00059@test.com'), '과일흠뻑화채설빙 먹었어요. 과일 여러가지 들어간거 먹고 싶어서 음료는 넣지 않고 냠냠 잘 먹었습니다. 홍대점이라 그런지 사람 엄청 많았고요.. 앉기가 불가능해보여서 다른 카페 가려다가 운 좋게 앞에 자리가 나서 앉아서 먹었어요. 사람 많고 배달 주문 계속 들어오는 것 치고는 빙수도 빨리 나왔구요! 오랜만에 설빙 맛있게 잘 먹었어요 ㅎㅎ 하지만 다음부터는 홍대에서는 설빙 패쓰.......... 사람 너무 많아...... ㅎ', 0, 0, NULL, SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00015@test.com'), 'ᴍ ᴇ ɴ ᴜ 김볶밥(5800) + 과일흠뻑화채설빙(17500) // 빙수 진짜 맛있어요~~ 뽕따/메로나 섞인 얼음 맛이에요 ㅎㅎ 김볶밥도 진짜 맛있어요~~~', 0, 0, NULL, SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00002@test.com'), '신메뉴 나왔다고해서 설빙왔어요. 뭐 먹을까 고민하다가 수박으로 정했는데, 탁월했네요ㅎㅎ 연유넣지않고 그냥 먹는게 훨씬맛있어요!', 0, 0, NULL, SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00016@test.com'), '매장넓은데 사람도 그만큼 엄청 많아요! 키오스크 한개라서 주문할때 웨이팅이 있어요..메뉴 나오는 시간보다 주문하려고 줄 서 있는 시간이 더 긴거같아요 ㅠㅠ 메뉴는 엄청 빨리 나와요 사람 많아서 대화하기 편하진않아요', 0, 0, NULL, SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00042@test.com'), '오랜만에 설빙왔는데 맛있네요. 근데 매장이 너무 추워요... 빙수도 차가운데 매장이 너무 추우니까 빨리 먹고 일어나야 할 것 같은..', 0, 0, NULL, SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00001@test.com'), '빙수가 입으로 들어가는지 코로 들어가는지..역시 홍대구나 싶네요. 사람이 어찌나 많은지 얘기 좀 하러 들어갔다가 결국 빙수만 후다닥 먹고 다른 카페 갔네요. 와~~찐!! 사람 정말 많아요', 0, 0, NULL, SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00069@test.com'), '늦은 시간에 방문해서 대기 없이 바로 주문할 수 있었고, 빙수도 빠르게 나와서 좋았습니다. 언제나처럼 신선한 재료로 만들어진 맛있는 빙수를 즐길 수 있었어요. 편안한 분위기에서 만족스러운 시간을 보냈습니다!', 1, 0, 'GOOD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00071@test.com'), '설빙하면 인절미토스트죠  날씨가 좋아 빙수 땡겨 설빙 간김에 인절미토스트 먹었는데 예전처럼 완전 맛있지도 않고 락스냄새같은게나서 먹기전부터 별로였는데 토스트기가 오래된건지 열올랐을때 그 쇠탄 냄새가 락스냄새처럼 나 빵에 냄새가 베서 먹을때 해가됐고 맛있는 냄새가 안나고 먹는 내내 토스트기 가열냄새가 빵에서 나 맛있지도 않아 잘못 시켰구나..  다시는 시키지말아야지하며 꾸역 먹었다. 입구에서 하겐다즈 배너 보고 고민안하고 시킨 하겐다즈딸기빙수도 실망.  기존 치케딸기빙수랑 똑같은데 하겐다즈 광고용 띠에 아수쿠림 한스쿱이 하겐다즈라는거외엔 똑같다.  오히려 딸기가 더 조금 들었고 딸기철이라 시켰는데 냉동딸기라 하나도 안달고 시고 딱딱해서 이 시렵다.  예전엔 매장에 손님 미어 터졌는데 어째 손님이 없더라..', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00020@test.com'), '진짜 이런 설빙 가게는 처음 봤어요 다른 설빙 체인점은 사진보다 과일의 양이 적거나 그런데 진짜 똑같이 나왔어요 ㅜㅜ 진짜 감동적인 일입니다 설빙은 여기서만 먹고 싶어요 ㅜㅜㅜㅜ 사랑합니다', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'DESSERT');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00085@test.com'), '예전에 넓은 좌석이 꽉차있던 기억이 있어 올라왔는데 한산하고 창가자리만 차있어서 이상타 했는데 빙수 나오는걸보니 청결하지 않아 보여요  그릇마다 겉에 연유? 같은게 묻어있는데 그릇을 안씻고 빙수를 담는지 다른 테이블 그릇도 다 그러네요  찝찝해서 겨우 먹음 다신 못올듯', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00076@test.com'), '인절미 토스트는 걍 쏘쏘 아는 맛인데 엄청 맛나진 않아요  담에는 또 안먹어도 될듯 일행이 딸기를 좋아해 사진에 혹해서 딸기하겐다즈 설빙을  시켰는데 이 역시 쏘쏘 그냥 아는 맛 망고 시킬껄 그랬어요~~^^', 0, 0, NULL, SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00022@test.com'), '홍대 놀러갔다가 2차로 빙수가 먹고 싶어서 갔는데 여전히 맛있는 설빙입니다 ~~~~~ 하겐다즈 딸기빙수는 딸기 아이스크림과 치즈케이크, 생크림까지 올려져 더 맛있어요 !', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'ADE');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00089@test.com'), '설빙은 역시 맛있습니다👍 여기는 자리도 많고 담요,가글,손세정제 등 여러가지를 서비스로 구비해두고 있더라구요 센스 굿 사람많아도 빙수 빨리빨리 나와용', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'FACILITY', 'PARKING');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00045@test.com'), '설빙은 딸기빙수 먹으러가는 곳!!!! 처음 설빙 생겼을때부터 지금까지 가장 좋아하는 딸기빙수♥︎ 이제 빙수 하면 팥빙수 아니고 딸기빙수♥︎ 그런데 아직 딸기 수급이 활발하지 않은지 딸기크기나, 신선도는 아쉬워요 ㅜㅜㅜ 크고, 무르지 않는 딸기가 조타궁💗 제 최애 메뉴였건 프리미엄딸기빙수는 이제 단종인가봐요 ㅜㅜㅜ 어느 지점을 봐도 읍어.... 프딸빙인줄 알고 잘못 주문한 요거트딸빙도 처음 먹어봤는데 생각보다 존맛탱이하 놀래버렸어요!! 이제 요거트딸기빙수로 갈아탄다🤭', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'CAKE');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00095@test.com'), '설빙 정말 좋아해서 자주 먹는데 홍대점은 별로네요ㅠㅠ 손님이 별로 없었음에도 불구하고 맛이 다른 지점에 비해 현저히 떨어졌어요. 일단 초코를 너무 두껍게 뿌려서 초콜릿이 질긴 느낌..? 그리고 원래 얼음에 기본적으로 우유맛이 섞여있어서 얼음만 먹어도 맛이 좀 느껴지는데 여기는 그냥 얼음 퍼먹는 느낌이었어요. 소스양들이 전체적으로 다 적은 느낌. 딸기시럽도, 빙수에 들어가는 우유양도…. 그래서 먹고 싶어서 온건데 아쉬웠어요.', 0, 1, 'BAD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'ADE');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00041@test.com'), '초코덕후끼리 와서 하겐다즈빙수 뿌셨습니다. 생각 보다 초코맛이 크게 강렬하지 않고 달지 않아 부담되지는 않았지만 살짝 아쉬웠어요. 겨울이라 중고딩 친구들이 떡볶이를 많이 먹고 있더군요. 유튜브로만 봤는데 정말 떡볶이 맛집인가봐요', 0, 0, NULL, SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'LATTE');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00057@test.com'), '저녁 먹고 빠질 수 없는 디저트타임 입니다. 그중 오랜만에 설빙 브랜드를 방문하였는데요, 아직 봄이 되지 않았지만 딸기 신메뉴가 제법 많이 나와 있었습니다. 작년에 못봤던 하겐다즈 딸기 빙수도 있었고 녹차딸기 트리빙수와 딸기 생요거트 빙수 등등 딸기 시리즈 빙수가 5 가지나 있었습니다. (냉동딸기라 맛없었어요...시즌메뉴X) 사이드 메뉴로 츄러스를 시켰는데 바삭바삭 훌륭한 맛이었습니다. 겨울임에도 불구하고 많은 손님들이 자리하고 있어 깜짝 놀랐으며, 이렇게 큰 매장에 키오스크가 하나밖에 없다는 게 웨이팅하는 입장에서는 조금 답답했습니다. 커플들부터 단체손님 등등 다양한 손님들이 자리를 할 수 있는 넓은 매장입니다. 주차는 따로 지원되지 않는 매장입니다. 매장평가 4.0 / 5.0', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'CAKE');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'BAKERY');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00027@test.com'), '홍대라 진짜 바쁜 매장일텐데 딸기도 듬뿍주시고 치즈큐브도 뜸뿍 심지어 연유 필요없을 정도로 요거트초코딸기였나 이메뉴 맛있었어요', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'ADE');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00053@test.com'), '딸기꽤 많이 들어있습니다! 매장도 엄청 넓어요!빙수랑 매장은 다 괜찮은디 화장실만 좀 관리하셔야할것같습니다 ㅠㅠ 손비누가 없어요🙀', 0, 0, NULL, SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00027@test.com'), '한낮엔 아직 덥네요~ 경의선숲길 산책하다 더위도 피할겸 빙수먹으러 들렸어요 역시나 맛있는 설빙빙수~ 매장이 넓고 엄청 에어컨이 세서 빙수먹다 으슬으슬~^^', 1, 0, 'GOOD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00051@test.com'), '홍대점은 유동 외국인관광객이 많아서 그런가 복작복작합니다. 사람 많은데 주문키오스크가 한 개 밖에 없어 주문 줄은 길지만 매장 넓고 좌석 빽빽해서 자리 못잡는 경우는 없을 듯 합니다. 달달구리 땡겨서 먹었는데 역시 맛있어요. 매장 에어컨이 쎄지 않아서 빙수 먹어도 춥지 않아 좋아요.', 1, 0, 'GOOD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00079@test.com'), '초코 츄러스 맛있어요. 이 매장은 항상 사람이 많은데 언제나 주문하는데 10분 이상 대기해야하는 듯... 키오스크 한대 더 있었으면 좋겠어요 ㅠ', 0, 0, NULL, SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00061@test.com'), '애플망고 치즈빙수에 요거트 아이스 추가했는데 와 치즈 큐브 짱맛 ㅎㅎㅎ 연유 추가금 없이 리필 되는건 나가면서 알아서 아숩🤣 살찔까봐 빙수 잘 안 먹는데 여긴 자꾸 생각나는 곳 홍대 카페 커피숍 추천 하지만 엄청 시끄러움 ㅎㅎㅎ', 1, 0, 'GOOD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00059@test.com'), '세상에서 제일 유명한 설빙이겠죠. 말해뭐하겠습니까. 화장실도 깨끗하고 빙수도 빨리 나오고. 홍대에서 빙수 땡기는 날은 무조건 설빙이죠~ 그린티초코설빙. 저 그린티 아이스크림처럼 생긴게 녹차맛이 엄청 찐하고 크림같은 느낌이라(아이스크림은 아닌듯) 잘 어울려요.. 중간에 초코 무언가가 씹히는데 그게 진짜 맛도리. 그냥 그린티만 시키면 좀 그럴 것 같은데 초코가 추가되니까 그냥 아주 .. 쌈@뽕허이 조쿠만~', 1, 0, 'GOOD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00093@test.com'), '매장이 넓은 만큼 사람도 진짜 많아요. 그치만 다른 설빙가면 직원분들 지쳐보이실때가 많은데, 여긴 친절하셨어요! 그리고 워낙넓어서 그런지 사람 많아도 답답하지가 않고 왁자지껄 떠드는 분위기도 좋았습니다👍 홍대올때 빙수먹으러 자주 가는듯!', 1, 0, 'GOOD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00057@test.com'), '홍대에서 망고애플빙수를 디저트로 때림 오 여긴 외쿡인가 한국인가 외국인손님들 되게많은 시장한복판이라 보시면됩니다 조용한 대화는 연남동 길거리에서하고 여긴 맛있는 빙수를 박살내겠다는 생각으로오세요', 1, 0, 'GOOD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00059@test.com'), '매장이 넓으나 홍대라는 위치의 유동인구로 사람이 많은편이었습니다. 키오스크도 한개 밖에 없어 줄이 긴편입니다. 하지만 메뉴가 빨리 나오는편이라 그런지 회전률이 꽤 빠르다고 느꼈습니다. 그리고 생수는 제공되지않고 구매해서 먹어야했습니다!', 0, 0, NULL, SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00058@test.com'), '피곤한 하루의 완벽한 마무리 설빙입니다 딸기빙수가 아직도 판매해서 너무 반가웠어요 홍대라 그런지 손님들이 정말 많네요 키오스크 주문이고 손님이 많은데도 빨리 나오는 편이예요 맛나게 잘 먹었어요', 1, 0, 'GOOD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00052@test.com'), '자극적인 속을 달래고자 설빙 홍대점에 방문해보았습니다 정말 사람이 너무너무 많네요 😯 찻입장부터 빙수를 다 먹고 나갈때까지 주문줄은 계속 많았고 자리도 계속 만석이었어요 맛있었지만 조용히 대화를 희망하시는경우 주말에는 피하시는게 좋을 것 같아요 🥲', 0, 0, NULL, SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00053@test.com'), '사람이 엄청 많은데 자리에 앉자마자 빙수 나오는거 보고 깜짝 놀랐어요 여기 알바생 분들 진짜 대단하다는 생각밖에............. 그리고 식혜 맛있어요', 0, 0, NULL, SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00024@test.com'), '더워서 사람 겁나많음.. 줄서서 키오스크 주문하고 자리없어서 찾아서 기다려야했음.. 먹고나니 시원해서 좋았으나 사람들엄청 많아서 기빨림..ㅋㅋ', 0, 0, NULL, SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00021@test.com'), '설빙~ 맛은 여전하지만 관리가 너~무 안되는. . . 자리도 좋고 손님도 꽤되는데? . . . 지저분하고. . 의자도 너덜너덜. . .음', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00084@test.com'), '평일 10시쯤 갔는데 사람 너어어어무 많아서 무슨 술집인줄 알았네요ㅋㅋㅋㅋ 그래도 10시반 쯤 되니까 조금씩 빠졌고 사람많은것 치고 금방 나왔어요 블루베리빙수는 첨 먹어보는데 너무 맛있었어요 치즈도 많이 들어있어요 다음에도 먹을듯', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'ADE');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00045@test.com'), '불친절 퀄리티 최악 시설 최악 처음에 빙수 받고 개밥인줄 알았어요 아무리 외국인 대상으로 장사한다고 하지만 이렇게 관리 안된 지점은 처음보네요.', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00041@test.com'), '주말홍대 설빙 어마~  하네요. 오후 4시에 왔는데도 주문줄만 30분 걸렸어요. 키오스크가 딱 하나... 그에비해 빙수는 금방나와요~ 떡볶이 망고빙수 맛납니다 ㅎㅎ', 0, 0, NULL, SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00075@test.com'), '키오스크가 한대뿐이라 조금 아쉬웠습니다. 그에 비해 매장은 엄~청 크고 많은 사람들을 수용할 수 있기는 했습니다만, 테이블 정리라던지. 그런건 잘 되지않았던 거 같아요~', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00054@test.com'), '시원하고 맛있어요! 엄청 빨리 나와요. 살짝 위생이 걱정되지만 장 트러블은 없었습니다. 비오는 여름날이라 그런가 사람이 너무 많고 복잡했습니다!', 0, 0, NULL, SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00082@test.com'), '화장실냄새가 심갇함요 화장실에서 테이블까지 거리가있는데 화장실 문열때마다 토할거같았음 화장실급한데 두려워서 못감 진짜 비위약한사람 가지마세요 다른설빙가던지 다른빙수집 가셈 위생 비위 우웩임 ㅠㅠㅠㅠㅠㅠㅠㅠ 자리를 옮겨도 그냄새가 나는거같아서 호다닥 나옴', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00096@test.com'), '한국인 뿐만아니라 외국인도 사랑하는 코리아디저트 맛집 설빙!! 메론요거통통은 역시나 존맛이였어요😋 홍대 매장이 정말 넓은 편인데 다른 매장에 비해 시설도 좀 낡고 지저분…한 느낌은 있네요', 0, 0, NULL, SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'DESSERT');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00063@test.com'), '매장이 넓고 시원해서 좋아요. 빙수도 맛있어요! 단점이라면..아무래도 홍대라 그런지 외국인이 많아서 시끄러웠던거! 어쩔 수 없다는 건 알지만 귀가 너무 아팠어요.', 0, 0, NULL, SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'ADE');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00060@test.com'), '매장이 넓어서 자리걱정은 없어요..하지만 뭔가 어수선한 느낌..깔끔한 느낌은 없었어요..그리고 웬걸 여기가 외국인줄 다양한 나라의 분들이 거리에도 설빙에도 잔뜩..ㅎㅎ..그리고 딸기빙수에 올라가는 핵심 딸기하나가 신선함과는 거리가 멀어보이네요..아쉽..', 0, 0, NULL, SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'DESSERT');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00079@test.com'), '맛있어서 기절할뻔 했습니다 하겐다즈 딸기 초코 같이 먹으니 쭉쭉 들어가요 1인1빙수는 국룰이죠 저 웨하스는 저희가 올린거예요 원래는 없습니당', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'DESSERT');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00061@test.com'), '홍대 중앙에 있어서 그런지 사람이 엄청 많았어요..^^ 기가 빨리고 시끄러웠지만 여러모로 비도 피하고 쉬었다 가기 좋았어요! ㅎㅎ', 0, 0, NULL, SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'ADE');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00007@test.com'), '쿠폰이 있어서 갔는데 한번에 두개시켜먹으려니 힘들어여~ 손님이 많아그런지 테이블이나 화장실 정리등이 잘안됨 빙수도 좀 녹아서 나옴 그래도 맛은 굿', 0, 0, NULL, SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'DESSERT');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00059@test.com'), '원래 리뷰 잘 안 쓰는데요, 개인적으로 불편했던 점이 있어서 올립니다. * 카운터에 계셨던 베이지 모자에 검정 뿔테 안경 쓰신 남자 직원분이 (제가 매장에 있었던 시간 동안 계속) 연속으로 재채기를 하시는데, 팔로 입을 가리거나 마스크를 쓰지 않으시면서 계속 일하시더라구요. ** 아무리 카운터를 맡고 계시지만, 음식이 나와서 받는 순간에도 재채기를 하시는데 (소스랑 시럽 통 방향으로) 고개만 살짝 돌리시는 건 비위생적인 것 같습니다. 앞으로 위생에 더 신경 써주시길 부탁드립니다!!', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00094@test.com'), '멜론이 철이아니라 그런지 단맛이전혀 없고 그냥 식감만 좋았어요 빙수는 맛있었습니다 :) 홍대 설빙은 처음 방문했는데 밤 10시까지 넓은 매장에 사람이 가득할 정도로 시끌벅적하더라고요ㅎㅎ', 0, 0, NULL, SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'DESSERT');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00058@test.com'), '사람이 많지만 자리는 빨리 나서 빙수만 딱 먹고 가면 좋을 것 같아요 시끌벅적하고 매장이 썩 청결하지도 않아요ㅠ 별개로 생딸기트리설빙은 맛있어용', 0, 0, NULL, SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'DESSERT');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00028@test.com'), '딸기시즌이라 그런지..주말에는 사람도 엄청 많고 빈자리 찾기 힘들어요! 픽업받는곳에 연유소스통이 따로 놓여져 있어서 좋더라고요. 빙수도 듬뿍 주시고 다른매장보다 맛있는거 같아요~', 0, 0, NULL, SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'DESSERT');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00042@test.com'), '하겐다즈 오레오 빙수 먹었어요. 맛있었어요! 웨이팅을 하긴 했지만 자리도 금방금방 나고 외국인도 엄청 많네요... ㅎㅎㅎ 홍대점이라 그런가 오레오 초코라 그런지 연유를 아주 조금 넣었는데도 많이 달아서 안넣어도 될 정도였어요 자리도 넉넉하고 좋았어요', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'DESSERT');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00011@test.com'), '굉장히 넓고 팥빙수 맛나서 홍대오면 자주옵니다~~ 하겐다즈 초코설빙도 맛있고 팥인절미도 완존맛있어요~~ 직원분도 친절하시고^^ 또 올게요', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'DESSERT');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00005@test.com'), '토스트는 미지근하게 식었고 떡볶이도 미근하게 식었고 떡도 덜 불었는지 딱딱했어요. 알바생분들이신지 직원분이신지는 모르겠지만 남자 두분께서 무뚝뚝하시고 툭 던지듯 말씀하시네요.. 설빙 홍대점 처음와서 먹는 것 같은데 전반적으로 그닥 좋지는 않았습니다. 아 물도 사드셔야 합니다.', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00029@test.com'), '설빙 매장 중, 재료 폭탄 1등인 홍대입구역점! 설빙은 지점차이가 나는 것 같은데 홍대입구역은 재료를 정말 푸짐하게 사용해주시더라구요? 이만큼 팥도 잔뜩, 인절미가루도 잔뜩 주시는 곳은 처음이었어요. 가루를 중간에 한 번 또 넣어주셔서 진짜 맛있게 먹었어요! 떡도 말랑 쫀득 넉넉하게 올려주신 거 보이시죠?', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'DESSERT');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00059@test.com'), '갠적으로 서울역 맥도날드보다 여기가 더 바쁜듯 합니다 ㅋㅋ 외국인 관광객들이 대다수고 테이블도 많은데 꽉꽉 찹니다 !! 역시 k 디져트🍧', 1, 0, 'GOOD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00014@test.com'), '🔹메뉴추천 : 팥인절미빙수 🔹개인적 의견 : 설빙은 지점 바이 지점 확실한데 일단 홍대입구역점은 인절미 빙수 시키면 인절미 가루 혜자롭게 많이 줌. 여름에 가면 무슨 빙지순례 온거처럼 주문 줄이 끝나지 않음... 사장님 부럽. 근데 너어어어무 바빠서 테이블 닦아주지 않고 화장실 개더러움. 여자화장실 세 칸 중 두 칸은 늘 고장나거나 물 다 넘쳐있고 빙수 먹는 도중에 화장실가면 개토나올 정도로 화장실 드러움.. 빙수만 맛있으면 됐지 뭐... 위생에 흐린 눈 어려운 내가 문제지 뭐... 아무튼 빙수는 화장실에서 만드는거 아니라서 깨끗함...😅 💚장길동의 맛도리로드💚', 0, 0, NULL, SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'DESSERT');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00018@test.com'), '직원은 이어폰 끼고있고 ㅋㅋㅋ 주문할때 말한마디 안하고 개판이네요... 설빙 QC관리 안하나요??? 전국 여기저기 설빙 많이다녀봤지만 홍대점은 진짜 최악중에 최악입니다. 설빙 먹칠하는 지점...아오진짜. 개인 커피숍도 아니고 응대력...ㅇㅈ', 0, 1, 'BAD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'DESSERT');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00003@test.com'), '여름을 좋아하는 이유가 단 하나있다면 메론통통빙수를 먹을 수 있다는 것. 후후 매장 안은 무지 넓고 시원합니다~! 그리구..넓은 만큼 사람들 짱 마나요..! 북적북적합니다', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'DESSERT');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00095@test.com'), '오랜만에 설빙갔는데 사람이 너무 많아서 깜짝놀랐아요. 에어컨 완전 빵빵해서 기분이 좋아졌네용. 팥인절미빙수에 찹쌀떡이랑 저번에 맛있게먹은 기억이 있어서샤인머스켓 구슬아이스크림 추가했어요ㅎㅎ 찹쌀떡은 약간 아쉽지만.. 메인인 빙수가 역시 설빙답게 맛있었어요!! 어딜가도 맛이 똑같아서 좋아요! 배달도 엄청 밀려있던데 직원분들 화이팅... 다음에 또올게요😆😆😆😆', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'DESSERT');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00013@test.com'), '중국인들 많고 의자며 책상이며 지저분하고 끈적거리고 인테리어 노후 되었고 물도 구매해야하고 직원도 불친절하고 빙수 맛은 좋아요 ㅎㅎ', 0, 0, NULL, SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'DESSERT');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00052@test.com'), '시원하니 좋습니다. 그런데 평일임에도 사람이 너무 너무 많아요… 저 살면서 설빙에 이렇게 줄서서 먹는 모습은 처음 봐용•_• 홍대라 다르네요..', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00088@test.com'), '샤인머스켓 메론 빙수입니다! 실제 샤인머스켓 과일이 있는건 아니고 시럽을 얼려서 포도모양으로 한거예요! 같이 빙수에 섞어 먹으면 달달하니 맛있습니다~ 매장이 넓은 편이지만 사람이 너무 많아 정신이 없어요 ㅠㅠ', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00073@test.com'), '주말방문해서인지 인파가 장난아니었음 키오스크 줄이 이미 매장 문밖까지 줄서야하는 상황이었음. 테이블 앉은 사람들과 주문 줄선 뒤섞여있어 매장이 난장판이었음 복잡한 상황에서의 직원 누구도 매장관리하지도 않았고 쓰레기가 테이블과 바닥에 널부러져있는채로 앉으려는 고객이 치워야하는 상황들 다른곳가고싶었지만 먹고싶다는 친구의 말에 먹음 그러나 매장안은 난리나서 대화하기 힘들었음. 자리가 없어서그런지 모르는 사람이 아무말없이 우리가 앉은테이블에 짐 놓기도 하기도하고 ㅋ 다 먹은 그릇 어디다 버려야하는지 물어보려고 하려했는데 (아직 묻지도 않은 상황) 매장 직원이 갑작이 짜증난 표정과 함께 저기! 저기라고!  라고 짜증섞인 말을함. 아니..ㅋ 마지막까지 거지같은 상황이 남발 ㅋ 다시는 안갈곳으로 찜', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00098@test.com'), '매장 크고 외국인 많음.... 느릿느릿 정신 없고 의욕도 없어보임 걍 다들 집 근처 매장 가시길... 여긴 너무 관광객화 되어있 집 근처가 여기여서 슬픔... 포장인데 매장에서 먹는거로 음식 나오고 빙수 양도 적음..', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00082@test.com'), '물을 돈주고 사먹으라는 설빙은 또 처음 보네요 빙수며 커피며 3만원 넘게 썼는데 외국인들 많아서 그런걸까요. 같은 체인점인데 운영 방침도 다른가봐요', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00049@test.com'), '외국에서 온 친구 3년만에 다시 만나고 그동안의 이야기를 나눈 곳 좋은 시간을 보냈다. 개인적인 입맛이지만 망고가 딸기보다 좋음. 일하시는 직원은 항상 불친절하다 @.@', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00099@test.com'), '생딸기프리미엄는 생크림이 같이 올라가 있어서 연유 없이 먹어도 될 정도로 달달해요~ 망고빙수는 연유가 필수! 치즈케이크 많이 들어 있어서 좋아요. 올라간 아이스크림은 요거트 아이스크림이라 상큼해요', 1, 0, 'GOOD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00070@test.com'), '키오스크엔 줄이 길어 주문하기힘든데도 계산대에선 주문을 받아주지않아 깜놀 매장안에 띵똥띵똥 음식나왔다는 소리가 너무 크고 사람들 떠드는 소리도 너무 시끄럽고 정신없는 매장이에요 게다가 빙수양도 적어요 빙수가 수북하지않고 이렇게 납작하게 나오는거 첨이에요', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00020@test.com'), '샤인머스캣 메론빙수 먹었어요. 위에 포도모양이 구슬아이스크림이어서 신기했고, 메론도 달았어요. 다만 너무 정신없고 사람많고, 테이블도 안치워진곳이 너무 많아서 아쉬웠습니다.', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00005@test.com'), '한여름 3만원 이상 먹어도 물한잔 안주고 돈내고 사먹으라고 하는곳배부른 장사 또떼기 시장 어차피 홍대라 뜨내기 손님 많으니 배 부르겠지요 물티슈하나 없어서 손님 간곳 더러운 식탁에서 먹고 또먹고 한번와도 두번안옴', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00042@test.com'), '설빙항상 프리미엄만 시키는데 새로운게 나와서 시켰어요! 밑에 딸기가 있는데 양도진짜많고 마싯어요 위에 케익까지 ㅎㅎ맛있게ㅜ먹고갑니다! 만족해용 연유도 스스로 셀프로 이용할수있어서 눈치도 안보고 사용할수 있는점은 좋은것같아요 물티슈도 자유롭고요!', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'CAKE');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00038@test.com'), '금요일 오후 3시쯤 방문했습니다 사람이 정말 많았고요 정신없으신 건 알지만 딸기 상태가 이게 뭐죠..? 먹으라고 준건지 아님 남은 딸기 처리하려고 한건지 모르겠네요😠 의자도 상태 안 좋습니다,, 바쁘셔도 신경써서 준비해주세요 확인도 안 하고 그냥 나온듯 ;', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00041@test.com'), '코로나 전부터 오던곳인데 계속 있어서 좋네요 사람들많고 맛도 그대로 좋아요 더워져서 빙수먹으러왔는데 얘기하기도좋고 빙수도맛나요', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'PURPOSE', 'TALK');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00029@test.com'), '포장했는데 다 포장해놓고 점원들끼리 얘기하느라 주지를 않음. 포장하고 한참 떠들고 스푼넣고 한참 떠들고 보냉제 넣고 한참 떠들고, 바로앞에 손님이 기다리고 있어도  자기들 말씀 다  끝나고 주더라구요. 매장은 넓고 시끄럽고 더럽고 내가본 설빙매장중에 최악인데 입지때문에 늘 붐비고 베짱장사임.', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00002@test.com'), '슬슬 더워져서 설빙왔는데 시원하게 맛있습니다. 자리도 넓고 창문이 큰 유리라서 밖이 잘보여요.ᐟ.ᐟ  빙수 먹고싶으면 여기 와서 드시면 좋을것 같아요 추천드립니다.ᐟ.ᐟ', 1, 0, 'GOOD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00042@test.com'), '맛있어요 예전부터 왔던곳인데 없어지지않아 굿이네요 친구들끼리 수다떨기좋고 매장커서 대화나누기 너무좋아요 직원들친절하고 오래안걸리고 바로바로 나오네요 다음에도 재방문이네요', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'PURPOSE', 'TALK');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00096@test.com'), '지나가다 설빙 딸기빙수 먹고싶어 왔어요~~ 매장 넓고 창가에 앉으니 좋으네요 영도 많고 맛있어요~^^ 연유 뿌리지 않아도 달달하니 딱이네요', 1, 0, 'GOOD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00065@test.com'), '설빙 시즌 한정 메뉴 생딸기크럼블빙수 먹어봤는데 양도 푸짐하고 얼음 우유 위에 토핑으로 상큼한 생딸기와 깔끔한 요거트 아이스크림 그리고 부드러우면서 달달한 우유 케이크와 생크림까지 듬뿍 들어 있어서 좋았으며 곳곳에 슬라이드 된 딸기와 새콤한 딸기시럽 그리고 고소하면서 부스럭 거리는 식감의 크럼블까지 다채롭게 들어 있어서 전체적으로 너무나도 잘 어울리는 조합이었어요. 거기에 얼음 우유랑 층층이 딸기시럽이 있었는데 달달한 연유 뿌려서 먹으니 입에서 사르르 녹더라고요. 연유는 카운터에서 먹고 싶은만큼 가져올 수 있어서 많이 담아왔어요. 어느 정도 먹다가 밑에 갈수록 싱거워질때 연유는 무조건 뿌려야 돼요~! 그냥 먹어도 우유 얼음이라 담백한데 연유랑 곳곳에 딸기들을 같이 먹으니 풍미있는 맛을 느낄 수 있었어요~!!', 1, 0, 'GOOD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00072@test.com'), '날씨가 더워서 그런지 사람이 엄청 많았어요. 관광객도 많아요. 망고빙수는 넘 맛있어요. 그런데 청결관리는 필요해보이네요. 테이블에서 쉰내가 심하게 나서 자리 이동했어요.', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00098@test.com'), '외국인이 많이 오네요! 구글 리뷰 작성하면 아메 증정 이벤트도 하고 있어요 설빙 양은 그냥 쏘쏘! 좌석 많지만 테이블이 더러워요ㅠ 더 자주 닦아주시면 좋을것같아요!', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00057@test.com'), '외국인들의 성지 홍대설빙! 매장은 더럽고(바쁘니까 무ㅓ...) 직원분들은 친절하지 못하고...(외국인 상대 마니 하니까 뭐...) 빙수만 딱 먹고 오기 좋은곳 저도 외국인 친구가 가고싶다해서 갔지만 다음에 또 방문은 모르겠네유', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00086@test.com'), '딸기빙수가 생겨서 먹어봤는데 엄청 맛잇어요~ 종종 이용하는데 늦게까지 영업해서 여유있게 먹을 수 잇어서 너무 좋아요! 오늘도 맛잇게 잘 먹고가요~', 1, 0, 'GOOD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00025@test.com'), '딸기치즈메론빙수에  딸기 없음 쟁반들고가서 저기요 이거 딸기가 하니깐. 알바 꼬라보면서 빙수파보면 있다고  개띠껍게 대답함. 리뷰에 이따구로쓰니깐 블랙컨슈머처럼 보이겠지만. 정말 어이없어서 ''진심''을 담아서  리뷰남깁니다.   홍대지점만 이러는지 모르겠는데.  물건파는 서비스업이면  니 감정 숨겨라.', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00037@test.com'), '음 정말 맛있어요 ~.^^ 사람이 많은 이유가 있네요~!!! 오래기다릴줄알았는데 생각보다 금방 나왔고 맛있었어요!! 또 올게요^^', 1, 0, 'GOOD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00015@test.com'), '맛있어요!!! 인절미 가루도 위에만 뿌려져 있지 않고 안에까지 뿌려져있어요 너무 맛있어서 먹기전에 사진을 못찍었네요ㅜㅜㅜ 홍대입구역에서 3분 거리에 있습니다!!! 꼭 오세용', 1, 0, 'GOOD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00042@test.com'), '사람이 많지만 빙수는 빨리 나오는 편이었어요 하지만 매장 테이블이 치워지지 않은 곳이 대다수였고 화장실엔 휴지, 손세정제가 떨어졌는데 채워지지 않은 상태였고 지저분해서 두 번은 가고 싶지 않네요', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00062@test.com'), '제가 어지간하면 리뷰를 안남기는데 여기는 진짜 심하네요 카운터에 계시는 직원분들이 우리가 중고등학생때 흔히 학교에서 볼수있는 양x치들이네요... 부모님을 모시고 왔는데 눈이 안좋으셔서 메뉴판이 없으면 음식 나올때 밑에 깔려있는 메뉴종이라도 한장 달라고 부탁드렸는데 말한번 참 예쁘게 하시네요! 홍대입구역 9번출구 앞 역세권에 위치해있엇그렇지 위치가 아니라면 진짜 손님이 없었을것같네요 나는 키오스크로 주문만 하고 별다른 요구사항 없다라고 하시면 추천드리고 나는 요구사항이 있어 직원과 한마디라도 나눠야한다라고 하시면 안가시는것을 추천드립니다.', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00020@test.com'), '아직 요거통통메론빙수 안나와서 아쉬워요ㅜ언제나오나욤?...?ㅜㅜ주말에 가면 사람진ㄴㄴㄴ짜 많아서 못 앉을 가능성 80프로 정도됩니다... 설빙에서 얘기할 생각하지 마시고 빙수만 먹고 치고 빠져야해염...그럼에도 불구하고 빙수 내사랑 츄베릅', 1, 0, 'GOOD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00081@test.com'), '주문하고 한참 안 나와서 가보니 저희 거 빼먹으셨더라구요 설명도 없고 물어보니까 남자 분이 그제서야 주방에서 실수 했다 그러더라구요 그런데 사과도 없으시고 오랜만에 다같이 만나서 먹으러 온 건데 기분만 잡쳤네요;; 접시 그릇도 처음부터 연유 다 묻어있고ㅎㅎ… 다신 안 올 거 같네요ㅎㅎ', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00021@test.com'), '좌석이 진짜 많고 넓어서 얘기 나누기 좋고, 빙수도 너무너무 맛있고 양도 많아요!!! 여름되니까 이만한 곳 없네요! 자주 올 것 같습니다^^!!!', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'PURPOSE', 'TALK');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00014@test.com'), '매장이 넓어 좌석이 많고, 깔끔해요. 신메뉴나 그 외 디저트 종류가 다양해서 고르는 재미도 굿☺️ 신메뉴 로아커더블초코 비주얼 굿, 맛도 굿. 역시 설빙 🍨🍨🍨 겨울에도 빙수 먹을수 있어서 좋아요><', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'DESSERT');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00010@test.com'), '오랜만에 설빙 와서 그린티 초코 빙수 먹고 갑니다! 초코 크로플도 맛있네요. 매장이 넓어서 공휴일 저녁 시간에도 자리가 있네요 잘 먹고 갑니다!', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'ADE');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'BAKERY');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'PURPOSE', 'STUDY');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00014@test.com'), '프렌차이즈니깐 맛은 보장입니다 근데 남자 알바분인지 직원인지 사장님인지 모르겠지만 손님왔는데 바로 앞에서 에어팟끼고 핸드폰 하다가 뒤늦게 응대해주시네요  근데 불친절까지해서 참.. 아쉽네요^^', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00010@test.com'), '알바생분인지 직원분인지 예의가 너무 없으셔서 기분이 너무 나빴어요 리뷰이벤트하면 아메리카노 준다고 써있길래 리뷰쓰고 보여줄테니까 아메리카노 대신 물가능하냐고 물어보려했는데 나: 사장님 리뷰쓰고 사장님: 보여주세요 라고 말 툭 끊어버리고 눈빛도 너무 무섭네요ㅜㅜ 맛있게 먹으러 왔다가 기분만 상하고 갑니다 그리고 위생도 별로 같네요 손님 나가면 한참뒤에 물티슈로 쓱 한번 쓸더니 끝이네요... 벌레 나왔어요 그리고 몇번 먹다가 바로 나왔네요 그냥 가지마세요 리뷰이벤트랍시고 다들 좋은 말씀 써주시는데 소수의 리뷰를 더 믿으시길 바랍니다...', 0, 1, 'BAD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'AMERICANO');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00015@test.com'), '여기 좋은리뷰 쓴사람들 다 거짓말임 떡볶이는 냉동 인데 그것마저 해동  못해서 차갑고 테이블 제대로 안닦아서 무심코 올린소매에 연유묻어요', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00094@test.com'), '이 지점 매장도 크고 너무 맛있어요!!!!! ₍ᐢɞ̴̶̷.̮ɞ̴̶̷ᐢ₎ 최근 갔던 설빙 지점 중에서 가장 좋은 거 같아요 다음에 근처 오면 또 들려야겠어요', 1, 0, 'GOOD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00004@test.com'), '딸들과 오랜만에 설빙 홍대입구역점에 왔는데 생딸기와 애플망고 치즈, 식혜 시켰는데 아이들이 많이 좋아하고 맛있어하네요^^ 자주 들러야겠어요', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'BAKERY');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00100@test.com'), '인절미 꿀호떡 맛있는데 프리미엄 생딸기 설빙은 가성비 너무 안 좋습니다… 딸기도 적고 당도도 낮습니다. 딸기 철 지났으니 어쩔 수 없나요~… 그리고 그냥 빙수가 너무 비싸! 기프티콘 쟁여뒀다가 여름에 메론이나 먹어야겠습니다. 인절미 아니면 메론요거통통이 짱인 듯', 0, 1, 'BAD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'CAKE');
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'ADE');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00039@test.com'), '설빙 딸기빙수라 인절미토스트 먹었는데 빙수안에 찹쌀떡이 있어서 차가운빙수랑 잘어울리고 먹어본 빙수중에 맛최고였어용 !!! 빙수맛집!', 1, 0, 'GOOD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'DESSERT');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00014@test.com'), '생딸기 처음 먹어보는데 맛있어요‼️ 맛은 있는데 너무 납작?해서 좀 실망.. 설빝 알바해봐서 봉긋하게 만드는 거 아는데 여긴 비교적 그랬어요', 0, 1, 'BAD', SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'CAKE');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00043@test.com'), '일하는 직원 정말 별로네요. 공짜로 달라는 것도 아니고 내돈 내산으로 먹는건데, 기분 나빠서 사먹는 사람 다시 안 가게 만들게', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00007@test.com'), '생딸기빙수는 냉딸 빙수같아요. 맛없어요 알바생 불친절, 매장 너무 지저분..합니다 총체적 난국입니다. 정말 별로예요 외국인도 많이 오던데 부끄러운 매장이예요.', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00076@test.com'), '학창시절 추억이 담긴 설빙! 오랜만에 고등학교 친구들을 만나서 방문하니 추억이 새록새록🍓여전히 딸기설빙은 맛있어요! 매장도 넓고 토요일 저녁에 가도 자리가 넉넉해요☃️', 1, 0, 'GOOD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00006@test.com'), '여자친구와 저녁식사 후 디저트 먹으러 방문함. 손씻으러 화장실 갔는데 비누 안나와서 같이 들어온 직원에게 없다고 얘기하려 했지만 그 직원도 볼일보고 손 안씻고 나와서 그대로 메뉴 전달해줌. 음식점에서 청결이 기본인데 역겨워서 빙수 먹을때마다 속이 좋지않았음. Ps 모자에 안경쓴 알바생분 손좀 씻으세요', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00020@test.com'), '겨울에도 손님이 은근 많네용 근데 초코 메뉴 아닌데 그릇 밑에 초코가 잔뜩 묻어있네요.. 만드실때 묻은거 같은데 한번 닦아주면 더 좋았을 것 같아요 그래도 빙수는 맛잇었어요', 0, 0, NULL, SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00047@test.com'), '딸기 가득가득하게 담아 너무 만족스러운 디저트가됬어요!! 매장도 엄청 넓고, 테이블도 많아서 2인 부터 6인 일행도 편하게 이용할수있어서 좋은듯하였고 여러가지 다 맘에들었습니다!', 1, 0, 'GOOD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00095@test.com'), '가본 설빙  중에 제일 퀄리티가 안좋네요 바쁘다고 해도 망고도 거의없고 치즈도 대충 몇개 올려주고 정말 실망스럽습니다 가격도 비싼데 이렇게 대충 해주는 곳은 처음이네요 다신 안 갈 것 같습니다', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00043@test.com'), '최애 과일 딸기 주문   먹어봤어요 생딸기가  톡톡 씹히고  달달  연유까지 뿌려  먹으니  짜릿한 새콤달콤 맛상큼한  빙수가 되니 최애 빙수가 되었어요 딸기와 빙수를 살살  긁어먹으며 쫀든한찹쌀떡과  함께 먹으면  적당한 단맛으로 마지막 한  수저까지  냠냠 했네요  역시  설빙 딸기 시리즈는  최고입니다', 1, 0, 'GOOD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00062@test.com'), '오랜만에 왔는데 맛있었고 매장도 넓어서 편하게 앉을 수 있었습니다. 날이 많이 추운데 안에는 따뜻해서 빙수 먹어도 춥지 않았습니다.', 1, 0, 'GOOD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00038@test.com'), '알바생 싸가지 없고 손님이 카운터에 주문하러 와도 여자,남2 알바생 지들끼리 시끄럽게 떠들면서 얘기하더라고요. 영수증 달라니까 한참뒤에 주고 말투도 참 오해하기 좋네요.두번다시 안가요.', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00026@test.com'), '오래만에 설빙 먹으러 찾아왔는데. 주문 받는직원이 불친절함으로 먹고싶지않은만큼 기분 많이 나쁘게 했어요. 인사하는커녕 말 한마디도 안 했어요. 커뮤니케이션장애분들인가요?', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00087@test.com'), '예전에 친구들과 너무 맛있게 먹었던 기억이 있어 오랜만에 방문했는데 너무 실망이네요. 직원은 너무 불친절하고 빙수도 너무 대충 만들어 주니 좋은 추억 때문에 했던 기대가 실망으로 돌아오네요. 이제 설빙은 안 올 것 같아요. 좋은 추억만 간직하렵니다.', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00085@test.com'), '- 홍대입구 9번출구 부근에 위치한 설빙. - 홍대답게 외국인 손님들이 많음. - 아메리카노 hot/ice 각각 시켰는데, 커피는 매~~~우 연한 편이다. - 2층에 위치하고 있고, 매장은 넓은 편이고, 좌석은 많고 의자는 편한 편이다. - 요새 미쯔빙수 등 새로운 빙수 등 신메뉴가 계속 출시되고 있는 거 같고, 로제떡볶이, 붕어빵 등 각종 디저트들도 많아졌다. 빙수전문점이 과연 맞는 것인가 의심스러움!', 0, 0, NULL, SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'MENU', 'AMERICANO');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00067@test.com'), '프리미엄 생딸기 설빙 시켰는데 메뉴판하고 똑같을거라고 생각한건 아니지만 너무 다르게 나옴. 하얀 얼음 듬성듬성 다보이고 통딸기는 이상하게 행주쉰내가 남ㅡㅡ 매장은 전반적으로 너저분하고 깔끔하지가 않음. 화장실은 더 가관임.. 남자 알바생 둘이 카운터 보던데 주문하러 다가가도 핸드폰만지고 있고 불친절함', 0, 1, 'BAD', SYSTIMESTAMP);

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00064@test.com'), '으앙 샤인머스캣 진짜 생 샤인머스캣인줄알았는데 고냥 쩰리같은거네용 ㅜㅜ... 힝.. 그래도 얼음 밑에 있는 샤인머스캣 쨈이 먹을만 해서 맛있게 먹었습니다.,,,. 언제나 매장 넓구 이용하기 좋아요!! 홍대에서 데이트, 친구 만남 등등 무난한 만남장소로 추천드려요!', 0, 0, NULL, SYSTIMESTAMP);
INSERT INTO review_tag (review_id, category_code, code) VALUES ((SELECT MAX(review_id) FROM review), 'PURPOSE', 'DATE');

INSERT INTO review (cafe_id, member_id, content, good, bad, sentiment, created_at) VALUES ((SELECT cafe_id FROM cafe WHERE name = '설빙 홍대입구역점'), (SELECT member_id FROM member WHERE email = 'user00039@test.com'), '매장이 넓긴한데 테이블이랑 의자가 관리가 안 되는지 조금 더러웠어요. 딸기생딸기빙수는 위에 올라간 딸기가 무맛이여서 조금 실망스러웠어요. 그래도 같이 시킨 인절미 빙수는 맛있었어요.', 0, 1, 'BAD', SYSTIMESTAMP);

-- DONE:102_설빙 홍대입구역점_리뷰.csv

