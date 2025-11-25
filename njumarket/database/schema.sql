--
-- PostgreSQL database dump
--

\restrict MK6ueft5xtjierIt8PoTsf9pizJhzlkP9OV09UasArwOA9UPXyeFm7USoWJViuI

-- Dumped from database version 16.11 (Debian 16.11-1.pgdg11+1)
-- Dumped by pg_dump version 18.1

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: nju_market; Type: SCHEMA; Schema: -; Owner: postgres
--

CREATE SCHEMA nju_market;


ALTER SCHEMA nju_market OWNER TO postgres;

--
-- Name: nju_market_backup; Type: SCHEMA; Schema: -; Owner: postgres
--

CREATE SCHEMA nju_market_backup;


ALTER SCHEMA nju_market_backup OWNER TO postgres;

--
-- Name: tiger; Type: SCHEMA; Schema: -; Owner: postgres
--

CREATE SCHEMA tiger;


ALTER SCHEMA tiger OWNER TO postgres;

--
-- Name: tiger_data; Type: SCHEMA; Schema: -; Owner: postgres
--

CREATE SCHEMA tiger_data;


ALTER SCHEMA tiger_data OWNER TO postgres;

--
-- Name: topology; Type: SCHEMA; Schema: -; Owner: postgres
--

CREATE SCHEMA topology;


ALTER SCHEMA topology OWNER TO postgres;

--
-- Name: SCHEMA topology; Type: COMMENT; Schema: -; Owner: postgres
--

COMMENT ON SCHEMA topology IS 'PostGIS Topology schema';


--
-- Name: fuzzystrmatch; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS fuzzystrmatch WITH SCHEMA public;


--
-- Name: EXTENSION fuzzystrmatch; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION fuzzystrmatch IS 'determine similarities and distance between strings';


--
-- Name: pg_stat_statements; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pg_stat_statements WITH SCHEMA public;


--
-- Name: EXTENSION pg_stat_statements; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION pg_stat_statements IS 'track planning and execution statistics of all SQL statements executed';


--
-- Name: pg_trgm; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pg_trgm WITH SCHEMA public;


--
-- Name: EXTENSION pg_trgm; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION pg_trgm IS 'text similarity measurement and index searching based on trigrams';


--
-- Name: postgis; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS postgis WITH SCHEMA public;


--
-- Name: EXTENSION postgis; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION postgis IS 'PostGIS geometry and geography spatial types and functions';


--
-- Name: postgis_tiger_geocoder; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS postgis_tiger_geocoder WITH SCHEMA tiger;


--
-- Name: EXTENSION postgis_tiger_geocoder; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION postgis_tiger_geocoder IS 'PostGIS tiger geocoder and reverse geocoder';


--
-- Name: postgis_topology; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS postgis_topology WITH SCHEMA topology;


--
-- Name: EXTENSION postgis_topology; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION postgis_topology IS 'PostGIS topology spatial types and functions';


--
-- Name: vector; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS vector WITH SCHEMA public;


--
-- Name: EXTENSION vector; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION vector IS 'vector data type and ivfflat and hnsw access methods';


--
-- Name: update_ai_conversation_updated_at(); Type: FUNCTION; Schema: nju_market; Owner: postgres
--

CREATE FUNCTION nju_market.update_ai_conversation_updated_at() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;


ALTER FUNCTION nju_market.update_ai_conversation_updated_at() OWNER TO postgres;

--
-- Name: update_commodity_vector_updated_at(); Type: FUNCTION; Schema: nju_market; Owner: postgres
--

CREATE FUNCTION nju_market.update_commodity_vector_updated_at() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;


ALTER FUNCTION nju_market.update_commodity_vector_updated_at() OWNER TO postgres;

--
-- Name: update_user_addresses_updated_time(); Type: FUNCTION; Schema: nju_market; Owner: postgres
--

CREATE FUNCTION nju_market.update_user_addresses_updated_time() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.update_time = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;


ALTER FUNCTION nju_market.update_user_addresses_updated_time() OWNER TO postgres;

--
-- Name: update_user_profile_vector_updated_at(); Type: FUNCTION; Schema: nju_market; Owner: postgres
--

CREATE FUNCTION nju_market.update_user_profile_vector_updated_at() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;


ALTER FUNCTION nju_market.update_user_profile_vector_updated_at() OWNER TO postgres;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: admin_operation_logs; Type: TABLE; Schema: nju_market; Owner: postgres
--

CREATE TABLE nju_market.admin_operation_logs (
    log_id character varying(50) NOT NULL,
    admin_id character varying(50) NOT NULL,
    operation_type character varying(50) NOT NULL,
    operation_desc character varying(500),
    target_id character varying(50),
    target_type character varying(50),
    operation_data text,
    ip_address character varying(50),
    user_agent character varying(500),
    create_time timestamp with time zone
)
WITH (fillfactor='90');


ALTER TABLE nju_market.admin_operation_logs OWNER TO postgres;

--
-- Name: TABLE admin_operation_logs; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON TABLE nju_market.admin_operation_logs IS '管理员操作日志表 - 记录管理员的操作行为';


--
-- Name: COLUMN admin_operation_logs.log_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admin_operation_logs.log_id IS '日志ID';


--
-- Name: COLUMN admin_operation_logs.admin_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admin_operation_logs.admin_id IS '管理员ID';


--
-- Name: COLUMN admin_operation_logs.operation_type; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admin_operation_logs.operation_type IS '操作类型';


--
-- Name: COLUMN admin_operation_logs.operation_desc; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admin_operation_logs.operation_desc IS '操作描述';


--
-- Name: COLUMN admin_operation_logs.target_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admin_operation_logs.target_id IS '目标对象ID';


--
-- Name: COLUMN admin_operation_logs.target_type; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admin_operation_logs.target_type IS '目标对象类型';


--
-- Name: COLUMN admin_operation_logs.operation_data; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admin_operation_logs.operation_data IS '操作数据（JSON格式）';


--
-- Name: COLUMN admin_operation_logs.ip_address; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admin_operation_logs.ip_address IS '操作IP';


--
-- Name: COLUMN admin_operation_logs.user_agent; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admin_operation_logs.user_agent IS '用户代理';


--
-- Name: COLUMN admin_operation_logs.create_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admin_operation_logs.create_time IS '操作时间';


--
-- Name: admin_sessions; Type: TABLE; Schema: nju_market; Owner: postgres
--

CREATE TABLE nju_market.admin_sessions (
    session_id character varying(100) NOT NULL,
    admin_id character varying(50) NOT NULL,
    token character varying(500) NOT NULL,
    ip_address character varying(50),
    user_agent character varying(500),
    login_time timestamp with time zone,
    last_activity_time timestamp with time zone,
    expire_time timestamp with time zone,
    is_active boolean DEFAULT true NOT NULL
)
WITH (fillfactor='90');


ALTER TABLE nju_market.admin_sessions OWNER TO postgres;

--
-- Name: TABLE admin_sessions; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON TABLE nju_market.admin_sessions IS '管理员会话表 - 管理管理员登录会话';


--
-- Name: COLUMN admin_sessions.session_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admin_sessions.session_id IS '会话ID';


--
-- Name: COLUMN admin_sessions.admin_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admin_sessions.admin_id IS '管理员ID';


--
-- Name: COLUMN admin_sessions.token; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admin_sessions.token IS 'JWT Token';


--
-- Name: COLUMN admin_sessions.ip_address; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admin_sessions.ip_address IS '登录IP';


--
-- Name: COLUMN admin_sessions.user_agent; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admin_sessions.user_agent IS '用户代理';


--
-- Name: COLUMN admin_sessions.login_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admin_sessions.login_time IS '登录时间';


--
-- Name: COLUMN admin_sessions.last_activity_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admin_sessions.last_activity_time IS '最后活动时间';


--
-- Name: COLUMN admin_sessions.expire_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admin_sessions.expire_time IS '过期时间';


--
-- Name: COLUMN admin_sessions.is_active; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admin_sessions.is_active IS '是否活跃';


--
-- Name: admins; Type: TABLE; Schema: nju_market; Owner: postgres
--

CREATE TABLE nju_market.admins (
    admin_id character varying(50) NOT NULL,
    username character varying(50) NOT NULL,
    password character varying(255) NOT NULL,
    real_name character varying(50),
    email character varying(100),
    department character varying(50),
    "position" character varying(50),
    admin_level character varying(20) DEFAULT 'administrator'::character varying NOT NULL,
    permissions text,
    create_time timestamp with time zone,
    update_time timestamp with time zone,
    last_login_time timestamp with time zone,
    last_login_ip character varying(50),
    account_status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    login_count integer DEFAULT 0 NOT NULL,
    remark character varying(500)
)
WITH (fillfactor='90');


ALTER TABLE nju_market.admins OWNER TO postgres;

--
-- Name: TABLE admins; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON TABLE nju_market.admins IS '管理员表 - 存储内部管理员账号信息';


--
-- Name: COLUMN admins.admin_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admins.admin_id IS '管理员ID';


--
-- Name: COLUMN admins.username; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admins.username IS '用户名';


--
-- Name: COLUMN admins.password; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admins.password IS '密码（加密存储）';


--
-- Name: COLUMN admins.real_name; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admins.real_name IS '真实姓名';


--
-- Name: COLUMN admins.email; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admins.email IS '邮箱';


--
-- Name: COLUMN admins.department; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admins.department IS '部门';


--
-- Name: COLUMN admins."position"; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admins."position" IS '职位';


--
-- Name: COLUMN admins.admin_level; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admins.admin_level IS '管理员级别：system-系统管理员，administrator-普通管理员';


--
-- Name: COLUMN admins.permissions; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admins.permissions IS '权限列表（JSON格式）';


--
-- Name: COLUMN admins.create_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admins.create_time IS '创建时间';


--
-- Name: COLUMN admins.update_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admins.update_time IS '更新时间';


--
-- Name: COLUMN admins.last_login_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admins.last_login_time IS '最后登录时间';


--
-- Name: COLUMN admins.last_login_ip; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admins.last_login_ip IS '最后登录IP';


--
-- Name: COLUMN admins.account_status; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admins.account_status IS '账户状态：ACTIVE-活跃，SUSPENDED-暂停，BANNED-禁用';


--
-- Name: COLUMN admins.login_count; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admins.login_count IS '登录次数';


--
-- Name: COLUMN admins.remark; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.admins.remark IS '备注';


--
-- Name: ai_conversations; Type: TABLE; Schema: nju_market; Owner: postgres
--

