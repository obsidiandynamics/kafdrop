-- this is just an example, if you want to use it as is, you "just" need to crack the secured bcrypt string
INSERT INTO users (username, password, enabled)
VALUES ('<REPLACEME>','<REPLACEME>',1);

INSERT INTO authorities (username, authority)
VALUES ('<REPLACEME>','ROLE_ADMIN'); -- ROLE_USER
