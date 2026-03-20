-- V2: 최적화 인덱스 추가
-- 시나리오 1: Full Table Scan 개선 — users.name 인덱스
CREATE INDEX idx_users_name ON users(name);

-- 시나리오 2: 인덱스 무력화 개선 — orders.created_at 인덱스
CREATE INDEX idx_orders_created_at ON orders(created_at);

-- 시나리오 4: 복합 인덱스 순서 개선 — logs(status, created_at) 복합 인덱스
CREATE INDEX idx_logs_status_created_at ON logs(status, created_at);
