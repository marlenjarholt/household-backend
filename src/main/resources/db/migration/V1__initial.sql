
CREATE TABLE users(
    id UUID PRIMARY KEY,
    mail VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE households(
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE user_household_relation(
    user_id UUID,
    CONSTRAINT fk_user FOREIGN KEY(user_id) REFERENCES users(id),
    household_id UUID,
    CONSTRAINT fk_household FOREIGN KEY(household_id) REFERENCES households(id),
    CONSTRAINT user_household_relation_pk PRIMARY KEY(user_id, household_id)
);

INSERT INTO users VALUES('e5310fb5-4e47-4e59-8664-33f1ce66232a', 'marly.marlen@gmail.com', 'Marlen Jarholt');
INSERT INTO users VALUES('b0acc769-6b96-4a6b-8d75-34d91675d002', 'mrsimenfonnes@gmail.com', 'Simen Fonnes');

INSERT INTO households VALUES('42269d17-77cf-48a5-bf37-b1524fa9bfe0', 'lol');

INSERT INTO user_household_relation VALUES('e5310fb5-4e47-4e59-8664-33f1ce66232a', '42269d17-77cf-48a5-bf37-b1524fa9bfe0');
INSERT INTO user_household_relation VALUES('b0acc769-6b96-4a6b-8d75-34d91675d002', '42269d17-77cf-48a5-bf37-b1524fa9bfe0');