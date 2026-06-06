-- Cafe Dummy Data

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner100@test.com'), '쏘스윗 홍대', '서울시 강남구 신사동 14-43', 37.5224, 127.017051, '02-7049-9311', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner101@test.com'), '콘하스 연남점', '서울시 용산구 이태원동 101-22', 37.532545, 126.998986, '02-6344-4359', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner102@test.com'), '설빙 홍대입구역점', '서울시 강남구 신사동 28-8', 37.52049, 127.025206, '02-4758-2603', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner103@test.com'), '내면의 발견', '서울시 용산구 이태원동 44-39', 37.53837, 126.997471, '02-7382-7054', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner104@test.com'), '17도씨', '서울시 서대문구 연희동 108-36', 37.573229, 126.94052, '02-9380-6744', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner105@test.com'), '이고 수플레 합정본점', '서울시 종로구 익선동 91-36', 37.578425, 126.985015, '02-3938-9618', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner106@test.com'), '배스킨라빈스 홍대상상마당', '서울시 강남구 신사동 52-38', 37.526105, 127.017114, '02-2559-7895', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner107@test.com'), '스타벅스 홍대입구역사거리R점', '서울시 송파구 잠실동 87-31', 37.513231, 127.097752, '02-3194-3046', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner108@test.com'), '오마이 메이드카페 오마이왕국점', '서울시 강동구 천호동 166-18', 37.538396, 127.120614, '02-4619-3032', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner109@test.com'), '메가MGC커피 홍대점', '서울시 마포구 연남동 5-13', 37.566805, 126.928483, '02-5519-1762', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner010@test.com'), '베이글랜드 홍대점', '서울시 은평구 불광동 49-13', 37.613641, 126.925431, '02-2612-4670', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner110@test.com'), '고망고 연남점', '서울시 성동구 성수동 51-36', 37.549309, 127.053737, '02-3332-5411', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner111@test.com'), '하우스키루', '서울시 광진구 건대입구 7-16', 37.5414, 127.070031, '02-1298-2593', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner112@test.com'), '훌리건 커피', '서울시 용산구 이태원동 42-1', 37.537174, 126.996752, '02-5596-6421', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner113@test.com'), '블루보틀 홍대 카페', '서울시 마포구 연남동 115-39', 37.565898, 126.925002, '02-8830-7588', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner114@test.com'), '낙랑파라', '서울시 종로구 익선동 132-22', 37.574245, 126.993318, '02-3071-9931', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner115@test.com'), '고망고 홍대입구역점', '서울시 용산구 이태원동 153-47', 37.535959, 126.994889, '02-5452-3243', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner116@test.com'), '눈내리는숲, 설래임 홍대본점', '서울시 송파구 잠실동 50-3', 37.51225, 127.095311, '02-1188-2853', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner117@test.com'), '주디마리 홍대점', '서울시 송파구 잠실동 20-38', 37.508359, 127.102111, '02-2309-3778', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner118@test.com'), '바이앤드커피', '서울시 마포구 연남동 29-45', 37.567679, 126.922802, '02-9236-3636', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner119@test.com'), '빨콩커피 연남점', '서울시 성동구 성수동 139-10', 37.543083, 127.052557, '02-5609-1967', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner011@test.com'), '올드페리도넛 연남점', '서울시 영등포구 여의도동 8-25', 37.524299, 126.926841, '02-2802-8405', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner120@test.com'), '미스터크레페', '서울시 송파구 잠실동 132-17', 37.509541, 127.095533, '02-1988-3079', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner121@test.com'), '올라운드커피', '서울시 종로구 익선동 99-2', 37.57481, 126.991236, '02-1966-2459', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner122@test.com'), '던킨 홍대역점', '서울시 영등포구 여의도동 27-35', 37.526135, 126.919409, '02-4972-1807', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner123@test.com'), '도덕과 규범', '서울시 용산구 이태원동 6-44', 37.534532, 126.998829, '02-6405-3966', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner124@test.com'), '할리스 연트럴파크점', '서울시 성동구 성수동 14-19', 37.547486, 127.055993, '02-6552-5289', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner125@test.com'), '마인드 비', '서울시 성동구 성수동 181-21', 37.548774, 127.057621, '02-3980-8599', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner126@test.com'), '디저트39 합정역점', '서울시 송파구 잠실동 113-50', 37.516033, 127.102421, '02-8003-7405', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner127@test.com'), '아우어베이커리 신촌숲길점', '서울시 마포구 홍대입구 150-48', 37.555932, 126.922415, '02-7240-3126', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner128@test.com'), '스타벅스 서교동사거리점', '서울시 은평구 불광동 120-35', 37.607737, 126.923347, '02-3201-2587', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner129@test.com'), '오디너리 아카이브', '서울시 영등포구 여의도동 14-47', 37.517986, 126.921281, '02-2291-8579', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner012@test.com'), '도란도란 1호점', '서울시 송파구 잠실동 96-17', 37.516964, 127.095106, '02-4541-7523', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner130@test.com'), '로파이', '서울시 영등포구 여의도동 98-48', 37.523916, 126.922241, '02-7480-6970', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner131@test.com'), '테라로사 홍대서교점', '서울시 강동구 천호동 106-49', 37.534594, 127.12389, '02-8797-7309', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner132@test.com'), '수택', '서울시 종로구 익선동 95-10', 37.576227, 126.990389, '02-7583-5158', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner133@test.com'), '커피랩스로스터리 합정점', '서울시 성동구 성수동 28-47', 37.546014, 127.060507, '02-3551-3456', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner134@test.com'), '금옥당 서교점', '서울시 광진구 건대입구 146-49', 37.542985, 127.072783, '02-8880-5864', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner135@test.com'), '커피덕', '서울시 성동구 성수동 194-50', 37.540428, 127.054132, '02-5040-9419', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner136@test.com'), '카페 호기심', '서울시 용산구 이태원동 69-25', 37.533559, 126.994114, '02-2347-3270', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner137@test.com'), '완포 코리아', '서울시 영등포구 여의도동 174-45', 37.519106, 126.921253, '02-8096-9810', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner138@test.com'), '사워도우 랩', '서울시 송파구 잠실동 140-5', 37.50976, 127.097093, '02-2541-2090', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner139@test.com'), '삼다코지', '서울시 송파구 잠실동 113-5', 37.518142, 127.103918, '02-2118-7303', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner013@test.com'), '로아프하우스', '서울시 마포구 홍대입구 158-22', 37.561164, 126.926787, '02-4627-7616', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner140@test.com'), '디저트 머라이언', '서울시 광진구 건대입구 198-23', 37.542582, 127.069895, '02-9488-7442', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner141@test.com'), '카페레터 합정', '서울시 용산구 이태원동 200-36', 37.536864, 126.994171, '02-3930-8563', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner142@test.com'), '커피빈 홍대역점', '서울시 광진구 건대입구 1-15', 37.537381, 127.071424, '02-2587-5014', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner143@test.com'), '디스코플래닛', '서울시 종로구 익선동 115-50', 37.569128, 126.992055, '02-3889-8462', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner144@test.com'), '요고 프로즌요거트 홍대점', '서울시 영등포구 여의도동 31-36', 37.52067, 126.921743, '02-7899-5966', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner145@test.com'), '루치펠 대저택', '서울시 마포구 홍대입구 127-45', 37.55678, 126.926674, '02-4708-9248', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner146@test.com'), '베어글스 홍대상수', '서울시 성동구 성수동 68-4', 37.540946, 127.058611, '02-4978-5929', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner147@test.com'), '모펀 AK PLAZA 홍대점', '서울시 마포구 홍대입구 52-44', 37.552504, 126.928765, '02-1125-6846', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner148@test.com'), '카페 장쌤', '서울시 용산구 이태원동 99-3', 37.539449, 126.997608, '02-4987-4422', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner149@test.com'), '달화채 홍대점', '서울시 마포구 홍대입구 35-35', 37.553896, 126.921662, '02-6058-5700', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner014@test.com'), '브라운하우스 연남', '서울시 강남구 신사동 33-22', 37.526394, 127.017675, '02-6792-5699', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner150@test.com'), '일쩜오플로어', '서울시 영등포구 여의도동 159-36', 37.522687, 126.923851, '02-3732-9137', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner151@test.com'), '크림온케이크', '서울시 서대문구 연희동 185-21', 37.566526, 126.935212, '02-5737-5319', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner152@test.com'), '쿠리노키제빵', '서울시 성동구 성수동 181-31', 37.547987, 127.050966, '02-4677-6584', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner153@test.com'), '오버트 합정', '서울시 은평구 불광동 11-41', 37.605421, 126.92796, '02-7837-2769', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner154@test.com'), '월페이퍼 서교', '서울시 영등포구 여의도동 90-43', 37.526483, 126.928236, '02-6578-1022', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner155@test.com'), '할리스 홍대거리점', '서울시 종로구 익선동 13-33', 37.57631, 126.986973, '02-1189-5904', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner156@test.com'), '드롭탑 홍대점', '서울시 광진구 건대입구 71-34', 37.536772, 127.072618, '02-6998-6413', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner157@test.com'), '슬릿 홍대', '서울시 종로구 익선동 153-31', 37.573192, 126.985318, '02-6665-8674', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner158@test.com'), '레드플랜트 합정본점', '서울시 광진구 건대입구 27-47', 37.544085, 127.068832, '02-3773-2246', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner159@test.com'), '츄로101 홍대본점', '서울시 용산구 이태원동 187-36', 37.539482, 126.995634, '02-7137-3759', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner015@test.com'), '더블유오앤', '서울시 성동구 성수동 32-5', 37.546298, 127.051518, '02-9020-3741', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner160@test.com'), '언플러그드 라운지', '서울시 광진구 건대입구 101-14', 37.535943, 127.069441, '02-9718-2330', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner161@test.com'), '젤라떼리아아라또', '서울시 송파구 잠실동 25-10', 37.514961, 127.09599, '02-2314-7865', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner162@test.com'), '작업실01', '서울시 강남구 신사동 111-24', 37.523872, 127.017988, '02-5166-4768', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner163@test.com'), '시타커피로스터스', '서울시 강남구 신사동 33-1', 37.524444, 127.023802, '02-7175-3077', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner164@test.com'), '카페카운티 홍대점', '서울시 강동구 천호동 140-49', 37.538543, 127.12147, '02-1691-6354', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner165@test.com'), 'NUHOOD', '서울시 강남구 신사동 82-7', 37.52099, 127.018659, '02-9637-5105', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner166@test.com'), '퍼스트커피랩 홍대점', '서울시 성동구 성수동 56-44', 37.544681, 127.059379, '02-4373-1723', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner167@test.com'), '멜로우', '서울시 은평구 불광동 127-40', 37.610529, 126.927891, '02-8014-3273', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner168@test.com'), '하이타운서울', '서울시 용산구 이태원동 92-17', 37.532602, 126.99304, '02-5938-5758', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner169@test.com'), '바나프레소 합정역점', '서울시 광진구 건대입구 5-35', 37.537612, 127.071845, '02-9676-2607', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner016@test.com'), '도란도란 2호점', '서울시 마포구 홍대입구 64-27', 37.553784, 126.921385, '02-5434-4354', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner170@test.com'), '로덴드론 동교 2호점', '서울시 강동구 천호동 15-22', 37.535982, 127.125954, '02-3476-9222', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner171@test.com'), '공룡빵공장', '서울시 성동구 성수동 81-16', 37.542547, 127.054004, '02-6257-1804', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner172@test.com'), '아이테르', '서울시 광진구 건대입구 194-46', 37.537931, 127.065127, '02-7676-6932', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner175@test.com'), '위드커피', '서울시 광진구 건대입구 107-18', 37.537568, 127.071472, '02-5474-7349', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner017@test.com'), '어반플랜트 합정', '서울시 종로구 익선동 161-7', 37.572031, 126.985195, '02-2616-6645', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner183@test.com'), '록집', '서울시 강동구 천호동 120-36', 37.538947, 127.126492, '02-2365-9947', SYSTIMESTAMP, 0, '02', 'APPROVED');

