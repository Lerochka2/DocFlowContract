INSERT INTO roles (code, name)
SELECT 'DOCUMENT_OPERATOR', 'Делопроизводитель'
WHERE NOT EXISTS (
    SELECT 1
    FROM roles
    WHERE code = 'DOCUMENT_OPERATOR'
);

UPDATE roles
SET name = 'Администратор системы'
WHERE code = 'ADMIN';

UPDATE roles
SET name = 'Менеджер по договорам'
WHERE code = 'CONTRACT_MANAGER';