-- Cafe Dummy Data

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner100@test.com'), '쏘스윗 홍대', '서울시 강남구 신사동 14-43', 37.5224, 127.017051, '02-7049-9311', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner101@test.com'), '콘하스 연남점', '서울시 용산구 이태원동 101-22', 37.532545, 126.998986, '02-6344-4359', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner102@test.com'), '설빙 홍대입구역점', '서울시 강남구 신사동 28-8', 37.52049, 127.025206, '02-4758-2603', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner103@test.com'), '내면의 발견', '서울시 용산구 이태원동 44-39', 37.53837, 126.997471, '02-7382-7054', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner104@test.com'), '17도씨', '서울시 서대문구 연희동 108-36', 37.573229, 126.94052, '02-9380-6744', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner105@test.com'), '이고 수플레 합정본점', '서울시 종로구 익선동 91-36', 37.578425, 126.985015, '02-3938-9618', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner106@test.com'), '배스킨라빈스 홍대상상마당', '서울시 강남구 신사동 52-38', 37.526105, 127.017114, '02-2559-7895', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner107@test.com'), '스타벅스 홍대입구역사거리R점', '서울시 송파구 잠실동 87-31', 37.513231, 127.097752, '02-3194-3046', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner108@test.com'), '오마이 메이드카페 오마이왕국점', '서울시 강동구 천호동 166-18', 37.538396, 127.120614, '02-4619-3032', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner109@test.com'), '메가MGC커피 홍대점', '서울시 마포구 연남동 5-13', 37.566805, 126.928483, '02-5519-1762', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner010@test.com'), '베이글랜드 홍대점', '서울시 은평구 불광동 49-13', 37.613641, 126.925431, '02-2612-4670', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner110@test.com'), '고망고 연남점', '서울시 성동구 성수동 51-36', 37.549309, 127.053737, '02-3332-5411', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner111@test.com'), '하우스키루', '서울시 광진구 건대입구 7-16', 37.5414, 127.070031, '02-1298-2593', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner112@test.com'), '훌리건 커피', '서울시 용산구 이태원동 42-1', 37.537174, 126.996752, '02-5596-6421', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner113@test.com'), '블루보틀 홍대 카페', '서울시 마포구 연남동 115-39', 37.565898, 126.925002, '02-8830-7588', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner114@test.com'), '낙랑파라', '서울시 종로구 익선동 132-22', 37.574245, 126.993318, '02-3071-9931', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner115@test.com'), '고망고 홍대입구역점', '서울시 용산구 이태원동 153-47', 37.535959, 126.994889, '02-5452-3243', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner116@test.com'), '눈내리는숲, 설래임 홍대본점', '서울시 송파구 잠실동 50-3', 37.51225, 127.095311, '02-1188-2853', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner117@test.com'), '주디마리 홍대점', '서울시 송파구 잠실동 20-38', 37.508359, 127.102111, '02-2309-3778', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner118@test.com'), '바이앤드커피', '서울시 마포구 연남동 29-45', 37.567679, 126.922802, '02-9236-3636', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner119@test.com'), '빨콩커피 연남점', '서울시 성동구 성수동 139-10', 37.543083, 127.052557, '02-5609-1967', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner011@test.com'), '올드페리도넛 연남점', '서울시 영등포구 여의도동 8-25', 37.524299, 126.926841, '02-2802-8405', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner120@test.com'), '미스터크레페', '서울시 송파구 잠실동 132-17', 37.509541, 127.095533, '02-1988-3079', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner121@test.com'), '올라운드커피', '서울시 종로구 익선동 99-2', 37.57481, 126.991236, '02-1966-2459', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner122@test.com'), '던킨 홍대역점', '서울시 영등포구 여의도동 27-35', 37.526135, 126.919409, '02-4972-1807', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner123@test.com'), '도덕과 규범', '서울시 용산구 이태원동 6-44', 37.534532, 126.998829, '02-6405-3966', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner124@test.com'), '할리스 연트럴파크점', '서울시 성동구 성수동 14-19', 37.547486, 127.055993, '02-6552-5289', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner125@test.com'), '마인드 비', '서울시 성동구 성수동 181-21', 37.548774, 127.057621, '02-3980-8599', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner126@test.com'), '디저트39 합정역점', '서울시 송파구 잠실동 113-50', 37.516033, 127.102421, '02-8003-7405', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner127@test.com'), '아우어베이커리 신촌숲길점', '서울시 마포구 홍대입구 150-48', 37.555932, 126.922415, '02-7240-3126', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner128@test.com'), '스타벅스 서교동사거리점', '서울시 은평구 불광동 120-35', 37.607737, 126.923347, '02-3201-2587', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner129@test.com'), '오디너리 아카이브', '서울시 영등포구 여의도동 14-47', 37.517986, 126.921281, '02-2291-8579', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner012@test.com'), '도란도란 1호점', '서울시 송파구 잠실동 96-17', 37.516964, 127.095106, '02-4541-7523', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner130@test.com'), '로파이', '서울시 영등포구 여의도동 98-48', 37.523916, 126.922241, '02-7480-6970', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner131@test.com'), '테라로사 홍대서교점', '서울시 강동구 천호동 106-49', 37.534594, 127.12389, '02-8797-7309', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner132@test.com'), '수택', '서울시 종로구 익선동 95-10', 37.576227, 126.990389, '02-7583-5158', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner133@test.com'), '커피랩스로스터리 합정점', '서울시 성동구 성수동 28-47', 37.546014, 127.060507, '02-3551-3456', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner134@test.com'), '금옥당 서교점', '서울시 광진구 건대입구 146-49', 37.542985, 127.072783, '02-8880-5864', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner135@test.com'), '커피덕', '서울시 성동구 성수동 194-50', 37.540428, 127.054132, '02-5040-9419', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner136@test.com'), '카페 호기심', '서울시 용산구 이태원동 69-25', 37.533559, 126.994114, '02-2347-3270', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner137@test.com'), '완포 코리아', '서울시 영등포구 여의도동 174-45', 37.519106, 126.921253, '02-8096-9810', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner138@test.com'), '사워도우 랩', '서울시 송파구 잠실동 140-5', 37.50976, 127.097093, '02-2541-2090', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner139@test.com'), '삼다코지', '서울시 송파구 잠실동 113-5', 37.518142, 127.103918, '02-2118-7303', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner013@test.com'), '로아프하우스', '서울시 마포구 홍대입구 158-22', 37.561164, 126.926787, '02-4627-7616', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner140@test.com'), '디저트 머라이언', '서울시 광진구 건대입구 198-23', 37.542582, 127.069895, '02-9488-7442', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner141@test.com'), '카페레터 합정', '서울시 용산구 이태원동 200-36', 37.536864, 126.994171, '02-3930-8563', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner142@test.com'), '커피빈 홍대역점', '서울시 광진구 건대입구 1-15', 37.537381, 127.071424, '02-2587-5014', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner143@test.com'), '디스코플래닛', '서울시 종로구 익선동 115-50', 37.569128, 126.992055, '02-3889-8462', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner144@test.com'), '요고 프로즌요거트 홍대점', '서울시 영등포구 여의도동 31-36', 37.52067, 126.921743, '02-7899-5966', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner145@test.com'), '루치펠 대저택', '서울시 마포구 홍대입구 127-45', 37.55678, 126.926674, '02-4708-9248', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner146@test.com'), '베어글스 홍대상수', '서울시 성동구 성수동 68-4', 37.540946, 127.058611, '02-4978-5929', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner147@test.com'), '모펀 AK PLAZA 홍대점', '서울시 마포구 홍대입구 52-44', 37.552504, 126.928765, '02-1125-6846', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner148@test.com'), '카페 장쌤', '서울시 용산구 이태원동 99-3', 37.539449, 126.997608, '02-4987-4422', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner149@test.com'), '달화채 홍대점', '서울시 마포구 홍대입구 35-35', 37.553896, 126.921662, '02-6058-5700', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner014@test.com'), '브라운하우스 연남', '서울시 강남구 신사동 33-22', 37.526394, 127.017675, '02-6792-5699', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner150@test.com'), '일쩜오플로어', '서울시 영등포구 여의도동 159-36', 37.522687, 126.923851, '02-3732-9137', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner151@test.com'), '크림온케이크', '서울시 서대문구 연희동 185-21', 37.566526, 126.935212, '02-5737-5319', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner152@test.com'), '쿠리노키제빵', '서울시 성동구 성수동 181-31', 37.547987, 127.050966, '02-4677-6584', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner153@test.com'), '오버트 합정', '서울시 은평구 불광동 11-41', 37.605421, 126.92796, '02-7837-2769', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner154@test.com'), '월페이퍼 서교', '서울시 영등포구 여의도동 90-43', 37.526483, 126.928236, '02-6578-1022', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner155@test.com'), '할리스 홍대거리점', '서울시 종로구 익선동 13-33', 37.57631, 126.986973, '02-1189-5904', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner156@test.com'), '드롭탑 홍대점', '서울시 광진구 건대입구 71-34', 37.536772, 127.072618, '02-6998-6413', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner157@test.com'), '슬릿 홍대', '서울시 종로구 익선동 153-31', 37.573192, 126.985318, '02-6665-8674', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner158@test.com'), '레드플랜트 합정본점', '서울시 광진구 건대입구 27-47', 37.544085, 127.068832, '02-3773-2246', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner159@test.com'), '츄로101 홍대본점', '서울시 용산구 이태원동 187-36', 37.539482, 126.995634, '02-7137-3759', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner015@test.com'), '더블유오앤', '서울시 성동구 성수동 32-5', 37.546298, 127.051518, '02-9020-3741', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner160@test.com'), '언플러그드 라운지', '서울시 광진구 건대입구 101-14', 37.535943, 127.069441, '02-9718-2330', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner161@test.com'), '젤라떼리아아라또', '서울시 송파구 잠실동 25-10', 37.514961, 127.09599, '02-2314-7865', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner162@test.com'), '작업실01', '서울시 강남구 신사동 111-24', 37.523872, 127.017988, '02-5166-4768', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner163@test.com'), '시타커피로스터스', '서울시 강남구 신사동 33-1', 37.524444, 127.023802, '02-7175-3077', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner164@test.com'), '카페카운티 홍대점', '서울시 강동구 천호동 140-49', 37.538543, 127.12147, '02-1691-6354', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner165@test.com'), 'NUHOOD', '서울시 강남구 신사동 82-7', 37.52099, 127.018659, '02-9637-5105', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner166@test.com'), '퍼스트커피랩 홍대점', '서울시 성동구 성수동 56-44', 37.544681, 127.059379, '02-4373-1723', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner167@test.com'), '멜로우', '서울시 은평구 불광동 127-40', 37.610529, 126.927891, '02-8014-3273', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner168@test.com'), '하이타운서울', '서울시 용산구 이태원동 92-17', 37.532602, 126.99304, '02-5938-5758', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner169@test.com'), '바나프레소 합정역점', '서울시 광진구 건대입구 5-35', 37.537612, 127.071845, '02-9676-2607', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner016@test.com'), '도란도란 2호점', '서울시 마포구 홍대입구 64-27', 37.553784, 126.921385, '02-5434-4354', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner170@test.com'), '로덴드론 동교 2호점', '서울시 강동구 천호동 15-22', 37.535982, 127.125954, '02-3476-9222', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner171@test.com'), '공룡빵공장', '서울시 성동구 성수동 81-16', 37.542547, 127.054004, '02-6257-1804', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner172@test.com'), '아이테르', '서울시 광진구 건대입구 194-46', 37.537931, 127.065127, '02-7676-6932', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner175@test.com'), '위드커피', '서울시 광진구 건대입구 107-18', 37.537568, 127.071472, '02-5474-7349', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner017@test.com'), '어반플랜트 합정', '서울시 종로구 익선동 161-7', 37.572031, 126.985195, '02-2616-6645', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner183@test.com'), '록집', '서울시 강동구 천호동 120-36', 37.538947, 127.126492, '02-2365-9947', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner184@test.com'), '민트하임', '서울시 강동구 천호동 4-5', 37.533508, 127.121625, '02-3834-7217', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner185@test.com'), '케이크예스플리즈', '서울시 종로구 익선동 22-30', 37.575357, 126.990127, '02-2861-4213', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner186@test.com'), '합정카페 웨하로스터스', '서울시 송파구 잠실동 133-29', 37.513725, 127.095642, '02-7665-4822', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner187@test.com'), '인생크레페', '서울시 서대문구 연희동 167-14', 37.568709, 126.938587, '02-3391-4511', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner188@test.com'), '대충유원지', '서울시 종로구 익선동 19-10', 37.575772, 126.994383, '02-8747-2730', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner189@test.com'), '투썸플레이스 동교동삼거리점', '서울시 종로구 익선동 97-24', 37.577015, 126.994788, '02-3284-3868', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner018@test.com'), '꾸울과자점', '서울시 강남구 신사동 183-6', 37.525145, 127.021212, '02-2913-9165', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner190@test.com'), '투썸플레이스 홍대걷고싶은거리점', '서울시 광진구 건대입구 77-14', 37.54478, 127.072446, '02-4635-8800', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner191@test.com'), '카페그랑주', '서울시 송파구 잠실동 51-1', 37.516911, 127.104922, '02-1143-6594', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner192@test.com'), '모코모코', '서울시 강남구 신사동 52-32', 37.525473, 127.024746, '02-8540-8297', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner193@test.com'), '몰리스피크닉', '서울시 마포구 연남동 63-23', 37.561481, 126.922328, '02-8546-5660', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner196@test.com'), '와플대학 홍대캠퍼스', '서울시 영등포구 여의도동 120-45', 37.517722, 126.922553, '02-4763-4662', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner019@test.com'), '커피폴리', '서울시 강동구 천호동 69-5', 37.539259, 127.12558, '02-1824-5030', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner001@test.com'), '바나프레소 홍대입구역사거리점', '서울시 성동구 성수동 178-30', 37.547533, 127.053646, '02-5364-8467', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner200@test.com'), '빵나무', '서울시 송파구 잠실동 169-43', 37.517672, 127.102121, '02-2420-7086', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner203@test.com'), '나와 나타샤', '서울시 광진구 건대입구 57-35', 37.540945, 127.072007, '02-4693-3151', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner204@test.com'), '하트클립', '서울시 마포구 연남동 167-33', 37.569786, 126.920858, '02-3235-3089', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner205@test.com'), '조블라 홍대점', '서울시 강남구 신사동 45-5', 37.518998, 127.025064, '02-2615-7378', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner206@test.com'), '케이크 밀쿠', '서울시 은평구 불광동 86-37', 37.613545, 126.923623, '02-8610-4057', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner207@test.com'), '우마이 당고', '서울시 종로구 익선동 33-9', 37.568931, 126.99232, '02-4073-7064', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner208@test.com'), '소림커피', '서울시 송파구 잠실동 17-5', 37.5103, 127.09892, '02-3550-5097', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner209@test.com'), '메가MGC커피 동교삼거리점', '서울시 강동구 천호동 16-9', 37.542593, 127.126552, '02-4879-8268', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner020@test.com'), '올루올루 홍대점', '서울시 광진구 건대입구 46-21', 37.545198, 127.072103, '02-7205-8770', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner210@test.com'), '카페 꼬모', '서울시 강남구 신사동 183-11', 37.522299, 127.025378, '02-1921-9507', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner211@test.com'), '먼로', '서울시 강남구 신사동 75-7', 37.522467, 127.02244, '02-8739-5196', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner212@test.com'), '메가MGC커피 창천점', '서울시 광진구 건대입구 61-6', 37.543922, 127.070905, '02-8408-7896', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner213@test.com'), '컴포즈커피 홍대동교점', '서울시 송파구 잠실동 12-24', 37.512622, 127.101727, '02-5081-9229', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner214@test.com'), '토커바웃커피 브루어스', '서울시 마포구 홍대입구 97-32', 37.552976, 126.92797, '02-3551-5459', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner215@test.com'), '카페요아정 홍대점', '서울시 은평구 불광동 34-29', 37.613079, 126.931783, '02-8442-3182', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner216@test.com'), '카페라래', '서울시 강동구 천호동 135-41', 37.539597, 127.120524, '02-9107-4686', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner217@test.com'), '오스카 커피부스', '서울시 은평구 불광동 148-30', 37.60556, 126.924164, '02-3101-5283', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner218@test.com'), '스튜디오앱트 STUDIOAPT', '서울시 강남구 신사동 117-8', 37.526042, 127.023866, '02-9387-7968', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner219@test.com'), '더웨이브라운지 후카바', '서울시 마포구 홍대입구 40-16', 37.560599, 126.922862, '02-8438-9562', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner021@test.com'), '아날로그가든', '서울시 은평구 불광동 58-10', 37.611999, 126.922282, '02-3844-2030', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner220@test.com'), '케이클링', '서울시 마포구 홍대입구 129-25', 37.558925, 126.926168, '02-5817-4941', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner221@test.com'), '사계의숲', '서울시 강남구 신사동 21-24', 37.524544, 127.02405, '02-7387-1576', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner222@test.com'), '카페 홀리데이', '서울시 서대문구 연희동 112-12', 37.572052, 126.936025, '02-3670-1275', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner223@test.com'), '미크플로 연트럴파크점', '서울시 강남구 신사동 105-11', 37.525713, 127.02019, '02-8574-1886', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner224@test.com'), '더베이크 홍대피규어프레소FP점', '서울시 영등포구 여의도동 145-20', 37.519357, 126.92665, '02-8202-1216', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner225@test.com'), '카페 이유', '서울시 송파구 잠실동 199-45', 37.509934, 127.105086, '02-8726-7534', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner226@test.com'), '할리스 홍대역점', '서울시 은평구 불광동 52-32', 37.606298, 126.924918, '02-7569-3235', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner227@test.com'), '센트플로우 커피 로스터리', '서울시 마포구 홍대입구 149-39', 37.556989, 126.923377, '02-5831-4423', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner228@test.com'), '어피스오브', '서울시 영등포구 여의도동 95-39', 37.525666, 126.925586, '02-8288-7299', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner229@test.com'), '도트블랭킷 연남점', '서울시 광진구 건대입구 197-6', 37.539492, 127.06784, '02-3076-8104', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner022@test.com'), '렉터스라운지 홍대', '서울시 광진구 건대입구 34-42', 37.542445, 127.066468, '02-7891-4107', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner230@test.com'), '써니데이 인 잔다리', '서울시 송파구 잠실동 54-40', 37.5131, 127.101215, '02-9219-8820', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner231@test.com'), '코코로카라', '서울시 송파구 잠실동 145-5', 37.509193, 127.104817, '02-8228-1389', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner232@test.com'), '카페리프 서교점', '서울시 은평구 불광동 12-42', 37.612686, 126.930719, '02-7222-2716', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner233@test.com'), '라포레스타', '서울시 영등포구 여의도동 43-9', 37.521045, 126.927869, '02-6800-9551', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner234@test.com'), '투니크 홍대점', '서울시 마포구 홍대입구 162-33', 37.561629, 126.927884, '02-5512-2942', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner235@test.com'), '스트레인지프룻', '서울시 용산구 이태원동 169-35', 37.530635, 126.995543, '02-4940-8170', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner023@test.com'), '라헬의부엌 홍대점', '서울시 마포구 연남동 52-30', 37.5663, 126.920387, '02-9793-2133', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner243@test.com'), '튜토리얼', '서울시 용산구 이태원동 106-46', 37.539476, 126.994113, '02-1936-4311', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner244@test.com'), '차곡파티세리 연남점', '서울시 영등포구 여의도동 69-24', 37.517013, 126.920095, '02-5484-9037', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner245@test.com'), '스테레오포닉사운드', '서울시 광진구 건대입구 128-1', 37.538627, 127.070075, '02-4649-7346', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner246@test.com'), '아이니드케이크 홍대점', '서울시 광진구 건대입구 133-41', 37.539586, 127.070811, '02-5813-7628', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner247@test.com'), '알 누오보', '서울시 강남구 신사동 192-13', 37.525255, 127.018409, '02-4983-7445', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner248@test.com'), '뮤즈 시노님', '서울시 성동구 성수동 183-46', 37.540682, 127.056632, '02-6782-7527', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner024@test.com'), '943 킹스크로스', '서울시 종로구 익선동 195-16', 37.572169, 126.993751, '02-9701-7151', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner025@test.com'), '코리코카페 연남점', '서울시 강동구 천호동 166-43', 37.537052, 127.122253, '02-4546-5505', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner026@test.com'), '무슈랑', '서울시 성동구 성수동 28-30', 37.544583, 127.051156, '02-7314-8122', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner027@test.com'), '베리블리스', '서울시 영등포구 여의도동 120-49', 37.520273, 126.919757, '02-5147-4696', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner028@test.com'), '침니맨션', '서울시 마포구 홍대입구 45-8', 37.552812, 126.922353, '02-7466-4514', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner029@test.com'), '홍대카페', '서울시 마포구 연남동 48-37', 37.563438, 126.92664, '02-1382-3148', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner002@test.com'), '리밀앤밀리', '서울시 영등포구 여의도동 50-48', 37.521282, 126.920803, '02-9679-3780', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner030@test.com'), '차백도 홍대1호점', '서울시 은평구 불광동 23-12', 37.61114, 126.92291, '02-8912-1061', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner031@test.com'), '커피벌스데이', '서울시 강동구 천호동 177-12', 37.536514, 127.120236, '02-1687-7554', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner032@test.com'), '발코니가든', '서울시 서대문구 연희동 199-4', 37.56766, 126.943909, '02-7604-6065', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner033@test.com'), '베어스덴베이커리', '서울시 서대문구 연희동 37-49', 37.563709, 126.939251, '02-8329-5677', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner034@test.com'), '카페공간 홍대점', '서울시 은평구 불광동 149-28', 37.606171, 126.926019, '02-7446-7068', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner035@test.com'), '카페345', '서울시 광진구 건대입구 71-7', 37.544467, 127.066702, '02-4755-9624', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner036@test.com'), '씨더라이트', '서울시 은평구 불광동 107-31', 37.612727, 126.922191, '02-9862-9998', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner037@test.com'), '포이지아', '서울시 영등포구 여의도동 46-41', 37.525707, 126.924706, '02-6075-6390', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner038@test.com'), '언더랩 연남점', '서울시 송파구 잠실동 39-2', 37.513242, 127.098812, '02-5607-5341', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner039@test.com'), '어 슬라이스', '서울시 용산구 이태원동 64-8', 37.533251, 126.99878, '02-2438-7667', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner003@test.com'), '공미학 마포홍대점', '서울시 광진구 건대입구 104-4', 37.537167, 127.070376, '02-4785-3006', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner040@test.com'), '멧커피 홍대점', '서울시 마포구 연남동 84-12', 37.567196, 126.929483, '02-9182-1531', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner041@test.com'), '피오니', '서울시 종로구 익선동 173-36', 37.578027, 126.987497, '02-1319-3405', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner042@test.com'), '저스트단비 크레페 홍대점', '서울시 강동구 천호동 105-42', 37.542912, 127.124916, '02-2944-3633', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner043@test.com'), '자연도소금빵in연남', '서울시 송파구 잠실동 156-19', 37.513754, 127.103439, '02-9831-4492', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner044@test.com'), 'HEYTEA 홍대점', '서울시 광진구 건대입구 20-34', 37.537397, 127.072592, '02-2121-8233', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner045@test.com'), '콤파일', '서울시 마포구 홍대입구 180-50', 37.552457, 126.927898, '02-5168-1741', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner046@test.com'), '푸글렌 서울', '서울시 은평구 불광동 5-20', 37.607149, 126.929205, '02-8928-1470', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner047@test.com'), '메가MGC커피 홍대L7점', '서울시 강동구 천호동 150-21', 37.542648, 127.127107, '02-9047-4817', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner048@test.com'), '카페꼼마 홍대점', '서울시 영등포구 여의도동 68-14', 37.522186, 126.928103, '02-8625-5505', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner049@test.com'), '미라지커피', '서울시 은평구 불광동 106-11', 37.606661, 126.92721, '02-4367-9803', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner004@test.com'), '비포블루밍', '서울시 종로구 익선동 130-24', 37.577123, 126.986948, '02-9171-7384', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner050@test.com'), '미니멀비', '서울시 종로구 익선동 119-22', 37.574014, 126.986964, '02-3595-7830', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner051@test.com'), '해피베어데이 합정 본점', '서울시 은평구 불광동 17-35', 37.606977, 126.929852, '02-7880-2681', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner052@test.com'), '합정 지튼', '서울시 은평구 불광동 182-42', 37.611253, 126.928839, '02-9059-6271', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner053@test.com'), '모멘트커피 2호점', '서울시 은평구 불광동 46-20', 37.614778, 126.922066, '02-7921-5267', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner054@test.com'), '벤스쿠키 연남점', '서울시 종로구 익선동 130-47', 37.57735, 126.989272, '02-6583-2875', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner055@test.com'), '소각커피', '서울시 용산구 이태원동 32-13', 37.532983, 126.991763, '02-3383-5748', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner056@test.com'), '마뽀즈 비건케이크', '서울시 강남구 신사동 44-18', 37.519289, 127.018632, '02-1653-9357', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner057@test.com'), '숲다방', '서울시 마포구 연남동 43-38', 37.568175, 126.926973, '02-3525-6007', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner058@test.com'), '할리스 홍대역2번출구점', '서울시 은평구 불광동 124-41', 37.607729, 126.930646, '02-6873-7737', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner059@test.com'), '1984', '서울시 종로구 익선동 169-38', 37.577699, 126.992657, '02-4593-4359', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner005@test.com'), '유포테이블카페&마치아소비', '서울시 용산구 이태원동 111-15', 37.538823, 126.996266, '02-3496-3634', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner060@test.com'), '머씨커피', '서울시 종로구 익선동 187-20', 37.568839, 126.99343, '02-1122-2405', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner061@test.com'), '마지텐시 메이드카페', '서울시 광진구 건대입구 143-26', 37.54456, 127.067851, '02-6956-4392', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner062@test.com'), '언플러그드 홍대', '서울시 영등포구 여의도동 151-30', 37.521561, 126.923872, '02-2210-9341', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner063@test.com'), '라운지 클라리멘토', '서울시 강동구 천호동 50-14', 37.542199, 127.125535, '02-8425-2867', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner064@test.com'), '말릭커피', '서울시 서대문구 연희동 172-24', 37.567637, 126.93661, '02-9522-8137', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner065@test.com'), '요거트월드 홍대직영점', '서울시 성동구 성수동 25-36', 37.540986, 127.058076, '02-1752-5151', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner066@test.com'), '티와이티디', '서울시 마포구 연남동 43-6', 37.56298, 126.922092, '02-4510-9226', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner067@test.com'), '카페 드 래빗', '서울시 종로구 익선동 110-33', 37.578406, 126.988564, '02-3838-2594', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner068@test.com'), '투썸플레이스 홍대서교점', '서울시 마포구 홍대입구 59-20', 37.557997, 126.924066, '02-1991-5917', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner069@test.com'), '투썸플레이스 홍대예술의거리점', '서울시 용산구 이태원동 132-29', 37.539171, 126.998662, '02-3046-8027', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner006@test.com'), '카페 공명 연남점', '서울시 마포구 홍대입구 83-43', 37.555829, 126.925744, '02-3989-9566', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner070@test.com'), '더그릭베어 홍대상수점', '서울시 마포구 연남동 142-40', 37.568876, 126.926521, '02-8287-7643', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner071@test.com'), '해피베어데이 상수점', '서울시 강동구 천호동 139-16', 37.536931, 127.121143, '02-3163-9883', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner072@test.com'), '녹턴', '서울시 강동구 천호동 186-14', 37.539506, 127.127498, '02-7582-9289', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner073@test.com'), '메이드카페 집사카페 카와이 홍대점', '서울시 은평구 불광동 91-23', 37.606125, 126.924819, '02-5212-8776', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner074@test.com'), '79파운야드 합정점', '서울시 강동구 천호동 43-43', 37.539278, 127.119617, '02-9188-6663', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner075@test.com'), '투썸플레이스 홍대입구역점', '서울시 광진구 건대입구 106-28', 37.545195, 127.072669, '02-8959-8259', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner076@test.com'), '콜린스커피', '서울시 강남구 신사동 145-13', 37.519831, 127.022778, '02-4820-9677', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner077@test.com'), '달콤한거짓말', '서울시 영등포구 여의도동 84-29', 37.525046, 126.922155, '02-8146-9549', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner078@test.com'), '작당모의', '서울시 종로구 익선동 134-14', 37.568804, 126.988361, '02-1154-5802', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner079@test.com'), '이미커피', '서울시 성동구 성수동 23-35', 37.541645, 127.059983, '02-5706-5352', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner007@test.com'), '카페 공명 홍대점', '서울시 강남구 신사동 176-44', 37.525668, 127.020955, '02-5560-3050', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner080@test.com'), '팻어케이크 연남점', '서울시 용산구 이태원동 35-17', 37.536517, 126.990595, '02-8473-8542', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner081@test.com'), '내맘대로폰케이스 홍대점', '서울시 성동구 성수동 30-44', 37.539686, 127.055574, '02-6096-3823', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner082@test.com'), '스타벅스 홍대동교점', '서울시 마포구 연남동 195-37', 37.568454, 126.92566, '02-8114-7778', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner083@test.com'), '아이덴티티커피랩', '서울시 영등포구 여의도동 75-32', 37.521656, 126.91886, '02-2583-4623', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner084@test.com'), '애니메이트 카페 홍대점', '서울시 마포구 홍대입구 56-43', 37.557396, 126.927642, '02-2563-6311', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner085@test.com'), '어반스페이스드로잉카페 앤 펍', '서울시 은평구 불광동 165-47', 37.611139, 126.923039, '02-9230-3653', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner086@test.com'), '카페 사운드웨이브 합정점', '서울시 강동구 천호동 80-4', 37.536218, 127.122391, '02-8706-2109', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner087@test.com'), '컴포즈커피 홍대삼거리점', '서울시 강동구 천호동 165-22', 37.536874, 127.121079, '02-4990-2512', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner088@test.com'), '공상온도', '서울시 광진구 건대입구 173-46', 37.538907, 127.069468, '02-2811-1391', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner089@test.com'), '엔제리너스 L7홍대점', '서울시 은평구 불광동 196-6', 37.612319, 126.929098, '02-8866-5967', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner008@test.com'), '아벨롭 홍대본점', '서울시 영등포구 여의도동 164-40', 37.51679, 126.925255, '02-3338-2376', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner090@test.com'), '무소식', '서울시 마포구 홍대입구 53-4', 37.555022, 126.924166, '02-1247-7353', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner091@test.com'), '메가MGC커피 합정중앙점', '서울시 강동구 천호동 2-36', 37.542761, 127.128509, '02-7694-9766', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner092@test.com'), '더 누크', '서울시 용산구 이태원동 133-39', 37.531175, 126.997466, '02-5084-4868', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner093@test.com'), '메가MGC커피 동교점', '서울시 강동구 천호동 198-50', 37.538833, 127.127996, '02-7647-4704', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner094@test.com'), '푸하하크림빵', '서울시 강남구 신사동 6-14', 37.524609, 127.016431, '02-6864-9845', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner095@test.com'), '김진환 제과점', '서울시 서대문구 연희동 1-33', 37.569676, 126.935192, '02-5375-7540', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner096@test.com'), '이혜와 서울', '서울시 송파구 잠실동 96-46', 37.516258, 127.096355, '02-7673-5536', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner097@test.com'), '엘로 커피바', '서울시 은평구 불광동 13-35', 37.605097, 126.927607, '02-4845-8550', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner098@test.com'), '시나모롤 스위트카페', '서울시 마포구 연남동 27-48', 37.564384, 126.926808, '02-2642-2635', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner099@test.com'), '비하인드', '서울시 종로구 익선동 69-50', 37.577893, 126.988968, '02-7158-8241', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner009@test.com'), '베니케이크', '서울시 광진구 건대입구 74-11', 37.538103, 127.068708, '02-8527-3278', CURRENT_TIMESTAMP, 0, '02', 'APPROVED');

