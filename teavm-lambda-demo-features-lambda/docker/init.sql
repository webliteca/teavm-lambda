CREATE TABLE IF NOT EXISTS items (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    quantity INTEGER NOT NULL DEFAULT 0
);

INSERT INTO items (name, description, quantity) VALUES
    ('Widget', 'A standard widget', 10),
    ('Gadget', 'A fancy gadget', 5);
