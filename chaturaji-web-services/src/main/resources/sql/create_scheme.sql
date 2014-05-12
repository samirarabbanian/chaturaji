-- this script creates the scheme and is called every time the GameDao is created during startup of Tomcat
-- therefore the script will be called each time Tomcat is restarted

DROP TABLE IF EXISTS MOVE;
DROP TABLE IF EXISTS PLAYER;
DROP TABLE IF EXISTS USER;
DROP TABLE IF EXISTS GAME;

CREATE TABLE GAME
(
  GAME_ID        VARCHAR(80) NOT NULL,
  CREATED_DATE   DATETIME    NOT NULL,
  CURRENT_PLAYER INTEGER     NOT NULL,
  GAME_STATUS    INTEGER     NOT NULL,
  PRIMARY KEY (GAME_ID)
);

CREATE TABLE USER
(
  USER_ID        VARCHAR(80)  NOT NULL,
  EMAIL          VARCHAR(80)  NOT NULL,
  NICKNAME       VARCHAR(80)  NOT NULL,
  PASSWORD       VARCHAR(150) NOT NULL,
  ONE_TIME_TOKEN VARCHAR(150),
  PRIMARY KEY (USER_ID)
);

CREATE TABLE PLAYER
(
  PLAYER_ID VARCHAR(80) NOT NULL,
  GAME_ID   VARCHAR(80) NOT NULL,
  USER_ID   VARCHAR(80) NOT NULL,
  COLOUR    INTEGER     NOT NULL,
  TYPE      VARCHAR(10) NOT NULL,
  POINTS    INTEGER     NOT NULL,
  PRIMARY KEY (PLAYER_ID),
  FOREIGN KEY (GAME_ID) REFERENCES GAME (GAME_ID),
  FOREIGN KEY (USER_ID) REFERENCES USER (USER_ID)
);

CREATE TABLE MOVE
(
  MOVE_ID     VARCHAR(80) NOT NULL,
  GAME_ID     VARCHAR(80) NOT NULL,
  COLOUR      INTEGER     NOT NULL,
  SOURCE      INTEGER     NOT NULL,
  DESTINATION INTEGER     NOT NULL,
  PRIMARY KEY (MOVE_ID, GAME_ID),
  FOREIGN KEY (GAME_ID) REFERENCES GAME (GAME_ID)
);

-- SHOW TABLES;
