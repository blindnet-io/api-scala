--
-- PostgreSQL database dump
--

-- Dumped from database version 13.6
-- Dumped by pg_dump version 13.6

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: users; Type: TABLE; Schema: public; Owner: blindnet
--

CREATE TABLE public.users (
    id text NOT NULL,
    app text NOT NULL,
    pub_enc_key text NOT NULL,
    pub_sign_key text NOT NULL,
    enc_priv_enc_key text,
    enc_priv_sign_key text,
    key_deriv_salt text,
    signed_pub_enc_key text
);


ALTER TABLE public.users OWNER TO blindnet;

--
-- Name: users users_pk; Type: CONSTRAINT; Schema: public; Owner: blindnet
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pk PRIMARY KEY (id, app);


--
-- PostgreSQL database dump complete
--

