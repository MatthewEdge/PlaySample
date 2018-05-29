# ZimQuotes schema

# --- !Ups

CREATE TABLE ZimQuotes (
    id bigint(20) NOT NULL AUTO_INCREMENT,
    quote varchar(512) NOT NULL,
    character varchar(255) NOT NULL,
    PRIMARY KEY (id)
);

INSERT INTO ZimQuotes(quote, character) VALUES ('I gonna sing the doom song!', 'Gir');
INSERT INTO ZimQuotes(quote, character) VALUES ('WAT??', 'Zim');

# --- !Downs

DROP TABLE ZimQuotes;