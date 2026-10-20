INSERT INTO patients(last_name,first_name,patronymic,phone,keycloak_subject) VALUES ('Тестовый','Пётр','Петрович','+12025550123','69e4b0f4-2aa6-4d2b-bcdc-3caba0708766') ON CONFLICT(keycloak_subject) DO NOTHING;
UPDATE doctors SET keycloak_subject='83d4eb74-a63a-4b70-85c4-0237108ac40f' WHERE id=(SELECT MIN(id) FROM doctors WHERE last_name='Учебный');
