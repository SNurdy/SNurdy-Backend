-- V9__fix_morpheme_typos.sql

-- morphemes.spelling 오탈자 수정
UPDATE morphemes SET spelling = 'cephal(o)-; cerebr(o)-; cerebral' WHERE id = 310;
UPDATE morphemes SET spelling = 'gingiva-; gingiv(o)-; gum' WHERE id = 353;

-- morpheme_variants 오탈자 수정
UPDATE morpheme_variants SET variant = 'cerebro', base = 'cerebr' WHERE morpheme_id = 310 AND variant = 'ceregro';
UPDATE morpheme_variants SET variant = 'gingivo', base = 'gingiv' WHERE morpheme_id = 353 AND variant = 'gingibio';