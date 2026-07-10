INSERT INTO mission (id, title, difficulty, is_active, created_at, updated_at)
VALUES (1, '자리에서 일어나 물 한 잔 마시기', 'EASY', TRUE, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

INSERT INTO mission (id, title, difficulty, is_active, created_at, updated_at)
VALUES (2, '5분간 가벼운 스트레칭 하기', 'EASY', TRUE, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

INSERT INTO mission (id, title, difficulty, is_active, created_at, updated_at)
VALUES (3, '눈을 감고 1분간 깊게 호흡하기', 'EASY', TRUE, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- 시퀀스 최신화 (다음 auto-increment 시 primary key 충돌 방지)
SELECT setval(pg_get_serial_sequence('mission', 'id'), COALESCE(MAX(id), 1)) FROM mission;
