INSERT INTO tags (id, tag)
VALUES (1, 'Name'),
       (2, 'Of'),
       (3, 'Tag');

INSERT INTO persons (id, first_name, last_name, reg_date, birth_date, email, phone, password, about)
VALUES (1, 'Gretchen', 'Contreras', '2022-09-29 21:49:07', '2000-06-08 10:54:06', 'rhoncus.nullam@yahoo.edu',
        '+7 (978) 311-43-59', 'ZvJ57ekY5Tc', 's');

INSERT INTO posts (id, is_blocked, is_deleted, post_text, time, title, author_id)
VALUES (1, false, false, 'e', '2022-03-19 11:53:27.000000', 'title', 1);

INSERT INTO post2tag (post_id, tag_id)
VALUES (1, 1),
       (1, 2),
       (1, 3);