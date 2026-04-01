-- Добавляем поле identifier в таблицу books
ALTER TABLE books ADD COLUMN identifier VARCHAR(255);

-- Делаем его уникальным
ALTER TABLE books ADD CONSTRAINT uk_books_identifier UNIQUE (identifier);

-- Создаем индекс для быстрого поиска
CREATE INDEX idx_books_identifier ON books(identifier);