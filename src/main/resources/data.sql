-- 미션 초기 데이터. spring.sql.init.mode=always 로 매 기동 시 실행된다.
-- ON CONFLICT (id) DO NOTHING 이라 여러 번 실행돼도 중복/덮어쓰기가 없다.
INSERT INTO mission (id, title, difficulty, is_active, created_at, updated_at) VALUES
  (1,  '침대에서 일어나 방문 앞까지 가기',        'EASY', TRUE, NOW(), NOW()),
  (2,  '방 한 바퀴 걷기',                        'EASY', TRUE, NOW(), NOW()),
  (3,  '주방으로 이동하기',                      'EASY', TRUE, NOW(), NOW()),
  (4,  '거실 소파까지 이동하기',                  'EASY', TRUE, NOW(), NOW()),
  (5,  '다른 방 가기',                          'EASY', TRUE, NOW(), NOW()),
  (6,  '책상으로 이동하기',                      'EASY', TRUE, NOW(), NOW()),
  (7,  '집 안에서 가장 좋아하는 공간 찾아가기',    'EASY', TRUE, NOW(), NOW()),
  (8,  '현관문 손잡이 잡기',                      'EASY', TRUE, NOW(), NOW()),
  (9,  '책상 위 물건 하나 정리하기',              'EASY', TRUE, NOW(), NOW()),
  (10, '냉장고 문 열었다 닫기',                   'EASY', TRUE, NOW(), NOW()),
  (11, '컵을 제자리에 두기',                      'EASY', TRUE, NOW(), NOW()),
  (12, '다른 방 가서 창문 열기',                  'EASY', TRUE, NOW(), NOW()),
  (13, '거실 조명 켜기',                         'EASY', TRUE, NOW(), NOW()),
  (14, '신발 한 짝 정리하기',                     'EASY', TRUE, NOW(), NOW()),
  (15, '물 한 컵 마시기',                        'EASY', TRUE, NOW(), NOW()),
  (16, '손 씻기',                               'EASY', TRUE, NOW(), NOW()),
  (17, '양치하기',                              'EASY', TRUE, NOW(), NOW()),
  (18, '로션 바르기',                           'EASY', TRUE, NOW(), NOW()),
  (19, '앉았다 일어났다 2번',                     'EASY', TRUE, NOW(), NOW()),
  (20, '발끝 터치 스트레칭',                      'EASY', TRUE, NOW(), NOW()),
  (21, '가벼운 스트레칭',                        'EASY', TRUE, NOW(), NOW()),
  (22, '기지개 켜기',                           'EASY', TRUE, NOW(), NOW()),
  (23, '좋아하는 물건 만지고 오기',              'EASY', TRUE, NOW(), NOW()),
  (24, '좋아하는 옷 찾아보기',                    'EASY', TRUE, NOW(), NOW()),
  (25, '내일 입을 옷 미리 꺼내기',               'EASY', TRUE, NOW(), NOW()),
  (26, '오늘 기분과 어울리는 옷 고르기',          'EASY', TRUE, NOW(), NOW()),
  (27, '책 펼치고 3초 독서',                     'EASY', TRUE, NOW(), NOW()),
  (28, '베란다 바깥 풍경 보기',                   'EASY', TRUE, NOW(), NOW()),
  (29, '가족에게 인사하기',                      'EASY', TRUE, NOW(), NOW()),
  (30, '화장실 거울 보고 웃기',                   'EASY', TRUE, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 명시적 id 삽입 후 시퀀스를 최댓값으로 맞춘다 (이후 앱이 만드는 미션의 PK 충돌 방지).
SELECT setval(pg_get_serial_sequence('mission', 'id'), COALESCE((SELECT MAX(id) FROM mission), 1));
