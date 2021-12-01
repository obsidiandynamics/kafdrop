-- this is just an example, if you want to use it as is, you "just" need to crack the secured bcrypt string
INSERT INTO users (username, password, enabled)
VALUES ('f95a2ec06fc04e72','$2a$10$MvbTMKGWeBNBeCXKqFo/we0da4ai17IlQ7lq4nWzd3rQAQjR9COIS',1);

INSERT INTO authorities (username, authority)
VALUES ('f95a2ec06fc04e72','ROLE_ADMIN'); -- ROLE_USER