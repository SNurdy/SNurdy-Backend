-- V54__fix_alpha_fetoprotein_spelling.sql
-- 860 spelling 보완 - fetoprotein 은 불완전 표기. AFP = Alpha-FetoProtein 이므로 alpha 포함
UPDATE terms SET spelling = 'alpha-fetoprotein' WHERE id = 860;
