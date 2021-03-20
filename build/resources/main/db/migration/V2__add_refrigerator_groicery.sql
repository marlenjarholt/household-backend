
CREATE TABLE refrigerators(
    id UUID PRIMARY KEY
);

CREATE TABLE groceries(
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    amount REAL NOT NULL,
    unit VARCHAR(255) NOT NULL
);

CREATE TABLE refrigerator_grocery_relation(
    refrigerator_id UUID,
    CONSTRAINT fk_refrigerator FOREIGN KEY(refrigerator_id) REFERENCES refrigerators(id),
    grocery_id UUID,
    CONSTRAINT fk_grocery FOREIGN KEY(grocery_id) REFERENCES groceries(id),
    CONSTRAINT id PRIMARY KEY(refrigerator_id, grocery_id)
);

DELETE FROM user_household_relation;
DELETE FROM users;
DELETE FROM households;


ALTER TABLE users
ADD COLUMN hash VARCHAR(255) NOT NULL,
ADD COLUMN salt VARCHAR(255) NOT NULL;

ALTER TABLE households
ADD COLUMN refrigerator_id UUID,
ADD CONSTRAINT fk_refrigerator FOREIGN KEY(refrigerator_id) REFERENCES refrigerators(id);

INSERT INTO users VALUES('e5310fb5-4e47-4e59-8664-33f1ce66232a', 'marly.marlen@gmail.com', 'Marlen Jarholt', '5WkkGjbFBqcG9HMi4WTQSrrmAQdFjFRCByPQt6IIQH8=', 'K5vaDTLwxPMHG7LdSSoyNaFVQNMHx5w2');
INSERT INTO users VALUES('b0acc769-6b96-4a6b-8d75-34d91675d002', 'mrsimenfonnes@gmail.com', 'Simen Fonnes', '5WkkGjbFBqcG9HMi4WTQSrrmAQdFjFRCByPQt6IIQH8=', 'K5vaDTLwxPMHG7LdSSoyNaFVQNMHx5w2');

INSERT INTO refrigerators VALUES('c87366e9-ad96-4ffe-9f8f-2b19c3736187');
INSERT INTO groceries VALUES('d9568894-11ed-4c01-9064-ede057626536', 'Melk', 3.5, 'dl');

INSERT INTO refrigerator_grocery_relation VALUES('c87366e9-ad96-4ffe-9f8f-2b19c3736187', 'd9568894-11ed-4c01-9064-ede057626536');

INSERT INTO households VALUES('42269d17-77cf-48a5-bf37-b1524fa9bfe0', 'lol', 'c87366e9-ad96-4ffe-9f8f-2b19c3736187');

INSERT INTO user_household_relation VALUES('e5310fb5-4e47-4e59-8664-33f1ce66232a', '42269d17-77cf-48a5-bf37-b1524fa9bfe0');
INSERT INTO user_household_relation VALUES('b0acc769-6b96-4a6b-8d75-34d91675d002', '42269d17-77cf-48a5-bf37-b1524fa9bfe0');