ALTER TABLE IF EXISTS centralsyncitem RENAME TO syncitem;
ALTER TABLE IF EXISTS syncitem RENAME COLUMN stagingrepoId TO releaseId;
