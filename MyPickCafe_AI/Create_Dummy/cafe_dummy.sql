-- Cafe Dummy Data

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner100@test.com'), '쏘스윗 홍대', '서울시 강남구 신사동 14-43', 37.5224, 127.017051, '02-7049-9311', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner101@test.com'), '콘하스 연남점', '서울시 용산구 이태원동 101-22', 37.532545, 126.998986, '02-6344-4359', SYSTIMESTAMP, 0, '02', 'APPROVED');

INSERT INTO cafe (owner_id, name, address, lat, lon, phone, registered_at, views, code, status) VALUES ((SELECT member_id FROM member WHERE email = 'owner102@test.com'), '설빙 홍대입구역점', '서울시 강남구 신사동 28-8', 37.52049, 127.025206, '02-4758-2603', SYSTIMESTAMP, 0, '02', 'APPROVED');

