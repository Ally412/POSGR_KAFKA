ALTER TABLE medical_record ADD COLUMN urgency varchar(255);
UPDATE medical_record SET urgency = 'ROUTINE' WHERE urgency IS NULL;
ALTER TABLE medical_record ALTER COLUMN urgency SET NOT NULL;