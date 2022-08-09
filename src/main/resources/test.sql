INSERT INTO question
VALUES (26, '');

INSERT INTO answer
VALUES (11, '');

select * from question;
select * from answer;


DELETE FROM question where question_id = 124;
DELETE FROM answer where answer_id = 138;

CREATE TABLE question
(question_id INTEGER NOT NULL UNIQUE,
 question_text CHAR(100) not null);

 CREATE TABLE answer
(answer_id INTEGER NOT NULL UNIQUE,
 answer_text CHAR(100) not null);


