CREATE TABLE nju_market.ai_conversations (
    conversation_id character varying(50) NOT NULL,
    user_id character varying(50) NOT NULL,
    title character varying(200),
    message_count integer DEFAULT 0,
    status character varying(20) DEFAULT 'ACTIVE'::character varying,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE nju_market.ai_conversations OWNER TO postgres;

--
-- Name: TABLE ai_conversations; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON TABLE nju_market.ai_conversations IS 'AI 聊天会话表，用于统一管理 AI 聊天会话';


--
-- Name: COLUMN ai_conversations.conversation_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.ai_conversations.conversation_id IS '会话ID（主键）';


--
-- Name: COLUMN ai_conversations.user_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.ai_conversations.user_id IS '用户ID（外键到users表）';


--
-- Name: COLUMN ai_conversations.title; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.ai_conversations.title IS '会话标题（第一条用户消息的前50个字符）';


--
-- Name: COLUMN ai_conversations.message_count; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.ai_conversations.message_count IS '消息数量（冗余字段，可通过查询计算）';


--
-- Name: COLUMN ai_conversations.status; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.ai_conversations.status IS '状态：ACTIVE（活跃）、DELETED（已删除）';


--
-- Name: COLUMN ai_conversations.created_at; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.ai_conversations.created_at IS '创建时间';


--
-- Name: COLUMN ai_conversations.updated_at; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.ai_conversations.updated_at IS '更新时间';


--
-- Name: audit_records; Type: TABLE; Schema: nju_market; Owner: postgres
--

CREATE TABLE nju_market.audit_records (
    record_id character varying(50) NOT NULL,
    commodity_id character varying(50) NOT NULL,
    reviewer_id character varying(50),
    reason text,
    decision character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    audit_time timestamp with time zone,
    audit_type character varying(20) DEFAULT 'AUTO'::character varying NOT NULL
)
WITH (fillfactor='90');


ALTER TABLE nju_market.audit_records OWNER TO postgres;

--
-- Name: TABLE audit_records; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON TABLE nju_market.audit_records IS '审核记录表';


--
-- Name: COLUMN audit_records.record_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.audit_records.record_id IS '记录ID';


--
-- Name: COLUMN audit_records.commodity_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.audit_records.commodity_id IS '商品ID';


--
-- Name: COLUMN audit_records.reviewer_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.audit_records.reviewer_id IS '审核员ID';


--
-- Name: COLUMN audit_records.reason; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.audit_records.reason IS '审核原因';


--
-- Name: COLUMN audit_records.decision; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.audit_records.decision IS '审核决定: APPROVED-通过, REJECTED-拒绝, PENDING-待审核';


--
-- Name: COLUMN audit_records.audit_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.audit_records.audit_time IS '审核时间';


--
-- Name: COLUMN audit_records.audit_type; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.audit_records.audit_type IS '审核类型: AUTO-自动审核, MANUAL-人工审核';


--
-- Name: ban_records; Type: TABLE; Schema: nju_market; Owner: postgres
--

CREATE TABLE nju_market.ban_records (
    ban_id character varying(50) NOT NULL,
    user_id character varying(50) NOT NULL,
    phone character varying(20),
    device_id character varying(100),
    real_name_id character varying(50),
    reason text NOT NULL,
    start_at timestamp with time zone,
    end_at timestamp with time zone,
    ban_type character varying(20) NOT NULL,
    is_active boolean DEFAULT true NOT NULL
)
WITH (fillfactor='90');


ALTER TABLE nju_market.ban_records OWNER TO postgres;

--
-- Name: TABLE ban_records; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON TABLE nju_market.ban_records IS '封禁记录表';


--
-- Name: COLUMN ban_records.ban_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.ban_records.ban_id IS '封禁ID';


--
-- Name: COLUMN ban_records.user_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.ban_records.user_id IS '用户ID';


--
-- Name: COLUMN ban_records.phone; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.ban_records.phone IS '手机号';


--
-- Name: COLUMN ban_records.device_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.ban_records.device_id IS '设备ID';


--
-- Name: COLUMN ban_records.real_name_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.ban_records.real_name_id IS '实名ID';


--
-- Name: COLUMN ban_records.reason; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.ban_records.reason IS '封禁原因';


--
-- Name: COLUMN ban_records.start_at; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.ban_records.start_at IS '封禁开始时间';


--
-- Name: COLUMN ban_records.end_at; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.ban_records.end_at IS '封禁结束时间';


--
-- Name: COLUMN ban_records.ban_type; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.ban_records.ban_type IS '封禁类型: TEMPORARY-临时, PERMANENT-永久, DEVICE-设备, PHONE-手机, REAL_NAME-实名';


--
-- Name: COLUMN ban_records.is_active; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.ban_records.is_active IS '是否生效: 0-无效, 1-有效';


--
-- Name: commodities; Type: TABLE; Schema: nju_market; Owner: postgres
--

CREATE TABLE nju_market.commodities (
    commodity_id character varying(50) NOT NULL,
    seller_id character varying(50) NOT NULL,
    title character varying(200) NOT NULL,
    description text,
    price double precision NOT NULL,
    stock integer NOT NULL,
    location character varying(200),
    category character varying(50),
    condition_level character varying(20) DEFAULT 'GOOD'::character varying,
    images text,
    publish_time timestamp with time zone,
    commodity_status character varying(20) DEFAULT 'DRAFT'::character varying NOT NULL,
    seller_visibility character varying(20) DEFAULT 'PUBLIC'::character varying NOT NULL,
    buyer_visibility character varying(20) DEFAULT 'PUBLIC'::character varying NOT NULL,
    click_count integer DEFAULT 0 NOT NULL,
    report_count integer DEFAULT 0 NOT NULL,
    address_id character varying(50),
    address_snapshot_province character varying(50),
    address_snapshot_city character varying(50),
    address_snapshot_district character varying(50),
    address_snapshot_street character varying(200),
    address_snapshot_detail character varying(500),
    address_snapshot_full text,
    location_geography public.geography(Point,4326),
    longitude double precision,
    latitude double precision
)
WITH (fillfactor='90');


ALTER TABLE nju_market.commodities OWNER TO postgres;

--
-- Name: TABLE commodities; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON TABLE nju_market.commodities IS '商品表';


--
-- Name: COLUMN commodities.commodity_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodities.commodity_id IS '商品ID';


--
-- Name: COLUMN commodities.seller_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodities.seller_id IS '卖家用户ID';


--
-- Name: COLUMN commodities.title; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodities.title IS '商品标题';


--
-- Name: COLUMN commodities.description; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodities.description IS '商品描述';


--
-- Name: COLUMN commodities.stock; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodities.stock IS '库存数量';


--
-- Name: COLUMN commodities.location; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodities.location IS '商品位置';


--
-- Name: COLUMN commodities.category; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodities.category IS '商品分类';


--
-- Name: COLUMN commodities.condition_level; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodities.condition_level IS '商品成色: EXCELLENT-优秀, GOOD-良好, FAIR-一般, POOR-较差';


--
-- Name: COLUMN commodities.images; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodities.images IS '商品图片URL列表(JSON格式)';


--
-- Name: COLUMN commodities.commodity_status; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodities.commodity_status IS '商品状态: DRAFT-草稿, PUBLISHED-已发布, SOLD_OUT-售罄, REMOVED-已下架';


--
-- Name: COLUMN commodities.seller_visibility; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodities.seller_visibility IS '卖家可见性: PUBLIC-公开, PRIVATE-私有, HIDDEN-隐藏';


--
-- Name: COLUMN commodities.buyer_visibility; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodities.buyer_visibility IS '买家可见性: PUBLIC-公开, PRIVATE-私有, HIDDEN-隐藏';


--
-- Name: COLUMN commodities.click_count; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodities.click_count IS '点击次数';


--
-- Name: COLUMN commodities.report_count; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodities.report_count IS '举报次数';


--
-- Name: COLUMN commodities.address_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodities.address_id IS '商品地址ID（引用user_addresses表）';


--
-- Name: COLUMN commodities.address_snapshot_province; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodities.address_snapshot_province IS '地址快照-省份';


--
-- Name: COLUMN commodities.address_snapshot_city; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodities.address_snapshot_city IS '地址快照-城市';


--
-- Name: COLUMN commodities.address_snapshot_district; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodities.address_snapshot_district IS '地址快照-区/县';


--
-- Name: COLUMN commodities.address_snapshot_street; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodities.address_snapshot_street IS '地址快照-街道';


--
-- Name: COLUMN commodities.address_snapshot_detail; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodities.address_snapshot_detail IS '地址快照-详细地址';


--
-- Name: COLUMN commodities.address_snapshot_full; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodities.address_snapshot_full IS '地址快照-完整地址';


--
-- Name: COLUMN commodities.location_geography; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodities.location_geography IS '地理位置（PostGIS Geography类型）';


--
-- Name: COLUMN commodities.longitude; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodities.longitude IS '经度';


--
-- Name: COLUMN commodities.latitude; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodities.latitude IS '纬度';


--
-- Name: commodity_categories; Type: TABLE; Schema: nju_market; Owner: postgres
--

CREATE TABLE nju_market.commodity_categories (
    category_id character varying(50) NOT NULL,
    category_name character varying(100) NOT NULL,
    parent_id character varying(50),
    sort_order integer DEFAULT 0 NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    create_time timestamp with time zone
)
WITH (fillfactor='90');


ALTER TABLE nju_market.commodity_categories OWNER TO postgres;

--
-- Name: TABLE commodity_categories; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON TABLE nju_market.commodity_categories IS '商品分类表';


--
-- Name: COLUMN commodity_categories.category_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodity_categories.category_id IS '分类ID';


--
-- Name: COLUMN commodity_categories.category_name; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodity_categories.category_name IS '分类名称';


--
-- Name: COLUMN commodity_categories.parent_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodity_categories.parent_id IS '父分类ID';


--
-- Name: COLUMN commodity_categories.sort_order; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodity_categories.sort_order IS '排序';


--
-- Name: COLUMN commodity_categories.is_active; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodity_categories.is_active IS '是否启用';


--
-- Name: COLUMN commodity_categories.create_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodity_categories.create_time IS '创建时间';


--
-- Name: commodity_snapshots; Type: TABLE; Schema: nju_market; Owner: postgres
--

CREATE TABLE nju_market.commodity_snapshots (
    snapshot_id character varying(50) NOT NULL,
    category character varying(50),
    commodity_status character varying(20),
    condition_level character varying(20),
    description text,
    images text,
    location character varying(200),
    original_commodity_id character varying(50),
    price double precision NOT NULL,
    seller_email character varying(100),
    seller_id character varying(50),
    seller_name character varying(100),
    seller_phone character varying(20),
    snapshot_time timestamp(6) with time zone,
    stock integer,
    title character varying(200) NOT NULL
)
WITH (fillfactor='90');


ALTER TABLE nju_market.commodity_snapshots OWNER TO postgres;

--
-- Name: commodity_vectors; Type: TABLE; Schema: nju_market; Owner: postgres
--

CREATE TABLE nju_market.commodity_vectors (
    id bigint NOT NULL,
    commodity_id character varying(50) NOT NULL,
    embedding public.vector(2000),
    content text NOT NULL,
    metadata jsonb,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE nju_market.commodity_vectors OWNER TO postgres;

--
-- Name: TABLE commodity_vectors; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON TABLE nju_market.commodity_vectors IS '商品向量表，用于存储商品的向量化表示（2000维）';


--
-- Name: COLUMN commodity_vectors.embedding; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.commodity_vectors.embedding IS '商品向量（2000维，HNSW索引限制）';


--
-- Name: commodity_vectors_id_seq; Type: SEQUENCE; Schema: nju_market; Owner: postgres
--

CREATE SEQUENCE nju_market.commodity_vectors_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE nju_market.commodity_vectors_id_seq OWNER TO postgres;

--
-- Name: commodity_vectors_id_seq; Type: SEQUENCE OWNED BY; Schema: nju_market; Owner: postgres
--

ALTER SEQUENCE nju_market.commodity_vectors_id_seq OWNED BY nju_market.commodity_vectors.id;


--
-- Name: complaints; Type: TABLE; Schema: nju_market; Owner: postgres
--

CREATE TABLE nju_market.complaints (
    complaint_id character varying(50) NOT NULL,
    complainant_id character varying(50) NOT NULL,
    defendant_id character varying(50) NOT NULL,
    related_order_id character varying(50),
    content text NOT NULL,
    evidence_files text,
    status character varying(20) DEFAULT 'SUBMITTED'::character varying NOT NULL,
    submit_time timestamp with time zone,
    resolve_time timestamp with time zone
)
WITH (fillfactor='90');


ALTER TABLE nju_market.complaints OWNER TO postgres;

--
-- Name: TABLE complaints; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON TABLE nju_market.complaints IS '投诉表';


--
-- Name: COLUMN complaints.complaint_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.complaints.complaint_id IS '投诉ID';


--
-- Name: COLUMN complaints.complainant_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.complaints.complainant_id IS '投诉人用户ID';


--
-- Name: COLUMN complaints.defendant_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.complaints.defendant_id IS '被投诉人用户ID';


--
-- Name: COLUMN complaints.related_order_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.complaints.related_order_id IS '相关订单ID';


--
-- Name: COLUMN complaints.content; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.complaints.content IS '投诉内容';


--
-- Name: COLUMN complaints.evidence_files; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.complaints.evidence_files IS '证据文件列表(JSON格式)';


--
-- Name: COLUMN complaints.status; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.complaints.status IS '投诉状态: SUBMITTED-已提交, PROCESSING-处理中, RESOLVED-已解决, REJECTED-已拒绝';


--
-- Name: COLUMN complaints.submit_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.complaints.submit_time IS '提交时间';


--
-- Name: COLUMN complaints.resolve_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.complaints.resolve_time IS '解决时间';


--
-- Name: contact_blacklist; Type: TABLE; Schema: nju_market; Owner: postgres
--

CREATE TABLE nju_market.contact_blacklist (
    id bigint NOT NULL,
    user_id character varying(50) NOT NULL,
    blocked_user_id character varying(50) NOT NULL,
    reason character varying(255),
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
)
WITH (fillfactor='90');


ALTER TABLE nju_market.contact_blacklist OWNER TO postgres;

--
-- Name: TABLE contact_blacklist; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON TABLE nju_market.contact_blacklist IS '联系人黑名单表';


--
-- Name: COLUMN contact_blacklist.user_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.contact_blacklist.user_id IS '用户ID';


--
-- Name: COLUMN contact_blacklist.blocked_user_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.contact_blacklist.blocked_user_id IS '被屏蔽用户ID';


--
-- Name: COLUMN contact_blacklist.reason; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.contact_blacklist.reason IS '屏蔽原因';


--
-- Name: COLUMN contact_blacklist.created_at; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.contact_blacklist.created_at IS '屏蔽时间';


--
-- Name: contact_blacklist_id_seq; Type: SEQUENCE; Schema: nju_market; Owner: postgres
--

CREATE SEQUENCE nju_market.contact_blacklist_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE nju_market.contact_blacklist_id_seq OWNER TO postgres;

--
-- Name: contact_blacklist_id_seq; Type: SEQUENCE OWNED BY; Schema: nju_market; Owner: postgres
--

ALTER SEQUENCE nju_market.contact_blacklist_id_seq OWNED BY nju_market.contact_blacklist.id;


--
-- Name: contact_info; Type: TABLE; Schema: nju_market; Owner: postgres
--

CREATE TABLE nju_market.contact_info (
    contact_id character varying(50) NOT NULL,
    owner_id character varying(50) NOT NULL,
    type character varying(20) NOT NULL,
    value_encrypted character varying(500) NOT NULL
)
WITH (fillfactor='90');


ALTER TABLE nju_market.contact_info OWNER TO postgres;

--
-- Name: TABLE contact_info; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON TABLE nju_market.contact_info IS '联系方式表';


--
-- Name: COLUMN contact_info.contact_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.contact_info.contact_id IS '联系方式ID';


--
-- Name: COLUMN contact_info.owner_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.contact_info.owner_id IS '所有者用户ID';


--
-- Name: COLUMN contact_info.type; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.contact_info.type IS '联系方式类型: PHONE-电话, EMAIL-邮箱, WECHAT-微信, QQ-QQ';


--
-- Name: COLUMN contact_info.value_encrypted; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.contact_info.value_encrypted IS '加密后的联系方式值';


--
-- Name: conversation_vectors; Type: TABLE; Schema: nju_market; Owner: postgres
--

CREATE TABLE nju_market.conversation_vectors (
    id bigint NOT NULL,
    conversation_id character varying(50) NOT NULL,
    message_id character varying(50) NOT NULL,
    user_id character varying(50) NOT NULL,
    embedding public.vector(2000),
    content text NOT NULL,
    role character varying(20) NOT NULL,
    metadata jsonb,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE nju_market.conversation_vectors OWNER TO postgres;

--
-- Name: TABLE conversation_vectors; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON TABLE nju_market.conversation_vectors IS '对话历史向量表，用于存储对话消息的向量化表示（2000维）';


--
-- Name: COLUMN conversation_vectors.embedding; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.conversation_vectors.embedding IS '消息向量（2000维，HNSW索引限制）';


--
-- Name: conversation_vectors_id_seq; Type: SEQUENCE; Schema: nju_market; Owner: postgres
--

CREATE SEQUENCE nju_market.conversation_vectors_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE nju_market.conversation_vectors_id_seq OWNER TO postgres;

--
-- Name: conversation_vectors_id_seq; Type: SEQUENCE OWNED BY; Schema: nju_market; Owner: postgres
--

ALTER SEQUENCE nju_market.conversation_vectors_id_seq OWNED BY nju_market.conversation_vectors.id;


--
-- Name: conversations; Type: TABLE; Schema: nju_market; Owner: postgres
--

CREATE TABLE nju_market.conversations (
    conversation_id character varying(255) NOT NULL,
    last_message_content text,
    last_message_time timestamp with time zone,
    status character varying(20) DEFAULT 'ACTIVE'::character varying,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    user_id_1 character varying(50) NOT NULL,
    user_id_2 character varying(50) NOT NULL,
    user_1_count integer DEFAULT 0 NOT NULL,
    user_2_count integer DEFAULT 0 NOT NULL,
    user_1_visibility boolean NOT NULL,
    user_2_visibility boolean NOT NULL,
    user_1_last_message_content text,
    user_1_last_message_time timestamp(6) with time zone,
    user_2_last_message_content text,
    user_2_last_message_time timestamp(6) with time zone
)
WITH (fillfactor='90');


ALTER TABLE nju_market.conversations OWNER TO postgres;

--
-- Name: TABLE conversations; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON TABLE nju_market.conversations IS '对话表';


--
-- Name: COLUMN conversations.conversation_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.conversations.conversation_id IS '对话ID';


--
-- Name: COLUMN conversations.last_message_content; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.conversations.last_message_content IS '最后一条消息内容';


--
-- Name: COLUMN conversations.last_message_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.conversations.last_message_time IS '最后消息时间';


--
-- Name: COLUMN conversations.status; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.conversations.status IS '对话状态：ACTIVE-活跃，ARCHIVED-已归档，DELETED-已删除';


--
-- Name: COLUMN conversations.created_at; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.conversations.created_at IS '创建时间';


--
-- Name: COLUMN conversations.updated_at; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.conversations.updated_at IS '更新时间';


--
-- Name: data_statistics; Type: TABLE; Schema: nju_market; Owner: postgres
--

CREATE TABLE nju_market.data_statistics (
    id bigint NOT NULL,
    cycle character varying(20) NOT NULL,
    dimension character varying(50) NOT NULL,
    value double precision NOT NULL,
    category character varying(50),
    date_key character varying(20) NOT NULL,
    create_time timestamp with time zone,
    extra_data text
)
WITH (fillfactor='90');


ALTER TABLE nju_market.data_statistics OWNER TO postgres;

--
-- Name: TABLE data_statistics; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON TABLE nju_market.data_statistics IS '数据统计表';


--
-- Name: COLUMN data_statistics.id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.data_statistics.id IS '自增主键';


--
-- Name: COLUMN data_statistics.cycle; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.data_statistics.cycle IS '统计周期: DAILY-日, WEEKLY-周, MONTHLY-月, YEARLY-年';


--
-- Name: COLUMN data_statistics.dimension; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.data_statistics.dimension IS '统计维度: SALES-销售, USER_ACTIVITY-用户活动, COMMODITY_VIEWS-商品浏览, REVENUE-收入';


--
-- Name: COLUMN data_statistics.category; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.data_statistics.category IS '分类';


--
-- Name: COLUMN data_statistics.date_key; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.data_statistics.date_key IS '日期键(格式: YYYY-MM-DD 或 YYYY-MM 或 YYYY)';


--
-- Name: COLUMN data_statistics.create_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.data_statistics.create_time IS '创建时间';


--
-- Name: COLUMN data_statistics.extra_data; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.data_statistics.extra_data IS '额外数据(JSON格式)';


--
-- Name: data_statistics_id_seq; Type: SEQUENCE; Schema: nju_market; Owner: postgres
--

CREATE SEQUENCE nju_market.data_statistics_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE nju_market.data_statistics_id_seq OWNER TO postgres;

--
-- Name: data_statistics_id_seq; Type: SEQUENCE OWNED BY; Schema: nju_market; Owner: postgres
--

ALTER SEQUENCE nju_market.data_statistics_id_seq OWNED BY nju_market.data_statistics.id;


--
-- Name: image_references; Type: TABLE; Schema: nju_market; Owner: postgres
--

CREATE TABLE nju_market.image_references (
    image_id bigint NOT NULL,
    deleted_time timestamp(6) with time zone,
    file_size bigint,
    image_path character varying(500) NOT NULL,
    image_type character varying(20) NOT NULL,
    is_deleted boolean,
    last_reference_time timestamp(6) with time zone,
    reference_count integer NOT NULL,
    upload_time timestamp(6) with time zone,
    upload_user_id character varying(50)
)
WITH (fillfactor='90');


ALTER TABLE nju_market.image_references OWNER TO postgres;

--
-- Name: image_references_image_id_seq; Type: SEQUENCE; Schema: nju_market; Owner: postgres
--

CREATE SEQUENCE nju_market.image_references_image_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE nju_market.image_references_image_id_seq OWNER TO postgres;

--
-- Name: image_references_image_id_seq; Type: SEQUENCE OWNED BY; Schema: nju_market; Owner: postgres
--

ALTER SEQUENCE nju_market.image_references_image_id_seq OWNED BY nju_market.image_references.image_id;


--
-- Name: message_notification_settings; Type: TABLE; Schema: nju_market; Owner: postgres
--

CREATE TABLE nju_market.message_notification_settings (
    user_id character varying(50) NOT NULL,
    enable_email_notification boolean DEFAULT true,
    enable_push_notification boolean DEFAULT true,
    enable_sound boolean DEFAULT true,
    quiet_hours_start time without time zone,
    quiet_hours_end time without time zone,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
)
WITH (fillfactor='90');


ALTER TABLE nju_market.message_notification_settings OWNER TO postgres;

--
-- Name: TABLE message_notification_settings; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON TABLE nju_market.message_notification_settings IS '消息通知设置表';


--
-- Name: COLUMN message_notification_settings.user_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.message_notification_settings.user_id IS '用户ID';


--
-- Name: COLUMN message_notification_settings.enable_email_notification; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.message_notification_settings.enable_email_notification IS '启用邮件通知';


--
-- Name: COLUMN message_notification_settings.enable_push_notification; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.message_notification_settings.enable_push_notification IS '启用推送通知';


--
-- Name: COLUMN message_notification_settings.enable_sound; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.message_notification_settings.enable_sound IS '启用声音提醒';


--
-- Name: COLUMN message_notification_settings.quiet_hours_start; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.message_notification_settings.quiet_hours_start IS '免打扰开始时间';


--
-- Name: COLUMN message_notification_settings.quiet_hours_end; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.message_notification_settings.quiet_hours_end IS '免打扰结束时间';


--
-- Name: COLUMN message_notification_settings.created_at; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.message_notification_settings.created_at IS '创建时间';


--
-- Name: COLUMN message_notification_settings.updated_at; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.message_notification_settings.updated_at IS '更新时间';


--
-- Name: messages; Type: TABLE; Schema: nju_market; Owner: postgres
--

CREATE TABLE nju_market.messages (
    message_id character varying(255) NOT NULL,
    conversation_id character varying(255) NOT NULL,
    sender_id character varying(50) NOT NULL,
    receiver_id character varying(50) NOT NULL,
    message_type character varying(20) DEFAULT 'TEXT'::character varying,
    content text NOT NULL,
    image_url character varying(500),
    is_read boolean DEFAULT false,
    read_time timestamp with time zone,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    deleted_by_receiver boolean,
    deleted_by_sender boolean,
    commodity_id character varying(50),
    order_id character varying(50)
)
WITH (fillfactor='90');


ALTER TABLE nju_market.messages OWNER TO postgres;

--
-- Name: TABLE messages; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON TABLE nju_market.messages IS '消息表';


--
-- Name: COLUMN messages.message_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.messages.message_id IS '消息ID';


--
-- Name: COLUMN messages.conversation_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.messages.conversation_id IS '对话ID';


--
-- Name: COLUMN messages.sender_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.messages.sender_id IS '发送者ID';


--
-- Name: COLUMN messages.receiver_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.messages.receiver_id IS '接收者ID';


--
-- Name: COLUMN messages.message_type; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.messages.message_type IS '消息类型：TEXT-文本，IMAGE-图片，COMMODITY-商品卡片，ORDER-订单卡片';


--
-- Name: COLUMN messages.content; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.messages.content IS '消息内容';


--
-- Name: COLUMN messages.image_url; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.messages.image_url IS '图片URL（当消息类型为IMAGE时）';


--
-- Name: COLUMN messages.is_read; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.messages.is_read IS '是否已读';


--
-- Name: COLUMN messages.read_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.messages.read_time IS '已读时间';


--
-- Name: COLUMN messages.created_at; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.messages.created_at IS '发送时间';


--
-- Name: order_snapshots; Type: TABLE; Schema: nju_market; Owner: postgres
--

CREATE TABLE nju_market.order_snapshots (
    snapshot_id character varying(50) NOT NULL,
    buyer_id character varying(50),
    commodity_snapshot_id character varying(50),
    order_status character varying(20),
    original_order_id character varying(50),
    pay_amount double precision,
    quantity integer,
    remark text,
    seller_id character varying(50),
    shipping_address text,
    snapshot_time timestamp(6) with time zone
)
WITH (fillfactor='90');


ALTER TABLE nju_market.order_snapshots OWNER TO postgres;

--
-- Name: order_status_logs; Type: TABLE; Schema: nju_market; Owner: postgres
--

CREATE TABLE nju_market.order_status_logs (
    log_id character varying(50) NOT NULL,
    order_id character varying(50) NOT NULL,
    from_status character varying(20),
    to_status character varying(20) NOT NULL,
    operator_id character varying(50),
    operator_type character varying(20) NOT NULL,
    reason text,
    seller_visibility_before character varying(20),
    seller_visibility_after character varying(20),
    buyer_visibility_before character varying(20),
    buyer_visibility_after character varying(20),
    return_reason text,
    return_rejection_reason text,
    create_time timestamp with time zone
)
WITH (fillfactor='90');


ALTER TABLE nju_market.order_status_logs OWNER TO postgres;

--
-- Name: TABLE order_status_logs; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON TABLE nju_market.order_status_logs IS '订单状态变更记录表';


--
-- Name: COLUMN order_status_logs.log_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.order_status_logs.log_id IS '日志ID';


--
-- Name: COLUMN order_status_logs.order_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.order_status_logs.order_id IS '订单ID';


--
-- Name: COLUMN order_status_logs.from_status; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.order_status_logs.from_status IS '原状态';


--
-- Name: COLUMN order_status_logs.to_status; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.order_status_logs.to_status IS '新状态';


--
-- Name: COLUMN order_status_logs.operator_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.order_status_logs.operator_id IS '操作者ID';


--
-- Name: COLUMN order_status_logs.operator_type; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.order_status_logs.operator_type IS '操作者类型: BUYER-买家, SELLER-卖家, ADMIN-管理员, SYSTEM-系统';


--
-- Name: COLUMN order_status_logs.reason; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.order_status_logs.reason IS '变更原因';


--
-- Name: COLUMN order_status_logs.seller_visibility_before; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.order_status_logs.seller_visibility_before IS '变更前卖家可见性';


--
-- Name: COLUMN order_status_logs.seller_visibility_after; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.order_status_logs.seller_visibility_after IS '变更后卖家可见性';


--
-- Name: COLUMN order_status_logs.buyer_visibility_before; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.order_status_logs.buyer_visibility_before IS '变更前买家可见性';


--
-- Name: COLUMN order_status_logs.buyer_visibility_after; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.order_status_logs.buyer_visibility_after IS '变更后买家可见性';


--
-- Name: COLUMN order_status_logs.return_reason; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.order_status_logs.return_reason IS '退货原因';


--
-- Name: COLUMN order_status_logs.return_rejection_reason; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.order_status_logs.return_rejection_reason IS '退货拒绝原因';


--
-- Name: COLUMN order_status_logs.create_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.order_status_logs.create_time IS '创建时间';


--
-- Name: orders; Type: TABLE; Schema: nju_market; Owner: postgres
--

CREATE TABLE nju_market.orders (
    order_id character varying(50) NOT NULL,
    buyer_id character varying(50) NOT NULL,
    seller_id character varying(50) NOT NULL,
    commodity_id character varying(50) NOT NULL,
    order_status character varying(20) DEFAULT 'CREATED'::character varying NOT NULL,
    seller_visibility character varying(20) DEFAULT 'PUBLIC'::character varying NOT NULL,
    buyer_visibility character varying(20) DEFAULT 'PUBLIC'::character varying NOT NULL,
    pay_amount double precision NOT NULL,
    create_time timestamp with time zone,
    shipping_time timestamp with time zone,
    delivery_time timestamp with time zone,
    tracking_number character varying(100),
    shipping_address text,
    remark text,
    return_reason text,
    return_request_time timestamp with time zone,
    return_approval_time timestamp with time zone,
    return_rejection_reason text,
    return_tracking_number character varying(100),
    return_completion_time timestamp with time zone,
    quantity integer DEFAULT 1 NOT NULL,
    commodity_snapshot_title character varying(200),
    commodity_snapshot_description text,
    commodity_snapshot_price double precision,
    commodity_snapshot_location character varying(200),
    commodity_snapshot_category character varying(50),
    commodity_snapshot_condition_level character varying(20),
    commodity_snapshot_images text,
    commodity_snapshot_status character varying(20),
    commodity_snapshot_seller_name character varying(100),
    commodity_snapshot_seller_phone character varying(20),
    commodity_snapshot_seller_email character varying(100),
    commodity_snapshot_time timestamp with time zone,
    pay_time timestamp(6) with time zone,
    shipping_address_id character varying(50),
    shipping_address_snapshot_province character varying(50),
    shipping_address_snapshot_city character varying(50),
    shipping_address_snapshot_district character varying(50),
    shipping_address_snapshot_street character varying(200),
    shipping_address_snapshot_detail character varying(500),
    shipping_address_snapshot_full text,
    shipping_address_snapshot_recipient_name character varying(100),
    shipping_address_snapshot_recipient_phone character varying(20),
    commodity_snapshot_address_province character varying(50),
    commodity_snapshot_address_city character varying(50),
    commodity_snapshot_address_district character varying(50),
    commodity_snapshot_address_street character varying(200),
    commodity_snapshot_address_detail character varying(500),
    commodity_snapshot_address_full text
)
WITH (fillfactor='90');


ALTER TABLE nju_market.orders OWNER TO postgres;

--
-- Name: TABLE orders; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON TABLE nju_market.orders IS '订单表-包含商品快照信息';


--
-- Name: COLUMN orders.order_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.order_id IS '订单ID';


--
-- Name: COLUMN orders.buyer_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.buyer_id IS '买家用户ID';


--
-- Name: COLUMN orders.seller_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.seller_id IS '卖家用户ID';


--
-- Name: COLUMN orders.commodity_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.commodity_id IS '商品ID';


--
-- Name: COLUMN orders.order_status; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.order_status IS '订单状态: CREATED-已创建, PAID-已支付, SHIPPED-已发货, COMPLETED-已完成, CANCELLED-已取消, REFUNDED-已退款';


--
-- Name: COLUMN orders.seller_visibility; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.seller_visibility IS '卖家可见性: PUBLIC-公开, PRIVATE-私有, HIDDEN-隐藏';


--
-- Name: COLUMN orders.buyer_visibility; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.buyer_visibility IS '买家可见性: PUBLIC-公开, PRIVATE-私有, HIDDEN-隐藏';


--
-- Name: COLUMN orders.create_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.create_time IS '创建时间';


--
-- Name: COLUMN orders.shipping_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.shipping_time IS '发货时间';


--
-- Name: COLUMN orders.delivery_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.delivery_time IS '签收时间';


--
-- Name: COLUMN orders.tracking_number; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.tracking_number IS '快递单号';


--
-- Name: COLUMN orders.shipping_address; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.shipping_address IS '收货地址';


--
-- Name: COLUMN orders.remark; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.remark IS '订单备注';


--
-- Name: COLUMN orders.return_reason; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.return_reason IS '退货原因';


--
-- Name: COLUMN orders.return_request_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.return_request_time IS '退货申请时间';


--
-- Name: COLUMN orders.return_approval_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.return_approval_time IS '退货审批时间';


--
-- Name: COLUMN orders.return_rejection_reason; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.return_rejection_reason IS '退货拒绝原因';


--
-- Name: COLUMN orders.return_tracking_number; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.return_tracking_number IS '退货快递单号';


--
-- Name: COLUMN orders.return_completion_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.return_completion_time IS '退货完成时间';


--
-- Name: COLUMN orders.quantity; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.quantity IS '购买数量';


--
-- Name: COLUMN orders.commodity_snapshot_title; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.commodity_snapshot_title IS '商品快照-标题';


--
-- Name: COLUMN orders.commodity_snapshot_description; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.commodity_snapshot_description IS '商品快照-描述';


--
-- Name: COLUMN orders.commodity_snapshot_location; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.commodity_snapshot_location IS '商品快照-位置';


--
-- Name: COLUMN orders.commodity_snapshot_category; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.commodity_snapshot_category IS '商品快照-分类';


--
-- Name: COLUMN orders.commodity_snapshot_condition_level; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.commodity_snapshot_condition_level IS '商品快照-成色';


--
-- Name: COLUMN orders.commodity_snapshot_images; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.commodity_snapshot_images IS '商品快照-图片(JSON格式)';


--
-- Name: COLUMN orders.commodity_snapshot_status; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.commodity_snapshot_status IS '商品快照-状态';


--
-- Name: COLUMN orders.commodity_snapshot_seller_name; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.commodity_snapshot_seller_name IS '商品快照-卖家名称';


--
-- Name: COLUMN orders.commodity_snapshot_seller_phone; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.commodity_snapshot_seller_phone IS '商品快照-卖家电话';


--
-- Name: COLUMN orders.commodity_snapshot_seller_email; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.commodity_snapshot_seller_email IS '商品快照-卖家邮箱';


--
-- Name: COLUMN orders.commodity_snapshot_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.commodity_snapshot_time IS '商品快照时间';


--
-- Name: COLUMN orders.shipping_address_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.shipping_address_id IS '收货地址ID（引用user_addresses表）';


--
-- Name: COLUMN orders.shipping_address_snapshot_province; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.shipping_address_snapshot_province IS '收货地址快照-省份';


--
-- Name: COLUMN orders.shipping_address_snapshot_city; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.shipping_address_snapshot_city IS '收货地址快照-城市';


--
-- Name: COLUMN orders.shipping_address_snapshot_district; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.shipping_address_snapshot_district IS '收货地址快照-区/县';


--
-- Name: COLUMN orders.shipping_address_snapshot_street; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.shipping_address_snapshot_street IS '收货地址快照-街道';


--
-- Name: COLUMN orders.shipping_address_snapshot_detail; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.shipping_address_snapshot_detail IS '收货地址快照-详细地址';


--
-- Name: COLUMN orders.shipping_address_snapshot_full; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.shipping_address_snapshot_full IS '收货地址快照-完整地址';


--
-- Name: COLUMN orders.shipping_address_snapshot_recipient_name; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.shipping_address_snapshot_recipient_name IS '收货地址快照-收货人姓名';


--
-- Name: COLUMN orders.shipping_address_snapshot_recipient_phone; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.shipping_address_snapshot_recipient_phone IS '收货地址快照-收货人电话';


--
-- Name: COLUMN orders.commodity_snapshot_address_province; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.commodity_snapshot_address_province IS '商品地址快照-省份';


--
-- Name: COLUMN orders.commodity_snapshot_address_city; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.commodity_snapshot_address_city IS '商品地址快照-城市';


--
-- Name: COLUMN orders.commodity_snapshot_address_district; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.commodity_snapshot_address_district IS '商品地址快照-区/县';


--
-- Name: COLUMN orders.commodity_snapshot_address_street; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.commodity_snapshot_address_street IS '商品地址快照-街道';


--
-- Name: COLUMN orders.commodity_snapshot_address_detail; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.commodity_snapshot_address_detail IS '商品地址快照-详细地址';


--
-- Name: COLUMN orders.commodity_snapshot_address_full; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.orders.commodity_snapshot_address_full IS '商品地址快照-完整地址';


--
-- Name: promotions; Type: TABLE; Schema: nju_market; Owner: postgres
--

CREATE TABLE nju_market.promotions (
    promotion_id character varying(50) NOT NULL,
    user_id character varying(50),
    type character varying(20) NOT NULL,
    rules text,
    start_time timestamp with time zone,
    end_time timestamp with time zone,
    status character varying(20) DEFAULT 'INACTIVE'::character varying NOT NULL,
    create_time timestamp with time zone,
    usage_count integer DEFAULT 0 NOT NULL,
    max_usage integer
)
WITH (fillfactor='90');


ALTER TABLE nju_market.promotions OWNER TO postgres;

--
-- Name: TABLE promotions; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON TABLE nju_market.promotions IS '促销活动表';


--
-- Name: COLUMN promotions.promotion_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.promotions.promotion_id IS '促销ID';


--
-- Name: COLUMN promotions.user_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.promotions.user_id IS '用户ID(用户专属促销)';


--
-- Name: COLUMN promotions.type; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.promotions.type IS '促销类型: COUPON-优惠券, FULL_REDUCE-满减, LIMITED_DISCOUNT-限时折扣';


--
-- Name: COLUMN promotions.rules; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.promotions.rules IS '促销规则(JSON格式)';


--
-- Name: COLUMN promotions.start_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.promotions.start_time IS '开始时间';


--
-- Name: COLUMN promotions.end_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.promotions.end_time IS '结束时间';


--
-- Name: COLUMN promotions.status; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.promotions.status IS '状态: ACTIVE-活跃, INACTIVE-未激活, EXPIRED-已过期, USED-已使用';


--
-- Name: COLUMN promotions.create_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.promotions.create_time IS '创建时间';


--
-- Name: COLUMN promotions.usage_count; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.promotions.usage_count IS '使用次数';


--
-- Name: COLUMN promotions.max_usage; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.promotions.max_usage IS '最大使用次数';


--
-- Name: return_reason_types; Type: TABLE; Schema: nju_market; Owner: postgres
--

CREATE TABLE nju_market.return_reason_types (
    reason_id character varying(20) NOT NULL,
    reason_name character varying(50) NOT NULL,
    description text,
    is_active boolean DEFAULT true NOT NULL
)
WITH (fillfactor='90');


ALTER TABLE nju_market.return_reason_types OWNER TO postgres;

--
-- Name: TABLE return_reason_types; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON TABLE nju_market.return_reason_types IS '退货原因类型表';


--
-- Name: COLUMN return_reason_types.reason_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.return_reason_types.reason_id IS '原因ID';


--
-- Name: COLUMN return_reason_types.reason_name; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.return_reason_types.reason_name IS '原因名称';


--
-- Name: COLUMN return_reason_types.description; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.return_reason_types.description IS '描述';


--
-- Name: COLUMN return_reason_types.is_active; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.return_reason_types.is_active IS '是否启用';


--
-- Name: return_records; Type: TABLE; Schema: nju_market; Owner: postgres
--

CREATE TABLE nju_market.return_records (
    return_id character varying(50) NOT NULL,
    order_id character varying(50) NOT NULL,
    return_reason text,
    return_request_time timestamp with time zone,
    return_approval_time timestamp with time zone,
    return_status character varying(20) NOT NULL,
    return_rejection_reason text,
    return_tracking_number character varying(100),
    return_completion_time timestamp with time zone,
    created_time timestamp with time zone,
    updated_time timestamp with time zone
)
WITH (fillfactor='90');


ALTER TABLE nju_market.return_records OWNER TO postgres;

--
-- Name: TABLE return_records; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON TABLE nju_market.return_records IS '退货记录表';


--
-- Name: COLUMN return_records.return_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.return_records.return_id IS '退货记录ID';


--
-- Name: COLUMN return_records.order_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.return_records.order_id IS '订单ID';


--
-- Name: COLUMN return_records.return_reason; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.return_records.return_reason IS '退货原因';


--
-- Name: COLUMN return_records.return_request_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.return_records.return_request_time IS '退货申请时间';


--
-- Name: COLUMN return_records.return_approval_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.return_records.return_approval_time IS '退货审批时间';


--
-- Name: COLUMN return_records.return_status; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.return_records.return_status IS '退货状态';


--
-- Name: COLUMN return_records.return_rejection_reason; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.return_records.return_rejection_reason IS '退货拒绝原因';


--
-- Name: COLUMN return_records.return_tracking_number; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.return_records.return_tracking_number IS '退货快递单号';


--
-- Name: COLUMN return_records.return_completion_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.return_records.return_completion_time IS '退货完成时间';


--
-- Name: COLUMN return_records.created_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.return_records.created_time IS '创建时间';


--
-- Name: COLUMN return_records.updated_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.return_records.updated_time IS '更新时间';


--
-- Name: user_activity_records; Type: TABLE; Schema: nju_market; Owner: postgres
--

CREATE TABLE nju_market.user_activity_records (
    record_id character varying(50) NOT NULL,
    user_id character varying(50) NOT NULL,
    activity_type character varying(50) NOT NULL,
    activity_time timestamp with time zone,
    activity_data text,
    ip_address character varying(50),
    user_agent character varying(500)
)
WITH (fillfactor='90');


ALTER TABLE nju_market.user_activity_records OWNER TO postgres;

--
-- Name: TABLE user_activity_records; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON TABLE nju_market.user_activity_records IS '用户活动记录表';


--
-- Name: COLUMN user_activity_records.record_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_activity_records.record_id IS '记录ID';


--
-- Name: COLUMN user_activity_records.user_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_activity_records.user_id IS '用户ID';


--
-- Name: COLUMN user_activity_records.activity_type; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_activity_records.activity_type IS '活动类型: LOGIN-登录, PUBLISH-发布, PURCHASE-购买, BROWSE-浏览, SEARCH-搜索';


--
-- Name: COLUMN user_activity_records.activity_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_activity_records.activity_time IS '活动时间';


--
-- Name: COLUMN user_activity_records.activity_data; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_activity_records.activity_data IS '活动数据(JSON格式)';


--
-- Name: COLUMN user_activity_records.ip_address; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_activity_records.ip_address IS 'IP地址';


--
-- Name: COLUMN user_activity_records.user_agent; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_activity_records.user_agent IS '用户代理';


--
-- Name: user_addresses; Type: TABLE; Schema: nju_market; Owner: postgres
--

CREATE TABLE nju_market.user_addresses (
    address_id character varying(50) NOT NULL,
    user_id character varying(50) NOT NULL,
    recipient_name character varying(100) NOT NULL,
    recipient_phone character varying(20) NOT NULL,
    province character varying(50) NOT NULL,
    city character varying(50) NOT NULL,
    district character varying(50) NOT NULL,
    street_address character varying(200) NOT NULL,
    detail_address character varying(500),
    full_address text NOT NULL,
    location public.geography(Point,4326),
    longitude double precision,
    latitude double precision,
    address_label character varying(20) DEFAULT 'HOME'::character varying,
    is_default boolean DEFAULT false NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE nju_market.user_addresses OWNER TO postgres;

--
-- Name: TABLE user_addresses; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON TABLE nju_market.user_addresses IS '用户地址表 - 存储用户的收货地址信息';


--
-- Name: COLUMN user_addresses.address_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_addresses.address_id IS '地址ID';


--
-- Name: COLUMN user_addresses.user_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_addresses.user_id IS '用户ID';


--
-- Name: COLUMN user_addresses.recipient_name; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_addresses.recipient_name IS '收货人姓名';


--
-- Name: COLUMN user_addresses.recipient_phone; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_addresses.recipient_phone IS '收货人电话';


--
-- Name: COLUMN user_addresses.province; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_addresses.province IS '省份';


--
-- Name: COLUMN user_addresses.city; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_addresses.city IS '城市';


--
-- Name: COLUMN user_addresses.district; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_addresses.district IS '区/县';


--
-- Name: COLUMN user_addresses.street_address; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_addresses.street_address IS '街道地址';


--
-- Name: COLUMN user_addresses.detail_address; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_addresses.detail_address IS '详细地址（楼栋、门牌号等）';


--
-- Name: COLUMN user_addresses.full_address; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_addresses.full_address IS '完整地址（拼接后的完整地址）';


--
-- Name: COLUMN user_addresses.location; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_addresses.location IS '地理位置（PostGIS Geography类型）';


--
-- Name: COLUMN user_addresses.longitude; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_addresses.longitude IS '经度';


--
-- Name: COLUMN user_addresses.latitude; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_addresses.latitude IS '纬度';


--
-- Name: COLUMN user_addresses.address_label; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_addresses.address_label IS '地址标签: HOME-家, SCHOOL-学校, COMPANY-公司, OTHER-其他';


--
-- Name: COLUMN user_addresses.is_default; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_addresses.is_default IS '是否默认地址';


--
-- Name: COLUMN user_addresses.is_active; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_addresses.is_active IS '是否启用';


--
-- Name: user_profile_vectors; Type: TABLE; Schema: nju_market; Owner: postgres
--

CREATE TABLE nju_market.user_profile_vectors (
    id bigint NOT NULL,
    user_id character varying(50) NOT NULL,
    embedding public.vector(2000),
    content text NOT NULL,
    metadata jsonb,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE nju_market.user_profile_vectors OWNER TO postgres;

--
-- Name: TABLE user_profile_vectors; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON TABLE nju_market.user_profile_vectors IS '用户画像向量表，用于存储用户的向量化画像（2000维）';


--
-- Name: COLUMN user_profile_vectors.embedding; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_profile_vectors.embedding IS '用户画像向量（2000维，HNSW索引限制）';


--
-- Name: user_profile_vectors_id_seq; Type: SEQUENCE; Schema: nju_market; Owner: postgres
--

CREATE SEQUENCE nju_market.user_profile_vectors_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE nju_market.user_profile_vectors_id_seq OWNER TO postgres;

--
-- Name: user_profile_vectors_id_seq; Type: SEQUENCE OWNED BY; Schema: nju_market; Owner: postgres
--

ALTER SEQUENCE nju_market.user_profile_vectors_id_seq OWNED BY nju_market.user_profile_vectors.id;


--
-- Name: user_profiles; Type: TABLE; Schema: nju_market; Owner: postgres
--

CREATE TABLE nju_market.user_profiles (
    profile_id character varying(50) NOT NULL,
    user_id character varying(50) NOT NULL,
    nickname character varying(50),
    avatar character varying(500),
    credit_score integer DEFAULT 100 NOT NULL,
    buyer_rating double precision,
    seller_rating double precision,
    total_sales integer DEFAULT 0 NOT NULL,
    total_purchases integer DEFAULT 0 NOT NULL,
    vip_level character varying(20) DEFAULT 'NORMAL'::character varying,
    buyer_order_has_new boolean,
    seller_order_has_new boolean
)
WITH (fillfactor='90');


ALTER TABLE nju_market.user_profiles OWNER TO postgres;

--
-- Name: TABLE user_profiles; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON TABLE nju_market.user_profiles IS '用户档案表';


--
-- Name: COLUMN user_profiles.profile_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_profiles.profile_id IS '档案ID';


--
-- Name: COLUMN user_profiles.user_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_profiles.user_id IS '用户ID';


--
-- Name: COLUMN user_profiles.nickname; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_profiles.nickname IS '昵称';


--
-- Name: COLUMN user_profiles.avatar; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_profiles.avatar IS '头像URL';


--
-- Name: COLUMN user_profiles.credit_score; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_profiles.credit_score IS '信用分';


--
-- Name: COLUMN user_profiles.total_sales; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_profiles.total_sales IS '总销售数';


--
-- Name: COLUMN user_profiles.total_purchases; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_profiles.total_purchases IS '总购买数';


--
-- Name: COLUMN user_profiles.vip_level; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.user_profiles.vip_level IS 'VIP等级: NORMAL-普通, BRONZE-青铜, SILVER-白银, GOLD-黄金, PLATINUM-铂金';


--
-- Name: users; Type: TABLE; Schema: nju_market; Owner: postgres
--

CREATE TABLE nju_market.users (
    user_id character varying(50) NOT NULL,
    primary_phone character varying(20) NOT NULL,
    username character varying(50),
    password character varying(255),
    register_time timestamp with time zone,
    account_status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL
)
WITH (fillfactor='90');


ALTER TABLE nju_market.users OWNER TO postgres;

--
-- Name: TABLE users; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON TABLE nju_market.users IS '用户表';


--
-- Name: COLUMN users.user_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.users.user_id IS '用户ID';


--
-- Name: COLUMN users.primary_phone; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.users.primary_phone IS '主要手机号';


--
-- Name: COLUMN users.username; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.users.username IS '用户名(可选)';


--
-- Name: COLUMN users.password; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.users.password IS '用户密码(加密存储)';


--
-- Name: COLUMN users.register_time; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.users.register_time IS '注册时间';


--
-- Name: COLUMN users.account_status; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.users.account_status IS '账户状态: ACTIVE-活跃, SUSPENDED-暂停, BANNED-封禁';


--
-- Name: visibility_types; Type: TABLE; Schema: nju_market; Owner: postgres
--

CREATE TABLE nju_market.visibility_types (
    type_id character varying(20) NOT NULL,
    type_name character varying(50) NOT NULL,
    description text,
    is_active boolean DEFAULT true NOT NULL
)
WITH (fillfactor='90');


ALTER TABLE nju_market.visibility_types OWNER TO postgres;

--
-- Name: TABLE visibility_types; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON TABLE nju_market.visibility_types IS '可见性类型表';


--
-- Name: COLUMN visibility_types.type_id; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.visibility_types.type_id IS '类型ID';


--
-- Name: COLUMN visibility_types.type_name; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.visibility_types.type_name IS '类型名称';


--
-- Name: COLUMN visibility_types.description; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.visibility_types.description IS '描述';


--
-- Name: COLUMN visibility_types.is_active; Type: COMMENT; Schema: nju_market; Owner: postgres
--

COMMENT ON COLUMN nju_market.visibility_types.is_active IS '是否启用';


--
-- Name: admin_operation_logs; Type: TABLE; Schema: nju_market_backup; Owner: postgres
--

CREATE TABLE nju_market_backup.admin_operation_logs (
    log_id character varying(50) NOT NULL,
    admin_id character varying(50) NOT NULL,
    operation_type character varying(50) NOT NULL,
    operation_desc character varying(500),
    target_id character varying(50),
    target_type character varying(50),
    operation_data text,
    ip_address character varying(50),
    user_agent character varying(500),
    create_time timestamp with time zone
)
WITH (fillfactor='90');


ALTER TABLE nju_market_backup.admin_operation_logs OWNER TO postgres;

--
-- Name: TABLE admin_operation_logs; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON TABLE nju_market_backup.admin_operation_logs IS '管理员操作日志表 - 记录管理员的操作行为';


--
-- Name: COLUMN admin_operation_logs.log_id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.admin_operation_logs.log_id IS '日志ID';


--
-- Name: COLUMN admin_operation_logs.admin_id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.admin_operation_logs.admin_id IS '管理员ID';


--
-- Name: COLUMN admin_operation_logs.operation_type; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.admin_operation_logs.operation_type IS '操作类型';


--
-- Name: COLUMN admin_operation_logs.operation_desc; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.admin_operation_logs.operation_desc IS '操作描述';


--
-- Name: COLUMN admin_operation_logs.target_id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.admin_operation_logs.target_id IS '目标对象ID';


--
-- Name: COLUMN admin_operation_logs.target_type; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.admin_operation_logs.target_type IS '目标对象类型';


--
-- Name: COLUMN admin_operation_logs.operation_data; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.admin_operation_logs.operation_data IS '操作数据（JSON格式）';


--
-- Name: COLUMN admin_operation_logs.ip_address; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.admin_operation_logs.ip_address IS '操作IP';


--
-- Name: COLUMN admin_operation_logs.user_agent; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.admin_operation_logs.user_agent IS '用户代理';


--
-- Name: COLUMN admin_operation_logs.create_time; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.admin_operation_logs.create_time IS '操作时间';


--
-- Name: admin_sessions; Type: TABLE; Schema: nju_market_backup; Owner: postgres
--

CREATE TABLE nju_market_backup.admin_sessions (
    session_id character varying(100) NOT NULL,
    admin_id character varying(50) NOT NULL,
    token character varying(500) NOT NULL,
    ip_address character varying(50),
    user_agent character varying(500),
    login_time timestamp with time zone,
    last_activity_time timestamp with time zone,
    expire_time timestamp with time zone,
    is_active boolean DEFAULT true NOT NULL
)
WITH (fillfactor='90');


ALTER TABLE nju_market_backup.admin_sessions OWNER TO postgres;

--
-- Name: TABLE admin_sessions; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON TABLE nju_market_backup.admin_sessions IS '管理员会话表 - 管理管理员登录会话';


--
-- Name: COLUMN admin_sessions.session_id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.admin_sessions.session_id IS '会话ID';


--
-- Name: COLUMN admin_sessions.admin_id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.admin_sessions.admin_id IS '管理员ID';


--
-- Name: COLUMN admin_sessions.token; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.admin_sessions.token IS 'JWT Token';


--
-- Name: COLUMN admin_sessions.ip_address; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.admin_sessions.ip_address IS '登录IP';


--
-- Name: COLUMN admin_sessions.user_agent; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.admin_sessions.user_agent IS '用户代理';


--
-- Name: COLUMN admin_sessions.login_time; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.admin_sessions.login_time IS '登录时间';


--
-- Name: COLUMN admin_sessions.last_activity_time; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.admin_sessions.last_activity_time IS '最后活动时间';


--
-- Name: COLUMN admin_sessions.expire_time; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.admin_sessions.expire_time IS '过期时间';


--
-- Name: COLUMN admin_sessions.is_active; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.admin_sessions.is_active IS '是否活跃';


--
-- Name: admins; Type: TABLE; Schema: nju_market_backup; Owner: postgres
--

CREATE TABLE nju_market_backup.admins (
    admin_id character varying(50) NOT NULL,
    username character varying(50) NOT NULL,
    password character varying(255) NOT NULL,
    real_name character varying(50),
    email character varying(100),
    department character varying(50),
    "position" character varying(50),
    admin_level character varying(20) DEFAULT 'administrator'::character varying NOT NULL,
    permissions text,
    create_time timestamp with time zone,
    update_time timestamp with time zone,
    last_login_time timestamp with time zone,
    last_login_ip character varying(50),
    account_status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    login_count integer DEFAULT 0 NOT NULL,
    remark character varying(500)
)
WITH (fillfactor='90');


ALTER TABLE nju_market_backup.admins OWNER TO postgres;

--
-- Name: TABLE admins; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON TABLE nju_market_backup.admins IS '管理员表 - 存储内部管理员账号信息';


--
-- Name: COLUMN admins.admin_id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.admins.admin_id IS '管理员ID';


--
-- Name: COLUMN admins.username; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.admins.username IS '用户名';


--
-- Name: COLUMN admins.password; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.admins.password IS '密码（加密存储）';


--
-- Name: COLUMN admins.real_name; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.admins.real_name IS '真实姓名';


--
-- Name: COLUMN admins.email; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.admins.email IS '邮箱';


--
-- Name: COLUMN admins.department; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.admins.department IS '部门';


--
-- Name: COLUMN admins."position"; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.admins."position" IS '职位';


--
-- Name: COLUMN admins.admin_level; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.admins.admin_level IS '管理员级别：system-系统管理员，administrator-普通管理员';


--
-- Name: COLUMN admins.permissions; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.admins.permissions IS '权限列表（JSON格式）';


--
-- Name: COLUMN admins.create_time; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.admins.create_time IS '创建时间';


--
-- Name: COLUMN admins.update_time; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.admins.update_time IS '更新时间';


--
-- Name: COLUMN admins.last_login_time; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.admins.last_login_time IS '最后登录时间';


--
-- Name: COLUMN admins.last_login_ip; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.admins.last_login_ip IS '最后登录IP';


--
-- Name: COLUMN admins.account_status; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.admins.account_status IS '账户状态：ACTIVE-活跃，SUSPENDED-暂停，BANNED-禁用';


--
-- Name: COLUMN admins.login_count; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.admins.login_count IS '登录次数';


--
-- Name: COLUMN admins.remark; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.admins.remark IS '备注';


--
-- Name: audit_records; Type: TABLE; Schema: nju_market_backup; Owner: postgres
--

CREATE TABLE nju_market_backup.audit_records (
    record_id character varying(50) NOT NULL,
    commodity_id character varying(50) NOT NULL,
    reviewer_id character varying(50),
    reason text,
    decision character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    audit_time timestamp with time zone,
    audit_type character varying(20) DEFAULT 'AUTO'::character varying NOT NULL
)
WITH (fillfactor='90');


ALTER TABLE nju_market_backup.audit_records OWNER TO postgres;

--
-- Name: TABLE audit_records; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON TABLE nju_market_backup.audit_records IS '审核记录表';


--
-- Name: COLUMN audit_records.record_id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.audit_records.record_id IS '记录ID';


--
-- Name: COLUMN audit_records.commodity_id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.audit_records.commodity_id IS '商品ID';


--
-- Name: COLUMN audit_records.reviewer_id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.audit_records.reviewer_id IS '审核员ID';


--
-- Name: COLUMN audit_records.reason; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.audit_records.reason IS '审核原因';


--
-- Name: COLUMN audit_records.decision; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.audit_records.decision IS '审核决定: APPROVED-通过, REJECTED-拒绝, PENDING-待审核';


--
-- Name: COLUMN audit_records.audit_time; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.audit_records.audit_time IS '审核时间';


--
-- Name: COLUMN audit_records.audit_type; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.audit_records.audit_type IS '审核类型: AUTO-自动审核, MANUAL-人工审核';


--
-- Name: ban_records; Type: TABLE; Schema: nju_market_backup; Owner: postgres
--

CREATE TABLE nju_market_backup.ban_records (
    ban_id character varying(50) NOT NULL,
    user_id character varying(50) NOT NULL,
    phone character varying(20),
    device_id character varying(100),
    real_name_id character varying(50),
    reason text NOT NULL,
    start_at timestamp with time zone,
    end_at timestamp with time zone,
    ban_type character varying(20) NOT NULL,
    is_active boolean DEFAULT true NOT NULL
)
WITH (fillfactor='90');


ALTER TABLE nju_market_backup.ban_records OWNER TO postgres;

--
-- Name: TABLE ban_records; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON TABLE nju_market_backup.ban_records IS '封禁记录表';


--
-- Name: COLUMN ban_records.ban_id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.ban_records.ban_id IS '封禁ID';


--
-- Name: COLUMN ban_records.user_id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.ban_records.user_id IS '用户ID';


--
-- Name: COLUMN ban_records.phone; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.ban_records.phone IS '手机号';


--
-- Name: COLUMN ban_records.device_id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.ban_records.device_id IS '设备ID';


--
-- Name: COLUMN ban_records.real_name_id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.ban_records.real_name_id IS '实名ID';


--
-- Name: COLUMN ban_records.reason; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.ban_records.reason IS '封禁原因';


--
-- Name: COLUMN ban_records.start_at; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.ban_records.start_at IS '封禁开始时间';


--
-- Name: COLUMN ban_records.end_at; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.ban_records.end_at IS '封禁结束时间';


--
-- Name: COLUMN ban_records.ban_type; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.ban_records.ban_type IS '封禁类型: TEMPORARY-临时, PERMANENT-永久, DEVICE-设备, PHONE-手机, REAL_NAME-实名';


--
-- Name: COLUMN ban_records.is_active; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.ban_records.is_active IS '是否生效: 0-无效, 1-有效';


--
-- Name: commodities; Type: TABLE; Schema: nju_market_backup; Owner: postgres
--

CREATE TABLE nju_market_backup.commodities (
    commodity_id character varying(50) NOT NULL,
    seller_id character varying(50) NOT NULL,
    title character varying(200) NOT NULL,
    description text,
    price double precision NOT NULL,
    stock integer NOT NULL,
    location character varying(200),
    category character varying(50),
    condition_level character varying(20) DEFAULT 'GOOD'::character varying,
    images text,
    publish_time timestamp with time zone,
    commodity_status character varying(20) DEFAULT 'DRAFT'::character varying NOT NULL,
    seller_visibility character varying(20) DEFAULT 'PUBLIC'::character varying NOT NULL,
    buyer_visibility character varying(20) DEFAULT 'PUBLIC'::character varying NOT NULL,
    click_count integer DEFAULT 0 NOT NULL,
    report_count integer DEFAULT 0 NOT NULL
)
WITH (fillfactor='90');


ALTER TABLE nju_market_backup.commodities OWNER TO postgres;

--
-- Name: TABLE commodities; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON TABLE nju_market_backup.commodities IS '商品表';


--
-- Name: COLUMN commodities.commodity_id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.commodities.commodity_id IS '商品ID';


--
-- Name: COLUMN commodities.seller_id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.commodities.seller_id IS '卖家用户ID';


--
-- Name: COLUMN commodities.title; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.commodities.title IS '商品标题';


--
-- Name: COLUMN commodities.description; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.commodities.description IS '商品描述';


--
-- Name: COLUMN commodities.stock; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.commodities.stock IS '库存数量';


--
-- Name: COLUMN commodities.location; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.commodities.location IS '商品位置';


--
-- Name: COLUMN commodities.category; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.commodities.category IS '商品分类';


--
-- Name: COLUMN commodities.condition_level; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.commodities.condition_level IS '商品成色: EXCELLENT-优秀, GOOD-良好, FAIR-一般, POOR-较差';


--
-- Name: COLUMN commodities.images; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.commodities.images IS '商品图片URL列表(JSON格式)';


--
-- Name: COLUMN commodities.commodity_status; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.commodities.commodity_status IS '商品状态: DRAFT-草稿, PUBLISHED-已发布, SOLD_OUT-售罄, REMOVED-已下架';


--
-- Name: COLUMN commodities.seller_visibility; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.commodities.seller_visibility IS '卖家可见性: PUBLIC-公开, PRIVATE-私有, HIDDEN-隐藏';


--
-- Name: COLUMN commodities.buyer_visibility; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.commodities.buyer_visibility IS '买家可见性: PUBLIC-公开, PRIVATE-私有, HIDDEN-隐藏';


--
-- Name: COLUMN commodities.click_count; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.commodities.click_count IS '点击次数';


--
-- Name: COLUMN commodities.report_count; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.commodities.report_count IS '举报次数';


--
-- Name: commodity_categories; Type: TABLE; Schema: nju_market_backup; Owner: postgres
--

CREATE TABLE nju_market_backup.commodity_categories (
    category_id character varying(50) NOT NULL,
    category_name character varying(100) NOT NULL,
    parent_id character varying(50),
    sort_order integer DEFAULT 0 NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    create_time timestamp with time zone
)
WITH (fillfactor='90');


ALTER TABLE nju_market_backup.commodity_categories OWNER TO postgres;

--
-- Name: TABLE commodity_categories; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON TABLE nju_market_backup.commodity_categories IS '商品分类表';


--
-- Name: COLUMN commodity_categories.category_id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.commodity_categories.category_id IS '分类ID';


--
-- Name: COLUMN commodity_categories.category_name; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.commodity_categories.category_name IS '分类名称';


--
-- Name: COLUMN commodity_categories.parent_id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.commodity_categories.parent_id IS '父分类ID';


--
-- Name: COLUMN commodity_categories.sort_order; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.commodity_categories.sort_order IS '排序';


--
-- Name: COLUMN commodity_categories.is_active; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.commodity_categories.is_active IS '是否启用';


--
-- Name: COLUMN commodity_categories.create_time; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.commodity_categories.create_time IS '创建时间';


--
-- Name: commodity_snapshots; Type: TABLE; Schema: nju_market_backup; Owner: postgres
--

CREATE TABLE nju_market_backup.commodity_snapshots (
    snapshot_id character varying(50) NOT NULL,
    category character varying(50),
    commodity_status character varying(20),
    condition_level character varying(20),
    description text,
    images text,
    location character varying(200),
    original_commodity_id character varying(50),
    price double precision NOT NULL,
    seller_email character varying(100),
    seller_id character varying(50),
    seller_name character varying(100),
    seller_phone character varying(20),
    snapshot_time timestamp(6) with time zone,
    stock integer,
    title character varying(200) NOT NULL
)
WITH (fillfactor='90');


ALTER TABLE nju_market_backup.commodity_snapshots OWNER TO postgres;

--
-- Name: complaints; Type: TABLE; Schema: nju_market_backup; Owner: postgres
--

CREATE TABLE nju_market_backup.complaints (
    complaint_id character varying(50) NOT NULL,
    complainant_id character varying(50) NOT NULL,
    defendant_id character varying(50) NOT NULL,
    related_order_id character varying(50),
    content text NOT NULL,
    evidence_files text,
    status character varying(20) DEFAULT 'SUBMITTED'::character varying NOT NULL,
    submit_time timestamp with time zone,
    resolve_time timestamp with time zone
)
WITH (fillfactor='90');


ALTER TABLE nju_market_backup.complaints OWNER TO postgres;

--
-- Name: TABLE complaints; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON TABLE nju_market_backup.complaints IS '投诉表';


--
-- Name: COLUMN complaints.complaint_id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.complaints.complaint_id IS '投诉ID';


--
-- Name: COLUMN complaints.complainant_id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.complaints.complainant_id IS '投诉人用户ID';


--
-- Name: COLUMN complaints.defendant_id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.complaints.defendant_id IS '被投诉人用户ID';


--
-- Name: COLUMN complaints.related_order_id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.complaints.related_order_id IS '相关订单ID';


--
-- Name: COLUMN complaints.content; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.complaints.content IS '投诉内容';


--
-- Name: COLUMN complaints.evidence_files; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.complaints.evidence_files IS '证据文件列表(JSON格式)';


--
-- Name: COLUMN complaints.status; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.complaints.status IS '投诉状态: SUBMITTED-已提交, PROCESSING-处理中, RESOLVED-已解决, REJECTED-已拒绝';


--
-- Name: COLUMN complaints.submit_time; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.complaints.submit_time IS '提交时间';


--
-- Name: COLUMN complaints.resolve_time; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.complaints.resolve_time IS '解决时间';


--
-- Name: contact_blacklist; Type: TABLE; Schema: nju_market_backup; Owner: postgres
--

CREATE TABLE nju_market_backup.contact_blacklist (
    id bigint NOT NULL,
    user_id character varying(50) NOT NULL,
    blocked_user_id character varying(50) NOT NULL,
    reason character varying(255),
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
)
WITH (fillfactor='90');


ALTER TABLE nju_market_backup.contact_blacklist OWNER TO postgres;

--
-- Name: TABLE contact_blacklist; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON TABLE nju_market_backup.contact_blacklist IS '联系人黑名单表';


--
-- Name: COLUMN contact_blacklist.user_id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.contact_blacklist.user_id IS '用户ID';


--
-- Name: COLUMN contact_blacklist.blocked_user_id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.contact_blacklist.blocked_user_id IS '被屏蔽用户ID';


--
-- Name: COLUMN contact_blacklist.reason; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.contact_blacklist.reason IS '屏蔽原因';


--
-- Name: COLUMN contact_blacklist.created_at; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.contact_blacklist.created_at IS '屏蔽时间';


--
-- Name: contact_blacklist_id_seq; Type: SEQUENCE; Schema: nju_market_backup; Owner: postgres
--

CREATE SEQUENCE nju_market_backup.contact_blacklist_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE nju_market_backup.contact_blacklist_id_seq OWNER TO postgres;

--
-- Name: contact_blacklist_id_seq; Type: SEQUENCE OWNED BY; Schema: nju_market_backup; Owner: postgres
--

ALTER SEQUENCE nju_market_backup.contact_blacklist_id_seq OWNED BY nju_market_backup.contact_blacklist.id;


--
-- Name: contact_info; Type: TABLE; Schema: nju_market_backup; Owner: postgres
--

CREATE TABLE nju_market_backup.contact_info (
    contact_id character varying(50) NOT NULL,
    owner_id character varying(50) NOT NULL,
    type character varying(20) NOT NULL,
    value_encrypted character varying(500) NOT NULL
)
WITH (fillfactor='90');


ALTER TABLE nju_market_backup.contact_info OWNER TO postgres;

--
-- Name: TABLE contact_info; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON TABLE nju_market_backup.contact_info IS '联系方式表';


--
-- Name: COLUMN contact_info.contact_id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.contact_info.contact_id IS '联系方式ID';


--
-- Name: COLUMN contact_info.owner_id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.contact_info.owner_id IS '所有者用户ID';


--
-- Name: COLUMN contact_info.type; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.contact_info.type IS '联系方式类型: PHONE-电话, EMAIL-邮箱, WECHAT-微信, QQ-QQ';


--
-- Name: COLUMN contact_info.value_encrypted; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.contact_info.value_encrypted IS '加密后的联系方式值';


--
-- Name: conversations; Type: TABLE; Schema: nju_market_backup; Owner: postgres
--

CREATE TABLE nju_market_backup.conversations (
    conversation_id character varying(255) NOT NULL,
    last_message_content text,
    last_message_time timestamp with time zone,
    status character varying(20) DEFAULT 'ACTIVE'::character varying,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    user_id_1 character varying(50) NOT NULL,
    user_id_2 character varying(50) NOT NULL,
    user_1_count integer DEFAULT 0 NOT NULL,
    user_2_count integer DEFAULT 0 NOT NULL,
    user_1_visibility boolean NOT NULL,
    user_2_visibility boolean NOT NULL,
    user_1_last_message_content text,
    user_1_last_message_time timestamp(6) with time zone,
    user_2_last_message_content text,
    user_2_last_message_time timestamp(6) with time zone
)
WITH (fillfactor='90');


ALTER TABLE nju_market_backup.conversations OWNER TO postgres;

--
-- Name: TABLE conversations; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON TABLE nju_market_backup.conversations IS '对话表';


--
-- Name: COLUMN conversations.conversation_id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.conversations.conversation_id IS '对话ID';


--
-- Name: COLUMN conversations.last_message_content; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.conversations.last_message_content IS '最后一条消息内容';


--
-- Name: COLUMN conversations.last_message_time; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.conversations.last_message_time IS '最后消息时间';


--
-- Name: COLUMN conversations.status; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.conversations.status IS '对话状态：ACTIVE-活跃，ARCHIVED-已归档，DELETED-已删除';


--
-- Name: COLUMN conversations.created_at; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.conversations.created_at IS '创建时间';


--
-- Name: COLUMN conversations.updated_at; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.conversations.updated_at IS '更新时间';


--
-- Name: data_statistics; Type: TABLE; Schema: nju_market_backup; Owner: postgres
--

CREATE TABLE nju_market_backup.data_statistics (
    id bigint NOT NULL,
    cycle character varying(20) NOT NULL,
    dimension character varying(50) NOT NULL,
    value double precision NOT NULL,
    category character varying(50),
    date_key character varying(20) NOT NULL,
    create_time timestamp with time zone,
    extra_data text
)
WITH (fillfactor='90');


ALTER TABLE nju_market_backup.data_statistics OWNER TO postgres;

--
-- Name: TABLE data_statistics; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON TABLE nju_market_backup.data_statistics IS '数据统计表';


--
-- Name: COLUMN data_statistics.id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.data_statistics.id IS '自增主键';


--
-- Name: COLUMN data_statistics.cycle; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.data_statistics.cycle IS '统计周期: DAILY-日, WEEKLY-周, MONTHLY-月, YEARLY-年';


--
-- Name: COLUMN data_statistics.dimension; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.data_statistics.dimension IS '统计维度: SALES-销售, USER_ACTIVITY-用户活动, COMMODITY_VIEWS-商品浏览, REVENUE-收入';


--
-- Name: COLUMN data_statistics.category; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.data_statistics.category IS '分类';


--
-- Name: COLUMN data_statistics.date_key; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.data_statistics.date_key IS '日期键(格式: YYYY-MM-DD 或 YYYY-MM 或 YYYY)';


--
-- Name: COLUMN data_statistics.create_time; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.data_statistics.create_time IS '创建时间';


--
-- Name: COLUMN data_statistics.extra_data; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.data_statistics.extra_data IS '额外数据(JSON格式)';


--
-- Name: data_statistics_id_seq; Type: SEQUENCE; Schema: nju_market_backup; Owner: postgres
--

CREATE SEQUENCE nju_market_backup.data_statistics_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE nju_market_backup.data_statistics_id_seq OWNER TO postgres;

--
-- Name: data_statistics_id_seq; Type: SEQUENCE OWNED BY; Schema: nju_market_backup; Owner: postgres
--

ALTER SEQUENCE nju_market_backup.data_statistics_id_seq OWNED BY nju_market_backup.data_statistics.id;


--
-- Name: image_references; Type: TABLE; Schema: nju_market_backup; Owner: postgres
--

CREATE TABLE nju_market_backup.image_references (
    image_id bigint NOT NULL,
    deleted_time timestamp(6) with time zone,
    file_size bigint,
    image_path character varying(500) NOT NULL,
    image_type character varying(20) NOT NULL,
    is_deleted boolean,
    last_reference_time timestamp(6) with time zone,
    reference_count integer NOT NULL,
    upload_time timestamp(6) with time zone,
    upload_user_id character varying(50)
)
WITH (fillfactor='90');


ALTER TABLE nju_market_backup.image_references OWNER TO postgres;

--
-- Name: image_references_image_id_seq; Type: SEQUENCE; Schema: nju_market_backup; Owner: postgres
--

CREATE SEQUENCE nju_market_backup.image_references_image_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE nju_market_backup.image_references_image_id_seq OWNER TO postgres;

--
-- Name: image_references_image_id_seq; Type: SEQUENCE OWNED BY; Schema: nju_market_backup; Owner: postgres
--

ALTER SEQUENCE nju_market_backup.image_references_image_id_seq OWNED BY nju_market_backup.image_references.image_id;


--
-- Name: message_notification_settings; Type: TABLE; Schema: nju_market_backup; Owner: postgres
--

CREATE TABLE nju_market_backup.message_notification_settings (
    user_id character varying(50) NOT NULL,
    enable_email_notification boolean DEFAULT true,
    enable_push_notification boolean DEFAULT true,
    enable_sound boolean DEFAULT true,
    quiet_hours_start time without time zone,
    quiet_hours_end time without time zone,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
)
WITH (fillfactor='90');


ALTER TABLE nju_market_backup.message_notification_settings OWNER TO postgres;

--
-- Name: TABLE message_notification_settings; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON TABLE nju_market_backup.message_notification_settings IS '消息通知设置表';


--
-- Name: COLUMN message_notification_settings.user_id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.message_notification_settings.user_id IS '用户ID';


--
-- Name: COLUMN message_notification_settings.enable_email_notification; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.message_notification_settings.enable_email_notification IS '启用邮件通知';


--
-- Name: COLUMN message_notification_settings.enable_push_notification; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.message_notification_settings.enable_push_notification IS '启用推送通知';


--
-- Name: COLUMN message_notification_settings.enable_sound; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.message_notification_settings.enable_sound IS '启用声音提醒';


--
-- Name: COLUMN message_notification_settings.quiet_hours_start; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.message_notification_settings.quiet_hours_start IS '免打扰开始时间';


--
-- Name: COLUMN message_notification_settings.quiet_hours_end; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.message_notification_settings.quiet_hours_end IS '免打扰结束时间';


--
-- Name: COLUMN message_notification_settings.created_at; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.message_notification_settings.created_at IS '创建时间';


--
-- Name: COLUMN message_notification_settings.updated_at; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.message_notification_settings.updated_at IS '更新时间';


--
-- Name: messages; Type: TABLE; Schema: nju_market_backup; Owner: postgres
--

CREATE TABLE nju_market_backup.messages (
    message_id character varying(255) NOT NULL,
    conversation_id character varying(255) NOT NULL,
    sender_id character varying(50) NOT NULL,
    receiver_id character varying(50) NOT NULL,
    message_type character varying(20) DEFAULT 'TEXT'::character varying,
    content text NOT NULL,
    image_url character varying(500),
    is_read boolean DEFAULT false,
    read_time timestamp with time zone,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    deleted_by_receiver boolean,
    deleted_by_sender boolean,
    commodity_id character varying(50),
    order_id character varying(50)
)
WITH (fillfactor='90');


ALTER TABLE nju_market_backup.messages OWNER TO postgres;

--
-- Name: TABLE messages; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON TABLE nju_market_backup.messages IS '消息表';


--
-- Name: COLUMN messages.message_id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.messages.message_id IS '消息ID';


--
-- Name: COLUMN messages.conversation_id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.messages.conversation_id IS '对话ID';


--
-- Name: COLUMN messages.sender_id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.messages.sender_id IS '发送者ID';


--
-- Name: COLUMN messages.receiver_id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.messages.receiver_id IS '接收者ID';


--
-- Name: COLUMN messages.message_type; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.messages.message_type IS '消息类型：TEXT-文本，IMAGE-图片，COMMODITY-商品卡片，ORDER-订单卡片';


--
-- Name: COLUMN messages.content; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.messages.content IS '消息内容';


--
-- Name: COLUMN messages.image_url; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.messages.image_url IS '图片URL（当消息类型为IMAGE时）';


--
-- Name: COLUMN messages.is_read; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.messages.is_read IS '是否已读';


--
-- Name: COLUMN messages.read_time; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.messages.read_time IS '已读时间';


--
-- Name: COLUMN messages.created_at; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.messages.created_at IS '发送时间';


--
-- Name: order_snapshots; Type: TABLE; Schema: nju_market_backup; Owner: postgres
--

CREATE TABLE nju_market_backup.order_snapshots (
    snapshot_id character varying(50) NOT NULL,
    buyer_id character varying(50),
    commodity_snapshot_id character varying(50),
    order_status character varying(20),
    original_order_id character varying(50),
    pay_amount double precision,
    quantity integer,
    remark text,
    seller_id character varying(50),
    shipping_address text,
    snapshot_time timestamp(6) with time zone
)
WITH (fillfactor='90');


ALTER TABLE nju_market_backup.order_snapshots OWNER TO postgres;

--
-- Name: order_status_logs; Type: TABLE; Schema: nju_market_backup; Owner: postgres
--

CREATE TABLE nju_market_backup.order_status_logs (
    log_id character varying(50) NOT NULL,
    order_id character varying(50) NOT NULL,
    from_status character varying(20),
    to_status character varying(20) NOT NULL,
    operator_id character varying(50),
    operator_type character varying(20) NOT NULL,
    reason text,
    seller_visibility_before character varying(20),
    seller_visibility_after character varying(20),
    buyer_visibility_before character varying(20),
    buyer_visibility_after character varying(20),
    return_reason text,
    return_rejection_reason text,
    create_time timestamp with time zone
)
WITH (fillfactor='90');


ALTER TABLE nju_market_backup.order_status_logs OWNER TO postgres;

--
-- Name: TABLE order_status_logs; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON TABLE nju_market_backup.order_status_logs IS '订单状态变更记录表';


--
-- Name: COLUMN order_status_logs.log_id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.order_status_logs.log_id IS '日志ID';


--
-- Name: COLUMN order_status_logs.order_id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.order_status_logs.order_id IS '订单ID';


--
-- Name: COLUMN order_status_logs.from_status; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.order_status_logs.from_status IS '原状态';


--
-- Name: COLUMN order_status_logs.to_status; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.order_status_logs.to_status IS '新状态';


--
-- Name: COLUMN order_status_logs.operator_id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.order_status_logs.operator_id IS '操作者ID';


--
-- Name: COLUMN order_status_logs.operator_type; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.order_status_logs.operator_type IS '操作者类型: BUYER-买家, SELLER-卖家, ADMIN-管理员, SYSTEM-系统';


--
-- Name: COLUMN order_status_logs.reason; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.order_status_logs.reason IS '变更原因';


--
-- Name: COLUMN order_status_logs.seller_visibility_before; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.order_status_logs.seller_visibility_before IS '变更前卖家可见性';


--
-- Name: COLUMN order_status_logs.seller_visibility_after; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.order_status_logs.seller_visibility_after IS '变更后卖家可见性';


--
-- Name: COLUMN order_status_logs.buyer_visibility_before; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.order_status_logs.buyer_visibility_before IS '变更前买家可见性';


--
-- Name: COLUMN order_status_logs.buyer_visibility_after; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.order_status_logs.buyer_visibility_after IS '变更后买家可见性';


--
-- Name: COLUMN order_status_logs.return_reason; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.order_status_logs.return_reason IS '退货原因';


--
-- Name: COLUMN order_status_logs.return_rejection_reason; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.order_status_logs.return_rejection_reason IS '退货拒绝原因';


--
-- Name: COLUMN order_status_logs.create_time; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.order_status_logs.create_time IS '创建时间';


--
-- Name: orders; Type: TABLE; Schema: nju_market_backup; Owner: postgres
--

CREATE TABLE nju_market_backup.orders (
    order_id character varying(50) NOT NULL,
    buyer_id character varying(50) NOT NULL,
    seller_id character varying(50) NOT NULL,
    commodity_id character varying(50) NOT NULL,
    order_status character varying(20) DEFAULT 'CREATED'::character varying NOT NULL,
    seller_visibility character varying(20) DEFAULT 'PUBLIC'::character varying NOT NULL,
    buyer_visibility character varying(20) DEFAULT 'PUBLIC'::character varying NOT NULL,
    pay_amount double precision NOT NULL,
    create_time timestamp with time zone,
    shipping_time timestamp with time zone,
    delivery_time timestamp with time zone,
    tracking_number character varying(100),
    shipping_address text,
    remark text,
    return_reason text,
    return_request_time timestamp with time zone,
    return_approval_time timestamp with time zone,
    return_rejection_reason text,
    return_tracking_number character varying(100),
    return_completion_time timestamp with time zone,
    quantity integer DEFAULT 1 NOT NULL,
    commodity_snapshot_title character varying(200),
    commodity_snapshot_description text,
    commodity_snapshot_price double precision,
    commodity_snapshot_location character varying(200),
    commodity_snapshot_category character varying(50),
    commodity_snapshot_condition_level character varying(20),
    commodity_snapshot_images text,
    commodity_snapshot_status character varying(20),
    commodity_snapshot_seller_name character varying(100),
    commodity_snapshot_seller_phone character varying(20),
    commodity_snapshot_seller_email character varying(100),
    commodity_snapshot_time timestamp with time zone,
    pay_time timestamp(6) with time zone
)
WITH (fillfactor='90');


ALTER TABLE nju_market_backup.orders OWNER TO postgres;

--
-- Name: TABLE orders; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON TABLE nju_market_backup.orders IS '订单表-包含商品快照信息';


--
-- Name: COLUMN orders.order_id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.orders.order_id IS '订单ID';


--
-- Name: COLUMN orders.buyer_id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.orders.buyer_id IS '买家用户ID';


--
-- Name: COLUMN orders.seller_id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.orders.seller_id IS '卖家用户ID';


--
-- Name: COLUMN orders.commodity_id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.orders.commodity_id IS '商品ID';


--
-- Name: COLUMN orders.order_status; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.orders.order_status IS '订单状态: CREATED-已创建, PAID-已支付, SHIPPED-已发货, COMPLETED-已完成, CANCELLED-已取消, REFUNDED-已退款';


--
-- Name: COLUMN orders.seller_visibility; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.orders.seller_visibility IS '卖家可见性: PUBLIC-公开, PRIVATE-私有, HIDDEN-隐藏';


--
-- Name: COLUMN orders.buyer_visibility; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.orders.buyer_visibility IS '买家可见性: PUBLIC-公开, PRIVATE-私有, HIDDEN-隐藏';


--
-- Name: COLUMN orders.create_time; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.orders.create_time IS '创建时间';


--
-- Name: COLUMN orders.shipping_time; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.orders.shipping_time IS '发货时间';


--
-- Name: COLUMN orders.delivery_time; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.orders.delivery_time IS '签收时间';


--
-- Name: COLUMN orders.tracking_number; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.orders.tracking_number IS '快递单号';


--
-- Name: COLUMN orders.shipping_address; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.orders.shipping_address IS '收货地址';


--
-- Name: COLUMN orders.remark; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.orders.remark IS '订单备注';


--
-- Name: COLUMN orders.return_reason; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.orders.return_reason IS '退货原因';


--
-- Name: COLUMN orders.return_request_time; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.orders.return_request_time IS '退货申请时间';


--
-- Name: COLUMN orders.return_approval_time; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.orders.return_approval_time IS '退货审批时间';


--
-- Name: COLUMN orders.return_rejection_reason; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.orders.return_rejection_reason IS '退货拒绝原因';


--
-- Name: COLUMN orders.return_tracking_number; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.orders.return_tracking_number IS '退货快递单号';


--
-- Name: COLUMN orders.return_completion_time; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.orders.return_completion_time IS '退货完成时间';


--
-- Name: COLUMN orders.quantity; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.orders.quantity IS '购买数量';


--
-- Name: COLUMN orders.commodity_snapshot_title; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.orders.commodity_snapshot_title IS '商品快照-标题';


--
-- Name: COLUMN orders.commodity_snapshot_description; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.orders.commodity_snapshot_description IS '商品快照-描述';


--
-- Name: COLUMN orders.commodity_snapshot_location; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.orders.commodity_snapshot_location IS '商品快照-位置';


--
-- Name: COLUMN orders.commodity_snapshot_category; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.orders.commodity_snapshot_category IS '商品快照-分类';


--
-- Name: COLUMN orders.commodity_snapshot_condition_level; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.orders.commodity_snapshot_condition_level IS '商品快照-成色';


--
-- Name: COLUMN orders.commodity_snapshot_images; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.orders.commodity_snapshot_images IS '商品快照-图片(JSON格式)';


--
-- Name: COLUMN orders.commodity_snapshot_status; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.orders.commodity_snapshot_status IS '商品快照-状态';


--
-- Name: COLUMN orders.commodity_snapshot_seller_name; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.orders.commodity_snapshot_seller_name IS '商品快照-卖家名称';


--
-- Name: COLUMN orders.commodity_snapshot_seller_phone; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.orders.commodity_snapshot_seller_phone IS '商品快照-卖家电话';


--
-- Name: COLUMN orders.commodity_snapshot_seller_email; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.orders.commodity_snapshot_seller_email IS '商品快照-卖家邮箱';


--
-- Name: COLUMN orders.commodity_snapshot_time; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.orders.commodity_snapshot_time IS '商品快照时间';


--
-- Name: promotions; Type: TABLE; Schema: nju_market_backup; Owner: postgres
--

CREATE TABLE nju_market_backup.promotions (
    promotion_id character varying(50) NOT NULL,
    user_id character varying(50),
    type character varying(20) NOT NULL,
    rules text,
    start_time timestamp with time zone,
    end_time timestamp with time zone,
    status character varying(20) DEFAULT 'INACTIVE'::character varying NOT NULL,
    create_time timestamp with time zone,
    usage_count integer DEFAULT 0 NOT NULL,
    max_usage integer
)
WITH (fillfactor='90');


ALTER TABLE nju_market_backup.promotions OWNER TO postgres;

--
-- Name: TABLE promotions; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON TABLE nju_market_backup.promotions IS '促销活动表';


--
-- Name: COLUMN promotions.promotion_id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.promotions.promotion_id IS '促销ID';


--
-- Name: COLUMN promotions.user_id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.promotions.user_id IS '用户ID(用户专属促销)';


--
-- Name: COLUMN promotions.type; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.promotions.type IS '促销类型: COUPON-优惠券, FULL_REDUCE-满减, LIMITED_DISCOUNT-限时折扣';


--
-- Name: COLUMN promotions.rules; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.promotions.rules IS '促销规则(JSON格式)';


--
-- Name: COLUMN promotions.start_time; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.promotions.start_time IS '开始时间';


--
-- Name: COLUMN promotions.end_time; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.promotions.end_time IS '结束时间';


--
-- Name: COLUMN promotions.status; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.promotions.status IS '状态: ACTIVE-活跃, INACTIVE-未激活, EXPIRED-已过期, USED-已使用';


--
-- Name: COLUMN promotions.create_time; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.promotions.create_time IS '创建时间';


--
-- Name: COLUMN promotions.usage_count; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.promotions.usage_count IS '使用次数';


--
-- Name: COLUMN promotions.max_usage; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.promotions.max_usage IS '最大使用次数';


--
-- Name: return_reason_types; Type: TABLE; Schema: nju_market_backup; Owner: postgres
--

CREATE TABLE nju_market_backup.return_reason_types (
    reason_id character varying(20) NOT NULL,
    reason_name character varying(50) NOT NULL,
    description text,
    is_active boolean DEFAULT true NOT NULL
)
WITH (fillfactor='90');


ALTER TABLE nju_market_backup.return_reason_types OWNER TO postgres;

--
-- Name: TABLE return_reason_types; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON TABLE nju_market_backup.return_reason_types IS '退货原因类型表';


--
-- Name: COLUMN return_reason_types.reason_id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.return_reason_types.reason_id IS '原因ID';


--
-- Name: COLUMN return_reason_types.reason_name; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.return_reason_types.reason_name IS '原因名称';


--
-- Name: COLUMN return_reason_types.description; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.return_reason_types.description IS '描述';


--
-- Name: COLUMN return_reason_types.is_active; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.return_reason_types.is_active IS '是否启用';


--
-- Name: return_records; Type: TABLE; Schema: nju_market_backup; Owner: postgres
--

CREATE TABLE nju_market_backup.return_records (
    return_id character varying(50) NOT NULL,
    order_id character varying(50) NOT NULL,
    return_reason text,
    return_request_time timestamp with time zone,
    return_approval_time timestamp with time zone,
    return_status character varying(20) NOT NULL,
    return_rejection_reason text,
    return_tracking_number character varying(100),
    return_completion_time timestamp with time zone,
    created_time timestamp with time zone,
    updated_time timestamp with time zone
)
WITH (fillfactor='90');


ALTER TABLE nju_market_backup.return_records OWNER TO postgres;

--
-- Name: TABLE return_records; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON TABLE nju_market_backup.return_records IS '退货记录表';


--
-- Name: COLUMN return_records.return_id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.return_records.return_id IS '退货记录ID';


--
-- Name: COLUMN return_records.order_id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.return_records.order_id IS '订单ID';


--
-- Name: COLUMN return_records.return_reason; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.return_records.return_reason IS '退货原因';


--
-- Name: COLUMN return_records.return_request_time; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.return_records.return_request_time IS '退货申请时间';


--
-- Name: COLUMN return_records.return_approval_time; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.return_records.return_approval_time IS '退货审批时间';


--
-- Name: COLUMN return_records.return_status; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.return_records.return_status IS '退货状态';


--
-- Name: COLUMN return_records.return_rejection_reason; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.return_records.return_rejection_reason IS '退货拒绝原因';


--
-- Name: COLUMN return_records.return_tracking_number; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.return_records.return_tracking_number IS '退货快递单号';


--
-- Name: COLUMN return_records.return_completion_time; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.return_records.return_completion_time IS '退货完成时间';


--
-- Name: COLUMN return_records.created_time; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.return_records.created_time IS '创建时间';


--
-- Name: COLUMN return_records.updated_time; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.return_records.updated_time IS '更新时间';


--
-- Name: user_activity_records; Type: TABLE; Schema: nju_market_backup; Owner: postgres
--

CREATE TABLE nju_market_backup.user_activity_records (
    record_id character varying(50) NOT NULL,
    user_id character varying(50) NOT NULL,
    activity_type character varying(50) NOT NULL,
    activity_time timestamp with time zone,
    activity_data text,
    ip_address character varying(50),
    user_agent character varying(500)
)
WITH (fillfactor='90');


ALTER TABLE nju_market_backup.user_activity_records OWNER TO postgres;

--
-- Name: TABLE user_activity_records; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON TABLE nju_market_backup.user_activity_records IS '用户活动记录表';


--
-- Name: COLUMN user_activity_records.record_id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.user_activity_records.record_id IS '记录ID';


--
-- Name: COLUMN user_activity_records.user_id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.user_activity_records.user_id IS '用户ID';


--
-- Name: COLUMN user_activity_records.activity_type; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.user_activity_records.activity_type IS '活动类型: LOGIN-登录, PUBLISH-发布, PURCHASE-购买, BROWSE-浏览, SEARCH-搜索';


--
-- Name: COLUMN user_activity_records.activity_time; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.user_activity_records.activity_time IS '活动时间';


--
-- Name: COLUMN user_activity_records.activity_data; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.user_activity_records.activity_data IS '活动数据(JSON格式)';


--
-- Name: COLUMN user_activity_records.ip_address; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.user_activity_records.ip_address IS 'IP地址';


--
-- Name: COLUMN user_activity_records.user_agent; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.user_activity_records.user_agent IS '用户代理';


--
-- Name: user_profiles; Type: TABLE; Schema: nju_market_backup; Owner: postgres
--

CREATE TABLE nju_market_backup.user_profiles (
    profile_id character varying(50) NOT NULL,
    user_id character varying(50) NOT NULL,
    nickname character varying(50),
    avatar character varying(500),
    credit_score integer DEFAULT 100 NOT NULL,
    buyer_rating double precision,
    seller_rating double precision,
    total_sales integer DEFAULT 0 NOT NULL,
    total_purchases integer DEFAULT 0 NOT NULL,
    vip_level character varying(20) DEFAULT 'NORMAL'::character varying,
    buyer_order_has_new boolean,
    seller_order_has_new boolean
)
WITH (fillfactor='90');


ALTER TABLE nju_market_backup.user_profiles OWNER TO postgres;

--
-- Name: TABLE user_profiles; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON TABLE nju_market_backup.user_profiles IS '用户档案表';


--
-- Name: COLUMN user_profiles.profile_id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.user_profiles.profile_id IS '档案ID';


--
-- Name: COLUMN user_profiles.user_id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.user_profiles.user_id IS '用户ID';


--
-- Name: COLUMN user_profiles.nickname; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.user_profiles.nickname IS '昵称';


--
-- Name: COLUMN user_profiles.avatar; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.user_profiles.avatar IS '头像URL';


--
-- Name: COLUMN user_profiles.credit_score; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.user_profiles.credit_score IS '信用分';


--
-- Name: COLUMN user_profiles.total_sales; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.user_profiles.total_sales IS '总销售数';


--
-- Name: COLUMN user_profiles.total_purchases; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.user_profiles.total_purchases IS '总购买数';


--
-- Name: COLUMN user_profiles.vip_level; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.user_profiles.vip_level IS 'VIP等级: NORMAL-普通, BRONZE-青铜, SILVER-白银, GOLD-黄金, PLATINUM-铂金';


--
-- Name: users; Type: TABLE; Schema: nju_market_backup; Owner: postgres
--

CREATE TABLE nju_market_backup.users (
    user_id character varying(50) NOT NULL,
    primary_phone character varying(20) NOT NULL,
    username character varying(50),
    password character varying(255),
    register_time timestamp with time zone,
    account_status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL
)
WITH (fillfactor='90');


ALTER TABLE nju_market_backup.users OWNER TO postgres;

--
-- Name: TABLE users; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON TABLE nju_market_backup.users IS '用户表';


--
-- Name: COLUMN users.user_id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.users.user_id IS '用户ID';


--
-- Name: COLUMN users.primary_phone; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.users.primary_phone IS '主要手机号';


--
-- Name: COLUMN users.username; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.users.username IS '用户名(可选)';


--
-- Name: COLUMN users.password; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.users.password IS '用户密码(加密存储)';


--
-- Name: COLUMN users.register_time; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.users.register_time IS '注册时间';


--
-- Name: COLUMN users.account_status; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.users.account_status IS '账户状态: ACTIVE-活跃, SUSPENDED-暂停, BANNED-封禁';


--
-- Name: visibility_types; Type: TABLE; Schema: nju_market_backup; Owner: postgres
--

CREATE TABLE nju_market_backup.visibility_types (
    type_id character varying(20) NOT NULL,
    type_name character varying(50) NOT NULL,
    description text,
    is_active boolean DEFAULT true NOT NULL
)
WITH (fillfactor='90');


ALTER TABLE nju_market_backup.visibility_types OWNER TO postgres;

--
-- Name: TABLE visibility_types; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON TABLE nju_market_backup.visibility_types IS '可见性类型表';


--
-- Name: COLUMN visibility_types.type_id; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.visibility_types.type_id IS '类型ID';


--
-- Name: COLUMN visibility_types.type_name; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.visibility_types.type_name IS '类型名称';


--
-- Name: COLUMN visibility_types.description; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.visibility_types.description IS '描述';


--
-- Name: COLUMN visibility_types.is_active; Type: COMMENT; Schema: nju_market_backup; Owner: postgres
--

COMMENT ON COLUMN nju_market_backup.visibility_types.is_active IS '是否启用';


--
-- Name: commodity_vectors id; Type: DEFAULT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.commodity_vectors ALTER COLUMN id SET DEFAULT nextval('nju_market.commodity_vectors_id_seq'::regclass);


--
-- Name: contact_blacklist id; Type: DEFAULT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.contact_blacklist ALTER COLUMN id SET DEFAULT nextval('nju_market.contact_blacklist_id_seq'::regclass);


--
-- Name: conversation_vectors id; Type: DEFAULT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.conversation_vectors ALTER COLUMN id SET DEFAULT nextval('nju_market.conversation_vectors_id_seq'::regclass);


--
-- Name: data_statistics id; Type: DEFAULT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.data_statistics ALTER COLUMN id SET DEFAULT nextval('nju_market.data_statistics_id_seq'::regclass);


--
-- Name: image_references image_id; Type: DEFAULT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.image_references ALTER COLUMN image_id SET DEFAULT nextval('nju_market.image_references_image_id_seq'::regclass);


--
-- Name: user_profile_vectors id; Type: DEFAULT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.user_profile_vectors ALTER COLUMN id SET DEFAULT nextval('nju_market.user_profile_vectors_id_seq'::regclass);


--
-- Name: contact_blacklist id; Type: DEFAULT; Schema: nju_market_backup; Owner: postgres
--

ALTER TABLE ONLY nju_market_backup.contact_blacklist ALTER COLUMN id SET DEFAULT nextval('nju_market_backup.contact_blacklist_id_seq'::regclass);


--
-- Name: data_statistics id; Type: DEFAULT; Schema: nju_market_backup; Owner: postgres
--

ALTER TABLE ONLY nju_market_backup.data_statistics ALTER COLUMN id SET DEFAULT nextval('nju_market_backup.data_statistics_id_seq'::regclass);


--
-- Name: image_references image_id; Type: DEFAULT; Schema: nju_market_backup; Owner: postgres
--

ALTER TABLE ONLY nju_market_backup.image_references ALTER COLUMN image_id SET DEFAULT nextval('nju_market_backup.image_references_image_id_seq'::regclass);


--
-- Name: ai_conversations ai_conversations_pkey; Type: CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.ai_conversations
    ADD CONSTRAINT ai_conversations_pkey PRIMARY KEY (conversation_id);


--
-- Name: commodity_vectors commodity_vectors_commodity_id_key; Type: CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.commodity_vectors
    ADD CONSTRAINT commodity_vectors_commodity_id_key UNIQUE (commodity_id);


--
-- Name: commodity_vectors commodity_vectors_pkey; Type: CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.commodity_vectors
    ADD CONSTRAINT commodity_vectors_pkey PRIMARY KEY (id);


--
-- Name: conversation_vectors conversation_vectors_pkey; Type: CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.conversation_vectors
    ADD CONSTRAINT conversation_vectors_pkey PRIMARY KEY (id);


--
-- Name: admin_operation_logs idx_21713_primary; Type: CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.admin_operation_logs
    ADD CONSTRAINT idx_21713_primary PRIMARY KEY (log_id);


--
-- Name: admin_sessions idx_21718_primary; Type: CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.admin_sessions
    ADD CONSTRAINT idx_21718_primary PRIMARY KEY (session_id);


--
-- Name: admins idx_21724_primary; Type: CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.admins
    ADD CONSTRAINT idx_21724_primary PRIMARY KEY (admin_id);


--
-- Name: audit_records idx_21732_primary; Type: CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.audit_records
    ADD CONSTRAINT idx_21732_primary PRIMARY KEY (record_id);


--
-- Name: ban_records idx_21739_primary; Type: CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.ban_records
    ADD CONSTRAINT idx_21739_primary PRIMARY KEY (ban_id);


--
-- Name: commodities idx_21745_primary; Type: CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.commodities
    ADD CONSTRAINT idx_21745_primary PRIMARY KEY (commodity_id);


--
-- Name: commodity_categories idx_21756_primary; Type: CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.commodity_categories
    ADD CONSTRAINT idx_21756_primary PRIMARY KEY (category_id);


--
-- Name: commodity_snapshots idx_21761_primary; Type: CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.commodity_snapshots
    ADD CONSTRAINT idx_21761_primary PRIMARY KEY (snapshot_id);


--
-- Name: complaints idx_21766_primary; Type: CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.complaints
    ADD CONSTRAINT idx_21766_primary PRIMARY KEY (complaint_id);


--
-- Name: contact_blacklist idx_21773_primary; Type: CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.contact_blacklist
    ADD CONSTRAINT idx_21773_primary PRIMARY KEY (id);


--
-- Name: contact_info idx_21778_primary; Type: CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.contact_info
    ADD CONSTRAINT idx_21778_primary PRIMARY KEY (contact_id);


--
-- Name: conversations idx_21783_primary; Type: CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.conversations
    ADD CONSTRAINT idx_21783_primary PRIMARY KEY (conversation_id);


--
-- Name: data_statistics idx_21794_primary; Type: CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.data_statistics
    ADD CONSTRAINT idx_21794_primary PRIMARY KEY (id);


--
-- Name: image_references idx_21801_primary; Type: CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.image_references
    ADD CONSTRAINT idx_21801_primary PRIMARY KEY (image_id);


--
-- Name: message_notification_settings idx_21807_primary; Type: CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.message_notification_settings
    ADD CONSTRAINT idx_21807_primary PRIMARY KEY (user_id);


--
-- Name: messages idx_21815_primary; Type: CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.messages
    ADD CONSTRAINT idx_21815_primary PRIMARY KEY (message_id);


--
-- Name: order_snapshots idx_21823_primary; Type: CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.order_snapshots
    ADD CONSTRAINT idx_21823_primary PRIMARY KEY (snapshot_id);


--
-- Name: order_status_logs idx_21828_primary; Type: CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.order_status_logs
    ADD CONSTRAINT idx_21828_primary PRIMARY KEY (log_id);


--
-- Name: orders idx_21833_primary; Type: CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.orders
    ADD CONSTRAINT idx_21833_primary PRIMARY KEY (order_id);


--
-- Name: promotions idx_21842_primary; Type: CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.promotions
    ADD CONSTRAINT idx_21842_primary PRIMARY KEY (promotion_id);


--
-- Name: return_reason_types idx_21849_primary; Type: CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.return_reason_types
    ADD CONSTRAINT idx_21849_primary PRIMARY KEY (reason_id);


--
-- Name: return_records idx_21855_primary; Type: CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.return_records
    ADD CONSTRAINT idx_21855_primary PRIMARY KEY (return_id);


--
-- Name: user_activity_records idx_21860_primary; Type: CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.user_activity_records
    ADD CONSTRAINT idx_21860_primary PRIMARY KEY (record_id);


--
-- Name: user_profiles idx_21865_primary; Type: CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.user_profiles
    ADD CONSTRAINT idx_21865_primary PRIMARY KEY (profile_id);


--
-- Name: users idx_21874_primary; Type: CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.users
    ADD CONSTRAINT idx_21874_primary PRIMARY KEY (user_id);


--
-- Name: visibility_types idx_21878_primary; Type: CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.visibility_types
    ADD CONSTRAINT idx_21878_primary PRIMARY KEY (type_id);


--
-- Name: user_addresses user_addresses_pkey; Type: CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.user_addresses
    ADD CONSTRAINT user_addresses_pkey PRIMARY KEY (address_id);


--
-- Name: user_profile_vectors user_profile_vectors_pkey; Type: CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.user_profile_vectors
    ADD CONSTRAINT user_profile_vectors_pkey PRIMARY KEY (id);


--
-- Name: user_profile_vectors user_profile_vectors_user_id_key; Type: CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.user_profile_vectors
    ADD CONSTRAINT user_profile_vectors_user_id_key UNIQUE (user_id);


--
-- Name: admin_operation_logs idx_21276_primary; Type: CONSTRAINT; Schema: nju_market_backup; Owner: postgres
--

ALTER TABLE ONLY nju_market_backup.admin_operation_logs
    ADD CONSTRAINT idx_21276_primary PRIMARY KEY (log_id);


--
-- Name: admin_sessions idx_21281_primary; Type: CONSTRAINT; Schema: nju_market_backup; Owner: postgres
--

ALTER TABLE ONLY nju_market_backup.admin_sessions
    ADD CONSTRAINT idx_21281_primary PRIMARY KEY (session_id);


--
-- Name: admins idx_21287_primary; Type: CONSTRAINT; Schema: nju_market_backup; Owner: postgres
--

ALTER TABLE ONLY nju_market_backup.admins
    ADD CONSTRAINT idx_21287_primary PRIMARY KEY (admin_id);


--
-- Name: audit_records idx_21295_primary; Type: CONSTRAINT; Schema: nju_market_backup; Owner: postgres
--

ALTER TABLE ONLY nju_market_backup.audit_records
    ADD CONSTRAINT idx_21295_primary PRIMARY KEY (record_id);


--
-- Name: ban_records idx_21302_primary; Type: CONSTRAINT; Schema: nju_market_backup; Owner: postgres
--

ALTER TABLE ONLY nju_market_backup.ban_records
    ADD CONSTRAINT idx_21302_primary PRIMARY KEY (ban_id);


--
-- Name: commodities idx_21308_primary; Type: CONSTRAINT; Schema: nju_market_backup; Owner: postgres
--

ALTER TABLE ONLY nju_market_backup.commodities
    ADD CONSTRAINT idx_21308_primary PRIMARY KEY (commodity_id);


--
-- Name: commodity_categories idx_21319_primary; Type: CONSTRAINT; Schema: nju_market_backup; Owner: postgres
--

ALTER TABLE ONLY nju_market_backup.commodity_categories
    ADD CONSTRAINT idx_21319_primary PRIMARY KEY (category_id);


--
-- Name: commodity_snapshots idx_21324_primary; Type: CONSTRAINT; Schema: nju_market_backup; Owner: postgres
--

ALTER TABLE ONLY nju_market_backup.commodity_snapshots
    ADD CONSTRAINT idx_21324_primary PRIMARY KEY (snapshot_id);


--
-- Name: complaints idx_21329_primary; Type: CONSTRAINT; Schema: nju_market_backup; Owner: postgres
--

ALTER TABLE ONLY nju_market_backup.complaints
    ADD CONSTRAINT idx_21329_primary PRIMARY KEY (complaint_id);


--
-- Name: contact_blacklist idx_21336_primary; Type: CONSTRAINT; Schema: nju_market_backup; Owner: postgres
--

ALTER TABLE ONLY nju_market_backup.contact_blacklist
    ADD CONSTRAINT idx_21336_primary PRIMARY KEY (id);


--
-- Name: contact_info idx_21341_primary; Type: CONSTRAINT; Schema: nju_market_backup; Owner: postgres
--

ALTER TABLE ONLY nju_market_backup.contact_info
    ADD CONSTRAINT idx_21341_primary PRIMARY KEY (contact_id);


--
-- Name: conversations idx_21346_primary; Type: CONSTRAINT; Schema: nju_market_backup; Owner: postgres
--

ALTER TABLE ONLY nju_market_backup.conversations
    ADD CONSTRAINT idx_21346_primary PRIMARY KEY (conversation_id);


--
-- Name: data_statistics idx_21357_primary; Type: CONSTRAINT; Schema: nju_market_backup; Owner: postgres
--

ALTER TABLE ONLY nju_market_backup.data_statistics
    ADD CONSTRAINT idx_21357_primary PRIMARY KEY (id);


--
-- Name: image_references idx_21364_primary; Type: CONSTRAINT; Schema: nju_market_backup; Owner: postgres
--

ALTER TABLE ONLY nju_market_backup.image_references
    ADD CONSTRAINT idx_21364_primary PRIMARY KEY (image_id);


--
-- Name: message_notification_settings idx_21370_primary; Type: CONSTRAINT; Schema: nju_market_backup; Owner: postgres
--

ALTER TABLE ONLY nju_market_backup.message_notification_settings
    ADD CONSTRAINT idx_21370_primary PRIMARY KEY (user_id);


--
-- Name: messages idx_21378_primary; Type: CONSTRAINT; Schema: nju_market_backup; Owner: postgres
--

ALTER TABLE ONLY nju_market_backup.messages
    ADD CONSTRAINT idx_21378_primary PRIMARY KEY (message_id);


--
-- Name: order_snapshots idx_21386_primary; Type: CONSTRAINT; Schema: nju_market_backup; Owner: postgres
--

ALTER TABLE ONLY nju_market_backup.order_snapshots
    ADD CONSTRAINT idx_21386_primary PRIMARY KEY (snapshot_id);


--
-- Name: order_status_logs idx_21391_primary; Type: CONSTRAINT; Schema: nju_market_backup; Owner: postgres
--

ALTER TABLE ONLY nju_market_backup.order_status_logs
    ADD CONSTRAINT idx_21391_primary PRIMARY KEY (log_id);


--
-- Name: orders idx_21396_primary; Type: CONSTRAINT; Schema: nju_market_backup; Owner: postgres
--

ALTER TABLE ONLY nju_market_backup.orders
    ADD CONSTRAINT idx_21396_primary PRIMARY KEY (order_id);


--
-- Name: promotions idx_21405_primary; Type: CONSTRAINT; Schema: nju_market_backup; Owner: postgres
--

ALTER TABLE ONLY nju_market_backup.promotions
    ADD CONSTRAINT idx_21405_primary PRIMARY KEY (promotion_id);


--
-- Name: return_reason_types idx_21412_primary; Type: CONSTRAINT; Schema: nju_market_backup; Owner: postgres
--

ALTER TABLE ONLY nju_market_backup.return_reason_types
    ADD CONSTRAINT idx_21412_primary PRIMARY KEY (reason_id);


--
-- Name: return_records idx_21418_primary; Type: CONSTRAINT; Schema: nju_market_backup; Owner: postgres
--

ALTER TABLE ONLY nju_market_backup.return_records
    ADD CONSTRAINT idx_21418_primary PRIMARY KEY (return_id);


--
-- Name: user_activity_records idx_21423_primary; Type: CONSTRAINT; Schema: nju_market_backup; Owner: postgres
--

ALTER TABLE ONLY nju_market_backup.user_activity_records
    ADD CONSTRAINT idx_21423_primary PRIMARY KEY (record_id);


--
-- Name: user_profiles idx_21428_primary; Type: CONSTRAINT; Schema: nju_market_backup; Owner: postgres
--

ALTER TABLE ONLY nju_market_backup.user_profiles
    ADD CONSTRAINT idx_21428_primary PRIMARY KEY (profile_id);


--
-- Name: users idx_21437_primary; Type: CONSTRAINT; Schema: nju_market_backup; Owner: postgres
--

ALTER TABLE ONLY nju_market_backup.users
    ADD CONSTRAINT idx_21437_primary PRIMARY KEY (user_id);


--
-- Name: visibility_types idx_21441_primary; Type: CONSTRAINT; Schema: nju_market_backup; Owner: postgres
--

ALTER TABLE ONLY nju_market_backup.visibility_types
    ADD CONSTRAINT idx_21441_primary PRIMARY KEY (type_id);


--
-- Name: ai_conversations_created_at_idx; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX ai_conversations_created_at_idx ON nju_market.ai_conversations USING btree (created_at DESC);


--
-- Name: ai_conversations_status_idx; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX ai_conversations_status_idx ON nju_market.ai_conversations USING btree (status);


--
-- Name: ai_conversations_updated_at_idx; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX ai_conversations_updated_at_idx ON nju_market.ai_conversations USING btree (updated_at DESC);


--
-- Name: ai_conversations_user_id_idx; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX ai_conversations_user_id_idx ON nju_market.ai_conversations USING btree (user_id);


--
-- Name: commodity_vectors_commodity_id_idx; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX commodity_vectors_commodity_id_idx ON nju_market.commodity_vectors USING btree (commodity_id);


--
-- Name: commodity_vectors_embedding_idx; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX commodity_vectors_embedding_idx ON nju_market.commodity_vectors USING hnsw (embedding public.vector_cosine_ops) WITH (m='16', ef_construction='64');


--
-- Name: conversation_vectors_conversation_id_idx; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX conversation_vectors_conversation_id_idx ON nju_market.conversation_vectors USING btree (conversation_id);


--
-- Name: conversation_vectors_created_at_idx; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX conversation_vectors_created_at_idx ON nju_market.conversation_vectors USING btree (created_at DESC);


--
-- Name: conversation_vectors_embedding_idx; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX conversation_vectors_embedding_idx ON nju_market.conversation_vectors USING hnsw (embedding public.vector_cosine_ops) WITH (m='16', ef_construction='64');


--
-- Name: conversation_vectors_user_id_idx; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX conversation_vectors_user_id_idx ON nju_market.conversation_vectors USING btree (user_id);


--
-- Name: idx_21713_idx_admin_id; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21713_idx_admin_id ON nju_market.admin_operation_logs USING btree (admin_id);


--
-- Name: idx_21713_idx_create_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21713_idx_create_time ON nju_market.admin_operation_logs USING btree (create_time);


--
-- Name: idx_21713_idx_operation_type; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21713_idx_operation_type ON nju_market.admin_operation_logs USING btree (operation_type);


--
-- Name: idx_21718_idx_admin_id; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21718_idx_admin_id ON nju_market.admin_sessions USING btree (admin_id);


--
-- Name: idx_21718_idx_expire_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21718_idx_expire_time ON nju_market.admin_sessions USING btree (expire_time);


--
-- Name: idx_21718_idx_is_active; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21718_idx_is_active ON nju_market.admin_sessions USING btree (is_active);


--
-- Name: idx_21718_idx_token; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21718_idx_token ON nju_market.admin_sessions USING btree (token);


--
-- Name: idx_21724_idx_account_status; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21724_idx_account_status ON nju_market.admins USING btree (account_status);


--
-- Name: idx_21724_idx_admin_level; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21724_idx_admin_level ON nju_market.admins USING btree (admin_level);


--
-- Name: idx_21724_idx_create_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21724_idx_create_time ON nju_market.admins USING btree (create_time);


--
-- Name: idx_21724_idx_department; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21724_idx_department ON nju_market.admins USING btree (department);


--
-- Name: idx_21724_idx_last_login_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21724_idx_last_login_time ON nju_market.admins USING btree (last_login_time);


--
-- Name: idx_21724_idx_username; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21724_idx_username ON nju_market.admins USING btree (username);


--
-- Name: idx_21724_username; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE UNIQUE INDEX idx_21724_username ON nju_market.admins USING btree (username);


--
-- Name: idx_21732_idx_audit_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21732_idx_audit_time ON nju_market.audit_records USING btree (audit_time);


--
-- Name: idx_21732_idx_audit_type; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21732_idx_audit_type ON nju_market.audit_records USING btree (audit_type);


--
-- Name: idx_21732_idx_commodity_id; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21732_idx_commodity_id ON nju_market.audit_records USING btree (commodity_id);


--
-- Name: idx_21732_idx_decision; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21732_idx_decision ON nju_market.audit_records USING btree (decision);


--
-- Name: idx_21732_idx_reviewer_id; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21732_idx_reviewer_id ON nju_market.audit_records USING btree (reviewer_id);


--
-- Name: idx_21739_idx_ban_type; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21739_idx_ban_type ON nju_market.ban_records USING btree (ban_type);


--
-- Name: idx_21739_idx_device_id; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21739_idx_device_id ON nju_market.ban_records USING btree (device_id);


--
-- Name: idx_21739_idx_end_at; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21739_idx_end_at ON nju_market.ban_records USING btree (end_at);


--
-- Name: idx_21739_idx_is_active; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21739_idx_is_active ON nju_market.ban_records USING btree (is_active);


--
-- Name: idx_21739_idx_phone; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21739_idx_phone ON nju_market.ban_records USING btree (phone);


--
-- Name: idx_21739_idx_start_at; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21739_idx_start_at ON nju_market.ban_records USING btree (start_at);


--
-- Name: idx_21739_idx_user_id; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21739_idx_user_id ON nju_market.ban_records USING btree (user_id);


--
-- Name: idx_21745_idx_category; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21745_idx_category ON nju_market.commodities USING btree (category);


--
-- Name: idx_21745_idx_category_status_visibility; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21745_idx_category_status_visibility ON nju_market.commodities USING btree (category, commodity_status, seller_visibility, buyer_visibility);


--
-- Name: idx_21745_idx_click_count; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21745_idx_click_count ON nju_market.commodities USING btree (click_count);


--
-- Name: idx_21745_idx_condition_level; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21745_idx_condition_level ON nju_market.commodities USING btree (condition_level);


--
-- Name: idx_21745_idx_price; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21745_idx_price ON nju_market.commodities USING btree (price);


--
-- Name: idx_21745_idx_publish_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21745_idx_publish_time ON nju_market.commodities USING btree (publish_time);


--
-- Name: idx_21745_idx_seller_id; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21745_idx_seller_id ON nju_market.commodities USING btree (seller_id);


--
-- Name: idx_21745_idx_seller_publish_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21745_idx_seller_publish_time ON nju_market.commodities USING btree (seller_id, publish_time);


--
-- Name: idx_21745_idx_seller_status_publish_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21745_idx_seller_status_publish_time ON nju_market.commodities USING btree (seller_id, commodity_status, publish_time);


--
-- Name: idx_21745_idx_status_click_count; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21745_idx_status_click_count ON nju_market.commodities USING btree (commodity_status, click_count);


--
-- Name: idx_21745_idx_status_price; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21745_idx_status_price ON nju_market.commodities USING btree (commodity_status, price);


--
-- Name: idx_21745_idx_status_publish_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21745_idx_status_publish_time ON nju_market.commodities USING btree (commodity_status, publish_time);


--
-- Name: idx_21745_idx_status_seller_buyer_visibility; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21745_idx_status_seller_buyer_visibility ON nju_market.commodities USING btree (commodity_status, seller_visibility, buyer_visibility);


--
-- Name: idx_21745_idx_stock; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21745_idx_stock ON nju_market.commodities USING btree (stock);


--
-- Name: idx_21756_idx_is_active; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21756_idx_is_active ON nju_market.commodity_categories USING btree (is_active);


--
-- Name: idx_21756_idx_parent_id; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21756_idx_parent_id ON nju_market.commodity_categories USING btree (parent_id);


--
-- Name: idx_21756_idx_sort_order; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21756_idx_sort_order ON nju_market.commodity_categories USING btree (sort_order);


--
-- Name: idx_21766_idx_complainant_id; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21766_idx_complainant_id ON nju_market.complaints USING btree (complainant_id);


--
-- Name: idx_21766_idx_defendant_id; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21766_idx_defendant_id ON nju_market.complaints USING btree (defendant_id);


--
-- Name: idx_21766_idx_related_order_id; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21766_idx_related_order_id ON nju_market.complaints USING btree (related_order_id);


--
-- Name: idx_21766_idx_status; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21766_idx_status ON nju_market.complaints USING btree (status);


--
-- Name: idx_21766_idx_submit_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21766_idx_submit_time ON nju_market.complaints USING btree (submit_time);


--
-- Name: idx_21773_idx_blocked_user_id; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21773_idx_blocked_user_id ON nju_market.contact_blacklist USING btree (blocked_user_id);


--
-- Name: idx_21773_idx_user_id; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21773_idx_user_id ON nju_market.contact_blacklist USING btree (user_id);


--
-- Name: idx_21773_uk_user_blocked; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE UNIQUE INDEX idx_21773_uk_user_blocked ON nju_market.contact_blacklist USING btree (user_id, blocked_user_id);


--
-- Name: idx_21778_idx_owner_id; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21778_idx_owner_id ON nju_market.contact_info USING btree (owner_id);


--
-- Name: idx_21778_idx_type; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21778_idx_type ON nju_market.contact_info USING btree (type);


--
-- Name: idx_21783_idx_last_message_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21783_idx_last_message_time ON nju_market.conversations USING btree (last_message_time);


--
-- Name: idx_21783_idx_user1_status_visibility_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21783_idx_user1_status_visibility_time ON nju_market.conversations USING btree (user_id_1, status, user_1_visibility, last_message_time);


--
-- Name: idx_21783_idx_user2_status_visibility_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21783_idx_user2_status_visibility_time ON nju_market.conversations USING btree (user_id_2, status, user_2_visibility, last_message_time);


--
-- Name: idx_21783_uk_user_pair_active; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE UNIQUE INDEX idx_21783_uk_user_pair_active ON nju_market.conversations USING btree (user_id_1, user_id_2, status);


--
-- Name: idx_21794_idx_category; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21794_idx_category ON nju_market.data_statistics USING btree (category);


--
-- Name: idx_21794_idx_create_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21794_idx_create_time ON nju_market.data_statistics USING btree (create_time);


--
-- Name: idx_21794_idx_cycle; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21794_idx_cycle ON nju_market.data_statistics USING btree (cycle);


--
-- Name: idx_21794_idx_date_key; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21794_idx_date_key ON nju_market.data_statistics USING btree (date_key);


--
-- Name: idx_21794_idx_dimension; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21794_idx_dimension ON nju_market.data_statistics USING btree (dimension);


--
-- Name: idx_21801_uk_ie6gy369soc8701jlxwe85tms; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE UNIQUE INDEX idx_21801_uk_ie6gy369soc8701jlxwe85tms ON nju_market.image_references USING btree (image_path);


--
-- Name: idx_21815_idx_conversation_deleted_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21815_idx_conversation_deleted_time ON nju_market.messages USING btree (conversation_id, deleted_by_sender, deleted_by_receiver, created_at);


--
-- Name: idx_21815_idx_conversation_id; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21815_idx_conversation_id ON nju_market.messages USING btree (conversation_id);


--
-- Name: idx_21815_idx_conversation_receiver_read_deleted; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21815_idx_conversation_receiver_read_deleted ON nju_market.messages USING btree (conversation_id, receiver_id, is_read, deleted_by_sender, deleted_by_receiver);


--
-- Name: idx_21815_idx_conversation_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21815_idx_conversation_time ON nju_market.messages USING btree (conversation_id, created_at);


--
-- Name: idx_21815_idx_created_at; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21815_idx_created_at ON nju_market.messages USING btree (created_at);


--
-- Name: idx_21815_idx_is_read; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21815_idx_is_read ON nju_market.messages USING btree (is_read);


--
-- Name: idx_21815_idx_messages_commodity_id; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21815_idx_messages_commodity_id ON nju_market.messages USING btree (commodity_id);


--
-- Name: idx_21815_idx_messages_order_id; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21815_idx_messages_order_id ON nju_market.messages USING btree (order_id);


--
-- Name: idx_21815_idx_receiver_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21815_idx_receiver_time ON nju_market.messages USING btree (receiver_id, created_at);


--
-- Name: idx_21815_idx_sender_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21815_idx_sender_time ON nju_market.messages USING btree (sender_id, created_at);


--
-- Name: idx_21828_idx_create_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21828_idx_create_time ON nju_market.order_status_logs USING btree (create_time);


--
-- Name: idx_21828_idx_operator_id; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21828_idx_operator_id ON nju_market.order_status_logs USING btree (operator_id);


--
-- Name: idx_21828_idx_order_id; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21828_idx_order_id ON nju_market.order_status_logs USING btree (order_id);


--
-- Name: idx_21833_idx_buyer_id; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21833_idx_buyer_id ON nju_market.orders USING btree (buyer_id);


--
-- Name: idx_21833_idx_buyer_status_visibility_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21833_idx_buyer_status_visibility_time ON nju_market.orders USING btree (buyer_id, order_status, buyer_visibility, create_time);


--
-- Name: idx_21833_idx_buyer_visibility; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21833_idx_buyer_visibility ON nju_market.orders USING btree (buyer_visibility);


--
-- Name: idx_21833_idx_buyer_visibility_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21833_idx_buyer_visibility_time ON nju_market.orders USING btree (buyer_id, buyer_visibility, create_time);


--
-- Name: idx_21833_idx_commodity_id; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21833_idx_commodity_id ON nju_market.orders USING btree (commodity_id);


--
-- Name: idx_21833_idx_commodity_snapshot_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21833_idx_commodity_snapshot_time ON nju_market.orders USING btree (commodity_snapshot_time);


--
-- Name: idx_21833_idx_create_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21833_idx_create_time ON nju_market.orders USING btree (create_time);


--
-- Name: idx_21833_idx_delivery_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21833_idx_delivery_time ON nju_market.orders USING btree (delivery_time);


--
-- Name: idx_21833_idx_order_status; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21833_idx_order_status ON nju_market.orders USING btree (order_status);


--
-- Name: idx_21833_idx_pay_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21833_idx_pay_time ON nju_market.orders USING btree (pay_time);


--
-- Name: idx_21833_idx_return_status; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21833_idx_return_status ON nju_market.orders USING btree (order_status);


--
-- Name: idx_21833_idx_seller_id; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21833_idx_seller_id ON nju_market.orders USING btree (seller_id);


--
-- Name: idx_21833_idx_seller_status_visibility_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21833_idx_seller_status_visibility_time ON nju_market.orders USING btree (seller_id, order_status, seller_visibility, create_time);


--
-- Name: idx_21833_idx_seller_visibility; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21833_idx_seller_visibility ON nju_market.orders USING btree (seller_visibility);


--
-- Name: idx_21833_idx_seller_visibility_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21833_idx_seller_visibility_time ON nju_market.orders USING btree (seller_id, seller_visibility, create_time);


--
-- Name: idx_21833_idx_shipping_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21833_idx_shipping_time ON nju_market.orders USING btree (shipping_time);


--
-- Name: idx_21842_idx_create_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21842_idx_create_time ON nju_market.promotions USING btree (create_time);


--
-- Name: idx_21842_idx_end_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21842_idx_end_time ON nju_market.promotions USING btree (end_time);


--
-- Name: idx_21842_idx_start_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21842_idx_start_time ON nju_market.promotions USING btree (start_time);


--
-- Name: idx_21842_idx_status; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21842_idx_status ON nju_market.promotions USING btree (status);


--
-- Name: idx_21842_idx_type; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21842_idx_type ON nju_market.promotions USING btree (type);


--
-- Name: idx_21842_idx_user_id; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21842_idx_user_id ON nju_market.promotions USING btree (user_id);


--
-- Name: idx_21855_idx_order_id; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21855_idx_order_id ON nju_market.return_records USING btree (order_id);


--
-- Name: idx_21855_idx_return_status; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21855_idx_return_status ON nju_market.return_records USING btree (return_status);


--
-- Name: idx_21860_idx_activity_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21860_idx_activity_time ON nju_market.user_activity_records USING btree (activity_time);


--
-- Name: idx_21860_idx_activity_type; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21860_idx_activity_type ON nju_market.user_activity_records USING btree (activity_type);


--
-- Name: idx_21860_idx_ip_address; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21860_idx_ip_address ON nju_market.user_activity_records USING btree (ip_address);


--
-- Name: idx_21860_idx_user_id; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21860_idx_user_id ON nju_market.user_activity_records USING btree (user_id);


--
-- Name: idx_21865_idx_credit_score; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21865_idx_credit_score ON nju_market.user_profiles USING btree (credit_score);


--
-- Name: idx_21865_idx_user_profile_nickname; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21865_idx_user_profile_nickname ON nju_market.user_profiles USING btree (nickname);


--
-- Name: idx_21865_idx_user_profile_nickname_avatar; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21865_idx_user_profile_nickname_avatar ON nju_market.user_profiles USING btree (user_id, nickname, avatar);


--
-- Name: idx_21865_idx_vip_level; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21865_idx_vip_level ON nju_market.user_profiles USING btree (vip_level);


--
-- Name: idx_21865_uk_user_id; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE UNIQUE INDEX idx_21865_uk_user_id ON nju_market.user_profiles USING btree (user_id);


--
-- Name: idx_21874_idx_account_status; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21874_idx_account_status ON nju_market.users USING btree (account_status);


--
-- Name: idx_21874_idx_register_time; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_21874_idx_register_time ON nju_market.users USING btree (register_time);


--
-- Name: idx_21874_uk_primary_phone; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE UNIQUE INDEX idx_21874_uk_primary_phone ON nju_market.users USING btree (primary_phone);


--
-- Name: idx_21874_uk_username; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE UNIQUE INDEX idx_21874_uk_username ON nju_market.users USING btree (username);


--
-- Name: idx_commodities_address_id; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_commodities_address_id ON nju_market.commodities USING btree (address_id);


--
-- Name: idx_commodities_city_district; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_commodities_city_district ON nju_market.commodities USING btree (address_snapshot_city, address_snapshot_district);


--
-- Name: idx_commodities_location_geography; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_commodities_location_geography ON nju_market.commodities USING gist (location_geography);


--
-- Name: idx_orders_shipping_address_id; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_orders_shipping_address_id ON nju_market.orders USING btree (shipping_address_id);


--
-- Name: idx_user_addresses_location; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_user_addresses_location ON nju_market.user_addresses USING gist (location);


--
-- Name: idx_user_addresses_user_active; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_user_addresses_user_active ON nju_market.user_addresses USING btree (user_id, is_active) WHERE (is_active = true);


--
-- Name: idx_user_addresses_user_default; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_user_addresses_user_default ON nju_market.user_addresses USING btree (user_id, is_default) WHERE (is_default = true);


--
-- Name: idx_user_addresses_user_id; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX idx_user_addresses_user_id ON nju_market.user_addresses USING btree (user_id);


--
-- Name: user_profile_vectors_embedding_idx; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX user_profile_vectors_embedding_idx ON nju_market.user_profile_vectors USING hnsw (embedding public.vector_cosine_ops) WITH (m='16', ef_construction='64');


--
-- Name: user_profile_vectors_user_id_idx; Type: INDEX; Schema: nju_market; Owner: postgres
--

CREATE INDEX user_profile_vectors_user_id_idx ON nju_market.user_profile_vectors USING btree (user_id);


--
-- Name: idx_21276_idx_admin_id; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21276_idx_admin_id ON nju_market_backup.admin_operation_logs USING btree (admin_id);


--
-- Name: idx_21276_idx_create_time; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21276_idx_create_time ON nju_market_backup.admin_operation_logs USING btree (create_time);


--
-- Name: idx_21276_idx_operation_type; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21276_idx_operation_type ON nju_market_backup.admin_operation_logs USING btree (operation_type);


--
-- Name: idx_21281_idx_admin_id; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21281_idx_admin_id ON nju_market_backup.admin_sessions USING btree (admin_id);


--
-- Name: idx_21281_idx_expire_time; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21281_idx_expire_time ON nju_market_backup.admin_sessions USING btree (expire_time);


--
-- Name: idx_21281_idx_is_active; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21281_idx_is_active ON nju_market_backup.admin_sessions USING btree (is_active);


--
-- Name: idx_21281_idx_token; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21281_idx_token ON nju_market_backup.admin_sessions USING btree (token);


--
-- Name: idx_21287_idx_account_status; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21287_idx_account_status ON nju_market_backup.admins USING btree (account_status);


--
-- Name: idx_21287_idx_admin_level; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21287_idx_admin_level ON nju_market_backup.admins USING btree (admin_level);


--
-- Name: idx_21287_idx_create_time; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21287_idx_create_time ON nju_market_backup.admins USING btree (create_time);


--
-- Name: idx_21287_idx_department; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21287_idx_department ON nju_market_backup.admins USING btree (department);


--
-- Name: idx_21287_idx_last_login_time; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21287_idx_last_login_time ON nju_market_backup.admins USING btree (last_login_time);


--
-- Name: idx_21287_idx_username; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21287_idx_username ON nju_market_backup.admins USING btree (username);


--
-- Name: idx_21287_username; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE UNIQUE INDEX idx_21287_username ON nju_market_backup.admins USING btree (username);


--
-- Name: idx_21295_idx_audit_time; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21295_idx_audit_time ON nju_market_backup.audit_records USING btree (audit_time);


--
-- Name: idx_21295_idx_audit_type; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21295_idx_audit_type ON nju_market_backup.audit_records USING btree (audit_type);


--
-- Name: idx_21295_idx_commodity_id; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21295_idx_commodity_id ON nju_market_backup.audit_records USING btree (commodity_id);


--
-- Name: idx_21295_idx_decision; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21295_idx_decision ON nju_market_backup.audit_records USING btree (decision);


--
-- Name: idx_21295_idx_reviewer_id; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21295_idx_reviewer_id ON nju_market_backup.audit_records USING btree (reviewer_id);


--
-- Name: idx_21302_idx_ban_type; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21302_idx_ban_type ON nju_market_backup.ban_records USING btree (ban_type);


--
-- Name: idx_21302_idx_device_id; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21302_idx_device_id ON nju_market_backup.ban_records USING btree (device_id);


--
-- Name: idx_21302_idx_end_at; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21302_idx_end_at ON nju_market_backup.ban_records USING btree (end_at);


--
-- Name: idx_21302_idx_is_active; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21302_idx_is_active ON nju_market_backup.ban_records USING btree (is_active);


--
-- Name: idx_21302_idx_phone; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21302_idx_phone ON nju_market_backup.ban_records USING btree (phone);


--
-- Name: idx_21302_idx_start_at; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21302_idx_start_at ON nju_market_backup.ban_records USING btree (start_at);


--
-- Name: idx_21302_idx_user_id; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21302_idx_user_id ON nju_market_backup.ban_records USING btree (user_id);


--
-- Name: idx_21308_idx_category; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21308_idx_category ON nju_market_backup.commodities USING btree (category);


--
-- Name: idx_21308_idx_category_status_visibility; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21308_idx_category_status_visibility ON nju_market_backup.commodities USING btree (category, commodity_status, seller_visibility, buyer_visibility);


--
-- Name: idx_21308_idx_click_count; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21308_idx_click_count ON nju_market_backup.commodities USING btree (click_count);


--
-- Name: idx_21308_idx_condition_level; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21308_idx_condition_level ON nju_market_backup.commodities USING btree (condition_level);


--
-- Name: idx_21308_idx_price; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21308_idx_price ON nju_market_backup.commodities USING btree (price);


--
-- Name: idx_21308_idx_publish_time; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21308_idx_publish_time ON nju_market_backup.commodities USING btree (publish_time);


--
-- Name: idx_21308_idx_seller_id; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21308_idx_seller_id ON nju_market_backup.commodities USING btree (seller_id);


--
-- Name: idx_21308_idx_seller_publish_time; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21308_idx_seller_publish_time ON nju_market_backup.commodities USING btree (seller_id, publish_time);


--
-- Name: idx_21308_idx_seller_status_publish_time; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21308_idx_seller_status_publish_time ON nju_market_backup.commodities USING btree (seller_id, commodity_status, publish_time);


--
-- Name: idx_21308_idx_status_click_count; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21308_idx_status_click_count ON nju_market_backup.commodities USING btree (commodity_status, click_count);


--
-- Name: idx_21308_idx_status_price; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21308_idx_status_price ON nju_market_backup.commodities USING btree (commodity_status, price);


--
-- Name: idx_21308_idx_status_publish_time; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21308_idx_status_publish_time ON nju_market_backup.commodities USING btree (commodity_status, publish_time);


--
-- Name: idx_21308_idx_status_seller_buyer_visibility; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21308_idx_status_seller_buyer_visibility ON nju_market_backup.commodities USING btree (commodity_status, seller_visibility, buyer_visibility);


--
-- Name: idx_21308_idx_stock; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21308_idx_stock ON nju_market_backup.commodities USING btree (stock);


--
-- Name: idx_21319_idx_is_active; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21319_idx_is_active ON nju_market_backup.commodity_categories USING btree (is_active);


--
-- Name: idx_21319_idx_parent_id; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21319_idx_parent_id ON nju_market_backup.commodity_categories USING btree (parent_id);


--
-- Name: idx_21319_idx_sort_order; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21319_idx_sort_order ON nju_market_backup.commodity_categories USING btree (sort_order);


--
-- Name: idx_21329_idx_complainant_id; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21329_idx_complainant_id ON nju_market_backup.complaints USING btree (complainant_id);


--
-- Name: idx_21329_idx_defendant_id; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21329_idx_defendant_id ON nju_market_backup.complaints USING btree (defendant_id);


--
-- Name: idx_21329_idx_related_order_id; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21329_idx_related_order_id ON nju_market_backup.complaints USING btree (related_order_id);


--
-- Name: idx_21329_idx_status; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21329_idx_status ON nju_market_backup.complaints USING btree (status);


--
-- Name: idx_21329_idx_submit_time; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21329_idx_submit_time ON nju_market_backup.complaints USING btree (submit_time);


--
-- Name: idx_21336_idx_blocked_user_id; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21336_idx_blocked_user_id ON nju_market_backup.contact_blacklist USING btree (blocked_user_id);


--
-- Name: idx_21336_idx_user_id; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21336_idx_user_id ON nju_market_backup.contact_blacklist USING btree (user_id);


--
-- Name: idx_21336_uk_user_blocked; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE UNIQUE INDEX idx_21336_uk_user_blocked ON nju_market_backup.contact_blacklist USING btree (user_id, blocked_user_id);


--
-- Name: idx_21341_idx_owner_id; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21341_idx_owner_id ON nju_market_backup.contact_info USING btree (owner_id);


--
-- Name: idx_21341_idx_type; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21341_idx_type ON nju_market_backup.contact_info USING btree (type);


--
-- Name: idx_21346_idx_last_message_time; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21346_idx_last_message_time ON nju_market_backup.conversations USING btree (last_message_time);


--
-- Name: idx_21346_idx_user1_status_visibility_time; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21346_idx_user1_status_visibility_time ON nju_market_backup.conversations USING btree (user_id_1, status, user_1_visibility, last_message_time);


--
-- Name: idx_21346_idx_user2_status_visibility_time; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21346_idx_user2_status_visibility_time ON nju_market_backup.conversations USING btree (user_id_2, status, user_2_visibility, last_message_time);


--
-- Name: idx_21346_uk_user_pair_active; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE UNIQUE INDEX idx_21346_uk_user_pair_active ON nju_market_backup.conversations USING btree (user_id_1, user_id_2, status);


--
-- Name: idx_21357_idx_category; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21357_idx_category ON nju_market_backup.data_statistics USING btree (category);


--
-- Name: idx_21357_idx_create_time; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21357_idx_create_time ON nju_market_backup.data_statistics USING btree (create_time);


--
-- Name: idx_21357_idx_cycle; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21357_idx_cycle ON nju_market_backup.data_statistics USING btree (cycle);


--
-- Name: idx_21357_idx_date_key; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21357_idx_date_key ON nju_market_backup.data_statistics USING btree (date_key);


--
-- Name: idx_21357_idx_dimension; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21357_idx_dimension ON nju_market_backup.data_statistics USING btree (dimension);


--
-- Name: idx_21364_uk_ie6gy369soc8701jlxwe85tms; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE UNIQUE INDEX idx_21364_uk_ie6gy369soc8701jlxwe85tms ON nju_market_backup.image_references USING btree (image_path);


--
-- Name: idx_21378_idx_conversation_deleted_time; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21378_idx_conversation_deleted_time ON nju_market_backup.messages USING btree (conversation_id, deleted_by_sender, deleted_by_receiver, created_at);


--
-- Name: idx_21378_idx_conversation_id; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21378_idx_conversation_id ON nju_market_backup.messages USING btree (conversation_id);


--
-- Name: idx_21378_idx_conversation_receiver_read_deleted; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21378_idx_conversation_receiver_read_deleted ON nju_market_backup.messages USING btree (conversation_id, receiver_id, is_read, deleted_by_sender, deleted_by_receiver);


--
-- Name: idx_21378_idx_conversation_time; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21378_idx_conversation_time ON nju_market_backup.messages USING btree (conversation_id, created_at);


--
-- Name: idx_21378_idx_created_at; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21378_idx_created_at ON nju_market_backup.messages USING btree (created_at);


--
-- Name: idx_21378_idx_is_read; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21378_idx_is_read ON nju_market_backup.messages USING btree (is_read);


--
-- Name: idx_21378_idx_messages_commodity_id; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21378_idx_messages_commodity_id ON nju_market_backup.messages USING btree (commodity_id);


--
-- Name: idx_21378_idx_messages_order_id; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21378_idx_messages_order_id ON nju_market_backup.messages USING btree (order_id);


--
-- Name: idx_21378_idx_receiver_time; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21378_idx_receiver_time ON nju_market_backup.messages USING btree (receiver_id, created_at);


--
-- Name: idx_21378_idx_sender_time; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21378_idx_sender_time ON nju_market_backup.messages USING btree (sender_id, created_at);


--
-- Name: idx_21391_idx_create_time; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21391_idx_create_time ON nju_market_backup.order_status_logs USING btree (create_time);


--
-- Name: idx_21391_idx_operator_id; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21391_idx_operator_id ON nju_market_backup.order_status_logs USING btree (operator_id);


--
-- Name: idx_21391_idx_order_id; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21391_idx_order_id ON nju_market_backup.order_status_logs USING btree (order_id);


--
-- Name: idx_21396_idx_buyer_id; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21396_idx_buyer_id ON nju_market_backup.orders USING btree (buyer_id);


--
-- Name: idx_21396_idx_buyer_status_visibility_time; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21396_idx_buyer_status_visibility_time ON nju_market_backup.orders USING btree (buyer_id, order_status, buyer_visibility, create_time);


--
-- Name: idx_21396_idx_buyer_visibility; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21396_idx_buyer_visibility ON nju_market_backup.orders USING btree (buyer_visibility);


--
-- Name: idx_21396_idx_buyer_visibility_time; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21396_idx_buyer_visibility_time ON nju_market_backup.orders USING btree (buyer_id, buyer_visibility, create_time);


--
-- Name: idx_21396_idx_commodity_id; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21396_idx_commodity_id ON nju_market_backup.orders USING btree (commodity_id);


--
-- Name: idx_21396_idx_commodity_snapshot_time; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21396_idx_commodity_snapshot_time ON nju_market_backup.orders USING btree (commodity_snapshot_time);


--
-- Name: idx_21396_idx_create_time; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21396_idx_create_time ON nju_market_backup.orders USING btree (create_time);


--
-- Name: idx_21396_idx_delivery_time; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21396_idx_delivery_time ON nju_market_backup.orders USING btree (delivery_time);


--
-- Name: idx_21396_idx_order_status; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21396_idx_order_status ON nju_market_backup.orders USING btree (order_status);


--
-- Name: idx_21396_idx_pay_time; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21396_idx_pay_time ON nju_market_backup.orders USING btree (pay_time);


--
-- Name: idx_21396_idx_return_status; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21396_idx_return_status ON nju_market_backup.orders USING btree (order_status);


--
-- Name: idx_21396_idx_seller_id; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21396_idx_seller_id ON nju_market_backup.orders USING btree (seller_id);


--
-- Name: idx_21396_idx_seller_status_visibility_time; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21396_idx_seller_status_visibility_time ON nju_market_backup.orders USING btree (seller_id, order_status, seller_visibility, create_time);


--
-- Name: idx_21396_idx_seller_visibility; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21396_idx_seller_visibility ON nju_market_backup.orders USING btree (seller_visibility);


--
-- Name: idx_21396_idx_seller_visibility_time; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21396_idx_seller_visibility_time ON nju_market_backup.orders USING btree (seller_id, seller_visibility, create_time);


--
-- Name: idx_21396_idx_shipping_time; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21396_idx_shipping_time ON nju_market_backup.orders USING btree (shipping_time);


--
-- Name: idx_21405_idx_create_time; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21405_idx_create_time ON nju_market_backup.promotions USING btree (create_time);


--
-- Name: idx_21405_idx_end_time; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21405_idx_end_time ON nju_market_backup.promotions USING btree (end_time);


--
-- Name: idx_21405_idx_start_time; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21405_idx_start_time ON nju_market_backup.promotions USING btree (start_time);


--
-- Name: idx_21405_idx_status; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21405_idx_status ON nju_market_backup.promotions USING btree (status);


--
-- Name: idx_21405_idx_type; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21405_idx_type ON nju_market_backup.promotions USING btree (type);


--
-- Name: idx_21405_idx_user_id; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21405_idx_user_id ON nju_market_backup.promotions USING btree (user_id);


--
-- Name: idx_21418_idx_order_id; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21418_idx_order_id ON nju_market_backup.return_records USING btree (order_id);


--
-- Name: idx_21418_idx_return_status; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21418_idx_return_status ON nju_market_backup.return_records USING btree (return_status);


--
-- Name: idx_21423_idx_activity_time; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21423_idx_activity_time ON nju_market_backup.user_activity_records USING btree (activity_time);


--
-- Name: idx_21423_idx_activity_type; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21423_idx_activity_type ON nju_market_backup.user_activity_records USING btree (activity_type);


--
-- Name: idx_21423_idx_ip_address; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21423_idx_ip_address ON nju_market_backup.user_activity_records USING btree (ip_address);


--
-- Name: idx_21423_idx_user_id; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21423_idx_user_id ON nju_market_backup.user_activity_records USING btree (user_id);


--
-- Name: idx_21428_idx_credit_score; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21428_idx_credit_score ON nju_market_backup.user_profiles USING btree (credit_score);


--
-- Name: idx_21428_idx_user_profile_nickname; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21428_idx_user_profile_nickname ON nju_market_backup.user_profiles USING btree (nickname);


--
-- Name: idx_21428_idx_user_profile_nickname_avatar; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21428_idx_user_profile_nickname_avatar ON nju_market_backup.user_profiles USING btree (user_id, nickname, avatar);


--
-- Name: idx_21428_idx_vip_level; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21428_idx_vip_level ON nju_market_backup.user_profiles USING btree (vip_level);


--
-- Name: idx_21428_uk_user_id; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE UNIQUE INDEX idx_21428_uk_user_id ON nju_market_backup.user_profiles USING btree (user_id);


--
-- Name: idx_21437_idx_account_status; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21437_idx_account_status ON nju_market_backup.users USING btree (account_status);


--
-- Name: idx_21437_idx_register_time; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE INDEX idx_21437_idx_register_time ON nju_market_backup.users USING btree (register_time);


--
-- Name: idx_21437_uk_primary_phone; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE UNIQUE INDEX idx_21437_uk_primary_phone ON nju_market_backup.users USING btree (primary_phone);


--
-- Name: idx_21437_uk_username; Type: INDEX; Schema: nju_market_backup; Owner: postgres
--

CREATE UNIQUE INDEX idx_21437_uk_username ON nju_market_backup.users USING btree (username);


--
-- Name: ai_conversations ai_conversations_updated_at_trigger; Type: TRIGGER; Schema: nju_market; Owner: postgres
--

CREATE TRIGGER ai_conversations_updated_at_trigger BEFORE UPDATE ON nju_market.ai_conversations FOR EACH ROW EXECUTE FUNCTION nju_market.update_ai_conversation_updated_at();


--
-- Name: commodity_vectors commodity_vectors_updated_at_trigger; Type: TRIGGER; Schema: nju_market; Owner: postgres
--

CREATE TRIGGER commodity_vectors_updated_at_trigger BEFORE UPDATE ON nju_market.commodity_vectors FOR EACH ROW EXECUTE FUNCTION nju_market.update_commodity_vector_updated_at();


--
-- Name: user_addresses trigger_user_addresses_update_time; Type: TRIGGER; Schema: nju_market; Owner: postgres
--

CREATE TRIGGER trigger_user_addresses_update_time BEFORE UPDATE ON nju_market.user_addresses FOR EACH ROW EXECUTE FUNCTION nju_market.update_user_addresses_updated_time();


--
-- Name: user_profile_vectors user_profile_vectors_updated_at_trigger; Type: TRIGGER; Schema: nju_market; Owner: postgres
--

CREATE TRIGGER user_profile_vectors_updated_at_trigger BEFORE UPDATE ON nju_market.user_profile_vectors FOR EACH ROW EXECUTE FUNCTION nju_market.update_user_profile_vector_updated_at();


--
-- Name: admin_operation_logs admin_operation_logs_ibfk_1; Type: FK CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.admin_operation_logs
    ADD CONSTRAINT admin_operation_logs_ibfk_1 FOREIGN KEY (admin_id) REFERENCES nju_market.admins(admin_id) ON DELETE CASCADE;


--
-- Name: admin_sessions admin_sessions_ibfk_1; Type: FK CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.admin_sessions
    ADD CONSTRAINT admin_sessions_ibfk_1 FOREIGN KEY (admin_id) REFERENCES nju_market.admins(admin_id) ON DELETE CASCADE;


--
-- Name: ai_conversations fk_ai_conversation_user; Type: FK CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.ai_conversations
    ADD CONSTRAINT fk_ai_conversation_user FOREIGN KEY (user_id) REFERENCES nju_market.users(user_id) ON DELETE CASCADE;


--
-- Name: audit_records fk_audit_records_commodity_id; Type: FK CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.audit_records
    ADD CONSTRAINT fk_audit_records_commodity_id FOREIGN KEY (commodity_id) REFERENCES nju_market.commodities(commodity_id) ON DELETE CASCADE;


--
-- Name: ban_records fk_ban_records_user_id; Type: FK CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.ban_records
    ADD CONSTRAINT fk_ban_records_user_id FOREIGN KEY (user_id) REFERENCES nju_market.users(user_id) ON DELETE CASCADE;


--
-- Name: commodities fk_commodities_address_id; Type: FK CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.commodities
    ADD CONSTRAINT fk_commodities_address_id FOREIGN KEY (address_id) REFERENCES nju_market.user_addresses(address_id) ON DELETE SET NULL;


--
-- Name: commodities fk_commodities_seller_id; Type: FK CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.commodities
    ADD CONSTRAINT fk_commodities_seller_id FOREIGN KEY (seller_id) REFERENCES nju_market.users(user_id) ON DELETE CASCADE;


--
-- Name: commodity_vectors fk_commodity_vector; Type: FK CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.commodity_vectors
    ADD CONSTRAINT fk_commodity_vector FOREIGN KEY (commodity_id) REFERENCES nju_market.commodities(commodity_id) ON DELETE CASCADE;


--
-- Name: complaints fk_complaints_complainant_id; Type: FK CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.complaints
    ADD CONSTRAINT fk_complaints_complainant_id FOREIGN KEY (complainant_id) REFERENCES nju_market.users(user_id) ON DELETE CASCADE;


--
-- Name: complaints fk_complaints_defendant_id; Type: FK CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.complaints
    ADD CONSTRAINT fk_complaints_defendant_id FOREIGN KEY (defendant_id) REFERENCES nju_market.users(user_id) ON DELETE CASCADE;


--
-- Name: complaints fk_complaints_related_order_id; Type: FK CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.complaints
    ADD CONSTRAINT fk_complaints_related_order_id FOREIGN KEY (related_order_id) REFERENCES nju_market.orders(order_id) ON DELETE SET NULL;


--
-- Name: contact_info fk_contact_info_owner_id; Type: FK CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.contact_info
    ADD CONSTRAINT fk_contact_info_owner_id FOREIGN KEY (owner_id) REFERENCES nju_market.users(user_id) ON DELETE CASCADE;


--
-- Name: conversation_vectors fk_conversation_vector_conversation; Type: FK CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.conversation_vectors
    ADD CONSTRAINT fk_conversation_vector_conversation FOREIGN KEY (conversation_id) REFERENCES nju_market.ai_conversations(conversation_id) ON DELETE CASCADE;


--
-- Name: conversation_vectors fk_conversation_vector_user; Type: FK CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.conversation_vectors
    ADD CONSTRAINT fk_conversation_vector_user FOREIGN KEY (user_id) REFERENCES nju_market.users(user_id) ON DELETE CASCADE;


--
-- Name: order_status_logs fk_order_status_logs_order_id; Type: FK CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.order_status_logs
    ADD CONSTRAINT fk_order_status_logs_order_id FOREIGN KEY (order_id) REFERENCES nju_market.orders(order_id) ON DELETE CASCADE;


--
-- Name: orders fk_orders_buyer_id; Type: FK CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.orders
    ADD CONSTRAINT fk_orders_buyer_id FOREIGN KEY (buyer_id) REFERENCES nju_market.users(user_id) ON DELETE CASCADE;


--
-- Name: orders fk_orders_commodity_id; Type: FK CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.orders
    ADD CONSTRAINT fk_orders_commodity_id FOREIGN KEY (commodity_id) REFERENCES nju_market.commodities(commodity_id) ON DELETE CASCADE;


--
-- Name: orders fk_orders_seller_id; Type: FK CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.orders
    ADD CONSTRAINT fk_orders_seller_id FOREIGN KEY (seller_id) REFERENCES nju_market.users(user_id) ON DELETE CASCADE;


--
-- Name: orders fk_orders_shipping_address_id; Type: FK CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.orders
    ADD CONSTRAINT fk_orders_shipping_address_id FOREIGN KEY (shipping_address_id) REFERENCES nju_market.user_addresses(address_id) ON DELETE SET NULL;


--
-- Name: promotions fk_promotions_user_id; Type: FK CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.promotions
    ADD CONSTRAINT fk_promotions_user_id FOREIGN KEY (user_id) REFERENCES nju_market.users(user_id) ON DELETE CASCADE;


--
-- Name: return_records fk_return_order; Type: FK CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.return_records
    ADD CONSTRAINT fk_return_order FOREIGN KEY (order_id) REFERENCES nju_market.orders(order_id) ON DELETE CASCADE;


--
-- Name: user_activity_records fk_user_activity_records_user_id; Type: FK CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.user_activity_records
    ADD CONSTRAINT fk_user_activity_records_user_id FOREIGN KEY (user_id) REFERENCES nju_market.users(user_id) ON DELETE CASCADE;


--
-- Name: user_addresses fk_user_addresses_user_id; Type: FK CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.user_addresses
    ADD CONSTRAINT fk_user_addresses_user_id FOREIGN KEY (user_id) REFERENCES nju_market.users(user_id) ON DELETE CASCADE;


--
-- Name: user_profile_vectors fk_user_profile_vector; Type: FK CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.user_profile_vectors
    ADD CONSTRAINT fk_user_profile_vector FOREIGN KEY (user_id) REFERENCES nju_market.users(user_id) ON DELETE CASCADE;


--
-- Name: user_profiles fk_user_profiles_user_id; Type: FK CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.user_profiles
    ADD CONSTRAINT fk_user_profiles_user_id FOREIGN KEY (user_id) REFERENCES nju_market.users(user_id) ON DELETE CASCADE;


--
-- Name: messages messages_ibfk_1; Type: FK CONSTRAINT; Schema: nju_market; Owner: postgres
--

ALTER TABLE ONLY nju_market.messages
    ADD CONSTRAINT messages_ibfk_1 FOREIGN KEY (conversation_id) REFERENCES nju_market.conversations(conversation_id) ON DELETE CASCADE;


--
-- Name: admin_operation_logs admin_operation_logs_ibfk_1; Type: FK CONSTRAINT; Schema: nju_market_backup; Owner: postgres
--

ALTER TABLE ONLY nju_market_backup.admin_operation_logs
    ADD CONSTRAINT admin_operation_logs_ibfk_1 FOREIGN KEY (admin_id) REFERENCES nju_market_backup.admins(admin_id) ON DELETE CASCADE;


--
-- Name: admin_sessions admin_sessions_ibfk_1; Type: FK CONSTRAINT; Schema: nju_market_backup; Owner: postgres
--

ALTER TABLE ONLY nju_market_backup.admin_sessions
    ADD CONSTRAINT admin_sessions_ibfk_1 FOREIGN KEY (admin_id) REFERENCES nju_market_backup.admins(admin_id) ON DELETE CASCADE;


--
-- Name: audit_records fk_audit_records_commodity_id; Type: FK CONSTRAINT; Schema: nju_market_backup; Owner: postgres
--

ALTER TABLE ONLY nju_market_backup.audit_records
    ADD CONSTRAINT fk_audit_records_commodity_id FOREIGN KEY (commodity_id) REFERENCES nju_market_backup.commodities(commodity_id) ON DELETE CASCADE;


--
-- Name: ban_records fk_ban_records_user_id; Type: FK CONSTRAINT; Schema: nju_market_backup; Owner: postgres
--

ALTER TABLE ONLY nju_market_backup.ban_records
    ADD CONSTRAINT fk_ban_records_user_id FOREIGN KEY (user_id) REFERENCES nju_market_backup.users(user_id) ON DELETE CASCADE;


--
-- Name: commodities fk_commodities_seller_id; Type: FK CONSTRAINT; Schema: nju_market_backup; Owner: postgres
--

ALTER TABLE ONLY nju_market_backup.commodities
    ADD CONSTRAINT fk_commodities_seller_id FOREIGN KEY (seller_id) REFERENCES nju_market_backup.users(user_id) ON DELETE CASCADE;


--
-- Name: complaints fk_complaints_complainant_id; Type: FK CONSTRAINT; Schema: nju_market_backup; Owner: postgres
--

ALTER TABLE ONLY nju_market_backup.complaints
    ADD CONSTRAINT fk_complaints_complainant_id FOREIGN KEY (complainant_id) REFERENCES nju_market_backup.users(user_id) ON DELETE CASCADE;


--
-- Name: complaints fk_complaints_defendant_id; Type: FK CONSTRAINT; Schema: nju_market_backup; Owner: postgres
--

ALTER TABLE ONLY nju_market_backup.complaints
    ADD CONSTRAINT fk_complaints_defendant_id FOREIGN KEY (defendant_id) REFERENCES nju_market_backup.users(user_id) ON DELETE CASCADE;


--
-- Name: complaints fk_complaints_related_order_id; Type: FK CONSTRAINT; Schema: nju_market_backup; Owner: postgres
--

ALTER TABLE ONLY nju_market_backup.complaints
    ADD CONSTRAINT fk_complaints_related_order_id FOREIGN KEY (related_order_id) REFERENCES nju_market_backup.orders(order_id) ON DELETE SET NULL;


--
-- Name: contact_info fk_contact_info_owner_id; Type: FK CONSTRAINT; Schema: nju_market_backup; Owner: postgres
--

ALTER TABLE ONLY nju_market_backup.contact_info
    ADD CONSTRAINT fk_contact_info_owner_id FOREIGN KEY (owner_id) REFERENCES nju_market_backup.users(user_id) ON DELETE CASCADE;


--
-- Name: order_status_logs fk_order_status_logs_order_id; Type: FK CONSTRAINT; Schema: nju_market_backup; Owner: postgres
--

ALTER TABLE ONLY nju_market_backup.order_status_logs
    ADD CONSTRAINT fk_order_status_logs_order_id FOREIGN KEY (order_id) REFERENCES nju_market_backup.orders(order_id) ON DELETE CASCADE;


--
-- Name: orders fk_orders_buyer_id; Type: FK CONSTRAINT; Schema: nju_market_backup; Owner: postgres
--

ALTER TABLE ONLY nju_market_backup.orders
    ADD CONSTRAINT fk_orders_buyer_id FOREIGN KEY (buyer_id) REFERENCES nju_market_backup.users(user_id) ON DELETE CASCADE;


--
-- Name: orders fk_orders_commodity_id; Type: FK CONSTRAINT; Schema: nju_market_backup; Owner: postgres
--

ALTER TABLE ONLY nju_market_backup.orders
    ADD CONSTRAINT fk_orders_commodity_id FOREIGN KEY (commodity_id) REFERENCES nju_market_backup.commodities(commodity_id) ON DELETE CASCADE;


--
-- Name: orders fk_orders_seller_id; Type: FK CONSTRAINT; Schema: nju_market_backup; Owner: postgres
--

ALTER TABLE ONLY nju_market_backup.orders
    ADD CONSTRAINT fk_orders_seller_id FOREIGN KEY (seller_id) REFERENCES nju_market_backup.users(user_id) ON DELETE CASCADE;


--
-- Name: promotions fk_promotions_user_id; Type: FK CONSTRAINT; Schema: nju_market_backup; Owner: postgres
--

ALTER TABLE ONLY nju_market_backup.promotions
    ADD CONSTRAINT fk_promotions_user_id FOREIGN KEY (user_id) REFERENCES nju_market_backup.users(user_id) ON DELETE CASCADE;


--
-- Name: return_records fk_return_order; Type: FK CONSTRAINT; Schema: nju_market_backup; Owner: postgres
--

ALTER TABLE ONLY nju_market_backup.return_records
    ADD CONSTRAINT fk_return_order FOREIGN KEY (order_id) REFERENCES nju_market_backup.orders(order_id) ON DELETE CASCADE;


--
-- Name: user_activity_records fk_user_activity_records_user_id; Type: FK CONSTRAINT; Schema: nju_market_backup; Owner: postgres
--

ALTER TABLE ONLY nju_market_backup.user_activity_records
    ADD CONSTRAINT fk_user_activity_records_user_id FOREIGN KEY (user_id) REFERENCES nju_market_backup.users(user_id) ON DELETE CASCADE;


--
-- Name: user_profiles fk_user_profiles_user_id; Type: FK CONSTRAINT; Schema: nju_market_backup; Owner: postgres
--

ALTER TABLE ONLY nju_market_backup.user_profiles
    ADD CONSTRAINT fk_user_profiles_user_id FOREIGN KEY (user_id) REFERENCES nju_market_backup.users(user_id) ON DELETE CASCADE;


--
-- Name: messages messages_ibfk_1; Type: FK CONSTRAINT; Schema: nju_market_backup; Owner: postgres
--

ALTER TABLE ONLY nju_market_backup.messages
    ADD CONSTRAINT messages_ibfk_1 FOREIGN KEY (conversation_id) REFERENCES nju_market_backup.conversations(conversation_id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

\unrestrict MK6ueft5xtjierIt8PoTsf9pizJhzlkP9OV09UasArwOA9UPXyeFm7USoWJViuI

